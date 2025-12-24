package com.brunocsantos.agoravai.ui.theme

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = Color(0xFF003733),
    primaryContainer = TealContainerDark,
    onPrimaryContainer = Color(0xFF88F7E8),
    secondary = MintSecondaryDark,
    onSecondary = Color(0xFF00382B),
    secondaryContainer = MintContainerDark,
    onSecondaryContainer = Color(0xFFA0F2D6),
    background = Color(0xFF191C1B),
    surface = Color(0xFF191C1B)
)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = Color(0xFF00201C),
    secondary = MintSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = MintContainerLight,
    onSecondaryContainer = Color(0xFF002118),
    background = Color(0xFFFAFDFB),
    surface = Color(0xFFFAFDFB)
)

@Composable
fun AgoraVaiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Mantemos em 'false' para garantir nosso tema
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
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}