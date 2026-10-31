package com.zennature.wallpaper.export

import com.zennature.wallpaper.data.ProviderType
import com.zennature.wallpaper.data.WallpaperItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

val ProviderType.wireName: String
    get() = when (this) {
        ProviderType.WIKIMEDIA -> "wikimedia"
        ProviderType.NASA -> "nasa"
        ProviderType.OPENVERSE -> "openverse"
        ProviderType.LOCAL -> "local"
    }

fun generateAttributionText(wallpaper: WallpaperItem, now: Date = Date()): String {
    val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    val lic = wallpaper.license
    val authorUrl = lic.authorUrl?.let { "Author Profile: $it\n" } ?: ""
    return """ZEN NATURE — WALLPAPER SOURCE & ATTRIBUTION
Generated: ${iso.format(now)}
-----------------------------------------------------------
Image Title: ${wallpaper.title}
Creator / Author: ${lic.author ?: "Unknown / Public Archives"}
${authorUrl}Source Provider: ${wallpaper.provider.wireName.uppercase()}
Original Archive Page: ${wallpaper.sourcePageUrl}

LICENSE & USAGE TERMS:
-----------------------------------------------------------
License: ${lic.name} (${lic.code})
License URL: ${lic.url ?: "Not provided"}
Attribution Required: ${if (lic.attributionRequired) "YES (Attribution included in this file)" else "NO (Public Domain / CC0)"}
Commercial Use: ${if (lic.commercialUseAllowed) "Allowed" else "Restricted / Non-Commercial"}
Derivative Works: ${if (lic.derivativeWorksAllowed) "Allowed" else "Restricted"}
Verified License Status: ${if (lic.verified) "Verified via Official API" else "Unverified provisional metadata"}

-----------------------------------------------------------
Exported from Zen Nature — Minimalist Wallpaper Studio.
Open-source nature wallpaper builder honoring Creative Commons and Public Domain imagery."""
}

/** One-line credit copied to the clipboard from the detail screen. */
fun attributionLine(wallpaper: WallpaperItem): String =
    "Image: \"${wallpaper.title}\" by ${wallpaper.license.author ?: "Unknown"} via " +
        "${wallpaper.provider.wireName.uppercase()} (${wallpaper.license.name}). ${wallpaper.sourcePageUrl}"

fun sanitizedTitle(title: String, maxLen: Int = 32): String =
    title.lowercase().replace(Regex("[^a-z0-9]"), "-").replace(Regex("-+"), "-").take(maxLen)
