package com.panther742.panther.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PantherColors = darkColorScheme(
    primary = Cyan,
    onPrimary = Ink,
    secondary = Violet,
    onSecondary = Ink,
    tertiary = Fuchsia,
    background = Ink,
    onBackground = TextHi,
    surface = InkSoft,
    onSurface = TextHi,
    surfaceVariant = SlateGlass,
    onSurfaceVariant = TextMid,
    outline = SlateLine,
    error = AlertRed,
    onError = Ink,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0891B2),
    secondary = Color(0xFF7C3AED),
    background = Color(0xFFF4F6FB),
    surface = Color(0xFFFFFFFF),
)

@Composable
fun PantherTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) PantherColors else LightColors,
        typography = PantherTypography,
        content = content,
    )
}
