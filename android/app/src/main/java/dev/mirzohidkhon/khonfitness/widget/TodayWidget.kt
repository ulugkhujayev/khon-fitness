package dev.mirzohidkhon.khonfitness.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.MainActivity
import dev.mirzohidkhon.khonfitness.data.ItemType
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.collectAsState

/** Launcher widget: today's item and a Start button that opens the app straight into the session. */
class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = (context.applicationContext as KhonApp).repo
        // Glance runs this function once per widget session; an update during a live session only recomposes.
        // So the data is collected inside the composition, where each change reaches the tile.
        val plan0 = repo.planData.first()
        val unfinished0 = repo.unfinishedSession.first()
        val stretch0 = repo.stretchSessions.first()
        val routines0 = repo.stretchRoutines.first()
        provideContent {
            val plan = repo.planData.collectAsState(plan0).value
            val unfinished = repo.unfinishedSession.collectAsState(unfinished0).value
            val stretchSessions = repo.stretchSessions.collectAsState(stretch0).value
            val routines = repo.stretchRoutines.collectAsState(routines0).value
            val today = LocalDate.now()
            val item = plan.itemFor(today)
            val title = unfinished?.programName ?: unfinished?.exerciseName ?: item.name
            val sub = when { unfinished != null -> "In progress"; item.isRest -> "Rest day"; item.itemType == ItemType.PROGRAM -> "Gym"; else -> "Cardio" }
            val label = when { unfinished != null -> "Resume"; item.isRest -> "Open"; else -> "Start" }
            val stretchedToday = stretchSessions.any { it.date == today.format(dev.mirzohidkhon.khonfitness.data.ISO) }
            val routine = routines.firstOrNull { it.active }
            val open = actionStartActivity(Intent(context, MainActivity::class.java).apply { action = if (item.isRest && unfinished == null) Intent.ACTION_MAIN else ACTION_START_TODAY; addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP) })
            Column(GlanceModifier.fillMaxSize().background(Color(0xFF1C1C1E)).cornerRadius(20.dp).padding(14.dp).clickable(open)
                .semantics { contentDescription = "Today: $title" + (if (unfinished != null) ", in progress" else "") + ", $label" }, verticalAlignment = Alignment.CenterVertically) {
                Text(sub, style = TextStyle(color = ColorProvider(Color(0xFF9B9BA3)), fontSize = 12.sp))
                Text(title, style = TextStyle(color = ColorProvider(Color(0xFFF4F4F5)), fontSize = 20.sp, fontWeight = FontWeight.Bold), maxLines = 2)
                if (routine != null) Text(if (stretchedToday) "Stretched today" else routine.name + " not yet", style = TextStyle(color = ColorProvider(if (stretchedToday) Color(0xFF30D158) else Color(0xFF8E8E93)), fontSize = 12.sp))
                Spacer(GlanceModifier.height(10.dp))
                Box(GlanceModifier.fillMaxWidth().height(40.dp).background(Color(0xFFFF9F0A)).cornerRadius(20.dp).clickable(open), contentAlignment = Alignment.Center) {
                    Text(label, style = TextStyle(color = ColorProvider(Color.White), fontSize = 16.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }

    companion object {
        const val ACTION_START_TODAY = "dev.mirzohidkhon.khonfitness.START_TODAY"
        suspend fun refresh(context: Context) = runCatching { TodayWidget().updateAll(context) }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
