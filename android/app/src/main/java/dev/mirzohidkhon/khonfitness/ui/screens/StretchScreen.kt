package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
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
        val steps = routineSteps(r.id, routineStretches, stretches)
        if (running == null && steps.isNotEmpty()) TimerService.startStretch(context, sessionId, r.name, steps)
    }
    var observedSessionId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state?.sessionId) {
        state?.takeIf { it.stretch && it.sessionId.startsWith("$routineId:") }?.let { observedSessionId = it.sessionId }
    }
    LaunchedEffect(finished) {
        val f = finished ?: return@LaunchedEffect
        if (f.sessionId == sessionId || f.sessionId == observedSessionId) { nav.popBackStack(); TimerService.finished.value = null }
    }
    var sawRunning by remember { mutableStateOf(false) }
    LaunchedEffect(state?.sessionId) { if (state?.sessionId?.startsWith("$routineId:") == true) sawRunning = true }
    LaunchedEffect(sawRunning, state == null) { if (sawRunning && state == null) { delay(1500); if (TimerService.state.value == null) nav.popBackStack() } }

    val st = state?.takeIf { it.stretch && it.sessionId.startsWith("$routineId:") }
    val phase = st?.takeUnless { it.done }?.phase
    val bg by animateColorAsState(if (st?.active == true) Color(0xFF14532D) else K.Bg, label = "bg")
    BoxWithConstraints(Modifier.fillMaxSize().background(bg)) {
        val compact = maxHeight < 680.dp
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                PlayerTextButton("Stop", Modifier.width(64.dp)) {
                    if (st != null) TimerService.send(context, TimerService.ACTION_STOP) else nav.popBackStack()
                }
                Text(st?.title ?: routine?.name ?: "Stretching", color = Color.White,
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                PlayerTextButton("Skip", Modifier.width(64.dp), enabled = phase != null) { TimerService.send(context, TimerService.ACTION_SKIP) }
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (phase != null) {
                    Text(phase.label, color = Color.White, style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text(listOfNotNull(phase.side.takeIf { it.isNotEmpty() },
                        if (phase.reps > 0) "${phase.reps} reps" else "Hold").joinToString(" · "),
                        color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 8.dp))
                    val motion = if (phase.reps > 0) {
                        val cycle = ((phase.seconds - st.remaining) / (phase.seconds.toDouble() / phase.reps)) % 1.0
                        (if (cycle < .5) cycle * 2 else (1 - cycle) * 2).toFloat()
                    } else 1f
                    Figure(phase.figure, Modifier.size(if (compact) 144.dp else 224.dp).padding(vertical = 8.dp),
                        mirror = phase.side == "Right", animate = false, progress = motion)
                    val cue = Figures.cue(phase.figure)
                    if (cue.isNotEmpty()) Text(cue, color = Color.White.copy(alpha = .8f),
                        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = if (compact) 8.dp else 16.dp))
                    Text(when { st.paused -> "Paused"; st.awaitingStart -> if (st.index == 0) "Ready" else "Next stretch";
                        st.preparing -> "Get ready"; else -> if (phase.reps > 0) "Move" else "Hold" },
                        color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    Text(if (st.preparing) kotlin.math.ceil(st.preparation).toInt().toString() else mmss(st.remaining),
                        color = Color.White, style = TextStyle(fontFamily = MaterialTheme.typography.headlineLarge.fontFamily,
                            fontSize = if (compact) 72.sp else 88.sp, fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum", textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    Text("${st.index + 1} of ${st.phases.size} · ${mmss(st.workRemaining)} remaining",
                        color = Color.White.copy(alpha = .65f), style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                } else {
                    Text(if (routine == null) "Loading routine" else "No stretches to play", color = Color.White)
                }
            }
            if (st != null && phase != null) {
                val action = when { st.awaitingStart -> TimerService.ACTION_BEGIN; st.paused -> TimerService.ACTION_RESUME; else -> TimerService.ACTION_PAUSE }
                val label = when {
                    st.awaitingStart && phase.side.isNotEmpty() -> "Start ${phase.side.lowercase()} side"
                    st.awaitingStart -> if (st.index == 0) "Start stretch" else "Start next stretch"
                    st.paused -> "Resume"
                    else -> "Pause"
                }
                Button(onClick = { TimerService.send(context, action) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)) {
                    Text(label, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                }
                if (BuildConfig.DEBUG) PlayerTextButton(if (st.speed > 1) "×20 on" else "×20", Modifier.fillMaxWidth()) {
                    TimerService.state.value = TimerService.state.value?.let { it.copy(speed = if (it.speed > 1) 1.0 else 20.0) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PlayerTextButton(text: String, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick, enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
