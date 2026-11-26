package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.Bodyweight
import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.data.iso
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
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
import java.math.BigDecimal
import java.time.LocalDate

/** Runs only when `app/build/shots-targets` exists; the PNGs land there. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h1000dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TargetShotsTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val dir = File("build/shots-targets")
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private val today = LocalDate.now()
    private var k = 0

    @Before fun open() {
        assumeTrue(dir.isDirectory)
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
    }

    @After fun close() { if (::app.isInitialized) { if (::vm.isInitialized) vm.viewModelScope.cancel(); app.scope.cancel(); app.db.close() } }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }

    private fun seed() = io {
        val dao = app.db.dao()
        dao.upsertExercises(listOf("date", "reached", "flat", "unset").map { Exercise(it, "Barbell Bench Press", Kind.STRENGTH, primaryMuscle = "chest") })
        val sessions = mutableListOf<Session>()
        val logs = mutableListOf<SetLog>()
        fun lift(ex: String, back: Long, value: Double) {
            val date = today.minusDays(back)
            val at = date.toEpochDay() * 86_400_000 + 7 * 3_600_000
            sessions += Session("$ex-$back", date.iso(), at, at + 3_000_000, ItemType.PROGRAM, programId = "p", programName = "TP v4")
            logs += SetLog("l${k++}", "$ex-$back", 0, false, 0, 0, 0, ex, "Barbell Bench Press", 5, 8, 2.5, weightKg = value / 2, reps = 30, status = SetStatus.DONE)
        }
        listOf(28L to 50.0, 14L to 75.0, 0L to 100.0).forEach { (b, v) -> lift("date", b, v) }
        listOf(42L to 90.0, 28L to 92.0, 14L to 94.0, 2L to 100.0).forEach { (b, v) -> lift("reached", b, v) }
        listOf(42L to 96.0, 28L to 94.0, 14L to 92.0, 0L to 90.0).forEach { (b, v) -> lift("flat", b, v) }
        listOf(42L to 90.0, 28L to 92.0, 14L to 94.0, 0L to 96.0).forEach { (b, v) -> lift("unset", b, v) }
        dao.upsertSessions(sessions)
        dao.upsertSetLogs(logs)
        val from = today.minusDays(50)
        vm.targets.setLift("date", BigDecimal(400), reps = false, date = from)
        vm.targets.setLift("reached", BigDecimal(100), reps = false, date = from)
        vm.targets.setLift("flat", BigDecimal("102.5"), reps = false, date = from)
    }

    private fun seedBody() = io {
        app.db.dao().upsertBodyweights((0..2).map { Bodyweight(today.minusDays(it.toLong()).iso(), 82.0) } + (7..9).map { Bodyweight(today.minusDays(it.toLong()).iso(), 82.3) })
        vm.targets.setBody(BigDecimal("80.0"), BigDecimal("83.2"), today.minusDays(20))
    }

    @Suppress("UNCHECKED_CAST")
    private fun shot(name: String) {
        compose.waitForIdle()
        compose.runOnIdle {
            val global = Class.forName("android.view.WindowManagerGlobal").getMethod("getInstance").invoke(null)
            val views = (global.javaClass.getDeclaredField("mViews").apply { isAccessible = true }.get(global) as List<View>).filter { it.isShown }
            val base = compose.activity.window.decorView
            val bmp = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            for (v in views) {
                val at = IntArray(2).also { v.getLocationOnScreen(it) }
                canvas.save(); canvas.translate(at[0].toFloat(), at[1].toFloat()); v.draw(canvas); canvas.restore()
            }
            File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun said(text: String) = compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription(text, substring = true).fetchSemanticsNodes().isNotEmpty() }

    private fun all(tag: String) {
        seed()
        var screen by mutableStateOf("date")
        compose.setContent {
            KhonTheme {
                androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) {
                    key(screen) {
                        if (screen == "body") Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) { HistoryProgress(vm, rememberNavController(), today) }
                        else ExerciseDetailScreen(vm, rememberNavController(), screen)
                    }
                }
            }
        }
        for ((id, spoken) in listOf("date" to "At plus 12.5", "reached" to "Reached, ", "flat" to "not at this rate", "unset" to "Target, not set")) {
            compose.runOnIdle { screen = id }
            said(spoken)
            shot("$tag-lift-$id")
        }
        compose.onNodeWithContentDescription("Target, not set").performClick()
        said("Target estimated one rep max")
        compose.mainClock.advanceTimeBy(1_000)
        shot("$tag-sheet")
        compose.runOnIdle { screen = "body" }
        said("Target, not set")
        compose.onNodeWithText("Compare two periods").performScrollTo()
        shot("$tag-body-unset")
        seedBody()
        said("At minus 0.3")
        compose.onNodeWithText("Compare two periods").performScrollTo()
        shot("$tag-body-date")
    }

    @Test fun at360() = all("360-f1.0")
    @Test @Config(qualifiers = "w411dp-h1000dp-xxhdpi") fun at411() = all("411-f1.0")
    @Test @Config(fontScale = 1.3f) fun at360Large() = all("360-f1.3")
    @Test @Config(qualifiers = "w411dp-h1000dp-xxhdpi", fontScale = 1.3f) fun at411Large() = all("411-f1.3")
}
