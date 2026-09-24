package dev.mirzohidkhon.khonfitness.window

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.MainActivity
import dev.mirzohidkhon.khonfitness.R
import dev.mirzohidkhon.khonfitness.data.EatingWindowRules
import dev.mirzohidkhon.khonfitness.data.iso
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Exact alarms at the window boundaries and the reminders before them. Each alarm refreshes the widgets,
 * posts a reminder when due, and arms the next one. Re-armed at boot and after every settings change.
 */
object WindowScheduler {
    const val CHANNEL = "window"
    private const val REQ_BOUNDARY = 71
    private const val REQ_REMIND = 72

    fun reschedule(context: Context) {
        val app = context.applicationContext as KhonApp
        app.scope.launch {
            val w = app.repo.eatingWindowOnce()
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val boundary = pi(context, REQ_BOUNDARY, "boundary")
            val remind = pi(context, REQ_REMIND, "remind")
            am.cancel(boundary); am.cancel(remind)
            dev.mirzohidkhon.khonfitness.widget.TodayWidget.refresh(context)
            dev.mirzohidkhon.khonfitness.widget.WindowWidget.refresh(context)
            if (!w.enabled) return@launch
            val now = LocalDateTime.now()
            val day = app.repo.windowDay(LocalDate.now().iso())
            val st = EatingWindowRules.state(w, day, now)
            set(am, st.changesAt.plusSeconds(1), boundary)
            val remindAt = st.changesAt.minusMinutes(w.remindBeforeMin.toLong())
            if (w.remindBeforeMin > 0 && remindAt.isAfter(now)) set(am, remindAt, remind)
        }
    }

    private fun set(am: AlarmManager, at: LocalDateTime, pi: PendingIntent) {
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        runCatching {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }

    private fun pi(context: Context, req: Int, kind: String): PendingIntent =
        PendingIntent.getBroadcast(context, req, Intent(context, WindowReceiver::class.java).setAction("dev.mirzohidkhon.khonfitness.WINDOW_" + kind.uppercase()), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun notify(context: Context, title: String, text: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Eating window", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_OPEN_WINDOW).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        nm.notify(43, NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_timer).setContentTitle(title).setContentText(text).setContentIntent(open).setAutoCancel(true).build())
    }
}

class WindowReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as KhonApp
        val pending = goAsync()
        app.scope.launch {
            runCatching {
                val w = app.repo.eatingWindowOnce()
                if (w.enabled) {
                    val st = EatingWindowRules.state(w, app.repo.windowDay(LocalDate.now().iso()), LocalDateTime.now())
                    val changesAt = EatingWindowRules.hhmm(st.changesAt.hour * 60 + st.changesAt.minute)
                    when (intent.action) {
                        "dev.mirzohidkhon.khonfitness.WINDOW_REMIND" -> WindowScheduler.notify(context, if (st.open) "Window closes in ${w.remindBeforeMin} min" else "Window opens in ${w.remindBeforeMin} min", if (st.open) "Closes at $changesAt" else "Opens at $changesAt")
                        Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> Unit
                        else -> WindowScheduler.notify(context, if (st.open) "Eating window open" else "Eating window closed", if (st.open) "Until $changesAt" else "Opens at $changesAt")
                    }
                }
            }
            WindowScheduler.reschedule(context)
            pending.finish()
        }
    }
}
