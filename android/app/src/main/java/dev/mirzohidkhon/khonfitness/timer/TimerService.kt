package dev.mirzohidkhon.khonfitness.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import dev.mirzohidkhon.khonfitness.MainActivity
import dev.mirzohidkhon.khonfitness.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class PhaseKind { WARMUP, WORK, REST, DONE }
data class Phase(val kind: PhaseKind, val seconds: Int, val round: Int)

data class TimerState(
    val sessionId: String,
    val title: String,
    val phases: List<Phase>,
    val index: Int = 0,
    val remaining: Double = 0.0,
    val elapsed: Double = 0.0,
    val paused: Boolean = false,
    val speed: Double = 1.0,
) {
    val phase: Phase get() = phases[index.coerceIn(0, phases.size - 1)]
    val done: Boolean get() = index >= phases.size
    val rounds: Int get() = phases.count { it.kind == PhaseKind.WORK }
    val total: Int get() = phases.sumOf { it.seconds }
}

/** Builds the phase list from an interval exercise: warmup, then work/rest per round, no trailing rest. */
fun buildPhases(warmupSec: Int, workSec: Int, restSec: Int, rounds: Int): List<Phase> {
    val out = mutableListOf<Phase>()
    if (warmupSec > 0) out += Phase(PhaseKind.WARMUP, warmupSec, 0)
    for (r in 1..rounds) {
        out += Phase(PhaseKind.WORK, workSec, r)
        if (r < rounds && restSec > 0) out += Phase(PhaseKind.REST, restSec, r)
    }
    return out
}

/** Foreground service that owns the countdown. UI observes [TimerService.state]. */
class TimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null
    private var tone: ToneGenerator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val phases = buildPhases(intent.getIntExtra("warmup", 0), intent.getIntExtra("work", 240), intent.getIntExtra("rest", 180), intent.getIntExtra("rounds", 4))
                val s = TimerState(intent.getStringExtra("sessionId") ?: "", intent.getStringExtra("title") ?: "Timer", phases, remaining = phases.firstOrNull()?.seconds?.toDouble() ?: 0.0, speed = intent.getDoubleExtra("speed", 1.0))
                state.value = s
                startInForeground(s)
                runLoop()
            }
            ACTION_PAUSE -> state.value = state.value?.copy(paused = true)
            ACTION_RESUME -> state.value = state.value?.copy(paused = false)
            ACTION_STOP -> finish(early = true)
        }
        return START_NOT_STICKY
    }

    private fun runLoop() {
        loop?.cancel()
        tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }.getOrNull()
        loop = scope.launch {
            var last = System.nanoTime()
            var lastWhole = -1
            while (true) {
                delay(100)
                val now = System.nanoTime()
                val dt = (now - last) / 1e9; last = now
                val s = state.value ?: break
                if (s.done) break
                if (s.paused) continue
                var remaining = s.remaining - dt * s.speed
                var index = s.index
                val elapsed = s.elapsed + dt * s.speed
                val whole = kotlin.math.ceil(remaining).toInt()
                if (whole in 1..3 && whole != lastWhole) beep(short = true)
                lastWhole = whole
                if (remaining <= 0) {
                    index += 1
                    if (index >= s.phases.size) { state.value = s.copy(index = index, remaining = 0.0, elapsed = elapsed); beep(short = false); finish(early = false); break }
                    remaining += s.phases[index].seconds
                    vibrate(400)
                }
                state.value = s.copy(index = index, remaining = remaining, elapsed = elapsed)
                if (whole != lastNotified) { lastNotified = whole; updateNotification(state.value!!) }
            }
        }
    }
    private var lastNotified = -2

    private fun finish(early: Boolean) {
        loop?.cancel()
        val s = state.value
        if (s != null) {
            finished.value = FinishedTimer(s.sessionId, s.elapsed.toInt(), early)
            state.value = if (early) null else s.copy(index = s.phases.size)
        }
        tone?.release(); tone = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        if (!early) state.value = null
    }

    private fun beep(short: Boolean) {
        runCatching { tone?.startTone(if (short) ToneGenerator.TONE_PROP_BEEP else ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, if (short) 150 else 900) }
        vibrate(if (short) 60 else 500)
    }

    private fun vibrate(ms: Long) {
        val v = if (Build.VERSION.SDK_INT >= 31) (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator else @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        runCatching { v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)) }
    }

    private fun startInForeground(s: TimerState) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Interval timer", NotificationManager.IMPORTANCE_LOW).apply { setSound(null, null) })
        val n = notification(s)
        if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(NOTIFICATION_ID, n)
    }

    private fun updateNotification(s: TimerState) {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, notification(s))
    }

    private fun notification(s: TimerState): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }, PendingIntent.FLAG_IMMUTABLE)
        fun action(a: String) = PendingIntent.getService(this, a.hashCode(), Intent(this, TimerService::class.java).setAction(a), PendingIntent.FLAG_IMMUTABLE)
        val phase = if (s.done) "Done" else when (s.phase.kind) { PhaseKind.WARMUP -> "Warm-up"; PhaseKind.WORK -> "Work ${s.phase.round}/${s.rounds}"; PhaseKind.REST -> "Rest ${s.phase.round}/${s.rounds}"; PhaseKind.DONE -> "Done" }
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(s.title)
            .setContentText("$phase · ${mmss(s.remaining)}")
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setContentIntent(open)
            .addAction(0, if (s.paused) "Resume" else "Pause", action(if (s.paused) ACTION_RESUME else ACTION_PAUSE))
            .addAction(0, "Stop", action(ACTION_STOP))
            .build()
    }

    override fun onDestroy() { loop?.cancel(); tone?.release(); super.onDestroy() }

    companion object {
        const val CHANNEL = "timer"; const val NOTIFICATION_ID = 41
        const val ACTION_START = "start"; const val ACTION_PAUSE = "pause"; const val ACTION_RESUME = "resume"; const val ACTION_STOP = "stop"
        val state = MutableStateFlow<TimerState?>(null)
        val finished = MutableStateFlow<FinishedTimer?>(null)

        fun start(context: Context, sessionId: String, title: String, warmup: Int, work: Int, rest: Int, rounds: Int, speed: Double = 1.0) {
            val i = Intent(context, TimerService::class.java).setAction(ACTION_START)
                .putExtra("sessionId", sessionId).putExtra("title", title).putExtra("warmup", warmup).putExtra("work", work).putExtra("rest", rest).putExtra("rounds", rounds).putExtra("speed", speed)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
        }
        fun send(context: Context, action: String) { context.startService(Intent(context, TimerService::class.java).setAction(action)) }
    }
}

data class FinishedTimer(val sessionId: String, val elapsedSec: Int, val early: Boolean)

fun mmss(seconds: Double): String { val s = kotlin.math.ceil(seconds).toInt().coerceAtLeast(0); return "%d:%02d".format(s / 60, s % 60) }
