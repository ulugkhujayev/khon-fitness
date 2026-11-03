package dev.mirzohidkhon.khonfitness.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.mirzohidkhon.khonfitness.R
import dev.mirzohidkhon.khonfitness.ui.screens.PRESET_COLORS

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
    /**
     * Secondary label and placeholder text: Apple's increased-contrast systemGray, so it reads at 4.5:1 or more on
     * every fill it sits on, the elevated field fill #3A3A3C included ([TextPairs]).
     */
    val Muted = Color(0xFFAEAEB2)
    /** systemGray2: chevrons, the grabber, decorative dots and off controls. Never readable text (2.8:1 on [Surface]). */
    val Dim = Color(0xFF636366)
    /** systemOrange, the app accent. */
    val Accent = Color(0xFFFF9F0A)
    /** Text and symbols on an accent fill: dark ink, 10:1 (white on orange is 2.1:1). */
    val AccentInk = Color(0xFF000000)
    val AccentSoft = Color(0x33FF9F0A)
    /** systemGreen. */
    val Green = Color(0xFF30D158)
    /** Text and symbols on a green fill (the done check). */
    val GreenInk = Color(0xFF000000)
    /** systemBlue. */
    val Blue = Color(0xFF0A84FF)
    /** systemRed, increased contrast: red text reads at 4.5:1 on the elevated fill and on its own tint. */
    val Red = Color(0xFFFF6961)
    /** Text on a red fill (a destructive confirm). */
    val RedInk = Color(0xFF000000)
    /** Elevated group surface for sheets. */
    val Elevated = Color(0xFF2C2C2E)
    /** 1 dp edge of the floating tab bar: white at 6 percent. */
    val Hairline = Color(0x0FFFFFFF)
    /** The interval timer's and the stretch player's fill in a work phase (a stretch is a work phase). */
    val WorkFill = Color(0xFF14532D)
    /** The interval timer's fill in a rest phase. */
    val RestFill = Color(0xFF1E3A5F)
    /** The interval timer's fill in the warm-up. */
    val WarmupFill = Color(0xFF3A3A40)
    /** Secondary text on the player fills and on black: white at 75 percent, 5.9:1 on [WorkFill] (60 percent was 4.4:1). */
    val OnPhaseMuted = Color.White.copy(alpha = 0.75f)
}

/**
 * The symbol color on a solid fill of any color, such as an [dev.mirzohidkhon.khonfitness.ui.components.IconTile]:
 * black or white, whichever reads better. One of the two is 4.5:1 or more on every opaque color.
 */
fun inkOn(fill: Color): Color = if (contrast(Color.Black, fill) >= contrast(Color.White, fill)) Color.Black else Color.White

/** The share of its color a tinted fill takes ([dev.mirzohidkhon.khonfitness.ui.components.TintedButton]): its text stays at 4.5:1 on it. */
const val TINT_ALPHA = 0.16f

/** A [fg] text or symbol color on a [bg] fill, as the app uses it. [large] is 18 sp and up bold, or a symbol: it needs only 3:1. */
data class TextPair(val name: String, val fg: Color, val bg: Color, val large: Boolean = false)

/** [fg] at [TINT_ALPHA] over [under]: the fill of a tinted button. */
fun tinted(fg: Color, under: Color): Color = fg.copy(alpha = TINT_ALPHA).compositeOver(under)

/**
 * Every color pair the app uses for readable text and for symbols that carry meaning, the timer's and the stretch
 * player's fills included. ContrastTest holds each to WCAG AA: 4.5:1, 3:1 for large text and symbols. It also fails a
 * raw color for text in the screens, so a new pair has to come through a token here.
 * Off controls ([K.Dim] text) are left out, as WCAG 1.4.3 exempts inactive controls. [K.Elevated] equals [K.Surface2].
 */
val TextPairs: List<TextPair> = buildList {
    for ((n, bg) in listOf("Bg" to K.Bg, "Surface" to K.Surface, "Surface2" to K.Surface2)) {
        add(TextPair("Text on $n", K.Text, bg))
        add(TextPair("Muted on $n", K.Muted, bg))
        add(TextPair("Accent on $n", K.Accent, bg))
        add(TextPair("Green on $n", K.Green, bg))
        add(TextPair("Red on $n", K.Red, bg))
    }
    // The selected segment, a selected chip, and the elevated number field's unit and placeholder.
    add(TextPair("Text on Surface3", K.Text, K.Surface3))
    add(TextPair("Muted on Surface3", K.Muted, K.Surface3))
    // Ink on a fill: Start, Save, Done, Done set check, stretch steps, the picked date, a destructive confirm.
    add(TextPair("AccentInk on Accent", K.AccentInk, K.Accent))
    add(TextPair("GreenInk on Green", K.GreenInk, K.Green))
    add(TextPair("RedInk on Red", K.RedInk, K.Red))
    // Index badges and today's cell on the soft accent; tinted buttons on the page and on a card or sheet.
    add(TextPair("Accent on AccentSoft over Bg", K.Accent, K.AccentSoft.compositeOver(K.Bg)))
    add(TextPair("Accent on AccentSoft over Surface", K.Accent, K.AccentSoft.compositeOver(K.Surface)))
    for ((n, under) in listOf("Bg" to K.Bg, "Surface" to K.Surface)) {
        add(TextPair("Accent on its tint over $n", K.Accent, tinted(K.Accent, under)))
        add(TextPair("Red on its tint over $n", K.Red, tinted(K.Red, under)))
        // "New" and "PR" chips (LabelChip, 18 percent) and the Log's today row (12 percent).
        add(TextPair("Green on its chip over $n", K.Green, K.Green.copy(alpha = 0.18f).compositeOver(under)))
        add(TextPair("Muted on the today row over $n", K.Muted, K.Accent.copy(alpha = 0.12f).compositeOver(under)))
    }
    // The timer and the stretch player: white and [K.OnPhaseMuted] on each phase fill and on black (paused, done).
    for ((n, fill) in listOf("Bg" to K.Bg, "WorkFill" to K.WorkFill, "RestFill" to K.RestFill, "WarmupFill" to K.WarmupFill)) {
        add(TextPair("Text on the player over $n", K.Text, fill))
        add(TextPair("OnPhaseMuted on the player over $n", K.OnPhaseMuted.compositeOver(fill), fill))
        // The player's round Stop, Back and Skip: a white symbol on white at 18 percent. A symbol needs 3:1.
        add(TextPair("Text on a player button over $n", K.Text, Color.White.copy(alpha = 0.18f).compositeOver(fill), large = true))
    }
    // A cardio type's own color: its name on the Plan list's type pill, its symbol on its circle (20 percent).
    for (c in PRESET_COLORS.map { Color(it) }) {
        val hex = "%06X".format(c.value.toLong() ushr 32 and 0xFFFFFF)
        add(TextPair("Type #$hex on its pill", c, K.Surface2))
        for ((n, under) in listOf("Bg" to K.Bg, "Surface" to K.Surface)) add(TextPair("Type #$hex on its circle over $n", c, c.copy(alpha = 0.2f).compositeOver(under), large = true))
    }
    // The demonstration's round controls: a white symbol on black at 45 percent over the light image.
    add(TextPair("Text on the demo glass", K.Text, Color.Black.copy(alpha = 0.45f).compositeOver(Color(0xFFF2F2F2)), large = true))
}

/** WCAG 2.1 contrast ratio of two opaque colors, 1 to 21. */
fun contrast(a: Color, b: Color): Double {
    val (lo, hi) = listOf(a.luminance().toDouble(), b.luminance().toDouble()).sorted()
    return (hi + 0.05) / (lo + 0.05)
}

/** Motion shared across the app. */
object Motion {
    /** A selection sliding to its new place (the tab bar's capsule, a segmented control): quick, no wobble. Damping 20, stiffness 240, mass 0.8. */
    private val slideSpring = dev.mirzohidkhon.khonfitness.ui.components.springOf(damping = 20f, stiffness = 240f, mass = 0.8f)
    fun <T> slide(): androidx.compose.animation.core.SpringSpec<T> = androidx.compose.animation.core.spring(dampingRatio = slideSpring.dampingRatio, stiffness = slideSpring.stiffness)
    /** Tab screens shift sideways as they swap: 50 dp over 150 ms. */
    const val SHIFT_MS = 150
    const val SHIFT_DP = 50
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
