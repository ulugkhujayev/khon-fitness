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
import android.os.SystemClock
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import dev.mirzohidkhon.khonfitness.data.EndedRun
import dev.mirzohidkhon.khonfitness.data.KhonRepository
import dev.mirzohidkhon.khonfitness.data.TimerCommit
import dev.mirzohidkhon.khonfitness.data.TimerControl
import dev.mirzohidkhon.khonfitness.data.newId

/** Foreground service that owns the countdown. UI observes [TimerService.state]. */
class TimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loop: Job? = null
    private var tone: ToneGenerator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val cues = CueGate()
    private var cueJob: Job? = null
    private var lastTickNanos = 0L
    private var finishing = false
    private var teardown: Job? = null
    private var restoring: Job? = null
    private var beginning: Job? = null
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
        live = this
        androidx.core.content.ContextCompat.registerReceiver(this, noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED)
        // Persist on transitions only (phase, pause, start, countdown, speed); the remaining time follows from the wall clock.
        scope.launch {
            var saved: List<Any?>? = null
            state.collect { s ->
                if (s == null || s.done) { saved = null; return@collect }
                val key = listOf(s.sessionId, s.runId, s.index, s.paused, s.awaitingStart, s.preparing, s.speed, s.clock?.number, s.clock?.broken)
                if (key != saved) { saved = key; TimerSnapshots.save(this@TimerService, s) }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastTickNanos = SystemClock.elapsedRealtimeNanos()
        when (intent?.action) {
            ACTION_START -> {
                if (continueExisting(intent.getStringExtra("sessionId") ?: "", stretch = false)) return START_STICKY
                val phases = buildPhases(intent.getIntExtra("warmup", 0), intent.getIntExtra("work", 240), intent.getIntExtra("rest", 180), intent.getIntExtra("rounds", 4))
                val s = TimerState(intent.getStringExtra("sessionId") ?: "", intent.getStringExtra("title") ?: "Timer", phases, remaining = phases.firstOrNull()?.seconds?.toDouble() ?: 0.0, speed = intent.getDoubleExtra("speed", 1.0), awaitingStart = true, startedAt = System.currentTimeMillis(), runId = newId())
                if (phases.isEmpty()) { stopSelf(); return START_NOT_STICKY }
                finished.value = null
                state.value = s
                startInForeground(s)
                runLoop()
            }
            ACTION_START_STRETCH -> {
                if (continueExisting(intent.getStringExtra("sessionId") ?: "", stretch = true)) return START_STICKY
                val phases = Json.decodeFromString<List<Phase>>(intent.getStringExtra("steps") ?: "[]")
                val s = TimerState(intent.getStringExtra("sessionId") ?: "", intent.getStringExtra("title") ?: "Stretching", phases, remaining = phases.firstOrNull()?.seconds?.toDouble() ?: 0.0, speed = intent.getDoubleExtra("speed", 1.0), stretch = true, startedAt = System.currentTimeMillis(), runId = newId())
                if (phases.isEmpty()) { stopSelf(); return START_NOT_STICKY }
                finished.value = null
                state.value = s
                startInForeground(s)
                runLoop()
            }
            ACTION_BEGIN -> {
                val s = state.value ?: return START_NOT_STICKY
                if (s.stretch) {
                    val next = s.begin()
                    if (next != s) { state.value = next; silence(); beep(short = true); updateNotification(next) }
                } else beginRun(s)
            }
            ACTION_SKIP -> {
                val s = state.value ?: return START_NOT_STICKY
                if (s.done) return START_NOT_STICKY
                silence()
                val next = s.skipAt(SystemClock.elapsedRealtime())
                state.value = next
                if (next.done) finish(early = false)
                else { say("Ready for ${phaseSpeech(next)}"); updateNotification(next) }
            }
            ACTION_BACK -> {
                val s = state.value ?: return START_NOT_STICKY
                val next = s.back()
                if (next != s) { silence(); state.value = next; say("Ready for ${phaseSpeech(next)}"); updateNotification(next) }
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
            ACTION_STOP -> if (beginning?.isActive == true) scope.launch { beginning?.join(); finish(early = true) } else finish(early = true)
            // A null intent is the system restarting the sticky service after it killed the process.
            ACTION_RESTORE, null -> restore()
        }
        return if (state.value?.done == false || restoring?.isActive == true) START_STICKY else START_NOT_STICKY
    }

    /**
     * A screen that starts the timer already running, or the one a snapshot holds (the screen came back from Recents
     * after process death), continues it instead of starting over. Stretch runs match by routine.
     */
    private fun continueExisting(sessionId: String, stretch: Boolean): Boolean {
        fun same(id: String) = if (stretch) id.substringBefore(":") == sessionId.substringBefore(":") else id == sessionId
        state.value?.takeIf { !it.done && it.stretch == stretch && same(it.sessionId) }?.let { startInForeground(it); return true }
        if (state.value != null) return false
        val now = System.currentTimeMillis()
        val snap = TimerSnapshots.load(this)?.takeIf { it.state.stretch == stretch && same(it.state.sessionId) && !it.expired(now) } ?: return false
        restoring?.cancel()
        val s = snap.advancedAt(now, SystemClock.elapsedRealtime())
        finished.value = null
        state.value = s
        startInForeground(s)
        runLoop()
        if (s.done) finish(early = false) else updateNotification(s)
        return true
    }

    /** Brings back the timer from its snapshot, advanced by the wall-clock time the process was dead. */
    private fun restore() {
        if (state.value != null || restoring?.isActive == true) return
        val snap = TimerSnapshots.load(this) ?: return stopIfIdle()
        val app = applicationContext as KhonApp
        if (snap.state.done && !snap.state.stretch) {
            // An ended run whose commit did not settle: commit it now, whatever the session's state.
            app.scope.launch { commitPending(app) }
            return stopIfIdle()
        }
        restoring = scope.launch {
            val open = runCatching { withContext(Dispatchers.IO) { stillOpen(app.repo, snap.state) } }.getOrNull() ?: return@launch stopIfIdle()
            if (state.value != null) return@launch
            if (!open || snap.expired(System.currentTimeMillis())) { TimerSnapshots.clearRun(app, snap.state); return@launch stopIfIdle() }
            val s = snap.advancedAt(System.currentTimeMillis(), SystemClock.elapsedRealtime())
            if (s.done) {
                // The workout ended while the process was dead: write the result the live timer would have written.
                // The ended snapshot never expires, so a commit that does not settle is tried again at the next start.
                TimerSnapshots.save(app, s)
                val generation = app.repo.generation
                app.scope.launch { recordTimer(app, s, generation) }
                return@launch stopIfIdle()
            }
            // Android can refuse a foreground start from the background; the snapshot stays for the next app open.
            if (runCatching { startInForeground(s) }.isFailure) return@launch stopIfIdle()
            finished.value = null
            state.value = s
            runLoop()
            updateNotification(s)
        }
    }

    private fun stopIfIdle() { if (state.value == null) stopSelf() }

    /**
     * The Start action of a cardio run, from the screen or the notification: the session gives the run its number,
     * then the run begins with the anchor of that instant. Without a number (the session is gone, the write failed)
     * it begins without a clock.
     */
    private fun beginRun(s: TimerState) {
        if (s.done || !s.awaitingStart || beginning?.isActive == true) return
        val app = applicationContext as KhonApp
        beginning = scope.launch {
            val start = runCatching { withContext(Dispatchers.IO) { app.repo.startTimerRun(s.sessionId) } }.getOrNull()
            val current = state.value?.takeIf { it.runId == s.runId && it.awaitingStart && !it.done } ?: return@launch
            val next = current.beginRun(start, System.currentTimeMillis(), SystemClock.elapsedRealtime())
            lastTickNanos = SystemClock.elapsedRealtimeNanos()
            state.value = next; silence(); beep(short = true); updateNotification(next)
        }
    }

    /** Ends the live timer without its result, for a Replace import or a reset; the snapshot stays. */
    private fun halt() {
        restoring?.cancel()
        beginning?.cancel()
        loop?.cancel()
        teardown?.cancel()
        finishing = true
        silence()
        state.value = null
        finished.value = null
        wakeLock?.let { if (it.isHeld) it.release() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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
        lastTickNanos = SystemClock.elapsedRealtimeNanos()
        loop = scope.launch {
            while (true) {
                delay(50)
                val now = SystemClock.elapsedRealtimeNanos()
                val dt = (now - lastTickNanos) / 1e9
                lastTickNanos = now
                val before = state.value ?: break
                if (before.done) break
                val ticked = before.tickAt(dt, now / 1_000_000L)
                val next = if (ticked.index != before.index) ticked.checkClock(System.currentTimeMillis(), now / 1_000_000L) else ticked
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
    private fun silence() { if (cues.cancel()) runCatching { tts?.stop() }; tone?.stopTone() }

    private fun phaseSpeech(s: TimerState): String = if (s.stretch) s.phase.label + (if (s.phase.side.isNotEmpty()) ", " + s.phase.side.lowercase() else "") + (if (s.phase.reps > 0) ", ${s.phase.reps} reps" else "") else when (s.phase.kind) {
        PhaseKind.WARMUP -> "Warm up, ${minutesWords(s.phase.seconds)}"
        PhaseKind.WORK -> "Round ${s.phase.round} of ${s.rounds}. Go"
        PhaseKind.REST -> "Rest, ${minutesWords(s.phase.seconds)}"
        PhaseKind.DONE -> "Done"
    }
    private fun minutesWords(sec: Int): String = if (sec % 60 == 0) "${sec / 60} minute" + (if (sec / 60 == 1) "" else "s") else "$sec seconds"
    /** Rapid skips must not hammer the speech engine: cues closer than 300 ms collapse into the latest one. */
    private fun say(text: String) {
        if (!ttsReady) return
        val now = cues.offer(text, SystemClock.elapsedRealtime())
        if (now != null) speak(now) else if (cueJob?.isActive != true) cues.dueAt()?.let { due ->
            cueJob = scope.launch { delay(due - SystemClock.elapsedRealtime()); cues.poll(SystemClock.elapsedRealtime())?.let(::speak) }
        }
    }
    private fun speak(text: String) { if (ttsReady) runCatching { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "khon") } }

    private fun finish(early: Boolean) {
        if (finishing) return
        finishing = true
        loop?.cancel()
        silence()
        val s = state.value
        if (s != null) {
            val elapsed = s.elapsed.toInt()
            val done = s.finishedAt(System.currentTimeMillis(), SystemClock.elapsedRealtime())
            state.value = done
            // A done snapshot until the result is written: if the process dies in between, restore writes it.
            TimerSnapshots.save(this, done)
            if (!early) { beep(short = false); say(if (s.stretch) "Routine done" else "All rounds done") }
            // The service writes the duration itself, so it survives even when no screen is watching.
            val app = applicationContext as KhonApp
            val generation = app.repo.generation
            app.scope.launch {
                recordTimer(app, done, generation)
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
        if (live === this) live = null
        unregisterReceiver(noisyReceiver)
        scope.cancel()
        tone?.release()
        wakeLock?.let { if (it.isHeld) it.release() }
        ttsReady = false
        tts?.let { runCatching { it.stop(); it.shutdown() } }
        tts = null
        super.onDestroy()
    }

    companion object {
        const val CHANNEL = "timer"; const val NOTIFICATION_ID = 41
        const val ACTION_START = "start"; const val ACTION_PAUSE = "pause"; const val ACTION_RESUME = "resume"; const val ACTION_STOP = "stop"
        const val ACTION_START_STRETCH = "start_stretch"; const val ACTION_SKIP = "skip"; const val ACTION_BACK = "back"
        const val ACTION_BEGIN = "begin"; const val ACTION_RESTORE = "restore"
        val state = MutableStateFlow<TimerState?>(null)
        val finished = MutableStateFlow<FinishedTimer?>(null)
        @Volatile private var live: TimerService? = null

        /** Stops the live timer without writing its result ([halt]); its snapshot stays. */
        suspend fun halt() = withContext(Dispatchers.Main.immediate) { live?.halt() ?: run { state.value = null } }

        fun start(context: Context, sessionId: String, title: String, warmup: Int, work: Int, rest: Int, rounds: Int, speed: Double = 1.0) {
            val i = Intent(context, TimerService::class.java).setAction(ACTION_START)
                .putExtra("sessionId", sessionId).putExtra("title", title).putExtra("warmup", warmup).putExtra("work", work).putExtra("rest", rest).putExtra("rounds", rounds).putExtra("speed", speed)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
        }
        /** sessionId is "routineId:sessionId". */
        fun startStretch(context: Context, sessionId: String, title: String, steps: List<Phase>, speed: Double = 1.0) {
            val i = Intent(context, TimerService::class.java).setAction(ACTION_START_STRETCH)
                .putExtra("sessionId", sessionId).putExtra("title", title).putExtra("steps", Json.encodeToString(steps)).putExtra("speed", speed)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
        }
        /** App start: if the system killed the process mid-workout, the service restores the timer. */
        fun restore(context: Context) {
            if (state.value == null && TimerSnapshots.exists(context)) runCatching { context.startService(Intent(context, TimerService::class.java).setAction(ACTION_RESTORE)) }
        }
        fun send(context: Context, action: String) { context.startService(Intent(context, TimerService::class.java).setAction(action)) }
    }
}

/**
 * Writes the timer result and clears the run's own snapshot: the stretch session, or the cardio run through
 * [KhonRepository.endTimerRun] against [generation], the repository generation when the run ended. Repeating it for
 * the same state changes nothing.
 */
internal suspend fun recordTimer(app: KhonApp, s: TimerState, generation: Long) {
    if (s.stretch) {
        runCatching { stretchResult(s, java.time.LocalDate.now())?.let { app.repo.recordStretchSession(it) } } // written once per session id
        TimerSnapshots.clear(app, s.sessionId)
    } else recordRun(app.repo, app, s, generation)
}

/** A cardio run's end: its result, and the clear of its snapshot only while the snapshot is still this run's. */
internal suspend fun recordRun(repo: KhonRepository, context: Context, s: TimerState, generation: Long?): TimerCommit =
    repo.endTimerRun(generation) { EndedRun(s.runResult()) { TimerSnapshots.clearRun(context, s) } }

/** The ended cardio run that a snapshot holds, committed at the app's start under the same rules as at its end. */
internal suspend fun commitPending(app: KhonApp): TimerCommit = commitPending(app.repo, app)

internal suspend fun commitPending(repo: KhonRepository, context: Context): TimerCommit = repo.endTimerRun {
    TimerSnapshots.load(context)?.state?.takeIf { it.done && !it.stretch }?.let { s -> EndedRun(s.runResult()) { TimerSnapshots.clearRun(context, s) } }
}

/** The timer as [KhonRepository] stops and clears it for a Replace import and a reset. */
class ServiceTimerControl(private val context: Context) : TimerControl {
    override suspend fun halt() = TimerService.halt()
    override fun clear() = TimerSnapshots.clearAll(context)
    override fun resume() = TimerService.restore(context)
}

/**
 * A stretch run's session, with the routine name captured when the run started. Stretch runs count only after one
 * completed stretch or 30 s of stretching; skipping through the list is not a session (null).
 */
internal fun stretchResult(s: TimerState, date: java.time.LocalDate): dev.mirzohidkhon.khonfitness.data.StretchSession? =
    if (s.completed > 0 || s.elapsed >= 30.0) dev.mirzohidkhon.khonfitness.data.StretchSession(
        id = s.sessionId, routineId = s.sessionId.substringBefore(":"), routineName = s.title, date = date.format(dev.mirzohidkhon.khonfitness.data.ISO),
        startedAt = s.startedAt, totalSec = s.elapsed.toInt(), completed = s.completed, skipped = s.skipped,
    ) else null

/** A snapshot is stale once its cardio session is gone or finished, or its stretch routine is gone or already recorded. */
internal suspend fun stillOpen(repo: dev.mirzohidkhon.khonfitness.data.KhonRepository, s: TimerState): Boolean =
    if (s.stretch) repo.stretchRoutines.first().any { it.id == s.sessionId.substringBefore(":") } && repo.stretchSessions.first().none { it.id == s.sessionId }
    else repo.session(s.sessionId).first()?.finished == false

data class FinishedTimer(val sessionId: String, val elapsedSec: Int, val early: Boolean)

fun mmss(seconds: Double): String { val s = kotlin.math.ceil(seconds).toInt().coerceAtLeast(0); return "%d:%02d".format(s / 60, s % 60) }

/** Workout speech follows the user's media output and volume, including Bluetooth headphones. */
internal fun workoutSpeechAttributes(): AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
    .build()
