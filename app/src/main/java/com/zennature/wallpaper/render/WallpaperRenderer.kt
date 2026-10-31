package com.zennature.wallpaper.render

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import com.zennature.wallpaper.data.EditorSettings
import com.zennature.wallpaper.data.FontStyle
import com.zennature.wallpaper.data.MeshBlendMode
import com.zennature.wallpaper.data.MeshGradientSettings
import com.zennature.wallpaper.data.MistGradient
import com.zennature.wallpaper.data.TextPosition
import com.zennature.wallpaper.data.TextSize
import com.zennature.wallpaper.data.WashPreset
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

class RenderFonts(val serif: Typeface, val sans: Typeface, val sansLight: Typeface)

/** Canvas blend operations used by the web renderer. */
enum class Blend { NORMAL, SOFT_LIGHT, OVERLAY, COLOR, LUMINOSITY, LIGHTEN, COLOR_BURN, MULTIPLY, SCREEN }

/**
 * Native port of exportEngine.renderWallpaperToCanvas. Layer order, colours, alphas and geometry follow
 * the web version step by step. On Android 10+ blend modes are exact (Skia, same as Chrome's canvas);
 * on Android 7–9 the four modes Android lacks there (soft-light, color, luminosity, color-burn) use the
 * closest PorterDuff mode — see PARITY.md.
 */
object WallpaperRenderer {

    private const val BACKGROUND = 0xFF121413.toInt()

    fun render(
        source: Bitmap,
        settings: EditorSettings,
        width: Int,
        height: Int,
        primaryColor: String,
        fonts: RenderFonts,
        noiseSeed: Long = 7L,
    ): Bitmap {
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(BACKGROUND)
        val full = Rect(0, 0, width, height)
        val mesh = settings.meshGradient
        val primary = mesh.customPrimaryColor ?: primaryColor

        if (mesh.enabled && mesh.blendMode == MeshBlendMode.PURE) {
            drawMesh(canvas, width, height, mesh, primary)
        } else {
            if (mesh.enabled && mesh.blendMode == MeshBlendMode.BACKDROP) {
                drawMesh(canvas, width, height, mesh, primary)
            }
            drawPhoto(canvas, source, settings, width, height)
        }

        if (settings.warmth != 0) {
            val a = kotlin.math.abs(settings.warmth) / 100f * 0.35f
            val color = if (settings.warmth > 0) argb(a, 240, 160, 80) else argb(a, 80, 160, 240)
            fill(canvas, full, color, Blend.SOFT_LIGHT)
        }

        if (settings.washPreset != WashPreset.NONE && settings.washIntensity > 0) {
            val a = settings.washIntensity / 100f * 0.45f
            when (settings.washPreset) {
                WashPreset.SAGE -> fill(canvas, full, argb(a, 138, 154, 134), Blend.COLOR)
                WashPreset.SUMI -> fill(canvas, full, argb(a * 1.3f, 40, 42, 40), Blend.LUMINOSITY)
                WashPreset.SAND -> fill(canvas, full, argb(a, 224, 214, 196), Blend.SOFT_LIGHT)
                WashPreset.MIST -> fill(canvas, full, argb(a, 235, 240, 238), Blend.LIGHTEN)
                WashPreset.TWILIGHT -> fill(canvas, full, argb(a, 45, 55, 75), Blend.COLOR_BURN)
                WashPreset.NONE -> Unit
            }
        }

        if (mesh.enabled && (mesh.blendMode == MeshBlendMode.SOFT_LIGHT || mesh.blendMode == MeshBlendMode.OVERLAY)) {
            drawMesh(canvas, width, height, mesh, primary)
        }

        if (settings.vignette > 0) {
            val radius = max(width, height) * 0.75f
            val a = settings.vignette / 100f * 0.85f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    width / 2f, height / 2f, radius,
                    intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, argb(a, 0, 0, 0)),
                    floatArrayOf(0f, 0.3f, 1f),
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawRect(full, paint)
        }

        if (settings.grain > 0) drawGrain(canvas, full, settings.grain, noiseSeed)

        if (settings.komorebiRays > 0) drawKomorebi(canvas, width, height, settings.komorebiRays)

        if (settings.mistGradient != MistGradient.NONE && settings.mistIntensity > 0) {
            val a = settings.mistIntensity / 100f * 0.75f
            val (startY, endY) = if (settings.mistGradient == MistGradient.BOTTOM) {
                height * 0.45f to height.toFloat()
            } else {
                height * 0.55f to 0f
            }
            val paint = Paint().apply {
                shader = LinearGradient(
                    0f, startY, 0f, endY,
                    intArrayOf(argb(0f, 240, 245, 242), argb(a * 0.4f, 240, 245, 242), argb(a, 235, 242, 238)),
                    floatArrayOf(0f, 0.4f, 1f),
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawRect(full, paint)
        }

        if (settings.washiTexture > 0) drawWashi(canvas, full, settings.washiTexture, noiseSeed)

        val text = settings.textOverlay
        if (text.enabled && (text.text.isNotEmpty() || text.kanji.isNotEmpty())) {
            drawText(canvas, width, height, settings, fonts)
        }
        return out
    }

    // ------------------------------------------------------------------ photo

    private fun drawPhoto(canvas: Canvas, src: Bitmap, s: EditorSettings, w: Int, h: Int) {
        val imgAspect = src.width.toFloat() / src.height
        val targetAspect = w.toFloat() / h
        val (baseW, baseH) = if (imgAspect > targetAspect) h * imgAspect to h.toFloat() else w.toFloat() to w / imgAspect
        val drawW = baseW * s.zoom
        val drawH = baseH * s.zoom
        val panX = s.panX / 50f * max(0f, (drawW - w) / 2)
        val panY = s.panY / 50f * max(0f, (drawH - h) / 2)
        val left = (w - drawW) / 2 + panX
        val top = (h - drawH) / 2 + panY
        val dst = RectF(left, top, left + drawW, top + drawH)

        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cssFilterMatrix(s.brightness / 100f, s.contrast / 100f, s.saturation / 100f))
        }
        val sigma = s.blur / 10f * (w / 800f)
        if (sigma < 0.3f) {
            canvas.drawBitmap(src, null, dst, paint)
            return
        }
        val layer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(layer).drawBitmap(src, null, dst, paint)
        val px = IntArray(w * h)
        layer.getPixels(px, 0, w, 0, 0, w, h)
        Blur.gaussian(px, w, h, sigma)
        layer.setPixels(px, 0, w, 0, 0, w, h)
        canvas.drawBitmap(layer, 0f, 0f, null)
        layer.recycle()
    }

    /** CSS `brightness() contrast() saturate()` applied in that order. */
    fun cssFilterMatrix(brightness: Float, contrast: Float, saturation: Float): ColorMatrix {
        val b = ColorMatrix().apply { setScale(brightness, brightness, brightness, 1f) }
        val off = 127.5f * (1 - contrast)
        val c = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, off,
                0f, contrast, 0f, 0f, off,
                0f, 0f, contrast, 0f, off,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        val sv = saturation
        val sat = ColorMatrix(
            floatArrayOf(
                0.213f + 0.787f * sv, 0.715f - 0.715f * sv, 0.072f - 0.072f * sv, 0f, 0f,
                0.213f - 0.213f * sv, 0.715f + 0.285f * sv, 0.072f - 0.072f * sv, 0f, 0f,
                0.213f - 0.213f * sv, 0.715f - 0.715f * sv, 0.072f + 0.928f * sv, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        return ColorMatrix(b).apply {
            postConcat(c)
            postConcat(sat)
        }
    }

    // ------------------------------------------------------------------ mesh

    /**
     * Each mesh node is drawn as its own blurred layer and blended onto the canvas in turn, matching the
     * web build where every node's fillRect was blended (and blurred by ctx.filter) independently.
     * Layers are rasterised at quarter size: they are smooth gradients, so this is visually lossless.
     */
    private fun drawMesh(canvas: Canvas, w: Int, h: Int, mesh: MeshGradientSettings, primary: String) {
        val nodes = generateMeshNodes(primary, mesh.preset, mesh.seed)
        val intensity = (mesh.intensity / 100f).coerceIn(0.05f, 1f)
        val blend = when (mesh.blendMode) {
            MeshBlendMode.SOFT_LIGHT -> Blend.SOFT_LIGHT
            MeshBlendMode.OVERLAY -> Blend.OVERLAY
            MeshBlendMode.BACKDROP, MeshBlendMode.PURE -> Blend.NORMAL
        }
        val scale = 4
        val lw = max(1, w / scale)
        val lh = max(1, h / scale)
        val sigma = if (mesh.blur > 0) (mesh.blur / 35f) * (w / 500f) * 12f / scale else 0f
        val dst = Rect(0, 0, w, h)
        val drawPaint = blendPaint(blend).apply { isFilterBitmap = true }

        fun composite(layer: Bitmap) {
            if (sigma >= 0.3f) {
                val px = IntArray(lw * lh)
                layer.getPixels(px, 0, lw, 0, 0, lw, lh)
                Blur.gaussian(px, lw, lh, sigma)
                layer.setPixels(px, 0, lw, 0, 0, lw, lh)
            }
            canvas.drawBitmap(layer, null, dst, drawPaint)
        }

        val layer = Bitmap.createBitmap(lw, lh, Bitmap.Config.ARGB_8888)
        val layerCanvas = Canvas(layer)
        if (mesh.blendMode == MeshBlendMode.PURE) {
            layerCanvas.drawColor(hexToArgb(nodes.last().color))
            composite(layer)
        }
        val maxDim = max(lw, lh)
        for (node in nodes) {
            layer.eraseColor(Color.TRANSPARENT)
            val cx = node.x / 100f * lw
            val cy = node.y / 100f * lh
            val radius = node.radius / 100f * maxDim * 0.75f
            val a = node.opacity * intensity
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    cx, cy, radius,
                    intArrayOf(hexToArgb(node.color, a), hexToArgb(node.color, a * 0.55f), hexToArgb(node.color, 0f)),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP,
                )
            }
            layerCanvas.drawRect(0f, 0f, lw.toFloat(), lh.toFloat(), paint)
            composite(layer)
        }
        layer.recycle()
    }

    // ------------------------------------------------------------------ textures

    private fun drawGrain(canvas: Canvas, full: Rect, grain: Int, seed: Long) {
        val random = Random(seed)
        val size = 128
        val intensity = grain / 100f * 45f
        val alpha = (grain / 100f * 50f).roundToInt().coerceIn(0, 255)
        val px = IntArray(size * size) {
            val v = (128 + (random.nextFloat() - 0.5f) * intensity).roundToInt().coerceIn(0, 255)
            (alpha shl 24) or (v shl 16) or (v shl 8) or v
        }
        val tile = Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
        val paint = blendPaint(Blend.OVERLAY).apply {
            shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        }
        canvas.drawRect(full, paint)
    }

    private fun drawKomorebi(canvas: Canvas, w: Int, h: Int, rays: Int) {
        val a = rays / 100f * 0.45f
        val scale = 4
        val lw = max(1, w / scale)
        val lh = max(1, h / scale)
        val px = rasterizeConicalGradient(
            lw, lh,
            x0 = lw * 0.15f, y0 = -lh * 0.1f, r0 = lw * 0.1f,
            x1 = lw * 0.6f, y1 = lh * 0.8f, r1 = lw * 1.5f,
            stops = listOf(
                Stop(0f, 255, 245, 215, a * 1.2f),
                Stop(0.35f, 255, 235, 195, a * 0.7f),
                Stop(0.7f, 255, 240, 220, a * 0.25f),
                Stop(1f, 255, 255, 255, 0f),
            ),
        )
        val layer = Bitmap.createBitmap(px, lw, lh, Bitmap.Config.ARGB_8888)
        canvas.drawBitmap(layer, null, Rect(0, 0, w, h), blendPaint(Blend.SCREEN).apply { isFilterBitmap = true })
        layer.recycle()
    }

    private fun drawWashi(canvas: Canvas, full: Rect, amount: Int, seed: Long) {
        val random = Random(seed * 31 + 3)
        val tile = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val tc = Canvas(tile)
        tc.drawColor(0xFFF8F6F0.toInt())
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            color = argb(0.25f, 160, 150, 135)
        }
        fun r() = random.nextFloat()
        repeat(35) {
            val sx = r() * 256
            val sy = r() * 256
            val path = Path().apply {
                moveTo(sx, sy)
                cubicTo(
                    sx + (r() - 0.5f) * 40, sy + (r() - 0.5f) * 40,
                    sx + (r() - 0.5f) * 60, sy + (r() - 0.5f) * 60,
                    sx + (r() - 0.5f) * 80, sy + (r() - 0.5f) * 80,
                )
            }
            tc.drawPath(path, stroke)
        }
        val paint = blendPaint(Blend.SOFT_LIGHT).apply {
            shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            alpha = (amount / 100f * 0.55f * 255).roundToInt().coerceIn(0, 255)
        }
        canvas.drawRect(full, paint)
    }

    // ------------------------------------------------------------------ typography

    private fun drawText(canvas: Canvas, w: Int, h: Int, s: EditorSettings, fonts: RenderFonts) {
        val t = s.textOverlay
        val scale = w / 1080f
        val base = when (t.size) {
            TextSize.SM -> 20f
            TextSize.MD -> 28f
            TextSize.LG -> 38f
            TextSize.XL -> 52f
        } * scale
        val face = if (t.fontStyle == FontStyle.SERIF) fonts.serif else fonts.sans
        val centerY = when (t.position) {
            TextPosition.TOP -> h * 0.28f
            TextPosition.CENTER -> h * 0.5f
            TextPosition.BOTTOM -> h * 0.78f
        }
        val opacity = t.opacity.coerceIn(0f, 1f)
        val baseColor = runCatching { hexToArgb(t.color) }.getOrDefault(Color.WHITE)

        fun paint(typeface: Typeface, size: Float, shadowBlur: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = size
            textAlign = Paint.Align.CENTER
            color = baseColor
            alpha = (opacity * 255).roundToInt()
            // Canvas shadowBlur is 2*sigma; Android's radius maps to sigma ≈ 0.577*r + 0.5.
            val sigma = shadowBlur / 2f
            val radius = ((sigma - 0.5f) / 0.57735f).coerceAtLeast(0.1f)
            setShadowLayer(radius, 0f, 2f * scale, argb(0.45f * opacity, 0, 0, 0))
        }

        fun Paint.drawMiddle(text: String, y: Float) {
            val fm = fontMetrics
            canvas.drawText(text, w / 2f, y - (fm.ascent + fm.descent) / 2f, this)
        }

        if (t.showJapaneseMark && t.kanji.isNotEmpty()) {
            paint(face, base * 1.6f, 12f * scale).drawMiddle(t.kanji, centerY - base * 1.4f)
        }

        val lines = t.text.split("\n")
        val lineHeight = base * 1.45f
        val startY = centerY - (lines.size - 1) * lineHeight / 2f
        val main = paint(face, base, 12f * scale)
        lines.forEachIndexed { i, line -> main.drawMiddle(line.trim(), startY + i * lineHeight) }

        if (t.subtext.isNotEmpty()) {
            val sub = paint(fonts.sansLight, base * 0.55f, 8f * scale)
            sub.drawMiddle(t.subtext.uppercase(), startY + lines.size * lineHeight + 4 * scale)
        }
    }

    // ------------------------------------------------------------------ helpers

    private fun fill(canvas: Canvas, rect: Rect, color: Int, blend: Blend) {
        canvas.drawRect(rect, blendPaint(blend).apply { this.color = color })
    }

    fun blendPaint(blend: Blend): Paint {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (blend == Blend.NORMAL) return paint
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            paint.blendMode = when (blend) {
                Blend.SOFT_LIGHT -> BlendMode.SOFT_LIGHT
                Blend.OVERLAY -> BlendMode.OVERLAY
                Blend.COLOR -> BlendMode.COLOR
                Blend.LUMINOSITY -> BlendMode.LUMINOSITY
                Blend.LIGHTEN -> BlendMode.LIGHTEN
                Blend.COLOR_BURN -> BlendMode.COLOR_BURN
                Blend.MULTIPLY -> BlendMode.MULTIPLY
                Blend.SCREEN -> BlendMode.SCREEN
                Blend.NORMAL -> BlendMode.SRC_OVER
            }
        } else {
            val mode = when (blend) {
                Blend.SOFT_LIGHT, Blend.OVERLAY -> PorterDuff.Mode.OVERLAY
                Blend.LIGHTEN -> PorterDuff.Mode.LIGHTEN
                Blend.COLOR_BURN, Blend.MULTIPLY -> PorterDuff.Mode.MULTIPLY
                Blend.SCREEN -> PorterDuff.Mode.SCREEN
                Blend.COLOR, Blend.LUMINOSITY, Blend.NORMAL -> PorterDuff.Mode.SRC_OVER
            }
            paint.xfermode = PorterDuffXfermode(mode)
        }
        return paint
    }

    private fun argb(alpha: Float, r: Int, g: Int, b: Int): Int =
        Color.argb((alpha.coerceIn(0f, 1f) * 255).roundToInt(), r, g, b)
}
