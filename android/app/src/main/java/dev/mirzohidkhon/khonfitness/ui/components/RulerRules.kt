package dev.mirzohidkhon.khonfitness.ui.components

import kotlin.math.roundToInt

/**
 * A horizontal ruler picker: ticks [step] apart in value and [gap] dp apart on screen, a labelled major tick every
 * [majorEvery] ticks and a half tick between two of them.
 */
data class Ruler(val min: Double, val max: Double, val step: Double, val gap: Float, val majorEvery: Int)

enum class TickKind { MAJOR, HALF, MINOR }

/** Number of ticks, both ends included. */
fun Ruler.tickCount(): Int = ((max - min) / step).roundToInt() + 1

private fun Ruler.clampIndex(i: Int): Int = i.coerceIn(0, tickCount() - 1)

/** The tick nearest to a value. A value off the ruler lands on its nearer end. */
fun Ruler.tickIndex(value: Double): Int = clampIndex(((value - min) / step).roundToInt())

/** The value of a tick, rounded so steps of 0.1 do not drift (80.6, not 80.60000000000001). */
fun Ruler.tickValue(index: Int): Double = Math.round((min + clampIndex(index) * step) * 1e6) / 1e6

/** Scroll offset (dp) that puts the value under the center line: tick i sits i × gap from the first. */
fun Ruler.offsetOf(value: Double): Float = tickIndex(value) * gap

/** The value under the center line at a scroll offset (dp). */
fun Ruler.valueAt(offset: Float): Double = tickValue((offset / gap).roundToInt())

/** The offset (dp) a fling that would end at [offset] stops at: the nearest tick, inside the ruler. */
fun Ruler.snapOffset(offset: Float): Float = clampIndex((offset / gap).roundToInt()) * gap

fun Ruler.tickKind(index: Int): TickKind = when {
    index % majorEvery == 0 -> TickKind.MAJOR
    majorEvery % 2 == 0 && index % (majorEvery / 2) == 0 -> TickKind.HALF
    else -> TickKind.MINOR
}

/** The largest scroll offset (dp): the last tick under the center line. */
fun Ruler.maxOffset(): Float = (tickCount() - 1) * gap

/** A scroll offset (dp) kept on the ruler. */
fun Ruler.clampOffset(offset: Float): Float = offset.coerceIn(0f, maxOffset())

/** A fling stops at either end of the ruler. */
fun Ruler.atEnd(offset: Float): Boolean = offset <= 0f || offset >= maxOffset()

/** What a typed figure sets: a number above 0, else nothing ("", "0", "0.", "."). */
fun typedFigure(text: String): Double? = typedNumber(text)?.takeIf { it > 0 }

/**
 * Who moves a ruler picker, one owner at a time. The user's own gesture (a drag and the fling and settle after it)
 * moves it and reports each new tick. A value from outside (typed, a screen reader adjust, a reset) stops that
 * gesture and moves the ruler to it with no report, so a fling never writes over newer input.
 */
class RulerMotion(private val ruler: Ruler, value: Double) {
    /** The offset (dp) of the value under the center line. */
    var offset: Float = ruler.offsetOf(value)
        private set

    /** The value the ruler shows: its last report, or the last outside value. A value equal to it moves nothing. */
    var shown: Double = value
        private set

    private var last = 0
    private var owner = 0

    /** A drag starts and takes the ruler from any earlier gesture. Returns its id for [move] and [end]. */
    fun start(): Int {
        owner = ++last
        return owner
    }

    /** Whether gesture [id] still owns the ruler: no outside value and no newer drag since it started. */
    fun owns(id: Int): Boolean = id != 0 && owner == id

    /** Gesture [id] settled on a tick. */
    fun end(id: Int) {
        if (owner == id) owner = 0
    }

    /**
     * A frame of gesture [id] at [to] dp, while the parent holds [held]. A [held] value that is not the one the ruler
     * shows came from outside since the last frame: it wins (see [outside]). Returns the new tick's value to report,
     * or null: the same tick, a gesture that lost the ruler, or outside input.
     */
    fun move(id: Int, to: Float, held: Double): Double? {
        if (outside(held) || !owns(id)) return null
        offset = ruler.clampOffset(to)
        val v = ruler.valueAt(offset)
        if (v == shown) return null
        shown = v
        return v
    }

    /** A value from outside. When it is new, any gesture loses the ruler and the ruler moves to it; returns whether it moved. */
    fun outside(value: Double): Boolean {
        if (value == shown) return false
        owner = 0
        shown = value
        offset = ruler.offsetOf(value)
        return true
    }
}
