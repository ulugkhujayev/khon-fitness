package dev.mirzohidkhon.khonfitness.data

import kotlinx.serialization.Serializable

/** [pose] names the photo of the step: `stretch_demos/<key>-<pose>-<view>.webp`. */
@Serializable
data class DemoStep(val title: String, val instruction: String, val pose: String)

/** [reps] are the two poses a moving stretch alternates between during the exercise. */
@Serializable
data class StretchDemo(
    val name: String,
    val steps: List<DemoStep>,
    val moving: Boolean,
    val reps: List<String> = emptyList(),
) {
    fun previewStep(elapsedMs: Long): Int = ((elapsedMs.coerceAtLeast(0) / STEP_MS) % steps.size).toInt()

    /**
     * How far the step timeline is filled, 0..1 from the first step's node to the last: every step up to the shown
     * one, and toward the next by the time spent in the shown step.
     */
    fun previewFill(elapsedMs: Long): Float {
        if (steps.size < 2) return 0f
        val into = (elapsedMs.coerceAtLeast(0) % STEP_MS).toFloat() / STEP_MS
        return ((previewStep(elapsedMs) + into) / (steps.size - 1)).coerceIn(0f, 1f)
    }

    fun stepFill(step: Int): Float = if (steps.size < 2) 0f else step.toFloat() / (steps.size - 1)

    /** A timed hold ([motion] null) shows the last step; reps show the first rep pose up to half way, then the second. */
    fun exercisePose(motion: Float?): String =
        if (motion == null || reps.size < 2) steps.last().pose else reps[if (motion >= 0.5f) 1 else 0]

    /**
     * The poses a panel may show: in the preview those its steps name; during the exercise the two rep poses when it
     * runs in [reps], else the hold pose.
     */
    fun shownPoses(preview: Boolean, reps: Boolean): List<String> = when {
        preview -> steps.map { it.pose }.distinct()
        reps -> listOf(exercisePose(0f), exercisePose(1f)).distinct()
        else -> listOf(exercisePose(null))
    }

    /** The instruction during the exercise: the last step that shows the pose on screen. */
    fun exerciseStep(motion: Float?): Int {
        if (motion == null || reps.size < 2) return steps.lastIndex
        val pose = exercisePose(motion)
        return steps.indexOfLast { it.pose == pose }.let { if (it < 0) steps.lastIndex else it }
    }

    companion object { const val STEP_MS = 2600L }
}

enum class DemoView(val file: String) { SIDE("side"), FRONT("front") }

fun demoPhotoPath(figure: String, pose: String, view: DemoView): String = "stretch_demos/$figure-$pose-${view.file}.webp"

/** A node of the step timeline: a step before the shown one, the shown one, or one after it. */
enum class StepNode { DONE, SHOWN, LATER }

/** The timeline's nodes for [count] steps with step [selected] shown. */
fun stepNodes(count: Int, selected: Int): List<StepNode> =
    List(count) { i -> when { i < selected -> StepNode.DONE; i == selected -> StepNode.SHOWN; else -> StepNode.LATER } }

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
