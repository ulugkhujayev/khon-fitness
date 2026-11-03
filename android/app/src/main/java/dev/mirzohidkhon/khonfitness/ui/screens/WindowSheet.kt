package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.components.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import dev.mirzohidkhon.khonfitness.ui.theme.K
import java.time.LocalDateTime
import androidx.compose.ui.semantics.contentDescription

/** Secondary text for the Today row and the widget: "closes in 2 h 10 m", "opens 12:00", "Off". */
fun windowSummary(w: EatingWindow?, day: WindowDay?, now: LocalDateTime = LocalDateTime.now()): Pair<Boolean, String> {
    if (w == null || !w.enabled) return false to "Off"
    val st = EatingWindowRules.state(w, day, now)
    return st.open to if (st.open) "closes in " + EatingWindowRules.untilText(now, st.changesAt) else "opens " + EatingWindowRules.hhmm(w.startMinute)
}

/**
 * The eating-window sheet: where the window stands now in words and on a day bar, Open now or Close now, then the
 * switch, the hours and the reminder, and the last 28 days as four weeks: filled when the fixed hours were kept, a
 * ring when opened or closed by hand, today in white.
 */
@Composable
fun WindowSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    val w = vm.eatingWindow.collectAsState().value ?: EatingWindow()
    val days by vm.windowDays.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    // The clock moves on each minute while the sheet is open, so the headline and the now mark stay true.
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(60_000L - System.currentTimeMillis() % 60_000L); now = LocalDateTime.now() } }
    val today = now.toLocalDate()
    val day = days.find { it.date == today.iso() }
    val open = w.enabled && EatingWindowRules.state(w, day, now).open
    // History counts from the first enable date; an existing user with none gets the fallback stored once.
    val storedSince by vm.repo.windowSince.collectAsState(null)
    LaunchedEffect(w.enabled) { if (w.enabled) vm.repo.keepWindowSince() }
    val since = storedSince ?: windowSinceFallback(days, today)
    // Each control writes its own setting onto the stored row ([KhonDao.setWindowStart] and the others), then re-arms the alarms.
    fun save(write: suspend () -> Unit) { vm.run { write(); dev.mirzohidkhon.khonfitness.window.WindowScheduler.reschedule(context) } }
    Sheet("Eating window", onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            GroupedList(elevated = true) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Dot(if (open) K.Green else K.Muted, filled = open, size = 10)
                        Text(windowStatus(w, day, now), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    }
                    if (w.enabled) WindowDayBar(w, now)
                }
            }
            if (w.enabled) TintedButton(if (open) "Close now" else "Open now", Modifier.padding(top = 12.dp)) {
                vm.run {
                    if (open) vm.repo.closeWindowNow(today.iso(), System.currentTimeMillis()) else vm.repo.openWindowNow(today.iso(), System.currentTimeMillis())
                    dev.mirzohidkhon.khonfitness.window.WindowScheduler.reschedule(context)
                }
                onDismiss()
            }
            GroupedList(Modifier.padding(top = 20.dp), elevated = true) {
                ToggleRow("On", w.enabled, divider = false) { on -> save { vm.repo.setWindowEnabled(on) } }
                FieldRow("Opens") { TimeStepper(w.startMinute, "Opens", EatingWindowRules.startRange(w.endMinute), elevated = true) { m -> save { vm.repo.setWindowStart(m) } } }
                FieldRow("Closes") { TimeStepper(w.endMinute, "Closes", EatingWindowRules.endRange(w.startMinute), elevated = true) { m -> save { vm.repo.setWindowEnd(m) } } }
                FieldRow("Remind before") {
                    NumberField(w.remindBeforeMin.toDouble(), { v -> save { vm.repo.setWindowRemind((v ?: 0.0).toInt()) } }, 5.0, "min", width = 176.dp, decimals = 0, height = 38, max = 120.0, elevated = true, label = "Remind before, minutes")
                }
            }
            if (w.enabled) {
                val strip = windowStrip(days, today, since)
                val summary = stripSummary(strip, since)
                SheetGroupTitle("Last 28 days")
                WindowWeeks(strip, summary)
                GroupFooter("$summary A ring is a day opened or closed by hand; today is white." + if (StripCell.BEFORE in strip) " Grey days came before tracking." else "")
            }
        }
    }
}

/**
 * hh:mm with 15-minute steps, in the number-widget shape, kept inside [range]. Like the number widget's steppers
 * ([Stepper]), a tap steps once with a click and saves; held, it repeats and saves once on release. [elevated] inside
 * a sheet's elevated group, where the box takes Surface3.
 */
@Composable
fun TimeStepper(minute: Int, name: String, range: IntRange, elevated: Boolean = false, onChange: (Int) -> Unit) {
    val tick = dev.mirzohidkhon.khonfitness.ui.components.rememberTick()
    val stepper = remember { Stepper(15.0) }.also { it.min = range.first.toDouble(); it.max = range.last.toDouble() }
    // The time shown while a step is held, and after a save until a new value comes. A new value shows unless a hold
    // is in progress.
    var shown by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(minute) { if (stepper.arrived()) shown = null }
    fun step(sign: Int, save: Boolean) {
        val s = stepper.step(sign, save, shown?.toString() ?: "", minute.toDouble(), null)
        tick(false)
        shown = s.shown.toInt()
        s.save?.let { onChange(it.toInt()) }
    }
    fun release() { stepper.release()?.let { shown = it.toInt(); onChange(it.toInt()) } }
    val text = EatingWindowRules.hhmm(shown ?: minute)
    // 166 dp wide; a large font widens the value, which stays on one line. The row is MIN_TAP tall so each step is a
    // MIN_TAP square to the touch; the box is drawn 38 dp tall in its middle, as [NumberField] draws its own.
    val fill = if (elevated) K.Surface3 else K.Surface2
    Row(
        Modifier.widthIn(min = 166.dp).height(MIN_TAP.dp).drawWithContent {
            val inset = (size.height - 38.dp.toPx()).coerceAtLeast(0f) / 2
            val box = Path().apply { addRoundRect(RoundRect(0f, inset, size.width, size.height - inset, CornerRadius(10.dp.toPx()))) }
            clipPath(box) { drawRect(fill); this@drawWithContent.drawContent() }
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(MIN_TAP.dp).fillMaxHeight().repeatPress("$name 15 minutes earlier", { step(-1, true) }, { step(-1, false) }, ::release), contentAlignment = Alignment.Center) { Text("−", fontSize = 22.sp, fontWeight = FontWeight.Medium) }
        Box(Modifier.width(1.dp).fillMaxHeight().background(K.Divider))
        Text(
            text, fontSize = 19.sp, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR), textAlign = TextAlign.Center,
            maxLines = 1, softWrap = false, modifier = Modifier.widthIn(min = 68.dp).padding(horizontal = 6.dp).semantics { contentDescription = "$name $text" },
        )
        Box(Modifier.width(1.dp).fillMaxHeight().background(K.Divider))
        Box(Modifier.width(MIN_TAP.dp).fillMaxHeight().repeatPress("$name 15 minutes later", { step(1, true) }, { step(1, false) }, ::release), contentAlignment = Alignment.Center) { Text("+", fontSize = 22.sp, fontWeight = FontWeight.Medium) }
    }
}
