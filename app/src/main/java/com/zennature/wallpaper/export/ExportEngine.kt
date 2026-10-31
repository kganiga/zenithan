package com.zennature.wallpaper.export

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.zennature.wallpaper.data.WallpaperItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

enum class ImageFormat(val mime: String, val ext: String) {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
}

enum class WallpaperTarget { HOME, LOCK, BOTH }

/**
 * Real file output. The web build used <a download> with blob URLs, which Capacitor's WebView silently
 * ignores, so "Download" and "License File" never produced files on Android. Here every save goes through
 * MediaStore (Pictures/Zenithan and Download/Zenithan) and sharing goes through a FileProvider.
 */
class ExportEngine(private val context: Context, private val http: OkHttpClient) {

    fun encode(bitmap: Bitmap, format: ImageFormat, quality: Int = 95): ByteArray {
        val bos = ByteArrayOutputStream()
        val compressFormat = if (format == ImageFormat.PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        bitmap.compress(compressFormat, quality, bos)
        return bos.toByteArray()
    }

    suspend fun saveImage(bytes: ByteArray, filename: String, mime: String): Uri? = withContext(Dispatchers.IO) {
        writeToMediaStore(filename, mime, bytes, Environment.DIRECTORY_PICTURES, imagesCollection())
    }

    suspend fun saveAttribution(wallpaper: WallpaperItem): Uri? = withContext(Dispatchers.IO) {
        val bytes = generateAttributionText(wallpaper).toByteArray(Charsets.UTF_8)
        val name = "zenithan_studio_${wallpaper.id.replace(Regex("[^A-Za-z0-9_-]"), "_")}_attribution.txt"
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            null
        }
        writeToMediaStore(name, "text/plain", bytes, Environment.DIRECTORY_DOWNLOADS, collection)
    }

    /** Fetches the original file bytes ("Download 4K"). Local/asset/content URIs are read directly. */
    suspend fun fetchOriginal(url: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            when {
                url.startsWith("http") -> http.newCall(Request.Builder().url(url).build()).execute().use { r ->
                    if (r.isSuccessful) r.body?.bytes() else null
                }
                url.startsWith("file:///android_asset/") ->
                    context.assets.open(url.removePrefix("file:///android_asset/")).use { it.readBytes() }
                else -> context.contentResolver.openInputStream(Uri.parse(url))?.use { it.readBytes() }
            }
        }.getOrNull()
    }

    suspend fun shareImage(bytes: ByteArray, filename: String, mime: String, title: String, text: String) {
        val uri = withContext(Dispatchers.IO) { cacheFile(bytes, filename) }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Hands the image to the system "Set as" picker (launcher/gallery wallpaper croppers). */
    suspend fun openSetAs(bytes: ByteArray, filename: String) {
        val uri = withContext(Dispatchers.IO) { cacheFile(bytes, filename) }
        val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
            setDataAndType(uri, "image/jpeg")
            putExtra("mimeType", "image/jpeg")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Set as wallpaper").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    suspend fun applyWallpaper(bitmap: Bitmap, target: WallpaperTarget) = withContext(Dispatchers.IO) {
        val manager = WallpaperManager.getInstance(context)
        val which = when (target) {
            WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
            WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
            WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        }
        manager.setBitmap(bitmap, null, true, which)
    }

    private fun cacheFile(bytes: ByteArray, filename: String): Uri {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        dir.listFiles()?.forEach { if (System.currentTimeMillis() - it.lastModified() > 60 * 60 * 1000) it.delete() }
        val file = File(dir, filename)
        file.writeBytes(bytes)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun imagesCollection(): Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        null
    }

    private fun writeToMediaStore(name: String, mime: String, bytes: ByteArray, publicDir: String, collection: Uri?): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && collection != null) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$publicDir/Zenithan")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: return null
            return runCatching {
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            }.getOrElse {
                resolver.delete(uri, null, null)
                null
            }
        }
        // Android 9 and below: needs WRITE_EXTERNAL_STORAGE (requested by the UI before calling).
        @Suppress("DEPRECATION")
        val dir = File(Environment.getExternalStoragePublicDirectory(publicDir), "Zenithan").apply { mkdirs() }
        val file = File(dir, name)
        return runCatching {
            FileOutputStream(file).use { out: OutputStream -> out.write(bytes) }
            if (mime.startsWith("image/")) {
                @Suppress("DEPRECATION")
                context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
            }
            Uri.fromFile(file)
        }.getOrNull()
    }
}

/** Aspect-fill [src] into a [width]×[height] bitmap, centred (the web "Phone Fit" canvas). */
fun coverFit(src: Bitmap, width: Int, height: Int): Bitmap {
    val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val scale = maxOf(width / src.width.toFloat(), height / src.height.toFloat())
    val dw = src.width * scale
    val dh = src.height * scale
    val dst = android.graphics.RectF((width - dw) / 2f, (height - dh) / 2f, (width + dw) / 2f, (height + dh) / 2f)
    android.graphics.Canvas(out).drawBitmap(src, null, dst, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
    return out
}

/** MIME type sniffed from the first bytes, so "Download 4K" keeps the original encoding. */
fun sniffImageMime(bytes: ByteArray): Pair<String, String> = when {
    bytes.size > 8 && bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() -> "image/png" to "png"
    bytes.size > 12 && bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() -> "image/webp" to "webp"
    else -> "image/jpeg" to "jpg"
}
