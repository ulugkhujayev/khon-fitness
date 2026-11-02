package dev.mirzohidkhon.khonfitness.ui.components

import kotlin.math.ceil
import kotlin.math.roundToLong
import kotlin.math.sqrt

/** How long a − or + has to be held before it repeats (ms). */
const val REPEAT_AFTER_MS = 350L

/** Delay before the nth repeat of a held − or +: 160 ms, 15 ms faster each time, down to 50. */
fun repeatDelay(n: Int): Long = maxOf(50L, 160L - n * 15L)

/** One step of a number widget from [base], inside its bounds, rounded so steps of 0.1 or 2.5 do not drift. */
fun stepValue(base: Double, sign: Int, step: Double, min: Double = 0.0, max: Double = Double.POSITIVE_INFINITY): Double =
    bound(((base + sign * step) * 10000).roundToLong().toDouble().div(10000), min, max)

/** Width of a chart's value axis: its widest measured label plus the gap to the plot, so the axis hugs its labels. */
fun axisWidth(labelWidths: List<Float>, gap: Float): Float = ceil(labelWidths.maxOrNull() ?: 0f) + gap

/**
 * What a search that found nothing says (iOS ContentUnavailableView.search): "No Results for “row”" and what to
 * try. Without a query only the filters can be to blame.
 */
fun noResults(query: String): Pair<String, String> {
    val q = query.trim()
    return if (q.isNotEmpty()) "No Results for “$q”" to "Check the spelling or try a new search." else "No Results" to "Try another filter."
}

/** Whether a text matches a search, ignoring case and the spaces around the query; an empty query matches all. */
fun matchesQuery(text: String, query: String): Boolean {
    val q = query.trim()
    return q.isEmpty() || text.contains(q, ignoreCase = true)
}

/** [y] mapped from 0..[distance] onto [from]..[to], clamped at both ends. */
fun fold(y: Float, distance: Float, from: Float, to: Float): Float {
    val t = (y / distance).coerceIn(0f, 1f)
    return from + (to - from) * t
}

/**
 * Geometry of the large title that folds as a tab screen scrolls (iOS large title), in dp. [row] is the title
 * row's height: a tap target, or taller when a large font scale makes the title line taller.
 */
class LargeTitleFold(val row: Float = 44f) {
    /** Room above the title row. */
    val rowTop = 4f
    /** Height of the unfolded bar; the content starts this far down. */
    val height = rowTop + row + 12f
    /** Height of the folded bar: the row and 2 dp around it. */
    val folded = row + 4f
    /** Scroll distance over which the bar's bottom edge rises with the content (60 to 48 at the default size). */
    val distance = height - folded

    /** How far the bar has moved up. */
    fun barShift(y: Float) = fold(y, distance, 0f, distance)
    /** How far the row moves down inside the rising bar, so it ends centered in the folded bar. */
    fun rowShift(y: Float) = fold(y, distance, 0f, distance + (folded - row) / 2 - rowTop)
    /** Title scale: 34 to about 20 over the first 40 dp. */
    fun titleScale(y: Float) = fold(y, TITLE_FOLD, 1f, TITLE_SCALE)
    /** Hairline under the bar: shows once content passes under it. */
    fun hairline(y: Float) = fold(y - (distance + 4f), 8f, 0f, 1f)

    companion object {
        const val TITLE_FOLD = 40f
        const val TITLE_SCALE = 0.6f
    }
}

/** Scroll distance (dp) over which a navigation bar's hairline fades in. */
const val NAV_HAIRLINE_FADE = 16f

/** Opacity of a navigation bar's hairline after [y] dp of scroll. */
fun navHairline(y: Float) = fold(y, NAV_HAIRLINE_FADE, 0f, 1f)

/** Opacity of a navigation bar's hairline after [px] of scroll on a screen of [density] px per dp. */
fun barHairline(px: Int, density: Float) = navHairline(px / density)

/** Scroll offset (px) of a lazy list for a navigation bar: past the first item it counts as scrolled all the way. */
fun listBarOffset(firstIndex: Int, firstOffsetPx: Int): Int = if (firstIndex > 0) Int.MAX_VALUE else firstOffsetPx

/** A spring given by mass, stiffness and damping, as Compose takes it: the ratio to critical damping and the stiffness at mass 1. */
data class SpringSpec(val dampingRatio: Float, val stiffness: Float)

fun springOf(damping: Float, stiffness: Float, mass: Float): SpringSpec =
    SpringSpec(damping / (2f * sqrt(stiffness * mass)), stiffness / mass)
