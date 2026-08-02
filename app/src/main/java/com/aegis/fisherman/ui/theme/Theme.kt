package com.aegis.fisherman.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Deep-sea palette - readable in bright sunlight on deck, and reuses the same
// SAFE/WARNING/DANGER colors as the boat unit's own LED so the phone and the
// hardware never disagree visually.
object AegisColors {
    val DeepOcean = Color(0xFF0B1D3A)
    val TealNavy = Color(0xFF163E5F)
    val Foam = Color(0xFFF2F7FA)
    
    // Glass Palette
    val GlassWhite = Color.White.copy(alpha = 0.15f)
    val GlassBorder = Color.White.copy(alpha = 0.25f)
    
    // Status Glows (Vibrant for visibility through glass)
    val ZoneSafe = Color(0xFF00E676)
    val ZoneWarning = Color(0xFFFFD600)
    val ZoneDanger = Color(0xFFFF1744)
    val ZoneUnknown = Color(0xFF9E9E9E)

    val BackgroundGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
        listOf(DeepOcean, TealNavy)
    )
}

private val DarkColors = darkColorScheme(
    primary = AegisColors.ZoneSafe,
    secondary = AegisColors.ZoneWarning,
    background = AegisColors.DeepOcean,
    surface = AegisColors.GlassWhite,
    onPrimary = Color.Black,
    onBackground = AegisColors.Foam,
    onSurface = AegisColors.Foam
)

private val LightColors = DarkColors // Maintain high contrast/dark theme for sea use

private val AegisTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 48.sp, letterSpacing = (-1).sp),
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
)

@Composable
fun AegisTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AegisTypography,
        content = content
    )
}
