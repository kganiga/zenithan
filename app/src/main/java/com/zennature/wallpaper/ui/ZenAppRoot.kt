package com.zennature.wallpaper.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zennature.wallpaper.R
import com.zennature.wallpaper.appContainer
import com.zennature.wallpaper.data.ThemeMode
import com.zennature.wallpaper.ui.components.BodyText
import com.zennature.wallpaper.ui.components.SerifText
import com.zennature.wallpaper.ui.detail.DetailSheet
import com.zennature.wallpaper.ui.discover.DiscoverScreen
import com.zennature.wallpaper.ui.export.ExportDialog
import com.zennature.wallpaper.ui.feed.FeedScreen
import com.zennature.wallpaper.ui.feed.FeedViewModel
import com.zennature.wallpaper.ui.saved.SavedScreen
import com.zennature.wallpaper.ui.studio.EditorViewModel
import com.zennature.wallpaper.ui.studio.StudioScreen
import com.zennature.wallpaper.ui.theme.Zen

/** Photo picker shared by every "Import" button (no storage permission needed). */
@Composable
fun rememberPhotoPicker(main: MainViewModel): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) main.importPhoto(uri)
    }
    return remember(launcher) {
        { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    }
}

@Composable
fun ZenAppRoot(main: MainViewModel) {
    val context = LocalContext.current
    val container = remember { context.appContainer }
    val tab by main.tab.collectAsStateWithLifecycle()
    val favorites by main.store.favorites.collectAsStateWithLifecycle()
    val theme by main.store.theme.collectAsStateWithLifecycle()
    val detail by main.detail.collectAsStateWithLifecycle()
    val exportOpen by main.exportOpen.collectAsStateWithLifecycle()
    val studio by main.studio.collectAsStateWithLifecycle()

    val feed: FeedViewModel = viewModel { FeedViewModel(main.repository) }
    val editor: EditorViewModel = viewModel { EditorViewModel(container, main.studio.value) }
    LaunchedEffect(studio) { editor.load(studio) }

    val pickPhoto = rememberPhotoPicker(main)

    BackHandler(enabled = detail == null && !exportOpen && tab != Tab.HOME) { main.navigate(Tab.HOME) }

    BoxWithConstraints(Modifier.fillMaxSize().background(Zen.colors.background)) {
        // Window size classes: compact (<600dp, phones in portrait) gets a bottom bar; anything wider
        // (landscape phones, foldables, tablets) gets a side rail so vertical space goes to content.
        val compact = maxWidth < 600.dp
        val shortHeight = maxHeight < 480.dp
        val contentWidth = if (compact) maxWidth else maxWidth - RAIL_WIDTH
        val wide = contentWidth >= 700.dp
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))) {
            Header(
                width = maxWidth,
                short = shortHeight,
                dark = theme == ThemeMode.DARK,
                onHome = { main.navigate(Tab.HOME) },
                onToggleTheme = main::toggleTheme,
            )
            Row(Modifier.weight(1f)) {
                if (!compact) NavRail(tab, favorites.size, main::navigate)
                Box(Modifier.weight(1f).then(if (compact) Modifier else Modifier.navigationBarsPadding())) {
                    Crossfade(targetState = tab, label = "tab") { current ->
                        when (current) {
                            Tab.HOME -> FeedScreen(main, feed, pickPhoto, wide)
                            Tab.CURATED -> DiscoverScreen(main, pickPhoto, wide)
                            Tab.STUDIO -> StudioScreen(main, editor, pickPhoto, wide)
                            Tab.SAVED -> SavedScreen(main, wide)
                        }
                    }
                }
            }
            if (compact) BottomNav(tab, favorites.size, main::navigate)
        }

        detail?.let { item ->
            run {
                DetailSheet(
                    wallpaper = item,
                    isFavorited = favorites.any { it.id == item.id },
                    onToggleFavorite = { main.toggleFavorite(item) },
                    onEditInStudio = {
                        main.closeDetail()
                        main.editInStudio(item)
                    },
                    onClose = main::closeDetail,
                )
            }
        }

        if (exportOpen) {
            val editorState by editor.state.collectAsStateWithLifecycle()
            ExportDialog(
                wallpaper = editorState.wallpaper,
                settings = editorState.settings,
                primaryColor = editorState.palette?.dominant ?: "#556952",
                onClose = main::closeExport,
            )
        }
    }
}

/**
 * App bar. Navigation lives only in the bottom bar / side rail; the old header repeated Saved and Create
 * (Studio) buttons that overflowed 360dp phones and duplicated the tabs.
 */
@Composable
private fun Header(width: Dp, short: Boolean, dark: Boolean, onHome: () -> Unit, onToggleTheme: () -> Unit) {
    val c = Zen.colors
    Column(Modifier.fillMaxWidth().background(c.background.copy(alpha = 0.96f)).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(if (short) 48.dp else 60.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = onHome).padding(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painterResource(R.drawable.zen_logo),
                    contentDescription = "Zenithan Studio Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(if (short) 30.dp else 36.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, c.border, RoundedCornerShape(10.dp)),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f, fill = false)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SerifText("Zenithan", if (short) 18.sp else 20.sp, weight = FontWeight.Bold, maxLines = 1)
                        if (width >= 340.dp) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "STUDIO",
                                fontSize = 10.sp,
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = c.accent,
                                maxLines = 1,
                                modifier = Modifier.background(c.sage.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    if (width >= 900.dp && !short) BodyText("High-resolution wallpapers & creative studio", size = 12.sp, maxLines = 1)
                }
            }
            Row(
                Modifier.height(44.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onToggleTheme).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    contentDescription = if (dark) "Switch to Warm Mist white theme" else "Switch to Zen Charcoal dark theme",
                    tint = if (dark) Color(0xFFBBCBB8) else c.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
                if (width >= 900.dp) {
                    Spacer(Modifier.width(8.dp))
                    BodyText(if (dark) "Mist White" else "Charcoal", weight = FontWeight.Medium)
                }
            }
        }
        HorizontalDivider(color = c.border)
    }
}

private val RAIL_WIDTH = 84.dp

private data class NavItem(val tab: Tab, val label: String, val icon: ImageVector)

private val NAV_ITEMS = listOf(
    NavItem(Tab.HOME, "Wallpapers", Icons.Outlined.Image),
    NavItem(Tab.CURATED, "Discover", Icons.Outlined.WbSunny),
    NavItem(Tab.STUDIO, "Studio", Icons.Outlined.Tune),
    NavItem(Tab.SAVED, "Saved", Icons.Outlined.BookmarkBorder),
)

@Composable
private fun BottomNav(current: Tab, favoritesCount: Int, onNavigate: (Tab) -> Unit) {
    val c = Zen.colors
    Column(Modifier.fillMaxWidth().background(c.background.copy(alpha = 0.97f)).navigationBarsPadding()) {
        HorizontalDivider(color = c.border)
        Row(Modifier.fillMaxWidth().height(64.dp)) {
            NAV_ITEMS.forEach { item ->
                val active = current == item.tab
                Column(
                    Modifier.weight(1f).fillMaxSize().clickable { onNavigate(item.tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = if (active) c.accent else c.textTertiary,
                            modifier = Modifier.size(if (active) 22.dp else 20.dp),
                        )
                        if (item.tab == Tab.SAVED && favoritesCount > 0) {
                            Box(
                                Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-4).dp)
                                    .background(c.accentSoft, CircleShape).padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (favoritesCount > 99) "99+" else favoritesCount.toString(),
                                    color = if (c.isDark) Color(0xFF121413) else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Text(
                        item.label,
                        fontSize = 10.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (active) c.textPrimary else c.textTertiary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Box(Modifier.padding(top = 2.dp).size(4.dp).background(if (active) c.accentSoft else Color.Transparent, CircleShape))
                }
            }
        }
    }
}

/** Side navigation for landscape phones, foldables and tablets. */
@Composable
private fun NavRail(current: Tab, favoritesCount: Int, onNavigate: (Tab) -> Unit) {
    val c = Zen.colors
    Row(Modifier.fillMaxHeight()) {
        Column(
            Modifier.width(RAIL_WIDTH - 1.dp).fillMaxHeight().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NAV_ITEMS.forEach { item ->
                val active = current == item.tab
                Column(
                    Modifier.width(72.dp).clip(RoundedCornerShape(16.dp)).clickable { onNavigate(item.tab) }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.width(52.dp).height(30.dp).clip(CircleShape).background(if (active) c.sage.copy(alpha = 0.2f) else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(item.icon, item.label, tint = if (active) c.accent else c.textTertiary, modifier = Modifier.size(20.dp))
                        if (item.tab == Tab.SAVED && favoritesCount > 0) {
                            Box(Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 6.dp).background(c.accentSoft, CircleShape).padding(horizontal = 4.dp)) {
                                Text(
                                    if (favoritesCount > 99) "99+" else favoritesCount.toString(),
                                    color = if (c.isDark) Color(0xFF121413) else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Text(
                        item.label,
                        fontSize = 11.sp,
                        maxLines = 1,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (active) c.textPrimary else c.textTertiary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(c.border))
    }
}
