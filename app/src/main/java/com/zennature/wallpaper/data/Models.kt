package com.zennature.wallpaper.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ProviderType {
    @SerialName("wikimedia") WIKIMEDIA,
    @SerialName("nasa") NASA,
    @SerialName("openverse") OPENVERSE,
    @SerialName("local") LOCAL,
}

@Serializable
enum class AspectRatioType(val id: String) {
    @SerialName("9:16") MOBILE("9:16"),
    @SerialName("20:9") FLAGSHIP("20:9"),
    @SerialName("3:4") TABLET("3:4"),
    @SerialName("16:9") DESKTOP("16:9"),
    @SerialName("1:1") SQUARE("1:1"),
}

@Serializable
data class LicenseInfo(
    val code: String,
    val name: String,
    val url: String? = null,
    val author: String? = null,
    val authorUrl: String? = null,
    val attributionRequired: Boolean,
    val commercialUseAllowed: Boolean,
    val derivativeWorksAllowed: Boolean,
    val verified: Boolean,
)

@Serializable
data class WallpaperItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val thumbnailUrl: String,
    val fullUrl: String,
    val provider: ProviderType,
    val sourcePageUrl: String,
    val width: Int,
    val height: Int,
    val aspectRatio: Double,
    val category: String,
    val license: LicenseInfo,
    val dateCreated: String? = null,
)

@Serializable
enum class WashPreset {
    @SerialName("none") NONE,
    @SerialName("sage") SAGE,
    @SerialName("sumi") SUMI,
    @SerialName("sand") SAND,
    @SerialName("mist") MIST,
    @SerialName("twilight") TWILIGHT,
}

@Serializable
enum class MeshPresetId {
    @SerialName("biophilic") BIOPHILIC,
    @SerialName("dawn") DAWN,
    @SerialName("twilight") TWILIGHT,
    @SerialName("river") RIVER,
    @SerialName("golden") GOLDEN,
    @SerialName("zen") ZEN,
}

@Serializable
enum class MeshBlendMode {
    @SerialName("overlay") OVERLAY,
    @SerialName("soft-light") SOFT_LIGHT,
    @SerialName("backdrop") BACKDROP,
    @SerialName("pure") PURE,
}

data class MeshNode(
    val x: Int,
    val y: Int,
    val radius: Int,
    val color: String,
    val opacity: Float,
)

@Serializable
data class MeshGradientSettings(
    val enabled: Boolean = false,
    val preset: MeshPresetId = MeshPresetId.BIOPHILIC,
    val intensity: Int = 65,
    val blur: Int = 35,
    val blendMode: MeshBlendMode = MeshBlendMode.SOFT_LIGHT,
    val seed: Int = 42,
    val customPrimaryColor: String? = null,
)

@Serializable
enum class FontStyle { @SerialName("serif") SERIF, @SerialName("sans") SANS }

@Serializable
enum class TextPosition { @SerialName("top") TOP, @SerialName("center") CENTER, @SerialName("bottom") BOTTOM }

@Serializable
enum class TextSize { @SerialName("sm") SM, @SerialName("md") MD, @SerialName("lg") LG, @SerialName("xl") XL }

@Serializable
data class TextOverlaySettings(
    val enabled: Boolean = false,
    val text: String = "",
    val subtext: String = "",
    val kanji: String = "",
    val fontStyle: FontStyle = FontStyle.SERIF,
    val position: TextPosition = TextPosition.BOTTOM,
    val size: TextSize = TextSize.MD,
    val color: String = "#FFFFFF",
    val opacity: Float = 0.9f,
    val showJapaneseMark: Boolean = true,
)

@Serializable
enum class GridOverlayType {
    @SerialName("none") NONE,
    @SerialName("ruleOfThirds") RULE_OF_THIRDS,
    @SerialName("goldenRatio") GOLDEN_RATIO,
    @SerialName("centerCross") CENTER_CROSS,
}

@Serializable
enum class UiPreviewMode { @SerialName("lockscreen") LOCKSCREEN, @SerialName("homescreen") HOMESCREEN, @SerialName("clean") CLEAN }

@Serializable
enum class MistGradient { @SerialName("none") NONE, @SerialName("bottom") BOTTOM, @SerialName("top") TOP }

@Serializable
data class EditorSettings(
    val cropRatio: AspectRatioType = AspectRatioType.FLAGSHIP,
    val zoom: Float = 1.0f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val brightness: Int = 100,
    val contrast: Int = 100,
    val saturation: Int = 100,
    val warmth: Int = 0,
    val blur: Int = 0,
    val vignette: Int = 15,
    val grain: Int = 5,
    val washPreset: WashPreset = WashPreset.NONE,
    val washIntensity: Int = 35,
    val meshGradient: MeshGradientSettings = MeshGradientSettings(),
    val textOverlay: TextOverlaySettings = TextOverlaySettings(),
    val gridOverlay: GridOverlayType = GridOverlayType.RULE_OF_THIRDS,
    val uiPreviewMode: UiPreviewMode = UiPreviewMode.LOCKSCREEN,
    val washiTexture: Int = 0,
    val komorebiRays: Int = 0,
    val mistGradient: MistGradient = MistGradient.NONE,
    val mistIntensity: Int = 40,
)

data class ColorPalette(
    val dominant: String,
    val secondary: String,
    val muted: String,
    val light: String,
    val dark: String,
) {
    val swatches: List<String> get() = listOf(dominant, secondary, muted, light, dark)

    companion object {
        val Default = ColorPalette("#556952", "#8A9A86", "#73716B", "#F8F6F0", "#121413")
    }
}

@Serializable
data class SavedProject(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val wallpaper: WallpaperItem,
    val settings: EditorSettings,
    /** file:// URI of a JPEG preview written by the editor (never inline base64). */
    val previewThumbnail: String? = null,
)

data class CuratedCategory(
    val id: String,
    val label: String,
    val subtitle: String,
    val query: String,
    val icon: CategoryIcon,
    val tagline: String,
    /** Extra search terms rotated page by page so a category never runs dry. */
    val extraQueries: List<String> = emptyList(),
    /** Space-themed categories also pull from NASA; nature ones skip it (NASA returns rockets for "forest"). */
    val useNasa: Boolean = false,
) {
    val queries: List<String> get() = listOf(query) + extraQueries
}

enum class CategoryIcon {
    MOUNTAIN, TREES, TREE_PINE, WAVES, COMPASS, SUN, CLOUD_RAIN, FEATHER, SUNSET, MOON,
    WATERFALL, SNOW, LEAF, FLOWER, AURORA, SPACE, CLOUD, CANYON, BEACH, DARK,
}

data class ZenPhrasePreset(
    val id: String,
    val text: String,
    val subtext: String? = null,
    val kanji: String? = null,
)

enum class ProviderFilter { ALL, LOCAL, WIKIMEDIA, NASA, OPENVERSE }

enum class OrientationFilter { ALL, PORTRAIT, LANDSCAPE, SQUARE }

enum class LicenseFilter { ALL, CC0_PD, COMMERCIAL }

data class FilterOptions(
    val query: String = "",
    val category: String = "",
    val provider: ProviderFilter = ProviderFilter.ALL,
    val orientation: OrientationFilter = OrientationFilter.ALL,
    val licenseOnly: LicenseFilter = LicenseFilter.ALL,
)
