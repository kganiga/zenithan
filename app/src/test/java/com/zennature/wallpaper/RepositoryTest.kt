package com.zennature.wallpaper

import com.zennature.wallpaper.data.FilterOptions
import com.zennature.wallpaper.data.LicenseFilter
import com.zennature.wallpaper.data.LicenseInfo
import com.zennature.wallpaper.data.OrientationFilter
import com.zennature.wallpaper.data.ProviderFilter
import com.zennature.wallpaper.data.ProviderType
import com.zennature.wallpaper.data.WallpaperItem
import com.zennature.wallpaper.data.WallpaperRepository
import com.zennature.wallpaper.data.parseLocalCatalog
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RepositoryTest {

    private val pd = LicenseInfo("CC0", "CC0", attributionRequired = false, commercialUseAllowed = true, derivativeWorksAllowed = true, verified = true)
    private val nc = LicenseInfo("CC BY-NC", "CC BY-NC", attributionRequired = true, commercialUseAllowed = false, derivativeWorksAllowed = true, verified = true)

    private fun item(id: String, provider: ProviderType = ProviderType.LOCAL, w: Int = 1080, h: Int = 1920, cat: String = "misty-mountains", lic: LicenseInfo = pd, thumb: String = "t/$id") =
        WallpaperItem(id, "Title $id", "desc $id", thumb, "f/$id", provider, "s/$id", w, h, w.toDouble() / h, cat, lic)

    private val locals = (1..45).map { item("l$it", cat = if (it % 3 == 0) "ocean-waves" else "misty-mountains") }

    private fun repo(
        wiki: List<WallpaperItem> = emptyList(),
        nasa: List<WallpaperItem> = emptyList(),
        open: List<WallpaperItem> = emptyList(),
        calls: MutableList<String> = mutableListOf(),
        now: () -> Long = { 0L },
    ) = WallpaperRepository(
        locals,
        wikimedia = { q, _, l, _ -> calls += "wiki:$q:$l"; wiki },
        nasa = { q, _, l, _ -> calls += "nasa:$q:$l"; nasa },
        openverse = { q, _, l, _ -> calls += "open:$q:$l"; open },
        clock = now,
        shuffler = { it },
    )

    @Test fun curatedOnlyPagesOnceWithoutWrap() = runBlocking {
        val r = repo()
        val f = FilterOptions(provider = ProviderFilter.LOCAL)
        val p1 = r.fetchWallpapers(f, 1).items
        val p3 = r.fetchWallpapers(f, 3).items
        val p4 = r.fetchWallpapers(f, 4).items
        assertEquals(20, p1.size)
        assertEquals("l1", p1.first().id)
        assertEquals(5, p3.size)
        assertTrue(p4.isEmpty()) // no silent repeats
    }

    @Test fun defaultFeedBlendsRemoteAndRotatesQueries() = runBlocking {
        val calls = mutableListOf<String>()
        val wiki = (1..14).map { item("w$it", ProviderType.WIKIMEDIA) }
        val open = (1..14).map { item("o$it", ProviderType.OPENVERSE) }
        val r = repo(wiki = wiki, open = open, calls = calls)
        val p1 = r.fetchWallpapers(FilterOptions(), 1).items
        assertEquals(6 + 14 + 14, p1.size)
        assertEquals(listOf("l1", "w1", "o1"), p1.take(3).map { it.id })
        r.fetchWallpapers(FilterOptions(), 2)
        val q = com.zennature.wallpaper.data.DISCOVERY_QUERIES
        assertTrue(calls.containsAll(listOf("wiki:${q[0]}:14", "open:${q[0]}:14", "wiki:${q[1]}:14", "open:${q[1]}:14")))
        assertTrue(calls.none { it.startsWith("nasa") }) // NASA only on every 4th unfiltered page
        r.fetchWallpapers(FilterOptions(), 4)
        assertTrue(calls.any { it.startsWith("nasa:earth from space") })
    }

    @Test fun remotePageAdvancesAfterFullQueryCycle() = runBlocking {
        val pages = mutableListOf<Pair<String, Int>>()
        val cat = com.zennature.wallpaper.data.CURATED_CATEGORIES.first { it.id == "ocean-waves" }
        val r = WallpaperRepository(locals, { q, _, _, p -> pages += q to p; emptyList() }, { _, _, _, _ -> emptyList() }, { _, _, _, _ -> emptyList() }, { 0L }, { it })
        val n = cat.queries.size
        r.fetchWallpapers(FilterOptions(category = cat.id), 1)
        r.fetchWallpapers(FilterOptions(category = cat.id), n + 1)
        assertEquals(listOf(cat.query to 1, cat.query to 2), pages)
    }

    @Test fun categoryFeedUsesCategoryAndNasaOnlyForSpace() = runBlocking {
        val calls = mutableListOf<String>()
        val r = repo(calls = calls)
        val ocean = r.fetchWallpapers(FilterOptions(category = "ocean-waves"), 1).items
        assertTrue(ocean.isNotEmpty() && ocean.all { it.category == "ocean-waves" })
        assertTrue(calls.none { it.startsWith("nasa") })
        r.fetchWallpapers(FilterOptions(category = "space-nebula"), 1)
        assertTrue(calls.any { it.startsWith("nasa:") })
    }

    @Test fun laterPagesEndWhenEverythingIsEmpty() = runBlocking {
        val r = repo()
        // page 1 falls back to the local catalog when remote is empty; far pages return nothing instead of looping
        assertTrue(r.fetchWallpapers(FilterOptions(), 1).items.isNotEmpty())
        assertTrue(r.fetchWallpapers(FilterOptions(), 50).items.isEmpty())
    }

    @Test fun categoriesAreWellFormed() {
        val cats = com.zennature.wallpaper.data.CURATED_CATEGORIES
        assertEquals(20, cats.size)
        assertEquals(cats.size, cats.map { it.id }.toSet().size)
        assertTrue(cats.all { it.queries.size >= 3 && it.queries.toSet().size == it.queries.size })
        val dq = com.zennature.wallpaper.data.DISCOVERY_QUERIES
        assertEquals(dq.size, dq.toSet().size)
        assertTrue(cats.all { it.query in dq })
        assertTrue(dq.take(cats.size).toSet() == cats.map { it.query }.toSet()) // round-robin: one per mood first
    }

    @Test fun allProvidersInterleaveAndDedupe() = runBlocking {
        val calls = mutableListOf<String>()
        val wiki = listOf(item("w1", ProviderType.WIKIMEDIA, thumb = "dup"), item("w2", ProviderType.WIKIMEDIA))
        val nasa = listOf(item("n1", ProviderType.NASA, thumb = "dup"))
        val open = listOf(item("o1", ProviderType.OPENVERSE))
        val items = repo(wiki, nasa, open, calls).fetchWallpapers(FilterOptions(query = "zzz-no-local"), 1).items
        assertEquals(listOf("w1", "o1", "w2"), items.map { it.id })
        assertTrue(calls.containsAll(listOf("wiki:zzz-no-local:16", "nasa:zzz-no-local:12", "open:zzz-no-local:12")))
    }

    @Test fun providerFailureFallsBackToLocal() = runBlocking {
        val r = WallpaperRepository(locals, { _, _, _, _ -> error("boom") }, { _, _, _, _ -> emptyList() }, { _, _, _, _ -> emptyList() }, { 0L }, { it })
        val items = r.fetchWallpapers(FilterOptions(query = "title l1"), 1).items
        assertFalse(items.isEmpty())
        assertTrue(items.all { it.provider == ProviderType.LOCAL })
    }

    @Test fun singleProviderDefaultQueries() = runBlocking {
        val calls = mutableListOf<String>()
        val r = repo(calls = calls)
        r.fetchWallpapers(FilterOptions(provider = ProviderFilter.NASA), 1)
        r.fetchWallpapers(FilterOptions(provider = ProviderFilter.WIKIMEDIA, query = "fog"), 1)
        assertEquals(listOf("nasa:space nebula:24", "wiki:fog:28"), calls)
    }

    @Test fun orientationAndLicenseFilters() = runBlocking {
        val wiki = listOf(item("land", ProviderType.WIKIMEDIA, w = 1920, h = 1080), item("sq", ProviderType.WIKIMEDIA, w = 1000, h = 1100), item("nc", ProviderType.WIKIMEDIA, lic = nc))
        val r = repo(wiki)
        assertEquals(listOf("land"), r.fetchWallpapers(FilterOptions(query = "q", provider = ProviderFilter.WIKIMEDIA, orientation = OrientationFilter.LANDSCAPE), 1).items.map { it.id })
        assertEquals(listOf("sq"), r.fetchWallpapers(FilterOptions(query = "q", provider = ProviderFilter.WIKIMEDIA, orientation = OrientationFilter.SQUARE), 1).items.map { it.id })
        assertEquals(listOf("land", "sq"), r.fetchWallpapers(FilterOptions(query = "q", provider = ProviderFilter.WIKIMEDIA, licenseOnly = LicenseFilter.COMMERCIAL), 1).items.map { it.id })
    }

    @Test fun cacheExpiresAfter15Minutes() = runBlocking {
        var now = 0L
        val calls = mutableListOf<String>()
        val r = repo(listOf(item("w1", ProviderType.WIKIMEDIA)), calls = calls, now = { now })
        val f = FilterOptions(provider = ProviderFilter.WIKIMEDIA, query = "a")
        r.fetchWallpapers(f, 1)
        assertTrue(r.fetchWallpapers(f, 1).isCached)
        now = WallpaperRepository.CACHE_EXPIRY_MS + 1
        assertFalse(r.fetchWallpapers(f, 1).isCached)
        assertEquals(2, calls.size)
    }

    /** The bundled catalog must parse and every asset it points at must exist. */
    @Test fun bundledCatalogIsValid() {
        val assets = File("src/main/assets")
        val catalog = parseLocalCatalog(File(assets, "catalog/local_wallpapers.json").readText())
        assertTrue(catalog.size >= 40)
        assertEquals(catalog.size, catalog.map { it.id }.toSet().size)
        catalog.filter { it.fullUrl.startsWith("file:///android_asset/") }.forEach {
            assertTrue("missing ${it.fullUrl}", File(assets, it.fullUrl.removePrefix("file:///android_asset/")).exists())
        }
    }
}
