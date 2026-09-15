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
) {
    /** First show the setup, then move into each position and hold it long enough to inspect. */
    fun previewFrame(elapsedMs: Long): Int {
        val slot = ((elapsedMs.coerceAtLeast(0) / 2600) % steps.size).toInt()
        if (slot == 0) return steps.first().frame
        val progress = ((elapsedMs % 2600) / 1600f).coerceIn(0f, 1f)
        return (steps[slot - 1].frame + (steps[slot].frame - steps[slot - 1].frame) * progress).roundToInt()
    }

    fun stepAt(frame: Int): Int = steps.indexOfFirst { frame <= it.frame }.let { if (it < 0) steps.lastIndex else it }

    /** The timer supplies rep progress. A timed hold stays in the final position. */
    fun exerciseFrame(progress: Float?): Int = if (progress == null) frameCount - 1 else
        (loopStart + progress.coerceIn(0f, 1f) * (frameCount - 1 - loopStart)).roundToInt()
}

/** Mirror the instruction as well as the image, without replacing substrings inside other words. */
fun String.forDemoSide(rightSide: Boolean): String = if (!rightSide) this else
    Regex("\\b(left|right)\\b", RegexOption.IGNORE_CASE).replace(this) { match ->
        val word = if (match.value.equals("left", ignoreCase = true)) "right" else "left"
        if (match.value.first().isUpperCase()) word.replaceFirstChar { it.uppercase() } else word
    }
