package com.zennature.wallpaper.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val CURATED_CATEGORIES: List<CuratedCategory> = listOf(
    CuratedCategory("misty-mountains", "Misty Mountains", "Solitary peaks veiled in cloud", "misty mountains", CategoryIcon.MOUNTAIN, "Quiet elevations rising through morning vapors",
        listOf("mountain fog valley", "alpine peaks clouds", "mountain range mist", "mountain ridge sunrise", "dolomites", "himalaya")),
    CuratedCategory("japanese-gardens", "Japanese Gardens", "Raked gravel and moss stones", "Japanese garden", CategoryIcon.TREES, "Centuries of contemplation sculpted in moss and rock",
        listOf("zen garden", "moss garden kyoto", "japanese maple garden", "koi pond garden", "torii gate forest", "stone lantern garden")),
    CuratedCategory("forests-moss", "Forests & Moss", "Dappled light in ancient cedar", "foggy forest", CategoryIcon.TREE_PINE, "Soft evergreen ground cover absorbing all noise",
        listOf("pine forest", "moss forest", "redwood forest", "forest path sunlight", "old growth forest", "rainforest canopy", "birch forest")),
    CuratedCategory("ocean-waves", "Ocean & Waves", "Endless horizon and rhythmic tide", "ocean waves", CategoryIcon.WAVES, "The timeless breathing of deep waters",
        listOf("sea horizon", "ocean sunset", "rocky coast waves", "sea cliffs", "seascape long exposure", "wave crashing")),
    CuratedCategory("lakes-reflections", "Lakes & Reflections", "Mirror calm glacial waters", "lake reflection", CategoryIcon.COMPASS, "Stillness that captures both sky and self",
        listOf("mountain lake", "glacial lake", "calm lake sunrise", "fjord", "alpine lake", "lake mist morning")),
    CuratedCategory("minimalist-deserts", "Minimalist Deserts", "Wind-rippled solitary dunes", "sand dunes", CategoryIcon.SUN, "Pure geometry carved by wandering winds",
        listOf("desert landscape", "sahara dunes", "desert sunset", "salt flat", "namib desert", "desert road")),
    CuratedCategory("rain-fog", "Rain & Fog", "Soft drops and silver mists", "fog landscape", CategoryIcon.CLOUD_RAIN, "The gentle sound of water renewing the earth",
        listOf("misty morning", "rain drops leaves", "fog trees", "foggy field", "mist hills", "rainy forest")),
    CuratedCategory("bamboo-stones", "Bamboo & Stones", "Flexible stalks and grounded granite", "bamboo forest", CategoryIcon.FEATHER, "Bending with the breeze, rooted deep in stone",
        listOf("arashiyama bamboo", "bamboo grove", "river stones", "pebbles beach", "stone balance", "bamboo leaves")),
    CuratedCategory("sunsets-dawn", "Sunsets & Dawn", "Ethereal amber and morning rose", "sunrise landscape", CategoryIcon.SUNSET, "The quiet threshold between dusk and awakening",
        listOf("sunset sky", "golden hour landscape", "dawn mountains", "sunset over sea", "twilight sky", "sunset silhouette trees")),
    CuratedCategory("moon-night-sky", "Moon & Night Sky", "Celestial silver over silent earth", "moon night sky", CategoryIcon.MOON, "Infinite quietude under cold celestial light",
        listOf("full moon", "night landscape stars", "moonrise", "crescent moon", "lunar surface"), useNasa = true),
    CuratedCategory("waterfalls-rivers", "Waterfalls & Rivers", "Falling water over mossy rock", "waterfall", CategoryIcon.WATERFALL, "Constant motion that somehow feels still",
        listOf("mountain river", "forest stream", "waterfall iceland", "river valley", "cascade moss", "river long exposure")),
    CuratedCategory("snow-winter", "Snow & Winter", "Hushed white and frozen blue", "snow mountains", CategoryIcon.SNOW, "Silence settles thickest after snowfall",
        listOf("winter forest snow", "frozen lake", "snowy landscape", "ice cave", "snow covered trees", "winter sunrise")),
    CuratedCategory("autumn-leaves", "Autumn Colors", "Maple fire and golden birch", "autumn forest", CategoryIcon.LEAF, "Letting go, one warm leaf at a time",
        listOf("fall foliage", "autumn leaves", "maple leaves", "autumn lake", "autumn road trees", "golden larch")),
    CuratedCategory("blossoms-meadows", "Blossoms & Meadows", "Cherry petals and wild fields", "cherry blossom", CategoryIcon.FLOWER, "Soft color that lasts only a moment",
        listOf("wildflower meadow", "lavender field", "sakura tree", "tulip field", "alpine meadow flowers", "poppy field")),
    CuratedCategory("aurora-stars", "Aurora & Stars", "Green light over frozen land", "aurora borealis", CategoryIcon.AURORA, "The sky quietly dancing for no one",
        listOf("milky way", "northern lights", "starry sky", "star trails", "night sky mountains", "aurora australis"), useNasa = true),
    CuratedCategory("space-nebula", "Space & Nebula", "Galaxies, nebulae and blue Earth", "nebula", CategoryIcon.SPACE, "Perspective from the far side of the sky",
        listOf("galaxy", "earth from space", "hubble deep field", "planet", "star cluster", "james webb"), useNasa = true),
    CuratedCategory("clouds-sky", "Clouds & Sky", "Drifting cumulus and open blue", "clouds sky", CategoryIcon.CLOUD, "Watching weather pass without holding on",
        listOf("sea of clouds", "dramatic clouds", "cumulus clouds", "cloudscape", "storm clouds", "clouds above mountains")),
    CuratedCategory("canyons-rocks", "Canyons & Rocks", "Layered stone and red earth", "canyon", CategoryIcon.CANYON, "Time made visible in stone",
        listOf("grand canyon", "sandstone", "rock formation", "antelope canyon", "monument valley", "cliffs")),
    CuratedCategory("tropical-coast", "Tropical Coast", "Turquoise lagoons and palms", "tropical beach", CategoryIcon.BEACH, "Warm water and slow afternoons",
        listOf("lagoon", "palm trees beach", "island aerial", "coral reef", "beach sunset palm", "maldives")),
    CuratedCategory("amoled-dark", "AMOLED Dark", "Deep blacks that save battery", "night landscape", CategoryIcon.DARK, "Darkness as a resting place for the eyes",
        listOf("black sand beach", "dark forest", "night mountains silhouette", "dark clouds", "volcano night", "moonlit lake"), useNasa = true),
)

/** Rotating search terms for the unfiltered feed: one per page, cycling through every mood. */
val DISCOVERY_QUERIES: List<String> = CURATED_CATEGORIES.flatMap { it.queries }.let { all ->
    // Interleave categories so consecutive pages feel different instead of 7 pages of mountains.
    val byCat = CURATED_CATEGORIES.map { it.queries }
    val max = byCat.maxOf { it.size }
    (0 until max).flatMap { i -> byCat.mapNotNull { it.getOrNull(i) } }.distinct().ifEmpty { all }
}

/** Categories whose wallpapers are mostly black, so tiles get the AMOLED badge. */
val DARK_CATEGORIES = setOf("moon-night-sky", "amoled-dark", "space-nebula", "aurora-stars")

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
