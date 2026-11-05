package com.abeerapps.kmpexpensesplitter.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Brand palette, taken from the Figma file "Expense splliter" (Tailwind teal/gray scale).
private val Teal50 = Color(0xFFF0FDFA)
private val Teal100 = Color(0xFFCCFBF1)
private val Teal400 = Color(0xFF2DD4BF)
private val Teal600 = Color(0xFF0D9488) // Primary
private val Teal900 = Color(0xFF134E4A)
private val Teal950 = Color(0xFF042F2E)

private val Coral20 = Color(0xFF5E1700)
private val Coral30 = Color(0xFF842500)
private val Coral40 = Color(0xFFA9380F)
private val Coral80 = Color(0xFFFFB59E)
private val Coral90 = Color(0xFFFFDBD0)
private val Coral10 = Color(0xFF3A0B00)

private val Ink = Color(0xFF1A1A1A)         // Primary text
private val Gray50 = Color(0xFFF9FAFB)      // Input fill
private val Gray100 = Color(0xFFF8F9FA)     // Screen background
private val Gray200 = Color(0xFFE5E7EB)     // Hairline borders
private val Gray400 = Color(0xFF9CA3AF)
private val Gray500 = Color(0xFF6B7280)     // Secondary text, icons
private val Gray700 = Color(0xFF374151)
private val Gray800 = Color(0xFF1F2937)
private val Gray900 = Color(0xFF111827)
private val Gray950 = Color(0xFF0B0F14)

internal val LightColors = lightColorScheme(
    primary = Teal600,
    onPrimary = Color.White,
    primaryContainer = Teal100,
    onPrimaryContainer = Teal900,
    secondary = Coral40,
    onSecondary = Color.White,
    secondaryContainer = Coral90,
    onSecondaryContainer = Coral10,
    background = Gray100,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Gray50,
    onSurfaceVariant = Gray500,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Gray50,
    outline = Gray500,
    outlineVariant = Gray200,
)

internal val DarkColors = darkColorScheme(
    primary = Teal400,
    onPrimary = Teal950,
    primaryContainer = Teal900,
    onPrimaryContainer = Teal100,
    secondary = Coral80,
    onSecondary = Coral20,
    secondaryContainer = Coral30,
    onSecondaryContainer = Coral90,
    background = Gray950,
    onBackground = Color(0xFFF3F4F6),
    surface = Gray900,
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Gray800,
    onSurfaceVariant = Gray400,
    surfaceContainerLowest = Gray900,
    surfaceContainerLow = Gray800,
    outline = Gray400,
    outlineVariant = Gray700,
)

/** Brand colors that have no Material [androidx.compose.material3.ColorScheme] slot. */
@Immutable
data class ExtendedColors(
    /** Top stop of the screen-header gradient; the bottom stop is `background`. */
    val headerGradientTop: Color,
)

internal val LightExtendedColors = ExtendedColors(headerGradientTop = Teal50)
internal val DarkExtendedColors = ExtendedColors(headerGradientTop = Color(0xFF0B2A27))
