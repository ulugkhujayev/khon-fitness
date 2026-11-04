package dev.mirzohidkhon.khonfitness.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.mirzohidkhon.khonfitness.data.StepNode
import dev.mirzohidkhon.khonfitness.data.stepNodes
import dev.mirzohidkhon.khonfitness.ui.theme.K

/** One segment of a run's progress: a stretch or side done, the one now, or one still to come. */
enum class RunSegment { DONE, CURRENT, AHEAD }

/** One segment per stretch and side of a run of [count] phases at [index]. */
fun runSegments(count: Int, index: Int): List<RunSegment> =
    List(count) { i -> when { i < index -> RunSegment.DONE; i == index -> RunSegment.CURRENT; else -> RunSegment.AHEAD } }

/** Done segments on the player's black and green: white at 60 percent. Text there takes [K.OnPhaseMuted], 75 percent. */
private val OnPhaseDim = Color.White.copy(alpha = 0.6f)
/** The soft white under the player's round controls and the segments still to come. */
private val OnPhaseSoft = Color.White.copy(alpha = 0.18f)
/** The dark glass under the demonstration's round controls, like a video player's. */
private val Glass = Color.Black.copy(alpha = 0.45f)

/**
 * A round control on the stretch player's bar, Stop, Back or Skip: a white symbol on white at 18 percent, readable on the
 * player's black and on its green, 40 dp in a 48 dp target. Off, it fades to 35 percent and does nothing.
 */
@Composable
fun PlayerButton(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(enabled = enabled, role = Role.Button, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(40.dp).alpha(if (enabled) 1f else 0.35f).clip(CircleShape).background(OnPhaseSoft), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
        }
    }
}

/**
 * Where a routine run is, under the player's bar: one 3 dp segment per stretch and side, 3 dp apart (done white at 60
 * percent, the current one white, the rest white at 18 percent), and [text] ("3 of 12 · 6:30 remaining") under them.
 * Screen readers get the text and the step as a progress value.
 */
@Composable
fun RoutineProgress(segments: List<RunSegment>, index: Int, text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().clearAndSetSemantics {
        contentDescription = text
        progressBarRangeInfo = ProgressBarRangeInfo((index + 1).toFloat(), 1f..segments.size.coerceAtLeast(1).toFloat())
    }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            segments.forEach { s ->
                val color = when (s) {
                    RunSegment.DONE -> OnPhaseDim
                    RunSegment.CURRENT -> Color.White
                    RunSegment.AHEAD -> OnPhaseSoft
                }
                Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(1.5.dp)).background(color))
            }
        }
        Text(text, style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"), color = K.OnPhaseMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
    }
}

/** A round control over a bottom corner of the demonstration: a white symbol on black at 45 percent, 40 dp in a 48 dp target. */
@Composable
fun DemoControl(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(MIN_TAP.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Glass), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * A guide's steps as a timeline: 26 dp numbered nodes on a 3 dp line, each title under its node in footnote muted.
 * Steps before the shown one are accent with white numbers, the shown one also sits in a 34 dp accent-tinted halo
 * with its title white and semibold, later ones Surface3 with muted numbers. The line runs between the first and
 * the last node's centers in Surface2 and fills in accent up to [fill] (0..1, [StretchDemo.timelineFill]). A tap
 * on a step holds its pose.
 */
@Composable
fun StepTimeline(titles: List<String>, selected: Int, fill: () -> Float, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val nodes = stepNodes(titles.size, selected)
    Row(modifier.fillMaxWidth().drawBehind {
        if (titles.size < 2) return@drawBehind
        val column = size.width / titles.size
        val track = size.width - column
        val line = 3.dp.toPx()
        val top = 17.dp.toPx() - line / 2
        val radius = CornerRadius(line / 2)
        drawRoundRect(K.Surface2, Offset(column / 2, top), Size(track, line), radius)
        val filled = track * fill().coerceIn(0f, 1f)
        if (filled > 0f) drawRoundRect(K.Accent, Offset(column / 2, top), Size(filled, line), radius)
    }) {
        titles.forEachIndexed { i, title ->
            val node = nodes[i]
            val reached = node != StepNode.LATER
            Column(
                Modifier.weight(1f).heightIn(min = 48.dp)
                    .selectable(selected = node == StepNode.SHOWN, role = Role.Tab, onClick = { onSelect(i) })
                    .clearAndSetSemantics { contentDescription = "Step ${i + 1}: $title" },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(34.dp).then(if (node == StepNode.SHOWN) Modifier.clip(CircleShape).background(K.AccentSoft) else Modifier), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(26.dp).clip(CircleShape).background(if (reached) K.Accent else K.Surface3), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.Bold, color = if (reached) K.AccentInk else K.Muted)
                    }
                }
                Text(
                    title, style = MaterialTheme.typography.bodySmall, color = if (node == StepNode.SHOWN) K.Text else K.Muted,
                    fontWeight = if (node == StepNode.SHOWN) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 4.dp),
                )
            }
        }
    }
}

/** A bare symbol as a row action (move up, move down, remove) on a 48 dp target; off, it turns dim and does nothing. */
@Composable
fun SymbolButton(icon: ImageVector, label: String, color: Color = K.Accent, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(MIN_TAP.dp).clip(CircleShape).clickable(enabled = enabled, role = Role.Button, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = if (enabled) color else K.Dim, modifier = Modifier.size(20.dp)) }
}
