package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
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
 * Screenshots of the History screens for a visual check, at the S25 width and a tall height so a whole page fits.
 * Runs only when `app/build/shots-p3` exists; the PNGs land there.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h1900dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HistoryShotsTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val dir = File("build/shots-p3")
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private val dao get() = app.db.dao()
    private val today = LocalDate.now()

    @Before fun open() {
        assumeTrue(dir.isDirectory)
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
    }

    @After fun close() { if (::app.isInitialized) { app.scope.cancel(); app.db.close() } }

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
    private fun set(s: String, ex: String, name: String, kg: Double, reps: Int, status: String = SetStatus.DONE) =
        SetLog("l${n++}", s, 0, false, 0, n, n, ex, name, 8, 12, 2.5, weightKg = kg, reps = reps, status = status)

    /** Four weeks like the prototype: push / run / pull / rest / legs / run / rest, two days missed, bodyweights, stretches, one import. */
    private suspend fun seed() {
        dao.upsertModalities(listOf(Modality("run", "Run", 0xFF7CE0A8), Modality("swim", "Swim", 0xFF71B7FF)))
        dao.upsertExercises(listOf(
            Exercise("bench", "Barbell Bench Press", Kind.STRENGTH, primaryMuscle = "chest", secondaryMuscles = "shoulders,triceps"),
            Exercise("ohp", "Shoulder Press", Kind.STRENGTH, primaryMuscle = "shoulders", secondaryMuscles = "triceps"),
            Exercise("row", "Cable Seated Row", Kind.STRENGTH, primaryMuscle = "upper back", secondaryMuscles = "lats,biceps"),
            Exercise("pulldown", "Lat Pulldown", Kind.STRENGTH, primaryMuscle = "lats", secondaryMuscles = "biceps"),
            Exercise("squat", "Back Squat", Kind.STRENGTH, primaryMuscle = "quads", secondaryMuscles = "glutes"),
            Exercise("rdl", "Romanian Deadlift", Kind.STRENGTH, primaryMuscle = "hamstrings", secondaryMuscles = "glutes"),
            Exercise("legpress", "Leg Press", Kind.STRENGTH, primaryMuscle = "quads"),
            Exercise("z2", "Zone 2 run", Kind.CARDIO, modalityId = "run"),
            Exercise("swim", "Easy swim", Kind.CARDIO, modalityId = "swim"),
        ))
        dao.upsertPrograms(listOf(Program("push", "Push", active = true), Program("pull", "Pull"), Program("legs", "Legs")))
        val week = listOf("program" to "push", "cardio" to "z2", "program" to "pull", "rest" to null, "program" to "legs", "cardio" to "swim", "rest" to null)
        dao.upsertWeekPlan(week.mapIndexed { i, (t, id) -> WeekPlan(i + 1, t, id) })
        val lifts = mapOf("push" to listOf("bench" to 60.0, "ohp" to 35.0), "pull" to listOf("row" to 50.0, "pulldown" to 45.0), "legs" to listOf("squat" to 80.0, "rdl" to 70.0))
        val names = mapOf("bench" to "Barbell Bench Press", "ohp" to "Shoulder Press", "row" to "Cable Seated Row", "pulldown" to "Lat Pulldown", "squat" to "Back Squat", "rdl" to "Romanian Deadlift")
        val start = weekOf(today).minusWeeks(3)
        var d = start
        var w = 0
        while (d < today) {
            val (t, id) = week[d.dayOfWeek.value - 1]
            val missed = d == start.plusDays(9) || d == start.plusDays(18)
            if (t != "rest" && !missed) {
                val sid = "s$d"
                val at = d.toEpochDay() * 86_400_000 + 7 * 3_600_000
                if (t == "program") {
                    dao.upsert(Session(sid, d.iso(), at, at + (55 + w) * 60_000L, ItemType.PROGRAM, programId = id, programName = id!!.replaceFirstChar(Char::uppercase)))
                    dao.upsertSetLogs(lifts.getValue(id).flatMap { (ex, kg) ->
                        val top = kg + 2.5 * w
                        (0 until 3).map { k -> set(sid, ex, names.getValue(ex), top, if (k == 2) 6 else 8) } + set(sid, ex, names.getValue(ex), top, 0, SetStatus.SKIPPED).let { if (w % 2 == 0) listOf(it) else emptyList() }
                    })
                } else if (id == "z2") {
                    dao.upsert(Session(sid, d.iso(), at, at + 1, ItemType.CARDIO, exerciseId = "z2", exerciseName = "Zone 2 run", timeSec = 1800 + w * 120, distanceM = 5000 + w * 300, avgHr = 138 + w, sourceId = if (w == 2) "hc$w" else null))
                } else {
                    dao.upsert(Session(sid, d.iso(), at, at + 1, ItemType.CARDIO, exerciseId = "swim", exerciseName = "Easy swim", timeSec = 1500, laps = 40 + w * 2, poolLength = 25))
                }
            }
            if (d.dayOfWeek.value == 7) w++
            d = d.plusDays(1)
        }
        dao.upsertBodyweights((0 until 10).map { Bodyweight(today.minusDays(3L * it + 1).iso(), 83.4 + it * 0.13) })
        dao.upsertStretchSessions((0 until 8).map { StretchSession("st$it", "daily", "Daily", today.minusDays(3L * it + 2).iso(), 1L, 600, 9, 0) })
    }

    private fun history() = compose.setContent { KhonTheme { androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) { HistoryScreen(vm, rememberNavController()) } } }

    @Test fun logAndProgress() {
        io { seed() }
        history()
        waitFor("planned done")
        shot("1-log-this-week")
        compose.onNodeWithText("Progress").performClick()
        waitFor("Personal records")
        shot("2-progress")
    }

    @Test fun logLastWeek() {
        io { seed() }
        history()
        waitFor("planned done")
        compose.onNodeWithContentDescription("Previous week").performClick()
        waitFor("missed")
        shot("3-log-last-week")
    }

    @Test fun exerciseDetail() {
        io { seed() }
        compose.setContent { KhonTheme { androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) { ExerciseDetailScreen(vm, rememberNavController(), "bench") } } }
        waitFor("Heaviest set per session")
        shot("4-exercise-detail")
    }

    @Test fun exerciseWithoutSets() {
        io { seed() }
        compose.setContent { KhonTheme { androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) { ExerciseDetailScreen(vm, rememberNavController(), "legpress") } } }
        waitFor("No sets yet")
        shot("5-exercise-no-sets")
    }

    @Test fun cardioType() {
        io { seed() }
        compose.setContent { KhonTheme { androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) { CardioTypeScreen(vm, rememberNavController(), "run") } } }
        waitFor("Sessions")
        shot("6-cardio-type")
    }

    @Test fun liftList() {
        io { seed() }
        compose.setContent { KhonTheme { androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) { LiftListScreen(vm, rememberNavController()) } } }
        waitFor("No sets yet")
        shot("11-lift-list")
    }

    @Test fun progressCardio() {
        io { seed() }
        history()
        waitFor("planned done")
        compose.onNodeWithText("Progress").performClick()
        waitFor("Distance per session")
        compose.onAllNodesWithText("Distance per session", substring = true)[0].performScrollTo()
        shot("12-progress-cardio")
    }

    @Test fun logAfterProgramDelete() {
        // Legs was missed last Friday: that row keeps its plan as "Skipped · Deleted program".
        io { seed(); vm.repo.deleteProgram("legs") }
        history()
        waitFor("planned done")
        compose.onNodeWithContentDescription("Previous week").performClick()
        waitFor("Deleted program")
        shot("13-log-deleted-program")
    }

    @Test @Config(fontScale = 1.6f) fun largeText() {
        io { seed() }
        history()
        waitFor("planned done")
        shot("9-log-font-1.6")
        compose.onNodeWithText("Progress").performClick()
        waitFor("Personal records")
        shot("10-progress-font-1.6")
    }

    @Test fun firstUse() {
        io {
            dao.upsertPrograms(listOf(Program("push", "Push", active = true)))
            dao.upsertWeekPlan((1..7).map { WeekPlan(it, if (it % 2 == 0) ItemType.REST else ItemType.PROGRAM, if (it % 2 == 0) null else "push") })
        }
        history()
        waitFor("planned done")
        shot("7-first-use-log")
        compose.onNodeWithText("Progress").performClick()
        waitFor("No records yet")
        shot("8-first-use-progress")
    }
}
