package dev.mirzohidkhon.khonfitness.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
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
fun TabScreen(title: String, trailing: (@Composable () -> Unit)? = null, scroll: ScrollState = rememberScrollState(), content: @Composable ColumnScope.() -> Unit) =
    TabScreen(trailing, scroll, title = { scale -> LargeTitle(title, scale) }, content = content)

/**
 * A tab screen whose large title is its own [title] content, drawn in the title row's leading space. It gets the
 * title's scale as the bar folds ([LargeTitleFold.titleScale], read in a draw layer or a derived state).
 */
@Composable
fun TabScreen(trailing: (@Composable () -> Unit)?, scroll: ScrollState, title: @Composable RowScope.(scale: () -> Float) -> Unit, content: @Composable ColumnScope.() -> Unit) {
    // The row is a tap target tall, or as tall as the title's line at a large font scale.
    val row = with(LocalDensity.current) { max(44.dp, 41.sp.toDp()).value }
    val geometry = remember(row) { LargeTitleFold(row) }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 16.dp).padding(top = (geometry.height + 4).dp, bottom = TAB_CONTENT_BOTTOM),
            content = content,
        )
        LargeTitleHeader(geometry, trailing, { scroll.value }, title)
    }
}

/** The plain large title: one heading that shrinks toward its leading edge by [scale]. */
@Composable
private fun RowScope.LargeTitle(title: String, scale: () -> Float) {
    Text(
        title, style = MaterialTheme.typography.headlineLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f).semantics { heading() }.graphicsLayer {
            val s = scale()
            scaleX = s; scaleY = s; transformOrigin = TransformOrigin(0f, 0.5f)
        },
    )
}

/**
 * The large title of a tab screen with its bar items, over the top of the content. As the content scrolls the
 * bar's bottom edge rises with it, the title shrinks toward its leading edge, and a hairline shows once content
 * passes under the bar (iOS large title). The scroll offset is read in the draw layers only, so scrolling does
 * not recompose the header.
 */
@Composable
fun LargeTitleHeader(geometry: LargeTitleFold, trailing: (@Composable () -> Unit)?, scrollPx: () -> Int, title: @Composable RowScope.(scale: () -> Float) -> Unit) {
    val density = LocalDensity.current.density
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
            title { geometry.titleScale(scrollPx() / density) }
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

/** The smallest size title tabs step down to when their words do not fit the title row (a large font scale). */
const val TITLE_TABS_MIN_SP = 22f
/** Space between two title tabs. */
private val TITLE_TABS_GAP = 16.dp
/** Room under a title tab's word for its accent bar: a 6 dp gap and the 3 dp bar. */
private val TITLE_TAB_BAR = 9.dp

/**
 * [base] at the largest whole size from its own down to [TITLE_TABS_MIN_SP] at which [width] (px, for a style) fits
 * [room] px, its line height scaled with it.
 */
fun titleTabsStyle(base: TextStyle, room: Int, width: (TextStyle) -> Int): TextStyle {
    fun at(sp: Float) = base.copy(fontSize = sp.sp, lineHeight = (base.lineHeight.value * sp / base.fontSize.value).sp)
    var sp = base.fontSize.value
    while (sp > TITLE_TABS_MIN_SP && width(at(sp)) > room) sp = maxOf(TITLE_TABS_MIN_SP, sp - 1f)
    return at(sp)
}

/**
 * Pages named in a tab screen's large title row ([TabScreen]): each of [words] is a tab in the large title's size and
 * weight, the open one in [K.Text] with a 3 dp accent bar under it as wide as the word, the closed ones in
 * [K.TitleOff]. The row is a heading spoken as [name]. Each word is a tap target of 48 dp or more. When the words do
 * not fit the row (a large font scale) their size steps down ([titleTabsStyle]). As the bar folds the words shrink by
 * [scale] like a title: the closed words and the bar fade, and the open word moves to the leading edge, so the folded
 * bar shows the open page's word. A closed word takes no taps once the bar is half folded.
 */
@Composable
fun RowScope.TitleTabs(words: List<String>, selected: Int, name: String, scale: () -> Float, onSelect: (Int) -> Unit) {
    val tick = rememberTick()
    val base = MaterialTheme.typography.headlineLarge
    val measurer = rememberTextMeasurer()
    // 0 while the bar is open, 1 once it is folded.
    val fold = { ((1f - scale()) / (1f - LargeTitleFold.TITLE_SCALE)).coerceIn(0f, 1f) }
    val folded by remember(scale) { derivedStateOf { fold() >= 0.5f } }
    BoxWithConstraints(Modifier.weight(1f)) {
        val density = LocalDensity.current
        val room = constraints.maxWidth
        val style = remember(words, room, base, measurer, density) {
            val gap = with(density) { TITLE_TABS_GAP.roundToPx() }
            val tab = with(density) { MIN_TAP.dp.roundToPx() }
            titleTabsStyle(base, room) { st -> words.sumOf { maxOf(tab, measurer.measure(it, st, softWrap = false, maxLines = 1).size.width) } + gap * (words.size - 1) }
        }
        Row(
            Modifier.graphicsLayer { val s = scale(); scaleX = s; scaleY = s; transformOrigin = TransformOrigin(0f, 0.5f) }
                .semantics { heading(); contentDescription = name }.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(TITLE_TABS_GAP), verticalAlignment = Alignment.CenterVertically,
        ) {
            words.forEachIndexed { i, w ->
                val on = i == selected
                var x by remember { mutableFloatStateOf(0f) }
                val tap = if (!on && folded) Modifier.clearAndSetSemantics {}
                    else Modifier.selectable(on, interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Tab) { if (!on) { tick(false); onSelect(i) } }
                Box(
                    // The word and its bar are taller than the row at the default size: they reach past it, centered.
                    Modifier.onPlaced { x = it.positionInParent().x }.graphicsLayer { if (on) translationX = -x * fold() else alpha = 1f - fold() }
                        .wrapContentHeight(unbounded = true).then(tap).heightIn(min = MIN_TAP.dp).widthIn(min = MIN_TAP.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        w, style = style, color = if (on) K.Text else K.TitleOff, maxLines = 1,
                        modifier = Modifier.padding(bottom = TITLE_TAB_BAR).drawBehind {
                            if (on) drawRoundRect(K.Accent, Offset(0f, size.height + 6.dp.toPx()), Size(size.width, 3.dp.toPx()), CornerRadius(1.5.dp.toPx()), alpha = 1f - fold())
                        },
                    )
                }
            }
        }
    }
}

/** The trailing bar action as a 34 dp accent capsule on a 48 dp target: Done, Edit, Save. Off ([enabled] false) it turns grey. */
@Composable
fun Capsule(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    val press = remember { MutableInteractionSource() }
    Box(Modifier.heightIn(min = MIN_TAP.dp).widthIn(min = MIN_TAP.dp).clickable(press, null, enabled = enabled, role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(if (enabled) K.Accent else K.Surface2).indication(press, LocalIndication.current).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text(text, color = if (enabled) K.AccentInk else K.Muted, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
        }
    }
}
