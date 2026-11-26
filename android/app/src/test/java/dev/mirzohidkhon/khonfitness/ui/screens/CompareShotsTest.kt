package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
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

/** Runs only when `app/build/shots-compare` exists; the PNGs land there. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h2400dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CompareShotsTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val dir = File("build/shots-compare")
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

    @After fun close() { if (::app.isInitialized) { if (::vm.isInitialized) vm.viewModelScope.cancel(); app.scope.cancel(); app.db.close() } }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }
    private fun waitFor(text: String) = compose.waitUntil(5_000) {
        compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() ||
            compose.onAllNodesWithContentDescription(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun shot(name: String, over: String? = null) {
        compose.waitForIdle()
        val sheet = over?.let { compose.onNodeWithText(it).fetchSemanticsNode().root as View }
        compose.runOnIdle {
            val v = compose.activity.window.decorView
            val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            v.draw(canvas)
            sheet?.rootView?.draw(canvas)
            File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private var n = 0
    private fun set(s: String, ex: String, name: String, kg: Double, reps: Int) = SetLog("l${n++}", s, 0, false, 0, n, n, ex, name, 8, 12, 2.5, weightKg = kg, reps = reps, status = SetStatus.DONE)

    private val longName = "Single-arm dumbbell Romanian deadlift from a deficit platform"

    private suspend fun seed() {
        dao.upsertModalities(listOf(Modality("run", "Run", 0xFF7CE0A8), Modality("swim", "Swim", 0xFF71B7FF), Modality("bike", "Bike", 0xFFFFD60A)))
        dao.upsertExercises(listOf(
            Exercise("bench", "Barbell Bench Press", Kind.STRENGTH, primaryMuscle = "chest", secondaryMuscles = "shoulders,triceps"),
            Exercise("squat", "Back Squat", Kind.STRENGTH, primaryMuscle = "quads", secondaryMuscles = "glutes"),
            Exercise("rdl", longName, Kind.STRENGTH, primaryMuscle = "hamstrings", secondaryMuscles = "glutes,lower back"),
            Exercise("z2", "Zone 2 run", Kind.CARDIO, modalityId = "run"),
            Exercise("swim", "Easy swim", Kind.CARDIO, modalityId = "swim"),
            Exercise("ride", "Long ride", Kind.CARDIO, modalityId = "bike"),
        ))
        dao.upsertPrograms(listOf(Program("push", "Push", active = true), Program("legs", "Legs and a very long program name for wrapping")))
        var d = today.minusDays(55)
        var w = 0
        while (d <= today) {
            val at = d.toEpochDay() * 86_400_000 + 7 * 3_600_000
            val id = "s$d"
            when (d.dayOfWeek.value) {
                1 -> { dao.upsert(Session(id, d.iso(), at, at + 3_300_000, ItemType.PROGRAM, programId = "push", programName = "Push"))
                    dao.upsertSetLogs((0 until 4).map { set(id, "bench", "Barbell Bench Press", 80.0 + 2.5 * w, 6) }) }
                3 -> dao.upsert(Session(id, d.iso(), at, at + 1, ItemType.CARDIO, exerciseId = "z2", exerciseName = "Zone 2 run", timeSec = 1800 + 60 * w, distanceM = 5200 + 200 * w))
                4 -> { dao.upsert(Session(id, d.iso(), at, at + 3_600_000, ItemType.PROGRAM, programId = "legs", programName = "Legs"))
                    dao.upsertSetLogs((0 until 3).map { set(id, "squat", "Back Squat", 100.0 + 5 * w, 5) } + (0 until 3).map { set(id, "rdl", longName, 102.5, 12) }) }
                6 -> dao.upsert(Session(id, d.iso(), at, at + 1, ItemType.CARDIO, exerciseId = "swim", exerciseName = "Easy swim", timeSec = 1500, laps = 40 + w, poolLength = 25))
                7 -> if (w >= 4) dao.upsert(Session(id, d.iso(), at, at + 1, ItemType.CARDIO, exerciseId = "ride", exerciseName = "Long ride", timeSec = 5400, distanceM = 42000))
            }
            if (d.dayOfWeek.value == 7) w++
            d = d.plusDays(1)
        }
        dao.upsertBodyweights((0 until 12).map { Bodyweight(today.minusDays(4L * it + 1).iso(), 83.4 + it * 0.13) })
        dao.upsertStretchSessions((0 until 10).map { StretchSession("st$it", "daily", "Daily", today.minusDays(5L * it + 2).iso(), 1L, 600, 9, 0) })
    }

    private fun show() = compose.setContent {
        KhonTheme {
            androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) {
                nav = rememberNavController()
                NavHost(nav, startDestination = Routes.HISTORY) {
                    composable(Routes.HISTORY) { HistoryScreen(vm, nav) }
                    composable(Routes.COMPARE) { CompareScreen(vm, nav) }
                }
            }
        }
    }

    private fun walk(prefix: String) {
        io { seed() }
        show()
        compose.onNodeWithText("Progress").performSemanticsAction(SemanticsActions.OnClick)
        waitFor("Compare two periods")
        compose.onNodeWithText("Compare two periods").performScrollTo()
        shot("$prefix-1-progress-entry")
        compose.onNodeWithText("Compare two periods").performSemanticsAction(SemanticsActions.OnClick)
        waitFor("Best set")
        shot("$prefix-2-compare")
        compose.onNodeWithText("Last 4 weeks").performSemanticsAction(SemanticsActions.OnClick)
        waitFor("Right column")
        compose.onNodeWithText("Custom dates").performSemanticsAction(SemanticsActions.OnClick)
        waitFor("From")
        shot("$prefix-3-sheet-custom", over = "Right column")
    }

    @Test fun w360() = walk("w360-f1.0")
    @Test @Config(fontScale = 1.3f) fun w360large() = walk("w360-f1.3")
    @Test @Config(qualifiers = "w411dp-h2400dp-xxhdpi") fun w411() = walk("w411-f1.0")
    @Test @Config(qualifiers = "w411dp-h2400dp-xxhdpi", fontScale = 1.3f) fun w411large() = walk("w411-f1.3")

    @Test fun empty() {
        show()
        compose.runOnIdle { nav.navigate(Routes.COMPARE) }
        waitFor("Stretching")
        shot("w360-f1.0-4-empty")
    }
}
