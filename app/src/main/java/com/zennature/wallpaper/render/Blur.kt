package com.zennature.wallpaper.render

import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Gaussian blur approximated with three box blurs, applied to premultiplied channels so transparent
 * edges fade the same way an HTML canvas `filter: blur()` does. Pixels outside the image count as transparent.
 */
object Blur {

    fun gaussian(pixels: IntArray, width: Int, height: Int, sigma: Float) {
        if (sigma < 0.3f || width < 2 || height < 2) return
        val boxes = boxesForGauss(sigma, 3)
        val alpha = IntArray(pixels.size)
        val channel = IntArray(pixels.size)
        val scratch = IntArray(pixels.size)

        for (i in pixels.indices) alpha[i] = (pixels[i] ushr 24) and 255
        val blurredAlpha = alpha.copyOf()
        runBoxes(blurredAlpha, scratch, width, height, boxes)

        val out = IntArray(pixels.size)
        for (shift in intArrayOf(16, 8, 0)) {
            for (i in pixels.indices) channel[i] = ((pixels[i] shr shift) and 255) * alpha[i] / 255
            runBoxes(channel, scratch, width, height, boxes)
            for (i in pixels.indices) {
                val a = blurredAlpha[i]
                val v = if (a == 0) 0 else (channel[i] * 255 / a).coerceIn(0, 255)
                out[i] = out[i] or (v shl shift)
            }
        }
        for (i in pixels.indices) pixels[i] = out[i] or (blurredAlpha[i].coerceIn(0, 255) shl 24)
    }

    private fun runBoxes(data: IntArray, scratch: IntArray, w: Int, h: Int, boxes: IntArray) {
        for (size in boxes) {
            val r = (size - 1) / 2
            if (r <= 0) continue
            boxHorizontal(data, scratch, w, h, r)
            boxVertical(scratch, data, w, h, r)
        }
    }

    private fun boxHorizontal(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (y in 0 until h) {
            val row = y * w
            var sum = 0
            for (x in -r..r) if (x in 0 until w) sum += src[row + x]
            for (x in 0 until w) {
                dst[row + x] = sum / div
                val add = x + r + 1
                val remove = x - r
                if (add < w) sum += src[row + add]
                if (remove >= 0) sum -= src[row + remove]
            }
        }
    }

    private fun boxVertical(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (x in 0 until w) {
            var sum = 0
            for (y in -r..r) if (y in 0 until h) sum += src[y * w + x]
            for (y in 0 until h) {
                dst[y * w + x] = sum / div
                val add = y + r + 1
                val remove = y - r
                if (add < h) sum += src[add * w + x]
                if (remove >= 0) sum -= src[remove * w + x]
            }
        }
    }

    /** Box widths whose successive application approximates a gaussian with the given sigma. */
    internal fun boxesForGauss(sigma: Float, n: Int): IntArray {
        val wIdeal = sqrt(12.0 * sigma * sigma / n + 1)
        var wl = floor(wIdeal).toInt()
        if (wl % 2 == 0) wl--
        val wu = wl + 2
        val mIdeal = (12.0 * sigma * sigma - n * wl * wl - 4.0 * n * wl - 3.0 * n) / (-4.0 * wl - 4)
        val m = mIdeal.roundToInt()
        return IntArray(n) { if (it < m) wl else wu }
    }
}
