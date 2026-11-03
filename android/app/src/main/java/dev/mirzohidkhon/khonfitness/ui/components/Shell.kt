package dev.mirzohidkhon.khonfitness.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import dev.mirzohidkhon.khonfitness.ui.theme.K
import dev.mirzohidkhon.khonfitness.ui.theme.Motion

/**
 * A detent as a selection or a value changes: a light click, a firmer one for a major value. The view's haptic
 * constants follow the system touch-feedback setting, so with it off nothing happens.
 */
@Composable
fun rememberTick(): (major: Boolean) -> Unit {
    val view = LocalView.current
    return remember(view) { { major -> view.performHapticFeedback(if (major) HapticFeedbackConstants.CONTEXT_CLICK else HapticFeedbackConstants.CLOCK_TICK) } }
}

/** Room between the end of a tab screen's content and the tab bar. */
private val TAB_CONTENT_BOTTOM = 24.dp

/**
 * A tab screen: the large title with its bar items stays at the top and folds as the content scrolls under it
 * ([LargeTitleHeader]); the content scrolls with 16 dp margins. A screen with pages passes each page's own [scroll].
 */
@Composable
fun TabScreen(title: String, trailing: (@Composable () -> Unit)? = null, scroll: ScrollState = rememberScrollState(), content: @Composable ColumnScope.() -> Unit) {
    // The row is a tap target tall, or as tall as the title's line at a large font scale.
    val row = with(LocalDensity.current) { max(44.dp, 41.sp.toDp()).value }
    val geometry = remember(row) { LargeTitleFold(row) }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 16.dp).padding(top = (geometry.height + 4).dp, bottom = TAB_CONTENT_BOTTOM),
            content = content,
        )
        LargeTitleHeader(title, geometry, trailing) { scroll.value }
    }
}

/**
 * The large title of a tab screen with its bar items, over the top of the content. As the content scrolls the
 * bar's bottom edge rises with it, the title shrinks toward its leading edge, and a hairline shows once content
 * passes under the bar (iOS large title). The scroll offset is read in the draw layers only, so scrolling does
 * not recompose the header.
 */
@Composable
fun LargeTitleHeader(title: String, geometry: LargeTitleFold, trailing: (@Composable () -> Unit)?, scrollPx: () -> Int) {
    Box(
        Modifier.fillMaxWidth().height(geometry.height.dp)
            .graphicsLayer { translationY = -geometry.barShift(scrollPx() / density).dp.toPx() }
            .background(K.Bg),
    ) {
        Row(
            Modifier.padding(top = geometry.rowTop.dp).height(geometry.row.dp).padding(horizontal = 16.dp)
                .graphicsLayer { translationY = geometry.rowShift(scrollPx() / density).dp.toPx() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title, style = MaterialTheme.typography.headlineLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).semantics { heading() }.graphicsLayer {
                    val s = geometry.titleScale(scrollPx() / density)
                    scaleX = s; scaleY = s; transformOrigin = TransformOrigin(0f, 0.5f)
                },
            )
            trailing?.invoke()
        }
        BarHairline(Modifier.align(Alignment.BottomStart)) { geometry.hairline(scrollPx() / density) }
    }
}

/** Hairline on a bar's bottom edge; [opacity] is read in the draw layer. */
@Composable
fun BarHairline(modifier: Modifier = Modifier, opacity: androidx.compose.ui.unit.Density.() -> Float) {
    Box(modifier.fillMaxWidth().height(0.7.dp).graphicsLayer { alpha = opacity() }.background(K.Divider))
}

/**
 * A row of equal slots with a selection marker that slides between them (the tab bar, a segmented control).
 * Its first place is set at once; later changes slide on [Motion.slide]. No marker while [selected] is -1.
 */
@Composable
fun SlidingCapsuleRow(count: Int, selected: Int, shape: Shape, color: Color, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    BoxWithConstraints(modifier) {
        val slot = maxWidth / count
        val x by animateDpAsState(slot * selected.coerceAtLeast(0), Motion.slide(), label = "capsule")
        if (selected >= 0) Box(Modifier.offset { IntOffset(x.roundToPx(), 0) }.width(slot).fillMaxHeight().clip(shape).background(color))
        Row(Modifier.fillMaxSize().selectableGroup(), content = content)
    }
}

/**
 * Fixed-width segmented control: every option gets an equal share, nothing scrolls. The selected segment's
 * capsule slides to the option tapped, with a light haptic click, like the tab bar's. [height] 48 dp makes each
 * segment a full tap target.
 */
@Composable
fun Segmented(options: List<String>, selected: Int, height: androidx.compose.ui.unit.Dp = 36.dp, onSelect: (Int) -> Unit) {
    val tick = rememberTick()
    SlidingCapsuleRow(
        options.size, selected, RoundedCornerShape(8.dp), K.Surface3,
        Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(10.dp)).background(K.Surface).padding(3.dp),
    ) {
        options.forEachIndexed { i, label ->
            val active = i == selected
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp))
                    .selectable(active, interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Tab) { if (!active) { tick(false); onSelect(i) } },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (active) K.Text else K.Muted, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** The trailing bar action as a 34 dp accent capsule: Done, Edit, Save. Off ([enabled] false) it turns grey. */
@Composable
fun Capsule(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(if (enabled) K.Accent else K.Surface2).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
        Text(text, color = if (enabled) K.AccentInk else K.Muted, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
    }
}
