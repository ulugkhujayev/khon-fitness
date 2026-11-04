package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The session's deferred focus ([PendingFocus], [rememberFieldFocus]) in a lazy list long enough that a wrap target
 * starts off screen: focus lands once the row is placed, once, and a header tap cancels it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class PendingFocusTest {
    @get:Rule val compose = createComposeRule()

    private val pending = PendingFocus()
    private var tick by mutableStateOf(0)

    // A header, then 40 rows of 60 dp in a 300 dp list: about five rows show, "r30" starts off screen.
    private fun showList() = compose.setContent {
        LazyColumn(Modifier.height(300.dp).testTag("list")) {
            item { BasicText("Block 1", Modifier.height(40.dp).clickable { pending.cancel() }.testTag("header")) }
            items(40) { i ->
                val focus = rememberFieldFocus("r$i", pending)
                BasicTextField("$i kg $tick", {}, Modifier.height(60.dp).then(focus.placed).focusRequester(focus.requester).testTag("r$i"))
            }
        }
    }

    // The screen scrolls the opened block to the top; the list item index counts the header.
    private fun scrollTo(row: Int) = compose.onNodeWithTag("list").performScrollToIndex(row + 1)

    @Test fun `a wrap to an off-screen row focuses it once it is placed`() {
        showList()
        compose.runOnIdle { pending.aim("r30") }
        compose.onNodeWithTag("r30").assertDoesNotExist()
        assertEquals("r30", compose.runOnIdle { pending.target })
        scrollTo(30)
        compose.onNodeWithTag("r30").assertIsFocused()
        assertNull(compose.runOnIdle { pending.target })
    }

    @Test fun `a field composed but not placed waits until it is placed`() {
        // What a lazy list does with a row it composes ahead of view: the field exists but has no place yet.
        var placeIt by mutableStateOf(false)
        compose.setContent {
            val focus = rememberFieldFocus("ahead", pending)
            Box(Modifier.layout { m, c -> val p = m.measure(c); layout(p.width, p.height) { if (placeIt) p.place(0, 0) } }) {
                BasicTextField("0 kg", {}, Modifier.then(focus.placed).focusRequester(focus.requester).testTag("ahead"))
            }
        }
        compose.runOnIdle { pending.aim("ahead") }
        assertEquals("ahead", compose.runOnIdle { pending.target })
        compose.runOnIdle { placeIt = true }
        compose.onNodeWithTag("ahead").assertIsFocused()
        assertNull(compose.runOnIdle { pending.target })
    }

    @Test fun `a placed row takes focus at once`() {
        showList()
        compose.runOnIdle { pending.aim("r2") }
        compose.onNodeWithTag("r2").assertIsFocused()
        assertNull(compose.runOnIdle { pending.target })
    }

    @Test fun `a header tap cancels the target`() {
        showList()
        compose.runOnIdle { pending.aim("r30") }
        compose.onNodeWithTag("header").performClick()
        assertNull(compose.runOnIdle { pending.target })
        scrollTo(30)
        compose.onNodeWithTag("r30").assertIsNotFocused()
    }

    @Test fun `a later recomposition does not take focus again`() {
        showList()
        compose.runOnIdle { pending.aim("r30") }
        scrollTo(30)
        compose.onNodeWithTag("r30").assertIsFocused()
        // The user moves on to another field; the rows then recompose.
        compose.onNodeWithTag("r31").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.runOnIdle { tick++ }
        compose.onNodeWithTag("r31").assertIsFocused()
        compose.onNodeWithTag("r30").assertIsNotFocused()
    }

    @Test fun `a newer target is kept when an older field takes focus`() {
        compose.runOnIdle { pending.aim("r30") }
        compose.runOnIdle { pending.taken("r2") }
        assertEquals("r30", compose.runOnIdle { pending.target })
    }
}
