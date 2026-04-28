package com.luvst.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = DeepRose,
    onPrimary = WarmWhite,
    primaryContainer = SoftPink,
    onPrimaryContainer = DarkBrown,
    secondary = Coral,
    onSecondary = WarmWhite,
    secondaryContainer = SoftPeach,
    onSecondaryContainer = DarkBrown,
    tertiary = Lavender,
    onTertiary = DarkBrown,
    tertiaryContainer = Lavender,
    onTertiaryContainer = DarkBrown,
    background = Cream,
    onBackground = Charcoal,
    surface = WarmWhite,
    onSurface = Charcoal,
    surfaceVariant = SoftGray,
    onSurfaceVariant = MediumGray,
    error = HeartRed,
    onError = WarmWhite,
    outline = MediumGray,
    outlineVariant = SoftGray
)

private val DarkColorScheme = darkColorScheme(
    primary = Rose,
    onPrimary = DarkBrown,
    primaryContainer = DeepRose,
    onPrimaryContainer = WarmWhite,
    secondary = Coral,
    onSecondary = WarmWhite,
    secondaryContainer = Charcoal,
    onSecondaryContainer = SoftGray,
    tertiary = Lavender,
    onTertiary = DarkBrown,
    tertiaryContainer = Charcoal,
    onTertiaryContainer = SoftGray,
    background = DarkBrown,
    onBackground = SoftGray,
    surface = Charcoal,
    onSurface = WarmWhite,
    surfaceVariant = Charcoal,
    onSurfaceVariant = MediumGray,
    error = HeartRed,
    onError = WarmWhite,
    outline = MediumGray,
    outlineVariant = Charcoal
)

@Composable
fun LuvstTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
