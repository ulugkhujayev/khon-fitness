package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.geometry.Offset
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UI07 verify 2: the folded session header's progress line, drawn just under the bar, takes the taps on it, so a tap
 * there never opens or closes the block header that scrolled under it. A drag that starts on the bar or on the line
 * scrolls the list.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SessionBarTouchTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private lateinit var nav: NavHostController
    private val dao get() = app.db.dao()

    @Before fun open() {
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
    }

    @After fun close() { vm.viewModelScope.cancel(); app.scope.cancel(); app.db.close() }

    /** Twenty blocks of three sets, so the list scrolls well past the header. */
    private fun openSession() {
        val logs = (0 until 20).flatMap { b ->
            (0 until 3).map { s -> SetLog("l$b-$s", "s1", b, false, 0, s, b * 3 + s, "ex$b", "Exercise $b", 6, 10, 2.5, 60.0, 8, SetStatus.PENDING) }
        }
        runBlocking(Dispatchers.IO) {
            dao.upsert(Session(id = "s1", date = "2026-11-03", startedAt = System.currentTimeMillis(), itemType = ItemType.PROGRAM, programId = "p1", programName = "Push"))
            dao.upsertSetLogs(logs)
        }
        compose.setContent {
            KhonTheme {
                nav = rememberNavController()
                NavHost(nav, startDestination = "today") {
                    composable("today") { Text("Today home") }
                    composable("session/{id}") { e -> SessionScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                }
            }
        }
        compose.runOnIdle { nav.navigate("session/s1") }
        repeat(250) { if (compose.onAllNodes(hasContentDescription("Block 1,", substring = true)).fetchSemanticsNodes().isNotEmpty()) return; compose.waitForIdle(); Thread.sleep(20) }
        throw AssertionError("the session did not open")
    }

    private fun block8() = compose.onAllNodes(hasContentDescription("Block 8,", substring = true)).fetchSemanticsNodes().single()

    /** Scrolls block 8's 52 dp header to span 50 to 102 dp, under the 56 dp bar and its line at 56 to 58 dp. */
    private fun block8UnderTheLine() {
        val px = compose.density.density
        compose.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Block 8,", substring = true))
        compose.waitForIdle()
        val dy = block8().boundsInRoot.top - 50 * px
        compose.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, dy) }
        compose.waitForIdle()
        assertEquals("block 8's header top in dp", 50f, block8().boundsInRoot.top / px, 1f)
    }

    private fun dragDownFrom(yDp: Float) {
        block8UnderTheLine()
        val px = compose.density.density
        val before = block8().boundsInRoot.top
        val state = block8().config.getOrNull(SemanticsProperties.StateDescription)
        compose.onRoot().performTouchInput { swipe(Offset(180 * px, yDp * px), Offset(180 * px, (yDp + 250) * px), durationMillis = 300) }
        compose.waitForIdle()
        assertTrue("block 8 moved ${(block8().boundsInRoot.top - before) / px} dp", block8().boundsInRoot.top - before > 150 * px)
        assertEquals("block 8 after the drag", state, block8().config.getOrNull(SemanticsProperties.StateDescription))
    }

    @Test fun `a drag that starts on the session bar scrolls the list`() { openSession(); dragDownFrom(28f) }

    @Test fun `a drag that starts on the folded progress line scrolls the list`() { openSession(); dragDownFrom(57f) }

    @Test fun `a tap on the folded progress line does not toggle the block header under it`() {
        openSession()
        val px = compose.density.density
        // The list composes block 8 once it scrolls near. The bar is 56 dp; the line lies from 56 to 58 dp. Scroll
        // block 8's 52 dp header to span 50 to 102 dp.
        compose.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Block 8,", substring = true))
        compose.waitForIdle()
        val dy = block8().boundsInRoot.top - 50 * px
        compose.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, dy) }
        compose.waitForIdle()
        val top = block8().boundsInRoot.top / px
        assertEquals("block 8's header top in dp", 50f, top, 1f)
        val before = block8().config.getOrNull(SemanticsProperties.StateDescription)
        compose.onRoot().performTouchInput { click(Offset(180 * px, 57 * px)) }
        compose.waitForIdle()
        assertEquals("block 8 after a tap on the line", before, block8().config.getOrNull(SemanticsProperties.StateDescription))
    }
}
