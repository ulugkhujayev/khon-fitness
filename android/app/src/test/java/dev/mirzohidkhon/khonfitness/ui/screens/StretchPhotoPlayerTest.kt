package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.DemoView
import dev.mirzohidkhon.khonfitness.data.RoutineStretch
import dev.mirzohidkhon.khonfitness.data.Stretch
import dev.mirzohidkhon.khonfitness.data.StretchDemo
import dev.mirzohidkhon.khonfitness.data.StretchRoutine
import dev.mirzohidkhon.khonfitness.data.demoPhotoPath
import dev.mirzohidkhon.khonfitness.data.forDemoSide
import dev.mirzohidkhon.khonfitness.timer.Phase
import dev.mirzohidkhon.khonfitness.timer.PhaseKind
import dev.mirzohidkhon.khonfitness.timer.TimerService
import dev.mirzohidkhon.khonfitness.timer.TimerState
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.components.ShownDemoPhoto
import dev.mirzohidkhon.khonfitness.ui.components.StretchDemonstration
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** The photo the demonstration panel draws, read from its semantics, for each mode of the player. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class StretchPhotoPlayerTest {
    @get:Rule val compose = createComposeRule()
    private val guides = Json.decodeFromString<Map<String, StretchDemo>>(File("src/main/assets/stretch_demos/guides.json").readText())

    private fun frame() { compose.mainClock.advanceTimeByFrame(); compose.waitForIdle() }

    private fun panel() = compose.onAllNodes(SemanticsMatcher.keyIsDefined(ShownDemoPhoto)).fetchSemanticsNodes().singleOrNull()
    private fun shown(): String? = panel()?.config?.getOrNull(ShownDemoPhoto)
    private fun description(): String = panel()?.config?.getOrNull(SemanticsProperties.ContentDescription)?.joinToString() ?: ""

    private fun until(what: String, ok: () -> Boolean) {
        repeat(250) { if (ok()) return; frame(); Thread.sleep(10) }
        throw AssertionError("expected $what; the panel shows ${shown()}: ${description()}")
    }

    private fun showing(key: String, pose: String, view: DemoView = DemoView.SIDE, mirrored: Boolean = false) {
        val path = demoPhotoPath(key, pose, view) + if (mirrored) " mirrored" else ""
        until(path) { shown() == path }
    }

    private fun tap(label: String) {
        until(label) { compose.onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription(label).performSemanticsAction(SemanticsActions.OnClick)
        frame()
    }

    @Test fun `preview plays each step's photo, pause holds it, and a tap shows the step with its instruction`() {
        var figure by mutableStateOf("wgs")
        compose.mainClock.autoAdvance = false
        compose.setContent { KhonTheme { key(figure) { StretchDemonstration(figure) } } }
        for ((key, guide) in guides) {
            figure = key
            frame()
            // Each check lands in the middle of a 2.6 s step.
            showing(key, guide.steps[0].pose)
            compose.mainClock.advanceTimeBy(1300)
            guide.steps.forEachIndexed { i, step ->
                showing(key, step.pose)
                assertTrue("$key step $i: ${description()}", description().endsWith("${step.title}. ${step.instruction}"))
                compose.onNodeWithContentDescription("Step ${i + 1}: ${step.title}").assertIsSelected()
                compose.mainClock.advanceTimeBy(2600)
            }
            showing(key, guide.steps[0].pose)

            tap("Pause demo")
            compose.mainClock.advanceTimeBy(6000)
            showing(key, guide.steps[0].pose)
            for (i in guide.steps.indices.reversed()) {
                val step = guide.steps[i]
                tap("Step ${i + 1}: ${step.title}")
                compose.mainClock.advanceTimeBy(3000)
                showing(key, step.pose)
                assertTrue("$key tapped step $i", description().endsWith("${step.title}. ${step.instruction}"))
                compose.onNodeWithContentDescription("Step ${i + 1}: ${step.title}").assertIsSelected()
                assertTrue(compose.onAllNodesWithText(step.instruction).fetchSemanticsNodes().isNotEmpty())
            }
            tap("Play demo")
            compose.onNodeWithContentDescription("Step 1: ${guide.steps[0].title}").assertIsSelected()
        }
    }

    @Test fun `during the exercise a timed hold shows the hold photo and reps follow the motion`() {
        var figure by mutableStateOf("needle")
        var motion by mutableStateOf<Float?>(null)
        compose.setContent { KhonTheme { key(figure) { StretchDemonstration(figure, preview = false, exerciseProgress = motion) } } }
        for ((key, guide) in guides.filterValues { !it.moving }) {
            figure = key
            showing(key, "hold")
            assertTrue(key, description().endsWith(guide.steps.last().let { "${it.title}. ${it.instruction}" }))
        }
        for ((key, guide) in guides.filterValues { it.moving }) {
            figure = key
            for ((value, pose) in listOf(0f to "start", 0.49f to "start", 0.5f to "hold", 1f to "hold")) {
                motion = value
                showing(key, pose)
                val step = guide.steps.last { it.pose == pose }
                assertTrue("$key at $value: ${description()}", description().endsWith("${step.title}. ${step.instruction}"))
            }
            motion = null
        }
    }

    @Test fun `the view button turns the camera from side to front and back and keeps the pose`() {
        var preview by mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent { KhonTheme { key(preview) { StretchDemonstration("wgs", preview = preview, exerciseProgress = 0.7f) } } }
        tap("Pause demo")
        tap("Step 3: Rotate")
        for (mode in listOf(true, false)) {
            preview = mode
            frame()
            showing("wgs", "hold")
            tap("Change camera angle. View 1 of 2")
            showing("wgs", "hold", DemoView.FRONT)
            tap("Change camera angle. View 2 of 2")
            showing("wgs", "hold")
            compose.onNodeWithContentDescription("Change camera angle. View 1 of 2").assertExists()
        }
    }

    @Test fun `the panel keeps the previous photo until the next one is ready`() {
        var figure by mutableStateOf("wgs")
        compose.mainClock.autoAdvance = false
        compose.setContent { KhonTheme { key(figure) { StretchDemonstration(figure) } } }
        showing("wgs", "start")
        fun everyFrame(path: String) {
            repeat(250) {
                assertTrue("the panel was empty on the way to $path", shown() != null)
                if (shown() == path) return
                frame(); Thread.sleep(10)
            }
            throw AssertionError("never reached $path")
        }
        compose.mainClock.advanceTimeBy(5200)
        everyFrame(demoPhotoPath("wgs", "hold", DemoView.SIDE))
        tap("Change camera angle. View 1 of 2")
        everyFrame(demoPhotoPath("wgs", "hold", DemoView.FRONT))
        figure = "plow"
        everyFrame(demoPhotoPath("plow", "start", DemoView.SIDE))
    }

    private fun cache(name: String): Any = Class.forName("dev.mirzohidkhon.khonfitness.ui.components.DemoPhotos")
        .getDeclaredField(name).apply { isAccessible = true }.get(null)

    private fun held(): Set<String> = (cache("held") as Map<*, *>).let { synchronized(it) { it.keys.map { k -> k as String }.toSet() } }

    /** Runs [block] while no photo can load, as if every decode were still running. */
    private fun <T> loadsWait(block: () -> T): T {
        val lock = cache("lock") as Mutex
        until("the photo loads to pause") { lock.tryLock() }
        try { return block() } finally { lock.unlock() }
    }

    /** The shown photo, the panel's spoken text, and the instruction under it all belong to one step of [key]. */
    private fun onePair(key: String, view: DemoView = DemoView.SIDE): Int {
        val guide = guides.getValue(key)
        val step = guide.steps.indexOfLast { description().endsWith("${it.title}. ${it.instruction}") }
        assertTrue("no step has the text ${description()}", step >= 0)
        assertEquals("the photo of '${description()}'", demoPhotoPath(key, guide.steps[step].pose, view), shown())
        assertTrue("the instruction under the photo of step $step",
            compose.onAllNodesWithText(guide.steps[step].instruction).fetchSemanticsNodes().isNotEmpty())
        return step
    }

    @Test fun `the instruction and the spoken text change with the photo, not before it`() {
        val wgs = guides.getValue("wgs")
        var preview by mutableStateOf(false)
        val motion = mutableFloatStateOf(0.49f)
        compose.mainClock.autoAdvance = false
        compose.setContent { KhonTheme { key(preview) { StretchDemonstration("wgs", preview = preview, exerciseProgress = motion.floatValue) } } }
        showing("wgs", "start")
        assertEquals(1, onePair("wgs"))
        loadsWait {
            compose.runOnIdle { motion.floatValue = 0.5f }
            repeat(5) { frame(); assertEquals(1, onePair("wgs")) }
        }
        until("the hold photo") { frame(); onePair("wgs") == 2 }
        assertEquals(demoPhotoPath("wgs", "hold", DemoView.SIDE), shown())

        preview = true
        frame()
        tap("Pause demo")
        tap("Step 1: ${wgs.steps[0].title}")
        until("the start photo") { onePair("wgs") == 0 }
        loadsWait {
            tap("Step 3: ${wgs.steps[2].title}")
            repeat(5) { frame(); assertEquals(0, onePair("wgs")) }
        }
        until("the hold photo of step 3") { frame(); onePair("wgs") == 2 }
        tap("Step 2: ${wgs.steps[1].title}")
        until("step 2") { onePair("wgs") == 1 }
        loadsWait {
            tap("Change camera angle. View 1 of 2")
            repeat(5) { frame(); assertEquals(1, onePair("wgs")) }
        }
        until("the front photo of step 2") { frame(); shown() == demoPhotoPath("wgs", "start", DemoView.FRONT) }
        assertEquals(1, onePair("wgs", DemoView.FRONT))
    }

    @Test fun `the cache holds only the poses the panel can show next`() {
        var preview by mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent { KhonTheme { StretchDemonstration("catcow", preview = preview, exerciseProgress = 0f) } }
        fun paths(view: DemoView, vararg poses: String) = poses.map { demoPhotoPath("catcow", it, view) }.toSet()
        val previewSide = paths(DemoView.SIDE, "setup", "start", "hold")
        until("the preview poses") { held() == previewSide }

        fun within(allowed: Set<String>, what: String, done: () -> Boolean) {
            repeat(250) {
                val now = held()
                assertTrue("$what: held $now, allowed $allowed", allowed.containsAll(now) && now.size <= 2)
                if (done()) return
                frame(); Thread.sleep(10)
            }
            throw AssertionError("$what never settled: held ${held()}")
        }
        preview = false
        frame()
        val repSide = paths(DemoView.SIDE, "start", "hold")
        until("the setup photo to go") { frame(); held() == repSide }
        tap("Change camera angle. View 1 of 2")
        val repFront = paths(DemoView.FRONT, "start", "hold")
        within(repSide + repFront, "the change to the front view") { held() == repFront && shown() == demoPhotoPath("catcow", "start", DemoView.FRONT) }
        tap("Change camera angle. View 2 of 2")
        within(repSide + repFront, "the change back to the side view") { held() == repSide && shown() == demoPhotoPath("catcow", "start", DemoView.SIDE) }
    }

    @Test fun `a guide file that is missing reads as no demonstrations`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        assertEquals(emptyMap<String, StretchDemo>(), dev.mirzohidkhon.khonfitness.ui.components.readDemoGuides(context, "stretch_demos/missing.json"))
        assertEquals(guides, dev.mirzohidkhon.khonfitness.ui.components.readDemoGuides(context, "stretch_demos/guides.json"))
    }

    @Test fun `a photo file that is missing decodes to nothing`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        for (sample in listOf(1, 4)) org.junit.Assert.assertNull(dev.mirzohidkhon.khonfitness.ui.components.decodePhoto(context, "stretch_demos/missing.webp", sample))
    }

    @Test fun `a right side mirrors the photo and swaps the instruction`() {
        val app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        val vm = AppViewModel(app)
        try {
            runBlocking(Dispatchers.IO) {
                app.db.dao().upsertStretches(listOf(Stretch("w", "World's greatest stretch", sided = true, figure = "wgs")))
                app.db.dao().upsert(StretchRoutine("r", "Daily", active = true))
                app.db.dao().upsertRoutineStretches(listOf(RoutineStretch("r-w", "r", "w", 0)))
            }
            val phases = listOf("Left", "Right").map { Phase(PhaseKind.WORK, 40, 0, label = "World's greatest stretch", side = it, figure = "wgs", stretchId = "w") }
            TimerService.state.value = TimerState("r:run", "Daily", phases, remaining = 40.0, stretch = true)
            compose.mainClock.autoAdvance = false
            compose.setContent { KhonTheme { StretchScreen(vm, rememberNavController(), "r") } }
            val setup = guides.getValue("wgs").steps[0].instruction
            showing("wgs", "start")
            assertTrue(compose.onAllNodesWithText(setup).fetchSemanticsNodes().isNotEmpty())

            TimerService.state.value = TimerService.state.value!!.copy(index = 1)
            showing("wgs", "start", mirrored = true)
            val swapped = setup.forDemoSide(true)
            assertEquals("Step your right foot outside your right hand, knee over ankle. Keep your left leg long.", swapped)
            until(swapped) { compose.onAllNodesWithText(swapped).fetchSemanticsNodes().isNotEmpty() }
            assertTrue(description().endsWith(swapped))
        } finally {
            TimerService.state.value = null; TimerService.finished.value = null
            vm.viewModelScope.cancel(); app.scope.cancel(); app.db.close()
        }
    }
}
