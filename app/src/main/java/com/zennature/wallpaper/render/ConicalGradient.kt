package com.zennature.wallpaper.render

import kotlin.math.roundToInt
import kotlin.math.sqrt

/** A colour stop with non-premultiplied 0..255 channels and 0..1 alpha. */
data class Stop(val offset: Float, val r: Int, val g: Int, val b: Int, val a: Float)

/**
 * Software two-point conical gradient with HTML canvas `createRadialGradient(x0,y0,r0,x1,y1,r1)` semantics,
 * including the transparent region outside the cone. Android only exposes this shader from API 31, and the
 * Komorebi sunbeam effect relies on that cone shape, so it is rasterised here (at reduced size; it is smooth).
 *
 * Coordinates are in the output pixel space of a [width] x [height] buffer.
 */
fun rasterizeConicalGradient(
    width: Int,
    height: Int,
    x0: Float, y0: Float, r0: Float,
    x1: Float, y1: Float, r1: Float,
    stops: List<Stop>,
): IntArray {
    val out = IntArray(width * height)
    val cdx = (x1 - x0).toDouble()
    val cdy = (y1 - y0).toDouble()
    val dr = (r1 - r0).toDouble()
    val a = cdx * cdx + cdy * cdy - dr * dr
    for (py in 0 until height) {
        for (px in 0 until width) {
            val pdx = px + 0.5 - x0
            val pdy = py + 0.5 - y0
            val b = pdx * cdx + pdy * cdy + r0 * dr
            val c = pdx * pdx + pdy * pdy - r0.toDouble() * r0
            val t: Double = if (kotlin.math.abs(a) < 1e-9) {
                if (kotlin.math.abs(b) < 1e-12) continue
                val tt = c / (2 * b)
                if (r0 + tt * dr < 0) continue
                tt
            } else {
                val disc = b * b - a * c
                if (disc < 0) continue
                val sq = sqrt(disc)
                val t1 = (b + sq) / a
                val t2 = (b - sq) / a
                val hi = maxOf(t1, t2)
                val lo = minOf(t1, t2)
                when {
                    r0 + hi * dr >= 0 -> hi
                    r0 + lo * dr >= 0 -> lo
                    else -> continue
                }
            }
            out[py * width + px] = colorAt(stops, t.toFloat())
        }
    }
    return out
}

internal fun colorAt(stops: List<Stop>, t: Float): Int {
    val first = stops.first()
    val last = stops.last()
    val s = when {
        t <= first.offset -> return pack(first)
        t >= last.offset -> return pack(last)
        else -> t
    }
    for (i in 0 until stops.size - 1) {
        val s0 = stops[i]
        val s1 = stops[i + 1]
        if (s >= s0.offset && s <= s1.offset) {
            val f = if (s1.offset == s0.offset) 0f else (s - s0.offset) / (s1.offset - s0.offset)
            return pack(
                Stop(
                    s,
                    lerp(s0.r, s1.r, f), lerp(s0.g, s1.g, f), lerp(s0.b, s1.b, f),
                    s0.a + (s1.a - s0.a) * f,
                ),
            )
        }
    }
    return pack(last)
}

private fun lerp(a: Int, b: Int, f: Float) = (a + (b - a) * f).roundToInt()

private fun pack(s: Stop): Int {
    val a = (s.a.coerceIn(0f, 1f) * 255).roundToInt()
    return (a shl 24) or (s.r.coerceIn(0, 255) shl 16) or (s.g.coerceIn(0, 255) shl 8) or s.b.coerceIn(0, 255)
}
