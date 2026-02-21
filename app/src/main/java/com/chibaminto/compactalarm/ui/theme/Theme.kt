package com.chibaminto.compactalarm.ui.theme

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

// ダークテーマのカラー
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),      // シアン
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF004D57),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = Color(0xFF4DD0E1),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004851),
    onSecondaryContainer = Color(0xFF97F0FF),
    tertiary = Color(0xFFB2EBF2),
    onTertiary = Color.Black,
    background = Color(0xFF0D1B1E),
    onBackground = Color(0xFFE1E3E3),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE1E3E3),
    surfaceVariant = Color(0xFF1E2A2D),
    onSurfaceVariant = Color(0xFFC0C8CA),
    error = Color(0xFFCF6679),
    onError = Color.Black,
)

// ライトテーマのカラー
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006874),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF97F0FF),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = Color(0xFF4A6267),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE7EC),
    onSecondaryContainer = Color(0xFF051F23),
    tertiary = Color(0xFF525E7D),
    onTertiary = Color.White,
    background = Color(0xFFFAFDFD),
    onBackground = Color(0xFF191C1D),
    surface = Color(0xFFFAFDFD),
    onSurface = Color(0xFF191C1D),
    surfaceVariant = Color(0xFFDBE4E6),
    onSurfaceVariant = Color(0xFF3F484A),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

@Composable
fun CompactAlarmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // デフォルトでオフ（ブランドカラーを優先）
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
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
