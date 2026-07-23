package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = GeoDarkPrimary,
        onPrimary = GeoOnPrimaryContainer,
        primaryContainer = GeoDarkPrimaryContainer,
        onPrimaryContainer = GeoDarkOnPrimaryContainer,
        secondary = GeoSecondaryContainer,
        onSecondary = GeoOnSecondaryContainer,
        background = GeoDarkBackground,
        onBackground = GeoDarkOnSurface,
        surface = GeoDarkSurface,
        onSurface = GeoDarkOnSurface,
        surfaceVariant = GeoDarkSurface,
        onSurfaceVariant = GeoDarkOnSurfaceVariant,
        outline = GeoOutline
    )

private val LightColorScheme =
    lightColorScheme(
        primary = GeoPrimary,
        onPrimary = GeoOnPrimary,
        primaryContainer = GeoPrimaryContainer,
        onPrimaryContainer = GeoOnPrimaryContainer,
        secondary = GeoSecondary,
        secondaryContainer = GeoSecondaryContainer,
        onSecondaryContainer = GeoOnSecondaryContainer,
        background = GeoBackground,
        onBackground = GeoOnBackground,
        surface = GeoSurface,
        onSurface = GeoOnSurface,
        surfaceVariant = GeoSurfaceVariant,
        onSurfaceVariant = GeoOnSurfaceVariant,
        outline = GeoOutline,
        outlineVariant = GeoOutlineVariant
    )

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {

  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
