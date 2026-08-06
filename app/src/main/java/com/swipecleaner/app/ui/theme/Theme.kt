package com.swipecleaner.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    background = PsColor.BgDark,
    onBackground = PsColor.TextDark,
    surface = PsColor.BgDark,
    onSurface = PsColor.TextDark,
    surfaceVariant = PsColor.CardDark,
    onSurfaceVariant = PsColor.subText(PsColor.TextDark),
    primary = PsColor.Blue,
    onPrimary = Color.White,
    primaryContainer = PsColor.CardDark,
    onPrimaryContainer = PsColor.TextDark,
    secondary = PsColor.Green,
    onSecondary = Color.White,
    secondaryContainer = PsColor.CardDark,
    onSecondaryContainer = PsColor.TextDark,
    tertiary = PsColor.Yellow,
    onTertiary = PsColor.BgDark,
    tertiaryContainer = PsColor.CardDark,
    onTertiaryContainer = PsColor.TextDark,
    error = PsColor.Orange,
    onError = Color.White,
    errorContainer = PsColor.CardDark,
    onErrorContainer = PsColor.TextDark,
    outline = PsColor.BorderDark,
    outlineVariant = PsColor.DividerDark
)

private val LightColors = lightColorScheme(
    background = PsColor.BgLight,
    onBackground = PsColor.TextLight,
    surface = PsColor.BgLight,
    onSurface = PsColor.TextLight,
    surfaceVariant = PsColor.CardLight,
    onSurfaceVariant = PsColor.subText(PsColor.TextLight),
    primary = PsColor.Blue,
    onPrimary = Color.White,
    primaryContainer = PsColor.CardLight,
    onPrimaryContainer = PsColor.TextLight,
    secondary = PsColor.Green,
    onSecondary = Color.White,
    secondaryContainer = PsColor.CardLight,
    onSecondaryContainer = PsColor.TextLight,
    tertiary = PsColor.Yellow,
    onTertiary = PsColor.TextLight,
    tertiaryContainer = PsColor.CardLight,
    onTertiaryContainer = PsColor.TextLight,
    error = PsColor.Orange,
    onError = Color.White,
    errorContainer = PsColor.CardLight,
    onErrorContainer = PsColor.TextLight,
    outline = PsColor.BorderLight,
    outlineVariant = PsColor.DividerLight
)

/**
 * Tema raíz de PhotoSwipeCleaner. Sigue el tema del sistema
 * (`isSystemInDarkTheme()`) — no hay override manual en Ajustes todavía;
 * se puede agregar después si se quiere forzar claro/oscuro desde la app.
 */
@Composable
fun PhotoSwipeCleanerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = PsTypography,
        shapes = PsShapes,
        content = content
    )
}
