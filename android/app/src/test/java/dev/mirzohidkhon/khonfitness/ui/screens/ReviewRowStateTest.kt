package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
@Config(application = Application::class, qualifiers = "w360dp-h480dp-xxhdpi")
class ReviewRowStateTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var app: KhonApp
    private lateinit var db: KhonDatabase
    private lateinit var vm: AppViewModel
    private lateinit var nav: NavHostController
    private val zone = ZoneId.systemDefault()
    private val day = LocalDate.now(zone).minusDays(1)

    @Before fun open() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java)
            .apply { isAccessible = true }.invoke(app, context)
        db = Room.inMemoryDatabaseBuilder(context, KhonDatabase::class.java).build()
    }

    @After fun close() {
        if (::vm.isInitialized) vm.viewModelScope.cancel()
        app.scope.cancel()
        db.close()
    }

    private fun useDao(dao: KhonDao) {
        KhonApp::class.java.getDeclaredField("repo\$delegate").apply { isAccessible = true }.set(app, lazy { KhonRepository(dao) })
        vm = AppViewModel(app)
    }

    private fun seed(f: PictureFixture, session: Session = f.session) {
        val dao = db.dao()
        runBlocking(Dispatchers.IO) { dao.upsert(session); dao.upsertSetLogs(f.logs); dao.upsertIntervalLogs(f.rounds) }
        useDao(dao)
    }

    private fun routes() = nav.currentBackStack.value.mapNotNull { it.destination.route }
    private fun until(what: String, ok: () -> Boolean) {
        repeat(250) { compose.waitForIdle(); if (ok()) return; Thread.sleep(20) }
        throw AssertionError("never: $what; routes ${routes()}")
    }

    private fun openFromHistory(route: String) {
        compose.setContent {
            KhonTheme {
                nav = rememberNavController()
                NavHost(nav, Routes.TODAY, enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None }) {
                    composable(Routes.TODAY) { Text("Today tab") }
                    composable(Routes.HISTORY) { Text("History tab") }
                    composable("cardio/{id}") { e -> CardioScreen(vm, nav, e.arguments!!.getString("id")!!) }
                    composable("session/{id}") { e -> SessionScreen(vm, nav, e.arguments!!.getString("id")!!) }
                    composable("review/{id}") { e -> ReviewScreen(vm, nav, e.arguments!!.getString("id")!!) }
                }
            }
        }
        compose.runOnIdle { nav.navigate(Routes.HISTORY) }
        compose.runOnIdle { nav.navigate(route) }
    }

    private fun reviewAction(): () -> Boolean {
        until("the Review row shows") { compose.onAllNodes(hasContentDescription("Review", substring = true)).fetchSemanticsNodes().size == 1 }
        return compose.onNode(hasContentDescription("Review", substring = true)).fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
    }
    private fun clickAction(text: String) = compose.onNodeWithText(text).fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
    private fun shows(text: String) = compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    private fun named(label: String) = compose.onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty()
    private fun stored(id: String) = runBlocking(Dispatchers.IO) { db.dao().sessionFlow(id).first() }

    @Test fun `a typed note and the Review action of the last frame keep the form and the note, and Save completes`() {
        val f = cardioFixture(day, zone); seed(f)
        openFromHistory(Routes.cardio(f.session.id))
        val review = reviewAction()
        val setNote = compose.onNodeWithContentDescription("Note").fetchSemanticsNode().config[SemanticsActions.SetText].action!!
        compose.runOnUiThread { setNote(AnnotatedString("keep this note")); review() }
        compose.waitForIdle()
        assertEquals("cardio/{id}", routes().last())
        assertTrue(shows("keep this note"))
        compose.runOnUiThread { clickAction("Save")() }
        until("Save leaves for History") { routes() == listOf("today", "history") }
        assertEquals("keep this note", stored(f.session.id)?.note)
    }

    @Test fun `a typed round value and the Review action of the last frame keep the form and the value`() {
        val f = cardioFixture(day, zone); seed(f)
        openFromHistory(Routes.cardio(f.session.id))
        val review = reviewAction()
        val setRound = compose.onNodeWithContentDescription("Round 1 heart rate").fetchSemanticsNode().config[SemanticsActions.SetText].action!!
        compose.runOnUiThread { setRound(AnnotatedString("150")); review() }
        compose.waitForIdle()
        assertEquals("cardio/{id}", routes().last())
        assertEquals("150", compose.onNodeWithContentDescription("Round 1 heart rate").fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
    }

    @Test fun `a Review tap while Save leaves the form does nothing, and Save completes`() {
        val f = cardioFixture(day, zone); seed(f)
        openFromHistory(Routes.cardio(f.session.id))
        val review = reviewAction()
        val save = clickAction("Save")
        compose.runOnUiThread { save(); review() }
        until("Save leaves for History") { routes() == listOf("today", "history") }
    }

    @Test fun `a Review tap after Edit took effect does nothing, and Edit after a Review tap does nothing`() {
        val f = gymFixture(day, zone, heart = false); seed(f)
        openFromHistory(Routes.session(f.session.id))
        var review = reviewAction()
        compose.runOnUiThread { clickAction("Edit")(); review() }
        compose.waitForIdle()
        assertEquals(listOf("today", "history", "session/{id}"), routes())
        assertTrue(shows("Done"))
        compose.runOnUiThread { clickAction("Done")() }
        review = reviewAction()
        val edit = clickAction("Edit")
        compose.runOnUiThread { review(); edit() }
        until("Review shows") { routes().last() == "review/{id}" }
        compose.runOnUiThread { nav.popBackStack() }
        until("the session screen is back, not in Edit mode") { routes().last() == "session/{id}" && shows("Edit") && !shows("Done") }
    }

    private fun blockTops(): Map<String, Float> = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription)).fetchSemanticsNodes()
        .associate { it.config[SemanticsProperties.ContentDescription].single().substringBefore(",") to it.boundsInRoot.top }

    private fun openBlock2Then(toggle: String) {
        val list = compose.onNode(hasScrollAction()).fetchSemanticsNode().boundsInRoot.top
        val block = compose.onNode(hasContentDescription("Block 2,", substring = true)).fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        val bar = clickAction(toggle)
        compose.mainClock.autoAdvance = false
        try {
            compose.runOnUiThread { block(); bar() }
            compose.mainClock.advanceTimeBy(1_500)
            compose.waitForIdle()
        } finally {
            compose.mainClock.autoAdvance = true
        }
        compose.waitForIdle()
        val tops = blockTops()
        val first = tops.filterValues { it >= list }.minByOrNull { it.value }?.key
        assertEquals("the first block at the top of the list after $toggle; tops $tops, list $list", "Block 2", first)
        assertTrue("Block 2 sits at the top; tops $tops, list $list", tops.getValue("Block 2") - list < 40 * compose.density.density)
    }

    @Test fun `a block opened in the frame of Edit scrolls to that block`() {
        val f = gymFixture(day, zone, heart = false); seed(f)
        compose.runOnUiThread { vm.openPicks[f.session.id] = OpenPick(true, null) }
        openFromHistory(Routes.session(f.session.id))
        until("the blocks show") { blockTops().size > 2 }
        openBlock2Then("Edit")
    }

    @Test fun `a block opened in the frame of Done scrolls to that block`() {
        val f = gymFixture(day, zone, heart = false); seed(f)
        compose.runOnUiThread { vm.openPicks[f.session.id] = OpenPick(true, null) }
        openFromHistory(Routes.session(f.session.id))
        until("the blocks show") { blockTops().size > 2 }
        compose.runOnUiThread { clickAction("Edit")() }
        until("Edit mode") { shows("Done") }
        openBlock2Then("Done")
    }

    private class LateLogs(val dao: KhonDao, val session: Session) {
        val logsAsked = CompletableDeferred<Unit>()
        val allow = CompletableDeferred<Unit>()
        val wrapped = object : KhonDao by dao {
            override fun setLogs(sessionId: String): Flow<List<SetLog>> = flow { logsAsked.complete(Unit); allow.await(); emitAll(dao.setLogs(sessionId)) }
            override fun intervalLogs(sessionId: String): Flow<List<IntervalLog>> = flow { logsAsked.complete(Unit); allow.await(); emitAll(dao.intervalLogs(sessionId)) }
        }
        private suspend fun <T> kotlinx.coroutines.flow.FlowCollector<T>.emitAll(f: Flow<T>) = f.collect { emit(it) }
    }

    private fun lateLogs(f: PictureFixture, finished: Boolean): LateLogs {
        val session = if (finished) f.session else f.session.copy(finishedAt = null)
        seed(f, session)
        return LateLogs(db.dao(), session).also { useDao(it.wrapped) }
    }

    private fun body() = compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty()

    private fun barWhileLoading(f: PictureFixture, finished: Boolean, route: String, back: String, done: String, leave: (LateLogs) -> Unit) {
        val late = lateLogs(f, finished)
        try {
            openFromHistory(route)
            until("the log query starts") { late.logsAsked.isCompleted }
            until("the bar shows while the logs load") { named(back) && shows(done) }
            assertTrue("no body before the logs", !body())
            leave(late)
            until("the screen left for History") { routes() == listOf("today", "history") }
        } finally {
            late.allow.complete(Unit)
        }
    }

    private fun barBack(back: String): (LateLogs) -> Unit = { compose.runOnUiThread { compose.onNodeWithContentDescription(back).fetchSemanticsNode().config[SemanticsActions.OnClick].action!!() } }
    private val systemBack: (LateLogs) -> Unit = { compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() } }
    private val deleted: (LateLogs) -> Unit = { late -> runBlocking(Dispatchers.IO) { late.dao.deleteSession(late.session.id) } }

    @Test fun `a finished gym session shows its bar while its sets load, and Back works`() =
        barWhileLoading(gymFixture(day, zone), true, Routes.session("gym"), "History", "Edit", barBack("History"))

    @Test fun `an unfinished gym session shows its bar while its sets load, and system Back works`() =
        barWhileLoading(gymFixture(day, zone), false, Routes.session("gym"), "Today", "Finish", systemBack)

    @Test fun `a finished cardio session shows its bar while its rounds load, and system Back works`() =
        barWhileLoading(cardioFixture(day, zone), true, Routes.cardio("cardio"), "History", "Save", systemBack)

    @Test fun `an unfinished cardio session shows its bar while its rounds load, and Back works`() =
        barWhileLoading(cardioFixture(day, zone), false, Routes.cardio("cardio"), "Today", "Save", barBack("Today"))

    @Test fun `a session deleted while its sets load leaves for History`() =
        barWhileLoading(gymFixture(day, zone), true, Routes.session("gym"), "History", "Edit", deleted)

    @Test fun `a cardio session deleted while its rounds load leaves for History`() =
        barWhileLoading(cardioFixture(day, zone), false, Routes.cardio("cardio"), "Today", "Save", deleted)

    @Test fun `the body shows once the logs load`() {
        val late = lateLogs(gymFixture(day, zone), finished = true)
        openFromHistory(Routes.session("gym"))
        until("the bar shows") { named("History") }
        late.allow.complete(Unit)
        until("the body shows") { body() && compose.onAllNodes(hasContentDescription("Review", substring = true)).fetchSemanticsNodes().size == 1 }
    }
}
