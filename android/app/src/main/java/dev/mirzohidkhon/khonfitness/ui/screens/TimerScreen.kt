package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.BuildConfig
import dev.mirzohidkhon.khonfitness.timer.PhaseKind
import dev.mirzohidkhon.khonfitness.timer.TimerService
import dev.mirzohidkhon.khonfitness.timer.mmss
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K
import kotlinx.coroutines.flow.first

/** Full-screen interval countdown. The service owns the clock; this screen only shows it. */
@Composable
fun TimerScreen(vm: AppViewModel, nav: NavHostController, sessionId: String) {
    val context = LocalContext.current
    val state by TimerService.state.collectAsStateWithLifecycle()
    val finished by TimerService.finished.collectAsStateWithLifecycle()
    val session by vm.repo.session(sessionId).collectAsStateWithLifecycle(null)
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val view = LocalView.current
    DisposableEffect(Unit) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }

    val s = session
    val ex = exercises.find { it.id == s?.exerciseId }
    // Start the service once for this session if it is not running.
    LaunchedEffect(s?.id, ex?.id) {
        if (s != null && ex != null && state?.sessionId != s.id) TimerService.start(context, s.id, ex.name, ex.warmupSec, ex.workSec, ex.restSec, ex.rounds)
    }
    val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
    var batteryOk by remember { mutableStateOf(pm.isIgnoringBatteryOptimizations(context.packageName)) }
    val batteryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { batteryOk = pm.isIgnoringBatteryOptimizations(context.packageName) }
    // When the service reports the end, store elapsed time and open the session form.
    LaunchedEffect(finished) {
        val f = finished ?: return@LaunchedEffect
        if (f.sessionId == sessionId) {
            TimerService.finished.value = null
            val current = vm.repo.session(sessionId).first()
            if (current != null) vm.repo.updateSession(current.copy(timeSec = f.elapsedSec))
            nav.navigate(Routes.cardio(sessionId)) { popUpTo(Routes.TODAY) }
        }
    }

    val st = state
    val kind = st?.phase?.kind ?: PhaseKind.WARMUP
    val bg by animateColorAsState(when { st == null || st.done -> K.Bg; kind == PhaseKind.WORK -> Color(0xFF14532D); kind == PhaseKind.REST -> Color(0xFF1E3A5F); else -> Color(0xFF3A3A40) }, label = "phase")
    Column(Modifier.fillMaxSize().background(bg).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton("Stop", color = Color.White) { TimerService.send(context, TimerService.ACTION_STOP) }
            Text(st?.title ?: ex?.name ?: "", color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            TextButton(if (st?.paused == true) "Resume" else "Pause", color = Color.White) { TimerService.send(context, if (st?.paused == true) TimerService.ACTION_RESUME else TimerService.ACTION_PAUSE) }
        }
        Spacer(Modifier.weight(1f))
        val label = when { st == null -> "Starting"; st.done -> "Done"; kind == PhaseKind.WARMUP -> "Warm-up"; kind == PhaseKind.WORK -> "Work"; else -> "Rest" }
        Text(label.uppercase(), color = Color.White.copy(alpha = .75f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 4.sp)
        Text(mmss(st?.remaining ?: 0.0), color = Color.White, fontSize = 136.sp, fontWeight = FontWeight.Bold, letterSpacing = (-5).sp, lineHeight = 136.sp, modifier = Modifier.padding(vertical = 8.dp))
        if (st != null && !st.done && kind != PhaseKind.WARMUP) Text("Round ${st.phase.round} of ${st.rounds}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        if (st != null && !st.done) {
            val next = st.phases.getOrNull(st.index + 1)
            Text(if (next == null) "Last one" else "Next: " + (when (next.kind) { PhaseKind.WORK -> "work"; PhaseKind.REST -> "rest"; else -> "warm-up" }) + " " + mmss(next.seconds.toDouble()), color = Color.White.copy(alpha = .75f), fontSize = 17.sp, modifier = Modifier.padding(top = 10.dp))
            Text("Elapsed ${mmss(st.elapsed)} of ${mmss(st.total.toDouble())}", color = Color.White.copy(alpha = .6f), fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.weight(1f))
        if (!batteryOk) {
            Text("Samsung may pause the timer when the screen is off. Allow it to run in the background once.", color = Color.White.copy(alpha = .8f), fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(bottom = 10.dp))
            TextButton("Allow background", color = Color.White) { batteryLauncher.launch(android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, android.net.Uri.parse("package:" + context.packageName))) }
            Spacer(Modifier.height(12.dp))
        }
        if (st?.paused == true) Text("Paused", color = Color.White, fontSize = 17.sp, modifier = Modifier.padding(bottom = 24.dp))
        if (BuildConfig.DEBUG && st != null) TextButton(if (st.speed > 1) "×60 on" else "×60", color = Color.White) { TimerService.state.value = st.copy(speed = if (st.speed > 1) 1.0 else 60.0) }
        Spacer(Modifier.height(32.dp))
    }
}
