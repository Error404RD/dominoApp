package com.error404.dominoapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

private val Default = Typography()

/**
 * "tnum" = tabular numbers: every digit has the same width,
 * so a score going from 95 to 100 doesn't make the layout jump.
 */
private fun TextStyle.scoreDigits(weight: FontWeight) =
    copy(fontWeight = weight, fontFeatureSettings = "tnum")

/** Material type scale with bolder headings; display styles are for the big scores. */
val Typography = Typography(
    displayLarge = Default.displayLarge.scoreDigits(FontWeight.Black),
    displayMedium = Default.displayMedium.scoreDigits(FontWeight.Black),
    displaySmall = Default.displaySmall.scoreDigits(FontWeight.Bold),
    headlineLarge = Default.headlineLarge.scoreDigits(FontWeight.Bold),
    headlineMedium = Default.headlineMedium.scoreDigits(FontWeight.Bold),
    headlineSmall = Default.headlineSmall.scoreDigits(FontWeight.SemiBold),
    titleLarge = Default.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = Default.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Default.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)
