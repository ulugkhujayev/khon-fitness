package dev.mirzohidkhon.khonfitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.mirzohidkhon.khonfitness.ui.KhonNav
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Dark-only app: keep status and nav bar icons light whatever the system theme is.
        enableEdgeToEdge(androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        volumeControlStream = android.media.AudioManager.STREAM_MUSIC
        if (intent?.action == dev.mirzohidkhon.khonfitness.widget.TodayWidget.ACTION_START_TODAY) startRequested.value = true
        if (intent?.action == ACTION_OPEN_WINDOW) windowRequested.value = true
        dev.mirzohidkhon.khonfitness.window.WindowScheduler.reschedule(this)
        dev.mirzohidkhon.khonfitness.timer.TimerService.restore(this)
        (getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager).cancel(dev.mirzohidkhon.khonfitness.update.UpdatedReceiver.NOTIFICATION_ID)
        setContent { KhonTheme { KhonNav() } }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.action == dev.mirzohidkhon.khonfitness.widget.TodayWidget.ACTION_START_TODAY) startRequested.value = true
        if (intent.action == ACTION_OPEN_WINDOW) windowRequested.value = true
    }

    companion object {
        /** Set by the widget's Start button; Today consumes it and starts the planned item. */
        val startRequested = kotlinx.coroutines.flow.MutableStateFlow(false)
        /** Set by the window widget or a reminder; Today opens the window sheet. */
        val windowRequested = kotlinx.coroutines.flow.MutableStateFlow(false)
        const val ACTION_OPEN_WINDOW = "dev.mirzohidkhon.khonfitness.OPEN_WINDOW"
    }
}
