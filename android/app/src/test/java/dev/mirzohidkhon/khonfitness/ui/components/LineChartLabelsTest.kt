package dev.mirzohidkhon.khonfitness.ui.components

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LineChartLabelsTest {
    private val margin = 2f

    private fun apart(drawn: Set<Int>, boxes: Map<Int, Rect>) {
        val list = drawn.map { boxes.getValue(it) }
        for (a in list.indices) for (b in a + 1 until list.size) {
            assertFalse("${list[a]} meets ${list[b]}", list[a].inflate(margin).overlaps(list[b]))
        }
    }

    /**
     * Seven weeks of 1,668 at 360 dp and font scale 1.3, in dp: a 328 dp canvas, a value axis 44 dp wide, labels 36 dp
     * wide (41 dp for the bold last one) and 19 dp high, all at one height.
     */
    @Test fun sevenEqualFourDigitValuesAt360AndScale13() {
        val n = 7
        val xs = (0 until n).map { 44f + it * 276f / (n - 1) }
        val boxes = (0 until n).associateWith { i -> pointLabelBox(i, n, xs[i], 60f, if (i == n - 1) 41f else 36f, 19f, 328f) }
        assertTrue("the review case overlaps", boxes.getValue(0).overlaps(boxes.getValue(1)))
        val drawn = drawnLabels(boxes, last = 6, highest = 0, margin = margin)
        assertEquals(setOf(6, 0, 4, 3, 2), drawn)
        assertEquals(listOf(6, 0, 4, 3, 2), drawn.toList())
        apart(drawn, boxes)
    }

    @Test fun twoEqualPoints() {
        val wide = mapOf(0 to pointLabelBox(0, 2, 44f, 60f, 36f, 19f, 328f), 1 to pointLabelBox(1, 2, 320f, 60f, 41f, 19f, 328f))
        assertEquals(setOf(1, 0), drawnLabels(wide, 1, 0, margin))
        val narrow = mapOf(0 to pointLabelBox(0, 2, 44f, 60f, 36f, 19f, 100f), 1 to pointLabelBox(1, 2, 92f, 60f, 41f, 19f, 100f))
        assertEquals(setOf(1), drawnLabels(narrow, 1, 0, margin))
    }

    @Test fun highestNextToTheLast() {
        val n = 12
        val xs = (0 until n).map { 44f + it * 276f / (n - 1) }
        val close = mapOf(10 to pointLabelBox(10, n, xs[10], 40f, 36f, 19f, 328f), 11 to pointLabelBox(11, n, xs[11], 60f, 41f, 19f, 328f))
        assertEquals(setOf(11), drawnLabels(close, 11, 10, margin))
        val high = mapOf(10 to pointLabelBox(10, n, xs[10], 30f, 36f, 19f, 328f), 11 to pointLabelBox(11, n, xs[11], 60f, 41f, 19f, 328f))
        assertEquals(setOf(11, 10), drawnLabels(high, 11, 10, margin))
        apart(setOf(11, 10), high)
    }

    @Test fun boxesKeepThePositionRule() {
        assertEquals(Rect(44f, 41f, 80f, 60f), pointLabelBox(0, 7, 44f, 60f, 36f, 19f, 328f))
        assertEquals(Rect(279f, 41f, 320f, 60f), pointLabelBox(6, 7, 320f, 60f, 41f, 19f, 328f))
        assertEquals(Rect(72f, 41f, 108f, 60f), pointLabelBox(1, 7, 90f, 60f, 36f, 19f, 328f))
        assertEquals(Rect(0f, 41f, 36f, 60f), pointLabelBox(1, 7, 10f, 60f, 36f, 19f, 328f))
        assertEquals(Rect(292f, 41f, 328f, 60f), pointLabelBox(0, 1, 320f, 60f, 36f, 19f, 328f))
    }

    @Test fun touchingWithinTheMarginDoesNotDraw() {
        val boxes = mapOf(1 to Rect(0f, 0f, 10f, 10f), 0 to Rect(11f, 0f, 21f, 10f))
        assertEquals(setOf(1), drawnLabels(boxes, 1, 0, margin))
        val far = mapOf(1 to Rect(0f, 0f, 10f, 10f), 0 to Rect(12.5f, 0f, 22f, 10f))
        assertEquals(setOf(1, 0), drawnLabels(far, 1, 0, margin))
    }
}
