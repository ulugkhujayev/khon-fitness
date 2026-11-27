package dev.mirzohidkhon.khonfitness.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.Modality
import dev.mirzohidkhon.khonfitness.data.Night
import dev.mirzohidkhon.khonfitness.data.NightStore
import dev.mirzohidkhon.khonfitness.data.NightsState
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.health.HcStatus
import dev.mirzohidkhon.khonfitness.health.HealthImport
import dev.mirzohidkhon.khonfitness.health.HealthSleepSource
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.Inter
import dev.mirzohidkhon.khonfitness.ui.theme.K
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.launch

const val NIGHTS_ALLOW_LINE = "Khon Fitness reads your sleep sessions from Health Connect. It only reads them."
const val NIGHTS_FAILED = "Could not read Health Connect"
const val NIGHTS_EMPTY = "No night data. Health Connect has no sleep sessions."
const val NIGHT_BEFORE_TITLE = "Night before a session · last 8 weeks"
const val NIGHTS_CHART_TAG = "nights-chart"
fun nightBarTag(day: LocalDate) = "night-bar-$day"
fun nightFillTag(day: LocalDate) = "night-fill-$day"
fun nightDotTag(day: LocalDate) = "night-dot-$day"

private val PLOT_HEIGHT = 132.dp
private val PLOT_TOP = 10.dp
private val AXIS_GAP = 8.dp
private val BAR_WIDTH = 10.dp
private val DOT_SIZE = 6

/** Asks the store for the nights each time the screen resumes, the first time included. */
@Composable
fun RefreshNightsOnResume(store: NightStore) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, store) { lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { store.refresh() } }
}

/** Asks the store for the nights once, when the screen opens. */
@Composable
fun RefreshNightsOnOpen(store: NightStore) {
    LaunchedEffect(store) { store.refresh() }
}

/**
 * The nights from Health Connect: the average and the shortest of the last 14 nights, a bar per night with the
 * sessions of that day under it, and the night before each kind of session in the last 8 weeks. Facts only.
 */
@Composable
fun NightsScreen(vm: AppViewModel, nav: NavHostController, clock: Clock = Clock.systemDefaultZone(), hcStatus: (() -> HcStatus)? = null) {
    val context = LocalContext.current
    val statusOf = hcStatus ?: { HealthImport.status(context.applicationContext) }
    val store = vm.nightStore
    val state by vm.nights.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    val today = rememberToday(clock)
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(statusOf()) }
    RefreshNightsOnResume(store)
    val permLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        scope.launch { store.refreshAfterChange() }
    }
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize()) {
        EditorTopBar("Back", "Nights", onBack = { nav.popBackStack() }, scrollPx = { scroll.value })
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            val s = state
            when {
                status != HcStatus.AVAILABLE || s is NightsState.NoAccess -> HealthIntro(
                    status, granted = false,
                    onStore = { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                    onAllow = { permLauncher.launch(setOf(HealthSleepSource.SLEEP_READ)) },
                    onCheckAgain = { status = statusOf(); scope.launch { store.refresh(force = true) } },
                    allowTitle = "Read sleep",
                    allowLine = NIGHTS_ALLOW_LINE,
                )
                s is NightsState.Failed -> Column(
                    Modifier.fillMaxWidth().clip(GroupShape).background(K.Surface).padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(NIGHTS_FAILED, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    PrimaryButton("Try again") { scope.launch { store.refresh(force = true) } }
                }
                s is NightsState.Loaded && s.nights.isEmpty() -> GroupedList { EmptyLine(NIGHTS_EMPTY) }
                s is NightsState.Loaded -> NightsLoaded(s.nights, sessions, exercises, modalities, today)
            }
        }
    }
}

@Composable
private fun NightsLoaded(nights: Map<LocalDate, Night>, sessions: List<Session>, exercises: List<Exercise>, modalities: List<Modality>, today: LocalDate) {
    val last = remember(nights, today) { lastNights(nights, today) }
    val before = remember(sessions, nights, today) { nightBeforeSessions(sessions, nights, today) }
    GroupedList {
        Row(Modifier.fillMaxWidth().padding(16.dp, 14.dp, 16.dp, 4.dp)) {
            Figure(last.averageSec, nightsAverageCaption(last.count), Alignment.Start, Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Figure(last.shortest?.asleepSec, nightsShortestCaption(last.shortest), Alignment.End, Modifier.weight(1f))
        }
        NightBars(last.days, today, sessions, exercises, modalities, Modifier.padding(12.dp, 8.dp, 12.dp, 12.dp))
    }
    if (before.isNotEmpty()) {
        SectionTitle(NIGHT_BEFORE_TITLE, Modifier.padding(top = 24.dp))
        GroupedList { before.forEachIndexed { i, t -> TitleNightsRow(t, divider = i > 0) } }
    }
}

@Composable
private fun Figure(sec: Int?, caption: String, align: Alignment.Horizontal, modifier: Modifier) {
    val text = sec?.let(::nightLength) ?: NO_VALUE
    val spoken = sec?.let(::nightLengthSpoken) ?: "none"
    val end = align == Alignment.End
    Column(modifier.clearAndSetSemantics { contentDescription = "$caption, $spoken" }, horizontalAlignment = align) {
        Text(text, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR))
        Text(caption, style = MaterialTheme.typography.bodySmall, color = K.Muted, textAlign = if (end) TextAlign.End else TextAlign.Start)
    }
}

/**
 * The 14 days as bars of asleep time on one color, the value axis from 0 with a dashed guide line every 4 hours
 * drawn as [LineChart] draws its grid; under each bar its weekday letter (today in accent) and a dot per finished
 * session of that day, two at most. Each day is one TalkBack element.
 */
@Composable
private fun NightBars(days: List<Pair<LocalDate, Night?>>, today: LocalDate, sessions: List<Session>, exercises: List<Exercise>, modalities: List<Modality>, modifier: Modifier) {
    val top = nightAxisHours(days.mapNotNull { it.second?.asleepSec }.maxOrNull())
    val ticks = nightAxisTicks(top)
    val measurer = rememberTextMeasurer()
    val tickStyle = TextStyle(fontFamily = Inter, color = K.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val labels = ticks.map { measurer.measure(nightAxisLabel(it), tickStyle) }
    val axis = with(LocalDensity.current) { axisWidth(labels.map { it.size.width.toFloat() }, AXIS_GAP.toPx()).toDp() }
    Box(modifier.fillMaxWidth().testTag(NIGHTS_CHART_TAG)) {
        Canvas(Modifier.matchParentSize()) {
            val plotH = PLOT_HEIGHT.toPx()
            val y0 = PLOT_TOP.toPx()
            ticks.forEachIndexed { k, h ->
                val y = y0 + plotH * (1f - h.toFloat() / top)
                drawLine(K.Divider, Offset(axis.toPx(), y), Offset(size.width, y), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
                val t = labels[k]
                drawText(t, topLeft = Offset(axis.toPx() - AXIS_GAP.toPx() - t.size.width, y - t.size.height / 2f))
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = axis, top = PLOT_TOP)) {
            days.forEach { (day, night) ->
                val done = finishedOn(day, sessions)
                Column(
                    Modifier.weight(1f).testTag(nightBarTag(day)).semantics(mergeDescendants = true) { contentDescription = nightBarSpoken(day, night, done.map(::sessionName)) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.fillMaxWidth().height(PLOT_HEIGHT), contentAlignment = Alignment.BottomCenter) {
                        if (night != null) {
                            val h = (PLOT_HEIGHT * (night.asleepSec / (top * 3600f))).coerceIn(2.dp, PLOT_HEIGHT)
                            Box(Modifier.width(BAR_WIDTH).height(h).testTag(nightFillTag(day)).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(K.Night))
                        }
                    }
                    Text(
                        weekdayLetter(day), style = tickStyle, color = if (day == today) K.Accent else K.Muted,
                        fontWeight = if (day == today) FontWeight.Bold else FontWeight.Medium, maxLines = 1, softWrap = false,
                        modifier = Modifier.padding(top = 6.dp).clearAndSetSemantics {},
                    )
                    Row(Modifier.padding(top = 4.dp).height(DOT_SIZE.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        done.take(NIGHT_DOTS).forEach { Box(Modifier.testTag(nightDotTag(day))) { Dot(sessionColor(it, exercises, modalities).first, true, size = DOT_SIZE) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun TitleNightsRow(t: TitleNights, divider: Boolean) {
    Column(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "${t.title}, ${nightCount(t.count)}, ${nightLengthSpoken(t.averageSec)}" }) {
        if (divider) HorizontalDivider(Modifier.padding(start = 16.dp), color = K.Divider, thickness = 0.7.dp)
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(t.title, style = MaterialTheme.typography.bodyLarge, maxLines = 3, modifier = Modifier.weight(1f))
            Column(Modifier.padding(start = 12.dp), horizontalAlignment = Alignment.End) {
                Text(nightLength(t.averageSec), style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR), maxLines = 1, softWrap = false)
                Text(nightCount(t.count), style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR), color = K.Muted, maxLines = 1, softWrap = false)
            }
        }
    }
}
