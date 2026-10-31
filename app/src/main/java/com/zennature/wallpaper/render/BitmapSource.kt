package com.zennature.wallpaper.render

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Precision
import coil3.size.Scale
import coil3.toBitmap
import com.zennature.wallpaper.data.ColorPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decodes wallpapers into software bitmaps for the renderer. The web editor re-downloaded and re-decoded
 * the full image on every slider tick and re-ran palette extraction inside every render; here the decoded
 * bitmap and palette are cached per URL and size bucket.
 */
class BitmapSource(private val context: Context, private val loader: ImageLoader) {

    private val bitmaps = object : LruCache<String, Bitmap>(96 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount / 1024
    }
    private val palettes = LruCache<String, ColorPalette>(64)

    suspend fun load(url: String, maxEdge: Int): Bitmap? {
        val key = "$url@$maxEdge"
        bitmaps.get(key)?.let { return it }
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(maxEdge, maxEdge)
            .scale(Scale.FIT)
            .precision(Precision.INEXACT)
            .allowHardware(false)
            .build()
        val result = loader.execute(request) as? SuccessResult ?: return null
        val bitmap = result.image.toBitmap()
        bitmaps.put(key, bitmap)
        return bitmap
    }

    suspend fun palette(url: String): ColorPalette {
        palettes.get(url)?.let { return it }
        val bmp = load(url, 512) ?: return ColorPalette.Default
        val palette = withContext(Dispatchers.Default) {
            val small = Bitmap.createScaledBitmap(bmp, 64, 64, true)
            val px = IntArray(64 * 64)
            small.getPixels(px, 0, 64, 0, 0, 64, 64)
            if (small !== bmp) small.recycle()
            extractPalette(px)
        }
        palettes.put(url, palette)
        return palette
    }
}
