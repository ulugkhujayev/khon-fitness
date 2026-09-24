package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.StretchSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DayDotsTest {
    private val date = "2026-09-23"
    private fun session(id: String, at: Long, finished: Boolean = true, day: String = date) = Session(id, day, at, finishedAt = if (finished) at + 1 else null, itemType = ItemType.CARDIO)
    private fun stretch(id: String, at: Long) = StretchSession(id, "daily", "Daily mobility", date, at, 600, 9, 0)

    @Test fun `one dot per finished session in start order with stretch runs as null`() {
        val dots = dayDots(date, listOf(session("b", 30), session("a", 10), session("open", 5, finished = false), session("other", 1, day = "2026-09-22")), listOf(stretch("s", 20)))
        assertEquals(listOf("a", null, "b"), dots.map { it?.id })
        assertNull(dots[1])
    }

    @Test fun `more than three sessions draw three dots`() {
        val dots = dayDots(date, (1..4).map { session("s$it", it.toLong()) }, listOf(stretch("x", 0)))
        assertEquals(listOf(null, "s1", "s2"), dots.map { it?.id })
    }

    @Test fun `no session gives no dots`() {
        assertTrue(dayDots(date, emptyList(), emptyList()).isEmpty())
    }
}
