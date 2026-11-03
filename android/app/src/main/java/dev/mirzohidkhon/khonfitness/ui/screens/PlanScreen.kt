package dev.mirzohidkhon.khonfitness.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.Icons
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K
import dev.mirzohidkhon.khonfitness.BuildConfig
import dev.mirzohidkhon.khonfitness.update.Updater
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Plan tab: "+" adds a gym program, cardio workout, stretch routine or exercise, each through its own create flow.
 * Then This week (the week as the plan resolves it, with Edit for the weekly template), the gym programs (the active
 * one filled), the cardio workouts with their type, the stretch routines, and the Library. Settings is behind the
 * gear on Today. A new program is only a draft in its editor until Done or its first block saves it.
 */
@Composable
fun PlanScreen(vm: AppViewModel, nav: NavHostController) {
    val plan by vm.planData.collectAsStateWithLifecycle()
    val programs by vm.programs.collectAsStateWithLifecycle()
    val blocks by vm.blocks.collectAsStateWithLifecycle()
    val blockExercises by vm.blockExercises.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    val routines by vm.stretchRoutines.collectAsStateWithLifecycle()
    val routineStretches by vm.routineStretches.collectAsStateWithLifecycle()
    val stretches by vm.stretches.collectAsStateWithLifecycle()
    var adding by rememberSaveable { mutableStateOf(false) }
    // This screen's back stack entry; a new routine's editor opens only if Plan is still on top when the save ends.
    val self = LocalLifecycleOwner.current
    fun newRoutine() {
        if (vm.creatingRoutine) return
        vm.creatingRoutine = true
        val id = newId()
        vm.run {
            try { vm.repo.createNewRoutine(id) } finally { vm.creatingRoutine = false }
            if (nav.currentBackStackEntry === self) nav.navigate(Routes.routine(id))
        }
    }
    TabScreen("Plan", trailing = { TitleAddButton("New") { adding = true } }) {
        SectionTitle("This week", trailing = { SectionAction("Edit", "Edit week plan") { nav.navigate(Routes.WEEK_PLAN) } })
        val cells = plan.weekCells(vm.today(), modalities)
        WeekGrid(cells, cells.map { it.spoken(plan.itemFor(it.date).name) })

        SectionTitle("Gym programs", Modifier.padding(top = 24.dp))
        GroupedList {
            programs.forEachIndexed { i, p ->
                val mine = blocks.filter { it.programId == p.id }
                val sets = mine.sumOf { b -> blockExercises.filter { it.blockId == b.id }.sumOf { it.sets } }
                PlanRow(p.name, programLine(mine.size, sets, plan.weekdaysOf(ItemType.PROGRAM, p.id)), divider = i > 0, leading = { DotMark(K.Accent, p.active) }) { nav.navigate(Routes.program(p.id)) }
            }
            if (programs.isEmpty()) {
                ListRow("No programs yet", chevron = false, divider = false, titleColor = K.Muted)
                ListRow("Create program", titleColor = K.Accent) { nav.navigate(Routes.program("new")) }
            }
        }

        val (archivedCardio, cardio) = exercises.filter { it.kind == Kind.CARDIO }.partition { it.archived }
        @Composable fun CardioRow(e: Exercise, i: Int) {
            val type = modalities.find { it.id == e.modalityId }
            PlanRow(e.name, cardioLine(e, plan.weekdaysOf(ItemType.CARDIO, e.id)), divider = i > 0, titleColor = if (e.archived) K.Dim else K.Text,
                leading = { TypePill(type?.name ?: "Cardio", type?.let { Color(it.color) } ?: K.Muted) }) { nav.navigate(Routes.exercise(e.id)) }
        }
        SectionTitle("Cardio workouts", Modifier.padding(top = 24.dp))
        GroupedList {
            cardio.forEachIndexed { i, e -> CardioRow(e, i) }
            if (cardio.isEmpty()) ListRow("No cardio workouts yet", chevron = false, divider = false, titleColor = K.Muted)
        }
        if (archivedCardio.isNotEmpty()) {
            SectionTitle("Archived", Modifier.padding(top = 16.dp))
            GroupedList { archivedCardio.forEachIndexed { i, e -> CardioRow(e, i) } }
        }

        SectionTitle("Stretch routines", Modifier.padding(top = 24.dp))
        GroupedList {
            routines.forEachIndexed { i, r ->
                PlanRow(r.name, routineSummary(r.id, routineStretches, stretches), divider = i > 0, leading = { DotMark(K.Green, r.active) }) { nav.navigate(Routes.routine(r.id)) }
            }
            if (routines.isEmpty()) ListRow("No routines yet", chevron = false, divider = false, titleColor = K.Muted)
        }

        SectionTitle("Library", Modifier.padding(top = 24.dp))
        GroupedList {
            ListRow("Exercises", secondary = "${exercises.count { it.kind == Kind.STRENGTH && !it.archived }}", divider = false, leading = { IconTile(Icons.Dumbbell, K.Accent) }) { nav.navigate(Routes.LIBRARY) }
            ListRow("Stretches", secondary = "${stretches.count { !it.archived }}", leading = { IconTile(Icons.Stretch, K.Green) }) { nav.navigate(Routes.STRETCHES) }
        }
    }
    if (adding) Sheet("New", { adding = false }) {
        Text("What do you want to add?", color = K.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))
        ChoiceList {
            fun open(route: String) { adding = false; nav.navigate(route) }
            ListRow("Gym program", divider = false, leading = { IconTile(Icons.Dumbbell, K.Accent) }) { open(Routes.program("new")) }
            ListRow("Cardio workout", leading = { IconTile(Icons.Runner, K.Blue) }) { open(Routes.exercise(NEW_CARDIO)) }
            ListRow("Stretch routine", leading = { IconTile(Icons.Stretch, K.Green) }) { adding = false; newRoutine() }
            ListRow("Exercise", leading = { IconTile(Icons.Dumbbell, K.Dim) }) { open(Routes.exercise("new")) }
        }
    }
}

/** The tab title's round "+": a 36 dp circle on a 48 dp target, which may reach past the title row's height. */
@Composable
private fun TitleAddButton(label: String, onClick: () -> Unit) {
    Box(Modifier.requiredSize(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(K.Surface), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(Icons.Plus, contentDescription = label, tint = K.Accent, modifier = Modifier.size(18.dp))
        }
    }
}

/** A section title's action in accent text on a 48 dp target, spoken as [description]. */
@Composable
private fun SectionAction(text: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }.padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = K.Accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, modifier = Modifier.clearAndSetSemantics {}) }
}

/**
 * The week Monday to Sunday in seven cells: the weekday, the item's short name (muted for rest), today's cell tinted
 * accent, and a small accent dot under a day that a one-day change set. Each cell reads as one spoken line.
 */
@Composable
private fun WeekGrid(cells: List<WeekCell>, spoken: List<String>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        cells.forEachIndexed { i, c ->
            Column(
                Modifier.weight(1f).heightIn(min = 68.dp).clip(RoundedCornerShape(12.dp)).background(if (c.today) K.AccentSoft else K.Surface)
                    .clearAndSetSemantics { contentDescription = spoken[i] }.padding(horizontal = 2.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(weekdayShort(c.date.dayOfWeek.value), fontSize = 11.sp, color = if (c.today) K.Accent else K.Muted, maxLines = 1)
                Text(c.name, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, color = if (c.rest) K.Muted else K.Text, textAlign = TextAlign.Center,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                if (c.changed) Box(Modifier.padding(top = 4.dp).size(5.dp).clip(CircleShape).background(K.Accent))
            }
        }
    }
}

/** A Plan list row: [leading] mark, the title over its muted [line], a chevron. The whole row opens [onClick]. */
@Composable
private fun PlanRow(title: String, line: String, divider: Boolean, titleColor: Color = K.Text, leading: @Composable () -> Unit, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        if (divider) HorizontalDivider(Modifier.padding(start = 16.dp), color = K.Divider, thickness = 0.7.dp)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(start = 16.dp, end = 14.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(line, style = MaterialTheme.typography.bodyMedium, color = K.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
            Chevron()
        }
    }
}

/** A program's or routine's dot: filled when it is the active one. */
@Composable
private fun DotMark(color: Color, filled: Boolean) { Box(Modifier.width(14.dp), contentAlignment = Alignment.CenterStart) { Dot(color, filled) } }

/** A cardio workout's type as a pill in the type's color. */
@Composable
private fun TypePill(name: String, color: Color) {
    Text(name, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(min = 44.dp, max = 72.dp).clip(RoundedCornerShape(9.dp)).background(K.Surface2).padding(horizontal = 8.dp, vertical = 3.dp))
}

/**
 * Exercises: the strength library, searchable, grouped by primary muscle, archived ones last ([libraryGroups]).
 * Each row opens the exercise editor; "+" adds an exercise. Cardio workouts are on Plan.
 */
@Composable
fun LibraryScreen(vm: AppViewModel, nav: NavHostController) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        val barScroll = rememberScrollState()
        EditorTopBar("Plan", "Exercises", scrollPx = { barScroll.value }, onBack = { nav.popBackStack() }, done = "+", addLabel = "New exercise") { nav.navigate(Routes.exercise("new")) }
        Column(Modifier.weight(1f).verticalScroll(barScroll).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            SearchField(query, { query = it })
            val groups = libraryGroups(exercises, query)
            groups.forEachIndexed { g, (title, list) ->
                SectionTitle(title, Modifier.padding(top = if (g == 0) 16.dp else 24.dp))
                GroupedList {
                    list.forEachIndexed { i, e -> ListRow(e.name, divider = i > 0, titleColor = if (e.archived) K.Dim else K.Text) { nav.navigate(Routes.exercise(e.id)) } }
                }
            }
            if (groups.isEmpty()) GroupedList(Modifier.padding(top = 16.dp)) { SearchEmpty(query) }
        }
    }
}

/**
 * The weekly template: what it is for ([WEEK_PLAN_NOTE]), the week at a glance, then each weekday with an icon tile of what it holds in its color
 * (today's name in accent), and a note on when a change applies. A change fills today and later dates only:
 * [KhonRepository.saveWeekPlan] freezes the past days of that weekday first.
 */
@Composable
fun WeekPlanScreen(vm: AppViewModel, nav: NavHostController) {
    val plan by vm.planData.collectAsStateWithLifecycle()
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Int?>(null) }
    val today = vm.today().dayOfWeek.value
    Column(Modifier.fillMaxSize()) {
    val barScroll = rememberScrollState()
    EditorTopBar("Plan", "Week plan", scrollPx = { barScroll.value }, onBack = { nav.popBackStack() })
    Column(Modifier.weight(1f).verticalScroll(barScroll).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
    Text(WEEK_PLAN_NOTE, color = K.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
    SectionTitle(plan.weekSummary())
    GroupedList {
        for (wd in 1..7) {
            val item = plan.templateItem(wd)
            val icon = when { item.isRest -> Icons.Moon; item.itemType == ItemType.PROGRAM -> Icons.Dumbbell; else -> Icons.Runner }
            ListRow(DayOfWeek.of(wd).getDisplayName(TextStyle.FULL, Locale.ENGLISH), secondary = item.name, divider = wd > 1, titleColor = if (wd == today) K.Accent else K.Text,
                leading = { IconTile(icon, itemColor(item, modalities)?.first ?: K.Dim) }) { editing = wd }
        }
    }
    GroupFooter("A change applies from today on; past days keep the plan they had.")
    } }
    editing?.let { wd ->
        val current = plan.plan.find { it.weekday == wd }
        fun pick(type: String, id: String?) { vm.run { vm.repo.saveWeekPlan(wd, type, id) }; editing = null }
        Sheet(DayOfWeek.of(wd).getDisplayName(TextStyle.FULL, Locale.ENGLISH), { editing = null }) {
            SheetGroupTitle("Programs")
            ChoiceList { plan.programs.forEachIndexed { i, p -> ChoiceRow(p.name, current?.itemType == ItemType.PROGRAM && current.itemId == p.id, K.Accent, divider = i > 0) { pick(ItemType.PROGRAM, p.id) } } }
            SheetGroupTitle("Cardio exercises")
            ChoiceList { plan.exercises.filter { it.kind == Kind.CARDIO && !it.archived }.forEachIndexed { i, e -> ChoiceRow(e.name, current?.itemType == ItemType.CARDIO && current.itemId == e.id, exerciseColor(e, modalities), e.intensity == Intensity.HIGH, divider = i > 0) { pick(ItemType.CARDIO, e.id) } } }
            SheetGroupTitle("Rest")
            ChoiceList { ChoiceRow("Rest", current == null || current.itemType == ItemType.REST, K.Dim, divider = false) { pick(ItemType.REST, null) } }
        }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavHostController) {
    Column(Modifier.fillMaxSize()) {
        val barScroll = rememberScrollState()
        EditorTopBar("Today", "Settings", scrollPx = { barScroll.value }, onBack = { nav.popBackStack() })
        Column(Modifier.weight(1f).verticalScroll(barScroll).padding(16.dp, 8.dp, 16.dp, 32.dp)) { Settings(vm) }
    }
}

/**
 * Settings in groups with icon tiles, as in iOS Settings: Backup (export, import, restore a saved copy, and what a
 * backup holds or how the last one went), App (version, the APK updater with its state, reopen after updates) and
 * Reset on its own, each with a footnote. Import says what the file holds, then asks Merge or Replace, and writes
 * a safety copy of the current data first.
 */
@Composable
private fun Settings(vm: AppViewModel) {
    val context = LocalContext.current
    var backupStatus by remember { mutableStateOf<BackupStatus?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    var restoreSheet by remember { mutableStateOf(false) }
    val update by vm.update.collectAsStateWithLifecycle()
    val progress by vm.updateProgress.collectAsStateWithLifecycle()
    val pendingFile by vm.pendingInstall.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.checkUpdate() }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { vm.installPending(context) }
    // No answer yet reads as "Checking…", so a new check clears the last one first.
    fun checkUpdate() { downloadError = null; vm.update.value = null; vm.checkUpdate(force = true) }
    var pendingImport by remember { mutableStateOf<Pair<String, String>?>(null) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        // Every file step runs inside runCatching, so a failure shows in the footnote ([backupFootnote]), never escapes.
        if (uri != null) vm.run {
            val read = runCatching {
                val text = context.contentResolver.openInputStream(uri).orNoFile().bufferedReader().use { it.readText() }
                text to vm.repo.describeBackup(text)
            }
            read.onSuccess { pendingImport = it }
            backupFootnote(BackupAction.OPEN, read)?.let { backupStatus = it }
        }
    }
    /** Reads the backup ([read]), writes a safety copy of the current data, then merges or replaces. */
    fun applyImport(action: BackupAction, read: () -> String) = vm.run {
        val result = runCatching {
            val text = read()
            val dir = java.io.File(context.getExternalFilesDir(null), "backups").apply { mkdirs() }
            java.io.File(dir, "before-import-${java.time.LocalDateTime.now().withNano(0).toString().replace(':', '-')}.json").writeText(vm.repo.exportJson())
            vm.repo.importJson(text, replace = action != BackupAction.MERGE)
        }
        backupStatus = backupFootnote(action, result)
        // The imported settings take effect at once: alarms re-armed and widgets redrawn.
        if (backupChangesData(action, result)) dev.mirzohidkhon.khonfitness.window.WindowScheduler.reschedule(context)
        pendingImport = null
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.run {
            backupStatus = backupFootnote(BackupAction.EXPORT, runCatching {
                val json = vm.repo.exportJson()
                context.contentResolver.openOutputStream(uri).orNoFile().use { it.write(json.toByteArray()) }
            })
        }
    }
    val listing = remember(backupStatus) { savedBackups(java.io.File(context.getExternalFilesDir(null), "backups")) }
    val backups = listing.getOrDefault(emptyList())
    val footnote = backupStatus ?: backupFootnote(BackupAction.LIST, listing)

    SectionTitle("Backup")
    GroupedList {
        ListRow("Export backup", divider = false, chevron = false, leading = { IconTile(Icons.Upload, K.Blue) }) { exportLauncher.launch("khon-fitness-${java.time.LocalDate.now()}.json") }
        ListRow("Import backup", chevron = false, leading = { IconTile(Icons.Download, K.Green) }) { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
        if (backups.isNotEmpty()) ListRow("Restore a saved copy", secondary = "${backups.size}", leading = { IconTile(Icons.Restore, K.Dim) }) { restoreSheet = true }
    }
    GroupFooter(
        footnote?.text ?: "One JSON file with every program, session, bodyweight, plan and stretch. Import shows what a file holds, then merges or replaces; a copy of the current data is saved first.",
        error = footnote?.error == true,
    )

    val status = updateStatus(update, progress, downloadError)
    SectionTitle("App", Modifier.padding(top = 24.dp))
    GroupedList {
        ListRow("Version", secondary = "v" + BuildConfig.VERSION_NAME, chevron = false, divider = false, leading = { IconTile(Icons.Info, K.Dim) })
        ListRow(status.title, secondary = status.value, chevron = false, titleColor = if (update is Updater.Result.Available) K.Accent else K.Text, leading = { IconTile(Icons.DownloadCircle, K.Accent) }) {
            if (update is Updater.Result.Available) { downloadError = null; vm.runUpdate(context, onNeedPermission = { }, onError = { downloadError = it }) } else checkUpdate()
        }
        if (android.os.Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(context)) ListRow("Reopen after updates", secondary = "allow once", leading = { IconTile(Icons.Reopen, K.Blue) }) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName)))
        }
    }
    status.note?.let { GroupFooter(it, error = status.error) }
    (update as? Updater.Result.Available)?.let { u ->
        Spacer(Modifier.height(16.dp))
        Text(u.release.name ?: u.release.tag_name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        u.release.body?.takeIf { it.isNotBlank() }?.let { Text(it, color = K.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp)) }
        if (pendingFile != null) {
            Spacer(Modifier.height(12.dp))
            Text("Android needs a one-time permission so this app can install its own updates.", color = K.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Allow and install") { permissionLauncher.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))) }
        }
    }

    GroupedList(Modifier.padding(top = 32.dp)) {
        ListRow("Reset to built-in data", divider = false, chevron = false, titleColor = K.Red, leading = { IconTile(Icons.Reset, K.Red) }) { confirmReset = true }
    }
    GroupFooter("Deletes every session, bodyweight and edit, and brings back the built-in programs and library.")

    pendingImport?.let { (text, summary) -> Sheet("Import backup?", { pendingImport = null }) {
        Text(summary, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Text("Merge keeps what is on the phone and adds or updates what the file has. Replace deletes everything on the phone first. Either way a copy of the current data goes to Android/data/${context.packageName}/files/backups.", color = K.Muted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Merge") { applyImport(BackupAction.MERGE) { text } }
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Replace", color = K.Red, textColor = K.Text) { applyImport(BackupAction.REPLACE) { text } }
        Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) { TextButton("Cancel", color = K.Muted) { pendingImport = null } }
    } }
    if (restoreSheet) Sheet("Restore a saved copy", { restoreSheet = false }) {
        Text("Copies the app wrote before an import. Restoring replaces everything with that copy; a new copy of the current data is written first.", color = K.Muted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        ChoiceList { backups.forEachIndexed { i, f -> ChoiceRow(f.name.removePrefix("before-import-").removeSuffix(".json").replace('T', ' '), false, divider = i > 0) { restoreSheet = false; applyImport(BackupAction.RESTORE) { f.readText() } } } }
    }
    if (confirmReset) ConfirmSheet("Reset to built-in data?", "Every session, bodyweight, program and setting goes back to the built-in data.", "Reset", onDismiss = { confirmReset = false }) {
        confirmReset = false
        vm.run { vm.repo.resetToSeed(); dev.mirzohidkhon.khonfitness.window.WindowScheduler.reschedule(context); backupStatus = BackupStatus("Reset to built-in data.") }
    }
}
