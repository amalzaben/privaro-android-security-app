package com.example.privaro.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrivaroTealLight,
    onPrimary = Color.Black,
    primaryContainer = PrivaroTealDark,
    onPrimaryContainer = Color.White,
    secondary = PrivaroTeal,
    secondaryContainer = Color(0xFF1A3D3D),
    onSecondaryContainer = Color.White,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2D3D3D),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0B0B0)
)

private val LightColorScheme = lightColorScheme(
    primary = PrivaroTeal,
    onPrimary = Color.White,
    primaryContainer = PrivaroTealLight,
    onPrimaryContainer = PrivaroTealDark,
    secondary = PrivaroTealDark,
    secondaryContainer = Color(0xFFE0F7F7),
    onSecondaryContainer = PrivaroTealDark,
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Color(0xFFF5FAFA),
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFF666666)
)

@Composable
fun PrivaroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled for consistent teal branding
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

    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
}