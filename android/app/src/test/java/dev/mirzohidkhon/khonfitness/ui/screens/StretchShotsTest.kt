package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.RoutineStretch
import dev.mirzohidkhon.khonfitness.data.Stretch
import dev.mirzohidkhon.khonfitness.data.StretchRoutine
import dev.mirzohidkhon.khonfitness.timer.Phase
import dev.mirzohidkhon.khonfitness.timer.PhaseKind
import dev.mirzohidkhon.khonfitness.timer.TimerService
import dev.mirzohidkhon.khonfitness.timer.TimerState
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.components.Figures
import dev.mirzohidkhon.khonfitness.ui.components.ShownDemoPhoto
import dev.mirzohidkhon.khonfitness.ui.theme.K
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Pictures of the stretch player, the editor preview and the stretch list into build/shots-stretch (made by hand
 * first), with a layout check per player picture: the photo panel at least 240 dp high, the instruction above the
 * status and the button, and the button inside the window.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h780dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StretchShotsTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val dir = File("build/shots-stretch")
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel

    @Before fun open() {
        assumeTrue(dir.isDirectory)
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
        runBlocking(Dispatchers.IO) {
            val dao = app.db.dao()
            dao.upsertStretches(Figures.keys.map { Stretch(it, Figures.name(it), sided = it == "ninety", figure = it) } +
                Stretch("none", "Neck roll", figure = ""))
            dao.upsert(StretchRoutine("r", "Daily", active = true))
            dao.upsertRoutineStretches(listOf(RoutineStretch("r-n", "r", "ninety", 0), RoutineStretch("r-p", "r", "plow", 1)))
        }
    }

    @After fun close() {
        TimerService.state.value = null; TimerService.finished.value = null
        if (!::app.isInitialized) return
        runBlocking(Dispatchers.IO) { app.db.withTransaction {} }
        vm.viewModelScope.cancel()
        app.scope.cancel()
        compose.waitForIdle()
        app.db.close()
    }

    private fun settle() { repeat(8) { compose.mainClock.advanceTimeByFrame(); compose.waitForIdle(); Thread.sleep(10) } }

    private fun shown() = compose.onAllNodes(SemanticsMatcher.keyIsDefined(ShownDemoPhoto)).fetchSemanticsNodes().singleOrNull()

    private fun showing(photo: String) {
        repeat(300) { if (shown()?.config?.getOrNull(ShownDemoPhoto) == photo) { compose.mainClock.advanceTimeBy(400); settle(); return }; settle() }
        throw AssertionError("the panel never showed $photo")
    }

    private fun until(what: String, ok: () -> Boolean) {
        repeat(300) { if (ok()) return; settle() }
        throw AssertionError("never reached: $what")
    }

    private fun tap(label: String) {
        until(label) { compose.onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription(label).performSemanticsAction(SemanticsActions.OnClick)
        settle()
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

    private fun content(scale: Float, screen: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false // the guide plays without end
        compose.setContent {
            val d = LocalDensity.current
            val config = Configuration(LocalConfiguration.current).apply { fontScale = scale }
            CompositionLocalProvider(LocalDensity provides Density(d.density, scale), LocalConfiguration provides config) {
                KhonTheme { Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) { screen() } }
            }
        }
    }

    private fun bounds(text: String, substring: Boolean = true): Rect = compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().first().boundsInRoot

    /** The scroll animates, so the clock runs for it; the guide is paused or absent here, so the screen gets idle. */
    private fun scrollTo(node: SemanticsNodeInteraction) {
        compose.mainClock.autoAdvance = true
        node.performScrollTo()
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        settle()
    }

    private fun scrollRange(): Float = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
        .fetchSemanticsNodes().maxOf { it.config[SemanticsProperties.VerticalScrollAxisRange].maxValue() }

    /**
     * Writes the measured layout next to the picture. The panel is never under 240 dp and the button stays in the window;
     * when the column fits, the instruction sits above the button, and when it must scroll, the panel is at its minimum
     * and a second picture shows the column scrolled to its end.
     */
    private fun shotAndCheck(name: String, instruction: String, button: String) {
        scrollTo(compose.onAllNodesWithText(label).onFirst())
        shot(name)
        val density = compose.activity.resources.displayMetrics.density
        val node = shown()!!
        val panel = node.boundsInRoot
        val panelHeight = node.size.height / density
        val text = bounds(instruction)
        val action = bounds(button, substring = false)
        val window = compose.activity.window.decorView.height
        val scroll = scrollRange() / density
        val line = "$name panel=${panel.width / density}x${panelHeight}dp panelTop=${panel.top / density} " +
            "instruction=${text.top / density}..${text.bottom / density} button=${action.top / density}..${action.bottom / density} " +
            "window=${window / density} scroll=${scroll}dp"
        File(dir, "layout.txt").appendText(line + "\n")
        assertTrue(line, panelHeight >= 239.5f && action.bottom <= window)
        if (scroll == 0f) assertTrue(line, panel.bottom <= text.top && text.bottom <= action.top)
        else {
            assertEquals(line, 240f, panelHeight, 0.5f)
            scrollTo(compose.onAllNodesWithText(instruction, substring = true).onFirst())
            shot("$name-scrolled")
        }
    }

    private var label = "90/90 stretch"

    private fun player(tag: String, scale: Float) {
        val phases = listOf(
            Phase(PhaseKind.WORK, 45, 0, label = "90/90 stretch", side = "Left", figure = "ninety", stretchId = "ninety"),
            Phase(PhaseKind.WORK, 45, 0, label = "90/90 stretch", side = "Right", figure = "ninety", stretchId = "ninety"),
            Phase(PhaseKind.WORK, 60, 0, label = "Plow pose", figure = "plow", stretchId = "plow"),
        )
        TimerService.state.value = TimerState("r:run", "Daily", phases, remaining = 45.0, stretch = true)
        content(scale) { StretchScreen(vm, rememberNavController(), "r") }
        val start = "Start left side"
        tap("Pause demo")
        tap("Step 1: Set up")
        showing("stretch_demos/ninety-start-side.webp")
        shotAndCheck("preview-first-$tag", "Sit tall with both sit bones", start)
        tap("Step 3: Back leg")
        showing("stretch_demos/ninety-hold-side.webp")
        shotAndCheck("preview-longest-$tag", "Take your right thigh", start)
        tap("Step 4: Lean")
        showing("stretch_demos/ninety-hold-side.webp")
        shotAndCheck("preview-last-$tag", "Lengthen your spine", start)
        tap("Change camera angle. View 1 of 2")
        showing("stretch_demos/ninety-hold-front.webp")
        shotAndCheck("front-$tag", "Lengthen your spine", start)

        TimerService.state.value = TimerService.state.value!!.copy(index = 1)
        settle()
        tap("Pause demo")
        tap("Step 3: Back leg")
        showing("stretch_demos/ninety-hold-side.webp mirrored")
        shotAndCheck("right-$tag", "Take your left thigh", "Start right side")

        TimerService.state.value = TimerService.state.value!!.copy(index = 2, remaining = 37.0, awaitingStart = false)
        label = "Plow pose"
        showing("stretch_demos/plow-hold-side.webp")
        shotAndCheck("exercise-$tag", "Rest your toes", "Pause")
    }

    @Test fun player780() = player("360x780-1_0", 1f)
    @Test fun player780x13() = player("360x780-1_3", 1.3f)
    @Config(qualifiers = "w360dp-h640dp-xxhdpi") @Test fun player640() = player("360x640-1_0", 1f)
    @Config(qualifiers = "w360dp-h640dp-xxhdpi") @Test fun player640x13() = player("360x640-1_3", 1.3f)

    @Test fun editor() {
        content(1f) { StretchEditorScreen(vm, rememberNavController(), "ninety") }
        tap("Pause demo")
        showing("stretch_demos/ninety-start-side.webp")
        shot("editor-360x780-1_0")
    }

    @Test fun editorWithoutDemonstration() {
        content(1f) { StretchEditorScreen(vm, rememberNavController(), "none") }
        until("no demonstration") { compose.onAllNodesWithText("No demonstration for this stretch").fetchSemanticsNodes().isNotEmpty() }
        settle()
        shot("editor-none-360x780-1_0")
    }

    @Test fun list() {
        content(1f) { StretchLibraryScreen(vm, rememberNavController()) }
        until("the list") { compose.onAllNodesWithText("Neck roll").fetchSemanticsNodes().isNotEmpty() }
        until("every thumbnail") { compose.onAllNodes(SemanticsMatcher.keyIsDefined(ShownDemoPhoto)).fetchSemanticsNodes().size == Figures.keys.size }
        shot("list-360x780-1_0")
    }
}
