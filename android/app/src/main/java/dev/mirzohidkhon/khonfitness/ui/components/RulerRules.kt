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
