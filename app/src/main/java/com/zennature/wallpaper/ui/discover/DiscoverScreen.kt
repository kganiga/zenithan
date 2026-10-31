package com.zennature.wallpaper.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zennature.wallpaper.R
import com.zennature.wallpaper.data.CURATED_CATEGORIES
import com.zennature.wallpaper.data.CategoryIcon
import com.zennature.wallpaper.data.circadianSlot
import com.zennature.wallpaper.render.hexToArgb
import com.zennature.wallpaper.ui.MainViewModel
import com.zennature.wallpaper.ui.components.BodyText
import com.zennature.wallpaper.ui.components.InverseButton
import com.zennature.wallpaper.ui.components.LockScreenOverlay
import com.zennature.wallpaper.ui.components.LockVariant
import com.zennature.wallpaper.ui.components.OutlineButton
import com.zennature.wallpaper.ui.components.PrimaryButton
import com.zennature.wallpaper.ui.components.SerifText
import com.zennature.wallpaper.ui.components.WallpaperImage
import com.zennature.wallpaper.ui.components.rememberClock
import com.zennature.wallpaper.ui.feed.WallpaperTile
import com.zennature.wallpaper.ui.theme.Zen

fun CategoryIcon.vector(): ImageVector = when (this) {
    CategoryIcon.MOUNTAIN -> Icons.Outlined.Landscape
    CategoryIcon.TREES -> Icons.Outlined.Forest
    CategoryIcon.TREE_PINE -> Icons.Outlined.Park
    CategoryIcon.WAVES -> Icons.Outlined.Waves
    CategoryIcon.COMPASS -> Icons.Outlined.Explore
    CategoryIcon.SUN -> Icons.Outlined.WbSunny
    CategoryIcon.CLOUD_RAIN -> Icons.Outlined.WaterDrop
    CategoryIcon.FEATHER -> Icons.Outlined.Spa
    CategoryIcon.SUNSET -> Icons.Outlined.WbTwilight
    CategoryIcon.MOON -> Icons.Outlined.Bedtime
}

/** Discover tab (HomeScreen.tsx): hero, Zen Moments, circadian pick, curated moods, recents, footer. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverScreen(main: MainViewModel, pickPhoto: () -> Unit, wide: Boolean) {
    val c = Zen.colors
    val featured = main.featured
    val recents by main.store.recents.collectAsStateWithLifecycle()
    val favorites by main.store.favorites.collectAsStateWithLifecycle()
    val favoriteIds = remember(favorites) { favorites.mapTo(HashSet()) { it.id } }
    val slot = remember { circadianSlot() }
    var breathing by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        // ---------------------------------------------------------------- hero
        Section(c.surfaceMuted.copy(alpha = 0.6f), c.border) {
            val heroText: @Composable (Modifier) -> Unit = { mod ->
                Column(mod, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(
                        Modifier.clip(CircleShape).background(c.sage.copy(alpha = 0.15f))
                            .border(1.dp, c.sage.copy(alpha = 0.25f), CircleShape).padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(6.dp).background(c.sage, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        BodyText("Today’s Contemplative Discovery", color = c.accent, weight = FontWeight.Medium)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SerifText("Find stillness in every frame.", if (wide) 44.sp else 30.sp, lineHeight = if (wide) 50.sp else 35.sp)
                        BodyText(
                            "Discover serene public domain and verified high-resolution photography. Customize with elegant typography, clock-safe ratios, and clean aesthetic washes.",
                            size = 14.sp, lineHeight = 21.sp,
                        )
                    }
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface.copy(alpha = 0.7f))
                            .border(1.dp, c.border, RoundedCornerShape(12.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BodyText(featured.title, color = c.textPrimary, weight = FontWeight.Medium, maxLines = 1, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            BodyText(featured.license.code, mono = true, size = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BodyText("By ${featured.license.author ?: "Open Archive"}", maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                            BodyText("  ·  ")
                            BodyText(featured.provider.name, size = 10.sp, color = c.accent, weight = FontWeight.SemiBold, letterSpacing = 1.sp)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton("Set as Wallpaper", { main.openDetail(featured) }, icon = Icons.Outlined.Smartphone, minHeight = 48.dp)
                        OutlineButton("Studio", { main.editInStudio(featured) }, icon = Icons.Outlined.AutoAwesome, iconTint = c.sage, borderColor = c.textPrimary, minHeight = 48.dp)
                        OutlineButton(
                            "Zen Moments (60s)", { breathing = true }, icon = Icons.Outlined.Air, iconTint = c.forest,
                            borderColor = c.sage, background = c.sage.copy(alpha = 0.1f), minHeight = 48.dp,
                        )
                        OutlineButton("Import Photo", pickPhoto, icon = Icons.Outlined.UploadFile, iconTint = c.accent, minHeight = 48.dp)
                    }
                }
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
                    heroText(Modifier.weight(7f))
                    Box(Modifier.weight(5f), contentAlignment = Alignment.Center) { PhoneFrame(featured.fullUrl.ifEmpty { featured.thumbnailUrl }) { main.openDetail(featured) } }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
                    heroText(Modifier.fillMaxWidth())
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PhoneFrame(featured.fullUrl.ifEmpty { featured.thumbnailUrl }) { main.openDetail(featured) } }
                }
            }
        }

        // ---------------------------------------------------------------- zen moments
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(
                    if (c.isDark) Brush.horizontalGradient(listOf(Color(0xFF252E27), Color(0xFF1D221F), Color(0xFF222923)))
                    else Brush.horizontalGradient(listOf(c.sage.copy(alpha = 0.15f), Color.White.copy(alpha = 0.7f), c.sage.copy(alpha = 0.1f))),
                )
                .border(1.dp, if (c.isDark) Color(0xFF3D4D3E) else c.sage.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(c.forest), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Air, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        BodyText("MINDFULNESS EXERCISE · 60 SECONDS", size = 11.sp, mono = true, color = c.accent, letterSpacing = 1.sp)
                        SerifText("Zen Moments Breathing Guide", 21.sp)
                        BodyText("Center your thoughts with 60 seconds of gentle guided breathwork and soft pulsating rhythms before entering the wallpaper studio.")
                    }
                }
                PrimaryButton("Begin 60s Breathwork", { breathing = true }, Modifier.fillMaxWidth(), icon = Icons.Outlined.Air)
            }
        }

        // ---------------------------------------------------------------- circadian
        Section(c.surfaceMuted.copy(alpha = 0.8f), c.border) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(Color(hexToArgb(slot.accentColor)), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        BodyText("ATMOSPHERIC STILLNESS · ${slot.hourRange}", size = 11.sp, mono = true, letterSpacing = 1.sp)
                    }
                    SerifText(slot.japaneseTitle, 21.sp)
                    BodyText(slot.moodDescription)
                }
                InverseButton("Immerse in ${slot.label}", { main.selectCategoryFromCurated(slot.recommendedCategory) }, icon = Icons.AutoMirrored.Outlined.ArrowForward)
            }
        }

        // ---------------------------------------------------------------- categories
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    SerifText("Curated Moods & Aesthetics", 24.sp)
                    BodyText("Select a calm natural aesthetic to discover peaceful public domain photography")
                }
                Row(Modifier.clickable(onClick = main::browseAll).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    BodyText("Browse Wallpapers", color = c.accent, weight = FontWeight.Medium)
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = c.accent, modifier = Modifier.size(14.dp))
                }
            }
            BoxWithConstraints {
                val cols = when {
                    maxWidth >= 1024.dp -> 5
                    maxWidth >= 600.dp -> 3
                    else -> 2
                }
                GridRows(CURATED_CATEGORIES, cols, 12.dp) { cat ->
                    Column(
                        Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).background(c.surface)
                            .border(1.dp, c.border, RoundedCornerShape(12.dp)).clickable { main.selectCategoryFromCurated(cat.id) }.padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(c.surfaceMuted), contentAlignment = Alignment.Center) {
                            Icon(cat.icon.vector(), null, tint = c.accent, modifier = Modifier.size(16.dp))
                        }
                        Column {
                            BodyText(cat.label, size = 13.sp, color = c.textPrimary, weight = FontWeight.Medium, maxLines = 1)
                            BodyText(cat.subtitle, size = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------- recents
        if (recents.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    SerifText("Recently Viewed Stillness", 24.sp)
                    BodyText("Quickly return to wallpapers you’ve inspected or edited")
                }
                BoxWithConstraints {
                    val cols = when {
                        maxWidth >= 1024.dp -> 6
                        maxWidth >= 768.dp -> 4
                        maxWidth >= 600.dp -> 3
                        else -> 2
                    }
                    GridRows(recents.take(6), cols, 12.dp) { item ->
                        WallpaperTile(
                            item = item,
                            favorited = item.id in favoriteIds,
                            onOpen = { main.openDetail(item) },
                            onStudio = { main.editInStudio(item) },
                            onFavorite = { main.toggleFavorite(item) },
                            badge = false,
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------------- footer
        Section(c.surfaceMuted.copy(alpha = 0.4f), c.border) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                androidx.compose.foundation.Image(
                    painterResource(R.drawable.zen_logo), "Zenithan Studio Logo",
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, c.border, RoundedCornerShape(12.dp)),
                )
                SerifText("\"Nothing missing, nothing to add. Nature finishes without hurrying.\"", 14.sp, italic = true, weight = FontWeight.Normal)
                BodyText("Zenithan Studio · 100% on-device · Free & verified open-licensed photography.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlineButton("Lush Moss", { main.selectCategoryFromCurated("japanese-gardens") }, minHeight = 36.dp)
                    OutlineButton("Mist Peaks", { main.selectCategoryFromCurated("misty-mountains") }, minHeight = 36.dp)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (breathing) {
        BreathingTimerDialog(
            onClose = { breathing = false },
            onStartBuilding = {
                breathing = false
                main.editInStudio(featured)
            },
        )
    }
}

@Composable
private fun Section(background: Color, border: Color, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(Modifier.fillMaxWidth().clip(shape).background(background).border(1.dp, border, shape).padding(24.dp)) { content() }
}

/** Phone mock with a live lock screen, tapping it opens the detail sheet. */
@Composable
private fun PhoneFrame(url: String, onClick: () -> Unit) {
    val clock = rememberClock()
    Box(
        Modifier.width(250.dp).aspectRatio(9f / 19f).clip(RoundedCornerShape(36.dp))
            .background(if (Zen.colors.isDark) Color(0xFF333534) else Color(0xFF2C2B29)).clickable(onClick = onClick).padding(10.dp),
    ) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)).background(Color.Black)) {
            WallpaperImage(url, null, Modifier.fillMaxSize())
            LockScreenOverlay(clock, variant = LockVariant.HERO, timeSize = 48.sp)
        }
        Box(
            Modifier.align(Alignment.TopCenter).padding(top = 6.dp).width(64.dp).height(14.dp).background(Color(0x99000000), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Box(Modifier.size(10.dp).background(Color(0xCC000000), CircleShape)) }
    }
}

/** Non-lazy grid for short, fixed lists inside a scrolling column. */
@Composable
fun <T> GridRows(items: List<T>, columns: Int, gap: androidx.compose.ui.unit.Dp, cell: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        items.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { Box(Modifier.weight(1f)) { cell(it) } }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
