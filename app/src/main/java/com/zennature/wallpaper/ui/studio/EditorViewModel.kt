package com.zennature.wallpaper.ui.studio

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zennature.wallpaper.AppContainer
import com.zennature.wallpaper.data.ColorPalette
import com.zennature.wallpaper.data.EditorSettings
import com.zennature.wallpaper.data.GridOverlayType
import com.zennature.wallpaper.data.SavedProject
import com.zennature.wallpaper.data.WallpaperItem
import com.zennature.wallpaper.data.aspectRatioDef
import com.zennature.wallpaper.render.WallpaperRenderer
import com.zennature.wallpaper.ui.StudioRequest
import com.zennature.wallpaper.ui.components.GridOpacity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

enum class EditorTab(val label: String) { CROP("Crop & Guides"), TUNE("Atmosphere"), MESH("Nature Mesh"), WASHES("Zen Washes"), TEXT("Typography") }

data class EditorUiState(
    val wallpaper: WallpaperItem,
    val settings: EditorSettings = EditorSettings(),
    val history: List<EditorSettings> = listOf(EditorSettings()),
    val historyIndex: Int = 0,
    val tab: EditorTab = EditorTab.CROP,
    val gridOpacity: GridOpacity = GridOpacity.MEDIUM,
    val showGoldenSpiral: Boolean = true,
    val palette: ColorPalette? = null,
    val preview: Bitmap? = null,
    val rendering: Boolean = false,
    val saveToken: Int = 0,
) {
    val canUndo get() = historyIndex > 0
    val canRedo get() = historyIndex < history.size - 1

    /** Mesh root colour as the web UI resolved it: explicit pick, else palette dominant, else sage. */
    val meshPrimary: String get() = settings.meshGradient.customPrimaryColor ?: palette?.dominant ?: "#556952"
}

/**
 * Wallpaper studio state (EditorScreen.tsx) kept in a ViewModel so edits survive tab switches.
 *
 * Differences from the web build, all bug fixes:
 * - every discrete change and every slider release is an undo step (web only recorded drags/resets/mesh toggles);
 * - the export dialog reads these live settings (web exported defaults or the last opened project's settings);
 * - opening a saved project restores its settings (web loaded the image but always started from defaults);
 * - the preview re-renders off the main thread and drops stale frames instead of queueing a render per tick.
 */
class EditorViewModel(private val container: AppContainer, initial: StudioRequest) : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState(wallpaper = initial.wallpaper))
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private var lastToken: Long? = null

    init {
        load(initial)
        viewModelScope.launch {
            _state
                .map { Triple(it.wallpaper, it.settings, it.palette) }
                .distinctUntilChanged()
                .collectLatest { (wallpaper, settings, palette) -> renderPreview(wallpaper, settings, palette) }
        }
    }

    fun load(request: StudioRequest) {
        if (request.token == lastToken) return
        lastToken = request.token
        val start = request.project?.settings ?: EditorSettings()
        _state.update {
            it.copy(
                wallpaper = request.wallpaper,
                settings = start,
                history = listOf(start),
                historyIndex = 0,
                palette = if (it.wallpaper.id == request.wallpaper.id) it.palette else null,
            )
        }
        viewModelScope.launch {
            val palette = container.bitmaps.palette(request.wallpaper.thumbnailUrl.ifEmpty { request.wallpaper.fullUrl })
            _state.update { if (it.wallpaper.id == request.wallpaper.id) it.copy(palette = palette) else it }
        }
    }

    /** Live change without a history entry (slider drag, pan gesture, text typing). */
    fun preview(transform: (EditorSettings) -> EditorSettings) {
        _state.update { it.copy(settings = transform(it.settings)) }
    }

    /** Records the current settings as an undo step (slider release, gesture end, text field commit). */
    fun commit() {
        _state.update { s ->
            if (s.history.getOrNull(s.historyIndex) == s.settings) return@update s
            val trimmed = s.history.take(s.historyIndex + 1) + s.settings
            val capped = if (trimmed.size > MAX_HISTORY) trimmed.drop(trimmed.size - MAX_HISTORY) else trimmed
            s.copy(history = capped, historyIndex = capped.size - 1)
        }
    }

    /** Discrete change recorded immediately (buttons, presets, toggles). */
    fun change(transform: (EditorSettings) -> EditorSettings) {
        preview(transform)
        commit()
    }

    fun undo() = _state.update { s ->
        if (!s.canUndo) s else s.copy(historyIndex = s.historyIndex - 1, settings = s.history[s.historyIndex - 1])
    }

    fun redo() = _state.update { s ->
        if (!s.canRedo) s else s.copy(historyIndex = s.historyIndex + 1, settings = s.history[s.historyIndex + 1])
    }

    fun reset() = change { EditorSettings() }

    fun setTab(tab: EditorTab) = _state.update { it.copy(tab = tab) }
    fun setGridOpacity(o: GridOpacity) = _state.update { it.copy(gridOpacity = o) }
    fun setGoldenSpiral(show: Boolean) = _state.update { it.copy(showGoldenSpiral = show) }

    fun cycleGrid() = change {
        it.copy(
            gridOverlay = when (it.gridOverlay) {
                GridOverlayType.NONE -> GridOverlayType.RULE_OF_THIRDS
                GridOverlayType.RULE_OF_THIRDS -> GridOverlayType.GOLDEN_RATIO
                GridOverlayType.GOLDEN_RATIO -> GridOverlayType.CENTER_CROSS
                GridOverlayType.CENTER_CROSS -> GridOverlayType.NONE
            },
        )
    }

    fun saveProject(): SavedProject {
        val s = _state.value
        val previewUri = s.preview?.let { bmp ->
            runCatching {
                val dir = container.store.previewsDir.apply { mkdirs() }
                val file = File(dir, "preview-${System.currentTimeMillis()}.jpg")
                file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 60, it) }
                android.net.Uri.fromFile(file).toString()
            }.getOrNull()
        }
        val project = container.store.saveProject(s.wallpaper, s.settings, "Zen ${s.wallpaper.title}", previewUri)
        _state.update { it.copy(saveToken = it.saveToken + 1) }
        return project
    }

    private suspend fun renderPreview(wallpaper: WallpaperItem, settings: EditorSettings, palette: ColorPalette?) {
        _state.update { it.copy(rendering = true) }
        val source = container.bitmaps.load(wallpaper.fullUrl.ifEmpty { wallpaper.thumbnailUrl }, PREVIEW_SOURCE_EDGE)
        if (source == null) {
            _state.update { it.copy(rendering = false) }
            return
        }
        val def = aspectRatioDef(settings.cropRatio)
        val width = PREVIEW_WIDTH
        val height = (PREVIEW_WIDTH / (def.widthRatio.toFloat() / def.heightRatio)).roundToInt()
        val bitmap = withContext(Dispatchers.Default) {
            WallpaperRenderer.render(
                source, settings, width, height,
                primaryColor = palette?.dominant ?: "#556952",
                fonts = container.fonts,
            )
        }
        _state.update { if (it.wallpaper.id == wallpaper.id) it.copy(preview = bitmap, rendering = false) else it }
    }

    companion object {
        const val PREVIEW_WIDTH = 540
        const val PREVIEW_SOURCE_EDGE = 1600
        const val MAX_HISTORY = 60
    }
}
