package com.zennature.wallpaper.render

import com.zennature.wallpaper.data.MeshNode
import com.zennature.wallpaper.data.MeshPresetId
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

data class MeshPresetDefinition(
    val id: MeshPresetId,
    val name: String,
    val japaneseName: String,
    val subtitle: String,
    val description: String,
    val moodTag: String,
)

val MESH_PRESETS = listOf(
    MeshPresetDefinition(MeshPresetId.BIOPHILIC, "Biophilic Flora", "木漏れ日", "Moss, fern canopy & fertile earth",
        "Blends your image primary tone with forest moss greens, damp bark, and soft leaf canopy highlights.", "Calming · Organic"),
    MeshPresetDefinition(MeshPresetId.DAWN, "Dawn Mist", "朝霧", "Morning dew & luminous horizon",
        "Bathes your wallpaper in ethereal morning mist, gentle pearl highlights, and faint amber sunrise vapor.", "Fresh · Awakening"),
    MeshPresetDefinition(MeshPresetId.TWILIGHT, "Alpine Twilight", "黄昏", "Dusk indigo & mountain ridge glow",
        "Harmonizes with twilight shadows, evening violet, and a warm fading horizon dusk ember.", "Serene · Reflective"),
    MeshPresetDefinition(MeshPresetId.RIVER, "River Stone", "清流", "Wet slate, pebble & water ripples",
        "Grounds your wallpaper with smooth river stone gray, calm stream tones, and mineral reflections.", "Steady · Centering"),
    MeshPresetDefinition(MeshPresetId.GOLDEN, "Golden Hour", "秋麗", "Warm cedar & amber sunbeams",
        "Infuses radiant afternoon sun warmth, honey amber, and sunset copper derived from your wallpaper.", "Inviting · Radiant"),
    MeshPresetDefinition(MeshPresetId.ZEN, "Zen Monolith", "墨絵", "Deep sumi ink & muted jade",
        "Deep minimalist contemplation with rich charcoal ink undertones and subtle weathered stone jade.", "Deep · Minimalist"),
)

data class Rgb(val r: Int, val g: Int, val b: Int)
data class Hsl(val h: Int, val s: Int, val l: Int)

fun hexToRgb(hex: String): Rgb {
    var clean = hex.removePrefix("#").trim()
    if (clean.length == 3) clean = clean.map { "$it$it" }.joinToString("")
    val num = clean.toIntOrNull(16) ?: 0
    return Rgb((num shr 16) and 255, (num shr 8) and 255, num and 255)
}

fun rgbToHex(r: Double, g: Double, b: Double): String {
    fun part(n: Double) = n.roundJs().coerceIn(0, 255).toString(16).padStart(2, '0')
    return "#${part(r)}${part(g)}${part(b)}"
}

/** JavaScript Math.round semantics (half rounds towards +infinity) so colours match the web build bit-for-bit. */
internal fun Double.roundJs(): Int = floor(this + 0.5).toInt()

fun hexToHsl(hex: String): Hsl {
    val (r, g, b) = hexToRgb(hex)
    val rn = r / 255.0
    val gn = g / 255.0
    val bn = b / 255.0
    val mx = maxOf(rn, gn, bn)
    val mn = minOf(rn, gn, bn)
    var h = 0.0
    var s = 0.0
    val l = (mx + mn) / 2
    if (mx != mn) {
        val d = mx - mn
        s = if (l > 0.5) d / (2 - mx - mn) else d / (mx + mn)
        h = when (mx) {
            rn -> (gn - bn) / d + (if (gn < bn) 6 else 0)
            gn -> (bn - rn) / d + 2
            else -> (rn - gn) / d + 4
        }
        h = (h * 60).roundJs().toDouble()
    }
    return Hsl(h.toInt(), (s * 100).roundJs(), (l * 100).roundJs())
}

fun hslToHex(h: Double, s: Double, l: Double): String {
    val hn = ((h % 360) + 360) % 360
    val sn = s.coerceIn(0.0, 100.0) / 100
    val ln = l.coerceIn(0.0, 100.0) / 100
    val c = (1 - abs(2 * ln - 1)) * sn
    val x = c * (1 - abs((hn / 60) % 2 - 1))
    val m = ln - c / 2
    val (r, g, b) = when {
        hn < 60 -> Triple(c, x, 0.0)
        hn < 120 -> Triple(x, c, 0.0)
        hn < 180 -> Triple(0.0, c, x)
        hn < 240 -> Triple(0.0, x, c)
        hn < 300 -> Triple(x, 0.0, c)
        else -> Triple(c, 0.0, x)
    }
    return rgbToHex((r + m) * 255, (g + m) * 255, (b + m) * 255)
}

private fun hsl(h: Number, s: Number, l: Number) = hslToHex(h.toDouble(), s.toDouble(), l.toDouble())

private fun pseudoRandom(seed: Int, index: Int): Double {
    val x = sin(seed * 9973.0 + index * 1013.0) * 10000
    return x - floor(x)
}

/** Five harmonious colour stops derived from the wallpaper's primary colour (port of generateMeshColors). */
fun generateMeshColors(primaryHex: String, preset: MeshPresetId): List<String> {
    val (h, s0, l0) = hexToHsl(primaryHex)
    val s = s0.toDouble()
    val l = l0.toDouble()
    return when (preset) {
        MeshPresetId.BIOPHILIC -> listOf(
            primaryHex,
            hsl(115, min(48.0, max(25.0, s * 0.85)), min(48.0, max(30.0, l * 0.8))),
            hsl(88, min(55.0, max(30.0, s)), 72),
            hsl(32, min(40.0, s * 0.7), 24),
            hsl(135, 22, 68),
        )
        MeshPresetId.DAWN -> listOf(
            hsl(h, min(45.0, s * 0.7), max(50.0, min(75.0, l))),
            hsl((h + 20) % 360, min(40.0, s * 0.6), 84),
            hsl(38, min(65.0, s * 0.9), 78),
            hsl(170, 18, 92),
            hsl(22, 50, 75),
        )
        MeshPresetId.TWILIGHT -> listOf(
            hsl(h, min(55.0, s), max(25.0, min(45.0, l * 0.8))),
            hsl(255, min(50.0, s * 0.8), 28),
            hsl(18, min(65.0, s * 1.1), 48),
            hsl(195, 25, 20),
            hsl(230, 30, 42),
        )
        MeshPresetId.RIVER -> listOf(
            hsl(h, min(30.0, s * 0.6), max(35.0, min(55.0, l))),
            hsl(210, 18, 42),
            hsl(38, 28, 58),
            hsl(190, 24, 76),
            hsl(220, 15, 18),
        )
        MeshPresetId.GOLDEN -> listOf(
            hsl((h + 15) % 360, min(70.0, s * 1.1), max(45.0, l)),
            hsl(42, min(85.0, max(55.0, s * 1.2)), 58),
            hsl(25, 60, 44),
            hsl(12, 65, 48),
            hsl(48, 80, 78),
        )
        MeshPresetId.ZEN -> listOf(
            hsl(h, min(25.0, s * 0.5), max(20.0, min(45.0, l * 0.7))),
            hsl(h, 12, 14),
            hsl(155, 20, 32),
            hsl(210, 10, 24),
            hsl(60, 8, 70),
        )
    }
}

private data class BaseNode(val x: Int, val y: Int, val r: Int, val opacity: Float)

private val BASE_LAYOUT = listOf(
    BaseNode(18, 22, 85, 0.9f),
    BaseNode(82, 18, 90, 0.85f),
    BaseNode(50, 55, 105, 0.75f),
    BaseNode(15, 82, 95, 0.9f),
    BaseNode(85, 85, 80, 0.85f),
)

fun generateMeshNodes(primaryHex: String, preset: MeshPresetId, seed: Int = 42): List<MeshNode> {
    val colors = generateMeshColors(primaryHex, preset)
    return BASE_LAYOUT.mapIndexed { i, layout ->
        val shiftX = (pseudoRandom(seed, i * 2) - 0.5) * 24
        val shiftY = (pseudoRandom(seed, i * 2 + 1) - 0.5) * 24
        val radiusShift = (pseudoRandom(seed, i + 10) - 0.5) * 20
        MeshNode(
            x = (layout.x + shiftX).roundJs().coerceIn(5, 95),
            y = (layout.y + shiftY).roundJs().coerceIn(5, 95),
            radius = (layout.r + radiusShift).roundJs().coerceIn(50, 130),
            color = colors[i % colors.size],
            opacity = layout.opacity,
        )
    }
}

fun hexToArgb(hex: String, alpha: Float = 1f): Int {
    val (r, g, b) = hexToRgb(hex)
    val a = (alpha.coerceIn(0f, 1f) * 255).roundToInt()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}
