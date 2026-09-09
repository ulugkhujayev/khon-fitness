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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.MainActivity
import dev.mirzohidkhon.khonfitness.data.ItemType
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/** Launcher widget: today's item and a Start button that opens the app straight into the session. */
class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = (context.applicationContext as KhonApp).repo
        val plan = repo.planData.first()
        val item = plan.itemFor(LocalDate.now())
        val unfinished = repo.unfinishedSession.first()
        val title = unfinished?.programName ?: unfinished?.exerciseName ?: item.name
        val sub = when { unfinished != null -> "In progress"; item.isRest -> "Rest day"; item.itemType == ItemType.PROGRAM -> "Gym"; else -> "Cardio" }
        val label = when { unfinished != null -> "Resume"; item.isRest -> "Open"; else -> "Start" }
        provideContent {
            val open = actionStartActivity(Intent(context, MainActivity::class.java).apply { action = if (item.isRest && unfinished == null) Intent.ACTION_MAIN else ACTION_START_TODAY; addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP) })
            Column(GlanceModifier.fillMaxSize().background(Color(0xFF1A1A1D)).cornerRadius(18.dp).padding(14.dp).clickable(open), verticalAlignment = Alignment.CenterVertically) {
                Text(sub, style = TextStyle(color = ColorProvider(Color(0xFF9B9BA3)), fontSize = 12.sp))
                Text(title, style = TextStyle(color = ColorProvider(Color(0xFFF4F4F5)), fontSize = 20.sp, fontWeight = FontWeight.Bold), maxLines = 2)
                Spacer(GlanceModifier.height(10.dp))
                Box(GlanceModifier.fillMaxWidth().height(40.dp).background(Color(0xFFF0A35A)).cornerRadius(12.dp).clickable(open), contentAlignment = Alignment.Center) {
                    Text(label, style = TextStyle(color = ColorProvider(Color(0xFF2A1708)), fontSize = 16.sp, fontWeight = FontWeight.Bold))
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
