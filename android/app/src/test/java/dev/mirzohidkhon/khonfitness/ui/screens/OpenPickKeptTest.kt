package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Back and Resume: the session screen leaves the composition and a new one opens the same session. The open block
 * lives in [OpenPicks] (the view model's), so it is kept; another session starts from its own default.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class OpenPickKeptTest {
    @get:Rule val compose = createComposeRule()

    private fun set(block: Int, index: Int, status: String = SetStatus.PENDING) =
        SetLog("s-$block-$index", "s", block, false, 0, index, block * 10 + index, "ex$block", "Name $block", 6, 10, 2.5, null, null, status)

    // Every block has work, so without a kept pick the first block opens.
    private val blocks = groupBlocks(listOf(set(0, 0), set(1, 0), set(2, 0)))

    private val picks = OpenPicks()
    private var shown by mutableStateOf(true)
    private var sessionId by mutableStateOf("s1")
    private lateinit var pick: MutableState<OpenPick>
    private var disposed = 0

    // The screen's share: its open-block state, read the way SessionScreen reads it.
    private fun show() = compose.setContent {
        if (shown) key(sessionId) {
            pick = rememberOpenPick(picks, sessionId)
            DisposableEffect(Unit) { onDispose { disposed++ } }
        }
    }

    private fun leaveAndReturn() {
        compose.runOnIdle { shown = false }
        compose.runOnIdle { assertEquals(1, disposed); shown = true }
    }

    private fun open() = compose.runOnIdle { openBlock(blocks, pick.value.picked, pick.value.chosen) }

    @Test fun `a chosen block stays open after Back and Resume`() {
        show()
        assertEquals(0, open())
        compose.runOnIdle { pick.value = OpenPick(picked = true, chosen = 2) }
        leaveAndReturn()
        assertEquals(OpenPick(true, 2), compose.runOnIdle { pick.value })
        assertEquals(2, open())
    }

    @Test fun `every block closed stays closed after Back and Resume`() {
        show()
        compose.runOnIdle { pick.value = OpenPick(picked = true, chosen = null) }
        leaveAndReturn()
        assertEquals(null, open())
    }

    @Test fun `another session starts from its own default`() {
        show()
        compose.runOnIdle { pick.value = OpenPick(picked = true, chosen = 2) }
        compose.runOnIdle { sessionId = "s2" }
        assertEquals(OpenPick(), compose.runOnIdle { pick.value })
        assertEquals(0, open())
        // Back to the first session: its choice is still there.
        compose.runOnIdle { sessionId = "s1" }
        assertEquals(2, open())
    }

    @Test fun `a finished or deleted session drops its choice`() {
        show()
        compose.runOnIdle { pick.value = OpenPick(picked = true, chosen = 2) }
        compose.runOnIdle { shown = false; picks.drop("s1") }
        compose.runOnIdle { shown = true }
        assertTrue(compose.runOnIdle { pick.value == OpenPick() })
        assertEquals(0, open())
    }
}
