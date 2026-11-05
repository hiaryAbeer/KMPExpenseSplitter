package com.abeerapps.kmpexpensesplitter.ui

import androidx.compose.runtime.Composable

/**
 * Makes status/navigation bar icons readable for the app's theme, which can
 * differ from the system's when the user picks Light or Dark in Settings.
 */
@Composable
expect fun SystemBarsAppearance(darkTheme: Boolean)
