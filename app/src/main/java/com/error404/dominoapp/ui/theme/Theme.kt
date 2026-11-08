package com.error404.dominoapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = CoralLight,
    onPrimary = OnCoralLight,
    primaryContainer = CoralContainerLight,
    onPrimaryContainer = OnCoralContainerLight,
    secondary = TurquoiseLight,
    onSecondary = OnTurquoiseLight,
    secondaryContainer = TurquoiseContainerLight,
    onSecondaryContainer = OnTurquoiseContainerLight,
    tertiary = SunLight,
    onTertiary = OnSunLight,
    tertiaryContainer = SunContainerLight,
    onTertiaryContainer = OnSunContainerLight,
    background = SandBackground,
    onBackground = OnSandBackground,
    surface = SandBackground,
    onSurface = OnSandBackground,
    surfaceVariant = SandVariant,
    onSurfaceVariant = OnSandVariant,
    outline = SandOutline,
    outlineVariant = SandOutlineVariant,
    surfaceTint = TurquoiseLight,
    surfaceBright = SandBackground,
    surfaceDim = SandDim,
    surfaceContainerLowest = SandContainerLowest,
    surfaceContainerLow = SandContainerLow,
    surfaceContainer = SandContainer,
    surfaceContainerHigh = SandContainerHigh,
    surfaceContainerHighest = SandContainerHighest,
)

private val DarkColorScheme = darkColorScheme(
    primary = CoralDark,
    onPrimary = OnCoralDark,
    primaryContainer = CoralContainerDark,
    onPrimaryContainer = OnCoralContainerDark,
    secondary = TurquoiseDark,
    onSecondary = OnTurquoiseDark,
    secondaryContainer = TurquoiseContainerDark,
    onSecondaryContainer = OnTurquoiseContainerDark,
    tertiary = SunDark,
    onTertiary = OnSunDark,
    tertiaryContainer = SunContainerDark,
    onTertiaryContainer = OnSunContainerDark,
    background = NightBackground,
    onBackground = OnNightBackground,
    surface = NightBackground,
    onSurface = OnNightBackground,
    surfaceVariant = NightVariant,
    onSurfaceVariant = OnNightVariant,
    outline = NightOutline,
    outlineVariant = NightOutlineVariant,
    surfaceTint = TurquoiseDark,
    surfaceBright = NightBright,
    surfaceDim = NightBackground,
    surfaceContainerLowest = NightContainerLowest,
    surfaceContainerLow = NightContainerLow,
    surfaceContainer = NightContainer,
    surfaceContainerHigh = NightContainerHigh,
    surfaceContainerHighest = NightContainerHighest,
)

/** Rounder corners than the Material default: friendlier look. */
private val DominoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * The app's theme. Dynamic color (wallpaper-based colors) is intentionally off
 * so the app keeps its own Caribbean identity on every phone.
 */
@Composable
fun DominoAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = DominoShapes,
        content = content,
    )
}

/**
 * Brand top bar: solid turquoise in light mode, deep teal in dark mode
 * (a bright bar at night would glare). Use it in every screen's TopAppBar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun dominoTopAppBarColors(): TopAppBarColors {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.5f
    val container = if (isDark) colors.secondaryContainer else colors.secondary
    val content = if (isDark) colors.onSecondaryContainer else colors.onSecondary
    return TopAppBarDefaults.topAppBarColors(
        containerColor = container,
        titleContentColor = content,
        navigationIconContentColor = content,
        actionIconContentColor = content,
    )
}
