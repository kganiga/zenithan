package com.zennature.wallpaper.ui.export

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zennature.wallpaper.appContainer
import com.zennature.wallpaper.data.EditorSettings
import com.zennature.wallpaper.data.MistGradient
import com.zennature.wallpaper.data.Resolution
import com.zennature.wallpaper.data.WallpaperItem
import com.zennature.wallpaper.data.aspectRatioDef
import com.zennature.wallpaper.data.resolutionForRatio
import com.zennature.wallpaper.export.ImageFormat
import com.zennature.wallpaper.render.WallpaperRenderer
import com.zennature.wallpaper.ui.components.BodyText
import com.zennature.wallpaper.ui.components.ChoiceChip
import com.zennature.wallpaper.ui.components.IconSquareButton
import com.zennature.wallpaper.ui.components.InverseButton
import com.zennature.wallpaper.ui.components.OutlineButton
import com.zennature.wallpaper.ui.components.SegmentedRow
import com.zennature.wallpaper.ui.components.SerifText
import com.zennature.wallpaper.ui.components.rememberStorageGate
import com.zennature.wallpaper.ui.components.toast
import com.zennature.wallpaper.ui.theme.Zen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

private enum class Bundle { SINGLE, DUO }
private enum class ResPreset(val label: String) { HIGH("QHD High"), NATIVE("Standard FHD"), ORIGINAL("Source Res") }

/** Longest edge rendered for "Source Res"; larger sources are scaled down to keep the bitmap under ~64 MB. */
private const val MAX_SOURCE_EDGE = 4096

/** Export dialog (ExportModal.tsx), rendering from the live studio settings. */
@Composable
fun ExportDialog(wallpaper: WallpaperItem, settings: EditorSettings, primaryColor: String, onClose: () -> Unit) {
    val c = Zen.colors
    val context = LocalContext.current
    val container = context.appContainer
    val scope = rememberCoroutineScope()
    val gate = rememberStorageGate()
    var format by remember { mutableStateOf(ImageFormat.JPEG) }
    var res by remember { mutableStateOf(ResPreset.HIGH) }
    var bundle by remember { mutableStateOf(Bundle.SINGLE) }
    var exporting by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    val sourceUrl = wallpaper.fullUrl.ifEmpty { wallpaper.thumbnailUrl }

    val dims = remember(res, settings.cropRatio, wallpaper) {
        if (res == ResPreset.ORIGINAL && wallpaper.width > 0 && wallpaper.height > 0) {
            sourceDims(wallpaper.width, wallpaper.height, settings)
        } else {
            resolutionForRatio(settings.cropRatio, res == ResPreset.HIGH)
        }
    }

    val preview by produceState<Bitmap?>(null, wallpaper, settings, dims) {
        val src = container.bitmaps.load(sourceUrl, 1600) ?: return@produceState
        value = withContext(Dispatchers.Default) {
            WallpaperRenderer.render(src, settings, 540, (540 / (dims.width.toFloat() / dims.height)).roundToInt(), primaryColor, container.fonts)
        }
    }

    suspend fun renderFull(s: EditorSettings): ByteArray? {
        val src = container.bitmaps.load(sourceUrl, maxOf(dims.width, dims.height)) ?: return null
        return withContext(Dispatchers.Default) {
            val bmp = WallpaperRenderer.render(src, s, dims.width, dims.height, primaryColor, container.fonts)
            container.exporter.encode(bmp, format, 95).also { bmp.recycle() }
        }
    }

    fun export(share: Boolean) {
        if (exporting) return
        exporting = true
        success = false
        scope.launch {
            val ok = runCatching {
                val ts = System.currentTimeMillis()
                val ratio = settings.cropRatio.id.replace(':', 'x')
                val lock = renderFull(settings) ?: return@runCatching false
                if (bundle == Bundle.DUO) {
                    val home = renderFull(homeSettings(settings))
                    val a = container.exporter.saveImage(lock, "zen_LOCK_SCREEN_${ratio}_$ts.${format.ext}", format.mime)
                    val b = home?.let { container.exporter.saveImage(it, "zen_HOME_SCREEN_SOFT_${ratio}_$ts.${format.ext}", format.mime) }
                    a != null && b != null
                } else {
                    val name = "zen_nature_${ratio}_$ts.${format.ext}"
                    if (share) {
                        container.exporter.shareImage(lock, name, format.mime, wallpaper.title, "Zen Nature Wallpaper: \"${wallpaper.title}\"")
                        true
                    } else {
                        container.exporter.saveImage(lock, name, format.mime) != null
                    }
                }
            }.getOrDefault(false)
            exporting = false
            if (ok) {
                success = true
                confetti++
            } else {
                toast(context, "Export failed. Check your connection and storage, then try again.")
            }
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            val shape = RoundedCornerShape(24.dp)
            Column(
                Modifier.widthIn(max = 720.dp).fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.border, shape)
                    .verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        SerifText("Export Zen Wallpaper", 21.sp)
                        BodyText("High-resolution export with verified license attribution")
                    }
                    IconSquareButton(Icons.Outlined.Close, "Close export modal", onClose, bordered = false)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        Modifier.width(120.dp).aspectRatio(dims.width.toFloat() / dims.height).clip(RoundedCornerShape(16.dp)).background(Color(0xFF0D0F0E)),
                        contentAlignment = Alignment.Center,
                    ) {
                        val bmp = preview
                        if (bmp != null) {
                            Image(bmp.asImageBitmap(), "Export preview", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            Text("12:00", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraLight, modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp))
                        } else {
                            BodyText("Rendering...", size = 10.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Label("Wallpaper Bundle")
                        SegmentedRow {
                            BundleOption(Icons.Outlined.Smartphone, "Single Wallpaper", "Current custom framing", bundle == Bundle.SINGLE, Modifier.weight(1f)) { bundle = Bundle.SINGLE }
                            BundleOption(Icons.Outlined.Layers, "Coordinating Duo", "Lock + Softened Home", bundle == Bundle.DUO, Modifier.weight(1f)) { bundle = Bundle.DUO }
                        }
                        Label("Output Resolution (${dims.width} × ${dims.height} px)")
                        SegmentedRow {
                            ResPreset.entries.forEach { p -> ChoiceChip(p.label, res == p, { res = p }, Modifier.weight(1f)) }
                        }
                        Label("File Format")
                        SegmentedRow {
                            ChoiceChip("High-Fidelity JPEG (95%)", format == ImageFormat.JPEG, { format = ImageFormat.JPEG }, Modifier.weight(1f))
                            ChoiceChip("Lossless PNG", format == ImageFormat.PNG, { format = ImageFormat.PNG }, Modifier.weight(1f))
                        }
                    }
                }
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.emerald.copy(alpha = 0.08f))
                        .border(1.dp, c.emerald.copy(alpha = 0.25f), RoundedCornerShape(12.dp)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.VerifiedUser, null, tint = c.emerald, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        BodyText("License: ${wallpaper.license.name} (${wallpaper.license.code})", color = c.textPrimary, weight = FontWeight.Medium)
                    }
                    BodyText(
                        if (wallpaper.license.attributionRequired) "Attribution is required for distribution. You can download the attribution text file below."
                        else "Public Domain / CC0 Dedication — Free for personal and commercial wallpaper use.",
                        size = 11.sp,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlineButton(
                        "Download Attribution File (TXT)",
                        {
                            gate {
                                scope.launch {
                                    val uri = container.exporter.saveAttribution(wallpaper)
                                    toast(context, if (uri != null) "Attribution saved to Downloads/Zenithan" else "Could not save attribution file")
                                }
                            }
                        },
                        Modifier.fillMaxWidth(), icon = Icons.Outlined.Description,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (bundle == Bundle.SINGLE) OutlineButton("Share", { export(true) }, icon = Icons.Outlined.Share, enabled = !exporting, minHeight = 46.dp)
                        InverseButton(
                            when {
                                exporting -> if (bundle == Bundle.DUO) "Processing Duo Set..." else "Processing Wallpaper..."
                                success -> if (bundle == Bundle.DUO) "Duo Saved to Device!" else "Saved to Device!"
                                else -> if (bundle == Bundle.DUO) "Download Duo (Lock + Home)" else "Download Wallpaper"
                            },
                            { gate { export(false) } },
                            Modifier.weight(1f),
                            icon = if (success && !exporting) Icons.Outlined.Check else Icons.Outlined.Download,
                            enabled = !exporting, minHeight = 46.dp,
                        )
                    }
                    if (exporting) CircularProgressIndicator(color = c.sage, strokeWidth = 2.dp, modifier = Modifier.size(20.dp).align(Alignment.CenterHorizontally))
                }
            }
            Confetti(confetti)
        }
    }
}

/** Coordinating home-screen variant: softer, darker edges, bottom mist, no text (web duo export). */
internal fun homeSettings(s: EditorSettings): EditorSettings = s.copy(
    blur = maxOf(5, s.blur + 5),
    vignette = maxOf(25, s.vignette + 12),
    mistGradient = MistGradient.BOTTOM,
    mistIntensity = maxOf(45, s.mistIntensity),
    textOverlay = s.textOverlay.copy(enabled = false),
)

/**
 * "Source Res": the largest frame of the chosen crop ratio that fits the source pixels, capped at
 * [MAX_SOURCE_EDGE]. The web build exported the raw source dimensions, which silently ignored the crop ratio.
 */
internal fun sourceDims(srcW: Int, srcH: Int, s: EditorSettings): Resolution {
    val def = aspectRatioDef(s.cropRatio)
    val target = def.widthRatio.toDouble() / def.heightRatio
    var w: Double
    var h: Double
    if (srcW.toDouble() / srcH > target) {
        h = srcH.toDouble(); w = h * target
    } else {
        w = srcW.toDouble(); h = w / target
    }
    val scale = min(1.0, MAX_SOURCE_EDGE / maxOf(w, h))
    w *= scale; h *= scale
    return Resolution(w.roundToInt().coerceAtLeast(1), h.roundToInt().coerceAtLeast(1))
}

@Composable
private fun Label(text: String) = BodyText(text.uppercase(), size = 10.sp, weight = FontWeight.SemiBold, letterSpacing = 1.sp)

@Composable
private fun BundleOption(icon: ImageVector, title: String, sub: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Zen.colors
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier.clip(shape).background(if (selected) c.inverseBg else Color.Transparent)
            .border(1.dp, if (selected) Color.Transparent else c.border, shape).clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (selected) c.inverseText else c.textSecondary
        Icon(icon, null, tint = fg, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(title, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(sub, color = fg.copy(alpha = 0.7f), fontSize = 9.sp, maxLines = 1)
        }
    }
}

private class Particle(val x: Float, val vx: Float, val vy: Float, val color: Color, val spin: Float, val size: Float)

/** 50-piece confetti burst from 70% height in the brand colours (canvas-confetti replacement). */
@Composable
private fun Confetti(trigger: Int) {
    if (trigger == 0) return
    val progress = remember(trigger) { Animatable(0f) }
    val particles = remember(trigger) {
        val colors = listOf(Color(0xFF8A9A86), Color(0xFFBBCBB8), Color(0xFFEFECE6), Color(0xFF2C2B29))
        List(50) {
            val angle = Math.toRadians(-90.0 + Random.nextDouble(-30.0, 30.0))
            val speed = Random.nextDouble(0.6, 1.2).toFloat()
            Particle(0.5f, (kotlin.math.cos(angle) * speed).toFloat(), (kotlin.math.sin(angle) * speed).toFloat(), colors[it % 4], Random.nextFloat() * 720f, Random.nextFloat() * 6f + 5f)
        }
    }
    LaunchedEffect(trigger) { progress.animateTo(1f, tween(1800, easing = LinearEasing)) }
    if (progress.value >= 1f) return
    Canvas(Modifier.fillMaxSize()) {
        val t = progress.value
        val alpha = 1f - t
        for (p in particles) {
            val x = size.width * p.x + p.vx * size.width * 0.6f * t
            val y = size.height * 0.7f + p.vy * size.height * 0.7f * t + size.height * 0.9f * t * t
            rotate(p.spin * t, Offset(x, y)) {
                drawRect(p.color.copy(alpha = alpha), Offset(x - p.size / 2, y - p.size / 4), Size(p.size, p.size / 2))
            }
        }
    }
}
