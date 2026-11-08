package com.error404.dominoapp.ui.theme

import androidx.compose.ui.graphics.Color

// "Caribbean" palette. Only Theme.kt should use these directly;
// screens must read colors from MaterialTheme.colorScheme.
// Each "On" color is the text/icon color drawn on top of its pair,
// chosen for at least 4.5:1 contrast.

// ---------- Light (sand background) ----------

// Coral: main actions (buttons, selection).
val CoralLight = Color(0xFFC2452D)
val OnCoralLight = Color(0xFFFFFFFF)
val CoralContainerLight = Color(0xFFFFDAD2)
val OnCoralContainerLight = Color(0xFF3C0800)

// Turquoise: brand color (top bar, highlights).
val TurquoiseLight = Color(0xFF006A6A)
val OnTurquoiseLight = Color(0xFFFFFFFF)
val TurquoiseContainerLight = Color(0xFF9CF1F0)
val OnTurquoiseContainerLight = Color(0xFF002020)

// Sun yellow: reserved for the winner.
val SunLight = Color(0xFF745B00)
val OnSunLight = Color(0xFFFFFFFF)
val SunContainerLight = Color(0xFFFFE08A)
val OnSunContainerLight = Color(0xFF241A00)

// Sand neutrals.
val SandBackground = Color(0xFFFFF8F0)
val OnSandBackground = Color(0xFF201A17)
val SandVariant = Color(0xFFF2DFD3)
val OnSandVariant = Color(0xFF52443C)
val SandOutline = Color(0xFF85736B)
val SandOutlineVariant = Color(0xFFD7C2B8)
val SandContainerLowest = Color(0xFFFFFFFF)
val SandContainerLow = Color(0xFFFCF2E9)
val SandContainer = Color(0xFFF6ECE3)
val SandContainerHigh = Color(0xFFF0E6DD)
val SandContainerHighest = Color(0xFFEAE0D8)
val SandDim = Color(0xFFE3D8CF)

// ---------- Dark (night blue background) ----------

val CoralDark = Color(0xFFFFB4A2)
val OnCoralDark = Color(0xFF5F1505)
val CoralContainerDark = Color(0xFF8A2C18)
val OnCoralContainerDark = Color(0xFFFFDAD2)

val TurquoiseDark = Color(0xFF4CDADA)
val OnTurquoiseDark = Color(0xFF003737)
val TurquoiseContainerDark = Color(0xFF004F4F)
val OnTurquoiseContainerDark = Color(0xFF9CF1F0)

val SunDark = Color(0xFFEBC248)
val OnSunDark = Color(0xFF3D2E00)
val SunContainerDark = Color(0xFF584400)
val OnSunContainerDark = Color(0xFFFFE08A)

val NightBackground = Color(0xFF0B1A24)
val OnNightBackground = Color(0xFFDCE4EA)
val NightVariant = Color(0xFF2A3A45)
val OnNightVariant = Color(0xFFBCC8D1)
val NightOutline = Color(0xFF87929B)
val NightOutlineVariant = Color(0xFF3D4B55)
val NightContainerLowest = Color(0xFF06121A)
val NightContainerLow = Color(0xFF13222D)
val NightContainer = Color(0xFF172732)
val NightContainerHigh = Color(0xFF21323D)
val NightContainerHighest = Color(0xFF2C3D48)
val NightBright = Color(0xFF31414C)
