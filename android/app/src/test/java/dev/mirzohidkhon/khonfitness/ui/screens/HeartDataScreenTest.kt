package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.health.HcStatus
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.theme.K
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h1200dp")
class HeartDataScreenTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private lateinit var scope: CoroutineScope
    private lateinit var fake: FakeHeart
    private lateinit var fill: HeartFill
    private var access = HeartAccess({ HcStatus.AVAILABLE }, { true })
    private val dao get() = app.db.dao()
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.now()
    private val yesterday = today.minusDays(1)

    @Before fun open() {
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        fake = FakeHeart()
        fill = app.repo.heartFill(fake, scope)
        KhonApp::class.java.getDeclaredField("heartFill\$delegate").apply { isAccessible = true }.set(app, lazyOf(fill))
    }

    /** The rule ends the activity only after @After, and its screen still reads the database. */
    @After fun close() {
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
        vm.viewModelScope.cancel(); scope.cancel(); app.scope.cancel(); app.db.close()
    }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }
    private fun t(h: Int, m: Int, day: LocalDate = yesterday) = day.atTime(h, m).atZone(zone).toInstant().toEpochMilli()
    private fun texts(text: String) = compose.onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes()
    private fun shows(text: String) = compose.waitUntil(5_000) { texts(text).isNotEmpty() }
    private fun gone(text: String) = compose.waitUntil(5_000) { texts(text).isEmpty() }
    private fun said(text: String) = compose.onAllNodesWithContentDescription(text, substring = true).fetchSemanticsNodes()
    private fun waitSaid(text: String) = compose.waitUntil(5_000) { said(text).isNotEmpty() }

    private fun gym(id: String, name: String, day: LocalDate, from: Pair<Int, Int> = 18 to 2, minutes: Int = 72, ranged: Boolean = true): Session {
        val start = t(from.first, from.second, day)
        val end = start + minutes * 60_000L
        return Session(id, day.iso(), start, end, ItemType.PROGRAM, programId = "p", programName = name, timedFrom = start.takeIf { ranged }, timedTo = end.takeIf { ranged })
    }

    private fun filled(s: Session, avg: Int = 128, max: Int = 164): SessionHr {
        val r = hrWindow(s, zone)!!
        return SessionHr(s.id, r.fromMs, r.toMs, HrState.FILLED, "0:120,5:130", 412, 398, "com.mi.health", "com.mi.health", r.fromMs, r.toMs - 60_000, 45, avg, max, fetchedAt = r.toMs + 60_000)
    }

    private fun state(s: Session, state: String) = filled(s).copy(state = state, avgHr = null, maxHr = null)

    private var page by mutableStateOf<(@Composable () -> Unit)?>(null)
    private var round by mutableIntStateOf(0)
    private var started = false

    /** The compose rule takes one setContent per test, so a new screen state comes from a new key. */
    private fun show(content: @Composable () -> Unit) {
        if (!started) {
            started = true
            compose.setContent { KhonTheme { androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) { key(round) { page?.invoke() } } } }
        }
        compose.runOnIdle { page = content; round++ }
        compose.waitForIdle()
    }

    private fun screen() = show { HeartDataScreen(vm, rememberNavController(), heart = fill, access = access) }

    /** A tap inside a sheet, as its click action: Robolectric drops a touch on a sheet that is still settling. */
    private fun click(text: String) { compose.onNodeWithText(text).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); compose.waitForIdle() }

    /** waitUntil alone does not run the main looper, where the screen's read resumes. */
    private fun reached() = compose.waitUntil(5_000) { compose.waitForIdle(); fake.reached.isCompleted }

    private fun readDone() = compose.waitUntil(5_000) {
        compose.onAllNodesWithText("Read again").fetchSemanticsNodes().singleOrNull()?.config?.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) == false
    }

    private fun disabled(text: String) =
        compose.onAllNodesWithText(text).fetchSemanticsNodes().single().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)

    private fun openSheet(name: String) {
        waitSaid("$name,")
        compose.onNodeWithContentDescription("$name,", substring = true).performScrollTo().performClick()
        shows("Time range")
    }

    @Test fun `the Settings row opens Heart rate data and Back returns to Settings`() {
        show {
            val nav = rememberNavController()
            NavHost(nav, Routes.SETTINGS) {
                composable(Routes.SETTINGS) { SettingsScreen(vm, nav) }
                composable(Routes.HEART_DATA) { HeartDataScreen(vm, nav, heart = fill, access = access) }
            }
        }
        shows("Heart rate data")
        compose.onNodeWithText("Heart rate data").performScrollTo().performClick()
        shows("No sessions in the last 30 days")
        compose.onNodeWithContentDescription("Settings").performClick()
        shows("Version")
        gone("No sessions in the last 30 days")
    }

    @Test fun `each state as a fact, the 30 days, finished sessions only, newest first`() {
        val push = gym("filled", "Push", yesterday)
        val run = Session("waiting", today.minusDays(2).iso(), t(7, 0, today.minusDays(2)), t(7, 31, today.minusDays(2)), ItemType.CARDIO, exerciseId = "z2",
            exerciseName = "Zone 2 run", timedFrom = t(7, 0, today.minusDays(2)), timedTo = t(7, 30, today.minusDays(2)))
        val pull = gym("none", "Pull", today.minusDays(3))
        val legs = gym("off", "Legs", today.minusDays(4))
        val arms = gym("notread", "Arms", today.minusDays(5))
        val back = gym("stale", "Back", today.minusDays(6))
        val chest = gym("norange", "Chest", today.minusDays(7), minutes = 9, ranged = false)
        val old = gym("old", "Old", today.minusDays(31))
        val open = gym("open", "Open", today).copy(finishedAt = null)
        io {
            listOf(push, run, pull, legs, arms, back, chest, old, open).forEach { dao.upsert(it) }
            dao.upsertSessionHr(filled(push)); dao.upsertSessionHr(state(run, HrState.WAITING)); dao.upsertSessionHr(state(pull, HrState.NONE))
            dao.upsertSessionHr(state(legs, HrState.OFF).copy(samples = "")); dao.upsertSessionHr(filled(back).copy(fromMs = filled(back).fromMs + 60_000))
            dao.upsertSessionHr(filled(old))
        }
        screen()
        waitSaid("Chest,")
        val expect = listOf("Push" to "average 128, maximum 164 beats per minute", "Zone 2 run" to "Waiting for data", "Pull" to "No data", "Legs" to "Removed",
            "Arms" to "Not read", "Back" to "Not read", "Chest" to "No time range")
        val tops = expect.map { (name, stateText) ->
            val node = said("$name,").single()
            val desc = node.config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].single()
            assertTrue(desc, desc.endsWith(", $stateText"))
            node.boundsInRoot.top
        }
        assertEquals(tops.sorted(), tops)
        assertTrue(said("Old,").isEmpty()); assertTrue(said("Open,").isEmpty())
        compose.onNodeWithText("avg\u00A0128 · max\u00A0164", useUnmergedTree = true).assertExists()
    }

    @Test fun `the sheet's facts, one read for two taps, a failed read keeps the facts`() {
        val push = gym("g", "Push", yesterday)
        io { dao.upsert(push); dao.upsertSessionHr(filled(push)) }
        screen()
        openSheet("Push")
        for (t in listOf("18:02 to 19:14", "412 read · 398 used", "18:02", "19:13", "45 s", "128 bpm", "164 bpm")) shows(t)
        fake.answer = { throw IllegalStateException("no provider") }
        fake.hold()
        click("Read again")
        reached()
        assertTrue(disabled("Read again"))
        click("Read again")
        assertTrue(disabled("Read again"))
        fake.release()
        shows("Health Connect did not answer")
        assertTrue(!disabled("Read again"))
        assertEquals(1, fake.calls.size)
        shows("128 bpm"); shows("412 read · 398 used")
        fake.answer = { r -> everyMinute(r, bpm = 140) }
        click("Read again")
        shows("140 bpm")
        assertEquals(2, fake.calls.size)
        gone("Health Connect did not answer")
        assertTrue(texts("avg 140 · max 140 bpm").isEmpty())
    }

    @Test fun `a sparse read shows the waiting row, and a session deleted during a read closes the sheet`() {
        val push = gym("g", "Push", yesterday)
        io { dao.upsert(push) }
        screen()
        openSheet("Push")
        fake.answer = { r -> sparse(r) }
        click("Read again")
        shows("Waiting for data")
        readDone()
        assertTrue(texts("No data yet").isEmpty())
        fake.hold()
        click("Read again")
        reached()
        io { app.repo.deleteSession("g") }
        gone("Time range")
        fake.release()
        compose.waitForIdle()
        assertTrue(texts("Read again").isEmpty())
        assertNull(io2 { dao.sessionHr("g") })
    }

    @Test fun `a late result after Remove or after a range change shows nothing by itself`() {
        val push = gym("g", "Push", yesterday)
        io { dao.upsert(push); dao.upsertSessionHr(filled(push)) }
        screen()
        openSheet("Push")
        fake.answer = { r -> everyMinute(r, bpm = 150) }
        fake.hold()
        click("Read again")
        reached()
        click("Remove")
        shows("Remove heart rate?")
        click("Remove")
        shows("Removed")
        fake.release()
        readDone()
        assertTrue(texts("avg 150").isEmpty()); assertTrue(texts("No data yet").isEmpty())
        assertEquals(HrState.OFF, io2 { dao.sessionHr("g") }!!.state)

        fake.hold()
        click("Read again")
        reached()
        io { dao.upsert(push.copy(timedTo = push.timedTo!! + 60_000)) }
        shows("18:02 to 19:15")
        fake.release()
        readDone()
        assertTrue(texts("avg 150").isEmpty()); assertTrue(texts("No data yet").isEmpty())
        shows("Removed")
    }

    @Test fun `Remove shows Removed everywhere, typed values stay, Read again brings the data back`() {
        val push = gym("g", "Push", yesterday)
        val run = Session("c", yesterday.iso(), t(7, 0), t(7, 31), ItemType.CARDIO, exerciseId = "z2", exerciseName = "Zone 2 run", timedFrom = t(7, 0), timedTo = t(7, 30), avgHr = 141)
        io { dao.upsert(push); dao.upsert(run); dao.upsertSessionHr(filled(push)); dao.upsertSessionHr(filled(run)) }
        screen()
        openSheet("Push")
        click("Remove")
        click("Remove")
        shows("Removed")
        assertTrue(texts("Remove").none { it.config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString() == "Remove" })
        val removed = io2 { dao.sessionHr("g") }!!
        assertEquals(HrState.OFF, removed.state)
        assertNull(hrLine(push, hrFacts(push, removed, zone)))
        fake.answer = { r -> everyMinute(r, bpm = 128) }
        click("Read again")
        shows("128 bpm")
        assertEquals(HrState.FILLED, io2 { dao.sessionHr("g") }!!.state)

        val runRow = io2 { dao.sessionHr("c") }!!
        assertEquals(141, shownAvgHr(run, hrFacts(run, runRow, zone)))
        io { fill.removeHeartRate("c") }
        val runOff = io2 { dao.sessionHr("c") }!!
        assertEquals("avg 141 bpm", hrLine(run, hrFacts(run, runOff, zone)))
    }

    @Test fun `Use this time range for a 40 minute gym session only, and the sheet follows its own fill`() {
        val forty = gym("g40", "Push", yesterday, minutes = 40, ranged = false)
        val nine = gym("g9", "Pull", today.minusDays(2), minutes = 9, ranged = false)
        val run = Session("c", yesterday.iso(), t(7, 0), t(7, 31), ItemType.CARDIO, exerciseId = "z2", exerciseName = "Zone 2 run")
        io { dao.upsert(forty); dao.upsert(nine); dao.upsert(run) }
        fake.answer = { r -> everyMinute(r, bpm = 131) }
        screen()
        waitSaid("Push,")
        compose.onNodeWithContentDescription("Push,", substring = true).performClick()
        shows("18:02 to 18:42"); shows("No time range"); shows("Use this time range")
        assertTrue(texts("Read again").isEmpty())
        click("Use this time range")
        gone("Use this time range")
        shows("Maximum"); shows("131 bpm"); shows("Read again")
        assertEquals(listOf(HrRange(forty.startedAt, forty.finishedAt!!)), fake.calls)
        val stored = io2 { dao.session("g40") }!!
        assertEquals(forty.startedAt, stored.timedFrom); assertEquals(forty.finishedAt, stored.timedTo)

        for (name in listOf("Pull", "Zone 2 run")) {
            screen()
            waitSaid("$name,")
            compose.onNodeWithContentDescription("$name,", substring = true).performClick()
            shows("No time range")
            assertTrue(texts("Use this time range").isEmpty()); assertTrue(texts("Read again").isEmpty())
        }
    }

    @Test fun `no Health Connect and no heart rate permission show the Import screen's card, heart rate alone works`() {
        access = HeartAccess({ HcStatus.NOT_INSTALLED }, { false })
        screen()
        shows("Get Health Connect")
        access = HeartAccess({ HcStatus.AVAILABLE }, { false })
        screen()
        shows("Read heart rate"); shows("Allow")
        access = HeartAccess({ HcStatus.AVAILABLE }, { true })
        screen()
        shows("No sessions in the last 30 days")
        assertTrue(texts("Allow").isEmpty()); assertTrue(texts("Get Health Connect").isEmpty())
    }

    @Test fun `Review's line appears, changes and goes without leaving the screen, a stale row shows nothing`() {
        val push = gym("g", "Push", yesterday)
        io { dao.upsert(push) }
        show { ReviewScreen(vm, rememberNavController(), "g") }
        shows(yesterday.format(shortDate))
        assertTrue(texts("bpm").isEmpty())
        io { dao.upsertSessionHr(filled(push)) }
        shows("avg 128 · max 164 bpm")
        compose.onNodeWithContentDescription("average 128, maximum 164 beats per minute").assertExists()
        io { fill.removeHeartRate("g") }
        gone("bpm")
        io { dao.upsertSessionHr(filled(push, avg = 130)) }
        shows("avg 130")
        io { dao.upsert(push.copy(timedTo = push.timedTo!! + 60_000)) }
        gone("bpm")
    }

    @Test fun `Review rounds and its average row take measured where typed is null`() {
        val run = Session("c", yesterday.iso(), t(7, 0), t(7, 31), ItemType.CARDIO, exerciseId = "z2", exerciseName = "Zone 2 run", timedFrom = t(7, 0), timedTo = t(7, 30))
        io {
            dao.upsert(Exercise("z2", "Zone 2 run", Kind.CARDIO, intervals = true, rounds = 3, fields = "time,avgHr"))
            dao.upsert(run)
            dao.upsertIntervalLogs(listOf(IntervalLog("i1", "c", 0, avgHr = 111, startedAt = t(7, 0), endedAt = t(7, 1)),
                IntervalLog("i2", "c", 1, startedAt = t(7, 5), endedAt = t(7, 6)), IntervalLog("i3", "c", 2, startedAt = t(7, 20), endedAt = t(7, 21))))
            dao.upsertSessionHr(filled(run).copy(samples = "0:120,5:130,300:150,305:160"))
        }
        show { ReviewScreen(vm, rememberNavController(), "c") }
        shows("Round 3")
        shows("111 bpm"); shows("155 bpm"); shows("—")
        shows("Avg heart rate")
    }

    @Test fun `the cardio form shows measured values as placeholders and Save writes none of them`() {
        val run = Session("c", yesterday.iso(), t(7, 0), t(7, 31), ItemType.CARDIO, exerciseId = "z2", exerciseName = "Zone 2 run", timedFrom = t(7, 0), timedTo = t(7, 30), timeSec = 1800)
        io {
            dao.upsert(Exercise("z2", "Zone 2 run", Kind.CARDIO, intervals = true, rounds = 2, fields = "time,avgHr"))
            dao.upsert(run)
            dao.upsertIntervalLogs(listOf(IntervalLog("i1", "c", 0, startedAt = t(7, 0), endedAt = t(7, 1)), IntervalLog("i2", "c", 1, startedAt = t(7, 5), endedAt = t(7, 6))))
            dao.upsertSessionHr(filled(run).copy(samples = "0:120,5:130,300:150,305:160"))
        }
        fun form() = show {
            val nav = rememberNavController()
            var opened by remember { mutableStateOf(false) }
            NavHost(nav, startDestination = "history") {
                composable("history") {
                    androidx.compose.material3.Text("History screen")
                    LaunchedEffect(Unit) { if (!opened) { opened = true; nav.navigate("cardio") } }
                }
                composable("cardio") { CardioScreen(vm, nav, "c") }
            }
        }
        form()
        shows("Round 2")
        shows("128"); shows("125"); shows("155")
        compose.onNodeWithText("Save").performClick()
        shows("History screen"); gone("Round 2")
        val saved = io2 { dao.session("c") }!!
        assertNull(saved.avgHr)
        assertEquals(listOf(null, null), io2 { dao.intervalLogsFor("c") }.map { it.avgHr })

        form()
        shows("Round 2")
        compose.onNodeWithContentDescription("Round 1 heart rate").performTextReplacement("133")
        compose.waitForIdle()
        compose.onNodeWithText("Save").performClick()
        shows("History screen"); gone("Round 2")
        assertEquals(listOf(133, null), io2 { dao.intervalLogsFor("c") }.sortedBy { it.index }.map { it.avgHr })
        assertEquals(133, shownRoundHr(io2 { dao.intervalLogsFor("c") }.first { it.index == 0 }, hrFacts(saved, io2 { dao.sessionHr("c") }, zone)))
    }


    private fun lateFailure(change: (Session) -> Unit) {
        val push = gym("g", "Push", yesterday)
        io { dao.upsert(push); dao.upsertSessionHr(filled(push)) }
        screen()
        openSheet("Push")
        fake.answer = { throw IllegalStateException("no provider") }
        fake.hold()
        click("Read again")
        reached()
        change(push)
        fake.release()
        compose.waitUntil(5_000) { compose.waitForIdle(); fake.active == 0 }
        compose.waitForIdle()
    }

    private val failure = "Health Connect did not answer"

    @Test fun `a late failure after Remove shows nothing`() {
        lateFailure { click("Remove"); shows("Remove heart rate?"); click("Remove"); shows("Removed") }
        readDone()
        assertTrue(texts(failure).isEmpty())
    }

    @Test fun `a late failure after a range change shows nothing`() {
        lateFailure { push -> io { dao.upsert(push.copy(timedTo = push.timedTo!! + 60_000)) }; shows("18:02 to 19:15") }
        readDone()
        assertTrue(texts(failure).isEmpty())
    }

    @Test fun `a late failure after a new row of the same state shows nothing`() {
        lateFailure { push -> io { dao.upsertSessionHr(filled(push, avg = 130)) }; shows("130 bpm") }
        readDone()
        assertTrue(texts(failure).isEmpty())
        shows("130 bpm")
    }

    @Test fun `a late failure after the sheet closed shows nothing when it opens again`() {
        lateFailure { closeSheet(); gone("Time range") }
        openSheet("Push")
        readDone()
        assertTrue(texts(failure).isEmpty())
    }

    @Test fun `a late failure after a delete shows nothing, also when the session comes back`() {
        lateFailure { push -> io { app.repo.deleteSession(push.id) }; gone("Time range") }
        assertTrue(texts(failure).isEmpty())
        io { dao.upsert(gym("g", "Push", yesterday)) }
        openSheet("Push")
        readDone()
        assertTrue(texts(failure).isEmpty())
    }

    @Test fun `a late failure after Remove tapped and cancelled shows nothing`() {
        lateFailure { click("Remove"); shows("Remove heart rate?"); click("Cancel"); shows("Time range") }
        readDone()
        assertTrue(texts(failure).isEmpty())
    }

    @Test fun `a late failure after Remove confirmed before Room publishes the off row shows nothing`() {
        lateFailure { click("Remove"); shows("Remove heart rate?"); click("Remove") }
        shows("Removed")
        readDone()
        assertTrue(texts(failure).isEmpty())
    }

    @Test fun `a late failure after a range change and a change back shows nothing`() {
        lateFailure { push ->
            io { dao.upsert(push.copy(timedTo = push.timedTo!! + 60_000)) }; shows("18:02 to 19:15")
            io { dao.upsert(push) }; shows("18:02 to 19:14")
        }
        readDone()
        assertTrue(texts(failure).isEmpty())
    }

    @Test fun `an automatic pass that replaces the row as the explicit read returns leaves no line`() {
        val push = gym("g", "Push", yesterday)
        io { dao.upsert(push) }
        val answers = ArrayDeque<suspend (HrRange) -> List<HeartRecord>>()
        fake.answer = { r -> answers.removeFirst()(r) }
        screen()
        openSheet("Push")
        answers += { r -> sparse(r) }; answers += { r -> everyMinute(r, bpm = 150) }
        fake.hold()
        click("Read again")
        reached()
        fill.fillHeartRate()
        fake.release()
        shows("150 bpm")
        readDone()
        assertEquals(2, fake.calls.size)
        assertTrue(texts("No data yet").isEmpty()); assertTrue(texts("avg 150 · max 150 bpm").isEmpty()); assertTrue(texts(failure).isEmpty())

        io { dao.upsertSessionHr(state(push, HrState.WAITING)) }
        shows("Waiting for data")
        answers += { throw IllegalStateException("no provider") }; answers += { r -> everyMinute(r, bpm = 160) }
        fake.hold()
        click("Read again")
        reached()
        fill.fillHeartRate()
        fake.release()
        shows("160 bpm")
        gone(failure)
        readDone()
        assertEquals(4, fake.calls.size)
    }

    @Test fun `an old request after leaving the screen and a range change shows nothing, the new request shows its own outcome`() {
        val push = gym("g", "Push", yesterday)
        val old = hrWindow(push, zone)!!
        io { dao.upsert(push); dao.upsertSessionHr(filled(push)) }
        fake.answer = { r -> if (r == old) throw IllegalStateException("no provider") else everyMinute(r, bpm = 150) }
        screen()
        openSheet("Push")
        fake.hold()
        click("Read again")
        reached()
        show { androidx.compose.material3.Text("Settings screen") }
        shows("Settings screen")
        io { dao.upsert(push.copy(timedTo = push.timedTo!! + 60_000)) }
        screen()
        openSheet("Push")
        shows("18:02 to 19:15")
        click("Read again")
        fake.release()
        compose.waitUntil(5_000) { compose.waitForIdle(); fake.active == 0 }
        readDone()
        shows("150 bpm")
        assertTrue(texts(failure).isEmpty())
    }

    @Test fun `a failure stays after a recomposition and a rotation`() {
        val push = gym("g", "Push", yesterday)
        io { dao.upsert(push); dao.upsertSessionHr(filled(push)) }
        fake.answer = { throw IllegalStateException("no provider") }
        val restore = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        restore.setContent {
            KhonTheme {
                androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) {
                    HeartDataScreen(vm, rememberNavController(), heart = fill, access = access)
                }
            }
        }
        openSheet("Push")
        click("Read again")
        shows(failure)
        io { dao.upsert(gym("p", "Pull", today.minusDays(2))) }
        waitSaid("Pull,")
        assertTrue(texts(failure).isNotEmpty())
        restore.emulateSavedInstanceStateRestore()
        shows("Time range")
        shows(failure)
        shows("128 bpm")
    }

    private fun closeSheet() {
        compose.onNodeWithContentDescription("Close sheet").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    @Test fun `the sheet shows measured values where Review shows the typed ones`() {
        val run = Session("c", yesterday.iso(), t(7, 0), t(7, 31), ItemType.CARDIO, exerciseId = "z2", exerciseName = "Zone 2 run", timedFrom = t(7, 0), timedTo = t(7, 30), avgHr = 141)
        io {
            dao.upsert(Exercise("z2", "Zone 2 run", Kind.CARDIO, intervals = true, rounds = 1, fields = "time,avgHr"))
            dao.upsert(run)
            dao.upsertIntervalLogs(listOf(IntervalLog("i1", "c", 0, avgHr = 150, startedAt = t(7, 0), endedAt = t(7, 1))))
            dao.upsertSessionHr(filled(run).copy(samples = "0:140,5:148"))
        }
        show { ReviewScreen(vm, rememberNavController(), "c") }
        shows("Round 1")
        shows("150 bpm"); shows("141 bpm")
        assertTrue(texts("144 bpm").isEmpty())
        screen()
        openSheet("Zone 2 run")
        shows("144 bpm"); shows("128 bpm")
        assertTrue(texts("150 bpm").isEmpty()); assertTrue(texts("141 bpm").isEmpty())
    }

    @Test fun `a read works with heart rate access only`() {
        access = HeartAccess.withPermissions({ HcStatus.AVAILABLE }, { setOf(dev.mirzohidkhon.khonfitness.health.HealthHeartSource.HEART_READ) })
        val push = gym("g", "Push", yesterday)
        io { dao.upsert(push) }
        fake.answer = { r -> everyMinute(r, bpm = 133) }
        screen()
        openSheet("Push")
        assertTrue(texts("Read heart rate").isEmpty()); assertTrue(texts("Allow").isEmpty())
        click("Read again")
        shows("133 bpm")
        assertEquals(HrState.FILLED, io2 { dao.sessionHr("g") }!!.state)
        access = HeartAccess.withPermissions({ HcStatus.AVAILABLE }, { emptySet() })
        screen()
        shows("Read heart rate")
    }

    @Test fun `a restored cardio form saves no placeholder, and a typed session average is saved`() {
        val run = Session("c", yesterday.iso(), t(7, 0), t(7, 31), ItemType.CARDIO, exerciseId = "z2", exerciseName = "Zone 2 run", timedFrom = t(7, 0), timedTo = t(7, 30), timeSec = 1800)
        io {
            dao.upsert(Exercise("z2", "Zone 2 run", Kind.CARDIO, intervals = true, rounds = 2, fields = "time,avgHr"))
            dao.upsert(run)
            dao.upsertIntervalLogs(listOf(IntervalLog("i1", "c", 0, startedAt = t(7, 0), endedAt = t(7, 1)), IntervalLog("i2", "c", 1, startedAt = t(7, 5), endedAt = t(7, 6))))
            dao.upsertSessionHr(filled(run).copy(samples = "0:120,5:130,300:150,305:160"))
        }
        val restore = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        var generation by mutableIntStateOf(0)
        restore.setContent {
            KhonTheme {
                androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) {
                    key(generation) {
                        val nav = rememberNavController()
                        var opened by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
                        NavHost(nav, startDestination = "history") {
                            composable("history") {
                                androidx.compose.material3.Text("History screen")
                                LaunchedEffect(Unit) { if (!opened) { opened = true; nav.navigate("cardio") } }
                            }
                            composable("cardio") { CardioScreen(vm, nav, "c") }
                        }
                    }
                }
            }
        }
        shows("Round 2"); shows("128"); shows("125"); shows("155")
        restore.emulateSavedInstanceStateRestore()
        shows("Round 2"); shows("128"); shows("125"); shows("155")
        compose.onNodeWithText("Save").performClick()
        shows("History screen"); gone("Round 2")
        assertNull(io2 { dao.session("c") }!!.avgHr)
        assertEquals(listOf(null, null), io2 { dao.intervalLogsFor("c") }.map { it.avgHr })

        compose.runOnIdle { generation++ }
        shows("Round 2"); shows("128")
        compose.onNode(androidx.compose.ui.test.hasSetTextAction() and androidx.compose.ui.test.hasAnyDescendant(androidx.compose.ui.test.hasText("128")), useUnmergedTree = true)
            .performTextReplacement("137")
        compose.waitForIdle()
        compose.onNodeWithText("Save").performClick()
        shows("History screen"); gone("Round 2")
        assertEquals(137, io2 { dao.session("c") }!!.avgHr)
        assertEquals(listOf(null, null), io2 { dao.intervalLogsFor("c") }.map { it.avgHr })
    }

    private fun <T> io2(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }
}
