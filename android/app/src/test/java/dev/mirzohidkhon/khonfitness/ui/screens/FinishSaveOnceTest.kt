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
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A running gym session on a real database and a real back stack (Today under the session): Save in the finish sheet
 * (row 1) and Discard (row 4) leave once, even for two taps in one frame; a check needs weight and reps (row 2).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class FinishSaveOnceTest {
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

    // The view model's flows outlive the screen by 5 s (WhileSubscribed): stop them before the database closes, or one
    // queries the closed database and its exception fails the next test.
    @After fun close() { vm.viewModelScope.cancel(); app.scope.cancel(); app.db.close() }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }

    private fun log(id: String, order: Int, status: String, weight: Double? = 60.0, reps: Int? = 8, step: Double = 2.5) =
        SetLog(id, "s1", 0, false, 0, order, order, "bench", "Bench Press", 6, 10, step, weight, reps, status)

    /**
     * Every set as the screen collects it, with the first list held until the test [release]s it: the query runs after
     * the release, or before the hold when [queryFirst]. Later lists follow the database.
     */
    private inner class HeldSets(private val queryFirst: Boolean = false) {
        private val gate = CompletableDeferred<Unit>()
        private val started = java.util.concurrent.atomic.AtomicInteger()
        val collections get() = started.get()
        val flow: Flow<List<SetLog>> = flow {
            started.incrementAndGet()
            val early = if (queryFirst) dao.allSetLogsOnce() else null
            gate.await()
            if (early != null) emit(early)
            emitAll(dao.allSetLogs())
        }
        fun release() { gate.complete(Unit) }
    }

    /** Today with the session pushed on it, or on History over Today when [fromHistory] (its "In progress" row). */
    private fun openSession(
        logs: List<SetLog>, fromHistory: Boolean = false, startedAt: Long = System.currentTimeMillis(),
        sets: Flow<List<SetLog>>? = null, clock: () -> Long = System::currentTimeMillis,
    ) {
        io {
            dao.upsert(Session(id = "s1", date = "2026-11-03", startedAt = startedAt, itemType = ItemType.PROGRAM, programId = "p1", programName = "Push"))
            dao.upsertSetLogs(logs)
        }
        compose.setContent {
            KhonTheme {
                nav = rememberNavController()
                NavHost(nav, startDestination = "today") {
                    composable("today") { Text("Today home") }
                    composable("history") { Text("History home") }
                    composable("session/{id}") { e ->
                        val id = e.arguments?.getString("id") ?: ""
                        if (sets == null) SessionScreen(vm, nav, id, clock = clock) else SessionScreen(vm, nav, id, allSets = sets, clock = clock)
                    }
                }
            }
        }
        compose.runOnIdle { if (fromHistory) nav.navigate("history"); nav.navigate("session/s1") }
        until { shows("Bench Press") }
    }

    private fun until(shown: () -> Boolean) {
        repeat(250) { if (shown()) return; compose.waitForIdle(); Thread.sleep(20) }
        throw AssertionError("the screen did not reach the expected state")
    }

    private fun shows(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    /** Two taps on [text] in one frame: the button's click runs twice before anything recomposes. */
    private fun doubleTap(text: String) {
        val click = compose.onNodeWithText(text).fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnUiThread { click(); click() }
    }

    private fun session() = runBlocking(Dispatchers.IO) { dao.session("s1") }

    @Test fun `a double tap on Save finishes once and leaves Today showing`() {
        openSession(listOf(log("a", 0, SetStatus.DONE), log("b", 1, SetStatus.PENDING)))
        compose.onNodeWithText("Finish").performClick()
        until { shows("Workout done") }
        doubleTap("Save")
        compose.waitForIdle()
        // One back navigation: Today is on top and still on the stack, never an empty screen.
        assertEquals("today", nav.currentDestination?.route)
        assertEquals(listOf("today"), nav.currentBackStack.value.mapNotNull { it.destination.route })
        until { shows("Today home") }
        until { session()?.finished == true }
    }

    /** Device QA (0.6.0 build ac2da13): Save of a session opened from History's "In progress" row landed on History. */
    @Test fun `a double tap on Save in a session opened from History finishes once and lands on Today`() {
        openSession(listOf(log("a", 0, SetStatus.DONE), log("b", 1, SetStatus.PENDING)), fromHistory = true)
        assertEquals(listOf("today", "history", "session/{id}"), nav.currentBackStack.value.mapNotNull { it.destination.route })
        compose.onNodeWithText("Finish").performClick()
        until { shows("Workout done") }
        doubleTap("Save")
        compose.waitForIdle()
        assertEquals(listOf("today"), nav.currentBackStack.value.mapNotNull { it.destination.route })
        until { shows("Today home") }
        until { session()?.finished == true }
        assertEquals(listOf("s1"), runBlocking(Dispatchers.IO) { dao.sessionsOnce() }.map { it.id })
    }

    @Test fun `Discard asks with the count of checked sets, and Cancel keeps everything`() {
        openSession(listOf(log("a", 0, SetStatus.DONE), log("b", 1, SetStatus.DONE), log("c", 2, SetStatus.DONE), log("d", 3, SetStatus.PENDING)))
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Discard workout"))
        compose.onNodeWithText("Discard workout").performClick()
        until { shows("Discard this workout?") }
        assertNotNull(compose.onNodeWithText("3 checked sets are lost.").fetchSemanticsNode())
        val cancel = compose.onNodeWithText("Cancel").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnUiThread { cancel() }
        until { !shows("3 checked sets are lost.") }
        assertNotNull(session())
        assertEquals(4, runBlocking(Dispatchers.IO) { dao.setLogsOnce("s1") }.size)
        assertEquals("session/{id}", nav.currentDestination?.route)
    }

    @Test fun `a double tap on Discard deletes the session and leaves once`() {
        openSession(listOf(log("a", 0, SetStatus.DONE)))
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Discard workout"))
        compose.onNodeWithText("Discard workout").performClick()
        until { shows("Discard this workout?") }
        doubleTap("Discard")
        compose.waitForIdle()
        assertEquals(listOf("today"), nav.currentBackStack.value.mapNotNull { it.destination.route })
        until { session() == null }
    }

    @Test fun `the finish sheet counts rep records against sessions no other screen has loaded`() {
        io {
            dao.upsert(Session(id = "s0", date = "2026-11-01", startedAt = 1L, finishedAt = 2L, itemType = ItemType.PROGRAM, programId = "p1", programName = "Push"))
            dao.upsertSetLogs(listOf(
                SetLog("e1", "s0", 0, false, 0, 0, 0, "bench", "Bench Press", 6, 10, 2.5, 85.0, 3, SetStatus.DONE),
                SetLog("e2", "s0", 0, false, 0, 1, 1, "bench", "Bench Press", 6, 10, 2.5, 80.0, 6, SetStatus.DONE),
            ))
        }
        openSession(listOf(log("a", 0, SetStatus.DONE, weight = 80.0, reps = 8)))
        compose.onNodeWithText("Finish").performClick()
        until { shows("Rep records") }
        assertEquals("1", rowValue("Rep records"))
        assertEquals("0", rowValue("New bests"))
    }

    /** The value shown on the line of a finish sheet row [label]. */
    private fun rowValue(label: String): String = rowTexts(label).single { it.isNotEmpty() && it.all(Char::isDigit) }

    private fun rowTexts(label: String): List<String> {
        val y = compose.onNodeWithText(label).fetchSemanticsNode().boundsInRoot.center.y
        val texts = androidx.compose.ui.semantics.SemanticsProperties.Text
        return compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(texts)).fetchSemanticsNodes()
            .filter { kotlin.math.abs(it.boundsInRoot.center.y - y) < 2f }.flatMap { n -> n.config[texts].map { it.text } }
            .filter { it != label }
    }

    private fun sheets() = compose.onAllNodesWithText("Workout done").fetchSemanticsNodes().size

    /** Lets every pending frame and coroutine run, so a late result would have shown by now. */
    private fun settle() = repeat(10) { compose.waitForIdle(); Thread.sleep(20) }

    private fun tapFinish() {
        val click = compose.onNodeWithText("Finish").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnUiThread { click() }
    }

    private fun dismissSheet() {
        val dismiss = compose.onNode(androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)).fetchSemanticsNode().config[SemanticsActions.Dismiss].action!!
        compose.runOnUiThread { dismiss() }
    }

    @Test fun `Finish taps before the sets load open one sheet at the first tap's time`() {
        val held = HeldSets()
        val started = 1_000_000_000L
        var now = started + 10 * 60_000L
        openSession(listOf(log("a", 0, SetStatus.DONE)), startedAt = started, sets = held.flow, clock = { now })
        doubleTap("Finish")
        now = started + 30 * 60_000L
        settle()
        tapFinish()
        settle()
        assertEquals(0, sheets())
        held.release()
        until { shows("Workout done") }
        settle()
        assertEquals(1, sheets())
        assertEquals(1, held.collections)
        assertEquals("10 min", rowTexts("Time").single { it.isNotEmpty() })
    }

    @Test fun `a dismissed sheet stays closed when the sets change`() {
        val held = HeldSets()
        openSession(listOf(log("a", 0, SetStatus.DONE), log("b", 1, SetStatus.PENDING)), sets = held.flow)
        tapFinish()
        held.release()
        until { shows("Workout done") }
        tapFinish()
        settle()
        assertEquals(1, sheets())
        dismissSheet()
        until { !shows("Workout done") }
        io { assertEquals(true, vm.repo.checkSet("b")) }
        until { compose.onAllNodesWithContentDescription("2 of 2 sets done").fetchSemanticsNodes().isNotEmpty() }
        settle()
        assertEquals(0, sheets())
    }

    @Test fun `a session deleted before the sets load opens no sheet`() {
        val held = HeldSets()
        openSession(listOf(log("a", 0, SetStatus.DONE)), sets = held.flow)
        tapFinish()
        settle()
        io { vm.repo.deleteSession("s1") }
        until { nav.currentDestination?.route == "today" }
        held.release()
        settle()
        assertEquals(0, sheets())
        assertNull(session())
    }

    @Test fun `a session finished before the sets load opens no sheet`() {
        val held = HeldSets()
        openSession(listOf(log("a", 0, SetStatus.DONE)), sets = held.flow)
        tapFinish()
        settle()
        io { vm.repo.finishSession("s1") }
        until { shows("Edit") }
        held.release()
        settle()
        assertEquals(0, sheets())
        assertEquals("session/{id}", nav.currentDestination?.route)
    }

    @Test fun `sets that load after Back while the screen is still composed open no sheet`() {
        val held = HeldSets()
        openSession(listOf(log("a", 0, SetStatus.DONE)), sets = held.flow)
        tapFinish()
        settle()
        val back = compose.onNodeWithContentDescription("Today").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread { back() }
        repeat(3) { compose.mainClock.advanceTimeByFrame() }
        assertEquals("today", nav.currentDestination?.route)
        assertEquals(true, shows("Bench Press"))
        held.release()
        repeat(5) { Thread.sleep(20); compose.mainClock.advanceTimeByFrame() }
        assertEquals(true, shows("Bench Press"))
        assertEquals(0, sheets())
        compose.mainClock.autoAdvance = true
        until { !shows("Bench Press") }
        settle()
        assertEquals(0, sheets())
        assertNotNull(session())
    }

    @Test fun `an open sheet goes when the session is finished elsewhere`() {
        openSession(listOf(log("a", 0, SetStatus.DONE)))
        tapFinish()
        until { shows("Workout done") }
        io { vm.repo.finishSession("s1") }
        until { shows("Edit") }
        settle()
        assertEquals(0, sheets())
        assertEquals("session/{id}", nav.currentDestination?.route)
    }

    @Test fun `an open sheet goes when the session is deleted elsewhere`() {
        openSession(listOf(log("a", 0, SetStatus.DONE)))
        tapFinish()
        until { shows("Workout done") }
        io { vm.repo.deleteSession("s1") }
        until { nav.currentDestination?.route == "today" }
        settle()
        assertEquals(0, sheets())
        assertNull(session())
    }

    @Test fun `an open sheet keeps its figures when a set of another session changes`() {
        seedEarlierBench()
        openSession(listOf(log("a", 0, SetStatus.DONE, weight = 80.0, reps = 8)))
        tapFinish()
        until { shows("Rep records") }
        assertEquals("1", rowValue("Rep records"))
        io { dao.upsertSetLogs(listOf(SetLog("e2", "s0", 0, false, 0, 1, 1, "bench", "Bench Press", 6, 10, 2.5, 80.0, 10, SetStatus.DONE))) }
        until { runBlocking(Dispatchers.IO) { dao.allSetLogsOnce() }.single { it.id == "e2" }.reps == 10 }
        settle()
        assertEquals(1, sheets())
        assertEquals("1", rowValue("Rep records"))
        assertEquals("0", rowValue("New bests"))
    }

    private fun seedEarlierBench() = io {
        dao.upsert(Session(id = "s0", date = "2026-11-01", startedAt = 1L, finishedAt = 2L, itemType = ItemType.PROGRAM, programId = "p1", programName = "Push"))
        dao.upsertSetLogs(listOf(
            SetLog("e1", "s0", 0, false, 0, 0, 0, "bench", "Bench Press", 6, 10, 2.5, 85.0, 3, SetStatus.DONE),
            SetLog("e2", "s0", 0, false, 0, 1, 1, "bench", "Bench Press", 6, 10, 2.5, 80.0, 6, SetStatus.DONE),
        ))
    }

    /**
     * Set a (80 × 8) is a rep record over 80 × 6; set b (90 × 3), once checked, is a new best over 85 × 3, so the bench
     * then counts as a best and no longer as a rep record. b is checked while the first list is held, and the clock moves
     * on.
     */
    private fun finishWithACheckDuringTheRead(queryFirst: Boolean): Map<String, String> {
        seedEarlierBench()
        val started = 1_000_000_000L
        var now = started + 10 * 60_000L
        val held = HeldSets(queryFirst)
        openSession(listOf(log("a", 0, SetStatus.DONE, weight = 80.0, reps = 8), log("b", 1, SetStatus.PENDING, weight = 90.0, reps = 3)), startedAt = started, sets = held.flow, clock = { now })
        tapFinish()
        settle()
        now = started + 40 * 60_000L
        io { assertEquals(true, vm.repo.checkSet("b")) }
        until { compose.onAllNodesWithContentDescription("2 of 2 sets done").fetchSemanticsNodes().isNotEmpty() }
        held.release()
        until { shows("Rep records") }
        settle()
        assertEquals(1, sheets())
        return listOf("Time", "Sets done", "New bests", "Rep records").associateWith { rowTexts(it).single { t -> t.isNotEmpty() } }
    }

    @Test fun `a set checked before the held query runs counts in every figure, at the tap's time`() {
        assertEquals(
            mapOf("Time" to "10 min", "Sets done" to "2 of 2", "New bests" to "1", "Rep records" to "0"),
            finishWithACheckDuringTheRead(queryFirst = false),
        )
    }

    @Test fun `a set checked after the query ran counts in no figure, at the tap's time`() {
        assertEquals(
            mapOf("Time" to "10 min", "Sets done" to "1 of 2", "New bests" to "0", "Rep records" to "1"),
            finishWithACheckDuringTheRead(queryFirst = true),
        )
    }

    @Test fun `a double tap on Back leaves once`() {
        openSession(listOf(log("a", 0, SetStatus.DONE)))
        val back = compose.onNodeWithContentDescription("Today").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnUiThread { back(); back() }
        compose.waitForIdle()
        assertEquals(listOf("today"), nav.currentBackStack.value.mapNotNull { it.destination.route })
        assertNotNull(session())
    }

    @Test fun `a check without reps stays pending and the row says Enter reps`() {
        openSession(listOf(log("a", 0, SetStatus.PENDING, reps = null)))
        compose.onNodeWithContentDescription("A1 Bench Press").performClick()
        until { shows("Enter reps") }
        assertEquals(SetStatus.PENDING, runBlocking(Dispatchers.IO) { dao.setLogsOnce("s1") }.single().status)
        assertNull(compose.onAllNodesWithText("Enter weight").fetchSemanticsNodes().firstOrNull())
    }

    @Test fun `a check with 0 kg on a weighted exercise stays pending and the row says Enter weight`() {
        openSession(listOf(log("a", 0, SetStatus.PENDING, weight = 0.0)))
        compose.onNodeWithContentDescription("A1 Bench Press").performClick()
        until { shows("Enter weight") }
        assertEquals(SetStatus.PENDING, runBlocking(Dispatchers.IO) { dao.setLogsOnce("s1") }.single().status)
    }

    @Test fun `a filled set checks, and a bodyweight set needs reps only`() {
        openSession(listOf(log("a", 0, SetStatus.PENDING), log("b", 1, SetStatus.PENDING, weight = null, step = 0.0)))
        compose.onNodeWithContentDescription("A1 Bench Press").performClick()
        compose.onNodeWithContentDescription("A2 Bench Press").performClick()
        until { runBlocking(Dispatchers.IO) { dao.setLogsOnce("s1") }.all { it.status == SetStatus.DONE } }
        assertNull(compose.onAllNodesWithText("Enter weight").fetchSemanticsNodes().firstOrNull())
    }
}
