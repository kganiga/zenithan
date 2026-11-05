package com.zennature.wallpaper.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zennature.wallpaper.appContainer
import com.zennature.wallpaper.data.ProviderType
import com.zennature.wallpaper.data.WallpaperItem
import com.zennature.wallpaper.export.WallpaperTarget
import com.zennature.wallpaper.export.attributionLine
import com.zennature.wallpaper.export.coverFit
import com.zennature.wallpaper.export.sanitizedTitle
import com.zennature.wallpaper.export.sniffImageMime
import com.zennature.wallpaper.ui.components.BodyText
import com.zennature.wallpaper.ui.components.HomeScreenSimulation
import com.zennature.wallpaper.ui.components.LockScreenOverlay
import com.zennature.wallpaper.ui.components.LockVariant
import com.zennature.wallpaper.ui.components.OutlineButton
import com.zennature.wallpaper.ui.components.PrimaryButton
import com.zennature.wallpaper.ui.components.SerifText
import com.zennature.wallpaper.ui.components.WallpaperImage
import com.zennature.wallpaper.ui.components.rememberClock
import com.zennature.wallpaper.ui.components.rememberFlag
import com.zennature.wallpaper.ui.components.rememberStorageGate
import com.zennature.wallpaper.ui.studio.copyText
import com.zennature.wallpaper.ui.theme.Zen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class PreviewMode { CLEAN, LOCK, HOME }

private const val PHONE_W = 1440
private const val PHONE_H = 2560

/**
 * Wallpaper detail (DetailModal.tsx). "Apply to Device" sets the wallpaper directly through WallpaperManager
 * (home, lock or both) or hands a phone-fit copy to the system "Set as" picker. The web build could only
 * open a share sheet and ask the user to finish in their gallery app.
 */
@Composable
fun DetailSheet(
    wallpaper: WallpaperItem,
    isFavorited: Boolean,
    onToggleFavorite: () -> Unit,
    onEditInStudio: () -> Unit,
    onClose: () -> Unit,
) {
    val c = Zen.colors
    val context = LocalContext.current
    val container = context.appContainer
    val scope = rememberCoroutineScope()
    val gate = rememberStorageGate()
    var mode by remember { mutableStateOf(PreviewMode.CLEAN) }
    var busy by remember { mutableStateOf(false) }
    var banner by remember { mutableStateOf<String?>(null) }
    var showGuide by remember { mutableStateOf(false) }
    var showTargets by remember { mutableStateOf(false) }
    var copiedToken by remember { mutableIntStateOf(0) }
    val copied = rememberFlag(2500, copiedToken)
    val imageUrl = wallpaper.fullUrl.ifEmpty { wallpaper.thumbnailUrl }
    val slug = sanitizedTitle(wallpaper.title)

    fun launchAction(block: suspend () -> String?) {
        if (busy) return
        busy = true
        scope.launch {
            banner = runCatching { block() }.getOrElse { "Something went wrong: ${it.message ?: "unknown error"}" } ?: banner
            busy = false
        }
    }

    suspend fun phoneFit() = container.bitmaps.load(imageUrl, PHONE_H)?.let { withContext(Dispatchers.Default) { coverFit(it, PHONE_W, PHONE_H) } }

    fun applyTo(target: WallpaperTarget) = launchAction {
        showTargets = false
        val bmp = phoneFit() ?: return@launchAction "Could not load the image. Check your connection and try again."
        container.exporter.applyWallpaper(bmp, target)
        when (target) {
            WallpaperTarget.HOME -> "Wallpaper applied to your home screen."
            WallpaperTarget.LOCK -> "Wallpaper applied to your lock screen."
            WallpaperTarget.BOTH -> "Wallpaper applied to home and lock screens."
        }
    }

    fun setAs() = launchAction {
        showTargets = false
        val bmp = phoneFit() ?: return@launchAction "Could not load the image. Check your connection and try again."
        val bytes = withContext(Dispatchers.Default) { container.exporter.encode(bmp, com.zennature.wallpaper.export.ImageFormat.JPEG) }
        container.exporter.openSetAs(bytes, "zenithan-studio-$slug-${PHONE_W}x$PHONE_H.jpg")
        showGuide = true
        "System sheet opened. Pick your launcher or gallery to set the wallpaper."
    }

    fun download4k() = gate {
        launchAction {
            val bytes = container.exporter.fetchOriginal(imageUrl) ?: return@launchAction "Download failed. Check your connection and try again."
            val (mime, ext) = sniffImageMime(bytes)
            container.exporter.saveImage(bytes, "zenithan-studio-$slug-fullres.$ext", mime)
                ?.let { "Downloaded 4K Original file to Pictures/Zenithan." } ?: "Could not save the file."
        }
    }

    fun licenseFile() = gate {
        launchAction { container.exporter.saveAttribution(wallpaper)?.let { "License file saved to Downloads/Zenithan." } ?: "Could not save the license file." }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xB3000000)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
            BoxWithConstraints(Modifier.statusBarsPadding().navigationBarsPadding().padding(12.dp)) {
                val wide = maxWidth >= 768.dp
                val shape = RoundedCornerShape(24.dp)
                Box(
                    Modifier.widthIn(max = 1000.dp).fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.border, shape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                ) {
                    val preview: @Composable (Modifier) -> Unit = { mod -> Preview(wallpaper, imageUrl, mode, { mode = it }, mod) }
                    val info: @Composable (Modifier) -> Unit = { mod ->
                        Column(mod.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Header(wallpaper)
                            AnimatedVisibility(banner != null) {
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.emerald.copy(alpha = 0.12f))
                                        .border(1.dp, c.emerald.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Outlined.Check, null, tint = c.emerald, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    BodyText(banner.orEmpty(), color = c.textPrimary, modifier = Modifier.weight(1f))
                                }
                            }
                            // Device setup box
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surfaceMuted.copy(alpha = 0.5f))
                                    .border(1.dp, c.border, RoundedCornerShape(16.dp)).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Smartphone, null, tint = c.accent, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    BodyText("DEVICE SETUP", size = 11.sp, weight = FontWeight.SemiBold, color = c.textPrimary, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                                    BodyText("Phone Fit (9:16)", size = 10.sp, mono = true, color = c.accent)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PrimaryButton(
                                        if (busy) "Preparing Wallpaper..." else "Apply to Device",
                                        { showTargets = !showTargets }, Modifier.weight(1f), icon = Icons.Outlined.Smartphone, enabled = !busy,
                                    )
                                    OutlineButton("Download 4K", ::download4k, icon = Icons.Outlined.Download, enabled = !busy, minHeight = 46.dp)
                                }
                                AnimatedVisibility(showTargets) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        TargetRow(Icons.Outlined.Home, "Home screen") { applyTo(WallpaperTarget.HOME) }
                                        TargetRow(Icons.Outlined.Lock, "Lock screen") { applyTo(WallpaperTarget.LOCK) }
                                        TargetRow(Icons.Outlined.PhoneAndroid, "Home & lock screens") { applyTo(WallpaperTarget.BOTH) }
                                        TargetRow(Icons.Outlined.Wallpaper, "Set as… (open with launcher or gallery)", ::setAs)
                                    }
                                }
                                if (busy) CircularProgressIndicator(color = c.sage, strokeWidth = 2.dp, modifier = Modifier.size(20.dp).align(Alignment.CenterHorizontally))
                                Guide(showGuide, { showGuide = !showGuide })
                            }
                            LicenseCard(wallpaper) { url ->
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlineButton(
                                    if (copied) "Copied Attribution" else "Copy Attribution",
                                    { copyText(context, attributionLine(wallpaper)); copiedToken++ },
                                    Modifier.weight(1f),
                                    icon = if (copied) Icons.Outlined.Check else Icons.Outlined.Description,
                                    iconTint = if (copied) c.emerald else null,
                                )
                                OutlineButton("License File", ::licenseFile, icon = Icons.Outlined.Download)
                            }
                            Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlineButton(
                                    if (isFavorited) "Saved" else "Favorite", onToggleFavorite,
                                    icon = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    iconTint = if (isFavorited) c.rose else null,
                                    contentColor = if (isFavorited) c.rose else c.textSecondary,
                                    borderColor = if (isFavorited) c.rose.copy(alpha = 0.35f) else c.border,
                                    background = if (isFavorited) c.rose.copy(alpha = 0.08f) else Color.Transparent,
                                )
                                PrimaryButton("Customize in Studio", onEditInStudio, Modifier.weight(1f), icon = Icons.Outlined.AutoAwesome, minHeight = 44.dp)
                            }
                        }
                    }
                    if (wide) {
                        Row(Modifier.heightIn(max = 760.dp)) {
                            preview(Modifier.weight(1f).fillMaxSize())
                            Box(Modifier.weight(1f).verticalScroll(rememberScrollState())) { info(Modifier) }
                        }
                    } else {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            preview(Modifier.fillMaxWidth().aspectRatio(9f / 14f))
                            info(Modifier)
                        }
                    }
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(12.dp).size(36.dp).clip(CircleShape).background(Color(0x80000000)).clickable(onClick = onClose),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.Close, "Close details", tint = Color.White, modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }
}

@Composable
private fun Preview(wallpaper: WallpaperItem, url: String, mode: PreviewMode, onMode: (PreviewMode) -> Unit, modifier: Modifier) {
    val clock = rememberClock()
    Box(modifier.background(Color(0xFF0D0F0E))) {
        WallpaperImage(url, wallpaper.title, Modifier.fillMaxSize())
        when (mode) {
            PreviewMode.LOCK -> LockScreenOverlay(clock, variant = LockVariant.DETAIL)
            PreviewMode.HOME -> HomeScreenSimulation()
            PreviewMode.CLEAN -> Unit
        }
        Row(
            Modifier.align(Alignment.TopStart).padding(12.dp).clip(CircleShape).background(Color(0x99000000)).padding(3.dp),
        ) {
            listOf(
                Triple(PreviewMode.CLEAN, "Clean", Icons.Outlined.Image),
                Triple(PreviewMode.LOCK, "Lock Screen", Icons.Outlined.Lock),
                Triple(PreviewMode.HOME, "Home Screen", Icons.Outlined.GridView),
            ).forEach { (m, label, icon) ->
                val sel = m == mode
                Row(
                    Modifier.clip(CircleShape).background(if (sel) Color.White else Color.Transparent).clickable { onMode(m) }.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, null, tint = if (sel) Color.Black else Color.White.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = if (sel) Color.Black else Color.White.copy(alpha = 0.8f))
                }
            }
        }
        Text(
            "${wallpaper.width} × ${wallpaper.height} px",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 10.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).background(Color(0x99000000), CircleShape).padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun Header(wallpaper: WallpaperItem) {
    val c = Zen.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.clip(CircleShape).background(c.surfaceMuted).padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(6.dp).background(c.sage, CircleShape))
                Spacer(Modifier.width(6.dp))
                BodyText(
                    when (wallpaper.provider) {
                        ProviderType.WIKIMEDIA -> "Wikimedia Commons"
                        ProviderType.NASA -> "NASA Earth & Space"
                        ProviderType.OPENVERSE -> "Openverse Public Archive"
                        ProviderType.LOCAL -> "Zenithan Studio Curated"
                    },
                    size = 11.sp, weight = FontWeight.Medium, color = c.textPrimary,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.VerifiedUser, null, tint = c.emerald, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(3.dp))
                BodyText("Verified Open License", size = 11.sp, color = c.emerald)
            }
        }
        SerifText(wallpaper.title, 24.sp, lineHeight = 30.sp)
        wallpaper.description?.takeIf { it.isNotBlank() }?.let { BodyText(it, size = 13.sp, maxLines = 4) }
    }
}

@Composable
private fun TargetRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    val c = Zen.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.surface).border(1.dp, c.border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = c.accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        BodyText(label, color = c.textPrimary, weight = FontWeight.Medium)
    }
}

@Composable
private fun Guide(open: Boolean, onToggle: () -> Unit) {
    val c = Zen.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).clickable(onClick = onToggle).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, null, tint = c.textSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                BodyText("How to set on your device", weight = FontWeight.Medium)
                Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
            }
        }
        if (open) {
            // Android only: the old web build also showed iPhone and desktop steps, which make no sense in an APK.
            val steps = listOf(
                "Tap " to "Apply to Device" to " above and pick Home, Lock or both. It is applied instantly.",
                "Prefer your launcher's cropper? Choose " to "Set as…" to " and pick your launcher or Photos.",
            )
            steps.forEachIndexed { i, (pre, after) ->
                val (before, bold) = pre
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(18.dp).background(c.forest, CircleShape), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        buildAnnotatedString {
                            append(before)
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = c.textPrimary)) { append(bold) }
                            append(after)
                        },
                        color = c.textSecondary, fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun LicenseCard(wallpaper: WallpaperItem, onOpenUrl: (String) -> Unit) {
    val c = Zen.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).border(1.dp, c.border, shape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row {
            BodyText("Creator / Artist", modifier = Modifier.weight(1f))
            BodyText(wallpaper.license.author ?: "Open Contributor", color = c.textPrimary, weight = FontWeight.Medium, maxLines = 1)
        }
        Row {
            BodyText("License Code", modifier = Modifier.weight(1f))
            BodyText(wallpaper.license.code, mono = true, color = c.textPrimary)
        }
        if (wallpaper.sourcePageUrl.startsWith("http")) {
            Row(Modifier.clickable { onOpenUrl(wallpaper.sourcePageUrl) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                BodyText("View original archive entry", color = c.accent, weight = FontWeight.Medium)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, tint = c.accent, modifier = Modifier.size(13.dp))
            }
        }
    }
}
