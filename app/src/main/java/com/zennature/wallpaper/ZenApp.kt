package com.zennature.wallpaper

import android.app.Application
import android.content.Context
import androidx.core.content.res.ResourcesCompat
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.zennature.wallpaper.data.UserStore
import com.zennature.wallpaper.data.WallpaperRepository
import com.zennature.wallpaper.data.parseLocalCatalog
import com.zennature.wallpaper.data.remote.USER_AGENT
import com.zennature.wallpaper.data.remote.searchNasaImages
import com.zennature.wallpaper.data.remote.searchOpenverse
import com.zennature.wallpaper.data.remote.searchWikimediaCommons
import com.zennature.wallpaper.export.ExportEngine
import com.zennature.wallpaper.render.BitmapSource
import com.zennature.wallpaper.render.RenderFonts
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import java.io.File
import java.util.concurrent.TimeUnit

class ZenApp : Application(), SingletonImageLoader.Factory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = container.imageLoader
}

/** Manual dependency container; the app is small enough that a DI framework would be dead weight. */
class AppContainer(context: Context) {
    private val app = context.applicationContext

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
        }
        .build()

    val imageLoader: ImageLoader = ImageLoader.Builder(app)
        .components { add(OkHttpNetworkFetcherFactory(callFactory = { http })) }
        .memoryCache { MemoryCache.Builder().maxSizePercent(app, 0.20).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(File(app.cacheDir, "image_cache").toOkioPath())
                .maxSizeBytes(256L * 1024 * 1024)
                .build()
        }
        .crossfade(true)
        .build()

    val store = UserStore(File(app.filesDir, "zen"))

    val repository: WallpaperRepository by lazy {
        val catalog = app.assets.open("catalog/local_wallpapers.json").bufferedReader().use { it.readText() }
        WallpaperRepository(
            localWallpapers = parseLocalCatalog(catalog),
            wikimedia = { q, c, l, p -> searchWikimediaCommons(http, q, c, l, p) },
            nasa = { q, c, l, p -> searchNasaImages(http, q, c, l, p) },
            openverse = { q, c, l, p -> searchOpenverse(http, q, c, l, p) },
        )
    }

    val bitmaps = BitmapSource(app, imageLoader)

    val exporter = ExportEngine(app, http)

    val fonts: RenderFonts by lazy {
        RenderFonts(
            serif = ResourcesCompat.getFont(app, R.font.playfair_regular)!!,
            sans = ResourcesCompat.getFont(app, R.font.jakarta_regular)!!,
            sansLight = ResourcesCompat.getFont(app, R.font.jakarta_light)!!,
        )
    }
}

val Context.appContainer: AppContainer get() = (applicationContext as ZenApp).container
