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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import dev.mirzohidkhon.khonfitness.data.DemoView
import dev.mirzohidkhon.khonfitness.data.demoPhotoPath
import dev.mirzohidkhon.khonfitness.ui.components.FadingDemoPhoto
import dev.mirzohidkhon.khonfitness.ui.components.HeldDemoBitmaps
import dev.mirzohidkhon.khonfitness.ui.components.ShownDemoPhoto
import dev.mirzohidkhon.khonfitness.ui.components.StretchDemonstration
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.sync.Mutex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Collections
import java.util.IdentityHashMap
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
    private fun drawing() = compose.onAllNodes(SemanticsMatcher.keyIsDefined(ShownDemoPhoto)).fetchSemanticsNodes().singleOrNull()?.config
    private fun shown(): String? = drawing()?.getOrNull(ShownDemoPhoto)
    private fun fading(): String? = drawing()?.getOrNull(FadingDemoPhoto)
    private fun spoken(): String? = drawing()?.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()

    private fun cache(name: String): Any = Class.forName("dev.mirzohidkhon.khonfitness.ui.components.DemoPhotos")
        .getDeclaredField(name).apply { isAccessible = true }.get(null)!!

    private fun held(): Set<String> = (cache("held") as Map<*, *>).let { synchronized(it) { it.keys.map { k -> k as String }.toSet() } }

    /** The number of bitmap objects that the cache and the panel hold together; two copies of one photo count twice. */
    private fun bitmaps(): Int {
        val all = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        (cache("held") as Map<*, *>).let { synchronized(it) { all.addAll(it.values.filterNotNull()) } }
        drawing()?.getOrNull(HeldDemoBitmaps)?.let { all.addAll(it) }
        return all.size
    }

    /** Runs [block] while no photo can load, as on a phone where the decode is slow. */
    private fun loadsWait(block: () -> Unit) {
        val lock = cache("lock") as Mutex
        until("the photo loads to pause") { lock.tryLock() }
        try { block() } finally { lock.unlock() }
    }

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

    private fun pixels(b: Bitmap) = IntArray(b.width * b.height).also { b.getPixels(it, 0, b.width, 0, 0, b.width, b.height) }
    private fun channels(p: Int) = intArrayOf(Color.red(p), Color.green(p), Color.blue(p))

    /** How far [frame] is on the way from [from] (0) to [to] (1), and its mean distance per channel from that blend. */
    private fun blend(frame: Bitmap, from: IntArray, to: IntArray): Pair<Double, Double> {
        val now = pixels(frame)
        var along = 0.0
        var length = 0.0
        for (i in now.indices) {
            val f = channels(now[i]); val a = channels(from[i]); val b = channels(to[i])
            for (c in 0..2) { along += (f[c] - a[c]) * (b[c] - a[c]).toDouble(); length += (b[c] - a[c]) * (b[c] - a[c]).toDouble() }
        }
        val share = along / length
        var off = 0.0
        for (i in now.indices) {
            val f = channels(now[i]); val a = channels(from[i]); val b = channels(to[i])
            for (c in 0..2) off += abs(f[c] - (a[c] + share * (b[c] - a[c])))
        }
        return share to off / (3.0 * now.size)
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

    @Test fun `a photo that stays while the other side loads keeps its own side`() {
        var mirror by mutableStateOf(false)
        compose.mainClock.autoAdvance = false
        compose.setContent { view = LocalView.current; KhonTheme { StretchDemonstration("needle", mirror = mirror, preview = false) } }
        val side = demoPhotoPath("needle", "hold", DemoView.SIDE)
        val front = demoPhotoPath("needle", "hold", DemoView.FRONT)
        until(side) { shown() == side }
        compose.onNodeWithContentDescription("Change camera angle. View 1 of 2").performClick()
        until(front) { shown() == front && fading() == null }
        val left = photoArea("Thread the needle")
        val leftText = spoken()
        assertTrue("the text names the left side: $leftText", leftText!!.contains("left"))
        loadsWait {
            compose.runOnIdle { mirror = true }
            repeat(30) {
                frame()
                assertEquals("frame $it while the right side loads", front, shown())
                assertEquals("frame $it while the right side loads", leftText, spoken())
                val moved = difference(left, photoArea("Thread the needle"), flipA = false)
                assertTrue("frame $it while the right side loads draws the left photo as it was: difference $moved", moved < 2)
            }
        }
        until("$side mirrored") { shown() == "$side mirrored" }
        assertTrue("the text names the right side: ${spoken()}", spoken()!!.contains("right"))
    }

    @Test fun `the cache and the panel hold the shown poses and one photo that goes`() {
        val motion = mutableFloatStateOf(0f)
        compose.mainClock.autoAdvance = false
        compose.setContent { view = LocalView.current; KhonTheme { StretchDemonstration("catcow", preview = false, exerciseProgress = motion.floatValue) } }
        fun photo(pose: String, view: DemoView) = demoPhotoPath("catcow", pose, view)
        fun inMemory() = held() + listOfNotNull(shown()?.removeSuffix(" mirrored"), fading())
        until("both side photos") { shown() == photo("start", DemoView.SIDE) && held() == setOf(photo("start", DemoView.SIDE), photo("hold", DemoView.SIDE)) }
        repeat(30) { frame() }
        assertNull("nothing fades before a change", fading())

        compose.runOnIdle { motion.floatValue = 0.5f }
        until(photo("hold", DemoView.SIDE)) { shown() == photo("hold", DemoView.SIDE) }
        loadsWait {
            assertEquals("the start photo fades out", photo("start", DemoView.SIDE), fading())
            compose.onNodeWithContentDescription("Change camera angle. View 1 of 2").performClick()
            frame()
            assertEquals(photo("hold", DemoView.SIDE), shown())
            assertNull("a panel that waits for the other view keeps no older photo", fading())
            repeat(30) { frame(); assertTrue("frame $it: ${bitmaps()} bitmaps of ${inMemory()}", bitmaps() <= 3) }
        }
        var going = false
        var settled = 0
        var frames = 0
        while (settled < 30) {
            frame(); Thread.sleep(5)
            assertTrue("frame $frames after the view change: ${bitmaps()} bitmaps of ${inMemory()}", bitmaps() <= 3)
            if (shown() == photo("hold", DemoView.FRONT)) {
                if (fading() == photo("hold", DemoView.SIDE)) going = true
                if (fading() == null) settled++
            }
            assertTrue("the front photo never settled", ++frames < 300)
        }
        assertTrue("the side photo faded out under the front photo", going)
        assertEquals(setOf(photo("start", DemoView.FRONT), photo("hold", DemoView.FRONT)), inMemory())
        assertEquals("one bitmap for each photo", 2, bitmaps())
    }

    @Test fun `the start of the exercise from the preview drops the preview photos at once`() {
        var preview by mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent { view = LocalView.current; KhonTheme { StretchDemonstration("catcow", preview = preview) } }
        fun photo(pose: String) = demoPhotoPath("catcow", pose, DemoView.SIDE)
        fun inMemory() = held() + listOfNotNull(shown(), fading())
        until("the three preview photos") { held() == setOf(photo("setup"), photo("start"), photo("hold")) && fading() == null }
        loadsWait {
            compose.runOnIdle { preview = false }
            repeat(30) { frame(); assertTrue("frame $it while the hold photo waits: ${bitmaps()} bitmaps of ${inMemory()}", bitmaps() <= 2) }
        }
        var settled = 0
        var frames = 0
        while (settled < 30) {
            frame(); Thread.sleep(5)
            assertTrue("frame $frames after the start: ${bitmaps()} bitmaps of ${inMemory()}", bitmaps() <= 2)
            if (shown() == photo("hold") && fading() == null) settled++
            assertTrue("the hold photo never settled", ++frames < 300)
        }
        assertEquals(setOf(photo("hold")), inMemory())
        assertEquals("one bitmap of the hold photo", 1, bitmaps())
    }

    @Test fun `the start of a hold while the photo of another view loads keeps one copy of that photo`() {
        var preview by mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent { view = LocalView.current; KhonTheme { StretchDemonstration("catcow", preview = preview) } }
        fun photo(pose: String, view: DemoView) = demoPhotoPath("catcow", pose, view)
        fun inMemory() = held() + listOfNotNull(shown(), fading())
        until("the three preview photos") {
            held() == setOf(photo("setup", DemoView.SIDE), photo("start", DemoView.SIDE), photo("hold", DemoView.SIDE)) && fading() == null
        }
        compose.onNodeWithContentDescription("Step 3: Arch").performClick()
        until(photo("hold", DemoView.SIDE)) { shown() == photo("hold", DemoView.SIDE) && fading() == null }
        loadsWait {
            compose.onNodeWithContentDescription("Change camera angle. View 1 of 2").performClick()
            repeat(5) { frame() }
            compose.runOnIdle { preview = false }
            repeat(10) { frame(); assertTrue("frame $it while the front photo waits: ${bitmaps()} bitmaps of ${inMemory()}", bitmaps() <= 2) }
        }
        var settled = 0
        var frames = 0
        while (settled < 30) {
            frame(); Thread.sleep(5)
            assertTrue("frame $frames after the load: ${bitmaps()} bitmaps of ${inMemory()}", bitmaps() <= 2)
            if (shown() == photo("hold", DemoView.FRONT) && fading() == null) settled++
            assertTrue("the front photo never settled", ++frames < 300)
        }
        assertEquals(setOf(photo("hold", DemoView.FRONT)), inMemory())
        assertEquals("one bitmap of the front photo", 1, bitmaps())
    }

    @Test fun `a change of pose fades from one photo to the other with a photo in every frame`() {
        val motion = mutableFloatStateOf(1f)
        compose.mainClock.autoAdvance = false
        compose.setContent { view = LocalView.current; KhonTheme { StretchDemonstration("wgs", preview = false, exerciseProgress = motion.floatValue) } }
        val name = "World's greatest stretch"
        val start = demoPhotoPath("wgs", "start", DemoView.SIDE)
        val hold = demoPhotoPath("wgs", "hold", DemoView.SIDE)
        until(hold) { shown() == hold }
        repeat(30) { frame() }
        val holdAlone = photoArea(name)
        compose.runOnIdle { motion.floatValue = 0f }
        until(start) { shown() == start }
        repeat(30) { frame() }
        val before = photoArea(name)
        val apart = difference(before, holdAlone, flipA = false)
        assertTrue("the two photos differ: mean difference $apart", apart > 6)
        // An empty panel is near 0.
        val lit = brightness(before) / 4
        assertTrue("the start photo shows: brightness ${brightness(before)}", lit > 1)

        loadsWait {
            compose.runOnIdle { motion.floatValue = 0.5f }
            // Longer than the 300 ms fade.
            repeat(30) {
                frame()
                assertTrue("frame $it while the hold photo loads is empty", brightness(photoArea(name)) > lit)
            }
        }
        val from = pixels(before)
        val to = pixels(holdAlone)
        var frames = 0
        var after = 0
        var between = 0
        var last = 0.0
        while (after < 30) {
            frame(); Thread.sleep(5)
            val area = photoArea(name)
            val now = brightness(area)
            assertTrue("frame $frames of the change to the hold photo is empty: brightness $now", now > lit)
            val (share, off) = blend(area, from, to)
            assertTrue("frame $frames is a mix of the start photo and the hold photo only: $off from the mix at $share", off < 1.5)
            assertTrue("frame $frames is between the two photos: $share", share > -0.02 && share < 1.02)
            assertTrue("frame $frames goes on from the frame before it without a jump: $last to $share", share > last - 0.02 && share < last + 0.25)
            if (share > 0.2 && share < 0.8 && share > last + 0.01) between++
            last = share
            if (shown() == hold) after++
            assertTrue("the hold photo never showed", ++frames < 300)
        }
        assertTrue("the change passes through mixes of the two photos: $between frames between them, each further than the one before", between >= 4)
        val end = difference(photoArea(name), holdAlone, flipA = false)
        assertTrue("the panel ends on the hold photo: mean difference $end", end < 2)
    }
}
