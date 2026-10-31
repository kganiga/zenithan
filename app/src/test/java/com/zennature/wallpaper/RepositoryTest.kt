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

    @Test fun defaultFeedPagesAndWraps() = runBlocking {
        val r = repo()
        val p1 = r.fetchWallpapers(FilterOptions(), 1).items
        val p3 = r.fetchWallpapers(FilterOptions(), 3).items
        val p4 = r.fetchWallpapers(FilterOptions(), 4).items
        assertEquals(20, p1.size)
        assertEquals("l1", p1.first().id)
        assertEquals(5, p3.size)
        assertEquals(20, p4.size) // wrap-around keeps the stream endless
        assertEquals("l16", p4.first().id) // (60 % 45) = 15
    }

    @Test fun categoryFilterOnDefaultFeed() = runBlocking {
        val items = repo().fetchWallpapers(FilterOptions(category = "ocean-waves"), 1).items
        assertEquals(15, items.size)
        assertTrue(items.all { it.category == "ocean-waves" })
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
