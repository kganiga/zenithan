package com.zennature.wallpaper.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zennature.wallpaper.data.CURATED_CATEGORIES
import com.zennature.wallpaper.data.LicenseFilter
import com.zennature.wallpaper.data.OrientationFilter
import com.zennature.wallpaper.data.ProviderFilter
import com.zennature.wallpaper.data.WallpaperItem
import com.zennature.wallpaper.ui.MainViewModel
import com.zennature.wallpaper.ui.components.BodyText
import com.zennature.wallpaper.ui.components.ChoiceChip
import com.zennature.wallpaper.ui.components.InverseButton
import com.zennature.wallpaper.ui.components.SerifText
import com.zennature.wallpaper.ui.components.WallpaperImage
import com.zennature.wallpaper.ui.components.rememberFlag
import com.zennature.wallpaper.ui.theme.Zen
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Composable
fun FeedScreen(main: MainViewModel, vm: FeedViewModel, pickPhoto: () -> Unit, wide: Boolean) {
    val state by vm.state.collectAsStateWithLifecycle()
    val favorites by main.store.favorites.collectAsStateWithLifecycle()
    val favoriteIds = remember(favorites) { favorites.mapTo(HashSet()) { it.id } }
    val categoryRequest by main.feedCategory.collectAsStateWithLifecycle()
    val scrollRequest by main.scrollToTop.collectAsStateWithLifecycle()
    val grid = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(categoryRequest) {
        categoryRequest?.let { vm.setCategory(it.categoryId) }
    }
    LaunchedEffect(scrollRequest, state.shuffleToken) { grid.animateScrollToItem(0) }

    // Infinite scroll: prefetch when the last visible cell is within ~2 rows of the end (≈ the web's 500px margin).
    LaunchedEffect(grid) {
        snapshotFlow {
            val info = grid.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= info.totalItemsCount - 8
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { vm.loadMore() }
    }

    val showScrollTop by remember { derivedStateOf { grid.firstVisibleItemIndex > 6 } }
    val toast = rememberFlag(2000, state.shuffleToken)
    BoxContent {
        val columns = gridColumns(wide, state.density)
        val gap: Dp = if (state.density == GridDensity.STANDARD) 12.dp else 10.dp
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = grid,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "controls") {
                Controls(state, vm, main, pickPhoto, toast)
            }
            when {
                state.loadingInitial -> items(16, key = { "skeleton-$it" }) {
                    Box(Modifier.aspectRatio(9f / 16f).clip(RoundedCornerShape(16.dp)).background(Zen.colors.surfaceMuted))
                }
                state.items.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                    EmptyState(onReset = vm::resetAndShuffle)
                }
                else -> {
                    items(state.items, key = { it.id + "|" + it.thumbnailUrl }) { item ->
                        WallpaperTile(
                            item = item,
                            favorited = item.id in favoriteIds,
                            onOpen = { main.openDetail(item) },
                            onStudio = { main.editInStudio(item) },
                            onFavorite = { main.toggleFavorite(item) },
                        )
                    }
                    item(span = { GridItemSpan(maxLineSpan) }, key = "footer") {
                        FeedFooter(state.loadingMore, state.hasMore)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showScrollTop,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Zen.colors.inverseBg)
                    .clickable { scope.launch { grid.animateScrollToItem(0) } },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.ArrowUpward, "Scroll to top", tint = Zen.colors.inverseText, modifier = Modifier.size(16.dp)) }
        }
    }
}

@Composable
private fun BoxContent(content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) =
    Box(Modifier.fillMaxSize(), content = content)

/** Mirrors the Tailwind breakpoints: standard 2/3/4, dense 2/3/4/5/6 columns. */
@Composable
private fun gridColumns(wide: Boolean, density: GridDensity): Int {
    val width = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    return when (density) {
        GridDensity.STANDARD -> when {
            width < 640 -> 2
            width < 1024 -> 3
            else -> 4
        }
        GridDensity.DENSE -> when {
            width < 640 -> 2
            width < 768 -> 3
            width < 1024 -> 4
            width < 1280 -> 5
            else -> 6
        }
    }.let { if (wide) maxOf(it, 3) else it }
}

@Composable
private fun Controls(state: FeedState, vm: FeedViewModel, main: MainViewModel, pickPhoto: () -> Unit, toast: Boolean) {
    val c = Zen.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 8.dp)) {
        // Search box
        val shape = RoundedCornerShape(12.dp)
        Row(
            Modifier.fillMaxWidth().height(44.dp).clip(shape).background(c.surface).border(1.dp, c.border, shape).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (state.searchInput.isEmpty()) {
                    BodyText("Search 100+ wallpapers: mountains, AMOLED dark, ocean, neon...", size = 14.sp, color = c.textTertiary, maxLines = 1)
                }
                BasicTextField(
                    value = state.searchInput,
                    onValueChange = vm::onSearchInput,
                    singleLine = true,
                    textStyle = TextStyle(color = c.textPrimary, fontSize = 14.sp),
                    cursorBrush = SolidColor(c.sage),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.searchInput.isNotEmpty()) {
                Icon(Icons.Outlined.Close, "Clear search", tint = c.textSecondary, modifier = Modifier.size(18.dp).clickable(onClick = vm::clearSearch))
            }
        }

        // Quick actions
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            InverseButton("Shuffle", vm::shuffle, icon = Icons.Outlined.Shuffle, iconTint = c.accentSoft, minHeight = 42.dp)
            ActionBox(
                icon = if (state.density == GridDensity.STANDARD) Icons.Outlined.ViewModule else Icons.Outlined.GridView,
                description = if (state.density == GridDensity.STANDARD) "Switch to compact view" else "Switch to standard view",
                onClick = vm::toggleDensity,
            )
            ActionBox(Icons.Outlined.UploadFile, "Import your personal wallpaper or photo", pickPhoto, tint = c.accent)
            ActionBox(
                Icons.Outlined.Tune, "Filters", vm::toggleFiltersPanel,
                highlighted = state.filtersActive, dot = state.filtersActive,
            )
            ActionBox(Icons.Outlined.Refresh, "Refresh wallpapers", vm::refresh, spinning = state.loadingInitial)
        }

        // Category ribbon
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Pill(
                    "Discover Moods", selected = false, outlined = true, icon = Icons.Outlined.AutoAwesome,
                    onClick = { main.navigate(com.zennature.wallpaper.ui.Tab.CURATED) },
                )
            }
            item { Pill("All Wallpapers", selected = state.filters.category.isEmpty(), onClick = { vm.setCategory("") }, strong = true) }
            items(CURATED_CATEGORIES, key = { it.id }) { cat ->
                Pill(cat.label, selected = state.filters.category == cat.id, sage = true, onClick = { vm.toggleCategory(cat.id) })
            }
        }

        // Provider selector
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val providers = listOf(
                ProviderFilter.ALL to "All Archives",
                ProviderFilter.LOCAL to "Curated 4K",
                ProviderFilter.WIKIMEDIA to "Wikimedia",
                ProviderFilter.NASA to "NASA Space",
            )
            items(providers) { (id, label) ->
                val selected = state.filters.provider == id
                Text(
                    label,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) c.accent else c.textSecondary,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .background(if (selected) c.sage.copy(alpha = 0.1f) else Color.Transparent)
                        .border(1.dp, if (selected) c.sage else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable { vm.setProvider(id) }
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }

        AnimatedVisibility(toast) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.sage.copy(alpha = 0.15f))
                    .border(1.dp, c.sage.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Shuffle, null, tint = c.accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                BodyText("Wallpapers reshuffled! New discover stream loaded.", color = c.accent, weight = FontWeight.Medium)
            }
        }

        AnimatedVisibility(state.showFilters) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface)
                    .border(1.dp, c.border, RoundedCornerShape(12.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FilterGroup(
                    "Image Orientation",
                    listOf(
                        OrientationFilter.ALL to "Any", OrientationFilter.PORTRAIT to "Phone Portrait",
                        OrientationFilter.LANDSCAPE to "Landscape", OrientationFilter.SQUARE to "Square",
                    ),
                    state.filters.orientation, vm::setOrientation,
                )
                FilterGroup(
                    "License & Copyright",
                    listOf(
                        LicenseFilter.ALL to "All Verified", LicenseFilter.CC0_PD to "Public Domain / CC0",
                        LicenseFilter.COMMERCIAL to "Commercial Friendly",
                    ),
                    state.filters.licenseOnly, vm::setLicense,
                )
            }
        }
    }
}

@Composable
private fun <T> FilterGroup(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BodyText(title, color = Zen.colors.textPrimary, weight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (id, label) ->
                ChoiceChip(label, selected == id, { onSelect(id) }, Modifier.weight(1f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ActionBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color = Zen.colors.textSecondary,
    highlighted: Boolean = false,
    dot: Boolean = false,
    spinning: Boolean = false,
) {
    val c = Zen.colors
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.size(42.dp).clip(shape)
            .background(if (highlighted) c.sage.copy(alpha = 0.2f) else c.surface)
            .border(1.dp, if (highlighted) c.sage else c.border, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (spinning) {
            CircularProgressIndicator(Modifier.size(16.dp), color = c.textSecondary, strokeWidth = 2.dp)
        } else {
            Icon(icon, description, tint = tint, modifier = Modifier.size(18.dp))
        }
        if (dot) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(6.dp).background(c.sage, CircleShape))
    }
}

@Composable
private fun Pill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    outlined: Boolean = false,
    strong: Boolean = false,
    sage: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val c = Zen.colors
    val bg = when {
        outlined -> Color.Transparent
        selected && sage -> c.sage
        selected -> c.inverseBg
        else -> c.surfaceMuted
    }
    val fg = when {
        outlined -> c.accent
        selected && sage -> Color.White
        selected -> c.inverseText
        else -> c.textSecondary
    }
    Row(
        Modifier.clip(CircleShape).background(bg)
            .then(if (outlined) Modifier.border(1.dp, c.sage.copy(alpha = 0.4f), CircleShape) else Modifier)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, color = fg, fontSize = 11.sp, fontWeight = if (selected || strong) FontWeight.SemiBold else FontWeight.Medium)
    }
}

/**
 * Grid tile. The web card hid title/actions behind a hover state that touch screens never trigger reliably;
 * here the title strip and Studio shortcut are always visible, tap opens Preview & Apply.
 */
@Composable
fun WallpaperTile(
    item: WallpaperItem,
    favorited: Boolean,
    onOpen: () -> Unit,
    onStudio: () -> Unit,
    onFavorite: () -> Unit,
    badge: Boolean = true,
) {
    val c = Zen.colors
    val shape = RoundedCornerShape(16.dp)
    val isDark = item.category == "moon-night-sky" || item.title.lowercase().let { it.contains("dark") || it.contains("obsidian") }
    Box(
        Modifier.aspectRatio(9f / 16f).clip(shape).background(Color(0xFF0D0F0E)).border(1.dp, c.border, shape).clickable(onClick = onOpen),
    ) {
        WallpaperImage(item.thumbnailUrl, item.title, Modifier.fillMaxSize())
        Row(
            Modifier.fillMaxWidth().padding(10.dp).align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (badge) {
                Text(
                    if (isDark) "AMOLED" else "4K HD",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.background(Color(0x99000000), CircleShape).padding(horizontal = 8.dp, vertical = 2.dp),
                )
            } else {
                Spacer(Modifier.width(1.dp))
            }
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(Color(0x66000000)).clickable(onClick = onFavorite),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (favorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    "Save to Favorites",
                    tint = if (favorited) c.rose else Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                .padding(start = 10.dp, end = 6.dp, top = 18.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                SerifText(item.title, 12.sp, color = Color.White, maxLines = 1)
                BodyText("${item.license.author ?: "Open Creator"} · ${item.license.code}", size = 10.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
            }
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(Color(0x33FFFFFF)).clickable(onClick = onStudio),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.AutoAwesome, "Customize in Studio", tint = Color.White, modifier = Modifier.size(14.dp)) }
        }
    }
}

@Composable
private fun EmptyState(onReset: () -> Unit) {
    val c = Zen.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, c.borderStrong, RoundedCornerShape(16.dp)).padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(48.dp).background(c.surfaceMuted, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Explore, null, tint = c.textSecondary)
        }
        SerifText("No Wallpapers in Current Stream", 18.sp, weight = FontWeight.Medium)
        CenteredBody("No matching imagery found. Try clearing your search keywords or tapping Shuffle to see a fresh selection.")
        InverseButton("Reset Filters & Shuffle", onReset, minHeight = 36.dp)
    }
}

@Composable
private fun CenteredBody(text: String) {
    Text(text, color = Zen.colors.textSecondary, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
}

@Composable
private fun FeedFooter(loadingMore: Boolean, hasMore: Boolean) {
    val c = Zen.colors
    Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
        when {
            loadingMore -> Row(
                Modifier.clip(CircleShape).background(c.surface).border(1.dp, c.border, CircleShape).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(16.dp), color = c.sage, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                BodyText("Loading more stunning wallpapers...", weight = FontWeight.Medium)
            }
            hasMore -> BodyText("Scroll down for continuous discovery", size = 11.sp, mono = true, color = c.textSecondary.copy(alpha = 0.7f))
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SerifText("\"Endless horizon.\"", 14.sp, color = c.textSecondary, weight = FontWeight.Normal, italic = true)
                BodyText("You have explored hundreds of wallpapers. Tap Shuffle above for a new stream!", size = 11.sp)
            }
        }
    }
}
