package com.zennature.wallpaper.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val CURATED_CATEGORIES: List<CuratedCategory> = listOf(
    CuratedCategory("misty-mountains", "Misty Mountains", "Solitary peaks veiled in cloud", "minimal mist mountain landscape", CategoryIcon.MOUNTAIN, "Quiet elevations rising through morning vapors"),
    CuratedCategory("japanese-gardens", "Japanese Gardens", "Raked gravel and moss stones", "Japanese moss garden", CategoryIcon.TREES, "Centuries of contemplation sculpted in moss and rock"),
    CuratedCategory("forests-moss", "Forests & Moss", "Dappled light in ancient cedar", "foggy pine forest", CategoryIcon.TREE_PINE, "Soft evergreen ground cover absorbing all noise"),
    CuratedCategory("ocean-waves", "Ocean & Waves", "Endless horizon and rhythmic tide", "minimal ocean horizon", CategoryIcon.WAVES, "The timeless breathing of deep waters"),
    CuratedCategory("lakes-reflections", "Lakes & Reflections", "Mirror calm glacial waters", "calm lake reflection", CategoryIcon.COMPASS, "Stillness that captures both sky and self"),
    CuratedCategory("minimalist-deserts", "Minimalist Deserts", "Wind-rippled solitary dunes", "minimal sand dunes", CategoryIcon.SUN, "Pure geometry carved by wandering winds"),
    CuratedCategory("rain-fog", "Rain & Fog", "Soft drops and silver mists", "misty rain forest", CategoryIcon.CLOUD_RAIN, "The gentle sound of water renewing the earth"),
    CuratedCategory("bamboo-stones", "Bamboo & Stones", "Flexible stalks and grounded granite", "bamboo forest mist", CategoryIcon.FEATHER, "Bending with the breeze, rooted deep in stone"),
    CuratedCategory("sunsets-dawn", "Sunsets & Dawn", "Ethereal amber and morning rose", "soft sunrise mountain", CategoryIcon.SUNSET, "The quiet threshold between dusk and awakening"),
    CuratedCategory("moon-night-sky", "Moon & Night Sky", "Celestial silver over silent earth", "moon over mountains", CategoryIcon.MOON, "Infinite quietude under cold celestial light"),
)

/** Built on demand so the "today" phrase never goes stale across midnight (the web build froze it at load). */
fun zenPhrases(today: Date = Date()): List<ZenPhrasePreset> = listOf(
    ZenPhrasePreset("basho-pond", "The ancient pond;\nA frog leaps in,\nWater’s echo.", "Matsuo Basho", "古池や"),
    ZenPhrasePreset("shoshin", "Beginner’s Mind", "In the beginner’s mind there are many possibilities", "初心"),
    ZenPhrasePreset("meikyo", "Clear Mirror, Still Water", "Mind undisturbed by passing reflections", "明鏡止水"),
    ZenPhrasePreset("breathe-stillness", "Breathe in stillness,\nBreathe out the storm.", "Mindfulness Reminder", "静寂"),
    ZenPhrasePreset("mountain-cloud", "Silent mountain,\nPassing white cloud.", "Non-attachment", "山雲"),
    ZenPhrasePreset("ichigo-ichie", "One Time, One Meeting", "Cherish every singular moment", "一期一会"),
    ZenPhrasePreset("unbroken-pine", "The pine bends with the heavy snow,\nYet never breaks.", "Resilience", "松雪"),
    ZenPhrasePreset("flowing-water", "Water flows without hurry,\nYet shapes the canyon.", "Patience", "流水"),
    ZenPhrasePreset(
        "today-date",
        SimpleDateFormat("MMMM d, yyyy", Locale.US).format(today),
        "Find stillness in this day",
        "今日",
    ),
)

enum class DeviceType { MOBILE, TABLET, DESKTOP, SQUARE }

data class AspectRatioDef(
    val id: AspectRatioType,
    val label: String,
    val widthRatio: Int,
    val heightRatio: Int,
    val description: String,
    val idealWidth: Int,
    val idealHeight: Int,
    val deviceType: DeviceType,
)

val ASPECT_RATIOS: List<AspectRatioDef> = listOf(
    AspectRatioDef(AspectRatioType.MOBILE, "9:16 Mobile", 9, 16, "Universal phone lock screen", 1080, 1920, DeviceType.MOBILE),
    AspectRatioDef(AspectRatioType.FLAGSHIP, "20:9 Flagship", 9, 20, "Modern tall Android display", 1080, 2400, DeviceType.MOBILE),
    AspectRatioDef(AspectRatioType.TABLET, "3:4 Tablet", 3, 4, "Tablet & iPad portrait", 1536, 2048, DeviceType.TABLET),
    AspectRatioDef(AspectRatioType.DESKTOP, "16:9 Desktop", 16, 9, "Laptop & widescreen monitor", 1920, 1080, DeviceType.DESKTOP),
    AspectRatioDef(AspectRatioType.SQUARE, "1:1 Square", 1, 1, "Square profile & framing", 1080, 1080, DeviceType.SQUARE),
)

fun aspectRatioDef(id: AspectRatioType): AspectRatioDef = ASPECT_RATIOS.first { it.id == id }

data class Resolution(val width: Int, val height: Int)

fun resolutionForRatio(ratio: AspectRatioType, highRes: Boolean = true): Resolution {
    if (highRes) {
        return when (ratio) {
            AspectRatioType.MOBILE -> Resolution(1440, 2560)
            AspectRatioType.FLAGSHIP -> Resolution(1080, 2400)
            AspectRatioType.TABLET -> Resolution(1536, 2048)
            AspectRatioType.DESKTOP -> Resolution(2560, 1440)
            AspectRatioType.SQUARE -> Resolution(1440, 1440)
        }
    }
    val def = aspectRatioDef(ratio)
    return Resolution(def.idealWidth, def.idealHeight)
}

enum class CircadianId { DAWN, DAYLIGHT, TWILIGHT, NIGHT }

data class CircadianSlot(
    val id: CircadianId,
    val label: String,
    val hourRange: String,
    val moodDescription: String,
    val recommendedCategory: String,
    val japaneseTitle: String,
    val accentColor: String,
    val suggestedQuery: String,
)

fun circadianSlot(hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): CircadianSlot = when (hour) {
    in 5..8 -> CircadianSlot(
        CircadianId.DAWN, "Morning Mist & Dew", "05:00 — 09:00",
        "Gentle morning radiance and silver vapors rising over tranquil mountain peaks.",
        "sunsets-dawn", "Dawn Light · Morning Mist", "#D89E6E", "soft sunrise mountain mist",
    )
    in 9..16 -> CircadianSlot(
        CircadianId.DAYLIGHT, "Clear Daylight Stillness", "09:00 — 17:00",
        "Lush green moss sanctuaries, sunlit forest trails, and mirror lake reflections.",
        "japanese-gardens", "Sunlit Serenity · Deep Woods", "#6E8A68", "Japanese moss garden lake",
    )
    in 17..20 -> CircadianSlot(
        CircadianId.TWILIGHT, "Twilight & Blue Hour", "17:00 — 21:00",
        "Deep amber horizon and rippling desert dunes fading into peaceful dusk.",
        "minimalist-deserts", "Twilight Horizon · Blue Hour", "#4A5B73", "minimal sand dunes dusk twilight",
    )
    else -> CircadianSlot(
        CircadianId.NIGHT, "Moonlit Quietude", "21:00 — 05:00",
        "Silver celestial moonlight resting over dark pine ridges and still alpine waters.",
        "moon-night-sky", "Night Sky · Cosmic Silence", "#3A4454", "moon over mountain lake night",
    )
}

/** Personal photo license used for images imported from the device gallery. */
val PERSONAL_PHOTO_LICENSE = LicenseInfo(
    code = "Personal Photo",
    name = "Personal Creative Work (Private on device)",
    author = "You (Device Owner)",
    attributionRequired = false,
    commercialUseAllowed = true,
    derivativeWorksAllowed = true,
    verified = true,
)
