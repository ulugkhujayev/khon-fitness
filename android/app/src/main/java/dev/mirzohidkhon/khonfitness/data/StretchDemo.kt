package dev.mirzohidkhon.khonfitness.data

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
data class DemoStep(val title: String, val instruction: String, val frame: Int)

@Serializable
data class StretchDemo(
    val name: String,
    val frameCount: Int,
    val columns: Int,
    val frameWidth: Int,
    val frameHeight: Int,
    val steps: List<DemoStep>,
    val loopStart: Int,
    val moving: Boolean,
    val repHoldFraction: Float = 0f,
) {
    /** First show the setup, then move into each position and hold it long enough to inspect. */
    fun previewFrame(elapsedMs: Long): Int {
        val slot = ((elapsedMs.coerceAtLeast(0) / 2600) % steps.size).toInt()
        if (slot == 0) return steps.first().frame
        val progress = ((elapsedMs % 2600) / 1600f).coerceIn(0f, 1f)
        return (steps[slot - 1].frame + (steps[slot].frame - steps[slot - 1].frame) * progress).roundToInt()
    }

    fun stepAt(frame: Int): Int = steps.indexOfFirst { frame <= it.frame }.let { if (it < 0) steps.lastIndex else it }

    /** Reserve part of each half-cycle for its endpoints. Timed holds stay in the final position. */
    fun exerciseFrame(progress: Float?): Int {
        if (progress == null) return frameCount - 1
        val movement = ((progress.coerceIn(0f, 1f) - repHoldFraction) / (1f - 2f * repHoldFraction))
            .coerceIn(0f, 1f)
        return (loopStart + movement * (frameCount - 1 - loopStart)).roundToInt()
    }
}

/** Maps vsync times to demo time from one origin, so rounding per frame never accumulates into drift. */
class DemoClock(private val startMs: Long) {
    private var originNanos = Long.MIN_VALUE
    fun elapsedMs(frameTimeNanos: Long): Long {
        if (originNanos == Long.MIN_VALUE) originNanos = frameTimeNanos
        return startMs + (frameTimeNanos - originNanos) / 1_000_000
    }
}

/** Mirror the instruction as well as the image, without replacing substrings inside other words. */
fun String.forDemoSide(rightSide: Boolean): String = if (!rightSide) this else
    Regex("\\b(left|right)\\b", RegexOption.IGNORE_CASE).replace(this) { match ->
        val word = if (match.value.equals("left", ignoreCase = true)) "right" else "left"
        if (match.value.first().isUpperCase()) word.replaceFirstChar { it.uppercase() } else word
    }
