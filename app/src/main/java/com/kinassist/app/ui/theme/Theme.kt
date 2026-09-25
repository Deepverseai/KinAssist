package com.kinassist.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = TextOnPrimary,
    primaryContainer = GoldContainer,
    onPrimaryContainer = Color.White,
    secondary = TerracottaSOS,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF571E14),
    onSecondaryContainer = Color(0xFFFFDAD4),
    tertiary = EmeraldTertiary,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF005234),
    onTertiaryContainer = Color(0xFF75F8BE),
    background = DeepCanvas,
    onBackground = TextOnSurfacePrimary,
    surface = SurfaceContainer,
    onSurface = TextOnSurfacePrimary,
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = TextOnSurfaceVariant,
    outline = OutlineBorder
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF856E16),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE088),
    onPrimaryContainer = Color(0xFF2B2100),
    secondary = Color(0xFFA83823),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD4),
    onSecondaryContainer = Color(0xFF3B0904),
    tertiary = Color(0xFF006C47),
    onTertiary = Color.White,
    background = Color(0xFFF9F9FB),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE2E2E9),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFFC4C6D0)
)

@Composable
fun KinAssistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
