package dev.mirzohidkhon.khonfitness.update

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import dev.mirzohidkhon.khonfitness.BuildConfig
import dev.mirzohidkhon.khonfitness.MainActivity
import dev.mirzohidkhon.khonfitness.R

/**
 * Runs in the new version right after a self-update. Reopens the app where Android allows it
 * (Android 9, or newer with the overlay permission) and posts a tap-to-open notification otherwise.
 */
class UpdatedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val open = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val canRelaunch = Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(context)
        if (!canRelaunch) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Updates", NotificationManager.IMPORTANCE_HIGH))
            val pi = PendingIntent.getActivity(context, 9, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            nm.notify(NOTIFICATION_ID, NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_timer).setContentTitle("Updated to ${BuildConfig.VERSION_NAME}").setContentText("Tap to open")
                .setContentIntent(pi).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH).build())
        }
        runCatching { context.startActivity(open) }
    }

    companion object { const val CHANNEL = "updates"; const val NOTIFICATION_ID = 42 }
}
