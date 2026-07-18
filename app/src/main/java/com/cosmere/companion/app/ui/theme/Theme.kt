package com.cosmere.companion.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette inspired by stormlight-infused spheres: deep navy, gold, and pale blue glow.
private val StormNavy = Color(0xFF16223D)
private val StormNavyLight = Color(0xFF24365C)
private val SphereGold = Color(0xFFC8A24B)
private val StormlightBlue = Color(0xFF7FB4E8)
private val PaleGlow = Color(0xFFE8F1FF)
private val ComplicationRed = Color(0xFFB33A3A)

private val DarkColors = darkColorScheme(
    primary = StormlightBlue,
    onPrimary = StormNavy,
    secondary = SphereGold,
    onSecondary = StormNavy,
    background = Color(0xFF0E1626),
    onBackground = PaleGlow,
    surface = StormNavy,
    onSurface = PaleGlow,
    surfaceVariant = StormNavyLight,
    onSurfaceVariant = Color(0xFFB9C6DE),
    error = ComplicationRed,
)

private val LightColors = lightColorScheme(
    primary = StormNavy,
    onPrimary = PaleGlow,
    secondary = SphereGold,
    onSecondary = StormNavy,
    background = Color(0xFFF4F7FC),
    onBackground = StormNavy,
    surface = Color(0xFFFFFFFF),
    onSurface = StormNavy,
    surfaceVariant = Color(0xFFDFE7F2),
    onSurfaceVariant = StormNavyLight,
    error = ComplicationRed,
)

@Composable
fun CosmereCompanionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
