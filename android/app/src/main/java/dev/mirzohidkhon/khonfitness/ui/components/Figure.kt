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

/**
 * A line figure seen from the side: twelve joints in a 100-unit box, floor at y = 86.
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
    private fun Offset.lerp(o: Offset, t: Float) = Offset(x + (o.x - x) * t, y + (o.y - y) * t)
}

private fun p(vararg v: Float) = Pose(
    Offset(v[0], v[1]), Offset(v[2], v[3]), Offset(v[4], v[5]), Offset(v[6], v[7]), Offset(v[8], v[9]), Offset(v[10], v[11]),
    Offset(v[12], v[13]), Offset(v[14], v[15]), Offset(v[16], v[17]), Offset(v[18], v[19]), Offset(v[20], v[21]), Offset(v[22], v[23]),
)

/** Pose pairs by figure key: the start and the end of one breath of the stretch. */
object Figures {
    val poses: Map<String, Pair<Pose, Pose>> = mapOf(
        // Standing fold, palms flat on the floor. One knee bends while the other leg straightens, then they swap.
        "elephant" to (p(30f, 62f, 36f, 54f, 46f, 42f, 58f, 32f, 60f, 58f, 60f, 86f, 54f, 62f, 56f, 86f, 30f, 70f, 38f, 86f, 34f, 70f, 44f, 86f)
            to p(30f, 62f, 36f, 54f, 46f, 42f, 58f, 32f, 52f, 62f, 60f, 86f, 58f, 58f, 56f, 86f, 30f, 70f, 38f, 86f, 34f, 70f, 44f, 86f)),
        // Standing hamstring fold with straight legs. The palms reach the floor and the chest sinks toward the shins.
        "fold" to (p(30f, 58f, 38f, 50f, 48f, 40f, 58f, 32f, 59f, 59f, 60f, 86f, 61f, 59f, 62f, 86f, 32f, 68f, 40f, 86f, 36f, 68f, 46f, 86f)
            to p(34f, 66f, 42f, 56f, 52f, 42f, 58f, 32f, 59f, 59f, 60f, 86f, 61f, 59f, 62f, 86f, 38f, 74f, 44f, 86f, 42f, 74f, 50f, 86f)),
        // Low lunge, back leg straight, torso folded forward. The near hand is on the floor inside the front foot; the other arm opens from the floor up to the ceiling.
        "wgs" to (p(52f, 42f, 47f, 50f, 42f, 56f, 36f, 62f, 60f, 64f, 66f, 86f, 22f, 76f, 6f, 86f, 55f, 66f, 58f, 86f, 47f, 68f, 48f, 86f)
            to p(54f, 34f, 49f, 44f, 43f, 54f, 36f, 62f, 60f, 64f, 66f, 86f, 22f, 76f, 6f, 86f, 58f, 28f, 62f, 12f, 47f, 68f, 48f, 86f)),
        // On all fours. One arm threads under the chest and the shoulder and ear rest on the floor.
        "needle" to (p(26f, 60f, 34f, 56f, 50f, 52f, 66f, 50f, 68f, 74f, 80f, 86f, 72f, 74f, 84f, 86f, 22f, 72f, 16f, 86f, 42f, 70f, 54f, 86f)
            to p(24f, 74f, 36f, 66f, 50f, 56f, 66f, 50f, 68f, 74f, 80f, 86f, 72f, 74f, 84f, 86f, 46f, 80f, 62f, 86f, 38f, 70f, 44f, 86f)),
        // Half kneel, back knee on the floor, torso tall. The pelvis tucks and glides forward over the front foot.
        "hipflexor" to (p(46f, 16f, 46f, 26f, 46f, 40f, 46f, 54f, 68f, 58f, 70f, 86f, 36f, 84f, 14f, 86f, 52f, 44f, 62f, 54f, 40f, 44f, 36f, 56f)
            to p(50f, 14f, 50f, 24f, 50f, 38f, 52f, 52f, 70f, 58f, 70f, 86f, 36f, 84f, 14f, 86f, 56f, 42f, 66f, 52f, 44f, 42f, 40f, 54f)),
        // Seated with the front shin across in front and the back shin folded behind. The torso hinges forward over the front shin.
        "ninety" to (p(44f, 26f, 44f, 36f, 44f, 50f, 44f, 66f, 70f, 70f, 84f, 86f, 22f, 72f, 8f, 86f, 52f, 54f, 58f, 70f, 36f, 54f, 32f, 70f)
            to p(62f, 44f, 58f, 50f, 50f, 58f, 44f, 66f, 70f, 70f, 84f, 86f, 22f, 72f, 8f, 86f, 68f, 64f, 82f, 76f, 54f, 66f, 62f, 80f)),
        // Standing. One hand goes behind the back and walks up the spine; the other arm hangs.
        "shoulderir" to (p(50f, 12f, 50f, 22f, 50f, 38f, 50f, 54f, 50f, 70f, 50f, 86f, 52f, 70f, 52f, 86f, 54f, 40f, 56f, 56f, 40f, 36f, 46f, 48f)
            to p(50f, 12f, 50f, 22f, 50f, 38f, 50f, 54f, 50f, 70f, 50f, 86f, 52f, 70f, 52f, 86f, 54f, 40f, 56f, 56f, 38f, 32f, 46f, 36f)),
        // On all fours. The spine rounds up to the ceiling with the head tucked, then sags with the head up.
        "catcow" to (p(20f, 62f, 32f, 50f, 52f, 36f, 70f, 50f, 72f, 72f, 86f, 86f, 74f, 72f, 88f, 86f, 30f, 68f, 28f, 86f, 34f, 68f, 32f, 86f)
            to p(18f, 40f, 32f, 48f, 52f, 60f, 70f, 52f, 72f, 72f, 86f, 86f, 74f, 72f, 88f, 86f, 30f, 68f, 28f, 86f, 34f, 68f, 32f, 86f)),
        // Lying on the back, hips lifted, both legs over the head with the toes reaching for the floor behind.
        "plow" to (p(80f, 80f, 70f, 80f, 62f, 62f, 56f, 44f, 40f, 52f, 24f, 68f, 42f, 52f, 26f, 70f, 54f, 84f, 38f, 86f, 56f, 84f, 40f, 86f)
            to p(80f, 80f, 70f, 80f, 62f, 62f, 56f, 44f, 36f, 58f, 20f, 82f, 38f, 58f, 22f, 84f, 54f, 84f, 38f, 86f, 56f, 84f, 40f, 86f)),
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
    val eased = if (phase < 0.5f) 2 * phase * phase else -1 + (4 - 2 * phase) * phase
    val pose = a.mix(b, eased)
    Canvas(modifier) {
        val s = size.minDimension / 100f
        fun pt(o: Offset) = Offset(if (mirror) (100f - o.x) * s else o.x * s, o.y * s)
        val stroke = Stroke(width = 2.6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
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
        drawPath(far, color, style = stroke)
        drawPath(near, color.copy(alpha = 1f), style = stroke)
        drawCircle(color, radius = 4.5f * s, center = pt(pose.head), style = Stroke(width = 2.6f * s))
        drawLine(color.copy(alpha = 0.35f), Offset(6f * s, 86f * s), Offset(94f * s, 86f * s), strokeWidth = 1f * s, cap = StrokeCap.Round)
    }
}
