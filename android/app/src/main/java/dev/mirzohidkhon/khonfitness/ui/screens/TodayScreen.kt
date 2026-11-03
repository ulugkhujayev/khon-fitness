package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.Icons
import dev.mirzohidkhon.khonfitness.timer.TimerService
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K
import java.time.LocalDate

/** Now, read again at each minute boundary so "34 min" and "closes in" stay right while the screen is open. */
@Composable
fun rememberMinuteClock(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(60_000 - System.currentTimeMillis() % 60_000); now = System.currentTimeMillis() } }
    return now
}

/**
 * Today: the week strip, today's item with Start or Resume, one group of rows (a newer release to install, the
 * stretch routine, the eating window, band sessions waiting for import), and bodyweight with its chart and entry.
 */
@Composable
fun TodayScreen(vm: AppViewModel, nav: NavHostController) {
    val plan by vm.planData.collectAsStateWithLifecycle()
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val blocks by vm.blocks.collectAsStateWithLifecycle()
    val blockExercises by vm.blockExercises.collectAsStateWithLifecycle()
    val bodyweights by vm.bodyweights.collectAsStateWithLifecycle()
    val unfinished by vm.unfinished.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val logs by vm.allSetLogs.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val item = plan.itemFor(today)
    val loaded = plan.plan.isNotEmpty() || plan.programs.isNotEmpty() || plan.exercises.isNotEmpty()
    var planDate by remember { mutableStateOf<LocalDate?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var pendingBand by remember { mutableIntStateOf(0) }
    val timer by TimerService.state.collectAsStateWithLifecycle()
    val discard = rememberConfirm<Session>()
    fun start() = vm.startItem(item) { s -> nav.navigate(when { s.itemType == ItemType.PROGRAM -> Routes.session(s.id); item.cardio?.intervals == true -> Routes.timer(s.id); else -> Routes.cardio(s.id) }) }
    fun resume(active: Session) = nav.navigate(if (active.itemType == ItemType.PROGRAM) Routes.session(active.id) else if (TimerService.state.value?.sessionId == active.id) Routes.timer(active.id) else Routes.cardio(active.id))
    val startRequested by dev.mirzohidkhon.khonfitness.MainActivity.startRequested.collectAsStateWithLifecycle()
    LaunchedEffect(startRequested, unfinished, item.name, loaded) {
        if (!startRequested || !loaded) return@LaunchedEffect
        dev.mirzohidkhon.khonfitness.MainActivity.startRequested.value = false
        val active = unfinished
        if (active != null) resume(active) else if (!item.isRest) start()
    }
    LaunchedEffect(item.name, unfinished?.id) { dev.mirzohidkhon.khonfitness.widget.TodayWidget.refresh(context) }
    val stretchRoutines by vm.stretchRoutines.collectAsStateWithLifecycle()
    val routineStretches by vm.routineStretches.collectAsStateWithLifecycle()
    val stretches by vm.stretches.collectAsStateWithLifecycle()
    val stretchSessions by vm.stretchSessions.collectAsStateWithLifecycle()
    val eatingWindow by vm.eatingWindow.collectAsStateWithLifecycle()
    val windowDays by vm.windowDays.collectAsStateWithLifecycle()
    var windowSheet by remember { mutableStateOf(false) }
    val windowRequested by dev.mirzohidkhon.khonfitness.MainActivity.windowRequested.collectAsStateWithLifecycle()
    LaunchedEffect(windowRequested) { if (windowRequested) { dev.mirzohidkhon.khonfitness.MainActivity.windowRequested.value = false; windowSheet = true } }
    val now = rememberMinuteClock()
    val update by vm.update.collectAsStateWithLifecycle()
    val updateProgress by vm.updateProgress.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.checkUpdate() }
    val notifLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }
    LaunchedEffect(Unit) { pendingBand = runCatching { dev.mirzohidkhon.khonfitness.health.HealthImport.pendingCount(context, vm.repo.importedSourceIds().toSet()) }.getOrDefault(0) }

    TabScreen("Today", trailing = { BarCircleButton(Icons.Gear, "Settings") { nav.navigate(Routes.SETTINGS) } }) {
        WeekStrip(today, plan, modalities) { planDate = it }
        if (loaded) {
            val active = unfinished
            val done = sessions.firstOrNull { it.date == today.iso() && it.finished && ((item.program != null && it.programId == item.program.id) || (item.cardio != null && it.exerciseId == item.cardio.id)) }
            Spacer(Modifier.height(16.dp))
            TodayCard(
                item, preview(item, blocks, blockExercises, modalities), active,
                progress = active?.takeIf { it.itemType == ItemType.PROGRAM }?.let { setProgress(it.id, logs) },
                doneText = done?.let { doneLine(it, logs) }, modalities = modalities, exercises = exercises, now = now,
                onStart = ::start, onResume = ::resume,
                timerStopped = active != null && timerStopped(active, timer?.sessionId), onDiscard = { discard.ask(it) },
            )
        }
        Spacer(Modifier.height(16.dp))
        GroupedList {
            val updateRow = updateRow(update, updateProgress)
            if (updateRow != null) HomeRow(Icons.Download, K.Accent, updateRow.title, updateRow.line, K.Accent, divider = false) {
                // One download at a time: a tap while it runs does nothing.
                if (!updateRow.downloading) vm.runUpdate(context, onNeedPermission = { nav.navigate(Routes.SETTINGS) }, onError = { })
            }
            val routine = stretchRoutines.find { it.active } ?: stretchRoutines.firstOrNull()
            if (routine != null) {
                val stretchedToday = stretchSessions.any { it.date == today.iso() }
                val summary = routineSummary(routineSeconds(routine.id, routineStretches, stretches), routineStretches.count { it.routineId == routine.id })
                HomeRow(
                    Icons.Stretch, K.Green, routine.name, routineLine(summary, stretchedToday), if (stretchedToday) K.Green else K.Muted, divider = updateRow != null,
                    trailing = if (stretchedToday) { { DoneCheck() } } else null,
                ) { nav.navigate(Routes.stretch(routine.id)) }
            }
            val windowDay = windowDays.find { it.date == today.iso() }
            val clock = java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(now), java.time.ZoneId.systemDefault())
            val open = windowSummary(eatingWindow, windowDay, clock).first
            HomeRow(Icons.ForkKnife, if (open) K.Green else K.Dim, "Eating window", windowStatus(eatingWindow, windowDay, clock), if (open) K.Green else K.Muted, divider = updateRow != null || routine != null) { windowSheet = true }
            if (pendingBand > 0) HomeRow(Icons.Heart, K.Red, pendingTitle(pendingBand), PENDING_LINE, divider = true) { nav.navigate(Routes.IMPORT) }
        }
        BodyweightSection(today, bodyweights) { kg -> vm.run { vm.repo.saveBodyweight(today.iso(), kg) } }
    }
    planDate?.let { date -> PlanSheet(vm, date, plan, modalities) { planDate = null } }
    if (windowSheet) WindowSheet(vm) { windowSheet = false }
    ConfirmSheet(discard, title = { "Discard this session?" }, body = { "The timer's time and rounds are not kept." }, action = "Discard") { s -> vm.run { vm.repo.deleteSession(s.id) } }
}

/** Change what a date holds: a program, a cardio exercise, or rest. */
@Composable
fun PlanSheet(vm: AppViewModel, date: LocalDate, plan: KhonRepository.PlanData, modalities: List<Modality>, onDismiss: () -> Unit) {
    val current = plan.itemFor(date)
    val cardio = plan.exercises.filter { it.kind == Kind.CARDIO && !it.archived }
    fun pick(type: String, id: String?) { vm.run { vm.repo.saveOverride(date.iso(), type, id) }; onDismiss() }
    Sheet(date.format(longDate), onDismiss) {
        SheetGroupTitle("Programs")
        ChoiceList { plan.programs.forEachIndexed { i, p -> ChoiceRow(p.name, current.program?.id == p.id, K.Accent, divider = i > 0) { pick(ItemType.PROGRAM, p.id) } } }
        SheetGroupTitle("Cardio exercises")
        ChoiceList { cardio.forEachIndexed { i, e -> ChoiceRow(e.name, current.cardio?.id == e.id, exerciseColor(e, modalities), e.intensity == Intensity.HIGH, divider = i > 0) { pick(ItemType.CARDIO, e.id) } } }
        SheetGroupTitle("Rest")
        ChoiceList { ChoiceRow("Rest", current.isRest, K.Dim, divider = false) { pick(ItemType.REST, null) } }
        if (current.overridden) Row(Modifier.padding(top = 12.dp)) { TextButton("Back to weekly template") { vm.run { vm.repo.clearOverride(date.iso()) }; onDismiss() } }
    }
}
