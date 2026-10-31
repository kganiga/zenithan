package com.zennature.wallpaper

import com.zennature.wallpaper.data.EditorSettings
import com.zennature.wallpaper.data.LicenseInfo
import com.zennature.wallpaper.data.ProviderType
import com.zennature.wallpaper.data.ThemeMode
import com.zennature.wallpaper.data.UserStore
import com.zennature.wallpaper.data.WallpaperItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class UserStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun item(id: String) = WallpaperItem(
        id, "T $id", null, "t/$id", "f/$id", ProviderType.LOCAL, "s", 10, 20, 0.5, "c",
        LicenseInfo("CC0", "CC0", attributionRequired = false, commercialUseAllowed = true, derivativeWorksAllowed = true, verified = true),
    )

    @Test fun favoritesToggleAndPersist() {
        val dir = tmp.newFolder("zen")
        val store = UserStore(dir)
        assertTrue(store.toggleFavorite(item("a")))
        store.toggleFavorite(item("b"))
        assertFalse(store.toggleFavorite(item("a")))
        assertEquals(listOf("b"), UserStore(dir).favorites.value.map { it.id })
    }

    @Test fun recentsCappedAt20AndMostRecentFirst() {
        val store = UserStore(tmp.newFolder("zen"))
        (1..25).forEach { store.addRecent(item("r$it")) }
        store.addRecent(item("r10"))
        val ids = store.recents.value.map { it.id }
        assertEquals(20, ids.size)
        assertEquals("r10", ids.first())
        assertEquals(1, ids.count { it == "r10" })
    }

    @Test fun projectsSaveDeleteAndClear() {
        val dir = tmp.newFolder("zen")
        val store = UserStore(dir)
        val preview = File(store.previewsDir.apply { mkdirs() }, "p.jpg").apply { writeText("x") }
        val p = store.saveProject(item("a"), EditorSettings(zoom = 1.5f), "Zen T a", android_uri(preview))
        assertEquals(1, UserStore(dir).projects.value.size)
        assertEquals(1.5f, UserStore(dir).projects.value.first().settings.zoom)
        store.deleteProject(p.id)
        assertTrue(store.projects.value.isEmpty())
        assertFalse("owned preview file should be deleted", preview.exists())

        store.setTheme(ThemeMode.DARK)
        store.toggleFavorite(item("f"))
        assertTrue(store.estimatedStorage().count >= 1)
        store.clearCache()
        val reopened = UserStore(dir)
        assertTrue(reopened.favorites.value.isEmpty())
        assertEquals(ThemeMode.DARK, reopened.theme.value)
    }

    private fun android_uri(f: File) = "file://" + f.absolutePath
}
