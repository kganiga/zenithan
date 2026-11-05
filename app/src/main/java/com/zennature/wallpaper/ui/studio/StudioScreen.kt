package com.zennature.wallpaper.ui.studio

import androidx.compose.material3.HorizontalDivider

import androidx.compose.foundation.layout.fillMaxHeight

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Grid3x3
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zennature.wallpaper.data.ASPECT_RATIOS
import com.zennature.wallpaper.data.AspectRatioType
import com.zennature.wallpaper.data.EditorSettings
import com.zennature.wallpaper.data.FontStyle
import com.zennature.wallpaper.data.GridOverlayType
import com.zennature.wallpaper.data.MeshBlendMode
import com.zennature.wallpaper.data.MeshGradientSettings
import com.zennature.wallpaper.data.MeshPresetId
import com.zennature.wallpaper.data.MistGradient
import com.zennature.wallpaper.data.TextOverlaySettings
import com.zennature.wallpaper.data.TextPosition
import com.zennature.wallpaper.data.TextSize
import com.zennature.wallpaper.data.UiPreviewMode
import com.zennature.wallpaper.data.WashPreset
import com.zennature.wallpaper.data.aspectRatioDef
import com.zennature.wallpaper.data.zenPhrases
import com.zennature.wallpaper.render.MESH_PRESETS
import com.zennature.wallpaper.render.generateMeshColors
import com.zennature.wallpaper.render.generateMeshNodes
import com.zennature.wallpaper.render.hexToArgb
import com.zennature.wallpaper.ui.MainViewModel
import com.zennature.wallpaper.ui.Tab
import com.zennature.wallpaper.ui.components.BodyText
import com.zennature.wallpaper.ui.components.ChoiceChip
import com.zennature.wallpaper.ui.components.GridOpacity
import com.zennature.wallpaper.ui.components.GridOverlay
import com.zennature.wallpaper.ui.components.HomeScreenSimulation
import com.zennature.wallpaper.ui.components.IconSquareButton
import com.zennature.wallpaper.ui.components.InverseButton
import com.zennature.wallpaper.ui.components.LabeledSlider
import com.zennature.wallpaper.ui.components.LockScreenOverlay
import com.zennature.wallpaper.ui.components.LockVariant
import com.zennature.wallpaper.ui.components.OutlineButton
import com.zennature.wallpaper.ui.components.PrimaryButton
import com.zennature.wallpaper.ui.components.SegmentedRow
import com.zennature.wallpaper.ui.components.SerifText
import com.zennature.wallpaper.ui.components.ZenCard
import com.zennature.wallpaper.ui.components.rememberClock
import com.zennature.wallpaper.ui.components.rememberFlag
import com.zennature.wallpaper.ui.discover.GridRows
import com.zennature.wallpaper.ui.theme.Zen
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

private val FALLBACK_SWATCHES = listOf("#556952", "#8A9A86", "#73716B", "#F8F6F0", "#121413")

/**
 * Studio tab (EditorScreen.tsx).
 *
 * Phones: compact toolbar, preview pinned to ~42% of the height, controls scroll underneath, so every slider
 * change is visible while you drag it (the old single scroll put a 630dp preview above the controls, and
 * dragging on the preview fought with page scrolling).
 * Landscape / tablets: preview on the left sized to the height, toolbar + controls scroll on the right.
 */
@Composable
fun StudioScreen(main: MainViewModel, editor: EditorViewModel, pickPhoto: () -> Unit, wide: Boolean) {
    val state by editor.state.collectAsStateWithLifecycle()
    val saved = rememberFlag(2500, state.saveToken)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        if (w >= 600.dp) {
            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(
                    Modifier.weight(if (w >= 900.dp) 5f else 4f).fillMaxHeight().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Viewport(state, editor, maxFrameHeight = (h - 96.dp).coerceAtLeast(160.dp), compact = false)
                }
                Column(Modifier.weight(7f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
                    TopBar(state, saved, pickPhoto, editor, onChangeImage = { main.navigate(Tab.HOME) }, onExport = main::openExport)
                    Spacer(Modifier.height(16.dp))
                    MonetCard(state, editor)
                    Spacer(Modifier.height(16.dp))
                    Controls(state, editor)
                    Spacer(Modifier.height(24.dp))
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                CompactTopBar(state, saved, pickPhoto, editor, onChangeImage = { main.navigate(Tab.HOME) }, onExport = main::openExport)
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Viewport(state, editor, maxFrameHeight = (h * 0.42f).coerceAtLeast(160.dp), compact = true)
                }
                HorizontalDivider(Modifier.padding(top = 8.dp), color = Zen.colors.border)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Controls(state, editor)
                    Spacer(Modifier.height(16.dp))
                    MonetCard(state, editor)
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

/** One-row toolbar for phones: icon actions only, so it never wraps onto two or three lines. */
@Composable
private fun CompactTopBar(
    state: EditorUiState,
    saved: Boolean,
    pickPhoto: () -> Unit,
    editor: EditorViewModel,
    onChangeImage: () -> Unit,
    onExport: () -> Unit,
) {
    val c = Zen.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconSquareButton(Icons.AutoMirrored.Outlined.ArrowBack, "Change image", onChangeImage, bordered = false)
        IconSquareButton(Icons.Outlined.UploadFile, "Import photo", pickPhoto, bordered = false, tint = c.accent)
        Spacer(Modifier.weight(1f))
        IconSquareButton(Icons.AutoMirrored.Outlined.Undo, "Undo", editor::undo, enabled = state.canUndo)
        IconSquareButton(Icons.AutoMirrored.Outlined.Redo, "Redo", editor::redo, enabled = state.canRedo)
        IconSquareButton(Icons.Outlined.RestartAlt, "Reset to default settings", editor::reset)
        IconSquareButton(
            if (saved) Icons.Outlined.Check else Icons.Outlined.Save, if (saved) "Saved" else "Save project",
            { editor.saveProject() }, tint = if (saved) c.emerald else c.textSecondary,
        )
        IconSquareButton(Icons.Outlined.Download, "Export", onExport, tint = c.inverseText, background = c.inverseBg, bordered = false)
    }
}

// ============================================================================ top bar

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopBar(
    state: EditorUiState,
    saved: Boolean,
    pickPhoto: () -> Unit,
    editor: EditorViewModel,
    onChangeImage: () -> Unit,
    onExport: () -> Unit,
) {
    val c = Zen.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onChangeImage).padding(vertical = 6.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = c.textSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                BodyText("Change Image", weight = FontWeight.Medium)
            }
            BodyText("  |  ", color = c.textTertiary)
            SerifText(state.wallpaper.title, 15.sp, Modifier.weight(1f), maxLines = 1)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlineButton("Import Photo", pickPhoto, icon = Icons.Outlined.UploadFile, iconTint = c.accent, minHeight = 40.dp)
            IconSquareButton(Icons.AutoMirrored.Outlined.Undo, "Undo", editor::undo, enabled = state.canUndo)
            IconSquareButton(Icons.AutoMirrored.Outlined.Redo, "Redo", editor::redo, enabled = state.canRedo)
            IconSquareButton(Icons.Outlined.RestartAlt, "Reset to default settings", editor::reset)
            OutlineButton(
                if (saved) "Saved" else "Save", { editor.saveProject() },
                icon = if (saved) Icons.Outlined.Check else Icons.Outlined.Save,
                iconTint = if (saved) c.emerald else null, minHeight = 40.dp,
            )
            InverseButton("Export", onExport, icon = Icons.Outlined.Download, minHeight = 40.dp)
        }
    }
}

// ============================================================================ viewport

@Composable
private fun Viewport(state: EditorUiState, editor: EditorViewModel, maxFrameHeight: Dp, compact: Boolean) {
    val c = Zen.colors
    val s = state.settings
    val ratio = aspectRatioDef(s.cropRatio)
    val phoneLike = s.cropRatio == AspectRatioType.MOBILE || s.cropRatio == AspectRatioType.FLAGSHIP

    Column(Modifier.fillMaxWidth().widthIn(max = 460.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BodyText(ratio.label, mono = true, size = 11.sp, modifier = Modifier.weight(1f))
            val gridOn = s.gridOverlay != GridOverlayType.NONE
            Row(
                Modifier.clip(RoundedCornerShape(8.dp))
                    .background(if (gridOn) c.sage.copy(alpha = 0.15f) else Color.Transparent)
                    .border(1.dp, if (gridOn) c.sage else c.border, RoundedCornerShape(8.dp))
                    .clickable(onClick = editor::cycleGrid).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Grid3x3, null, tint = if (gridOn) c.accent else c.textSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                BodyText(
                    when (s.gridOverlay) {
                        GridOverlayType.NONE -> "Grid: Off"
                        GridOverlayType.RULE_OF_THIRDS -> "3×3"
                        GridOverlayType.GOLDEN_RATIO -> "Golden Φ"
                        GridOverlayType.CENTER_CROSS -> "Center"
                    },
                    size = 11.sp, color = if (gridOn) c.accent else c.textSecondary,
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(Modifier.clip(RoundedCornerShape(8.dp)).background(c.surfaceMuted).padding(2.dp)) {
                listOf(UiPreviewMode.LOCKSCREEN to "Lock", UiPreviewMode.HOMESCREEN to "Apps", UiPreviewMode.CLEAN to "Clean").forEach { (mode, label) ->
                    val sel = s.uiPreviewMode == mode
                    Text(
                        label,
                        fontSize = 10.sp,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (sel) c.textPrimary else c.textTertiary,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (sel) c.surface else Color.Transparent)
                            .clickable { editor.change { it.copy(uiPreviewMode = mode) } }.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }

        val (frameWidth, frameAspect) = when (s.cropRatio) {
            AspectRatioType.MOBILE, AspectRatioType.FLAGSHIP -> 300.dp to 9f / 19f
            AspectRatioType.TABLET -> 340.dp to 3f / 4f
            AspectRatioType.DESKTOP -> 460.dp to 16f / 9f
            AspectRatioType.SQUARE -> 320.dp to 1f
        }
        val clock = rememberClock()
        val density = LocalDensity.current.density
        Box(
            // Bounded by width AND the height the layout can spare, so it fits short and landscape screens.
            Modifier.widthIn(max = minOf(frameWidth, maxFrameHeight * frameAspect)).fillMaxWidth().aspectRatio(frameAspect)
                .clip(RoundedCornerShape(32.dp)).background(Color(0xFF0D0F0E))
                .pointerInput(Unit) {
                    // Drag pans at the web touch factor (0.35 per CSS px); pinch zoom is new on native.
                    coroutineScope {
                        launch { commitOnRelease(editor) }
                        detectTransformGestures { _, pan, zoom, _ ->
                            editor.preview {
                                it.copy(
                                    panX = (it.panX + pan.x / density * 0.35f).coerceIn(-50f, 50f),
                                    panY = (it.panY + pan.y / density * 0.35f).coerceIn(-50f, 50f),
                                    zoom = (it.zoom * zoom).coerceIn(1f, 2.5f),
                                )
                            }
                        }
                    }
                },
        ) {
            state.preview?.let { bmp ->
                Image(bmp.asImageBitmap(), state.wallpaper.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.sage, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
            GridOverlay(s.gridOverlay, state.gridOpacity, state.showGoldenSpiral)
            if (phoneLike && s.uiPreviewMode == UiPreviewMode.LOCKSCREEN) LockScreenOverlay(clock, variant = LockVariant.STUDIO)
            if (phoneLike && s.uiPreviewMode == UiPreviewMode.HOMESCREEN) HomeScreenSimulation()
            if (s.zoom > 1.05f) {
                Row(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp).background(Color(0x99000000), CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.OpenWith, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Drag to pan", color = Color.White, fontSize = 10.sp)
                }
            }
        }
        BodyText(
            if (compact) "Drag to pan · Pinch to zoom · Guides are never exported"
            else "Drag to pan · Pinch to zoom · Toggle Apps/Lock above to test legibility · Guides & overlays are never exported",
            size = 11.sp, color = c.textTertiary, maxLines = if (compact) 1 else 3,
        )
    }
}

/** Records one undo step whenever the last finger lifts (web: touchend → history push). */
private suspend fun PointerInputScope.commitOnRelease(editor: EditorViewModel) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Final)
            if (event.changes.all { !it.pressed } && event.changes.any { it.previousPressed }) editor.commit()
        }
    }
}

@Composable
private fun MonetCard(state: EditorUiState, editor: EditorViewModel) {
    val c = Zen.colors
    val palette = state.palette ?: return
    val context = LocalContext.current
    var copiedToken by remember { mutableIntStateOf(0) }
    val copied = rememberFlag(2000, copiedToken)
    ZenCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Palette, null, tint = c.accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            BodyText("MATERIAL YOU (MONET) PALETTE", size = 11.sp, weight = FontWeight.SemiBold, color = c.textPrimary, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            OutlineButton(
                if (copied) "Copied" else "Copy Hex",
                {
                    copyText(
                        context,
                        "/* Zen Nature — Android Material You (Monet) Palette */\n" +
                            "--monet-dominant: ${palette.dominant};\n--monet-secondary: ${palette.secondary};\n" +
                            "--monet-muted: ${palette.muted};\n--monet-light: ${palette.light};\n--monet-dark: ${palette.dark};\n",
                    )
                    copiedToken++
                },
                icon = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy, minHeight = 32.dp,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            palette.swatches.forEach { hex ->
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .clickable { editor.change { it.copy(textOverlay = it.textOverlay.copy(color = hex)) } }.padding(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(hexToArgb(hex))).border(1.dp, c.border, RoundedCornerShape(8.dp)))
                    BodyText(hex.uppercase(), size = 9.sp, mono = true, maxLines = 1)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BodyText("Dynamic Mesh Generator:", size = 11.sp, modifier = Modifier.weight(1f))
            PrimaryButton(
                "Generate Mesh",
                {
                    editor.change {
                        it.copy(meshGradient = it.meshGradient.copy(enabled = true, customPrimaryColor = it.meshGradient.customPrimaryColor ?: palette.dominant))
                    }
                    editor.setTab(EditorTab.MESH)
                },
                icon = Icons.Outlined.AutoAwesome, minHeight = 34.dp,
            )
        }
    }
}

// ============================================================================ control panel

@Composable
private fun Controls(state: EditorUiState, editor: EditorViewModel) {
    val c = Zen.colors
    ZenCard(Modifier.fillMaxWidth(), padding = 0.dp) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            EditorTab.entries.forEach { tab ->
                val sel = state.tab == tab
                Column(
                    Modifier.clickable { editor.setTab(tab) }.width(IntrinsicTabWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(Modifier.padding(vertical = 12.dp, horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(tab.icon(), null, tint = if (sel) c.textPrimary else c.textSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(tab.label, fontSize = 11.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium, color = if (sel) c.textPrimary else c.textSecondary, maxLines = 1)
                    }
                    Box(Modifier.fillMaxWidth().height(2.dp).background(if (sel) c.sage else Color.Transparent))
                }
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            when (state.tab) {
                EditorTab.CROP -> CropTab(state, editor)
                EditorTab.TUNE -> TuneTab(state.settings, editor)
                EditorTab.MESH -> MeshTab(state, editor)
                EditorTab.WASHES -> WashTab(state.settings, editor)
                EditorTab.TEXT -> TextTab(state, editor)
            }
        }
    }
}

private val IntrinsicTabWidth = 132.dp

private fun EditorTab.icon(): ImageVector = when (this) {
    EditorTab.CROP -> Icons.Outlined.Crop
    EditorTab.TUNE -> Icons.Outlined.Tune
    EditorTab.MESH -> Icons.Outlined.AutoAwesome
    EditorTab.WASHES -> Icons.Outlined.Palette
    EditorTab.TEXT -> Icons.Outlined.TextFields
}

@Composable
private fun SectionTitle(text: String, icon: ImageVector? = null, badge: String? = null) {
    val c = Zen.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, tint = c.accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        BodyText(text, size = 13.sp, weight = FontWeight.SemiBold, color = c.textPrimary, modifier = Modifier.weight(1f))
        if (badge != null) BodyText(badge.uppercase(), size = 9.sp, mono = true, color = c.accent, letterSpacing = 1.sp)
    }
}

/** Option card with title, description and a check when selected (the web's ring-1 cards). */
@Composable
private fun OptionCard(
    title: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 0.dp,
    leading: (@Composable () -> Unit)? = null,
    trailingTag: String? = null,
) {
    val c = Zen.colors
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier.fillMaxWidth().clip(shape)
            .background(if (selected) c.surfaceSunken else c.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) c.textPrimary else c.border, shape)
            .clickable(onClick = onClick).padding(12.dp)
            .then(if (minHeight > 0.dp) Modifier.height(minHeight) else Modifier),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leading != null) leading()
        Row(verticalAlignment = Alignment.CenterVertically) {
            BodyText(title, size = 12.sp, weight = FontWeight.SemiBold, color = c.textPrimary, modifier = Modifier.weight(1f), maxLines = 2)
            if (selected) Icon(Icons.Outlined.Check, null, tint = c.accent, modifier = Modifier.size(14.dp))
        }
        BodyText(desc, size = 11.sp, maxLines = 3)
        if (trailingTag != null) BodyText(trailingTag.uppercase(), size = 9.sp, mono = true, color = c.accent, letterSpacing = 1.sp)
    }
}

// ---------------------------------------------------------------------------- crop

@Composable
private fun CropTab(state: EditorUiState, editor: EditorViewModel) {
    val c = Zen.colors
    val s = state.settings
    SectionTitle("Visual Crop & Composition Guides", Icons.Outlined.Grid3x3, "Persistent Overlay")
    val guides = listOf(
        Triple(GridOverlayType.RULE_OF_THIRDS, "3×3 Rule of Thirds", "Balances horizons and subject anchors on the 1/3 power points"),
        Triple(GridOverlayType.GOLDEN_RATIO, "Golden Ratio (Phi Φ)", "Divine 1:1.618 ratio creating natural flowing organic harmony"),
        Triple(GridOverlayType.CENTER_CROSS, "Center & Horizon Cross", "Exact 50% axes for symmetrical reflections & centered framing"),
        Triple(GridOverlayType.NONE, "No Grid Overlay", "Clean unobstructed wallpaper view without visual guides"),
    )
    GridRows(guides, 2, 10.dp) { (type, label, desc) ->
        OptionCard(label, desc, s.gridOverlay == type, { editor.change { it.copy(gridOverlay = type) } }, minHeight = 104.dp)
    }
    if (s.gridOverlay != GridOverlayType.NONE) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surfaceMuted.copy(alpha = 0.5f)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (s.gridOverlay == GridOverlayType.GOLDEN_RATIO) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        BodyText("Show Fibonacci Golden Spiral", color = c.textPrimary, weight = FontWeight.Medium)
                        BodyText("Natural logarithmic curve for sand waves & ridges", size = 11.sp)
                    }
                    ZenSwitch(state.showGoldenSpiral, editor::setGoldenSpiral)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                BodyText("Guide Visibility / Opacity", modifier = Modifier.weight(1f))
                GridOpacity.entries.forEach { lvl ->
                    Spacer(Modifier.width(6.dp))
                    ChoiceChip(lvl.name.lowercase().replaceFirstChar { it.uppercase() }, state.gridOpacity == lvl, { editor.setGridOpacity(lvl) })
                }
            }
        }
    }
    SectionTitle("Target Device Framing & Ratio")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ASPECT_RATIOS.forEach { r ->
            val sel = s.cropRatio == r.id
            val shape = RoundedCornerShape(12.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape).background(if (sel) c.surface else c.surfaceMuted.copy(alpha = 0.4f))
                    .border(if (sel) 2.dp else 1.dp, if (sel) c.textPrimary else c.border, shape)
                    .clickable { editor.change { it.copy(cropRatio = r.id) } }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BodyText(r.label, size = 13.sp, weight = FontWeight.SemiBold, color = c.textPrimary)
                        Spacer(Modifier.width(8.dp))
                        BodyText(r.deviceType.name.lowercase(), size = 9.sp, mono = true, color = c.accent,
                            modifier = Modifier.background(c.sage.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 1.dp))
                    }
                    BodyText("${r.description} · Target: ${r.idealWidth} × ${r.idealHeight} px", size = 11.sp)
                }
                if (sel) Icon(Icons.Outlined.Check, null, tint = c.accent, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------- tune

@Composable
private fun TuneTab(s: EditorSettings, editor: EditorViewModel) {
    val c = Zen.colors
    SectionTitle("Atmospheric Effects (Mist, Komorebi & Washi)", Icons.Outlined.WbSunny)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            BodyText("Mist Gradient (Creates clean space for phone dock/apps)", color = c.textPrimary, weight = FontWeight.Medium, modifier = Modifier.weight(1f))
            BodyText(if (s.mistGradient == MistGradient.NONE) "Off" else "${s.mistIntensity}%", mono = true)
        }
        SegmentedRow {
            listOf(MistGradient.NONE to "None", MistGradient.BOTTOM to "Bottom (Dock/Apps)", MistGradient.TOP to "Top (Clock/Widgets)").forEach { (m, label) ->
                ChoiceChip(label, s.mistGradient == m, { editor.change { it.copy(mistGradient = m) } }, Modifier.weight(1f))
            }
        }
        if (s.mistGradient != MistGradient.NONE) {
            IntSlider("", "${s.mistIntensity}%", s.mistIntensity, 15..90, 5, editor) { st, v -> st.copy(mistIntensity = v) }
        }
    }
    IntSlider("Komorebi (Dappled Sunlight Sunbeams)", "${s.komorebiRays}%", s.komorebiRays, 0..80, 5, editor) { st, v -> st.copy(komorebiRays = v) }
    IntSlider("Washi Handmade Paper Texture", "${s.washiTexture}%", s.washiTexture, 0..70, 5, editor) { st, v -> st.copy(washiTexture = v) }

    Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))

    LabeledSlider(
        "Scale & Zoom", "%.2f×".format(s.zoom), s.zoom, 1f..2.5f, 0.05f,
        onChange = { v -> editor.preview { it.copy(zoom = (v * 20).roundToInt() / 20f) } },
        onChangeFinished = editor::commit,
    )
    IntSlider("Brightness", "${s.brightness}%", s.brightness, 60..140, 1, editor) { st, v -> st.copy(brightness = v) }
    IntSlider("Contrast", "${s.contrast}%", s.contrast, 60..140, 1, editor) { st, v -> st.copy(contrast = v) }
    IntSlider("Saturation", "${s.saturation}%", s.saturation, 0..140, 1, editor) { st, v -> st.copy(saturation = v) }
    IntSlider(
        "Color Warmth",
        when {
            s.warmth > 0 -> "+${s.warmth} Warm"
            s.warmth < 0 -> "${s.warmth} Cool"
            else -> "Neutral"
        },
        s.warmth, -40..40, 2, editor,
    ) { st, v -> st.copy(warmth = v) }
    IntSlider("Soft Focus / Blur", "${s.blur} px", s.blur, 0..15, 1, editor) { st, v -> st.copy(blur = v) }
    IntSlider("Vignette Edge Falloff", "${s.vignette}%", s.vignette, 0..60, 2, editor) { st, v -> st.copy(vignette = v) }
    IntSlider("Analog Film Grain", "${s.grain}%", s.grain, 0..35, 1, editor) { st, v -> st.copy(grain = v) }
}

/** Integer slider: live preview while dragging, one undo step on release. */
@Composable
private fun IntSlider(
    label: String,
    valueText: String,
    value: Int,
    range: IntRange,
    step: Int,
    editor: EditorViewModel,
    apply: (EditorSettings, Int) -> EditorSettings,
) {
    LabeledSlider(
        label, valueText, value.toFloat(), range.first.toFloat()..range.last.toFloat(), step.toFloat(),
        onChange = { f ->
            val snapped = (range.first + ((f - range.first) / step).roundToInt() * step).coerceIn(range.first, range.last)
            editor.preview { apply(it, snapped) }
        },
        onChangeFinished = editor::commit,
    )
}

// ---------------------------------------------------------------------------- mesh

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MeshTab(state: EditorUiState, editor: EditorViewModel) {
    val c = Zen.colors
    val s = state.settings
    val mesh = s.meshGradient
    val primary = state.meshPrimary
    val context = LocalContext.current
    fun meshEdit(transform: (MeshGradientSettings) -> MeshGradientSettings): (EditorSettings) -> EditorSettings = { it.copy(meshGradient = transform(it.meshGradient)) }

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.sage.copy(alpha = 0.1f))
            .border(1.dp, c.sage.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionTitle("Dynamic Nature Mesh Background", Icons.Outlined.AutoAwesome, "Generative")
            BodyText("Creates subtle, multi-stop organic gradients dynamically derived from the primary colors extracted from your selected wallpaper.", size = 11.sp)
        }
        Spacer(Modifier.width(12.dp))
        ZenSwitch(mesh.enabled) { on -> editor.change(meshEdit { m -> m.copy(enabled = on) }) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            BodyText("Root Color Anchor (Extracted from Wallpaper)", color = c.textPrimary, weight = FontWeight.Medium, modifier = Modifier.weight(1f))
            BodyText(primary.uppercase(), mono = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val labels = listOf("Dom", "Sec", "Mute", "Light", "Dark")
            (state.palette?.swatches ?: FALLBACK_SWATCHES).forEachIndexed { idx, hex ->
                val sel = primary.equals(hex, ignoreCase = true)
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (sel) c.surfaceMuted else Color.Transparent)
                        .border(if (sel) 2.dp else 0.dp, if (sel) c.textPrimary else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable { editor.change(meshEdit { m -> m.copy(enabled = true, customPrimaryColor = hex) }) }.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(Color(hexToArgb(hex))).border(1.dp, c.border, CircleShape))
                    BodyText(labels[idx], size = 9.sp, mono = true)
                }
            }
        }
    }

    SectionTitle("Nature Mesh Presets", badge = "6 Algorithmic Palettes")
    GridRows(MESH_PRESETS, 2, 10.dp) { preset ->
        OptionCard(
            preset.name,
            preset.description,
            mesh.preset == preset.id && mesh.enabled,
            { editor.change(meshEdit { m -> m.copy(enabled = true, preset = preset.id, customPrimaryColor = primary) }) },
            leading = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MeshSwatch(primary, preset.id, mesh.seed, Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        BodyText(preset.japaneseName, size = 11.sp, color = c.accent)
                        BodyText(preset.subtitle, size = 10.sp, maxLines = 2)
                    }
                }
            },
            trailingTag = preset.moodTag,
        )
    }

    SectionTitle("Application & Blend Mode", Icons.Outlined.Layers, "Canvas Integration")
    val modes = listOf(
        Triple(MeshBlendMode.SOFT_LIGHT, "Atmospheric Wash", "Subtle soft light tint blending with the photo"),
        Triple(MeshBlendMode.OVERLAY, "Vibrant Harmony", "Rich atmospheric contrast and color depth"),
        Triple(MeshBlendMode.BACKDROP, "Ambient Backdrop", "Background layer behind framed wallpapers"),
        Triple(MeshBlendMode.PURE, "Pure Mesh Wallpaper", "Standalone minimalist nature gradient wallpaper"),
    )
    GridRows(modes, 2, 10.dp) { (mode, label, desc) ->
        OptionCard(label, desc, mesh.blendMode == mode, { editor.change(meshEdit { m -> m.copy(enabled = true, blendMode = mode, customPrimaryColor = primary) }) })
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionTitleInline("Fine-Tuning & Generative Seed", Modifier.weight(1f))
        OutlineButton("Nudge Nodes", { editor.change(meshEdit { m -> m.copy(enabled = true, seed = (m.seed + 17) % 1000) }) }, icon = Icons.Outlined.Shuffle, minHeight = 34.dp)
    }
    IntSlider("Mesh Gradient Intensity", "${mesh.intensity}%", mesh.intensity, 15..100, 5, editor) { st, v -> st.copy(meshGradient = st.meshGradient.copy(enabled = true, intensity = v)) }
    IntSlider("Atmospheric Diffusion (Blur)", "${mesh.blur} px", mesh.blur, 5..60, 5, editor) { st, v -> st.copy(meshGradient = st.meshGradient.copy(enabled = true, blur = v)) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            BodyText("Derived Nature Color Stops", color = c.textPrimary, weight = FontWeight.Medium, modifier = Modifier.weight(1f))
            BodyText("Tap stop to copy", size = 10.sp, color = c.textTertiary)
        }
        Row(Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(8.dp))) {
            generateMeshColors(primary, mesh.preset).forEach { hex ->
                Box(Modifier.weight(1f).fillMaxSize().background(Color(hexToArgb(hex))).clickable { copyText(context, hex) })
            }
        }
    }
}

@Composable
private fun SectionTitleInline(text: String, modifier: Modifier) {
    BodyText(text, size = 13.sp, weight = FontWeight.SemiBold, color = Zen.colors.textPrimary, modifier = modifier)
}

/** Mini mesh bubble: the web's stacked CSS radial-gradients drawn natively. */
@Composable
private fun MeshSwatch(primary: String, preset: MeshPresetId, seed: Int, modifier: Modifier) {
    val nodes = remember(primary, preset, seed) { generateMeshNodes(primary, preset, seed) }
    Canvas(modifier) {
        drawRect(Color(hexToArgb(nodes.last().color)))
        for (node in nodes.asReversed()) {
            val center = Offset(size.width * node.x / 100f, size.height * node.y / 100f)
            val farthest = max(
                max(hypot(center.x, center.y), hypot(size.width - center.x, center.y)),
                max(hypot(center.x, size.height - center.y), hypot(size.width - center.x, size.height - center.y)),
            )
            val radius = (farthest * node.radius / 100f).coerceAtLeast(1f)
            val alpha = (node.opacity * 0.85f).coerceIn(0f, 1f)
            val col = Color(hexToArgb(node.color))
            drawCircle(Brush.radialGradient(listOf(col.copy(alpha = alpha), col.copy(alpha = 0f)), center, radius), radius, center)
        }
    }
}

// ---------------------------------------------------------------------------- washes

@Composable
private fun WashTab(s: EditorSettings, editor: EditorViewModel) {
    val c = Zen.colors
    BodyText("Apply calm, contemplative duotone tint washes inspired by traditional Japanese natural materials and pigments.")
    val washes = listOf(
        Triple(WashPreset.NONE, "Natural Raw", "Original authentic spectrum") to null,
        Triple(WashPreset.SUMI, "Sumi Ink", "Monochrome carbon wash") to "#282A29",
        Triple(WashPreset.SAGE, "Moss Sage", "Calming lichen green") to "#8A9A86",
        Triple(WashPreset.SAND, "Kyoto Sand", "Warm gravel garden") to "#DCD0C0",
        Triple(WashPreset.MIST, "Morning Mist", "Silver vapor glaze") to "#E5ECE9",
        Triple(WashPreset.TWILIGHT, "Twilight Indigo", "Deep dusk sky") to "#1C2A3C",
    )
    GridRows(washes, 2, 10.dp) { (info, hex) ->
        val (preset, label, desc) = info
        OptionCard(
            label, desc, s.washPreset == preset, { editor.change { it.copy(washPreset = preset) } },
            leading = {
                val mod = Modifier.size(24.dp).clip(CircleShape)
                if (hex == null) Box(mod.border(1.dp, c.textTertiary, CircleShape)) else Box(mod.background(Color(hexToArgb(hex))))
            },
        )
    }
    if (s.washPreset != WashPreset.NONE) {
        IntSlider("Wash Intensity", "${s.washIntensity}%", s.washIntensity, 10..80, 2, editor) { st, v -> st.copy(washIntensity = v) }
    }
}

// ---------------------------------------------------------------------------- typography

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TextTab(state: EditorUiState, editor: EditorViewModel) {
    val c = Zen.colors
    val t = state.settings.textOverlay
    fun text(transform: (TextOverlaySettings) -> TextOverlaySettings): (EditorSettings) -> EditorSettings = { it.copy(textOverlay = transform(it.textOverlay)) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BodyText("Display Zen Typography", size = 13.sp, color = c.textPrimary, weight = FontWeight.SemiBold)
            BodyText("Haiku, mindfulness phrases, or personal dates", size = 11.sp)
        }
        ZenSwitch(t.enabled) { on -> editor.change(text { it.copy(enabled = on) }) }
    }
    if (!t.enabled) return

    Label("Contemplative Presets")
    val phrases = remember { zenPhrases() }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        phrases.forEach { p ->
            ChoiceChip(
                (p.kanji?.let { "$it · " } ?: "") + p.text.substringBefore('\n'),
                t.text == p.text,
                { editor.change(text { it.copy(text = p.text, subtext = p.subtext ?: "", kanji = p.kanji ?: "") }) },
            )
        }
    }

    Label("Custom Text")
    ZenTextField(t.text, "Enter a calming thought, poem, or reminder...", editor, minLines = 3) { v -> text { it.copy(text = v) } }

    state.palette?.let { palette ->
        Label("Typography Color (Matched to Wallpaper Palette)")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("#FFFFFF", "#121413", palette.dominant, palette.secondary, palette.light, palette.dark).forEach { col ->
                val sel = t.color.equals(col, ignoreCase = true)
                Box(
                    Modifier.size(if (sel) 32.dp else 28.dp).clip(CircleShape).background(Color(hexToArgb(col)))
                        .border(if (sel) 2.dp else 1.dp, if (sel) c.sage else c.border, CircleShape)
                        .clickable { editor.change(text { it.copy(color = col) }) },
                )
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Label("Author / Subtitle")
            ZenTextField(t.subtext, "e.g. Matsuo Basho", editor) { v -> text { it.copy(subtext = v) } }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Label("Japanese Kanji Motif")
            ZenTextField(t.kanji, "e.g. 静寂", editor) { v -> text { it.copy(kanji = v) } }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Label("Vertical Position")
            SegmentedRow {
                TextPosition.entries.forEach { p ->
                    ChoiceChip(p.name.lowercase().replaceFirstChar { it.uppercase() }, t.position == p, { editor.change(text { it.copy(position = p) }) }, Modifier.weight(1f))
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Label("Font Style")
            SegmentedRow {
                listOf(FontStyle.SERIF to "Playfair Serif", FontStyle.SANS to "Jakarta Sans").forEach { (f, label) ->
                    ChoiceChip(label, t.fontStyle == f, { editor.change(text { it.copy(fontStyle = f) }) }, Modifier.weight(1f))
                }
            }
        }
    }

    // New on native: the renderer always supported these, the web build simply never exposed them.
    Label("Text Size")
    SegmentedRow {
        TextSize.entries.forEach { sz ->
            ChoiceChip(sz.name, t.size == sz, { editor.change(text { it.copy(size = sz) }) }, Modifier.weight(1f))
        }
    }
    LabeledSlider(
        "Text Opacity", "${(t.opacity * 100).roundToInt()}%", t.opacity, 0.2f..1f, 0.05f,
        onChange = { v -> editor.preview(text { it.copy(opacity = (v * 20).roundToInt() / 20f) }) },
        onChangeFinished = editor::commit,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        BodyText("Show kanji motif", color = c.textPrimary, weight = FontWeight.Medium, modifier = Modifier.weight(1f))
        ZenSwitch(t.showJapaneseMark) { on -> editor.change(text { it.copy(showJapaneseMark = on) }) }
    }
}

@Composable
private fun Label(text: String) {
    BodyText(text.uppercase(), size = 10.sp, weight = FontWeight.SemiBold, letterSpacing = 1.sp)
}

/** Text input that previews as you type and records one undo step when focus leaves. */
@Composable
private fun ZenTextField(
    value: String,
    placeholder: String,
    editor: EditorViewModel,
    minLines: Int = 1,
    apply: (String) -> (EditorSettings) -> EditorSettings,
) {
    val c = Zen.colors
    val shape = RoundedCornerShape(10.dp)
    BasicTextField(
        value = value,
        onValueChange = { editor.preview(apply(it)) },
        minLines = minLines,
        textStyle = TextStyle(color = c.textPrimary, fontSize = 13.sp),
        cursorBrush = SolidColor(c.sage),
        modifier = Modifier.fillMaxWidth().clip(shape).background(c.surfaceSunken).border(1.dp, c.border, shape)
            .onFocusChanged { if (!it.isFocused) editor.commit() }.padding(10.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, color = c.textTertiary, fontSize = 13.sp)
                inner()
            }
        },
    )
}

@Composable
private fun ZenSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Zen.colors
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = if (c.isDark) c.sage else c.forest,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = c.borderStrong,
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

internal fun copyText(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Zen Nature", text))
}
