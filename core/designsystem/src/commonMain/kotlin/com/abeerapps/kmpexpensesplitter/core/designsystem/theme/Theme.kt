package com.abeerapps.kmpexpensesplitter.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing scale. Use these instead of literal dp values in screens.
 *
 * App-wide rules:
 * - Space between views never exceeds [md] (16dp), so the scale stops there.
 * - Every screen uses [screenHorizontal] (16dp) as its left/right margin.
 */
@Immutable
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val smd: Dp = 12.dp,
    val md: Dp = 16.dp,
    /** Left/right margin of every screen and sheet. */
    val screenHorizontal: Dp = 16.dp,
)

/** Border widths. The design uses a 0.73dp hairline for inputs and cards. */
@Immutable
data class Borders(
    val hairline: Dp = 0.73.dp,
)

private val LocalSpacing = staticCompositionLocalOf { Spacing() }
private val LocalBorders = staticCompositionLocalOf { Borders() }
private val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/** Inputs, buttons, cards and menus all use a 16dp radius in the design. */
private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

/**
 * Root theme for every screen on Android and iOS.
 *
 * @param darkTheme follows the OS setting by default.
 */
@Composable
fun ExpenseSplitterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalExtendedColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

/** Accessors for design tokens not covered by [MaterialTheme]. */
object ExpenseSplitterTheme {
    val spacing: Spacing
        @Composable get() = LocalSpacing.current

    val borders: Borders
        @Composable get() = LocalBorders.current

    val extendedColors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
