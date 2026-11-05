package com.abeerapps.kmpexpensesplitter.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Default = Typography()

/**
 * Text styles from the Figma spec (sizes/line heights/letter spacing).
 *
 * The design uses Inter; we currently render with the platform font
 * (Roboto / SF Pro), which has near-identical metrics. Bundle Inter via
 * Compose resources and set `fontFamily` here to match the design exactly.
 */
internal val AppTypography = Typography(
    // Screen titles, e.g. "Set up your profile".
    headlineSmall = Default.headlineSmall.merge(
        TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.07.sp),
    ),
    // Field labels, input values, button labels.
    titleMedium = Default.titleMedium.merge(
        TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.31).sp),
    ),
    // Text typed into inputs.
    bodyLarge = Default.bodyLarge.merge(
        TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.31).sp),
    ),
    // Supporting copy under titles.
    bodyMedium = Default.bodyMedium.merge(
        TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.15).sp),
    ),
    // List item primary text.
    labelLarge = Default.labelLarge.merge(
        TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.15).sp),
    ),
    // Small metadata, e.g. currency codes.
    labelMedium = Default.labelMedium.merge(
        TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
    ),
)
