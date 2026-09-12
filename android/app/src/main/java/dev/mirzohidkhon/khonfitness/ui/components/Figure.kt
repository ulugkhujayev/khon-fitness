package dev.mirzohidkhon.khonfitness.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path

/**
 * A line figure: nine joints in a 100-unit box. A stretch has two poses; the figure eases between them and back.
 * Joints: head (circle center), neck, hip, kneeF, footF, kneeB, footB, handF, handB. "F" is the leg or arm nearer the viewer.
 * The floor is the line y = 86. Right-side stretches mirror the figure.
 */
data class Pose(
    val head: Offset, val neck: Offset, val hip: Offset,
    val kneeF: Offset, val footF: Offset, val kneeB: Offset, val footB: Offset,
    val handF: Offset, val handB: Offset,
) {
    fun mix(b: Pose, t: Float) = Pose(
        head.lerp(b.head, t), neck.lerp(b.neck, t), hip.lerp(b.hip, t),
        kneeF.lerp(b.kneeF, t), footF.lerp(b.footF, t), kneeB.lerp(b.kneeB, t), footB.lerp(b.footB, t),
        handF.lerp(b.handF, t), handB.lerp(b.handB, t),
    )
    private fun Offset.lerp(o: Offset, t: Float) = Offset(x + (o.x - x) * t, y + (o.y - y) * t)
}

private fun p(hx: Float, hy: Float, nx: Float, ny: Float, px: Float, py: Float, kfx: Float, kfy: Float, ffx: Float, ffy: Float, kbx: Float, kby: Float, fbx: Float, fby: Float, afx: Float, afy: Float, abx: Float, aby: Float) =
    Pose(Offset(hx, hy), Offset(nx, ny), Offset(px, py), Offset(kfx, kfy), Offset(ffx, ffy), Offset(kbx, kby), Offset(fbx, fby), Offset(afx, afy), Offset(abx, aby))

/** Pose pairs by figure key. The pair is the start and the end of one breath of the stretch. */
object Figures {
    val poses: Map<String, Pair<Pose, Pose>> = mapOf(
        // Half-kneeling lunge; the pelvis tucks forward over the breath.
        "hipflexor" to (p(52f, 18f, 52f, 26f, 50f, 56f, 70f, 60f, 72f, 84f, 36f, 84f, 16f, 86f, 74f, 50f, 30f, 50f)
            to p(50f, 14f, 50f, 22f, 46f, 54f, 70f, 58f, 74f, 84f, 30f, 84f, 10f, 86f, 76f, 44f, 24f, 46f)),
        // Standing fold, palms toward the floor; the hands sink lower.
        "fold" to (p(44f, 60f, 48f, 52f, 60f, 30f, 60f, 58f, 60f, 86f, 62f, 58f, 62f, 86f, 40f, 72f, 44f, 74f)
            to p(42f, 66f, 46f, 56f, 60f, 30f, 60f, 58f, 60f, 86f, 62f, 58f, 62f, 86f, 40f, 84f, 44f, 85f)),
        // Elephant walk: fold with one heel pressing down, then the other.
        "elephant" to (p(44f, 58f, 48f, 50f, 60f, 30f, 62f, 56f, 64f, 86f, 58f, 52f, 56f, 78f, 40f, 82f, 44f, 83f)
            to p(44f, 58f, 48f, 50f, 60f, 30f, 62f, 52f, 62f, 78f, 58f, 56f, 58f, 86f, 40f, 82f, 44f, 83f)),
        // World's greatest stretch: low lunge, one hand down, the other arm opens up to the ceiling.
        "wgs" to (p(58f, 30f, 56f, 38f, 44f, 60f, 70f, 62f, 74f, 86f, 26f, 76f, 8f, 86f, 66f, 86f, 46f, 86f)
            to p(60f, 24f, 58f, 34f, 44f, 60f, 70f, 62f, 74f, 86f, 26f, 76f, 8f, 86f, 60f, 6f, 46f, 86f)),
        // Thread the needle on all fours; the shoulder sinks to the floor.
        "needle" to (p(34f, 56f, 40f, 54f, 66f, 54f, 68f, 76f, 74f, 86f, 70f, 76f, 78f, 86f, 22f, 60f, 36f, 84f)
            to p(30f, 66f, 38f, 60f, 66f, 54f, 68f, 76f, 74f, 86f, 70f, 76f, 78f, 86f, 12f, 70f, 36f, 84f)),
        // 90/90 seated; the torso leans over the front shin.
        "ninety" to (p(48f, 22f, 48f, 32f, 46f, 62f, 70f, 66f, 78f, 84f, 30f, 72f, 22f, 86f, 66f, 66f, 30f, 62f)
            to p(58f, 30f, 56f, 38f, 46f, 62f, 70f, 66f, 78f, 84f, 30f, 72f, 22f, 86f, 78f, 74f, 40f, 68f)),
        // Standing, one hand behind the back walks up the spine.
        "shoulderir" to (p(50f, 12f, 50f, 22f, 50f, 54f, 46f, 70f, 45f, 86f, 54f, 70f, 55f, 86f, 40f, 44f, 58f, 50f)
            to p(50f, 12f, 50f, 22f, 50f, 54f, 46f, 70f, 45f, 86f, 54f, 70f, 55f, 86f, 40f, 44f, 56f, 38f)),
        // Cat cow on all fours: the spine rounds, then arches.
        "catcow" to (p(22f, 52f, 32f, 44f, 64f, 42f, 68f, 66f, 70f, 86f, 72f, 66f, 76f, 86f, 26f, 86f, 34f, 86f)
            to p(20f, 42f, 30f, 52f, 64f, 56f, 68f, 70f, 70f, 86f, 72f, 70f, 76f, 86f, 26f, 86f, 34f, 86f)),
        // Plow: on the back, legs over the head; the toes reach toward the floor.
        "plow" to (p(78f, 80f, 68f, 72f, 50f, 40f, 32f, 50f, 22f, 70f, 34f, 50f, 24f, 72f, 60f, 86f, 66f, 86f)
            to p(78f, 80f, 68f, 72f, 50f, 40f, 30f, 56f, 20f, 80f, 32f, 56f, 22f, 82f, 60f, 86f, 66f, 86f)),
    )
    val keys: List<String> get() = poses.keys.toList()
    fun of(key: String): Pair<Pose, Pose> = poses[key] ?: poses.getValue("hipflexor")
}

/** Draws the figure for [figure], looping between its two poses over [periodMs]. [mirror] flips it for the right side. */
@Composable
fun Figure(figure: String, modifier: Modifier = Modifier, mirror: Boolean = false, color: Color = Color.White.copy(alpha = 0.9f), periodMs: Int = 2000, animate: Boolean = true) {
    val (a, b) = Figures.of(figure)
    val transition = rememberInfiniteTransition(label = "figure")
    val t by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Reverse), label = "t")
    val phase = if (animate) t else 0f
    // Ease in and out so the figure rests at each pose.
    val eased = if (phase < 0.5f) 2 * phase * phase else -1 + (4 - 2 * phase) * phase
    val pose = a.mix(b, eased)
    Canvas(modifier) {
        val s = size.minDimension / 100f
        fun pt(o: Offset) = Offset(if (mirror) (100f - o.x) * s else o.x * s, o.y * s)
        val stroke = Stroke(width = 2.6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            fun seg(x: Offset, y: Offset) { moveTo(pt(x).x, pt(x).y); lineTo(pt(y).x, pt(y).y) }
            seg(pose.neck, pose.hip); seg(pose.hip, pose.kneeF); seg(pose.kneeF, pose.footF); seg(pose.hip, pose.kneeB); seg(pose.kneeB, pose.footB)
            seg(pose.neck, pose.handF); seg(pose.neck, pose.handB)
        }
        drawPath(path, color, style = stroke)
        drawCircle(color, radius = 4.5f * s, center = pt(pose.head), style = Stroke(width = 2.6f * s))
        drawLine(color.copy(alpha = 0.35f), Offset(6f * s, 86f * s), Offset(94f * s, 86f * s), strokeWidth = 1f * s, cap = StrokeCap.Round)
    }
}
