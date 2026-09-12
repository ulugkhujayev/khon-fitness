package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.BuildConfig
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.timer.TimerService
import dev.mirzohidkhon.khonfitness.timer.mmss
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K
import kotlinx.coroutines.delay

/** Builds the player steps for a routine: sided stretches become two phases. Returns "label|side|seconds|figure|reps|stretchId" lines. */
fun routineSteps(routineId: String, routineStretches: List<RoutineStretch>, stretches: List<Stretch>): List<String> =
    routineStretches.filter { it.routineId == routineId }.sortedBy { it.sortOrder }.flatMap { rs ->
        val st = stretches.find { it.id == rs.stretchId } ?: return@flatMap emptyList()
        val sec = rs.seconds ?: st.seconds
        val reps = if (st.mode == StretchMode.REPS) st.reps else 0
        if (st.sided) listOf("Left", "Right").map { side -> "${st.name}|$side|$sec|${st.figure}|$reps|${st.id}" } else listOf("${st.name}||$sec|${st.figure}|$reps|${st.id}")
    }

fun routineSeconds(routineId: String, routineStretches: List<RoutineStretch>, stretches: List<Stretch>): Int =
    routineSteps(routineId, routineStretches, stretches).sumOf { it.split("|")[2].toInt() }

/** Full-screen stretch player. The service owns the clock and writes the session; this screen shows the figure and leaves at the end. */
@Composable
fun StretchScreen(vm: AppViewModel, nav: NavHostController, routineId: String) {
    val context = LocalContext.current
    val state by TimerService.state.collectAsStateWithLifecycle()
    val finished by TimerService.finished.collectAsStateWithLifecycle()
    val routines by vm.stretchRoutines.collectAsStateWithLifecycle()
    val routineStretches by vm.routineStretches.collectAsStateWithLifecycle()
    val stretches by vm.stretches.collectAsStateWithLifecycle()
    val view = LocalView.current
    DisposableEffect(Unit) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }
    val routine = routines.find { it.id == routineId }
    val sessionId = remember { "$routineId:" + newId() }

    LaunchedEffect(routine?.id, routineStretches.size, stretches.size) {
        val r = routine ?: return@LaunchedEffect
        if (stretches.isEmpty()) return@LaunchedEffect
        val running = TimerService.state.value
        if (running == null || !running.stretch || !running.sessionId.startsWith("$routineId:")) TimerService.startStretch(context, sessionId, r.name, routineSteps(r.id, routineStretches, stretches))
    }
    LaunchedEffect(finished) {
        val f = finished ?: return@LaunchedEffect
        if (f.sessionId.startsWith("$routineId:")) { nav.popBackStack(); TimerService.finished.value = null }
    }
    var sawRunning by remember { mutableStateOf(false) }
    LaunchedEffect(state?.sessionId) { if (state?.sessionId?.startsWith("$routineId:") == true) sawRunning = true }
    LaunchedEffect(sawRunning, state == null) { if (sawRunning && state == null) { delay(1500); if (TimerService.state.value == null) nav.popBackStack() } }

    val st = state?.takeIf { it.stretch }
    val phase = st?.phase
    val bg by animateColorAsState(if (st == null || st.done) K.Bg else if (st.paused) Color(0xFF3A3A40) else Color(0xFF14532D), label = "bg")
    Column(Modifier.fillMaxSize().background(bg).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton("Stop", color = Color.White) { TimerService.send(context, TimerService.ACTION_STOP) }
            Text(st?.title ?: routine?.name ?: "", color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            TextButton("Skip", color = Color.White) { TimerService.send(context, TimerService.ACTION_SKIP) }
        }
        Spacer(Modifier.weight(1f))
        Figure(phase?.figure ?: "hipflexor", Modifier.size(240.dp), mirror = phase?.side == "Right", animate = st?.paused != true)
        Text(phase?.label ?: "Starting", color = Color.White, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Text(when { phase == null -> ""; phase.reps > 0 -> "${phase.reps} reps" + (if (phase.side.isNotEmpty()) " · " + phase.side else ""); phase.side.isNotEmpty() -> phase.side; else -> "Hold" },
            color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 2.dp))
        Text(mmss(st?.remaining ?: 0.0), color = Color.White, fontSize = 96.sp, fontWeight = FontWeight.Bold, letterSpacing = (-3).sp, lineHeight = 96.sp, modifier = Modifier.padding(top = 6.dp))
        if (st != null && !st.done) {
            val next = st.phases.getOrNull(st.index + 1)
            Text(if (next == null) "Last one" else "Next: " + next.label + (if (next.side.isNotEmpty()) ", " + next.side.lowercase() else ""), color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
            Text("${st.index + 1} of ${st.phases.size} · ${mmss(st.total - st.elapsed)} left", color = Color.White.copy(alpha = .6f), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.weight(1f))
        if (st?.paused == true) Text("Paused", color = Color.White, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 12.dp))
        TextButton(if (st?.paused == true) "Resume" else "Pause", color = Color.White) { TimerService.send(context, if (st?.paused == true) TimerService.ACTION_RESUME else TimerService.ACTION_PAUSE) }
        if (BuildConfig.DEBUG && st != null) TextButton(if (st.speed > 1) "×20 on" else "×20", color = Color.White) { TimerService.state.value = st.copy(speed = if (st.speed > 1) 1.0 else 20.0) }
        Spacer(Modifier.height(32.dp))
    }
}
