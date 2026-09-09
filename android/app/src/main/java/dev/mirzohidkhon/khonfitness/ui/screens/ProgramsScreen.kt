package dev.mirzohidkhon.khonfitness.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun ProgramsScreen(vm: AppViewModel, nav: NavHostController) {
    val programs by vm.programs.collectAsStateWithLifecycle()
    val blocks by vm.blocks.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 24.dp)) {
        ScreenTitle("Programs") { TextButton("+") { val id = newId(); vm.run { vm.repo.saveProgram(Program(id, "New program", active = programs.isEmpty(), sortOrder = programs.size)) }; nav.navigate(Routes.program(id)) } }
        GroupedList {
            programs.forEachIndexed { i, p ->
                ListRow(p.name, secondary = "${blocks.count { it.programId == p.id }} blocks", dotColor = K.Accent, dotFilled = p.active, divider = i > 0) { nav.navigate(Routes.program(p.id)) }
            }
            if (programs.isEmpty()) ListRow("No programs yet", chevron = false, divider = false, titleColor = K.Muted)
        }
        Spacer(Modifier.height(24.dp))
        GroupedList {
            ListRow("Exercises", secondary = "${exercises.count { !it.archived }}", divider = false) { nav.navigate(Routes.LIBRARY) }
            ListRow("Modalities", secondary = "${modalities.count { !it.archived }}") { nav.navigate(Routes.MODALITIES) }
            ListRow("Week plan") { nav.navigate(Routes.WEEK_PLAN) }
            ListRow("Settings") { nav.navigate(Routes.SETTINGS) }
        }
    }
}

@Composable
private fun ProgramList(vm: AppViewModel, nav: NavHostController) {
    val programs by vm.programs.collectAsStateWithLifecycle()
    val blocks by vm.blocks.collectAsStateWithLifecycle()
    SectionTitle("Programs") { TextButton("+") { val id = newId(); vm.run { vm.repo.saveProgram(Program(id, "New program", active = programs.isEmpty(), sortOrder = programs.size)) }; nav.navigate(Routes.program(id)) } }
    GroupedList {
        programs.forEachIndexed { i, p ->
            ListRow(p.name, secondary = "${blocks.count { it.programId == p.id }} blocks", dotColor = K.Accent, dotFilled = p.active, divider = i > 0) { nav.navigate(Routes.program(p.id)) }
        }
        if (programs.isEmpty()) ListRow("No programs yet", chevron = false, divider = false, titleColor = K.Muted)
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
        EditorTopBar("Programs", "Exercises", onBack = { nav.popBackStack() }, done = "+") { nav.navigate(Routes.exercise("new")) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            SearchField(query, { query = it })
            Spacer(Modifier.height(12.dp))
            Segmented(listOf("All", "Strength", "Cardio"), when (kind) { Kind.STRENGTH -> 1; Kind.CARDIO -> 2; else -> 0 }) { kind = when (it) { 1 -> Kind.STRENGTH; 2 -> Kind.CARDIO; else -> "" } }
            if (kind != Kind.CARDIO) {
                Spacer(Modifier.height(12.dp))
                GroupedList { FieldRow("Muscle", divider = false, onClick = { muscleSheet = true }) { Text(if (muscle.isEmpty()) "Any" else muscle.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold); Chevron() } }
            }
            Spacer(Modifier.height(16.dp))
            val list = exercises.filter { (showArchived || !it.archived) && (kind.isEmpty() || it.kind == kind) && (muscle.isEmpty() || it.primaryMuscle == muscle || muscle in it.secondaryList) && (query.isBlank() || it.name.contains(query.trim(), true)) }
            GroupedList {
                list.forEachIndexed { i, e ->
                    val secondary = if (e.kind == Kind.STRENGTH) e.primaryMuscle?.replaceFirstChar { it.uppercase() } else modalities.find { it.id == e.modalityId }?.name
                    ListRow(e.name, secondary = secondary, dotColor = exerciseColor(e, modalities), dotFilled = e.kind == Kind.STRENGTH || e.intensity == Intensity.HIGH, divider = i > 0, titleColor = if (e.archived) K.Dim else K.Text) { nav.navigate(Routes.exercise(e.id)) }
                }
                if (list.isEmpty()) ListRow("Nothing matches", chevron = false, divider = false, titleColor = K.Muted)
            }
            Spacer(Modifier.height(16.dp))
            GroupedList { FieldRow("Show archived", divider = false) { androidx.compose.material3.Switch(showArchived, { showArchived = it }, colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = K.Green, checkedThumbColor = androidx.compose.ui.graphics.Color.White)) } }
        }
    }
    if (muscleSheet) Sheet("Muscle", { muscleSheet = false }) { ChoiceList {
        ChoiceRow("Any", muscle.isEmpty(), divider = false) { muscle = ""; muscleSheet = false }
        MUSCLE_GROUPS.forEach { m -> ChoiceRow(m.replaceFirstChar { it.uppercase() }, muscle == m) { muscle = m; muscleSheet = false } }
    } }
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(10.dp)).background(K.Surface).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Icon(Icons.Search, contentDescription = null, tint = K.Dim, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        androidx.compose.foundation.text.BasicTextField(value, onChange, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = K.Text), cursorBrush = androidx.compose.ui.graphics.SolidColor(K.Accent), modifier = Modifier.weight(1f),
            decorationBox = { inner -> Box { if (value.isEmpty()) Text("Search", color = K.Dim, style = MaterialTheme.typography.bodyLarge); inner() } })
        if (value.isNotEmpty()) Text("×", color = K.Muted, fontSize = 20.sp, modifier = Modifier.clickable { onChange("") }.padding(4.dp))
    }
}

/** Fixed-width segmented control: every option gets an equal share, nothing scrolls. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(10.dp)).background(K.Surface).padding(3.dp)) {
        options.forEachIndexed { i, label ->
            val active = i == selected
            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(if (active) K.Surface3 else androidx.compose.ui.graphics.Color.Transparent).clickable { onSelect(i) }, contentAlignment = Alignment.Center) {
                Text(label, color = if (active) K.Text else K.Muted, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun Chip(text: String, active: Boolean, onClick: () -> Unit) {
    Box(Modifier.height(34.dp).clip(RoundedCornerShape(9.dp)).background(if (active) K.AccentSoft else K.Surface).clickable(onClick = onClick).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
        Text(text, color = if (active) K.Accent else K.Muted, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ModalitiesScreen(vm: AppViewModel, nav: NavHostController) {
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        EditorTopBar("Programs", "Modalities", onBack = { nav.popBackStack() }, done = "+") { nav.navigate(Routes.modality("new")) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            GroupedList { modalities.filter { !it.archived }.forEachIndexed { i, m -> ListRow(m.name, dotColor = androidx.compose.ui.graphics.Color(m.color), divider = i > 0) { nav.navigate(Routes.modality(m.id)) } } }
        }
    }
}

@Composable
fun WeekPlanScreen(vm: AppViewModel, nav: NavHostController) {
    val plan by vm.planData.collectAsStateWithLifecycle()
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Int?>(null) }
    Column(Modifier.fillMaxSize()) {
    EditorTopBar("Programs", "Week plan", onBack = { nav.popBackStack() })
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
    GroupedList {
        for (wd in 1..7) {
            val w = plan.plan.find { it.weekday == wd }
            val item = when (w?.itemType) {
                ItemType.PROGRAM -> plan.programs.find { it.id == w.itemId }?.let { PlanItem(ItemType.PROGRAM, program = it) }
                ItemType.CARDIO -> plan.exercises.find { it.id == w.itemId }?.let { PlanItem(ItemType.CARDIO, cardio = it) }
                else -> null
            } ?: PlanItem(ItemType.REST)
            val c = itemColor(item, modalities)
            ListRow(DayOfWeek.of(wd).getDisplayName(TextStyle.FULL, Locale.ENGLISH), secondary = item.name, dotColor = c?.first ?: K.Dim, dotFilled = c?.second ?: false, divider = wd > 1) { editing = wd }
        }
    }
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
        EditorTopBar("Today", "Settings", onBack = { nav.popBackStack() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp, 8.dp, 16.dp, 32.dp)) { Settings(vm) }
    }
}

@Composable
private fun Settings(vm: AppViewModel) {
    val context = LocalContext.current
    var toast by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    val update by vm.update.collectAsStateWithLifecycle()
    val progress by vm.updateProgress.collectAsStateWithLifecycle()
    val pendingFile by vm.pendingInstall.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.checkUpdate() }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { vm.installPending(context) }
    val updateHint = when (val u = update) { is Updater.Result.Available -> " → " + u.release.tag_name; is Updater.Result.UpToDate -> " · latest"; else -> "" }
    fun checkUpdate() { toast = "Checking…"; vm.checkUpdate(force = true) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.run {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return@run
            runCatching { vm.repo.importJson(text, replace = true) }.onSuccess { toast = "Imported" }.onFailure { toast = "Import failed: ${it.message}" }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.run {
            val json = vm.repo.exportJson()
            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            toast = "Exported"
        }
    }
    LaunchedEffect(update) { toast = when (val u = update) { is Updater.Result.UpToDate -> "v${u.current} is the latest"; is Updater.Result.Available -> null; is Updater.Result.Failed -> u.message; null -> null } }
    GroupedList {
        ListRow("Export backup", divider = false) { exportLauncher.launch("khon-fitness-${java.time.LocalDate.now()}.json") }
        ListRow("Import backup") { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
        (update as? Updater.Result.Available)?.let { u ->
            ListRow("Update to ${u.release.tag_name}", secondary = if (progress != null) "${((progress ?: 0f) * 100).toInt()}%" else "v" + BuildConfig.VERSION_NAME + updateHint, titleColor = K.Accent) {
                if (progress == null) vm.runUpdate(context, onNeedPermission = { }, onError = { toast = it })
            }
        } ?: ListRow("Check for update", secondary = "v" + BuildConfig.VERSION_NAME + updateHint) { checkUpdate() }
        if (android.os.Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(context)) ListRow("Reopen after updates", secondary = "allow once") {
            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName)))
        }
        ListRow("Reset to seed data", titleColor = K.Red, chevron = false) { confirmReset = true }
    }
    toast?.let { Text(it, color = K.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp)) }
    (update as? Updater.Result.Available)?.let { u ->
        Spacer(Modifier.height(16.dp))
        Text(u.release.name ?: u.release.tag_name, style = MaterialTheme.typography.titleMedium)
        u.release.body?.takeIf { it.isNotBlank() }?.let { Text(it, color = K.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp)) }
        if (pendingFile != null) {
            Spacer(Modifier.height(12.dp))
            Text("Android needs a one-time permission so this app can install its own updates.", color = K.Muted, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Allow and install") { permissionLauncher.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))) }
        }
    }
    if (confirmReset) Sheet("Reset all data?", { confirmReset = false }) {
        Text("Every session, program, and setting goes back to the seed.", color = K.Muted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Reset") { vm.run { vm.repo.resetToSeed() }; confirmReset = false }
        Spacer(Modifier.height(8.dp))
        TextButton("Cancel") { confirmReset = false }
    }
}
