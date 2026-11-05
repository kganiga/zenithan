package com.zennature.wallpaper.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.max

/** Remote search function: (query, categoryTag, limit, page) -> items. Failures must return an empty list. */
typealias RemoteSearch = suspend (query: String, category: String, limit: Int, page: Int) -> List<WallpaperItem>

data class FetchResult(
    val items: List<WallpaperItem>,
    val providersQueried: List<ProviderType>,
    val isCached: Boolean,
)

private val catalogJson = Json { ignoreUnknownKeys = true }

fun parseLocalCatalog(raw: String): List<WallpaperItem> =
    catalogJson.decodeFromString(ListSerializer(WallpaperItem.serializer()), raw)

/**
 * Port of wallpaperRepository.ts. Same paging, interleaving, fallback, dedupe and filter rules;
 * the session shuffle and the 15 minute in-memory cache behave identically.
 */
class WallpaperRepository(
    private val localWallpapers: List<WallpaperItem>,
    private val wikimedia: RemoteSearch,
    private val nasa: RemoteSearch,
    private val openverse: RemoteSearch,
    private val clock: () -> Long = System::currentTimeMillis,
    private val shuffler: (List<WallpaperItem>) -> List<WallpaperItem> = { it.shuffled() },
) {
    private class CacheEntry(val timestamp: Long, val items: List<WallpaperItem>)

    private val cache = HashMap<String, CacheEntry>()
    private val lock = Any()

    @Volatile
    private var sessionShuffled: List<WallpaperItem> = shuffler(localWallpapers)

    fun reshuffleSession(): List<WallpaperItem> {
        sessionShuffled = shuffler(localWallpapers)
        synchronized(lock) { cache.clear() }
        return sessionShuffled
    }

    fun featuredWallpaper(): WallpaperItem {
        if (sessionShuffled.isEmpty()) sessionShuffled = shuffler(localWallpapers)
        return sessionShuffled.firstOrNull() ?: localWallpapers.first()
    }

    suspend fun fetchWallpapers(filters: FilterOptions, page: Int = 1): FetchResult {
        val queryTrimmed = filters.query.trim().lowercase()
        val categoryDef = CURATED_CATEGORIES.firstOrNull { it.id == filters.category }
        val effectiveQuery = queryTrimmed.ifEmpty { categoryDef?.query.orEmpty() }
        val categoryTag = filters.category.ifEmpty { "nature" }

        val cacheKey = "${filters.provider}-${filters.category}-$queryTrimmed-${filters.orientation}-${filters.licenseOnly}-p$page"
        synchronized(lock) { cache[cacheKey] }?.let { cached ->
            if (clock() - cached.timestamp < CACHE_EXPIRY_MS) {
                return FetchResult(cached.items, listOf(ProviderType.LOCAL), isCached = true)
            }
        }

        val providersQueried = mutableListOf<ProviderType>()
        var fetched: List<WallpaperItem> = emptyList()
        val pool0 = sessionShuffled

        val isDefaultFeed = queryTrimmed.isEmpty() &&
            (filters.provider == ProviderFilter.ALL || filters.provider == ProviderFilter.LOCAL)

        if (isDefaultFeed && filters.provider == ProviderFilter.LOCAL) {
            // "Curated 4K" only: the bundled catalog, paged once through (no silent repeats).
            providersQueried += ProviderType.LOCAL
            fetched = categoryPool(pool0, filters.category).drop((page - 1) * PAGE_SIZE).take(PAGE_SIZE)
        } else if (isDefaultFeed) {
            // Endless discovery feed: a slice of curated picks plus fresh remote results. Each page uses the next
            // search term from the category's rotation (or every mood when unfiltered), and the remote page
            // number only advances after a full cycle, so results keep coming without hammering one query.
            providersQueried += ProviderType.LOCAL
            val local = categoryPool(pool0, filters.category).drop((page - 1) * LOCAL_PER_PAGE).take(LOCAL_PER_PAGE)
            val queries = categoryDef?.queries ?: DISCOVERY_QUERIES
            val idx = page - 1
            val query = queries[idx % queries.size]
            val remotePage = idx / queries.size + 1
            val withNasa = categoryDef?.useNasa == true || (categoryDef == null && idx % 4 == 3)
            providersQueried += listOf(ProviderType.WIKIMEDIA, ProviderType.OPENVERSE)
            if (withNasa) providersQueried += ProviderType.NASA
            val (wiki, open, nasaItems) = coroutineScope {
                val w = async { runCatching { wikimedia(query, categoryTag, REMOTE_PER_PAGE, remotePage) }.getOrDefault(emptyList()) }
                val o = async { runCatching { openverse(query, categoryTag, REMOTE_PER_PAGE, remotePage) }.getOrDefault(emptyList()) }
                val n = async {
                    if (!withNasa) emptyList()
                    else if (categoryDef != null) runCatching { nasa(query, categoryTag, 10, remotePage) }.getOrDefault(emptyList())
                    else {
                        val n = idx / 4
                        runCatching { nasa(NASA_QUERIES[n % NASA_QUERIES.size], categoryTag, 10, n / NASA_QUERIES.size + 1) }.getOrDefault(emptyList())
                    }
                }
                Triple(w.await(), o.await(), n.await())
            }
            fetched = interleave(local, wiki, open, nasaItems)
        } else {
            when (filters.provider) {
                ProviderFilter.LOCAL -> {
                    providersQueried += ProviderType.LOCAL
                    fetched = localWallpapers
                }
                ProviderFilter.WIKIMEDIA -> {
                    providersQueried += ProviderType.WIKIMEDIA
                    fetched = wikimedia(effectiveQuery.ifEmpty { "minimal nature" }, categoryTag, 28, page)
                }
                ProviderFilter.NASA -> {
                    providersQueried += ProviderType.NASA
                    fetched = nasa(effectiveQuery.ifEmpty { "space nebula" }, categoryTag, 24, page)
                }
                ProviderFilter.OPENVERSE -> {
                    providersQueried += ProviderType.OPENVERSE
                    fetched = openverse(effectiveQuery.ifEmpty { "landscape nature" }, categoryTag, 24, page)
                }
                ProviderFilter.ALL -> {
                    providersQueried += listOf(ProviderType.WIKIMEDIA, ProviderType.NASA, ProviderType.OPENVERSE)
                    val localMatches = localWallpapers.filter { item ->
                        val matchQuery = queryTrimmed.isEmpty() || item.matches(queryTrimmed)
                        val matchCat = filters.category.isEmpty() || item.category == filters.category
                        matchQuery && matchCat
                    }
                    val (wiki, nasaItems, open) = coroutineScope {
                        val w = async { runCatching { wikimedia(effectiveQuery, categoryTag, 16, page) }.getOrDefault(emptyList()) }
                        val n = async { runCatching { nasa(effectiveQuery, categoryTag, 12, page) }.getOrDefault(emptyList()) }
                        val o = async { runCatching { openverse(effectiveQuery, categoryTag, 12, page) }.getOrDefault(emptyList()) }
                        Triple(w.await(), n.await(), o.await())
                    }
                    fetched = interleave(if (page == 1) localMatches else emptyList(), wiki, nasaItems, open)
                }
            }
        }

        // Offline/empty fallback only on the first page; later pages return empty so the feed can end honestly
        // instead of looping the same curated images forever.
        if (fetched.isEmpty() && page == 1) {
            var pool = pool0
            if (queryTrimmed.isNotEmpty()) pool = pool.filter { it.matches(queryTrimmed) }
            if (filters.category.isNotEmpty()) pool = pool.filter { it.category == filters.category }
            fetched = pool.ifEmpty { pool0.take(PAGE_SIZE) }
        }

        val seen = HashSet<String>()
        var filtered = fetched.filter { seen.add(it.thumbnailUrl.ifEmpty { it.fullUrl.ifEmpty { it.id } }) }

        filtered = when (filters.orientation) {
            OrientationFilter.PORTRAIT -> filtered.filter { it.height >= it.width * 1.05 }
            OrientationFilter.LANDSCAPE -> filtered.filter { it.width >= it.height * 1.05 }
            OrientationFilter.SQUARE -> filtered.filter { abs(it.width - it.height).toDouble() / max(it.width, it.height) < 0.2 }
            OrientationFilter.ALL -> filtered
        }
        filtered = when (filters.licenseOnly) {
            LicenseFilter.CC0_PD -> filtered.filter {
                !it.license.attributionRequired || it.license.code.contains("CC0") || it.license.code.contains("Public")
            }
            LicenseFilter.COMMERCIAL -> filtered.filter { it.license.commercialUseAllowed }
            LicenseFilter.ALL -> filtered
        }

        synchronized(lock) { cache[cacheKey] = CacheEntry(clock(), filtered) }
        return FetchResult(filtered, providersQueried, isCached = false)
    }

    private fun categoryPool(pool: List<WallpaperItem>, category: String): List<WallpaperItem> =
        if (category.isEmpty()) pool else pool.filter { it.category == category }

    private fun interleave(vararg lists: List<WallpaperItem>): List<WallpaperItem> {
        val out = ArrayList<WallpaperItem>(lists.sumOf { it.size })
        val max = lists.maxOfOrNull { it.size } ?: 0
        for (i in 0 until max) for (l in lists) l.getOrNull(i)?.let(out::add)
        return out
    }

    private fun WallpaperItem.matches(q: String): Boolean =
        title.lowercase().contains(q) || (description?.lowercase()?.contains(q) ?: false)

    companion object {
        const val CACHE_EXPIRY_MS = 15 * 60 * 1000L
        const val LOCAL_PER_PAGE = 6
        private val NASA_QUERIES = listOf("earth from space", "nebula", "galaxy", "aurora from space", "milky way", "planet")
        const val REMOTE_PER_PAGE = 14
        const val PAGE_SIZE = 20
    }
}
