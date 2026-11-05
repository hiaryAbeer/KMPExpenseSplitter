package com.abeerapps.kmpexpensesplitter.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private val Default = Typography()

/**
 * Line height only spaces lines *within* a paragraph; the extra leading above
 * the first line and below the last one is trimmed. Without this, an 18sp
 * title in a 28sp line adds ~5dp of invisible space, so a 16dp gap next to
 * text would look like ~21dp and break the "max 16dp between views" rule.
 */
private val TrimmedLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

private fun TextStyle.spec(
    size: TextUnit,
    lineHeight: TextUnit,
    weight: FontWeight,
    letterSpacing: TextUnit,
) = merge(
    TextStyle(
        fontSize = size,
        lineHeight = lineHeight,
        fontWeight = weight,
        letterSpacing = letterSpacing,
        lineHeightStyle = TrimmedLineHeight,
    ),
)

/**
 * Text styles from the Figma spec (sizes/line heights/letter spacing).
 *
 * The design uses Inter; we currently render with the platform font
 * (Roboto / SF Pro), which has near-identical metrics. Bundle Inter via
 * Compose resources and set `fontFamily` here to match the design exactly.
 */
internal val AppTypography = Typography(
    // Screen titles, e.g. "Set up your profile", "Hello, Ahmad".
    headlineSmall = Default.headlineSmall.spec(24.sp, 32.sp, FontWeight.Medium, 0.07.sp),
    // Field labels, input values, button labels.
    titleMedium = Default.titleMedium.spec(16.sp, 24.sp, FontWeight.Medium, (-0.31).sp),
    // Text typed into inputs.
    bodyLarge = Default.bodyLarge.spec(16.sp, 24.sp, FontWeight.Normal, (-0.31).sp),
    // Supporting copy under titles.
    bodyMedium = Default.bodyMedium.spec(14.sp, 20.sp, FontWeight.Normal, (-0.15).sp),
    // Empty-state titles ("No groups yet").
    titleLarge = Default.titleLarge.spec(20.sp, 28.sp, FontWeight.Medium, (-0.45).sp),
    // Section titles ("Recent Groups").
    titleSmall = Default.titleSmall.spec(18.sp, 28.sp, FontWeight.Medium, (-0.44).sp),
    // List item primary text.
    labelLarge = Default.labelLarge.spec(14.sp, 20.sp, FontWeight.Medium, (-0.15).sp),
    // Small metadata, e.g. currency codes.
    labelMedium = Default.labelMedium.spec(12.sp, 16.sp, FontWeight.Medium, 0.sp),
    // Onboarding title (unchanged Material size, trimmed like the rest).
    headlineMedium = Default.headlineMedium.merge(TextStyle(lineHeightStyle = TrimmedLineHeight)),
)
