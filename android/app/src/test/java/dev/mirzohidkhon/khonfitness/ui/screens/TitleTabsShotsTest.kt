package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.*
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
import java.time.LocalDate

/**
 * UI07 screenshots for a visual check at the S25 size (360 x 780 dp): History's title tabs and Plan's week strip, at
 * font scales 1.0 and 1.6. Runs only when `build/shots-ui07` exists; the PNGs land there.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h780dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TitleTabsShotsTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val dir = File("build/shots-ui07")
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

    /** The owner's week from the mockup: swim, gym, rest, bike intervals, swim, gym, rest; a few lifts for Progress. */
    private suspend fun seed() {
        dao.upsertModalities(Seed.modalities)
        dao.upsertExercises(listOf(
            Exercise("bench", "Incline DB Press", Kind.STRENGTH, primaryMuscle = "chest"),
            Exercise("swim", "Zone 2 swim", Kind.CARDIO, modalityId = "swim"),
            Exercise("nor", "Norwegian 4×4", Kind.CARDIO, modalityId = "bike", intervals = true, rounds = 4, workSec = 240, restSec = 180),
        ))
        dao.upsertPrograms(listOf(Program("gym", "GYM", active = true)))
        val week = listOf("cardio" to "swim", "program" to "gym", "rest" to null, "cardio" to "nor", "cardio" to "swim", "program" to "gym", "rest" to null)
        dao.upsertWeekPlan(week.mapIndexed { i, (t, id) -> WeekPlan(i + 1, t, id) })
        val d = today.minusDays(1)
        val at = d.toEpochDay() * 86_400_000 + 7 * 3_600_000
        dao.upsert(Session("s1", d.iso(), at, at + 3_600_000, ItemType.PROGRAM, programId = "gym", programName = "GYM"))
        dao.upsertSetLogs((0 until 3).map { SetLog("l$it", "s1", 0, false, 0, it, it, "bench", "Incline DB Press", 8, 12, 2.5, weightKg = 120.0, reps = 8, status = SetStatus.DONE) })
        dao.upsertBodyweights((0 until 10).map { Bodyweight(today.minusDays(3L * it + 1).iso(), 83.4 + it * 0.13) })
        // A one-day change on Sunday (Saturday when today is Sunday), so its mark shows and Thursday keeps the bike.
        dao.upsert(DayOverride(today.plusDays(if (today.dayOfWeek.value == 7) -1L else 7L - today.dayOfWeek.value).iso(), ItemType.PROGRAM, "gym"))
    }

    private fun show(scale: Float, screen: @Composable () -> Unit) = compose.setContent {
        val d = LocalDensity.current
        val config = Configuration(LocalConfiguration.current).apply { fontScale = scale }
        CompositionLocalProvider(LocalDensity provides Density(d.density, scale), LocalConfiguration provides config) {
            KhonTheme { androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) { screen() } }
        }
    }

    private fun history(scale: Float, tag: String) {
        io { seed() }
        show(scale) { HistoryScreen(vm, rememberNavController()) }
        waitFor("planned done")
        shot("history-log-$tag")
        compose.onNodeWithText("Progress").performClick()
        waitFor("Personal records")
        shot("history-progress-$tag")
    }

    @Test fun history() = history(1f, "1.0")
    @Test fun historyLarge() = history(1.6f, "1.6")

    /** The title row stays pinned at full size while the page scrolls under it. */
    @Test fun historyScrolled() {
        io { seed() }
        show(1f) { HistoryScreen(vm, rememberNavController()) }
        waitFor("planned done")
        compose.onNodeWithText("Progress").performClick()
        waitFor("Personal records")
        scrollPage(60f)
        shot("history-progress-scrolled-60")
    }

    /** Scrolls the page under the header by [dp]. */
    private fun scrollPage(dp: Float) {
        val px = dp * compose.density.density
        compose.onNode(androidx.compose.ui.test.hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, px) }
        compose.waitForIdle()
    }

    private fun plan(scale: Float, tag: String) {
        io { seed() }
        show(scale) { PlanScreen(vm, rememberNavController()) }
        waitFor("Zone 2 swim")
        shot("plan-$tag")
    }

    @Test fun plan() = plan(1f, "1.0")
    @Test fun planLarge() = plan(1.6f, "1.6")

    @Test fun planSelected() {
        io { seed() }
        show(1f) { PlanScreen(vm, rememberNavController()) }
        waitFor("Zone 2 swim")
        compose.onNode(androidx.compose.ui.test.hasContentDescription("Thursday,", substring = true)).performClick()
        waitFor("Thu · ")
        shot("plan-thursday")
    }

    /** A 320 dp screen: the week columns stay 48 dp and the strip scrolls sideways. */
    @Config(qualifiers = "w320dp-h780dp-xxhdpi")
    @Test fun planNarrow() = plan(1f, "320dp")
}
