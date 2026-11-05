package com.zennature.wallpaper.data.remote

import com.zennature.wallpaper.data.LicenseInfo
import com.zennature.wallpaper.data.ProviderType
import com.zennature.wallpaper.data.WallpaperItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

/** Wikimedia asks API clients to send a descriptive User-Agent; browsers could not, native can. */
internal const val USER_AGENT = "ZenNatureWallpaper/2.0 (Android; https://github.com/kganiga)"

private fun JsonElement?.obj(): JsonObject? = this as? JsonObject
private fun JsonElement?.arr(): JsonArray? = this as? JsonArray
private fun JsonElement?.str(): String? = (this as? JsonPrimitive)?.contentOrNull
private fun JsonElement?.int(): Int? = (this as? JsonPrimitive)?.intOrNull

private suspend fun getJson(client: OkHttpClient, url: String, timeoutMs: Long): JsonElement? =
    withContext(Dispatchers.IO) {
        val scoped = client.newBuilder().callTimeout(timeoutMs, TimeUnit.MILLISECONDS).build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .build()
        runCatching {
            scoped.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()?.let { json.parseToJsonElement(it) }
            }
        }.getOrNull()
    }

// ---------------------------------------------------------------- Wikimedia

internal fun cleanHtml(input: String?): String {
    if (input.isNullOrEmpty()) return ""
    return input
        .replace(Regex("<[^>]*>"), "")
        .replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace("&#039;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .trim()
}

internal fun parseWikimediaLicense(
    licenseShortName: String?,
    usageTerms: String?,
    licenseUrl: String?,
    artistHtml: String?,
): LicenseInfo {
    val shortName = licenseShortName?.trim()?.takeIf { it.isNotEmpty() }
        ?: usageTerms?.trim()?.takeIf { it.isNotEmpty() }
        ?: "Unknown"
    val artist = cleanHtml(artistHtml).ifEmpty { "Wikimedia Commons Contributor" }
    val lower = shortName.lowercase()
    val isPD = lower.contains("public domain") || lower.contains("cc0") ||
        lower.contains("no rights reserved") || lower.contains("pd")
    val isCCBY = lower.contains("cc by") || lower.contains("creative commons attribution")
    val isCCBYSA = lower.contains("cc by-sa") || lower.contains("sharealike")
    val isNC = lower.contains("nc") || lower.contains("noncommercial")
    val isND = lower.contains("nd") || lower.contains("noderivs")
    return LicenseInfo(
        code = shortName,
        name = shortName,
        url = licenseUrl ?: if (isPD) "https://creativecommons.org/publicdomain/zero/1.0/" else null,
        author = artist,
        attributionRequired = !isPD,
        commercialUseAllowed = !isNC,
        derivativeWorksAllowed = !isND,
        verified = isPD || isCCBY || isCCBYSA || shortName != "Unknown",
    )
}

internal fun parseWikimediaResponse(root: JsonElement?, categoryTag: String): List<WallpaperItem> {
    val pages = root.obj()?.get("query").obj()?.get("pages").obj() ?: return emptyList()
    val items = mutableListOf<WallpaperItem>()
    for ((_, pageEl) in pages) {
        val page = pageEl.obj() ?: continue
        val info = page["imageinfo"].arr()?.firstOrNull().obj() ?: continue
        val url = info["url"].str() ?: continue
        val mime = info["mime"].str()?.lowercase().orEmpty()
        if (!mime.contains("jpeg") && !mime.contains("jpg") && !mime.contains("png") && !mime.contains("webp")) continue
        val width = info["width"].int() ?: 1200
        val height = info["height"].int() ?: 1800
        if (maxOf(width, height) < 1000) continue

        val meta = info["extmetadata"].obj()
        fun metaValue(key: String) = meta?.get(key).obj()?.get("value").str()

        val pageTitle = page["title"].str().orEmpty()
        val rawTitle = pageTitle.replace(Regex("^File:", RegexOption.IGNORE_CASE), "").replace(Regex("\\.[^/.]+$"), "")
        val cleanTitle = cleanHtml(metaValue("ObjectName")).ifEmpty { rawTitle.replace('_', ' ') }
        val description = cleanHtml(metaValue("ImageDescription")).ifEmpty { cleanTitle }
        val pageId = page["pageid"].int() ?: continue

        items += WallpaperItem(
            id = "wiki-$pageId",
            title = if (cleanTitle.length > 60) cleanTitle.take(57) + "..." else cleanTitle,
            description = description,
            thumbnailUrl = info["thumburl"].str() ?: url,
            fullUrl = url,
            provider = ProviderType.WIKIMEDIA,
            sourcePageUrl = info["descriptionurl"].str()
                ?: "https://commons.wikimedia.org/wiki/${URLEncoder.encode(pageTitle, "UTF-8")}",
            width = width,
            height = height,
            aspectRatio = width.toDouble() / height,
            category = categoryTag,
            license = parseWikimediaLicense(
                metaValue("LicenseShortName"), metaValue("UsageTerms"), metaValue("LicenseUrl"), metaValue("Artist"),
            ),
            dateCreated = metaValue("DateTimeOriginal"),
        )
    }
    return items
}

suspend fun searchWikimediaCommons(
    client: OkHttpClient,
    query: String,
    categoryTag: String = "nature",
    limit: Int = 24,
    page: Int = 1,
): List<WallpaperItem> {
    val url = "https://commons.wikimedia.org/w/api.php".toHttpUrl().newBuilder()
        .addQueryParameter("action", "query")
        .addQueryParameter("generator", "search")
        .addQueryParameter("gsrnamespace", "6")
        .addQueryParameter("gsrlimit", limit.toString())
        .addQueryParameter("gsroffset", maxOf(0, (page - 1) * limit).toString())
        .addQueryParameter("gsrsearch", "$query filetype:bitmap -logo -flag -map -diagram -icon -poster -drawing -painting -illustration")
        .addQueryParameter("prop", "imageinfo")
        .addQueryParameter("iiprop", "url|size|mime|extmetadata")
        .addQueryParameter("iiurlwidth", "800")
        .addQueryParameter("format", "json")
        .build()
    return parseWikimediaResponse(getJson(client, url.toString(), 8_000), categoryTag)
}

// ---------------------------------------------------------------- NASA

private val NASA_LICENSE = LicenseInfo(
    code = "NASA Public Domain",
    name = "NASA Media Usage Guidelines (Public Domain)",
    url = "https://www.nasa.gov/multimedia/guidelines/index.html",
    author = "NASA",
    attributionRequired = false,
    commercialUseAllowed = true,
    derivativeWorksAllowed = true,
    verified = true,
)

internal fun parseNasaResponse(root: JsonElement?, categoryTag: String, limit: Int): List<WallpaperItem> {
    val items = root.obj()?.get("collection").obj()?.get("items").arr() ?: return emptyList()
    return items.take(limit).mapNotNull { element ->
        val item = element.obj() ?: return@mapNotNull null
        val datum = item["data"].arr()?.firstOrNull().obj() ?: return@mapNotNull null
        val thumb = item["links"].arr()?.firstOrNull().obj()?.get("href").str() ?: return@mapNotNull null
        val nasaId = datum["nasa_id"].str() ?: return@mapNotNull null
        val full = thumb
            .replace(Regex("~thumb\\.jpg$", RegexOption.IGNORE_CASE), "~orig.jpg")
            .replace(Regex("~thumb\\.png$", RegexOption.IGNORE_CASE), "~orig.png")
        val center = datum["center"].str()
        val author = datum["photographer"].str() ?: datum["secondary_creator"].str()
            ?: if (center != null) "NASA / $center" else "NASA"
        val title = datum["title"].str()
        WallpaperItem(
            id = "nasa-$nasaId",
            title = title ?: "Earth and Space Nature",
            description = datum["description"].str() ?: title,
            thumbnailUrl = thumb,
            fullUrl = full,
            provider = ProviderType.NASA,
            sourcePageUrl = "https://images.nasa.gov/details-$nasaId",
            width = 1920,
            height = 1080,
            aspectRatio = 16.0 / 9.0,
            category = categoryTag,
            license = NASA_LICENSE.copy(author = author),
            dateCreated = datum["date_created"].str(),
        )
    }
}

suspend fun searchNasaImages(
    client: OkHttpClient,
    query: String,
    categoryTag: String = "nature",
    limit: Int = 20,
    page: Int = 1,
): List<WallpaperItem> {
    val url = "https://images-api.nasa.gov/search".toHttpUrl().newBuilder()
        .addQueryParameter("q", query)
        .addQueryParameter("media_type", "image")
        .addQueryParameter("page", page.toString())
        .build()
    return parseNasaResponse(getJson(client, url.toString(), 8_000), categoryTag, limit)
}

// ---------------------------------------------------------------- Openverse

internal fun parseOpenverseLicense(license: String?, version: String?, licenseUrl: String?, creator: String?, creatorUrl: String?): LicenseInfo {
    val lic = license.orEmpty().lowercase()
    val ver = version ?: "4.0"
    val isPD = lic == "cc0" || lic == "pdm"
    val code = if (isPD) (if (lic == "cc0") "CC0" else "Public Domain") else "CC-${lic.uppercase()}-$ver"
    return LicenseInfo(
        code = code,
        name = code,
        url = licenseUrl ?: if (isPD) "https://creativecommons.org/publicdomain/zero/1.0/" else null,
        author = creator ?: "Openverse Contributor",
        authorUrl = creatorUrl,
        attributionRequired = !isPD,
        commercialUseAllowed = !lic.contains("nc"),
        derivativeWorksAllowed = !lic.contains("nd"),
        verified = true,
    )
}

internal fun parseOpenverseResponse(root: JsonElement?, categoryTag: String): List<WallpaperItem> {
    val results = root.obj()?.get("results").arr() ?: return emptyList()
    return results.mapNotNull { element ->
        val r = element.obj() ?: return@mapNotNull null
        val url = r["url"].str() ?: return@mapNotNull null
        val id = r["id"].str() ?: return@mapNotNull null
        val width = r["width"].int() ?: 1200
        val height = r["height"].int() ?: 1800
        val title = r["title"].str()
        WallpaperItem(
            id = "openverse-$id",
            title = title ?: "Nature Landscape",
            description = r["attribution"].str() ?: title,
            thumbnailUrl = r["thumbnail"].str() ?: url,
            fullUrl = url,
            provider = ProviderType.OPENVERSE,
            sourcePageUrl = r["foreign_landing_url"].str() ?: url,
            width = width,
            height = height,
            aspectRatio = width.toDouble() / height,
            category = categoryTag,
            license = parseOpenverseLicense(
                r["license"].str(), r["license_version"].str(), r["license_url"].str(),
                r["creator"].str(), r["creator_url"].str(),
            ),
        )
    }
}

suspend fun searchOpenverse(
    client: OkHttpClient,
    query: String,
    categoryTag: String = "nature",
    pageSize: Int = 20,
    page: Int = 1,
): List<WallpaperItem> {
    val url = "https://api.openverse.org/v1/images/".toHttpUrl().newBuilder()
        .addQueryParameter("q", query)
        .addQueryParameter("page_size", pageSize.toString())
        .addQueryParameter("page", page.toString())
        .addQueryParameter("license_type", "all-cc")
        .addQueryParameter("category", "photograph")
        .addQueryParameter("mature", "false")
        .build()
    return parseOpenverseResponse(getJson(client, url.toString(), 7_000), categoryTag)
}
