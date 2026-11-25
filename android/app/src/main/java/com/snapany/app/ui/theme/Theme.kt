package com.snapany.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val CosmicBg = Color(0xFF07090E)
val SurfaceGlass = Color(0xFF101522)
val SurfaceBorder = Color(0x18FFFFFF)
val IndigoAccent = Color(0xFF6366F1)
val PurpleAccent = Color(0xFFA855F7)
val PinkAccent = Color(0xFFEC4899)
val EmeraldOnline = Color(0xFF10B981)
val ErrorRed = Color(0xFFF87171)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

val PrimaryGradient = Brush.linearGradient(
    colors = listOf(IndigoAccent, PurpleAccent, PinkAccent)
)

private val DarkScheme = darkColorScheme(
    primary = IndigoAccent,
    secondary = PurpleAccent,
    tertiary = PinkAccent,
    background = CosmicBg,
    surface = SurfaceGlass,
    onPrimary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = ErrorRed
)

@Composable
fun SnapAnyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        content = content
    )
}
