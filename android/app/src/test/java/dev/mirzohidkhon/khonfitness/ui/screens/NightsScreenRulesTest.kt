package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Night
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.iso
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class NightsScreenRulesTest {
    private val nov3 = LocalDate.of(2026, 11, 3)
    private fun h(h: Int, m: Int = 0) = h * 3600 + m * 60

    @Test fun `U6 the axis ends at the next 2 hours at or above the longest night, at least 8`() {
        assertEquals(8, nightAxisHours(h(7, 40)))
        assertEquals(10, nightAxisHours(h(9, 10)))
        assertEquals(8, nightAxisHours(h(8)))
        assertEquals(10, nightAxisHours(h(8) + 1))
        assertEquals(8, nightAxisHours(h(3)))
        assertEquals(8, nightAxisHours(null))
        assertEquals(listOf(0, 4, 8), nightAxisTicks(8))
        assertEquals(listOf(0, 4, 8), nightAxisTicks(10))
        assertEquals(listOf(0, 4, 8, 12), nightAxisTicks(12))
        assertEquals("4 h", nightAxisLabel(4))
    }

    @Test fun `U5 the captions of the two figures`() {
        assertEquals("average · last 14 nights", nightsAverageCaption(14))
        assertEquals("average · 11 of 14 nights", nightsAverageCaption(11))
        assertEquals("average · 0 of 14 nights", nightsAverageCaption(0))
        assertEquals("shortest · Nov 3", nightsShortestCaption(Night(nov3, h(5, 40), 0, 1)))
        assertEquals("shortest", nightsShortestCaption(null))
    }

    @Test fun `U7 the count of nights`() {
        assertEquals("7 nights", nightCount(7))
        assertEquals("1 night", nightCount(1))
    }

    @Test fun `U6 a bar speaks the date, the length or no night data, and the titles of the finished sessions`() {
        assertEquals("Tuesday, November 3, 5 hours 40 minutes, GYM", nightBarSpoken(nov3, Night(nov3, h(5, 40), 0, 1), listOf("GYM")))
        assertEquals("Tuesday, November 3, no night data", nightBarSpoken(nov3, null, emptyList()))
        val gym = Session("g", nov3.iso(), 2_000, 3_000, ItemType.PROGRAM, programName = "GYM")
        val swim = Session("s", nov3.iso(), 1_000, 1_500, ItemType.CARDIO, exerciseName = "Swim")
        val open = Session("o", nov3.iso(), 4_000, null, ItemType.CARDIO, exerciseName = "Run")
        val other = Session("x", nov3.plusDays(1).iso(), 1_000, 2_000, ItemType.CARDIO, exerciseName = "Bike")
        assertEquals(listOf("s", "g"), finishedOn(nov3, listOf(gym, open, other, swim)).map { it.id })
        assertEquals("T", weekdayLetter(nov3))
    }
}
