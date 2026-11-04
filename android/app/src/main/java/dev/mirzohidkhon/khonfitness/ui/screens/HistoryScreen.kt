package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Icons
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * History: the title row names the two pages as tabs, Log (a week of sessions against the plan) and Progress (records,
 * sets per muscle, bodyweight, cardio), with the import button at its end. Log opens first. Each page keeps its own scroll position and state (the week shown, the
 * bodyweight range), also across page and tab switches: the page, both scroll states and each page's saved state
 * are saved with the tab. Today's date is read again on resume and at midnight.
 */
@Composable
fun HistoryScreen(vm: AppViewModel, nav: NavHostController, clock: Clock = Clock.systemDefaultZone()) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val logScroll = rememberScrollState()
    val progressScroll = rememberScrollState()
    val pages = rememberSaveableStateHolder()
    val today = rememberToday(clock)
    TabScreen(
        trailing = { BarCircleButton(Icons.Download, "Import workouts") { nav.navigate(Routes.IMPORT) } },
        scroll = if (page == 0) logScroll else progressScroll,
        title = { scale -> TitleTabs(listOf("Log", "Progress"), page, "History", scale) { page = it } },
    ) {
        pages.SaveableStateProvider(page) { if (page == 0) HistoryLog(vm, nav, today) else HistoryProgress(vm, nav, today) }
    }
}

/** Time from [now] to the next midnight, plus a second so the new date has begun. */
fun untilNextDay(now: LocalDateTime): Duration = Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).plusSeconds(1)

/** Today's date from [clock], read again each time the screen resumes and at each midnight while it is shown. */
@Composable
fun rememberToday(clock: Clock): LocalDate {
    var today by remember { mutableStateOf(LocalDate.now(clock)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, clock) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                today = LocalDate.now(clock)
                delay(untilNextDay(LocalDateTime.now(clock)).toMillis())
            }
        }
    }
    return today
}

/** The records every History screen reads: the app's sessions, sets, stretch runs and bodyweights up to [today]. */
@Composable
fun rememberHistoryData(vm: AppViewModel, today: LocalDate): HistoryData {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val logs by vm.allSetLogs.collectAsStateWithLifecycle()
    val stretches by vm.stretchSessions.collectAsStateWithLifecycle()
    val bodyweights by vm.bodyweights.collectAsStateWithLifecycle()
    return remember(today, sessions, logs, stretches, bodyweights) { historyData(today, sessions, logs, stretches, bodyweights) }
}

fun sessionColor(s: Session, exercises: List<Exercise>, modalities: List<Modality>): Pair<Color, Boolean> =
    if (s.itemType == ItemType.PROGRAM) K.Accent to true
    else exercises.find { it.id == s.exerciseId }?.let { ex -> exerciseColor(ex, modalities) to (ex.intensity == Intensity.HIGH) } ?: (K.Muted to true)

data class MuscleVolume(val muscle: String, val thisWeek: Double, val lastWeek: Double)

/** Sets per muscle group for the current week up to [today] and the one before. Primary muscle counts 1, each secondary 0.5. */
fun muscleVolume(logs: List<SetLog>, sessions: List<Session>, exercises: List<Exercise>, today: LocalDate): List<MuscleVolume> {
    val monday = today.with(DayOfWeek.MONDAY); val lastMonday = monday.minusWeeks(1)
    val dates = sessions.filter { it.finished }.associate { it.id to it.date.toDate() }
    val exById = exercises.associateBy { it.id }
    val acc = HashMap<String, DoubleArray>()
    for (l in logs) {
        if (l.status != SetStatus.DONE) continue
        val d = dates[l.sessionId] ?: continue
        val idx = when { d in monday..today -> 0; d in lastMonday..<monday -> 1; else -> continue }
        val ex = exById[l.exerciseId] ?: continue
        ex.primaryMuscle?.let { acc.getOrPut(it) { DoubleArray(2) }[idx] += 1.0 }
        ex.secondaryList.forEach { acc.getOrPut(it) { DoubleArray(2) }[idx] += 0.5 }
    }
    return acc.map { (m, v) -> MuscleVolume(m, v[0], v[1]) }.sortedByDescending { maxOf(it.thisWeek, it.lastWeek) }
}

/** The shortest bar a week with sets gets, so a small count still shows. */
private const val MIN_SHARE = 0.02

/** Where a muscle's bar ends, as shares of the track: this week's fill and the mark for last week. */
data class VolumeShare(val now: Double, val last: Double)

/**
 * Where the sets-per-muscle bars end, as shares of the biggest count of either week: this week's fill and the mark
 * for last week. Zero stays zero, so nothing shows for a week without sets.
 */
fun volumeShares(volume: List<MuscleVolume>): List<VolumeShare> {
    val max = maxOf(1.0, volume.maxOfOrNull { maxOf(it.thisWeek, it.lastWeek) } ?: 0.0)
    fun share(v: Double) = if (v <= 0) 0.0 else maxOf(MIN_SHARE, v / max)
    return volume.map { VolumeShare(share(it.thisWeek), share(it.lastWeek)) }
}

/** Opens a session from History: a gym session's page, a running timer, else the cardio form (Edit stays there). */
fun openSession(nav: NavHostController, s: Session) = nav.navigate(when {
    s.itemType == ItemType.PROGRAM -> Routes.session(s.id)
    !s.finished && dev.mirzohidkhon.khonfitness.timer.TimerService.state.value?.sessionId == s.id -> Routes.timer(s.id)
    else -> Routes.cardio(s.id)
})

/** A small rounded label: "New", "PR". */
@androidx.compose.runtime.Composable
fun LabelChip(text: String, color: Color = K.Green) {
    androidx.compose.material3.Text(
        text, color = color, fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.18f)).padding(horizontal = 7.dp, vertical = 2.dp),
    )
}
