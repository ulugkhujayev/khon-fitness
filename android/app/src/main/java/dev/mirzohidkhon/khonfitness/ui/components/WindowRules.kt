package dev.mirzohidkhon.khonfitness.ui.components

import dev.mirzohidkhon.khonfitness.data.EatingWindow
import dev.mirzohidkhon.khonfitness.data.WindowDay
import dev.mirzohidkhon.khonfitness.data.iso
import java.time.LocalDate
import java.time.LocalDateTime

/** One cell of the eating window's 28-day strip: kept the fixed hours, changed them by hand, or today (not settled yet). */
enum class StripCell { KEPT, CHANGED, TODAY }

/** The last 28 days, oldest first: a day with no manual open and no manual close kept the hours. */
fun windowStrip(days: List<WindowDay>, today: LocalDate): List<StripCell> {
    val byDate = days.associateBy { it.date }
    return (27 downTo 0).map { back ->
        val d = byDate[today.minusDays(back.toLong()).iso()]
        when {
            back == 0 -> StripCell.TODAY
            d == null || (d.openedAt == null && d.closedAt == null) -> StripCell.KEPT
            else -> StripCell.CHANGED
        }
    }
}

/** "Kept the hours on 20 of 27 days.": the settled days of the strip, today left out. */
fun stripSummary(cells: List<StripCell>): String {
    val settled = cells.filter { it != StripCell.TODAY }
    return "Kept the hours on ${settled.count { it == StripCell.KEPT }} of ${settled.size} days."
}

/** Where things sit on a day drawn from 00:00 to 24:00, as shares of it: the fixed hours, and now. */
data class DayShares(val from: Float, val to: Float, val now: Float)

fun dayShares(w: EatingWindow, now: LocalDateTime): DayShares {
    val day = 24f * 60f
    return DayShares(w.startMinute / day, w.endMinute / day, (now.hour * 60 + now.minute) / day)
}

/**
 * Left edges of the day bar's two time labels on a bar [width] wide: each centered under its end of the hours
 * ([from] and [to], in the same unit), kept inside the bar, and at least [gap] apart when short hours bring them close.
 */
fun dayBarLabels(from: Float, to: Float, width: Float, startWidth: Float, endWidth: Float, gap: Float): Pair<Float, Float> {
    var a = (from - startWidth / 2).coerceIn(0f, maxOf(0f, width - startWidth))
    var b = (to - endWidth / 2).coerceIn(0f, maxOf(0f, width - endWidth))
    val overlap = a + startWidth + gap - b
    if (overlap > 0) {
        a -= overlap / 2
        b += overlap / 2
        if (a < 0f) { b -= a; a = 0f }
        if (b + endWidth > width) { a = maxOf(0f, a - (b + endWidth - width)); b = maxOf(0f, width - endWidth) }
    }
    return a to b
}

/** Left edge of the [mark]-wide now mark at share [now] of a bar [width] wide, kept inside the bar. */
fun dayBarMark(now: Float, width: Float, mark: Float): Float = (now * width - mark / 2).coerceIn(0f, maxOf(0f, width - mark))
