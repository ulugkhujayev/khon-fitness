package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.NightStore
import dev.mirzohidkhon.khonfitness.data.NightsState
import dev.mirzohidkhon.khonfitness.health.HcStatus
import dev.mirzohidkhon.khonfitness.health.HealthSleepSource
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h2400dp")
class NightsScreenTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private lateinit var scope: CoroutineScope
    private lateinit var store: NightStore
    private val source = FakeSleepSource()
    private val zone = ZoneId.systemDefault()
    private val clock = nightTestClock(zone)
    private fun nov(d: Int) = LocalDate.of(2026, 11, d)

    @Before fun open() {
        app = newApp()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        store = installNights(app, source, scope, zone)
        vm = AppViewModel(app)
        io { vm.maxHeartRate.set(LOAD_FIXTURE_MAX) }
    }

    @After fun close() {
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
        vm.viewModelScope.cancel(); scope.cancel(); app.scope.cancel(); app.db.close()
    }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }
    private fun texts(text: String) = compose.onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes()
    private fun said(text: String) = compose.onAllNodesWithContentDescription(text, substring = true).fetchSemanticsNodes()
    private fun shows(text: String) = compose.waitUntil(5_000) { texts(text).isNotEmpty() }
    private fun gone(text: String) = compose.waitUntil(5_000) { texts(text).isEmpty() }
    private fun speaks(text: String) = compose.waitUntil(5_000) { said(text).isNotEmpty() }
    private fun textBox(text: String): Rect = texts(text).single().boundsInRoot
    private fun exactBox(text: String): Rect = compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().single().boundsInRoot
    private fun firstBox(text: String): Rect = texts(text).minBy { it.boundsInRoot.top }.boundsInRoot
    private fun saidBox(text: String): Rect = said(text).single().boundsInRoot
    private fun tagged(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes()
    private fun click(text: String) { compose.onNodeWithText(text).performScrollTo().performSemanticsAction(SemanticsActions.OnClick); compose.waitForIdle() }

    private fun show(content: @Composable () -> Unit) = compose.setContent {
        KhonTheme { Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) { content() } }
    }

    private fun nights(status: () -> HcStatus = { HcStatus.AVAILABLE }) = show { NightsScreen(vm, rememberNavController(), clock, hcStatus = status) }

    private fun waitState(check: (NightsState) -> Boolean) = compose.waitUntil(5_000) { check(store.state.value) }

    @Test fun `U1 the Log shows the night line under the last row and the stretch lines, from the first night to today`() {
        source.spans = logWeekSpans(zone)
        io { seedLoad(app, logWeekFixtures(zone)); app.db.dao().upsertStretchSessions(listOf(stretchRun(nov(24), zone))) }
        show { HistoryScreen(vm, rememberNavController(), clock) }
        shows("Night 7 h 05 m · 23:10 – 06:15")
        shows("Night 10 h 05 m · 23:10 – 09:15")
        shows(NO_NIGHT_DATA)
        assertEquals(1, texts("Night 7 h 05 m").size)
        assertEquals(2, texts("Night ").size)
        assertEquals(1, texts(NO_NIGHT_DATA).size)

        val night = textBox("Night 7 h 05 m")
        assertTrue(night.top >= textBox("Stretch: Morning").bottom)
        assertTrue(night.top > textBox("Zone 2 run").bottom)
        assertTrue(textBox("Zone 2 run").top > textBox("Push").bottom)
        val restDay = textBox("Night 10 h 05 m")
        assertTrue(textBox(NO_NIGHT_DATA).top > restDay.bottom)

        val tuesday = compose.onNode(hasContentDescription("Night 7 hours 5 minutes, 23:10 to 06:15", substring = true)).fetchSemanticsNode()
        assertTrue(tuesday.config.getOrNull(SemanticsProperties.Text).orEmpty().any { it.text == "Zone 2 run" })
        val wednesday = compose.onNode(hasContentDescription("Night 10 hours 5 minutes, 23:10 to 09:15", substring = true)).fetchSemanticsNode()
        assertTrue(wednesday.config.getOrNull(SemanticsProperties.Text).orEmpty().any { it.text == "Rest" })
    }

    private fun logWithout(mode: SleepMode, reached: (NightsState) -> Boolean) {
        source.spans = logWeekSpans(zone)
        source.mode = mode
        io { seedLoad(app, logWeekFixtures(zone)) }
        show { HistoryScreen(vm, rememberNavController(), clock) }
        shows("Zone 2 run")
        compose.waitUntil(5_000) { source.reads.get() >= 1 && reached(store.state.value) }
        compose.waitForIdle()
        assertEquals(0, texts("Night ").size)
        assertEquals(0, texts(NO_NIGHT_DATA).size)
    }

    @Test fun `U2 no night line while the nights are not read yet`() = logWithout(SleepMode.HANG) { it == NightsState.NotRead }
    @Test fun `U2 no night line without access`() = logWithout(SleepMode.NO_ACCESS) { it == NightsState.NoAccess }
    @Test fun `U2 no night line after a failed read`() = logWithout(SleepMode.FAIL) { it == NightsState.Failed }

    @Test fun `U3 the Nights row on Progress opens the Nights screen and Back returns`() {
        show {
            val nav = rememberNavController()
            NavHost(nav, Routes.HISTORY) {
                composable(Routes.HISTORY) { HistoryScreen(vm, nav, clock) }
                composable(Routes.NIGHTS) { NightsScreen(vm, nav, clock, hcStatus = { HcStatus.AVAILABLE }) }
            }
        }
        shows("Progress")
        compose.onNodeWithText("Progress").performSemanticsAction(SemanticsActions.OnClick)
        shows("Compare two periods")
        val compare = textBox("Compare two periods")
        val row = exactBox("Nights")
        assertTrue(row.top > compare.bottom && row.top - compare.bottom < with(compose.density) { 30.dp.toPx() })
        compose.onNodeWithText("Nights").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        shows(NIGHTS_EMPTY)
        compose.onNodeWithContentDescription("Back").performClick()
        shows("Compare two periods")
        gone(NIGHTS_EMPTY)
    }

    @Test fun `U4 the three no-access states, and Allow asks only for sleep and loads the nights`() {
        var status = HcStatus.NOT_INSTALLED
        source.mode = SleepMode.NO_ACCESS
        val registry = RecordingRegistryOwner { source.mode = SleepMode.ANSWER; source.spans = chartSpans(zone) }
        show { CompositionLocalProvider(LocalActivityResultRegistryOwner provides registry) { NightsScreen(vm, rememberNavController(), clock, hcStatus = { status }) } }
        shows("The band's app writes workouts to Health Connect")
        assertEquals(2, texts("Get Health Connect").size)
        shows("Check again")

        status = HcStatus.UPDATE_REQUIRED
        click("Check again")
        shows("Health Connect needs an update before it can share data.")
        assertEquals(2, texts("Update Health Connect").size)

        status = HcStatus.AVAILABLE
        click("Check again")
        shows("Read sleep")
        shows(NIGHTS_ALLOW_LINE)
        assertEquals(0, texts("Check again").size)
        assertTrue(registry.inputs.isEmpty())

        click("Allow")
        speaks("average · 11 of 14 nights")
        assertEquals(listOf<Any?>(setOf(HealthSleepSource.SLEEP_READ)), registry.inputs)
        gone(NIGHTS_ALLOW_LINE)
    }

    @Test fun `U5 the figures of 11 nights of 14`() {
        source.spans = chartSpans(zone)
        nights()
        speaks("average · 11 of 14 nights, 6 hours 51 minutes")
        speaks("shortest · Nov 18, 5 hours 40 minutes")
        shows("6 h 51 m")
        shows("5 h 40 m")
    }

    @Test fun `U5 a dash and the captions with no night in the 14 days but older nights`() {
        source.spans = listOf(nightSpan(LocalDate.of(2026, 10, 20), 400, zone), nightSpan(LocalDate.of(2026, 10, 21), 410, zone))
        nights()
        speaks("average · 0 of 14 nights, none")
        speaks("shortest, none")
        assertEquals(2, texts(NO_VALUE).size)
        assertEquals(14, (0L until 14L).count { tagged(nightBarTag(NIGHT_TODAY.minusDays(it))).isNotEmpty() })
        assertTrue((0L until 14L).none { tagged(nightFillTag(NIGHT_TODAY.minusDays(it))).isNotEmpty() })
    }

    @Test fun `U5 the empty text with no night at all`() {
        nights()
        shows(NIGHTS_EMPTY)
        assertEquals(0, texts("average").size)
    }

    @Test fun `U5 a failed read shows the failed text, and Try again reads again`() {
        source.mode = SleepMode.FAIL
        nights()
        shows(NIGHTS_FAILED)
        val before = source.reads.get()
        source.mode = SleepMode.ANSWER
        source.spans = chartSpans(zone)
        click("Try again")
        speaks("average · 11 of 14 nights")
        assertEquals(before + 1, source.reads.get())
        gone(NIGHTS_FAILED)
    }

    @Test fun `U6 one bar per night, the dots of the finished sessions, the spoken text of each bar`() {
        source.spans = chartSpans(zone)
        io { seedLoad(app, chartFixtures(zone)) }
        nights()
        speaks("Thursday, November 19, 6 hours 40 minutes, Zone 2 run, Push, Norwegian 4x4")
        assertEquals(14, (0L until 14L).count { tagged(nightBarTag(NIGHT_TODAY.minusDays(it))).isNotEmpty() })
        assertEquals(11, (0L until 14L).count { tagged(nightFillTag(NIGHT_TODAY.minusDays(it))).isNotEmpty() })
        assertTrue(tagged(nightFillTag(nov(20))).isEmpty())
        assertTrue(tagged(nightFillTag(nov(26))).isEmpty())
        assertEquals(2, tagged(nightDotTag(nov(19))).size)
        assertEquals(2, tagged(nightDotTag(nov(24))).size)
        assertEquals(1, tagged(nightDotTag(nov(17))).size)
        assertEquals(0, tagged(nightDotTag(nov(20))).size)
        assertEquals(1, said("Friday, November 20, no night data").size)
        assertEquals(1, said("Tuesday, November 24, 7 hours 5 minutes, Zone 2 run, Push").size)
        assertEquals(14, said(", November ").size)

        val plot = with(compose.density) { 132.dp.toPx() }
        val longest = tagged(nightFillTag(nov(21))).single().boundsInRoot.height
        assertEquals(plot * 460f / (8 * 60), longest, 2f)
    }

    @Test fun `U6 a night of 9 h 10 m makes the axis end at 10 hours`() {
        source.spans = chartSpans(zone, longestMin = 550)
        nights()
        speaks("average · 11 of 14 nights")
        val plot = with(compose.density) { 132.dp.toPx() }
        assertEquals(plot * 550f / (10 * 60), tagged(nightFillTag(nov(21))).single().boundsInRoot.height, 2f)
        assertEquals(plot * 340f / (10 * 60), tagged(nightFillTag(nov(18))).single().boundsInRoot.height, 2f)
    }

    @Test fun `U7 the night before a session, most nights first, then the title`() {
        source.spans = chartSpans(zone)
        io { seedLoad(app, chartFixtures(zone)) }
        nights()
        shows(NIGHT_BEFORE_TITLE)
        val rows = listOf("Push, 4 nights, ", "Zone 2 run, 3 nights, ", "Norwegian 4x4, 1 night, ", "$LONG_TITLE, 1 night, ")
        rows.forEach { speaks(it) }
        val tops = rows.map { saidBox(it).top }
        assertEquals(tops.sorted(), tops)
        shows("4 nights"); shows("1 night")
        assertEquals(1, said("Push, 4 nights, 7 hours 9 minutes").size)
    }

    @Test fun `U7 no list without a session that has a night`() {
        source.spans = chartSpans(zone)
        nights()
        speaks("average · 11 of 14 nights")
        assertEquals(0, texts(NIGHT_BEFORE_TITLE).size)
    }

    private fun review(id: String) = show { ReviewScreen(vm, rememberNavController(), id) }

    @Test fun `U8 a gym session with a night shows the two rows after the heart picture and before the exercises`() {
        source.spans = logWeekSpans(zone)
        io { seedLoad(app, logWeekFixtures(zone)) }
        review("log-g")
        speaks("Night before, 7 hours 5 minutes")
        speaks("Asleep, 23:10 to 06:15")
        val night = saidBox("Night before, ")
        compose.waitUntil(5_000) { tagged(HEART_PICTURE_TAG).isNotEmpty() }
        assertTrue(night.top >= tagged(HEART_PICTURE_TAG).single().boundsInRoot.bottom)
        assertTrue(saidBox("Asleep, ").top > night.top)
        assertTrue(firstBox(GYM_LIFTS.first().first).top > saidBox("Asleep, ").bottom)
    }

    @Test fun `U8 a cardio session with a night shows the two rows before the cardio rows`() {
        source.spans = logWeekSpans(zone)
        io { seedLoad(app, logWeekFixtures(zone)) }
        review("log-z")
        speaks("Night before, 7 hours 5 minutes")
        val asleep = saidBox("Asleep, ")
        shows("Type")
        assertTrue(textBox("Type").top > asleep.bottom)
        if (tagged(HEART_PICTURE_TAG).isNotEmpty()) assertTrue(saidBox("Night before, ").top >= tagged(HEART_PICTURE_TAG).single().boundsInRoot.bottom)
    }

    @Test fun `U8 a session without a night has no night rows`() {
        source.spans = logWeekSpans(zone)
        io { seedLoad(app, listOf(gymFixture(nov(23), zone, id = "mon"))) }
        review("mon")
        shows(GYM_LIFTS.first().first)
        compose.waitUntil(5_000) { store.state.value is NightsState.Loaded }
        compose.waitForIdle()
        assertEquals(0, said("Night before").size)
        assertEquals(0, texts("Night before").size)
    }

    @Test fun `U9 Compare shows the Nights group with both rows when the periods have nights`() {
        source.spans = logWeekSpans(zone) + nightSpan(LocalDate.of(2026, 10, 20), 400, zone)
        show { CompareScreen(vm, rememberNavController(), clock) }
        speaks("Sleep per night, ")
        speaks("Nights with data, ")
        shows("Nights")
        val title = exactBox("Nights")
        assertTrue(saidBox("Sleep per night, ").top > title.bottom)
        val sleep = said("Sleep per night, ").single().config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().single()
        assertTrue(sleep, "8 h 35 m" in sleep && "6 h 40 m" in sleep)
        val count = said("Nights with data, ").single().config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().single()
        assertTrue(count, "2 in" in count && "1 in" in count)
    }

    @Test fun `U9 Compare has no Nights group when neither period has a night`() {
        show { CompareScreen(vm, rememberNavController(), clock) }
        compose.waitUntil(5_000) { store.state.value is NightsState.Loaded }
        compose.waitForIdle()
        assertEquals(0, said("Sleep per night").size)
        assertEquals(0, texts("Sleep per night").size)
    }

    private fun readsAgainOnResume(content: @Composable () -> Unit, shown: String) {
        source.mode = SleepMode.FAIL
        show(content)
        shows(shown)
        compose.waitUntil(5_000) { store.state.value == NightsState.Failed }
        val before = source.reads.get()
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        compose.waitUntil(5_000) { source.reads.get() == before + 1 }
    }

    @Test fun `the Nights screen reads when it opens and again when it resumes`() =
        readsAgainOnResume({ NightsScreen(vm, rememberNavController(), clock, hcStatus = { HcStatus.AVAILABLE }) }, NIGHTS_FAILED)

    @Test fun `History reads the nights again when it resumes`() = readsAgainOnResume({ HistoryScreen(vm, rememberNavController(), clock) }, "Log")
}
