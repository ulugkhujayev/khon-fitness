package dev.mirzohidkhon.khonfitness.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import dev.mirzohidkhon.khonfitness.R
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

/** Bundled Roboto, so a phone-wide custom font (Samsung lets you pick one) does not change the app. */
val Roboto = FontFamily(
    Font(R.font.roboto_regular, FontWeight.Normal),
    Font(R.font.roboto_medium, FontWeight.Medium),
    Font(R.font.roboto_bold, FontWeight.Bold),
)

private fun t(size: Int, weight: FontWeight, line: Int, spacing: Float = 0f) =
    TextStyle(fontFamily = Roboto, fontSize = size.sp, fontWeight = weight, lineHeight = line.sp, letterSpacing = spacing.sp)

val KhonTypography = Typography(
    headlineLarge = t(32, FontWeight.Bold, 36, -0.5f),
    headlineMedium = t(26, FontWeight.Bold, 30, -0.3f),
    titleLarge = t(20, FontWeight.Bold, 24),
    titleMedium = t(17, FontWeight.Medium, 22),
    bodyLarge = t(17, FontWeight.Normal, 22),
    bodyMedium = t(15, FontWeight.Normal, 20),
    bodySmall = t(13, FontWeight.Normal, 16),
    labelLarge = t(17, FontWeight.Medium, 22),
    labelMedium = t(12, FontWeight.Medium, 14),
    labelSmall = t(11, FontWeight.Medium, 14),
)

@Composable
fun KhonTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = KhonTypography) {
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalTextStyle provides KhonTypography.bodyLarge, content = content)
    }
}
