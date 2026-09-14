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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.hypot
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * A line figure in a side or oblique view: twelve joints in a 100-unit box, floor at y = 86.
 * The spine is a curve through the chest so it can round and arch. "F" limbs are nearer the viewer.
 * Poses live in mockup/figures.json; mockup/figures.html previews them; this file is generated from the JSON.
 */
data class Pose(
    val head: Offset, val shoulder: Offset, val chest: Offset, val hip: Offset,
    val kneeF: Offset, val footF: Offset, val kneeB: Offset, val footB: Offset,
    val elbowF: Offset, val handF: Offset, val elbowB: Offset, val handB: Offset,
) {
    fun mix(b: Pose, t: Float) = Pose(
        head.lerp(b.head, t), shoulder.lerp(b.shoulder, t), chest.lerp(b.chest, t), hip.lerp(b.hip, t),
        kneeF.lerp(b.kneeF, t), footF.lerp(b.footF, t), kneeB.lerp(b.kneeB, t), footB.lerp(b.footB, t),
        elbowF.lerp(b.elbowF, t), handF.lerp(b.handF, t), elbowB.lerp(b.elbowB, t), handB.lerp(b.handB, t),
    )
    fun rotateArm(b: Pose, t: Float): Pose {
        val mixed = mix(b, t)
        fun segment(origin: Offset, from: Offset, to: Offset): Offset {
            val start = atan2(from.y, from.x)
            val end = atan2(to.y, to.x)
            var turn = end - start
            while (turn > PI) turn -= (2 * PI).toFloat()
            while (turn < -PI) turn += (2 * PI).toFloat()
            val angle = start + turn * t
            val length = from.getDistance() + (to.getDistance() - from.getDistance()) * t
            return origin + Offset(cos(angle) * length, sin(angle) * length)
        }
        val elbow = segment(mixed.shoulder, elbowF - shoulder, b.elbowF - b.shoulder)
        val hand = segment(elbow, handF - elbowF, b.handF - b.elbowF)
        return mixed.copy(elbowF = elbow, handF = hand)
    }
    private fun Offset.lerp(o: Offset, t: Float) = Offset(x + (o.x - x) * t, y + (o.y - y) * t)
}

private fun p(vararg v: Float) = Pose(
    Offset(v[0], v[1]), Offset(v[2], v[3]), Offset(v[4], v[5]), Offset(v[6], v[7]), Offset(v[8], v[9]), Offset(v[10], v[11]),
    Offset(v[12], v[13]), Offset(v[14], v[15]), Offset(v[16], v[17]), Offset(v[18], v[19]), Offset(v[20], v[21]), Offset(v[22], v[23]),
)

/** Pose pairs by figure key: the start and the end of one breath of the stretch. */
object Figures {
    val poses: Map<String, Pair<Pose, Pose>> = mapOf(
        // Keep your hands down. Alternate bending each knee.
        "elephant" to (p(30f, 62f, 36f, 54f, 46f, 42f, 58f, 32f, 60f, 58f, 60f, 86f, 54f, 62f, 56f, 86f, 30f, 70f, 38f, 86f, 34f, 70f, 44f, 86f)
            to p(30f, 62f, 36f, 54f, 46f, 42f, 58f, 32f, 52f, 62f, 60f, 86f, 58f, 58f, 56f, 86f, 30f, 70f, 38f, 86f, 34f, 70f, 44f, 86f)),
        // Fold at the hips with straight legs. Reach toward the floor.
        "fold" to (p(30f, 58f, 38f, 50f, 48f, 40f, 58f, 32f, 59f, 59f, 60f, 86f, 61f, 59f, 62f, 86f, 32f, 68f, 40f, 86f, 36f, 68f, 46f, 86f)
            to p(34f, 66f, 42f, 56f, 52f, 42f, 58f, 32f, 59f, 59f, 60f, 86f, 61f, 59f, 62f, 86f, 38f, 74f, 44f, 86f, 42f, 74f, 50f, 86f)),
        // Front foot stays planted. Lower the elbow inside it, then rotate the chest and reach up.
        "wgs" to (p(63f, 47f, 55f, 54f, 46f, 57f, 36f, 62f, 64f, 63f, 67f, 86f, 22f, 74f, 6f, 86f, 64f, 76f, 51f, 80f, 51f, 70f, 48f, 86f)
            to p(59f, 32f, 53f, 42f, 45f, 52f, 36f, 62f, 64f, 63f, 67f, 86f, 22f, 74f, 6f, 86f, 57f, 20f, 60f, 5f, 50f, 64f, 48f, 86f)),
        // Slide one arm under the other. Rest that shoulder and the side of your head on the floor.
        "needle" to (p(28f, 58f, 38f, 54f, 52f, 52f, 67f, 51f, 69f, 86f, 90f, 86f, 61f, 79f, 80f, 79f, 43f, 69f, 49f, 85f, 27f, 68f, 18f, 86f)
            to p(28f, 80f, 39f, 83f, 52f, 63f, 67f, 51f, 69f, 86f, 90f, 86f, 61f, 79f, 80f, 79f, 55f, 84f, 73f, 85f, 50f, 65f, 61f, 86f)),
        // Back knee down. Tuck your pelvis and keep your chest tall.
        "hipflexor" to (p(46f, 16f, 46f, 26f, 46f, 40f, 46f, 54f, 68f, 58f, 70f, 86f, 36f, 84f, 14f, 86f, 52f, 44f, 62f, 54f, 40f, 44f, 36f, 56f)
            to p(50f, 14f, 50f, 24f, 50f, 38f, 52f, 52f, 70f, 58f, 70f, 86f, 36f, 84f, 14f, 86f, 56f, 42f, 66f, 52f, 44f, 42f, 40f, 54f)),
        // Both knees bend to 90°. Front shin across you, back shin behind. Lean over the front leg.
        "ninety" to (p(46f, 22f, 46f, 32f, 46f, 48f, 46f, 66f, 22f, 78f, 30f, 94f, 74f, 66f, 74f, 42f, 30f, 48f, 16f, 66f, 62f, 48f, 82f, 63f)
            to p(31f, 36f, 34f, 44f, 40f, 55f, 46f, 66f, 22f, 78f, 30f, 94f, 74f, 66f, 74f, 42f, 18f, 58f, 9f, 75f, 47f, 60f, 57f, 80f)),
        // Reach one hand behind your back and slide it up gently.
        "shoulderir" to (p(50f, 12f, 50f, 22f, 50f, 38f, 50f, 54f, 50f, 70f, 50f, 86f, 52f, 70f, 52f, 86f, 54f, 40f, 56f, 56f, 40f, 36f, 46f, 48f)
            to p(50f, 12f, 50f, 22f, 50f, 38f, 50f, 54f, 50f, 70f, 50f, 86f, 52f, 70f, 52f, 86f, 54f, 40f, 56f, 56f, 38f, 32f, 46f, 36f)),
        // On hands and knees, slowly round your back, then arch it.
        "catcow" to (p(20f, 62f, 32f, 50f, 52f, 36f, 70f, 50f, 72f, 72f, 86f, 86f, 74f, 72f, 88f, 86f, 30f, 68f, 28f, 86f, 34f, 68f, 32f, 86f)
            to p(18f, 40f, 32f, 48f, 52f, 60f, 70f, 52f, 72f, 72f, 86f, 86f, 74f, 72f, 88f, 86f, 30f, 68f, 28f, 86f, 34f, 68f, 32f, 86f)),
        // Legs go over your head. Keep your head still and your weight on your shoulders.
        "plow" to (p(30f, 80f, 42f, 80f, 51f, 61f, 52f, 40f, 33f, 60f, 13f, 81f, 36f, 59f, 16f, 81f, 61f, 84f, 83f, 85f, 60f, 81f, 80f, 82f)
            to p(30f, 80f, 42f, 80f, 51f, 59f, 50f, 37f, 30f, 61f, 9f, 86f, 33f, 60f, 12f, 85f, 61f, 84f, 83f, 85f, 60f, 81f, 80f, 82f)),
    )
    // BEGIN FIGURE METADATA
    val moving = setOf("elephant", "wgs", "catcow")
    private val cues = mapOf(
        "elephant" to "Keep your hands down. Alternate bending each knee.",
        "fold" to "Fold at the hips with straight legs. Reach toward the floor.",
        "wgs" to "Front foot stays planted. Lower the elbow inside it, then rotate the chest and reach up.",
        "needle" to "Slide one arm under the other. Rest that shoulder and the side of your head on the floor.",
        "hipflexor" to "Back knee down. Tuck your pelvis and keep your chest tall.",
        "ninety" to "Both knees bend to 90°. Front shin across you, back shin behind. Lean over the front leg.",
        "shoulderir" to "Reach one hand behind your back and slide it up gently.",
        "catcow" to "On hands and knees, slowly round your back, then arch it.",
        "plow" to "Legs go over your head. Keep your head still and your weight on your shoulders.",
    )
    fun cue(key: String): String = cues[key].orEmpty()
    // END FIGURE METADATA
    val keys: List<String> get() = poses.keys.toList()
    fun of(key: String): Pair<Pose, Pose> = poses[key] ?: poses.getValue("hipflexor")
}

/** Holds show the final position. Reps can use a preview loop or progress supplied by the exercise clock. */
@Composable
fun Figure(figure: String, modifier: Modifier = Modifier, mirror: Boolean = false, color: Color = Color.White.copy(alpha = 0.9f), periodMs: Int = 2000, animate: Boolean = true, progress: Float? = null) {
    val (a, b) = Figures.of(figure)
    val phase = progress?.coerceIn(0f, 1f) ?: if (animate && figure in Figures.moving) {
        val transition = rememberInfiniteTransition(label = "figure")
        val t by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Reverse), label = "t")
        t
    } else 1f
    val eased = if (phase < 0.5f) 2 * phase * phase else -1 + (4 - 2 * phase) * phase
    val pose = if (figure == "wgs") a.rotateArm(b, eased) else a.mix(b, eased)
    Canvas(modifier.semantics { contentDescription = Figures.cue(figure) }) {
        val s = size.minDimension / 100f
        fun pt(o: Offset) = Offset(if (mirror) (100f - o.x) * s else o.x * s, o.y * s)
        val stroke = Stroke(width = 2.6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        if (figure == "ninety") {
            // An oblique floor makes the rear shin read as lying behind the hip, not pointing upward.
            val mat = Path().apply {
                val corners = listOf(Offset(4f, 88f), Offset(50f, 99f), Offset(98f, 45f), Offset(52f, 33f))
                corners.forEachIndexed { i, point -> val v = pt(point); if (i == 0) moveTo(v.x, v.y) else lineTo(v.x, v.y) }
                close()
            }
            drawPath(mat, color.copy(alpha = .06f))
            drawPath(mat, color.copy(alpha = .22f), style = Stroke(width = .6f * s))
        }
        val far = Path().apply {
            moveTo(pt(pose.shoulder).x, pt(pose.shoulder).y); quadraticTo(pt(pose.chest).x, pt(pose.chest).y, pt(pose.hip).x, pt(pose.hip).y)
            fun seg(x: Offset, y: Offset) { moveTo(pt(x).x, pt(x).y); lineTo(pt(y).x, pt(y).y) }
            seg(pose.hip, pose.kneeB); seg(pose.kneeB, pose.footB); seg(pose.shoulder, pose.elbowB); seg(pose.elbowB, pose.handB)
            // neck: shoulder to the edge of the head, computed in pose space so the mirror applies once
            val dx = pose.head.x - pose.shoulder.x; val dy = pose.head.y - pose.shoulder.y; val d = hypot(dx, dy).takeIf { it > 0f } ?: 1f
            seg(pose.shoulder, Offset(pose.head.x - dx / d * 4.5f, pose.head.y - dy / d * 4.5f))
        }
        val near = Path().apply {
            fun seg(x: Offset, y: Offset) { moveTo(pt(x).x, pt(x).y); lineTo(pt(y).x, pt(y).y) }
            seg(pose.hip, pose.kneeF); seg(pose.kneeF, pose.footF); seg(pose.shoulder, pose.elbowF); seg(pose.elbowF, pose.handF)
        }
        drawPath(far, color.copy(alpha = .55f), style = stroke)
        drawPath(near, color.copy(alpha = 1f), style = stroke)
        drawCircle(color, radius = 4.5f * s, center = pt(pose.head), style = Stroke(width = 2.6f * s))
        if (figure != "ninety") drawLine(color.copy(alpha = 0.35f), Offset(6f * s, 86f * s), Offset(94f * s, 86f * s), strokeWidth = 1f * s, cap = StrokeCap.Round)
    }
}
