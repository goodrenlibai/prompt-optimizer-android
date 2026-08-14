package com.promptoptimizer.ui.theme

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

// 主色调（与原项目网站品牌色一致：靛蓝紫渐变）
private val LightColors = lightColorScheme(
    primary = Color(0xFF667EEA),
    onPrimary = Color.White,
    secondary = Color(0xFF764BA2),
    onSecondary = Color.White,
    primaryContainer = Color(0xFFE4E8FB),
    background = Color(0xFFF7F8FC),
    surface = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8B9CFF),
    onPrimary = Color(0xFF141A38),
    secondary = Color(0xFFC7A7EA),
    background = Color(0xFF12131A),
    surface = Color(0xFF1C1E29)
)

@Composable
fun PromptOptimizerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
