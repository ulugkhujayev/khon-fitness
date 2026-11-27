package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.openTab
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.Dispatchers
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
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ReviewFromHistoryTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private lateinit var nav: NavHostController
    private val dao get() = app.db.dao()
    private val zone = ZoneId.systemDefault()
    private val day = LocalDate.now(zone).minusDays(1)
    private val gym = gymFixture(day, zone)
    private val cardio = cardioFixture(day, zone)

    @Before fun open() {
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
    }

    @After fun close() { vm.viewModelScope.cancel(); app.scope.cancel(); app.db.close() }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }
    private fun seed(f: PictureFixture, withRow: Boolean = true, session: Session = f.session) = io {
        dao.upsert(session); dao.upsertSetLogs(f.logs); dao.upsertIntervalLogs(f.rounds)
        if (withRow) f.row?.let { dao.upsertSessionHr(it) }
    }
    private fun routes() = nav.currentBackStack.value.mapNotNull { it.destination.route }
    private fun until(what: String, ok: () -> Boolean) {
        repeat(250) { if (ok()) return; compose.waitForIdle(); Thread.sleep(20) }
        throw AssertionError("never: $what; routes ${routes()}")
    }
    private fun named(label: String) = compose.onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty()
    private fun shows(text: String) = compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    private fun reviewRows() = compose.onAllNodes(hasContentDescription("Review", substring = true) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)).fetchSemanticsNodes()
    private fun click(label: String): () -> Boolean = compose.onNodeWithContentDescription(label).fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
    private fun clickRow(): () -> Boolean = reviewRows().single().config[SemanticsActions.OnClick].action!!
    private fun twice(click: () -> Boolean) = compose.runOnUiThread { click(); click() }
    private fun systemBack() { compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }; compose.waitForIdle() }

    private fun spoken(f: PictureFixture) = reviewEntrySpoken(hrSpoken(f.session, hrFacts(f.session, f.row, zone)))
    private fun line(f: PictureFixture) = hrLine(f.session, hrFacts(f.session, f.row, zone))!!
    private fun tagged(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    private fun roundValue(f: PictureFixture, i: Int) = "${shownRoundHr(f.rounds[i], hrFacts(f.session, f.row, zone))} bpm"

    private fun openFromHistory(route: String) {
        compose.setContent {
            KhonTheme {
                nav = rememberNavController()
                NavHost(nav, Routes.TODAY) {
                    composable(Routes.TODAY) { TodayScreen(vm, nav) }
                    composable(Routes.HISTORY) { HistoryScreen(vm, nav) }
                    composable("session/{id}") { e -> SessionScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("cardio/{id}") { e -> CardioScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("review/{id}") { e -> ReviewScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                }
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { nav.openTab(Routes.HISTORY) }
        compose.waitForIdle()
        compose.runOnIdle { nav.navigate(route) }
        compose.waitForIdle()
    }

    @Test fun `a finished gym session has the Review row first, with Review's heart rate line, and none in Edit mode`() {
        seed(gym); openFromHistory(Routes.session("gym"))
        until("the row speaks the line") { named(spoken(gym)) }
        assertEquals("Review, average ${gym.row!!.avgHr}, maximum ${gym.row!!.maxHr} beats per minute", spoken(gym))
        assertTrue(shows(line(gym)))
        val row = reviewRows().single()
        val blocks = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription)).fetchSemanticsNodes()
        assertTrue("the row is above every block", blocks.isNotEmpty() && blocks.all { it.boundsInRoot.top >= row.boundsInRoot.bottom })
        compose.onNodeWithText("Edit").performSemanticsActionClick()
        until("Edit mode has no row") { reviewRows().isEmpty() && shows("Done") }
        compose.onNodeWithText("Done").performSemanticsActionClick()
        until("the row is back after Done") { reviewRows().size == 1 }
    }

    @Test fun `an unfinished gym session and an unfinished cardio session have no Review row`() {
        seed(gym, session = gym.session.copy(finishedAt = null)); seed(cardio, session = cardio.session.copy(finishedAt = null))
        openFromHistory(Routes.session("gym"))
        until("the running session shows") { compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription)).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(reviewRows().isEmpty())
        compose.runOnIdle { nav.popBackStack(); nav.navigate(Routes.cardio("cardio")) }
        until("the form shows") { shows("Discard") }
        assertTrue(reviewRows().isEmpty())
    }

    @Test fun `a waiting read and no read give the row no value, and a fill that commits while the screen is open shows`() {
        seed(gym, withRow = false)
        io { dao.upsertSessionHr(gym.row!!.copy(state = HrState.WAITING, samples = "", avgHr = null, maxHr = null)) }
        openFromHistory(Routes.session("gym"))
        until("the row shows") { reviewRows().size == 1 }
        assertEquals(listOf("Review"), reviewRows().single().config[SemanticsProperties.ContentDescription])
        assertEquals(listOf("Review"), reviewRows().single().config.getOrNull(SemanticsProperties.Text)?.map { it.text })
        io { dao.deleteSessionHr("gym") }
        compose.waitForIdle()
        assertEquals(listOf("Review"), reviewRows().single().config[SemanticsProperties.ContentDescription])
        io { dao.upsertSessionHr(gym.row!!) }
        until("the committed fill shows") { named(spoken(gym)) && shows(line(gym)) }
    }

    @Test fun `a fill that commits while the cardio form is open shows in its row`() {
        seed(cardio, withRow = false)
        io { dao.upsertSessionHr(cardio.row!!.copy(state = HrState.WAITING, samples = "", avgHr = null, maxHr = null)) }
        openFromHistory(Routes.cardio("cardio"))
        until("the row shows") { reviewRows().size == 1 }
        assertEquals(listOf("Review"), reviewRows().single().config[SemanticsProperties.ContentDescription])
        io { dao.upsertSessionHr(cardio.row!!) }
        until("the committed fill shows") { named(spoken(cardio)) && shows(line(cardio)) }
    }

    @Test fun `the row opens Review once, Back says History, and the bar Back and system Back return to the session as it was`() {
        seed(gym); openFromHistory(Routes.session("gym"))
        until("the row shows") { reviewRows().size == 1 }
        val blocks = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription)).fetchSemanticsNodes()
        compose.runOnUiThread { blocks[1].config[SemanticsActions.OnClick].action!!() }
        compose.waitForIdle(); Thread.sleep(400); compose.waitForIdle()
        compose.onNode(hasScrollAction()).performScrollToIndex(0)
        compose.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 20f) }
        compose.waitForIdle()
        fun state() = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription)).fetchSemanticsNodes()
            .map { it.config[SemanticsProperties.ContentDescription].single() to it.config[SemanticsProperties.StateDescription] + " " + it.boundsInRoot.top.toInt() }
        val before = state()
        assertTrue(before.toString(), before.any { "Expanded" in it.second })
        twice(clickRow())
        until("Review shows") { routes().lastOrNull() == "review/{id}" && shows("Sets in order") }
        assertEquals(listOf("today", "history", "session/{id}", "review/{id}"), routes())
        until("Review draws the heart rate picture") { tagged(HEART_PICTURE_TAG) }
        assertTrue("Back names History", named("History"))
        twice(click("History"))
        until("the session screen is back") { routes().lastOrNull() == "session/{id}" && reviewRows().size == 1 }
        assertEquals(listOf("today", "history", "session/{id}"), routes())
        compose.waitForIdle()
        assertEquals("the open block and its place", before, state())
        compose.runOnUiThread { clickRow()() }
        until("Review shows again") { routes().lastOrNull() == "review/{id}" && shows("Sets in order") }
        systemBack()
        until("system Back returns") { routes().lastOrNull() == "session/{id}" && reviewRows().size == 1 }
        assertEquals(listOf("today", "history", "session/{id}"), routes())
        systemBack()
        until("the next Back leaves for History") { routes() == listOf("today", "history") }
    }

    @Test fun `Review from Today says Today`() {
        seed(gym)
        compose.setContent {
            KhonTheme {
                nav = rememberNavController()
                NavHost(nav, Routes.TODAY) {
                    composable(Routes.TODAY) { TodayScreen(vm, nav) }
                    composable("review/{id}") { e -> ReviewScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                }
            }
        }
        compose.runOnIdle { nav.navigate(Routes.review("gym")) }
        until("Review shows") { shows("Sets in order") }
        assertTrue(named("Today"))
        compose.runOnUiThread { click("Today")() }
        until("Today shows") { routes() == listOf("today") }
    }

    @Test fun `a finished cardio session has the row only while nothing is typed`() {
        seed(cardio); openFromHistory(Routes.cardio("cardio"))
        until("the row speaks the line") { named(spoken(cardio)) && shows(line(cardio)) }
        compose.onNodeWithContentDescription("Note").performTextInput("easy")
        until("a typed field hides the row") { reviewRows().isEmpty() }
        compose.onNodeWithText("Save").performSemanticsActionClick()
        until("Save goes back to History") { routes() == listOf("today", "history") }
        compose.runOnIdle { nav.navigate(Routes.cardio("cardio")) }
        until("the saved session has the row again") { reviewRows().size == 1 }
        compose.onNodeWithContentDescription("Round 1 heart rate").performTextInput("150")
        until("a typed round hides the row") { reviewRows().isEmpty() }
    }

    @Test fun `the cardio row opens Review once and Back returns to the form`() {
        seed(cardio); openFromHistory(Routes.cardio("cardio"))
        until("the row shows") { reviewRows().size == 1 }
        compose.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 20f) }
        compose.waitForIdle()
        fun place() = compose.onNodeWithText("Round 1", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top
        val before = place()
        twice(clickRow())
        until("Review shows") { routes().lastOrNull() == "review/{id}" && shows("Norwegian 4×4") }
        assertEquals(listOf("today", "history", "cardio/{id}", "review/{id}"), routes())
        assertTrue(named("History"))
        until("Review draws the heart rate picture") { tagged(HEART_PICTURE_TAG) }
        until("Review shows the cardio figures") { shows("Intervals") && shows(roundValue(cardio, 0)) && shows(roundValue(cardio, cardio.rounds.lastIndex)) }
        systemBack()
        until("the form is back") { routes().lastOrNull() == "cardio/{id}" && reviewRows().size == 1 }
        assertEquals(listOf("today", "history", "cardio/{id}"), routes())
        assertEquals("the scroll place", before, place(), 0.5f)
        compose.runOnUiThread { clickRow()() }
        until("Review shows again") { routes().lastOrNull() == "review/{id}" && shows("Intervals") }
        twice(click("History"))
        until("the bar Back returns to the form") { routes().lastOrNull() == "cardio/{id}" && reviewRows().size == 1 }
        assertEquals(listOf("today", "history", "cardio/{id}"), routes())
        assertEquals("the scroll place after the bar Back", before, place(), 0.5f)
    }

    @Test fun `a session deleted under Review takes Review and its session screen away to History`() {
        seed(gym); seed(cardio)
        openFromHistory(Routes.session("gym"))
        until("the row shows") { reviewRows().size == 1 }
        compose.runOnUiThread { clickRow()() }
        until("Review shows") { routes().lastOrNull() == "review/{id}" && shows("Sets in order") }
        io { vm.repo.deleteSession("gym") }
        until("History shows") { routes() == listOf("today", "history") }
        until("History draws its log with the cardio session and without the deleted one") { shows("Log") && shows("Norwegian 4×4") && !shows("Push") }
        compose.runOnIdle { nav.navigate(Routes.cardio("cardio")) }
        until("the form row shows") { reviewRows().size == 1 }
        compose.runOnUiThread { clickRow()() }
        until("Review shows") { routes().lastOrNull() == "review/{id}" }
        io { vm.repo.deleteSession("cardio") }
        until("History shows") { routes() == listOf("today", "history") }
        until("History draws its log without the deleted session") { shows("Log") && !shows("Norwegian 4×4") }
    }

    @Test fun `the row speaks Review and the line, as a button`() {
        assertEquals("Review", reviewEntrySpoken(null))
        assertEquals("Review, average 121, maximum 163 beats per minute", reviewEntrySpoken("average 121, maximum 163 beats per minute"))
        assertEquals("History", reviewBack(Routes.session("x").substringBefore("/") + "/{id}"))
        assertEquals("History", reviewBack("cardio/{id}"))
        assertEquals("Today", reviewBack(Routes.TODAY))
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.performSemanticsActionClick() {
        val action = fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnUiThread { action() }
        compose.waitForIdle()
    }
}
