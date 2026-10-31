package com.zennature.wallpaper.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.text.DateFormat
import java.util.Date
import kotlin.random.Random

enum class ThemeMode { LIGHT, DARK }

/**
 * On-device persistence replacing the web build's localStorage keys
 * (zen_nature_favorites_v1, _recents_v1, _projects_v1, _theme_v1).
 *
 * Every collection is exposed as a StateFlow so every screen sees the same data: in the web build the
 * Saved screen kept its own copy, so removing a favourite there left the header/tab badge count stale.
 * Images (imports, project previews) are stored as files, never inlined as base64 into JSON.
 */
class UserStore(private val dir: File) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val itemList = ListSerializer(WallpaperItem.serializer())
    private val projectList = ListSerializer(SavedProject.serializer())

    private val favoritesFile = File(dir, "favorites.json")
    private val recentsFile = File(dir, "recents.json")
    private val projectsFile = File(dir, "projects.json")
    private val themeFile = File(dir, "theme.txt")
    val importsDir = File(dir, "imports")
    val previewsDir = File(dir, "previews")

    private val _favorites = MutableStateFlow(read(favoritesFile, itemList))
    val favorites: StateFlow<List<WallpaperItem>> = _favorites.asStateFlow()

    private val _recents = MutableStateFlow(read(recentsFile, itemList))
    val recents: StateFlow<List<WallpaperItem>> = _recents.asStateFlow()

    private val _projects = MutableStateFlow(read(projectsFile, projectList))
    val projects: StateFlow<List<SavedProject>> = _projects.asStateFlow()

    private val _theme = MutableStateFlow(readTheme())
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    init {
        dir.mkdirs()
    }

    fun isFavorite(id: String): Boolean = _favorites.value.any { it.id == id }

    /** Returns true when the item is now a favourite. */
    fun toggleFavorite(item: WallpaperItem): Boolean {
        var nowFavorite = false
        _favorites.update { favs ->
            if (favs.any { it.id == item.id }) {
                favs.filterNot { it.id == item.id }
            } else {
                nowFavorite = true
                listOf(item) + favs
            }
        }
        write(favoritesFile, itemList, _favorites.value)
        return nowFavorite
    }

    fun addRecent(item: WallpaperItem) {
        _recents.update { recents -> (listOf(item) + recents.filterNot { it.id == item.id }).take(MAX_RECENTS) }
        write(recentsFile, itemList, _recents.value)
    }

    fun saveProject(
        wallpaper: WallpaperItem,
        settings: EditorSettings,
        title: String? = null,
        previewThumbnail: String? = null,
        now: Long = System.currentTimeMillis(),
    ): SavedProject {
        val project = SavedProject(
            id = "project-$now-${randomSuffix()}",
            title = title ?: "Zen Project ${DateFormat.getDateInstance().format(Date(now))}",
            createdAt = now,
            updatedAt = now,
            wallpaper = wallpaper,
            settings = settings,
            previewThumbnail = previewThumbnail,
        )
        _projects.update { listOf(project) + it }
        write(projectsFile, projectList, _projects.value)
        return project
    }

    fun deleteProject(projectId: String) {
        val removed = _projects.value.firstOrNull { it.id == projectId }
        _projects.update { list -> list.filterNot { it.id == projectId } }
        write(projectsFile, projectList, _projects.value)
        removed?.previewThumbnail?.let(::deleteOwnedFile)
    }

    fun setTheme(mode: ThemeMode) {
        _theme.value = mode
        runCatching { themeFile.writeText(mode.name) }
    }

    data class StorageInfo(val sizeKb: Long, val count: Int)

    /** Size of everything this app stores (JSON plus imported photos and project previews). */
    fun estimatedStorage(): StorageInfo {
        val bytes = listOf(favoritesFile, recentsFile, projectsFile, themeFile).sumOf { if (it.exists()) it.length() else 0L } +
            dirSize(importsDir) + dirSize(previewsDir)
        val records = _favorites.value.size + _recents.value.size + _projects.value.size
        return StorageInfo(sizeKb = (bytes + 512) / 1024, count = records)
    }

    /** Clears favourites, recents, projects and stored images. Theme is kept, as in the web build. */
    fun clearCache() {
        _favorites.value = emptyList()
        _recents.value = emptyList()
        _projects.value = emptyList()
        favoritesFile.delete()
        recentsFile.delete()
        projectsFile.delete()
        importsDir.deleteRecursively()
        previewsDir.deleteRecursively()
    }

    private fun deleteOwnedFile(uri: String) {
        val path = uri.removePrefix("file://")
        val file = File(path)
        if (file.canonicalPath.startsWith(dir.canonicalPath)) file.delete()
    }

    private fun dirSize(d: File): Long = d.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    private fun readTheme(): ThemeMode =
        runCatching { ThemeMode.valueOf(themeFile.readText().trim()) }.getOrDefault(ThemeMode.LIGHT)

    private fun <T> read(file: File, serializer: KSerializer<List<T>>): List<T> =
        runCatching { if (file.exists()) json.decodeFromString(serializer, file.readText()) else emptyList() }
            .getOrDefault(emptyList())

    private fun <T> write(file: File, serializer: KSerializer<List<T>>, value: List<T>) {
        synchronized(this) {
            runCatching {
                dir.mkdirs()
                val tmp = File(file.parentFile, file.name + ".tmp")
                tmp.writeText(json.encodeToString(serializer, value))
                if (!tmp.renameTo(file)) {
                    file.delete()
                    tmp.renameTo(file)
                }
            }
        }
    }

    private fun randomSuffix(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    companion object {
        const val MAX_RECENTS = 20
    }
}
