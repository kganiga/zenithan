package com.zennature.wallpaper.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zennature.wallpaper.appContainer
import com.zennature.wallpaper.data.PERSONAL_PHOTO_LICENSE
import com.zennature.wallpaper.data.ProviderType
import com.zennature.wallpaper.data.SavedProject
import com.zennature.wallpaper.data.ThemeMode
import com.zennature.wallpaper.data.WallpaperItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class Tab { HOME, CURATED, STUDIO, SAVED }

/** One-shot request for the studio: which wallpaper to load and optional settings to restore. */
data class StudioRequest(val wallpaper: WallpaperItem, val project: SavedProject?, val token: Long = System.nanoTime())

/** App-level navigation and cross-screen state (App.tsx). */
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val container = app.appContainer
    val store = container.store
    val repository = container.repository

    private val _tab = MutableStateFlow(Tab.HOME)
    val tab: StateFlow<Tab> = _tab.asStateFlow()

    /** Category to apply on the Wallpapers feed when arriving from Discover; null = keep feed state. */
    private val _feedCategory = MutableStateFlow<CategoryRequest?>(null)
    val feedCategory: StateFlow<CategoryRequest?> = _feedCategory.asStateFlow()

    val featured: WallpaperItem = repository.featuredWallpaper()

    private val _studio = MutableStateFlow(StudioRequest(featured, null))
    val studio: StateFlow<StudioRequest> = _studio.asStateFlow()

    private val _detail = MutableStateFlow<WallpaperItem?>(null)
    val detail: StateFlow<WallpaperItem?> = _detail.asStateFlow()

    private val _exportOpen = MutableStateFlow(false)
    val exportOpen: StateFlow<Boolean> = _exportOpen.asStateFlow()

    private val _scrollToTop = MutableStateFlow(0)
    val scrollToTop: StateFlow<Int> = _scrollToTop.asStateFlow()

    /** Unlike the web build, the feed keeps its scroll position, filters and loaded pages across tab switches. */
    fun navigate(tab: Tab) {
        _tab.value = tab
    }

    /** "Browse Wallpapers" / "Explore" shortcuts: open the feed with no category filter. */
    fun browseAll() {
        _feedCategory.value = CategoryRequest("")
        _tab.value = Tab.HOME
    }

    fun toggleTheme() {
        store.setTheme(if (store.theme.value == ThemeMode.LIGHT) ThemeMode.DARK else ThemeMode.LIGHT)
    }

    fun openDetail(item: WallpaperItem) {
        _detail.value = item
        store.addRecent(item)
    }

    fun closeDetail() {
        _detail.value = null
    }

    fun editInStudio(item: WallpaperItem, project: SavedProject? = null) {
        _studio.value = StudioRequest(item, project)
        store.addRecent(item)
        _tab.value = Tab.STUDIO
    }

    fun selectCategoryFromCurated(categoryId: String) {
        _feedCategory.value = CategoryRequest(categoryId)
        _tab.value = Tab.HOME
        _scrollToTop.value++
    }

    fun openExport() {
        _exportOpen.value = true
    }

    fun closeExport() {
        _exportOpen.value = false
    }

    fun toggleFavorite(item: WallpaperItem) {
        store.toggleFavorite(item)
    }

    /**
     * Imports a photo picked from the gallery. The file is copied into app storage so it survives the
     * picker grant expiring; the web build instead inlined the whole image as base64 into localStorage.
     */
    fun importPhoto(uri: Uri) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val item = withContext(Dispatchers.IO) {
                runCatching {
                    val now = System.currentTimeMillis()
                    val dir = store.importsDir.apply { mkdirs() }
                    val file = File(dir, "import-$now.jpg")
                    ctx.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
                        ?: return@runCatching null
                    val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts)
                    if (opts.outWidth <= 0 || opts.outHeight <= 0) {
                        file.delete()
                        return@runCatching null
                    }
                    val name = queryDisplayName(uri)
                    val title = name?.substringBeforeLast('.')?.replace(Regex("[_-]"), " ")?.takeIf { it.isNotBlank() }
                        ?: "Personal Photo"
                    val fileUri = Uri.fromFile(file).toString()
                    WallpaperItem(
                        id = "custom-$now",
                        title = title,
                        description = "Imported from personal device library.",
                        thumbnailUrl = fileUri,
                        fullUrl = fileUri,
                        provider = ProviderType.LOCAL,
                        sourcePageUrl = "local-device",
                        width = opts.outWidth,
                        height = opts.outHeight,
                        aspectRatio = opts.outWidth.toDouble() / opts.outHeight,
                        category = "nature",
                        license = PERSONAL_PHOTO_LICENSE,
                    )
                }.getOrNull()
            }
            if (item != null) editInStudio(item)
        }
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        getApplication<Application>().contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()
}

/** Wrapper so selecting the same category twice still re-triggers the feed. */
data class CategoryRequest(val categoryId: String, val token: Long = System.nanoTime())
