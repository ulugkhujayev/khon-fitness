package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K
import java.time.LocalDate
import java.time.LocalDateTime

/** Secondary text for the Today row and the widget: "closes in 2 h 10 m", "opens 12:00", "Off". */
fun windowSummary(w: EatingWindow?, day: WindowDay?, now: LocalDateTime = LocalDateTime.now()): Pair<Boolean, String> {
    if (w == null || !w.enabled) return false to "Off"
    val st = EatingWindowRules.state(w, day, now)
    return st.open to if (st.open) "closes in " + EatingWindowRules.untilText(now, st.changesAt) else "opens " + EatingWindowRules.hhmm(w.startMinute)
}

/** The eating-window sheet: Open now or Close now, the hours, the switch, and the strip of kept days. */
@Composable
fun WindowSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    val w = vm.eatingWindow.collectAsState().value ?: EatingWindow()
    val days by vm.windowDays.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val today = LocalDate.now()
    val day = days.find { it.date == today.iso() }
    val now = LocalDateTime.now()
    val st = EatingWindowRules.state(w, day, now)
    fun save(n: EatingWindow) { vm.run { vm.repo.saveEatingWindow(n); dev.mirzohidkhon.khonfitness.window.WindowScheduler.reschedule(context) } }
    Sheet("Eating window", onDismiss) {
        GroupedList {
            if (w.enabled) {
                if (st.open) ListRow("Close now", chevron = false, divider = false, titleColor = K.Accent) { vm.run { vm.repo.closeWindowNow(today.iso(), System.currentTimeMillis()); dev.mirzohidkhon.khonfitness.window.WindowScheduler.reschedule(context) }; onDismiss() }
                else ListRow("Open now", chevron = false, divider = false, titleColor = K.Accent) { vm.run { vm.repo.openWindowNow(today.iso(), System.currentTimeMillis()); dev.mirzohidkhon.khonfitness.window.WindowScheduler.reschedule(context) }; onDismiss() }
            }
            FieldRow("Opens", divider = w.enabled) { TimeStepper(w.startMinute) { m -> save(w.copy(startMinute = m.coerceIn(0, w.endMinute - 60))) } }
            FieldRow("Closes") { TimeStepper(w.endMinute) { m -> save(w.copy(endMinute = m.coerceIn(w.startMinute + 60, 24 * 60 - 1))) } }
            FieldRow("Remind before", divider = true) { NumberField(w.remindBeforeMin.toDouble(), { v -> save(w.copy(remindBeforeMin = (v ?: 0.0).toInt().coerceIn(0, 120))) }, 5.0, "min", Modifier.width(176.dp), 0, 38) }
            FieldRow("On") { Switch(w.enabled, { save(w.copy(enabled = it)) }, colors = SwitchDefaults.colors(checkedTrackColor = K.Green, checkedThumbColor = Color.White, uncheckedTrackColor = K.Surface3, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent)) }
        }
        if (w.enabled) {
            Text("Last 28 days", color = K.Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp))
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 27 downTo 0) {
                    val d = today.minusDays(i.toLong()); val wd = days.find { it.date == d.iso() }
                    val kept = wd == null || (wd.openedAt == null && wd.closedAt == null)
                    val m = Modifier.size(8.dp).clip(CircleShape)
                    Box(if (d == today) m.border(1.5.dp, K.Green, CircleShape) else if (kept) m.background(K.Green) else m.border(1.5.dp, K.Green, CircleShape))
                }
            }
        }
    }
}

/** hh:mm with 15-minute steps, in the number-widget shape. */
@Composable
fun TimeStepper(minute: Int, onChange: (Int) -> Unit) {
    Row(Modifier.width(150.dp).height(38.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp)).background(K.Surface2), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Box(Modifier.width(40.dp).fillMaxHeight().clickable { onChange(minute - 15) }, contentAlignment = androidx.compose.ui.Alignment.Center) { Text("−", fontWeight = FontWeight.Medium) }
        Text(EatingWindowRules.hhmm(minute), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Box(Modifier.width(40.dp).fillMaxHeight().clickable { onChange(minute + 15) }, contentAlignment = androidx.compose.ui.Alignment.Center) { Text("+", fontWeight = FontWeight.Medium) }
    }
}
