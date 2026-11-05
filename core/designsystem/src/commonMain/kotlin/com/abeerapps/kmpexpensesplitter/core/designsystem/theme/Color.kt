package com.abeerapps.kmpexpensesplitter.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand palette: a calm teal ("money that's settled") with a warm coral accent.
private val Teal10 = Color(0xFF00201C)
private val Teal20 = Color(0xFF003731)
private val Teal30 = Color(0xFF005048)
private val Teal40 = Color(0xFF006B60)
private val Teal80 = Color(0xFF6FD8C8)
private val Teal90 = Color(0xFF8DF5E4)

private val Coral10 = Color(0xFF3A0B00)
private val Coral20 = Color(0xFF5E1700)
private val Coral30 = Color(0xFF842500)
private val Coral40 = Color(0xFFA9380F)
private val Coral80 = Color(0xFFFFB59E)
private val Coral90 = Color(0xFFFFDBD0)

private val Neutral10 = Color(0xFF191C1B)
private val Neutral90 = Color(0xFFE0E3E1)
private val Neutral95 = Color(0xFFEFF1EF)
private val Neutral99 = Color(0xFFFAFDFB)
private val NeutralVariant30 = Color(0xFF3F4946)
private val NeutralVariant50 = Color(0xFF6F7976)
private val NeutralVariant80 = Color(0xFFBEC9C5)
private val NeutralVariant90 = Color(0xFFDAE5E1)

internal val LightColors = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = Coral40,
    onSecondary = Color.White,
    secondaryContainer = Coral90,
    onSecondaryContainer = Coral10,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    outline = NeutralVariant50,
)

internal val DarkColors = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,
    secondary = Coral80,
    onSecondary = Coral20,
    secondaryContainer = Coral30,
    onSecondaryContainer = Coral90,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    outline = Neutral95.copy(alpha = 0.4f),
)
