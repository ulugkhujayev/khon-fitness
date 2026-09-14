package dev.mirzohidkhon.khonfitness.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import java.util.Locale
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.MainActivity
import dev.mirzohidkhon.khonfitness.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Foreground service that owns the countdown. UI observes [TimerService.state]. */
class TimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loop: Job? = null
    private var tone: ToneGenerator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var lastTickNanos = 0L
    private var finishing = false
    private var teardown: Job? = null
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                silence()
                state.value?.takeIf { !it.done && !it.awaitingStart }?.let {
                    state.value = it.copy(paused = true)
                    updateNotification(state.value!!)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        androidx.core.content.ContextCompat.registerReceiver(this, noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastTickNanos = System.nanoTime()
        when (intent?.action) {
            ACTION_START -> {
                val phases = buildPhases(intent.getIntExtra("warmup", 0), intent.getIntExtra("work", 240), intent.getIntExtra("rest", 180), intent.getIntExtra("rounds", 4))
                val s = TimerState(intent.getStringExtra("sessionId") ?: "", intent.getStringExtra("title") ?: "Timer", phases, remaining = phases.firstOrNull()?.seconds?.toDouble() ?: 0.0, speed = intent.getDoubleExtra("speed", 1.0), awaitingStart = true, startedAt = System.currentTimeMillis())
                if (phases.isEmpty()) { stopSelf(); return START_NOT_STICKY }
                finished.value = null
                state.value = s
                startInForeground(s)
                runLoop()
            }
            ACTION_START_STRETCH -> {
                val steps = intent.getStringArrayExtra("steps") ?: emptyArray()
                val phases = steps.map { line -> val f = line.split("|"); Phase(PhaseKind.WORK, f[2].toInt(), 0, label = f[0], side = f[1], figure = f[3], reps = f[4].toInt(), stretchId = f[5]) }
                val s = TimerState(intent.getStringExtra("sessionId") ?: "", intent.getStringExtra("title") ?: "Stretching", phases, remaining = phases.firstOrNull()?.seconds?.toDouble() ?: 0.0, speed = intent.getDoubleExtra("speed", 1.0), stretch = true, startedAt = System.currentTimeMillis())
                if (phases.isEmpty()) { stopSelf(); return START_NOT_STICKY }
                finished.value = null
                state.value = s
                startInForeground(s)
                runLoop()
            }
            ACTION_BEGIN -> {
                val s = state.value ?: return START_NOT_STICKY
                val next = s.begin()
                if (next != s) { state.value = next; silence(); beep(short = true); updateNotification(next) }
            }
            ACTION_SKIP -> {
                val s = state.value ?: return START_NOT_STICKY
                if (s.done) return START_NOT_STICKY
                silence()
                val next = s.skip()
                state.value = next
                if (next.done) finish(early = false)
                else { say("Ready for ${phaseSpeech(next)}"); updateNotification(next) }
            }
            ACTION_PAUSE -> {
                state.value?.takeIf { !it.done && !it.awaitingStart }?.let {
                    state.value = it.copy(paused = true); silence(); updateNotification(state.value!!)
                }
            }
            ACTION_RESUME -> {
                state.value?.takeIf { it.paused && !it.awaitingStart && !it.done }?.let {
                    state.value = it.copy(paused = false); updateNotification(state.value!!)
                }
            }
            ACTION_STOP -> finish(early = true)
        }
        return START_NOT_STICKY
    }

    private fun runLoop() {
        loop?.cancel()
        teardown?.cancel()
        finishing = false
        tone?.release()
        tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }.getOrNull()
        if (wakeLock == null) wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "khon:timer").apply { setReferenceCounted(false) }
        state.value?.let { syncWakeLock(it) }
        if (tts == null) tts = TextToSpeech(this) { status ->
            scope.launch {
                ttsReady = status == TextToSpeech.SUCCESS
                if (ttsReady) {
                    tts?.language = Locale.US
                    tts?.setAudioAttributes(workoutSpeechAttributes())
                }
            }
        }
        state.value?.takeIf { it.active }?.let { say(phaseSpeech(it)) }
        lastTickNanos = System.nanoTime()
        loop = scope.launch {
            while (true) {
                delay(50)
                val now = System.nanoTime()
                val dt = (now - lastTickNanos) / 1e9
                lastTickNanos = now
                val before = state.value ?: break
                if (before.done) break
                val next = before.tick(dt)
                if (next == before) continue
                state.value = next
                if (next.done) { finish(early = false); break }
                when {
                    next.index != before.index -> {
                        beep(short = false)
                        say(if (next.awaitingStart) "Stretch complete. Next, ${phaseSpeech(next)}" else phaseSpeech(next))
                        updateNotification(next)
                    }
                    before.preparing -> {
                        if (!next.preparing) { vibrate(200); say("Go"); updateNotification(next) }
                        else if (kotlin.math.ceil(next.preparation) != kotlin.math.ceil(before.preparation)) beep(short = true)
                    }
                    next.active -> {
                        val whole = kotlin.math.ceil(next.remaining).toInt()
                        val previousWhole = kotlin.math.ceil(before.remaining).toInt()
                        if (whole != previousWhole) {
                            if (whole in 1..3) beep(short = true)
                            if (whole == 10 && next.phase.seconds > 20 && next.phase.reps == 0) say("ten seconds")
                            if (next.phase.reps > 0) {
                                val per = next.phase.seconds.toDouble() / next.phase.reps
                                val rep = ((next.phase.seconds - next.remaining) / per).toInt()
                                val previousRep = ((before.phase.seconds - before.remaining) / per).toInt()
                                if (rep in 1..next.phase.reps && rep != previousRep) say(rep.toString())
                            }
                        }
                    }
                }
            }
        }
    }
    private fun silence() { tts?.stop(); tone?.stopTone() }

    private fun phaseSpeech(s: TimerState): String = if (s.stretch) s.phase.label + (if (s.phase.side.isNotEmpty()) ", " + s.phase.side.lowercase() else "") + (if (s.phase.reps > 0) ", ${s.phase.reps} reps" else "") else when (s.phase.kind) {
        PhaseKind.WARMUP -> "Warm up, ${minutesWords(s.phase.seconds)}"
        PhaseKind.WORK -> "Round ${s.phase.round} of ${s.rounds}. Go"
        PhaseKind.REST -> "Rest, ${minutesWords(s.phase.seconds)}"
        PhaseKind.DONE -> "Done"
    }
    private fun minutesWords(sec: Int): String = if (sec % 60 == 0) "${sec / 60} minute" + (if (sec / 60 == 1) "" else "s") else "$sec seconds"
    private fun say(text: String) { if (ttsReady) runCatching { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "khon") } }

    private fun finish(early: Boolean) {
        if (finishing) return
        finishing = true
        loop?.cancel()
        silence()
        val s = state.value
        if (s != null) {
            val elapsed = s.elapsed.toInt()
            state.value = s.copy(index = s.phases.size, remaining = 0.0)
            if (!early) { beep(short = false); say(if (s.stretch) "Routine done" else "All rounds done") }
            // The service writes the duration itself, so it survives even when no screen is watching.
            val app = applicationContext as KhonApp
            app.scope.launch {
                runCatching {
                    if (s.stretch) { if (s.elapsed > 0.0 || s.skipped > 0) app.repo.saveStretchSession(dev.mirzohidkhon.khonfitness.data.StretchSession(
                        id = s.sessionId, routineId = s.sessionId.substringBefore(":"), routineName = s.title, date = java.time.LocalDate.now().format(dev.mirzohidkhon.khonfitness.data.ISO),
                        startedAt = s.startedAt, totalSec = elapsed, completed = s.completed, skipped = s.skipped)) }
                    else app.repo.session(s.sessionId).first()?.let { app.repo.updateSession(it.copy(timeSec = elapsed)) }
                }
                if (state.value?.sessionId == s.sessionId) {
                    finished.value = FinishedTimer(s.sessionId, elapsed, early)
                    state.value = null
                }
            }
        }
        wakeLock?.let { if (it.isHeld) it.release() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        teardown = scope.launch { if (!early) delay(1800); stopSelf() }
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
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Interval timer", NotificationManager.IMPORTANCE_HIGH).apply { setSound(null, null); enableVibration(false); lockscreenVisibility = Notification.VISIBILITY_PUBLIC })
        val n = notification(s)
        if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(NOTIFICATION_ID, n)
    }

    private fun syncWakeLock(s: TimerState) {
        wakeLock?.let {
            if (s.done || s.paused || s.awaitingStart) { if (it.isHeld) it.release() }
            else if (!it.isHeld) it.acquire(3 * 60 * 60 * 1000L)
        }
    }

    private fun updateNotification(s: TimerState) {
        syncWakeLock(s)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, notification(s))
    }

    private fun notification(s: TimerState): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }, PendingIntent.FLAG_IMMUTABLE)
        fun action(a: String) = PendingIntent.getService(this, a.hashCode(), Intent(this, TimerService::class.java).setAction(a), PendingIntent.FLAG_IMMUTABLE)
        val phase = if (s.done) "Done" else if (s.stretch) s.phase.label + (if (s.phase.side.isNotEmpty()) " · " + s.phase.side else "") else when (s.phase.kind) { PhaseKind.WARMUP -> "Warm-up"; PhaseKind.WORK -> "Work ${s.phase.round}/${s.rounds}"; PhaseKind.REST -> "Rest ${s.phase.round}/${s.rounds}"; PhaseKind.DONE -> "Done" }
        val b = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(if (s.done) s.title else "$phase · ${s.title}")
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
        when {
            s.done -> b.setContentText("Done")
            s.awaitingStart -> b.setContentText("Ready · ${mmss(s.remaining)}")
            s.paused -> b.setContentText("Paused · ${mmss(if (s.preparing) s.preparation else s.remaining)}")
            else -> b.setUsesChronometer(true).setChronometerCountDown(true)
                .setWhen(System.currentTimeMillis() + (if (s.preparing) s.preparation * 1000 else s.remaining * 1000 / s.speed).toLong()).setShowWhen(true)
        }
        if (!s.done) {
            b.addAction(0, when { s.awaitingStart -> "Start"; s.paused -> "Resume"; else -> "Pause" },
                action(when { s.awaitingStart -> ACTION_BEGIN; s.paused -> ACTION_RESUME; else -> ACTION_PAUSE }))
            if (s.stretch) b.addAction(0, "Skip", action(ACTION_SKIP))
            b.addAction(0, "Stop", action(ACTION_STOP))
        }
        return b.build()
    }

    override fun onDestroy() {
        unregisterReceiver(noisyReceiver)
        scope.cancel()
        tone?.release()
        wakeLock?.let { if (it.isHeld) it.release() }
        tts?.shutdown()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL = "timer"; const val NOTIFICATION_ID = 41
        const val ACTION_START = "start"; const val ACTION_PAUSE = "pause"; const val ACTION_RESUME = "resume"; const val ACTION_STOP = "stop"
        const val ACTION_START_STRETCH = "start_stretch"; const val ACTION_SKIP = "skip"
        const val ACTION_BEGIN = "begin"
        val state = MutableStateFlow<TimerState?>(null)
        val finished = MutableStateFlow<FinishedTimer?>(null)

        fun start(context: Context, sessionId: String, title: String, warmup: Int, work: Int, rest: Int, rounds: Int, speed: Double = 1.0) {
            val i = Intent(context, TimerService::class.java).setAction(ACTION_START)
                .putExtra("sessionId", sessionId).putExtra("title", title).putExtra("warmup", warmup).putExtra("work", work).putExtra("rest", rest).putExtra("rounds", rounds).putExtra("speed", speed)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
        }
        /** Steps: one line per phase, "label|side|seconds|figure|reps|stretchId". sessionId is "routineId:sessionId". */
        fun startStretch(context: Context, sessionId: String, title: String, steps: List<String>, speed: Double = 1.0) {
            val i = Intent(context, TimerService::class.java).setAction(ACTION_START_STRETCH)
                .putExtra("sessionId", sessionId).putExtra("title", title).putExtra("steps", steps.toTypedArray()).putExtra("speed", speed)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
        }
        fun send(context: Context, action: String) { context.startService(Intent(context, TimerService::class.java).setAction(action)) }
    }
}

data class FinishedTimer(val sessionId: String, val elapsedSec: Int, val early: Boolean)

fun mmss(seconds: Double): String { val s = kotlin.math.ceil(seconds).toInt().coerceAtLeast(0); return "%d:%02d".format(s / 60, s % 60) }

/** Workout speech follows the user's media output and volume, including Bluetooth headphones. */
internal fun workoutSpeechAttributes(): AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
    .build()
