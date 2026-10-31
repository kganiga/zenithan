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

        if (isDefaultFeed) {
            providersQueried += ProviderType.LOCAL
            var pool = pool0
            if (filters.category.isNotEmpty()) {
                pool = pool.filter { it.category == filters.category }
                if (pool.size < 5) pool = pool + localWallpapers.filter { it.category == filters.category }
            }
            val startIndex = (page - 1) * PAGE_SIZE
            val paged = pool.drop(startIndex).take(PAGE_SIZE)
            fetched = when {
                paged.isNotEmpty() -> paged
                pool.isNotEmpty() -> {
                    // Endless stream: wrap around the pool once exhausted.
                    val looped = startIndex % pool.size
                    val slice = pool.drop(looped).take(PAGE_SIZE)
                    if (slice.size < PAGE_SIZE) slice + pool.take(PAGE_SIZE - slice.size) else slice
                }
                else -> emptyList()
            }
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
                    val maxLen = maxOf(localMatches.size, wiki.size, nasaItems.size, open.size)
                    val interleaved = ArrayList<WallpaperItem>(localMatches.size + wiki.size + nasaItems.size + open.size)
                    for (i in 0 until maxLen) {
                        localMatches.getOrNull(i)?.let(interleaved::add)
                        wiki.getOrNull(i)?.let(interleaved::add)
                        nasaItems.getOrNull(i)?.let(interleaved::add)
                        open.getOrNull(i)?.let(interleaved::add)
                    }
                    fetched = interleaved
                }
            }
        }

        if (fetched.isEmpty()) {
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

    private fun WallpaperItem.matches(q: String): Boolean =
        title.lowercase().contains(q) || (description?.lowercase()?.contains(q) ?: false)

    companion object {
        const val CACHE_EXPIRY_MS = 15 * 60 * 1000L
        const val PAGE_SIZE = 20
    }
}
