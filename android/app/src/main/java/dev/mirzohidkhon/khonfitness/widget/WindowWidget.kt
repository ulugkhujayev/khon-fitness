package dev.mirzohidkhon.khonfitness.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.MainActivity
import dev.mirzohidkhon.khonfitness.data.EatingWindow
import dev.mirzohidkhon.khonfitness.data.iso
import dev.mirzohidkhon.khonfitness.ui.screens.windowSummary
import java.time.LocalDate

/** 2x1 eating-window tile: a dot, Open or Closed, and the time left. Tap opens the window sheet. */
class WindowWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = (context.applicationContext as KhonApp).repo
        val w = repo.eatingWindowOnce()
        val (open, text) = windowSummary(w, repo.windowDay(LocalDate.now().iso()))
        provideContent {
            val openApp = actionStartActivity(Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_OPEN_WINDOW).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            Row(GlanceModifier.fillMaxSize().background(Color(0xFF1C1C1E)).cornerRadius(20.dp).padding(horizontal = 14.dp).clickable(openApp), verticalAlignment = Alignment.CenterVertically) {
                Box(GlanceModifier.size(12.dp).background(if (open) Color(0xFF30D158) else Color(0xFF636366)).cornerRadius(6.dp)) {}
                Spacer(GlanceModifier.width(12.dp))
                Column {
                    Text(if (!w.enabled) "Window off" else if (open) "Open" else "Closed", style = TextStyle(color = ColorProvider(Color.White), fontSize = 17.sp, fontWeight = FontWeight.Bold))
                    Text(text, style = TextStyle(color = ColorProvider(Color(0xFF8E8E93)), fontSize = 13.sp))
                }
            }
        }
    }

    companion object { suspend fun refresh(context: Context) = runCatching { WindowWidget().updateAll(context) } }
}

class WindowWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WindowWidget()
}
