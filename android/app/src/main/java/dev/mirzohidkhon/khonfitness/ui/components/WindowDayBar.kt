package dev.mirzohidkhon.khonfitness.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import dev.mirzohidkhon.khonfitness.data.EatingWindow
import dev.mirzohidkhon.khonfitness.data.EatingWindowRules
import dev.mirzohidkhon.khonfitness.ui.theme.K
import java.time.LocalDateTime

/**
 * The day from 00:00 to 24:00: an 8 dp track with the fixed hours in green and a 2 × 16 dp white mark at now, the
 * opening and closing times under the ends of the green ([dayBarLabels] keeps them inside and apart). Decorative:
 * the headline above it says the same in words.
 */
@Composable
fun WindowDayBar(w: EatingWindow, now: LocalDateTime, modifier: Modifier = Modifier) {
    val s = dayShares(w, now)
    Column(modifier.fillMaxWidth().clearAndSetSemantics {}) {
        Box(
            Modifier.fillMaxWidth().height(16.dp).drawBehind {
                val track = 8.dp.toPx()
                val top = (size.height - track) / 2
                val radius = CornerRadius(track / 2)
                drawRoundRect(K.Surface3, Offset(0f, top), Size(size.width, track), radius)
                // The hours: square inside the bar, rounded where they reach its ends.
                clipRoundTrack(top, track) {
                    drawRect(K.Green, Offset(s.from * size.width, top), Size((s.to - s.from) * size.width, track))
                }
                val mark = 2.dp.toPx()
                drawRoundRect(Color.White, Offset(dayBarMark(s.now, size.width, mark), 0f), Size(mark, size.height), CornerRadius(mark / 2))
            },
        )
        val label = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")
        Layout(
            content = {
                Text(EatingWindowRules.hhmm(w.startMinute), style = label, color = K.Muted, maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
                Text(EatingWindowRules.hhmm(w.endMinute), style = label, color = K.Muted, maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
            },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        ) { measurables, constraints ->
            val loose = Constraints(maxWidth = constraints.maxWidth)
            val (start, end) = measurables.map { it.measure(loose) }
            val width = constraints.maxWidth.toFloat()
            val (a, b) = dayBarLabels(s.from * width, s.to * width, width, start.width.toFloat(), end.width.toFloat(), 4.dp.toPx())
            layout(constraints.maxWidth, maxOf(start.height, end.height)) {
                start.place(a.toInt(), 0)
                end.place(b.toInt(), 0)
            }
        }
    }
}

/** Draws [block] clipped to the rounded track. */
private inline fun DrawScope.clipRoundTrack(top: Float, track: Float, block: DrawScope.() -> Unit) {
    val path = Path().apply { addRoundRect(RoundRect(0f, top, size.width, top + track, CornerRadius(track / 2))) }
    clipPath(path) { block() }
}

/**
 * The last 28 days as four weeks of seven 12 dp dots on an elevated card, oldest first: filled green when the
 * fixed hours were kept, a green ring when opened or closed by hand, today a white ring. Spoken as one summary.
 */
@Composable
fun WindowWeeks(cells: List<StripCell>, summary: String, modifier: Modifier = Modifier) {
    GroupedList(modifier, elevated = true) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp).clearAndSetSemantics { contentDescription = "Last 28 days. $summary" }) {
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    week.forEach { cell ->
                        Box(Modifier.size(28.dp, 24.dp), contentAlignment = Alignment.Center) {
                            if (cell == StripCell.BEFORE) Dot(K.Surface3, filled = true, size = 12)
                            else Dot(if (cell == StripCell.TODAY) K.Text else K.Green, filled = cell == StripCell.KEPT, size = 12)
                        }
                    }
                }
            }
        }
    }
}
