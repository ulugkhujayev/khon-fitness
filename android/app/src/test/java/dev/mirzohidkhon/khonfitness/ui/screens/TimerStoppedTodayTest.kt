package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.data.PlanItem
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Today after the interval timer's Stop (row 8): the card offers Save (the form, with the timer's time and rounds) and
 * Discard, not Resume, also after an app restart, when no timer state is left and only the stored time says so.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TimerStoppedTodayTest {
    @get:Rule val compose = createComposeRule()

    private val tabata = Exercise("tabata", "Tabata bike", Kind.CARDIO, intervals = true, workSec = 20, restSec = 10, rounds = 8)
    private fun cardio(timeSec: Int? = null, finishedAt: Long? = null) =
        Session(id = "c1", date = "2026-11-03", startedAt = 1L, finishedAt = finishedAt, itemType = ItemType.CARDIO, exerciseId = "tabata", exerciseName = "Tabata bike", timeSec = timeSec)

    @Test fun `a stopped timer is one whose time is stored while no timer runs for the session`() {
        assertTrue(timerStopped(cardio(timeSec = 95), runningTimer = null)) // after Stop, or after an app restart
        assertTrue(timerStopped(cardio(timeSec = 95), runningTimer = "other"))
        assertFalse(timerStopped(cardio(timeSec = 95), runningTimer = "c1")) // still running: Resume opens the timer
        assertFalse(timerStopped(cardio(), runningTimer = null)) // no timer result: Resume opens the form
        assertFalse(timerStopped(cardio(timeSec = 95, finishedAt = 5L), runningTimer = null))
        assertFalse(timerStopped(Session(id = "p", date = "2026-11-03", startedAt = 1L, itemType = ItemType.PROGRAM, timeSec = 95), runningTimer = null))
    }

    @Test fun `the card offers Save and Discard, and no Resume`() {
        val s = cardio(timeSec = 95)
        var saved: Session? = null
        var discarded: Session? = null
        compose.setContent {
            KhonTheme {
                TodayCard(
                    PlanItem(ItemType.CARDIO, cardio = tabata), "", s, null, null, emptyList(), listOf(tabata), now = 100_000L,
                    onStart = {}, onResume = { saved = it }, timerStopped = timerStopped(s, null), onDiscard = { discarded = it },
                )
            }
        }
        assertTrue(compose.onAllNodesWithText("Resume").fetchSemanticsNodes().isEmpty())
        compose.onNodeWithText("Timer stopped · 1:35").fetchSemanticsNode()
        compose.onNodeWithText("Save").performClick()
        assertEquals(s, saved)
        compose.onNodeWithText("Discard").performClick()
        assertEquals(s, discarded)
    }

    @Test fun `a running cardio session keeps Resume`() {
        val s = cardio()
        compose.setContent {
            KhonTheme { TodayCard(PlanItem(ItemType.CARDIO, cardio = tabata), "", s, null, null, emptyList(), listOf(tabata), now = 100_000L, onStart = {}, onResume = {}) }
        }
        compose.onNodeWithText("Resume").fetchSemanticsNode()
        assertTrue(compose.onAllNodesWithText("Discard").fetchSemanticsNodes().isEmpty())
    }
}
