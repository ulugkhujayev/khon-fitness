package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dev.mirzohidkhon.khonfitness.data.DemoView
import dev.mirzohidkhon.khonfitness.data.demoPhotoPath
import dev.mirzohidkhon.khonfitness.ui.components.ShownDemoPhoto
import dev.mirzohidkhon.khonfitness.ui.components.StretchDemonstration
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.sync.Mutex
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/** What the demonstration panel draws, read from its pixels (native graphics). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StretchPhotoPixelsTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private lateinit var view: View

    /** Found by its spoken text, which it has also while it draws no photo. */
    private fun panel(name: String) = compose.onNode(SemanticsMatcher("the panel of $name") {
        it.config.getOrNull(SemanticsProperties.ContentDescription)?.singleOrNull()?.startsWith("$name. ") == true
    })
    private fun shown(): String? = compose.onAllNodes(SemanticsMatcher.keyIsDefined(ShownDemoPhoto)).fetchSemanticsNodes()
        .singleOrNull()?.config?.getOrNull(ShownDemoPhoto)

    private fun frame() { compose.mainClock.advanceTimeByFrame(); compose.waitForIdle() }

    private fun until(what: String, ok: () -> Boolean) {
        repeat(250) { if (ok()) return; frame(); Thread.sleep(10) }
        throw AssertionError("expected $what; the panel shows ${shown()}")
    }

    /** The panel above its buttons, which do not mirror. */
    private fun photoArea(name: String): Bitmap = compose.nodeImage(view, panel(name)).let { Bitmap.createBitmap(it, 0, 0, it.width, it.height * 3 / 4) }

    private fun luma(p: Int) = (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000.0
    private fun brightness(b: Bitmap): Double {
        var sum = 0.0
        for (y in 0 until b.height) for (x in 0 until b.width) sum += luma(b.getPixel(x, y))
        return sum / (b.width * b.height)
    }

    /** Mean difference per channel between [a] and [b], with [a] read from right to left when [flipA]. */
    private fun difference(a: Bitmap, b: Bitmap, flipA: Boolean): Double {
        var sum = 0L
        for (y in 0 until a.height) for (x in 0 until a.width) {
            val p = a.getPixel(if (flipA) a.width - 1 - x else x, y)
            val q = b.getPixel(x, y)
            sum += abs(Color.red(p) - Color.red(q)) + abs(Color.green(p) - Color.green(q)) + abs(Color.blue(p) - Color.blue(q))
        }
        return sum / (3.0 * a.width * a.height)
    }

    @Test fun `the right side draws the left side's photo flipped`() {
        var mirror by mutableStateOf(false)
        compose.setContent { view = LocalView.current; KhonTheme { StretchDemonstration("needle", mirror = mirror, preview = false) } }
        val path = demoPhotoPath("needle", "hold", DemoView.SIDE)
        until(path) { shown() == path }
        compose.mainClock.advanceTimeBy(400)
        val left = photoArea("Thread the needle")
        mirror = true
        until("$path mirrored") { shown() == "$path mirrored" }
        compose.mainClock.advanceTimeBy(400)
        val right = photoArea("Thread the needle")
        assertTrue("the panel draws a photo: brightness ${brightness(left)}", brightness(left) > 3)
        val flipped = difference(left, right, flipA = true)
        val same = difference(left, right, flipA = false)
        assertTrue("the right side is the left side flipped: mean difference $flipped", flipped < 2)
        assertTrue("the photo is not symmetric, so the check proves the flip: unflipped difference $same", same > 6)
    }

    @Test fun `a change of pose draws a photo in every frame of the cross-fade`() {
        val motion = mutableFloatStateOf(0f)
        compose.mainClock.autoAdvance = false
        compose.setContent { view = LocalView.current; KhonTheme { StretchDemonstration("wgs", preview = false, exerciseProgress = motion.floatValue) } }
        val start = demoPhotoPath("wgs", "start", DemoView.SIDE)
        val hold = demoPhotoPath("wgs", "hold", DemoView.SIDE)
        until(start) { shown() == start }
        repeat(30) { frame() }
        val before = photoArea("World's greatest stretch")
        // A cross-fade over black dips to about 60 % of a photo's brightness; an empty panel is near 0.
        val lit = brightness(before) / 4
        assertTrue("the start photo shows: brightness ${brightness(before)}", lit > 1)

        val lock = Class.forName("dev.mirzohidkhon.khonfitness.ui.components.DemoPhotos")
            .getDeclaredField("lock").apply { isAccessible = true }.get(null) as Mutex
        until("the photo loads to pause") { lock.tryLock() }
        try {
            compose.runOnIdle { motion.floatValue = 0.5f }
            // Longer than the 300 ms fade, as on a phone where the decode is slow.
            repeat(30) {
                frame()
                assertTrue("frame $it while the hold photo loads is empty", brightness(photoArea("World's greatest stretch")) > lit)
            }
        } finally {
            lock.unlock()
        }
        var frames = 0
        var after = 0
        while (after < 30) {
            frame(); Thread.sleep(5)
            val now = brightness(photoArea("World's greatest stretch"))
            assertTrue("frame $frames of the change to the hold photo is empty: brightness $now", now > lit)
            if (shown() == hold) after++
            assertTrue("the hold photo never showed", ++frames < 300)
        }
        val end = photoArea("World's greatest stretch")
        assertTrue("the panel ends on another photo than it started", difference(before, end, flipA = false) > 6)
    }
}
