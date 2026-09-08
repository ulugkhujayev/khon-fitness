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
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K
import dev.mirzohidkhon.khonfitness.BuildConfig
import dev.mirzohidkhon.khonfitness.update.Updater
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private val SEGMENTS = listOf("Programs", "Library", "Modalities", "Week plan", "Settings")

@Composable
fun ProgramsScreen(vm: AppViewModel, nav: NavHostController) {
    var segment by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 24.dp)) {
        ScreenTitle(SEGMENTS[segment])
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            SEGMENTS.forEachIndexed { i, name ->
                val active = i == segment
                Column(Modifier.clickable { segment = i }) {
                    Text(name, color = if (active) K.Text else K.Muted, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 10.dp))
                    Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(3.dp)).background(if (active) K.Accent else androidx.compose.ui.graphics.Color.Transparent))
                }
            }
        }
        HorizontalDivider(color = K.Divider)
        Spacer(Modifier.height(18.dp))
        when (segment) {
            0 -> ProgramList(vm, nav)
            1 -> Library(vm, nav)
            2 -> ModalityList(vm, nav)
            3 -> WeekPlanList(vm)
            4 -> Settings(vm)
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
private fun Library(vm: AppViewModel, nav: NavHostController) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    var kind by rememberSaveable { mutableStateOf("") }
    var muscle by rememberSaveable { mutableStateOf("") }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<String?>(null) }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip(if (kind.isEmpty()) "Kind: Any" else "Kind: ${kind.replaceFirstChar { it.uppercase() }}", kind.isNotEmpty()) { sheet = "kind" }
        Chip(if (muscle.isEmpty()) "Muscle: Any" else "Muscle: ${muscle.replaceFirstChar { it.uppercase() }}", muscle.isNotEmpty()) { sheet = "muscle" }
        Chip(if (showArchived) "Archived shown" else "Archived hidden", showArchived) { showArchived = !showArchived }
    }
    SectionTitle("Exercises") { TextButton("+") { nav.navigate(Routes.exercise("new")) } }
    val list = exercises.filter { (showArchived || !it.archived) && (kind.isEmpty() || it.kind == kind) && (muscle.isEmpty() || it.primaryMuscle == muscle || muscle in it.secondaryList) }
    GroupedList {
        list.forEachIndexed { i, e ->
            val secondary = if (e.kind == Kind.STRENGTH) e.primaryMuscle?.replaceFirstChar { it.uppercase() } else modalities.find { it.id == e.modalityId }?.name
            ListRow(e.name, secondary = secondary, dotColor = exerciseColor(e, modalities), dotFilled = e.kind == Kind.STRENGTH || e.intensity == Intensity.HIGH, divider = i > 0, titleColor = if (e.archived) K.Dim else K.Text) { nav.navigate(Routes.exercise(e.id)) }
        }
        if (list.isEmpty()) ListRow("Nothing here", chevron = false, divider = false, titleColor = K.Muted)
    }
    when (sheet) {
        "kind" -> Sheet("Kind", { sheet = null }) { ChoiceList {
            ChoiceRow("Any", kind.isEmpty(), divider = false) { kind = ""; sheet = null }
            ChoiceRow("Strength", kind == Kind.STRENGTH) { kind = Kind.STRENGTH; sheet = null }
            ChoiceRow("Cardio", kind == Kind.CARDIO) { kind = Kind.CARDIO; sheet = null }
        } }
        "muscle" -> Sheet("Muscle", { sheet = null }) { ChoiceList {
            ChoiceRow("Any", muscle.isEmpty(), divider = false) { muscle = ""; sheet = null }
            MUSCLE_GROUPS.forEach { m -> ChoiceRow(m.replaceFirstChar { it.uppercase() }, muscle == m) { muscle = m; sheet = null } }
        } }
    }
}

@Composable
fun Chip(text: String, active: Boolean, onClick: () -> Unit) {
    Box(Modifier.height(34.dp).clip(RoundedCornerShape(9.dp)).background(if (active) K.AccentSoft else K.Surface).clickable(onClick = onClick).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
        Text(text, color = if (active) K.Accent else K.Muted, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ModalityList(vm: AppViewModel, nav: NavHostController) {
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    SectionTitle("Modalities") { TextButton("+") { nav.navigate(Routes.modality("new")) } }
    GroupedList { modalities.filter { !it.archived }.forEachIndexed { i, m -> ListRow(m.name, dotColor = androidx.compose.ui.graphics.Color(m.color), divider = i > 0) { nav.navigate(Routes.modality(m.id)) } } }
}

@Composable
private fun WeekPlanList(vm: AppViewModel) {
    val plan by vm.planData.collectAsStateWithLifecycle()
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Int?>(null) }
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
private fun Settings(vm: AppViewModel) {
    val context = LocalContext.current
    var toast by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<Updater.Result?>(null) }
    var progress by remember { mutableStateOf<Float?>(null) }
    var pendingFile by remember { mutableStateOf<java.io.File?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val f = pendingFile
        if (f != null && context.packageManager.canRequestPackageInstalls()) { pendingFile = null; Updater.install(context, f) }
    }
    val updateHint = when (val u = update) { is Updater.Result.Available -> " → " + u.release.tag_name; else -> "" }
    fun checkUpdate() { toast = "Checking…"; vm.run { update = Updater.check(BuildConfig.VERSION_NAME); toast = when (val u = update) { is Updater.Result.UpToDate -> "v${u.current} is the latest"; is Updater.Result.Available -> "Update ${u.release.tag_name} available"; is Updater.Result.Failed -> u.message; null -> null } } }
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
    GroupedList {
        ListRow("Export backup", divider = false) { exportLauncher.launch("khon-fitness-${java.time.LocalDate.now()}.json") }
        ListRow("Import backup") { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
        ListRow("Check for update", secondary = "v" + BuildConfig.VERSION_NAME + updateHint) { checkUpdate() }
        ListRow("Reset to seed data", titleColor = K.Red, chevron = false) { confirmReset = true }
    }
    toast?.let { Text(it, color = K.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp)) }
    (update as? Updater.Result.Available)?.let { u ->
        Spacer(Modifier.height(16.dp))
        Text(u.release.name ?: u.release.tag_name, style = MaterialTheme.typography.titleMedium)
        u.release.body?.takeIf { it.isNotBlank() }?.let { Text(it, color = K.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.height(12.dp))
        val p = progress
        when {
            p != null -> Text("Downloading… ${(p * 100).toInt()}%", color = K.Muted, modifier = Modifier.padding(top = 4.dp))
            pendingFile != null -> {
                Text("Android needs a one-time permission so this app can install its own updates. Allow it, then come back.", color = K.Muted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Allow and install") { permissionLauncher.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))) }
            }
            else -> PrimaryButton("Update to ${u.release.tag_name}") {
                progress = 0f
                vm.run {
                    runCatching { Updater.download(context, u.apk) { progress = it } }
                        .onSuccess { file -> progress = null; if (context.packageManager.canRequestPackageInstalls()) Updater.install(context, file) else pendingFile = file }
                        .onFailure { progress = null; toast = "Download failed: ${it.message}" }
                }
            }
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
