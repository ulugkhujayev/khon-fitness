package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.theme.K
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.mirzohidkhon.khonfitness.timer.Phase
import dev.mirzohidkhon.khonfitness.timer.PhaseKind
import dev.mirzohidkhon.khonfitness.timer.TimerService
import dev.mirzohidkhon.khonfitness.timer.TimerState
import dev.mirzohidkhon.khonfitness.timer.buildPhases
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.openTab
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/** Screenshots of the screens UI06 P5 changed, at the S25 size, for a visual check. Runs only when `app/build/shots-p5` exists. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PolishShotsTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val dir = File("build/shots-p5")
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private lateinit var nav: NavHostController
    private val dao get() = app.db.dao()
    private val today = LocalDate.now()

    @Before fun open() {
        assumeTrue(dir.isDirectory)
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
    }

    @After fun close() { TimerService.state.value = null; if (::app.isInitialized) { if (::vm.isInitialized) vm.viewModelScope.cancel(); app.scope.cancel(); app.db.close() } }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }
    private fun waitFor(text: String) = compose.waitUntil(5_000) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
    private fun shot(name: String) {
        compose.waitForIdle()
        compose.runOnIdle {
            val v = compose.activity.window.decorView
            val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
            v.draw(android.graphics.Canvas(bmp))
            File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun seed() = io {
        dao.upsert(Modality("run", "Run", 0xFF30D158))
        dao.upsert(Exercise("bench", "Bench press", Kind.STRENGTH, primaryMuscle = "chest"))
        dao.upsert(Exercise("fly", "Cable fly", Kind.STRENGTH, primaryMuscle = "chest"))
        dao.upsert(Exercise("hiit", "Bike intervals", Kind.CARDIO, modalityId = "run", intervals = true, workSec = 60, restSec = 30, rounds = 4))
        dao.upsert(Program("push", "Push day", active = true))
        dao.upsert(Block("b1", "push", 0))
        dao.upsert(BlockExercise("be1", "b1", 0, "bench", sets = 2))
        dao.upsert(WeekPlan(today.dayOfWeek.value, ItemType.PROGRAM, "push"))
        dao.upsert(Stretch("fold", "Forward fold", seconds = 40))
        dao.upsert(Stretch("hip", "Hip opener", seconds = 40, sided = true))
        dao.upsert(StretchRoutine("daily", "Daily mobility", active = true))
        dao.upsertRoutineStretches(listOf(RoutineStretch("r1", "daily", "fold", 0), RoutineStretch("r2", "daily", "hip", 1)))
    }

    private fun openApp(start: String = Routes.TODAY) {
        compose.setContent {
            KhonTheme {
                // The app's Scaffold gives its content white text; this Box does the same.
                androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides K.Text) {
                Box(Modifier.fillMaxSize().background(K.Bg)) {
                    nav = rememberNavController()
                    NavHost(nav, start) {
                        composable(Routes.TODAY) { TodayScreen(vm, nav) }
                        composable(Routes.PROGRAMS) { PlanScreen(vm, nav) }
                        composable("session/{id}") { e -> SessionScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                        composable(Routes.LIBRARY) { LibraryScreen(vm, nav) }
                        composable("timer/{id}") { e -> TimerScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                        composable("stretch/{id}") { e -> StretchScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    }
                }
                }
            }
        }
    }

    @Test fun shots() {
        seed()
        openApp()
        waitFor("Bodyweight")
        shot("01-today")
        compose.onNodeWithText("Eating window").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(androidx.compose.ui.test.hasContentDescription("On")).fetchSemanticsNodes().isNotEmpty() }
        shot("02-window-sheet")
        compose.activity.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.runOnIdle { nav.openTab(Routes.PROGRAMS) }
        waitFor("Daily mobility")
        shot("03-plan")
        compose.runOnIdle { nav.navigate(Routes.LIBRARY) }
        waitFor("Bench press")
        compose.onNodeWithContentDescription("Search").performTextInput("ben")
        shot("04-library-search")
    }

    @Test fun session() {
        seed()
        io {
            dao.upsert(Session(id = "s1", date = today.toString(), startedAt = System.currentTimeMillis(), itemType = ItemType.PROGRAM, programId = "push", programName = "Push day"))
            dao.upsertSetLogs(listOf(
                SetLog("a", "s1", 0, false, 0, 0, 0, "bench", "Bench press", 6, 10, 2.5, 60.0, 8, SetStatus.DONE),
                SetLog("b", "s1", 0, false, 0, 1, 1, "bench", "Bench press", 6, 10, 2.5, null, null, SetStatus.PENDING),
            ))
        }
        openApp()
        compose.runOnIdle { nav.navigate(Routes.session("s1")) }
        waitFor("Bench press")
        shot("05-session")
        compose.onNodeWithText("Discard workout").performScrollTo()
        shot("06-session-end")
    }

    @Test fun timer() {
        seed()
        io { dao.upsert(Session(id = "t1", date = today.toString(), startedAt = System.currentTimeMillis(), itemType = ItemType.CARDIO, exerciseId = "hiit")) }
        TimerService.state.value = TimerState("t1", "Bike intervals", buildPhases(0, 60, 30, 4), remaining = 60.0, awaitingStart = true)
        openApp()
        compose.runOnIdle { nav.navigate(Routes.timer("t1")) }
        waitFor("Battery settings")
        shot("07-timer")
    }

    @Test fun stretch() {
        seed()
        val phases = listOf(Phase(PhaseKind.WORK, 40, 0, label = "Forward fold", figure = "hipflexor"), Phase(PhaseKind.WORK, 40, 0, label = "Hip opener", side = "Left", figure = "hipflexor"))
        TimerService.state.value = TimerState("daily:x", "Daily mobility", phases, index = 1, remaining = 40.0, stretch = true, skipped = 1, skippedPhases = listOf(0))
        // The demonstration plays forever: drive the clock by hand.
        compose.mainClock.autoAdvance = false
        openApp()
        repeat(10) { compose.mainClock.advanceTimeBy(100); Thread.sleep(50) }
        compose.runOnUiThread { nav.navigate(Routes.stretch("daily")) }
        repeat(40) { compose.mainClock.advanceTimeBy(100); Thread.sleep(50) }
        compose.runOnUiThread {
            val v = compose.activity.window.decorView
            val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
            v.draw(android.graphics.Canvas(bmp))
            File(dir, "08-stretch.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
