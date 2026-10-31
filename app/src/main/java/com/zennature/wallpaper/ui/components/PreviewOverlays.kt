package com.zennature.wallpaper.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zennature.wallpaper.data.GridOverlayType
import com.zennature.wallpaper.ui.theme.Jakarta
import com.zennature.wallpaper.ui.theme.Playfair

private val TextShadow = Shadow(Color(0x99000000), Offset(0f, 1f), 6f)

/** Simulated Android lock screen used in the detail sheet, discover hero and studio frame. */
@Composable
fun LockScreenOverlay(
    clock: ClockState,
    modifier: Modifier = Modifier,
    variant: LockVariant = LockVariant.DETAIL,
    timeSize: TextUnit = 60.sp,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to Color(0x73000000), 0.5f to Color.Transparent, 1f to Color(0xA6000000))),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                when (variant) {
                    LockVariant.DETAIL -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Lock, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            OverlayText("SWIPE UP TO UNLOCK", 11.sp, FontWeight.Medium, alpha = 0.85f)
                        }
                        OverlayText(clock.time, timeSize, FontWeight.ExtraLight)
                        OverlayText(clock.longDate, 12.sp, FontWeight.Medium, alpha = 0.9f)
                    }
                    LockVariant.STUDIO, LockVariant.HERO -> {
                        OverlayText(clock.shortDate.uppercase(), 11.sp, FontWeight.Medium, alpha = 0.85f)
                        OverlayText(clock.time, timeSize, FontWeight.ExtraLight)
                    }
                }
            }
            when (variant) {
                LockVariant.DETAIL -> Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    ShortcutBubble(Icons.Outlined.FlashlightOn)
                    ShortcutBubble(Icons.Outlined.CameraAlt)
                }
                LockVariant.STUDIO -> Box(
                    Modifier.padding(bottom = 12.dp).width(96.dp).height(4.dp).background(Color.White.copy(alpha = 0.4f), CircleShape),
                )
                LockVariant.HERO -> Column(Modifier.padding(bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "\"Find stillness in every frame.\"",
                        style = TextStyle(fontFamily = Playfair, fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, shadow = TextShadow),
                    )
                    Text(
                        "ZENITHAN STUDIO · TAP TO EDIT",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFFBBCBB8), letterSpacing = 1.5.sp),
                    )
                }
            }
        }
    }
}

enum class LockVariant { DETAIL, STUDIO, HERO }

@Composable
private fun OverlayText(text: String, size: TextUnit, weight: FontWeight, alpha: Float = 1f) {
    Text(
        text,
        style = TextStyle(
            fontFamily = Jakarta,
            fontSize = size,
            fontWeight = weight,
            color = Color.White.copy(alpha = alpha),
            shadow = TextShadow,
            letterSpacing = if (size.value < 14) 1.sp else (-1).sp,
        ),
    )
}

@Composable
private fun ShortcutBubble(icon: ImageVector) {
    Box(
        Modifier.size(40.dp).background(Color(0x66000000), CircleShape).border(1.dp, Color(0x33FFFFFF), CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
}

private data class SimApp(val label: String, val icon: ImageVector, val color: Color)

private val MAIN_APPS = listOf(
    SimApp("Photos", Icons.Outlined.Image, Color(0xCC10B981)),
    SimApp("Notes", Icons.Outlined.Chat, Color(0xCCF59E0B)),
    SimApp("Music", Icons.Outlined.MusicNote, Color(0xCC6366F1)),
    SimApp("Clock", Icons.Outlined.Schedule, Color(0xCC334155)),
    SimApp("Maps", Icons.Outlined.Explore, Color(0xCC14B8A6)),
    SimApp("Calendar", Icons.Outlined.CalendarMonth, Color(0xCCF43F5E)),
    SimApp("Weather", Icons.Outlined.WbCloudy, Color(0xCC0EA5E9)),
    SimApp("Files", Icons.Outlined.Folder, Color(0xCC2563EB)),
)

private val DOCK_APPS = listOf(
    SimApp("Phone", Icons.Outlined.Phone, Color(0xE6059669)),
    SimApp("Messages", Icons.Outlined.Chat, Color(0xE63B82F6)),
    SimApp("Camera", Icons.Outlined.CameraAlt, Color(0xE61E293B)),
    SimApp("Settings", Icons.Outlined.Settings, Color(0xE657534E)),
)

/** Simulated home screen: weather pill, 4x2 app grid, search pill, dock and gesture bar. */
@Composable
fun HomeScreenSimulation(modifier: Modifier = Modifier, compact: Boolean = false) {
    val iconBox = if (compact) 36.dp else 44.dp
    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.background(Color(0x66000000), CircleShape).border(1.dp, Color(0x1AFFFFFF), CircleShape).padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.WbCloudy, null, tint = Color(0xFFFCD34D), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text("21°C · Zen Calm", color = Color.White.copy(alpha = 0.95f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Text(
                "Mon, Oct 21",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.background(Color(0x4D000000), CircleShape).padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MAIN_APPS.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { app ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
                            AppIcon(app, iconBox)
                            Text(app.label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, style = TextStyle(shadow = TextShadow))
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth().height(38.dp).background(Color(0xBFFFFFFF), CircleShape).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("G", color = Color(0xFF0284C7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("Search...", color = Color(0xFF525252), fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Mic, null, tint = Color(0xFF737373), modifier = Modifier.size(14.dp))
                    Icon(Icons.Outlined.Search, null, tint = Color(0xFF737373), modifier = Modifier.size(14.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DOCK_APPS.forEach { AppIcon(it, iconBox) }
            }
            Box(Modifier.align(Alignment.CenterHorizontally).width(96.dp).height(4.dp).background(Color.White.copy(alpha = 0.6f), CircleShape))
        }
    }
}

@Composable
private fun AppIcon(app: SimApp, size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier.size(size).background(app.color, RoundedCornerShape(14.dp)).border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) { Icon(app.icon, null, tint = Color.White, modifier = Modifier.size(size * 0.45f)) }
}

enum class GridOpacity(val alpha: Float) { SUBTLE(0.35f), MEDIUM(0.6f), HIGH(0.85f) }

/** Composition guides drawn over the studio frame; never exported (same as web). */
@Composable
fun BoxScope.GridOverlay(type: GridOverlayType, opacity: GridOpacity, showGoldenSpiral: Boolean) {
    if (type == GridOverlayType.NONE) return
    val stroke = Color.White.copy(alpha = opacity.alpha)
    val shadow = Color.Black.copy(alpha = opacity.alpha * 0.9f)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 2.dp.toPx()))
        val line = 1.dp.toPx()
        fun guide(start: Offset, end: Offset) {
            drawLine(shadow, start + Offset(0.5f, 0.5f), end + Offset(0.5f, 0.5f), line, pathEffect = dash)
            drawLine(stroke, start, end, line, pathEffect = dash)
        }
        when (type) {
            GridOverlayType.RULE_OF_THIRDS -> {
                for (f in listOf(1f / 3, 2f / 3)) {
                    guide(Offset(w * f, 0f), Offset(w * f, h))
                    guide(Offset(0f, h * f), Offset(w, h * f))
                }
                for (fx in listOf(1f / 3, 2f / 3)) for (fy in listOf(1f / 3, 2f / 3)) {
                    val c = Offset(w * fx, h * fy)
                    drawCircle(stroke, 5.dp.toPx(), c, style = Stroke(line))
                    drawCircle(stroke, 1.6.dp.toPx(), c)
                }
            }
            GridOverlayType.GOLDEN_RATIO -> {
                for (f in listOf(0.382f, 0.618f)) {
                    guide(Offset(w * f, 0f), Offset(w * f, h))
                    guide(Offset(0f, h * f), Offset(w, h * f))
                }
                for (fx in listOf(0.382f, 0.618f)) for (fy in listOf(0.382f, 0.618f)) {
                    drawDiamond(Offset(w * fx, h * fy), 8.dp.toPx(), stroke, line)
                }
                if (showGoldenSpiral) drawGoldenSpiral(stroke, line, dash)
            }
            GridOverlayType.CENTER_CROSS -> {
                guide(Offset(w / 2, 0f), Offset(w / 2, h))
                guide(Offset(0f, h / 2), Offset(w, h / 2))
                drawCircle(stroke, 12.dp.toPx(), Offset(w / 2, h / 2), style = Stroke(line))
                drawCircle(stroke, 2.dp.toPx(), Offset(w / 2, h / 2))
            }
            GridOverlayType.NONE -> Unit
        }
    }
    val label = when (type) {
        GridOverlayType.RULE_OF_THIRDS -> "3×3 Rule of Thirds"
        GridOverlayType.GOLDEN_RATIO -> if (showGoldenSpiral) "Golden Ratio + Spiral (1:1.618)" else "Golden Ratio / Phi (1:1.618)"
        else -> "Center & Horizon Cross"
    }
    Row(
        Modifier.align(Alignment.BottomEnd).padding(10.dp)
            .background(Color(0x99000000), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).background(Color(0xFF8A9A86), CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

private fun DrawScope.drawDiamond(c: Offset, r: Float, color: Color, width: Float) {
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(c.x, c.y - r); lineTo(c.x + r, c.y); lineTo(c.x, c.y + r); lineTo(c.x - r, c.y); close()
    }
    drawPath(path, color, style = Stroke(width))
    drawCircle(color, 2.dp.toPx(), c)
}

/**
 * The web overlay drew the spiral as SVG arcs in a 1000x1000 box stretched to the frame. Reproduce those
 * same arc endpoints/radii so the curve sits in the same place.
 */
private fun DrawScope.drawGoldenSpiral(color: Color, width: Float, dash: PathEffect) {
    val sx = size.width / 1000f
    val sy = size.height / 1000f
    // Quarter arcs from the SVG path: (from, to, radius) in 1000-unit space, all sweep-flag=1.
    val arcs = listOf(
        Triple(Offset(1000f, 1000f), Offset(0f, 0f), 1000f),
        Triple(Offset(0f, 0f), Offset(618f, 618f), 618f),
        Triple(Offset(618f, 618f), Offset(236f, 236f), 382f),
        Triple(Offset(236f, 236f), Offset(472f, 472f), 236f),
        Triple(Offset(472f, 472f), Offset(326f, 326f), 146f),
        Triple(Offset(326f, 326f), Offset(416f, 416f), 90f),
    )
    val path = androidx.compose.ui.graphics.Path()
    path.moveTo(1000f * sx, 1000f * sy)
    for ((from, to, r) in arcs) {
        // SVG arc with large-arc=0, sweep=1: centre lies to the right of the from->to chord direction.
        val mx = (from.x + to.x) / 2
        val my = (from.y + to.y) / 2
        val dx = to.x - from.x
        val dy = to.y - from.y
        val chord = kotlin.math.sqrt(dx * dx + dy * dy)
        val rr = maxOf(r, chord / 2)
        val hdist = kotlin.math.sqrt((rr * rr - chord * chord / 4).coerceAtLeast(0f))
        val cx = mx - dy / chord * hdist
        val cy = my + dx / chord * hdist
        val start = Math.toDegrees(kotlin.math.atan2((from.y - cy).toDouble(), (from.x - cx).toDouble())).toFloat()
        var end = Math.toDegrees(kotlin.math.atan2((to.y - cy).toDouble(), (to.x - cx).toDouble())).toFloat()
        var sweep = end - start
        while (sweep <= 0) sweep += 360f
        if (sweep > 180f) sweep -= 360f
        val rect = androidx.compose.ui.geometry.Rect((cx - rr) * sx, (cy - rr) * sy, (cx + rr) * sx, (cy + rr) * sy)
        path.arcTo(rect, start, sweep, false)
    }
    drawPath(path, color, style = Stroke(width * 1.2f, pathEffect = dash))
}
