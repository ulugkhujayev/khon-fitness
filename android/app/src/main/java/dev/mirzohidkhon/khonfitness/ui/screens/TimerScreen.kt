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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.delay
import dev.mirzohidkhon.khonfitness.ui.LeaveIfSessionMissing
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.map

/** Under the timer while the phone may stop the app in the background. Any phone maker can, so it names none. */
const val BATTERY_NOTE = "If the timer stops when the screen is off, allow Khon to run in the background in battery settings."

/** The app's own settings page, where its battery use is set (Battery, then Unrestricted on most phones). */
fun appSettingsIntent(packageName: String) =
    android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", packageName, null))

/** Full-screen interval countdown. The service owns the clock; this screen only shows it. */
@Composable
fun TimerScreen(vm: AppViewModel, nav: NavHostController, sessionId: String) {
    val context = LocalContext.current
    val state by TimerService.state.collectAsStateWithLifecycle()
    val finished by TimerService.finished.collectAsStateWithLifecycle()
    val sessionState by remember(sessionId) { vm.repo.session(sessionId).map { it to true } }.collectAsStateWithLifecycle(null to false)
    val session = sessionState.first
    LeaveIfSessionMissing(nav, "timer/{id}", sessionId, loaded = sessionState.second, missing = session == null)
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
    // The service stores the duration and then posts [finished]; this screen only leaves.
    // Clear the flag after navigating: clearing first changes this effect's key and cancels it mid-way.
    LaunchedEffect(finished) {
        val f = finished ?: return@LaunchedEffect
        if (f.sessionId == sessionId) {
            nav.navigate(Routes.cardio(sessionId)) { popUpTo(Routes.TODAY) }
            TimerService.finished.value = null
        }
    }
    // Safety net: the timer ran here and the service is gone without a report (process death, missed flow). Leave anyway.
    var sawRunning by remember { mutableStateOf(false) }
    LaunchedEffect(state?.sessionId) { if (state?.sessionId == sessionId) sawRunning = true }
    LaunchedEffect(sawRunning, state == null) {
        if (sawRunning && state == null) { delay(1500); if (TimerService.state.value == null) nav.navigate(Routes.cardio(sessionId)) { popUpTo(Routes.TODAY) } }
    }

    val st = state
    val kind = st?.phase?.kind ?: PhaseKind.WARMUP
    val bg by animateColorAsState(when { st == null || st.done || st.awaitingStart || st.preparing || st.paused -> K.Bg; kind == PhaseKind.WORK -> K.WorkFill; kind == PhaseKind.REST -> K.RestFill; else -> K.WarmupFill }, label = "phase")
    Column(Modifier.fillMaxSize().background(bg).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Equal 64 dp sides keep the title centered.
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(64.dp), contentAlignment = Alignment.CenterStart) { TextButton("Stop", color = Color.White) { TimerService.send(context, TimerService.ACTION_STOP) } }
            Text(
                st?.title ?: ex?.name ?: "", color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).semantics { heading() }, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.width(64.dp))
        }
        Spacer(Modifier.weight(1f))
        val label = when { st == null -> "Starting"; st.done -> "Done"; st.awaitingStart -> "Ready"; st.preparing -> "Get ready"; st.paused -> "Paused"; kind == PhaseKind.WARMUP -> "Warm-up"; kind == PhaseKind.WORK -> "Work"; else -> "Rest" }
        // The phase word is spoken when it changes; the countdown is not, as it changes every second.
        Text(label, color = K.OnPhaseMuted, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        Text(if (st?.preparing == true) kotlin.math.ceil(st.preparation).toInt().toString() else mmss(st?.remaining ?: 0.0), color = Color.White, fontSize = 96.sp, fontWeight = FontWeight.Bold, letterSpacing = (-5).sp, lineHeight = 112.sp, style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR), modifier = Modifier.padding(vertical = 8.dp))
        if (st != null && !st.done && kind != PhaseKind.WARMUP) Text("Round ${st.phase.round} of ${st.rounds}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp)
        if (st != null && !st.done) {
            val next = st.phases.getOrNull(st.index + 1)
            Text(if (next == null) "Last one" else "Next: " + (when (next.kind) { PhaseKind.WORK -> "work"; PhaseKind.REST -> "rest"; else -> "warm-up" }) + " " + mmss(next.seconds.toDouble()), color = K.OnPhaseMuted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 10.dp))
            Text("Elapsed ${mmss(st.elapsed)} of ${mmss(st.total.toDouble())}", color = K.OnPhaseMuted, style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TABULAR), modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.weight(1f))
        if (!batteryOk) {
            Text(BATTERY_NOTE, color = K.OnPhaseMuted, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 10.dp))
            TextButton("Battery settings", color = Color.White) {
                runCatching { batteryLauncher.launch(appSettingsIntent(context.packageName)) }
                    .onFailure { batteryLauncher.launch(android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (st != null && !st.done) PrimaryButton(when { st.awaitingStart -> "Start timer"; st.paused -> "Resume"; else -> "Pause" }) {
            TimerService.send(context, when { st.awaitingStart -> TimerService.ACTION_BEGIN; st.paused -> TimerService.ACTION_RESUME; else -> TimerService.ACTION_PAUSE })
        }
        if (BuildConfig.DEBUG && st != null) TextButton(if (st.speed > 1) "×60 on" else "×60", color = Color.White) { TimerService.state.value = st.copy(speed = if (st.speed > 1) 1.0 else 60.0) }
        Spacer(Modifier.height(32.dp))
    }
}
