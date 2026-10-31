package com.zennature.wallpaper.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.zennature.wallpaper.ui.theme.Playfair
import com.zennature.wallpaper.ui.theme.Zen
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ----------------------------------------------------------------------------- text

@Composable
fun SerifText(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = Zen.colors.textPrimary,
    weight: FontWeight = FontWeight.SemiBold,
    italic: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    lineHeight: TextUnit = TextUnit.Unspecified,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            fontFamily = Playfair,
            fontSize = size,
            fontWeight = weight,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            lineHeight = lineHeight,
        ),
    )
}

@Composable
fun BodyText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 12.sp,
    color: Color = Zen.colors.textSecondary,
    weight: FontWeight = FontWeight.Normal,
    maxLines: Int = Int.MAX_VALUE,
    mono: Boolean = false,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size,
        fontWeight = weight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        fontFamily = if (mono) FontFamily.Monospace else null,
        letterSpacing = letterSpacing,
        lineHeight = lineHeight,
    )
}

// ----------------------------------------------------------------------------- buttons

/** Filled dark button (bg-[#2C2B29] / dark:bg-[#E2E3E0]). */
@Composable
fun InverseButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    enabled: Boolean = true,
    minHeight: Dp = 44.dp,
) {
    val c = Zen.colors
    ZenButton(text, onClick, modifier, c.inverseBg, c.inverseText, null, icon, iconTint, enabled, minHeight)
}

/** Forest green call to action (bg-[#556952]). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    minHeight: Dp = 46.dp,
) {
    ZenButton(text, onClick, modifier, Zen.colors.forest, Color.White, null, icon, null, enabled, minHeight)
}

@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    enabled: Boolean = true,
    minHeight: Dp = 44.dp,
    borderColor: Color = Zen.colors.borderStrong,
    background: Color = Color.Transparent,
    contentColor: Color = Zen.colors.textPrimary,
) {
    ZenButton(text, onClick, modifier, background, contentColor, BorderStroke(1.dp, borderColor), icon, iconTint, enabled, minHeight)
}

@Composable
fun ZenButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    background: Color,
    content: Color,
    border: BorderStroke?,
    icon: ImageVector?,
    iconTint: Color?,
    enabled: Boolean,
    minHeight: Dp,
    shape: Shape = RoundedCornerShape(12.dp),
    fontSize: TextUnit = 12.sp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .clip(shape)
            .background(background)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.5f)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = iconTint ?: content, modifier = Modifier.size(16.dp))
        if (text.isNotEmpty()) Text(text, color = content, fontSize = fontSize, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun IconSquareButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Zen.colors.textSecondary,
    background: Color = Color.Transparent,
    bordered: Boolean = true,
    size: Dp = 40.dp,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(background)
            .then(if (bordered) Modifier.border(1.dp, Zen.colors.border, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.3f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(18.dp))
    }
}

/** Selectable option chip used across filters and editor panels (selected = inverse fill). */
@Composable
fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 11.sp,
) {
    val c = Zen.colors
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) c.inverseBg else Color.Transparent)
            .border(1.dp, if (selected) Color.Transparent else c.border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) c.inverseText else c.textSecondary,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SegmentedRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), content = content)
}

// ----------------------------------------------------------------------------- containers

@Composable
fun ZenCard(
    modifier: Modifier = Modifier,
    background: Color = Zen.colors.surface,
    border: Color = Zen.colors.border,
    radius: Dp = 16.dp,
    padding: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

/** Labelled slider with a monospace value read-out (the web range inputs). */
@Composable
fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    onChange: (Float) -> Unit,
    onChangeFinished: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = Zen.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BodyText(label, color = c.textPrimary, weight = FontWeight.Medium, modifier = Modifier.weight(1f))
            BodyText(valueText, mono = true)
        }
        val steps = (((range.endInclusive - range.start) / step).toInt() - 1).coerceAtLeast(0)
        Slider(
            value = value.coerceIn(range),
            onValueChange = onChange,
            onValueChangeFinished = onChangeFinished,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = c.sage,
                activeTrackColor = c.sage,
                inactiveTrackColor = c.border,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
    }
}

// ----------------------------------------------------------------------------- images

@Composable
fun WallpaperImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    AsyncImage(
        model = remember(url) { ImageRequest.Builder(context).data(url).build() },
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
}

// ----------------------------------------------------------------------------- time

data class ClockState(val time: String, val shortDate: String, val longDate: String)

/** Live 24h clock that ticks once per second, used by every lock-screen preview. */
@Composable
fun rememberClock(): ClockState {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1000L - (System.currentTimeMillis() % 1000L))
        }
    }
    val locale = Locale.getDefault()
    return ClockState(
        time = SimpleDateFormat("HH:mm", locale).format(now),
        shortDate = SimpleDateFormat("EEE, MMM d", locale).format(now),
        longDate = SimpleDateFormat("EEEE, MMM d", locale).format(now),
    )
}

/** Small transient banner state (the web build's setTimeout toasts). */
@Composable
fun rememberFlag(durationMs: Long, trigger: Int): Boolean {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            visible = true
            delay(durationMs)
            visible = false
        }
    }
    return visible
}
