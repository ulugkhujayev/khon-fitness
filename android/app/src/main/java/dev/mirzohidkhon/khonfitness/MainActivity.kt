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
        if (intent?.action == dev.mirzohidkhon.khonfitness.widget.TodayWidget.ACTION_START_TODAY) startRequested.value = true
        (getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager).cancel(dev.mirzohidkhon.khonfitness.update.UpdatedReceiver.NOTIFICATION_ID)
        setContent { KhonTheme { KhonNav() } }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.action == dev.mirzohidkhon.khonfitness.widget.TodayWidget.ACTION_START_TODAY) startRequested.value = true
    }

    companion object {
        /** Set by the widget's Start button; Today consumes it and starts the planned item. */
        val startRequested = kotlinx.coroutines.flow.MutableStateFlow(false)
    }
}
