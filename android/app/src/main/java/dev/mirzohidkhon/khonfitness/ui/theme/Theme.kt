package dev.mirzohidkhon.khonfitness.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.mirzohidkhon.khonfitness.R

/**
 * iOS dark-appearance semantic palette (Human Interface Guidelines, Color and Dark Mode).
 * Names stay the ones the screens already use; the values follow Apple's dark system colors.
 */
object K {
    /** systemGroupedBackground, base level. */
    val Bg = Color(0xFF000000)
    /** secondarySystemGroupedBackground: the inset group surface. */
    val Surface = Color(0xFF1C1C1E)
    /** tertiarySystemGroupedBackground: fills inside a group (inputs, steppers). */
    val Surface2 = Color(0xFF2C2C2E)
    /** systemGray4: pressed fill and strong borders. */
    val Surface3 = Color(0xFF3A3A3C)
    /** separator over the group surface. */
    val Divider = Color(0xFF38383A)
    /** label. */
    val Text = Color(0xFFFFFFFF)
    /** secondaryLabel: EBEBF5 at 60 percent over black. */
    val Muted = Color(0xFF8E8E93)
    /** tertiaryLabel: EBEBF5 at 30 percent over black. */
    val Dim = Color(0xFF636366)
    /** systemOrange, the app accent. */
    val Accent = Color(0xFFFF9F0A)
    /** Label on an accent fill. Apple uses white on prominent buttons. */
    val AccentInk = Color(0xFFFFFFFF)
    val AccentSoft = Color(0x33FF9F0A)
    /** systemGreen. */
    val Green = Color(0xFF30D158)
    val GreenInk = Color(0xFFFFFFFF)
    /** systemBlue. */
    val Blue = Color(0xFF0A84FF)
    /** systemRed. */
    val Red = Color(0xFFFF453A)
    /** Elevated group surface for sheets. */
    val Elevated = Color(0xFF2C2C2E)
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

/** Inter, bundled: the closest open typeface to SF Pro. A phone-wide custom font does not change the app. */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun t(size: Int, weight: FontWeight, line: Int, spacing: Float = 0f) =
    TextStyle(fontFamily = Inter, fontSize = size.sp, fontWeight = weight, lineHeight = line.sp, letterSpacing = spacing.sp)

/**
 * The iOS text styles at the default (Large) size, mapped onto Material slots:
 * headlineLarge = Large Title 34/41 bold · headlineMedium = Title 1 28/34 bold · titleLarge = Title 2 22/28 bold
 * titleMedium = Headline 17/22 semibold · bodyLarge = Body 17/22 · bodyMedium = Subheadline 15/20
 * bodySmall = Footnote 13/18 · labelLarge = Body 17 (buttons) · labelMedium = Caption 1 12/16 · labelSmall = Caption 2 11/13
 */
val KhonTypography = Typography(
    headlineLarge = t(34, FontWeight.Bold, 41, -0.4f),
    headlineMedium = t(28, FontWeight.Bold, 34, -0.3f),
    titleLarge = t(22, FontWeight.Bold, 28, -0.2f),
    titleMedium = t(17, FontWeight.SemiBold, 22, -0.2f),
    bodyLarge = t(17, FontWeight.Normal, 22, -0.2f),
    bodyMedium = t(15, FontWeight.Normal, 20, -0.1f),
    bodySmall = t(13, FontWeight.Normal, 18),
    labelLarge = t(17, FontWeight.Normal, 22, -0.2f),
    labelMedium = t(12, FontWeight.Normal, 16),
    labelSmall = t(11, FontWeight.Normal, 13),
)

@Composable
fun KhonTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = KhonTypography) {
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalTextStyle provides KhonTypography.bodyLarge, content = content)
    }
}
