package com.zennature.wallpaper.render

import com.zennature.wallpaper.data.ColorPalette

/**
 * Port of colorExtractor.ts. Input is a 64x64 ARGB pixel array (the caller downsamples the image),
 * output is the same five-swatch palette the web build produced.
 */
fun extractPalette(argb64: IntArray): ColorPalette {
    data class Bin(val r: Int, val g: Int, val b: Int, var count: Int)

    val bins = LinkedHashMap<Int, Bin>()
    var i = 0
    while (i < argb64.size) {
        val p = argb64[i]
        val a = (p ushr 24) and 255
        if (a >= 128) {
            val qr = quantize((p shr 16) and 255)
            val qg = quantize((p shr 8) and 255)
            val qb = quantize(p and 255)
            val key = (qr shl 16) or (qg shl 8) or qb
            val bin = bins[key]
            if (bin != null) bin.count++ else bins[key] = Bin(qr, qg, qb, 1)
        }
        i += 4 // web build stepped 16 bytes = every 4th pixel
    }

    val colors = bins.values.sortedByDescending { it.count }
    if (colors.size < 5) return ColorPalette.Default

    val byLum = colors.sortedBy { 0.299 * it.r + 0.587 * it.g + 0.114 * it.b }
    val darkest = byLum.first()
    val lightest = byLum.last()
    val dominant = colors[0]
    val secondary = if (colors.size > 2) colors[2] else colors[1]
    val muted = byLum[byLum.size / 2]

    fun hex(b: Bin) = rgbToHex(b.r.toDouble(), b.g.toDouble(), b.b.toDouble())
    return ColorPalette(
        dominant = hex(dominant),
        secondary = hex(secondary),
        muted = hex(muted),
        light = hex(lightest),
        dark = hex(darkest),
    )
}

private fun quantize(v: Int): Int = (v / 24.0).roundJs() * 24
