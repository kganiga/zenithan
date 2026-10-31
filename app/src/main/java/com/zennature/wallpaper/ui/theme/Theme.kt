package com.zennature.wallpaper.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.zennature.wallpaper.R

/** Palette lifted from the Tailwind classes of the web build ("Warm Mist White" / "Zen Charcoal"). */
@Immutable
data class ZenColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,          // cards: white / #1E201F
    val surfaceMuted: Color,     // #EFECE6 / #202221
    val surfaceSunken: Color,    // #F8F6F0 / #151716
    val border: Color,           // #E8E4DC / #2D302F
    val borderStrong: Color,     // #DCD7CE / #383A38
    val textPrimary: Color,      // #2C2B29 / #F3F4F1
    val textSecondary: Color,    // #726F68 / #939188
    val textTertiary: Color,     // #9E9B93 / #6C6E6D
    val accent: Color,           // sage #556952 / #BBCBB8
    val accentSoft: Color,       // #8A9A86
    val inverseBg: Color,        // #2C2B29 / #E2E3E0 (primary buttons)
    val inverseText: Color,      // white / #121413
    val sage: Color = Color(0xFF8A9A86),
    val forest: Color = Color(0xFF556952),
    val forestPressed: Color = Color(0xFF435340),
    val rose: Color = Color(0xFFF43F5E),
    val emerald: Color = Color(0xFF10B981),
)

val LightZen = ZenColors(
    isDark = false,
    background = Color(0xFFF8F6F0),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFEFECE6),
    surfaceSunken = Color(0xFFF8F6F0),
    border = Color(0xFFE8E4DC),
    borderStrong = Color(0xFFDCD7CE),
    textPrimary = Color(0xFF2C2B29),
    textSecondary = Color(0xFF726F68),
    textTertiary = Color(0xFF9E9B93),
    accent = Color(0xFF556952),
    accentSoft = Color(0xFF8A9A86),
    inverseBg = Color(0xFF2C2B29),
    inverseText = Color(0xFFF8F6F0),
)

val DarkZen = ZenColors(
    isDark = true,
    background = Color(0xFF121413),
    surface = Color(0xFF1E201F),
    surfaceMuted = Color(0xFF202221),
    surfaceSunken = Color(0xFF151716),
    border = Color(0xFF2D302F),
    borderStrong = Color(0xFF383A38),
    textPrimary = Color(0xFFF3F4F1),
    textSecondary = Color(0xFF939188),
    textTertiary = Color(0xFF6C6E6D),
    accent = Color(0xFFBBCBB8),
    accentSoft = Color(0xFFBBCBB8),
    inverseBg = Color(0xFFE2E3E0),
    inverseText = Color(0xFF121413),
)

val LocalZenColors = staticCompositionLocalOf { LightZen }

val Playfair = FontFamily(
    Font(R.font.playfair_regular, FontWeight.Normal),
    Font(R.font.playfair_medium, FontWeight.Medium),
    Font(R.font.playfair_semibold, FontWeight.SemiBold),
    Font(R.font.playfair_bold, FontWeight.Bold),
    Font(R.font.playfair_italic, FontWeight.Normal, FontStyle.Italic),
)

val Jakarta = FontFamily(
    Font(R.font.jakarta_extralight, FontWeight.ExtraLight),
    Font(R.font.jakarta_light, FontWeight.Light),
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
)

object Zen {
    val colors: ZenColors
        @Composable get() = LocalZenColors.current
}

@Composable
fun ZenTheme(dark: Boolean, content: @Composable () -> Unit) {
    val zen = if (dark) DarkZen else LightZen
    val scheme = if (dark) {
        darkColorScheme(
            primary = zen.accent, onPrimary = zen.inverseText, background = zen.background,
            surface = zen.surface, onSurface = zen.textPrimary, onBackground = zen.textPrimary,
            outline = zen.border,
        )
    } else {
        lightColorScheme(
            primary = zen.forest, onPrimary = Color.White, background = zen.background,
            surface = zen.surface, onSurface = zen.textPrimary, onBackground = zen.textPrimary,
            outline = zen.border,
        )
    }
    CompositionLocalProvider(LocalZenColors provides zen) {
        MaterialTheme(colorScheme = scheme, typography = JakartaTypography, content = content)
    }
}

private val JakartaTypography: Typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = Jakarta),
        displayMedium = displayMedium.copy(fontFamily = Jakarta),
        displaySmall = displaySmall.copy(fontFamily = Jakarta),
        headlineLarge = headlineLarge.copy(fontFamily = Jakarta),
        headlineMedium = headlineMedium.copy(fontFamily = Jakarta),
        headlineSmall = headlineSmall.copy(fontFamily = Jakarta),
        titleLarge = titleLarge.copy(fontFamily = Jakarta),
        titleMedium = titleMedium.copy(fontFamily = Jakarta),
        titleSmall = titleSmall.copy(fontFamily = Jakarta),
        bodyLarge = bodyLarge.copy(fontFamily = Jakarta),
        bodyMedium = bodyMedium.copy(fontFamily = Jakarta),
        bodySmall = bodySmall.copy(fontFamily = Jakarta),
        labelLarge = labelLarge.copy(fontFamily = Jakarta),
        labelMedium = labelMedium.copy(fontFamily = Jakarta),
        labelSmall = labelSmall.copy(fontFamily = Jakarta),
    )
}
