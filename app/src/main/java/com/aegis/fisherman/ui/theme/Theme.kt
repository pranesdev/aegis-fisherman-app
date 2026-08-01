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
    val DeepSeaBlue = Color(0xFF0B3D5C)
    val Foam = Color(0xFFF2F7FA)
    val SunGold = Color(0xFFF2A007)

    val ZoneSafe = Color(0xFF1E8E5A)
    val ZoneWarning = Color(0xFFF2A007)
    val ZoneDanger = Color(0xFFD32F2F)
    val ZoneUnknown = Color(0xFF757575)
}

private val DarkColors = darkColorScheme(
    primary = AegisColors.SunGold,
    secondary = AegisColors.DeepSeaBlue,
    background = Color(0xFF07293F),
    surface = Color(0xFF0B3D5C),
    onPrimary = Color.Black,
    onBackground = AegisColors.Foam,
    onSurface = AegisColors.Foam
)

private val LightColors = lightColorScheme(
    primary = AegisColors.DeepSeaBlue,
    secondary = AegisColors.SunGold,
    background = AegisColors.Foam,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = AegisColors.DeepSeaBlue,
    onSurface = AegisColors.DeepSeaBlue
)

private val AegisTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 48.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp)
)

@Composable
fun AegisTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AegisTypography,
        content = content
    )
}
