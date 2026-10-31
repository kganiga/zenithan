package com.zennature.wallpaper.ui.saved

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zennature.wallpaper.appContainer
import com.zennature.wallpaper.data.SavedProject
import com.zennature.wallpaper.data.WallpaperItem
import com.zennature.wallpaper.ui.MainViewModel
import com.zennature.wallpaper.ui.components.BodyText
import com.zennature.wallpaper.ui.components.InverseButton
import com.zennature.wallpaper.ui.components.OutlineButton
import com.zennature.wallpaper.ui.components.SerifText
import com.zennature.wallpaper.ui.components.WallpaperImage
import com.zennature.wallpaper.ui.components.rememberStorageGate
import com.zennature.wallpaper.ui.components.toast
import com.zennature.wallpaper.ui.discover.GridRows
import com.zennature.wallpaper.ui.theme.Zen
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private enum class SavedTab { FAVORITES, PROJECTS }

/** Saved tab (CollectionsScreen.tsx). Lists are live flows, so counts never go stale (web bug). */
@Composable
fun SavedScreen(main: MainViewModel, wide: Boolean) {
    val c = Zen.colors
    val favorites by main.store.favorites.collectAsStateWithLifecycle()
    val projects by main.store.projects.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(SavedTab.FAVORITES) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val storage = remember(favorites, projects, confirmClear) { main.store.estimatedStorage() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gate = rememberStorageGate()

    fun downloadLicense(item: WallpaperItem) = gate {
        scope.launch {
            val uri = context.appContainer.exporter.saveAttribution(item)
            toast(context, if (uri != null) "License file saved to Downloads/Zenithan" else "Could not save license file")
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                SerifText("Saved Collections", 30.sp)
                BodyText("Your favorited nature photography and custom wallpaper projects stored locally on this device.", size = 13.sp)
            }
            Row(Modifier.clip(RoundedCornerShape(12.dp)).background(c.surfaceMuted).padding(4.dp)) {
                TabButton("Favorites (${favorites.size})", Icons.Outlined.FavoriteBorder, tab == SavedTab.FAVORITES) { tab = SavedTab.FAVORITES }
                TabButton("Custom Projects (${projects.size})", Icons.Outlined.Tune, tab == SavedTab.PROJECTS) { tab = SavedTab.PROJECTS }
            }
        }

        when (tab) {
            SavedTab.FAVORITES -> if (favorites.isEmpty()) {
                EmptyState(
                    Icons.Outlined.FavoriteBorder, "No Favorites Yet",
                    "Tap the heart icon on any nature photograph to save it for offline contemplation.",
                ) { InverseButton("Explore Nature Archives", main::browseAll, icon = Icons.Outlined.Explore) }
            } else {
                BoxWithConstraints {
                    val cols = when {
                        maxWidth >= 1024.dp -> 5
                        maxWidth >= 768.dp -> 4
                        maxWidth >= 600.dp -> 3
                        else -> 2
                    }
                    GridRows(favorites, cols, 12.dp) { item ->
                        FavoriteTile(
                            item,
                            onOpen = { main.openDetail(item) },
                            onRemove = { main.toggleFavorite(item) },
                            onLicense = { downloadLicense(item) },
                            onEdit = { main.editInStudio(item) },
                        )
                    }
                }
            }
            SavedTab.PROJECTS -> if (projects.isEmpty()) {
                EmptyState(
                    Icons.Outlined.Tune, "No Saved Projects",
                    "When you customize adjustments or typography in Studio, tap \"Save\" to keep your editable setup here.",
                ) {}
            } else {
                BoxWithConstraints {
                    val cols = if (maxWidth >= 768.dp) 2 else 1
                    GridRows(projects, cols, 12.dp) { proj ->
                        ProjectRow(proj, onOpen = { main.editInStudio(proj.wallpaper, proj) }, onDelete = { main.store.deleteProject(proj.id) })
                    }
                }
            }
        }

        // Storage card
        val shape = RoundedCornerShape(16.dp)
        Column(
            Modifier.fillMaxWidth().clip(shape).background(c.surfaceMuted.copy(alpha = 0.5f)).border(1.dp, c.border, shape).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Storage, null, tint = c.accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    BodyText("On-Device Storage: ~${storage.sizeKb} KB (${storage.count} stored records)", color = c.textPrimary, weight = FontWeight.Medium)
                    BodyText("100% on-device. No user data, text edits, or custom wallpapers leave your device.", size = 11.sp)
                }
            }
            if (confirmClear) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BodyText("Clear all local data?", color = c.rose, weight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    OutlineButton("Confirm", { main.store.clearCache(); confirmClear = false }, background = c.rose, contentColor = Color.White, borderColor = c.rose, minHeight = 34.dp)
                    OutlineButton("Cancel", { confirmClear = false }, minHeight = 34.dp)
                }
            } else {
                OutlineButton("Clear Local Cache", { confirmClear = true }, minHeight = 36.dp, contentColor = c.rose, borderColor = c.rose.copy(alpha = 0.4f))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TabButton(text: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val c = Zen.colors
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(if (selected) c.surface else Color.Transparent).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (selected) c.textPrimary else c.textSecondary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = if (selected) c.textPrimary else c.textSecondary)
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, body: String, action: @Composable () -> Unit) {
    val c = Zen.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).border(1.dp, c.border, shape).padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = c.textTertiary, modifier = Modifier.size(32.dp))
        SerifText(title, 18.sp)
        Text(body, color = c.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        action()
    }
}

@Composable
private fun FavoriteTile(item: WallpaperItem, onOpen: () -> Unit, onRemove: () -> Unit, onLicense: () -> Unit, onEdit: () -> Unit) {
    val c = Zen.colors
    val shape = RoundedCornerShape(14.dp)
    Box(Modifier.fillMaxWidth().aspectRatio(9f / 16f).clip(shape).background(Color(0xFF0D0F0E)).border(1.dp, c.border, shape).clickable(onClick = onOpen)) {
        WallpaperImage(item.thumbnailUrl, item.title, Modifier.fillMaxSize())
        Box(
            Modifier.align(Alignment.TopEnd).padding(8.dp).size(30.dp).clip(CircleShape).background(Color(0x80000000)).clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Delete, "Remove from favorites", tint = Color.White, modifier = Modifier.size(15.dp)) }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xD9000000)))).padding(start = 10.dp, end = 10.dp, top = 24.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Column {
                SerifText(item.title, 12.sp, color = Color.White, maxLines = 1)
                BodyText(item.license.author ?: "Open Creator", size = 10.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniAction("License", Icons.Outlined.Description, Color(0x33FFFFFF), Color.White, Modifier.weight(1f), onLicense)
                MiniAction("Edit", Icons.Outlined.AutoAwesome, Color(0xE6FFFFFF), Color.Black, Modifier.weight(1f), onEdit)
            }
        }
    }
}

@Composable
private fun MiniAction(text: String, icon: ImageVector, bg: Color, fg: Color, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(6.dp)).background(bg).clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = fg, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProjectRow(proj: SavedProject, onOpen: () -> Unit, onDelete: () -> Unit) {
    val c = Zen.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.border, shape).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WallpaperImage(
            proj.previewThumbnail ?: proj.wallpaper.thumbnailUrl, proj.title,
            Modifier.width(56.dp).aspectRatio(9f / 16f).clip(RoundedCornerShape(8.dp)).background(Color(0xFF0D0F0E)),
        )
        Column(Modifier.weight(1f)) {
            SerifText(proj.title, 14.sp, maxLines = 1)
            BodyText("Ratio: ${proj.settings.cropRatio.id} · Zoom: ${"%.1f".format(proj.settings.zoom)}×", size = 11.sp, mono = true)
            BodyText(DateFormat.getDateInstance().format(Date(proj.updatedAt)), size = 11.sp, color = c.textTertiary)
        }
        InverseButton("Open", onOpen, minHeight = 36.dp)
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, c.border, RoundedCornerShape(10.dp)).clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Delete, "Delete project", tint = c.rose, modifier = Modifier.size(16.dp)) }
    }
}
