package dev.logickoder.retrostash.example.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


private val Brand = Color(0xFF6750A4)
private val BrandSecondary = Color(0xFF625B71)

private val LightScheme = lightColorScheme(
    primary = Brand,
    secondary = BrandSecondary,
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
)

@Composable
fun RetrostashTheme(useDark: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (useDark) DarkScheme else LightScheme,
        content = content,
    )
}