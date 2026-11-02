package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.runtime.MutableState
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Leaving a running session and returning to it (saved state restored) keeps the block the user had open. */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class OpenPickRestoreTest {
    @get:Rule val compose = createComposeRule()

    private fun set(block: Int, index: Int) =
        SetLog("s-$block-$index", "s", block, false, 0, index, block * 10 + index, "ex$block", "Name $block", 6, 10, 2.5, null, null, SetStatus.PENDING)

    // Every block has work, so without the saved pick the first block would open.
    private val blocks = groupBlocks(listOf(set(0, 0), set(1, 0), set(2, 0)))

    private fun restored(pick: OpenPick): OpenPick {
        val tester = StateRestorationTester(compose)
        lateinit var state: MutableState<OpenPick>
        tester.setContent { state = rememberOpenPick("s") }
        compose.runOnIdle { assertEquals(OpenPick(), state.value); state.value = pick }
        tester.emulateSavedInstanceStateRestore()
        return compose.runOnIdle { state.value }
    }

    @Test fun `a picked block stays open after return`() {
        val pick = restored(OpenPick(picked = true, chosen = 2))
        assertEquals(OpenPick(true, 2), pick)
        assertEquals(2, openBlock(blocks, pick.picked, pick.chosen))
    }

    @Test fun `every block closed stays closed after return`() {
        val pick = restored(OpenPick(picked = true, chosen = null))
        assertNull(openBlock(blocks, pick.picked, pick.chosen))
    }
}
