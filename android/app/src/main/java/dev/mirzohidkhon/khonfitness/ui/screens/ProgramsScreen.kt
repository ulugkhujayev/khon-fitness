package dev.mirzohidkhon.khonfitness.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
 * Programs tab: the programs (the active one filled), then rows that open a part of the app, each with an icon
 * tile like iOS Settings: Library (exercises, stretching, modalities), Plan (week plan), and Settings on its own.
 * A new program is only a draft in its editor until Done or its first block saves it.
 */
@Composable
fun ProgramsScreen(vm: AppViewModel, nav: NavHostController) {
    val programs by vm.programs.collectAsStateWithLifecycle()
    val blocks by vm.blocks.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    TabScreen("Programs", trailing = { BarCircleButton(Icons.Plus, "New program") { nav.navigate(Routes.program("new")) } }) {
        GroupedList {
            programs.forEachIndexed { i, p ->
                ListRow(p.name, secondary = blocksText(blocks.count { it.programId == p.id }), dotColor = K.Accent, dotFilled = p.active, divider = i > 0) { nav.navigate(Routes.program(p.id)) }
            }
            if (programs.isEmpty()) ListRow("No programs yet", chevron = false, divider = false, titleColor = K.Muted)
        }
        SectionTitle("Library", Modifier.padding(top = 24.dp))
        GroupedList {
            ListRow("Exercises", secondary = "${exercises.count { !it.archived }}", divider = false, leading = { IconTile(Icons.Dumbbell, K.Accent) }) { nav.navigate(Routes.LIBRARY) }
            ListRow("Stretching", leading = { IconTile(Icons.Stretch, K.Green) }) { nav.navigate(Routes.ROUTINES) }
            ListRow("Modalities", secondary = "${modalities.count { !it.archived }}", leading = { IconTile(Icons.Runner, K.Blue) }) { nav.navigate(Routes.MODALITIES) }
        }
        SectionTitle("Plan", Modifier.padding(top = 24.dp))
        GroupedList { ListRow("Week plan", divider = false, leading = { IconTile(Icons.Today, K.Red) }) { nav.navigate(Routes.WEEK_PLAN) } }
        // Gray like the system's own Settings icon.
        GroupedList(Modifier.padding(top = 32.dp)) { ListRow("Settings", divider = false, leading = { IconTile(Icons.Gear, K.Dim) }) { nav.navigate(Routes.SETTINGS) } }
    }
}

@Composable
fun LibraryScreen(vm: AppViewModel, nav: NavHostController) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf("") }
    var muscle by rememberSaveable { mutableStateOf("") }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var muscleSheet by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        val barScroll = rememberScrollState()
        EditorTopBar("Programs", "Exercises", scrollPx = { barScroll.value }, onBack = { nav.popBackStack() }, done = "+", addLabel = "New exercise") { nav.navigate(Routes.exercise("new")) }
        Column(Modifier.weight(1f).verticalScroll(barScroll).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            SearchField(query, { query = it })
            Spacer(Modifier.height(12.dp))
            Segmented(listOf("All", "Strength", "Cardio"), when (kind) { Kind.STRENGTH -> 1; Kind.CARDIO -> 2; else -> 0 }) { kind = when (it) { 1 -> Kind.STRENGTH; 2 -> Kind.CARDIO; else -> "" } }
            if (kind != Kind.CARDIO) {
                Spacer(Modifier.height(12.dp))
                GroupedList { FieldRow("Muscle", divider = false, onClick = { muscleSheet = true }) { FieldValue(if (muscle.isEmpty()) "Any" else muscle.replaceFirstChar { it.uppercase() }) } }
            }
            Spacer(Modifier.height(16.dp))
            val list = exercises.filter { (showArchived || !it.archived) && (kind.isEmpty() || it.kind == kind) && (muscle.isEmpty() || it.primaryMuscle == muscle || muscle in it.secondaryList) && (query.isBlank() || it.name.contains(query.trim(), true)) }
            GroupedList {
                list.forEachIndexed { i, e ->
                    val secondary = if (e.kind == Kind.STRENGTH) e.primaryMuscle?.replaceFirstChar { it.uppercase() } else modalities.find { it.id == e.modalityId }?.name
                    ListRow(e.name, secondary = secondary, dotColor = exerciseColor(e, modalities), dotFilled = e.kind == Kind.STRENGTH || e.intensity == Intensity.HIGH, divider = i > 0, titleColor = if (e.archived) K.Dim else K.Text) { nav.navigate(Routes.exercise(e.id)) }
                }
                if (list.isEmpty()) SearchEmpty(query)
            }
            Spacer(Modifier.height(16.dp))
            GroupedList { ToggleRow("Show archived", showArchived, divider = false) { showArchived = it } }
        }
    }
    if (muscleSheet) ChoiceSheet(
        "Muscle", listOf(Choice("", "Any")) + MUSCLE_GROUPS.map { Choice(it, it.replaceFirstChar { c -> c.uppercase() }) },
        onPick = { muscle = it; muscleSheet = false }, onDismiss = { muscleSheet = false }, isSelected = { it == muscle },
    )
}

/**
 * Modalities: each with its color and how many exercises it groups, a note on what they are for, and the archived
 * ones in their own group when shown.
 */
@Composable
fun ModalitiesScreen(vm: AppViewModel, nav: NavHostController) {
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    var showArchived by rememberSaveable { mutableStateOf(false) }
    val (archived, live) = modalities.partition { it.archived }
    @Composable fun ModalityRow(m: Modality, i: Int) = ListRow(m.name, secondary = modalityUse(m.id, exercises), dotColor = androidx.compose.ui.graphics.Color(m.color), divider = i > 0,
        titleColor = if (m.archived) K.Dim else K.Text) { nav.navigate(Routes.modality(m.id)) }
    Column(Modifier.fillMaxSize()) {
        val barScroll = rememberScrollState()
        EditorTopBar("Programs", "Modalities", scrollPx = { barScroll.value }, onBack = { nav.popBackStack() }, done = "+", addLabel = "New modality") { nav.navigate(Routes.modality("new")) }
        Column(Modifier.weight(1f).verticalScroll(barScroll).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            GroupedList {
                live.forEachIndexed { i, m -> ModalityRow(m, i) }
                if (live.isEmpty()) ListRow("No modalities yet", chevron = false, divider = false, titleColor = K.Muted)
            }
            GroupFooter("A modality groups cardio exercises, such as running or cycling, and colors them on Today and in History.")
            GroupedList(Modifier.padding(top = 24.dp)) { ToggleRow("Show archived", showArchived, divider = false) { showArchived = it } }
            if (showArchived && archived.isNotEmpty()) {
                SectionTitle("Archived", Modifier.padding(top = 24.dp))
                GroupedList { archived.forEachIndexed { i, m -> ModalityRow(m, i) } }
            }
        }
    }
}

/**
 * The weekly template: the week at a glance, then each weekday with an icon tile of what it holds in its color
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
    EditorTopBar("Programs", "Week plan", scrollPx = { barScroll.value }, onBack = { nav.popBackStack() })
    Column(Modifier.weight(1f).verticalScroll(barScroll).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
    SectionTitle(plan.weekSummary())
    GroupedList {
        for (wd in 1..7) {
            val item = plan.templateItem(wd)
            val icon = when { item.isRest -> Icons.Moon; item.itemType == ItemType.PROGRAM -> Icons.Dumbbell; else -> Icons.Runner }
            ListRow(DayOfWeek.of(wd).getDisplayName(TextStyle.FULL, Locale.ENGLISH), secondary = item.name, divider = wd > 1, titleColor = if (wd == today) K.Accent else K.Text,
                leading = { IconTile(icon, itemColor(item, modalities)?.first ?: K.Dim) }) { editing = wd }
        }
    }
    GroupFooter("A change applies from today on; past days keep the plan they had. To change one date only, tap it on Today.")
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

/** What the backup footnote says after an export, import or restore; red for an error. */
private data class BackupStatus(val text: String, val error: Boolean = false)

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
        if (uri != null) vm.run {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return@run
            runCatching { vm.repo.describeBackup(text) }.onSuccess { pendingImport = text to it }.onFailure { backupStatus = BackupStatus("Not a Khon backup: ${it.message}", error = true) }
        }
    }
    fun applyImport(text: String, replace: Boolean) = vm.run {
        runCatching {
            val dir = java.io.File(context.getExternalFilesDir(null), "backups").apply { mkdirs() }
            java.io.File(dir, "before-import-${java.time.LocalDateTime.now().withNano(0).toString().replace(':', '-')}.json").writeText(vm.repo.exportJson())
            vm.repo.importJson(text, replace)
        }.onSuccess { backupStatus = BackupStatus(if (replace) "Backup restored: everything was replaced." else "Backup merged.") }
            .onFailure { backupStatus = BackupStatus("Import failed: ${it.message}", error = true) }
        pendingImport = null
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.run {
            runCatching {
                val json = vm.repo.exportJson()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }.onSuccess { backupStatus = BackupStatus("Backup exported.") }.onFailure { backupStatus = BackupStatus("Export failed: ${it.message}", error = true) }
        }
    }
    val backups = remember(backupStatus) { java.io.File(context.getExternalFilesDir(null), "backups").listFiles()?.filter { it.name.endsWith(".json") }?.sortedByDescending { it.name } ?: emptyList() }

    SectionTitle("Backup")
    GroupedList {
        ListRow("Export backup", divider = false, chevron = false, leading = { IconTile(Icons.Upload, K.Blue) }) { exportLauncher.launch("khon-fitness-${java.time.LocalDate.now()}.json") }
        ListRow("Import backup", chevron = false, leading = { IconTile(Icons.Download, K.Green) }) { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
        if (backups.isNotEmpty()) ListRow("Restore a saved copy", secondary = "${backups.size}", leading = { IconTile(Icons.Restore, K.Dim) }) { restoreSheet = true }
    }
    GroupFooter(
        backupStatus?.text ?: "One JSON file with every program, session, bodyweight, plan and stretch. Import shows what a file holds, then merges or replaces; a copy of the current data is saved first.",
        error = backupStatus?.error == true,
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
        PrimaryButton("Merge") { applyImport(text, replace = false) }
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Replace", color = K.Red, textColor = K.Text) { applyImport(text, replace = true) }
        Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) { TextButton("Cancel", color = K.Muted) { pendingImport = null } }
    } }
    if (restoreSheet) Sheet("Restore a saved copy", { restoreSheet = false }) {
        Text("Copies the app wrote before an import. Restoring replaces everything with that copy; a new copy of the current data is written first.", color = K.Muted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        ChoiceList { backups.forEachIndexed { i, f -> ChoiceRow(f.name.removePrefix("before-import-").removeSuffix(".json").replace('T', ' '), false, divider = i > 0) { restoreSheet = false; applyImport(f.readText(), replace = true) } } }
    }
    if (confirmReset) ConfirmSheet("Reset to built-in data?", "Every session, bodyweight, program and setting goes back to the built-in data.", "Reset", onDismiss = { confirmReset = false }) {
        confirmReset = false
        vm.run { vm.repo.resetToSeed(); backupStatus = BackupStatus("Reset to built-in data.") }
    }
}
