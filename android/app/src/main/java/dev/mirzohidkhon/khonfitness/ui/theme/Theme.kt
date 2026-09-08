package dev.mirzohidkhon.khonfitness.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

object K {
    val Bg = Color(0xFF0E0E10)
    val Surface = Color(0xFF1A1A1D)
    val Surface2 = Color(0xFF26262A)
    val Surface3 = Color(0xFF313136)
    val Divider = Color(0xFF2B2B30)
    val Text = Color(0xFFF4F4F5)
    val Muted = Color(0xFF9B9BA3)
    val Dim = Color(0xFF5F5F67)
    val Accent = Color(0xFFF0A35A)
    val AccentInk = Color(0xFF2A1708)
    val AccentSoft = Color(0x29F0A35A)
    val Green = Color(0xFF4CD28A)
    val GreenInk = Color(0xFF0F2418)
    val Blue = Color(0xFF71B7FF)
    val Red = Color(0xFFFF7B7B)
}

private val scheme: ColorScheme = darkColorScheme(
    primary = K.Accent,
    onPrimary = K.AccentInk,
    background = K.Bg,
    onBackground = K.Text,
    surface = K.Surface,
    onSurface = K.Text,
    surfaceVariant = K.Surface2,
    onSurfaceVariant = K.Muted,
    outline = K.Divider,
    error = K.Red,
)

val KhonTypography = Typography(
    headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 14.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 14.sp),
)

@Composable
fun KhonTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = KhonTypography, content = content)
}
