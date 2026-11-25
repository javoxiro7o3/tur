package uz.uydanimabor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import uz.uydanimabor.data.ThemeMode

/** Rishton sopol idishlaridagi kobalt koʻk. */
val Cobalt = Color(0xFF1E4FA8)
/** Firuza. */
val Turquoise = Color(0xFF0E9AA7)
/** Sabzi rangi — faqat "tez ishlatish kerak" belgisi uchun. */
val Carrot = Color(0xFFF07A1A)
val NightBackground = Color(0xFF0C1424)
val Ink = Color(0xFF111827)

private val LightColors = lightColorScheme(
    primary = Cobalt,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FA),
    onPrimaryContainer = Color(0xFF0B2350),
    secondary = Color(0xFF0A7D88), // matn uchun yetarli kontrast (4.5:1+)
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDEFF2),
    onSecondaryContainer = Color(0xFF00363C),
    tertiary = Carrot,
    onTertiary = Ink,
    background = Color(0xFFF7F9FC),
    onBackground = Ink,
    surface = Color(0xFFF7F9FC),
    onSurface = Ink,
    surfaceVariant = Color(0xFFE6EAF2),
    onSurfaceVariant = Color(0xFF3F4756),
    surfaceContainer = Color(0xFFEEF2F8),
    surfaceContainerHigh = Color(0xFFE8EDF5),
    surfaceContainerLow = Color(0xFFF2F5FA),
    outline = Color(0xFF6B7385),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DB9F2),
    onPrimary = NightBackground,
    primaryContainer = Cobalt,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF5FD0DA),
    onSecondary = NightBackground,
    secondaryContainer = Color(0xFF0B5B63),
    onSecondaryContainer = Color(0xFFCDEFF2),
    tertiary = Carrot,
    onTertiary = Ink,
    background = NightBackground,
    onBackground = Color(0xFFE6EAF2),
    surface = NightBackground,
    onSurface = Color(0xFFE6EAF2),
    surfaceVariant = Color(0xFF26324A),
    onSurfaceVariant = Color(0xFFC3CAD8),
    surfaceContainer = Color(0xFF16213A),
    surfaceContainerHigh = Color(0xFF1C2842),
    surfaceContainerLow = Color(0xFF111B30),
    outline = Color(0xFF8C95A8),
    error = Color(0xFFF2B8B5),
)

@Immutable
data class ExtraColors(val urgent: Color, val onUrgent: Color, val have: Color, val onHave: Color)

val LocalExtraColors = staticCompositionLocalOf {
    ExtraColors(Carrot, Ink, Color(0xFFDCE6FA), Color(0xFF0B2350))
}

@Composable
fun UydaTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors: ColorScheme = if (dark) DarkColors else LightColors
    val extra = ExtraColors(
        urgent = Carrot,
        onUrgent = Ink,
        have = colors.primaryContainer,
        onHave = colors.onPrimaryContainer,
    )
    androidx.compose.runtime.CompositionLocalProvider(LocalExtraColors provides extra) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}

fun isDark(mode: ThemeMode, system: Boolean) = when (mode) {
    ThemeMode.SYSTEM -> system
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
