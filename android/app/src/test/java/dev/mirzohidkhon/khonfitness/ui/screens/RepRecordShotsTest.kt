package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import dev.mirzohidkhon.khonfitness.ui.theme.K
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/**
 * Screenshots of rep records and the bodyweight trend, at the S25 width and a tall height: the Log, the exercise
 * detail, Progress' bodyweight card, Today's bodyweight card and the finish sheet. Runs only when
 * `app/build/shots-reps` exists; the PNGs land there.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h1900dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RepRecordShotsTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val dir = File("build/shots-reps")
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private lateinit var nav: NavHostController
    private val dao get() = app.db.dao()
    private val today = LocalDate.now()
    private val lastMonday = weekOf(today).minusWeeks(1)

    @Before fun open() {
        assumeTrue(dir.isDirectory)
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
    }

    @After fun close() { if (::app.isInitialized) { if (::vm.isInitialized) vm.viewModelScope.cancel(); app.scope.cancel(); app.db.close() } }

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

    private var n = 0
    private fun set(s: String, kg: Double, reps: Int, ex: String = "bench", status: String = SetStatus.DONE) =
        SetLog("l${n++}", s, 0, false, 0, n, n, ex, if (ex == "bench") "Barbell Bench Press" else "Back Squat", 8, 12, 2.5, weightKg = kg, reps = reps, status = status)

    private fun gym(id: String, date: LocalDate, finished: Boolean = true): Session {
        val at = date.toEpochDay() * 86_400_000 + 7 * 3_600_000
        return Session(id, date.iso(), at, if (finished) at + 3_480_000 else null, ItemType.PROGRAM, programId = "push", programName = "Push")
    }

    /** Bench over two weeks: a weight record, a rep record under it, then a weight record with a rep record set; daily bodyweights. */
    private suspend fun seed() {
        dao.upsertExercises(listOf(
            Exercise("bench", "Barbell Bench Press", Kind.STRENGTH, primaryMuscle = "chest"),
            Exercise("squat", "Back Squat", Kind.STRENGTH, primaryMuscle = "quads"),
        ))
        dao.upsertPrograms(listOf(Program("push", "Push", active = true)))
        dao.upsert(Block("b1", "push", 0))
        dao.upsert(BlockExercise("be1", "b1", 0, "bench", sets = 2))
        dao.upsertWeekPlan((1..7).map { WeekPlan(it, if (it == 7) ItemType.REST else ItemType.PROGRAM, if (it == 7) null else "push") })
        val first = lastMonday.minusWeeks(1)
        dao.upsertSessions(listOf(gym("a", first), gym("b", first.plusDays(2)), gym("mon", lastMonday), gym("wed", lastMonday.plusDays(2)), gym("fri", lastMonday.plusDays(4))))
        dao.upsertSetLogs(listOf(
            set("a", 80.0, 6), set("a", 80.0, 6), set("a", 70.0, 8), set("a", 100.0, 5, ex = "squat"),
            set("b", 85.0, 3), set("b", 80.0, 5), set("b", 102.5, 5, ex = "squat"),
            set("mon", 80.0, 8), set("mon", 80.0, 7), set("mon", 70.0, 9), set("mon", 100.0, 6, ex = "squat"),
            set("wed", 80.0, 6), set("wed", 60.0, 15), set("wed", 105.0, 5, ex = "squat"),
            set("fri", 87.5, 2), set("fri", 80.0, 9), set("fri", 70.0, 6, status = SetStatus.SKIPPED),
        ))
        dao.upsertBodyweights((0 until 40).filter { it % 5 != 3 }.map { Bodyweight(today.minusDays(it.toLong()).iso(), 82.4 + it * 0.035 + (if (it % 2 == 0) 0.3 else -0.2)) })
    }

    private fun openApp(start: String) {
        compose.setContent {
            KhonTheme {
                androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides K.Text) {
                    Box(Modifier.fillMaxSize().background(K.Bg)) {
                        nav = rememberNavController()
                        NavHost(nav, start) {
                            composable(Routes.TODAY) { TodayScreen(vm, nav) }
                            composable(Routes.HISTORY) { HistoryScreen(vm, nav) }
                            composable("lift/{id}") { e -> ExerciseDetailScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                            composable("session/{id}") { e -> SessionScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                        }
                    }
                }
            }
        }
    }

    @Test fun logAndDetail() {
        io { seed() }
        openApp(Routes.HISTORY)
        waitFor("planned done")
        compose.onNodeWithContentDescription("Previous week").performClick()
        waitFor("rep\u00A0PR")
        shot("1-log-rep-pr")
        compose.runOnIdle { nav.navigate(Routes.exerciseDetail("bench")) }
        waitFor("Most reps by weight")
        shot("2-exercise-detail")
    }

    @Test fun progressBodyweight() {
        io { seed() }
        openApp(Routes.HISTORY)
        waitFor("planned done")
        compose.onNodeWithText("Progress").performClick()
        waitFor("7-day avg")
        compose.onNodeWithText("7-day avg", substring = true).performScrollTo()
        shot("3-progress-bodyweight")
    }

    @Test fun todayBodyweight() {
        io { seed() }
        openApp(Routes.TODAY)
        waitFor("7-day avg")
        shot("4-today-bodyweight")
    }

    @Test fun finishSheet() {
        io {
            seed()
            dao.upsert(gym("now", today, finished = false))
            dao.upsertSetLogs(listOf(set("now", 80.0, 10), set("now", 107.5, 5, ex = "squat")))
        }
        openApp(Routes.TODAY)
        compose.runOnIdle { nav.navigate(Routes.session("now")) }
        waitFor("Finish")
        compose.onNodeWithText("Finish").performClick()
        waitFor("Rep records")
        compose.waitForIdle()
        // The sheet has its own window: draw every window over the activity's.
        compose.runOnIdle {
            val wmg = Class.forName("android.view.WindowManagerGlobal").getMethod("getInstance").invoke(null)
            @Suppress("UNCHECKED_CAST")
            val views = wmg.javaClass.getDeclaredField("mViews").apply { isAccessible = true }.get(wmg) as List<android.view.View>
            val base = compose.activity.window.decorView
            val bmp = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            views.forEach { v ->
                val at = IntArray(2).also { v.getLocationOnScreen(it) }
                canvas.save(); canvas.translate(at[0].toFloat(), at[1].toFloat()); v.draw(canvas); canvas.restore()
            }
            File(dir, "5-finish-sheet.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
