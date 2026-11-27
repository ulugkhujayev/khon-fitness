package dev.mirzohidkhon.khonfitness.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LineChartStepTest {
    @Test fun stepsAbove100KeepThreeOrFourLines() {
        assertEquals(1.0, niceStep(3.0, 3), 0.0)
        assertEquals(100.0, niceStep(290.0, 3), 0.0)
        assertEquals(1000.0, niceStep(1568.0, 3), 0.0)
        assertEquals(1000.0, niceStep(2900.0, 3), 0.0)
    }

    @Test fun stepsAbove1000AreOneTwoTwoAndAHalfOrFiveTimesAPowerOfTen() {
        assertEquals(1000.0, niceStep(3000.0, 3), 0.0)
        assertEquals(2000.0, niceStep(3001.0, 3), 0.0)
        assertEquals(2500.0, niceStep(7000.0, 3), 0.0)
        assertEquals(5000.0, niceStep(12600.0, 3), 0.0)
        assertEquals(10000.0, niceStep(20000.0, 3), 0.0)
        assertEquals(20000.0, niceStep(50000.0, 3), 0.0)
        assertEquals(25000.0, niceStep(70000.0, 3), 0.0)
        assertEquals(500000.0, niceStep(1e6, 3), 0.0)
        assertEquals(5e8, niceStep(1e9, 3), 0.0)
    }

    @Test fun stepsUpTo3000DoNotChange() {
        val old = doubleArrayOf(0.5, 1.0, 2.0, 5.0, 10.0, 20.0, 25.0, 50.0, 100.0, 200.0, 250.0, 500.0, 1000.0)
        for (span in (0..3000).map { it.toDouble() } + listOf(0.1, 0.7, 2.9, 299.9, 2999.5)) {
            assertEquals("span $span", old.first { it >= span / 3 }, niceStep(span, 3), 0.0)
        }
    }

    @Test fun everyFiniteSpanHasAtMostFiveTicks() {
        val spans = listOf(0.0, 0.3, 2.0, 3000.0, 3001.0, 12600.0, 1e6, 1e9, 123456789.0, 9.99e15, 1e100, 1e300, Double.MAX_VALUE / 2)
        val lows = listOf(0.0, -7.0, 1668.0, 1e6 + 3)
        for (span in spans) for (low in lows) {
            val axis = valueAxis(listOf(low, low + span))
            assertTrue("span $span from $low: ${axis.ticks}", axis.ticks.size in 1..5)
            assertTrue("span $span from $low: ${axis.ticks}", axis.ticks.first() <= low && axis.ticks.last() >= low + span)
        }
        assertEquals(listOf(0.0, 5000.0, 10000.0, 15000.0), valueAxis(listOf(0.0, 12600.0)).ticks)
        assertEquals(listOf(0.0, 1000.0, 2000.0, 3000.0), valueAxis(listOf(0.0, 3000.0)).ticks)
        assertEquals(listOf(0.0, 2000.0, 4000.0), valueAxis(listOf(0.0, 3001.0)).ticks)
        assertEquals(listOf(0.0, 500000.0, 1000000.0), valueAxis(listOf(0.0, 1e6)).ticks)
        assertEquals(listOf(0.0, 5e8, 1e9), valueAxis(listOf(0.0, 1e9)).ticks)
        for (values in listOf(listOf(245110341540439968.0, 245110341540440032.0), listOf(0.0, Double.MAX_VALUE), listOf(-Double.MAX_VALUE, Double.MAX_VALUE), listOf(1e300, 1e300))) {
            val ticks = valueAxis(values).ticks
            assertTrue("$values: $ticks", ticks.size in 1..5 && ticks.all { it.isFinite() } && ticks.distinct() == ticks)
        }
        assertEquals(5, ValueAxis(0.0, 3e9, 1.0).ticks.size)
        for (span in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.MAX_VALUE)) assertTrue("$span", niceStep(span, 3).isFinite())
    }
}
