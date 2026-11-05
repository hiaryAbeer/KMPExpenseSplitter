package com.abeerapps.kmpexpensesplitter.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale (4dp grid). Use these instead of literal dp values in screens. */
@Immutable
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
)

private val LocalSpacing = staticCompositionLocalOf { Spacing() }

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
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

/** Accessors for design tokens not covered by [MaterialTheme]. */
object ExpenseSplitterTheme {
    val spacing: Spacing
        @Composable get() = LocalSpacing.current
}
