package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Device QA (0.6.0 build ac2da13): Today's done line and the finish summary cut a session's minutes, History rounded
 * them, so one workout read "3 min" on Today and "4 min" in History. One rule now ([roundMinutes]): 3 min 40 s is
 * "4 min" on Today, in the summary and in History. A running session's elapsed time still counts whole minutes.
 */
class MinutesRuleTest {
    private val start = 1_700_000_000_000L
    private val took = 3 * 60_000L + 40_000L
    private val set = SetLog("1", "s", 0, false, 0, 0, 0, "e", "Bench", 8, 12, 2.5, weightKg = 80.0, reps = 10, status = SetStatus.DONE)

    @Test fun `a gym session of 3 min 40 s reads 4 min on Today, in the finish summary and in History`() {
        val s = Session("s", "2026-11-03", start, start + took, ItemType.PROGRAM, programId = "p")
        assertTrue(doneLine(s, listOf(set)), doneLine(s, listOf(set)).endsWith(" · 4 min"))
        assertEquals("4 min", summarize(listOf(set), listOf(set), "s", start, start + took).time)
        assertTrue(sessionLine(s, listOf(set), null), sessionLine(s, listOf(set), null).endsWith(" · 4 min"))
    }

    @Test fun `a cardio session of 3 min 40 s reads 4 min on Today and in History`() {
        val s = Session("c", "2026-11-03", start, start + took, ItemType.CARDIO, exerciseId = "run", exerciseName = "Run", timeSec = 220)
        val run = Exercise("run", "Run", Kind.CARDIO)
        assertEquals("Done · 4 min", doneLine(s, emptyList()))
        assertEquals("4 min", sessionLine(s, emptyList(), run))
    }

    @Test fun `the rule rounds to the nearest minute and a running session still counts whole minutes`() {
        assertEquals("3 min", sessionMinutes(209))
        assertEquals("4 min", sessionMinutes(210))
        // Each place keeps its form: History "65 min", Today's done line and the summary "1 h 5 min".
        assertEquals("65 min", sessionMinutes(sessionSeconds(start, start + 65 * 60_000L)))
        assertEquals("1 h 5 min", sessionTime(sessionSeconds(start, start + 64 * 60_000L + 30_000L)))
        assertEquals(0, sessionSeconds(start, start - 5_000))
        assertEquals("3 min", workoutTime(start, start + took))
    }
}
