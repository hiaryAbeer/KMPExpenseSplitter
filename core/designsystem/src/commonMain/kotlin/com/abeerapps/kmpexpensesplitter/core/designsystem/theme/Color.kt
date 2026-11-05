package com.abeerapps.kmpexpensesplitter.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Brand palette, taken from the Figma file "Expense splliter" (Tailwind teal/gray scale).
private val Teal50 = Color(0xFFF0FDFA)
private val Teal100 = Color(0xFFCCFBF1)
private val Teal400 = Color(0xFF2DD4BF)
private val Teal500 = Color(0xFF14B8A6) // Primary (dark)
private val Teal600 = Color(0xFF0D9488) // Primary
private val Teal900 = Color(0xFF134E4A)

private val Coral20 = Color(0xFF5E1700)
private val Coral30 = Color(0xFF842500)
private val Coral40 = Color(0xFFA9380F)
private val Coral80 = Color(0xFFFFB59E)
private val Coral90 = Color(0xFFFFDBD0)
private val Coral10 = Color(0xFF3A0B00)

private val Ink = Color(0xFF1A1A1A)         // Primary text
private val Gray50 = Color(0xFFF9FAFB)      // Input fill (light), primary text (dark)
private val Gray100 = Color(0xFFF8F9FA)     // Screen background
private val Gray150 = Color(0xFFF3F4F6)     // Unselected chips/tiles (Tailwind gray-100)
private val Gray200 = Color(0xFFE5E7EB)     // Hairline borders
private val Gray400 = Color(0xFF9CA3AF)
private val Gray500 = Color(0xFF6B7280)     // Secondary text, icons
private val Gray700 = Color(0xFF374151)
private val Gray800 = Color(0xFF1F2937)
private val Gray900 = Color(0xFF111827)

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
    surfaceContainerHigh = Gray150, // Unselected category tiles and split-type chips.
    outline = Gray500,
    outlineVariant = Gray200,
)

// Dark values from the Figma "home" dark design (Tailwind gray-900/800/700 + teal-500).
internal val DarkColors = darkColorScheme(
    primary = Teal500,
    onPrimary = Color.White, // "+" icon and button labels are white in the design.
    primaryContainer = Teal900,
    onPrimaryContainer = Teal100,
    secondary = Coral80,
    onSecondary = Coral20,
    secondaryContainer = Coral30,
    onSecondaryContainer = Coral90,
    background = Gray900,          // #111827 screen
    onBackground = Gray50,         // #F9FAFB titles
    surface = Gray800,             // #1F2937 cards, search bar, bottom bar
    onSurface = Gray50,
    surfaceVariant = Gray800,
    onSurfaceVariant = Gray400,    // #9CA3AF secondary text and icons
    surfaceContainerLowest = Gray800,
    surfaceContainerLow = Gray800, // input fill
    surfaceContainerHigh = Gray700, // unselected tiles/chips, one step above cards
    outlineVariant = Gray700,      // #374151 hairline borders
    outline = Gray400,
)

/** Brand colors that have no Material [androidx.compose.material3.ColorScheme] slot. */
@Immutable
data class ExtendedColors(
    /** Top stop of the screen-header gradient; the bottom stop is `background`. */
    val headerGradientTop: Color,
    /** Money in your favour ("You are owed", "+85.25"). */
    val positive: Color,
    /** Money you owe ("You owe", "-45.00"). */
    val negative: Color,
    /** Member avatar backgrounds, picked deterministically per member name. */
    val avatarPalette: List<Color>,
    /** Background of the "+N" overflow avatar. */
    val avatarOverflow: Color,
    /** Initials on coloured avatars: white in both themes, as in the design. */
    val onAvatar: Color = Color.White,
) {
    /** Stable avatar colour for a person: the same name always gets the same colour. */
    fun avatarColorFor(name: String): Color =
        avatarPalette[(name.trim().lowercase().hashCode() and Int.MAX_VALUE) % avatarPalette.size]
}

private val AvatarPalette = listOf(
    Color(0xFF615FFF), // Indigo
    Color(0xFFAD46FF), // Purple
    Color(0xFFF0B100), // Amber
    Color(0xFF0D9488), // Teal
    Color(0xFFEC4899), // Pink
    Color(0xFF3B82F6), // Blue
)

internal val LightExtendedColors = ExtendedColors(
    headerGradientTop = Teal50,
    positive = Color(0xFF10B981),
    negative = Color(0xFFEF4444),
    avatarPalette = AvatarPalette,
    avatarOverflow = Color(0xFFF3F4F6),
)
internal val DarkExtendedColors = ExtendedColors(
    headerGradientTop = Color(0xFF0B2A27),
    positive = Color(0xFF34D399),
    negative = Color(0xFFF87171),
    avatarPalette = AvatarPalette,
    avatarOverflow = Gray700,
)
