package dev.mirzohidkhon.khonfitness.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.BuildConfig
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.health.HcStatus
import dev.mirzohidkhon.khonfitness.health.HealthImport
import dev.mirzohidkhon.khonfitness.health.ImportDraft
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K

/** Reads band sessions from Health Connect and turns them into cardio sessions, one tap each. */
@Composable
fun ImportScreen(vm: AppViewModel, nav: NavHostController) {
    val context = LocalContext.current
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    val plan by vm.planData.collectAsStateWithLifecycle()
    var status by remember { mutableStateOf(HealthImport.status(context)) }
    var granted by remember { mutableStateOf(false) }
    var drafts by remember { mutableStateOf<List<ImportDraft>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var picking by remember { mutableStateOf<ImportDraft?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val cardio = exercises.filter { it.kind == Kind.CARDIO && !it.archived }

    fun defaultExercise(d: ImportDraft): Exercise? {
        val planned = plan.itemFor(d.date).cardio
        if (planned != null && planned.modalityId == d.modalityId) return planned
        return cardio.filter { it.modalityId == d.modalityId }.sortedBy { it.intervals }.firstOrNull() ?: cardio.firstOrNull()
    }

    val permLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { refresh++ }
    LaunchedEffect(refresh, status) {
        if (status != HcStatus.AVAILABLE) return@LaunchedEffect
        granted = runCatching { HealthImport.hasPermissions(context) }.getOrDefault(false)
        if (granted) {
            error = null
            drafts = runCatching { HealthImport.drafts(context, 60, vm.repo.importedSourceIds().toSet()) }.onFailure { error = it.message ?: "Read failed" }.getOrNull()
        }
    }

    fun import(d: ImportDraft, ex: Exercise) {
        vm.run { vm.repo.importDraft(d, ex); refresh++ }
    }

    Column(Modifier.fillMaxSize()) {
        EditorTopBar("History", "Import", onBack = { nav.popBackStack() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            when {
                status == HcStatus.NOT_INSTALLED -> {
                    Text("Health Connect is not on this phone. The band's app writes workouts there, and this app reads them.", color = K.Muted, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("Get Health Connect") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    Spacer(Modifier.height(12.dp))
                    TextButton("Check again") { status = HealthImport.status(context) }
                }
                status == HcStatus.UPDATE_REQUIRED -> {
                    Text("Health Connect needs an update before it can share data.", color = K.Muted, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("Update Health Connect") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                !granted -> {
                    Text("Allow this app to read workouts, heart rate, and distance from Health Connect. Nothing is written back.", color = K.Muted, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("Allow") { permLauncher.launch(HealthImport.readPermissions) }
                }
                else -> {
                    val list = drafts
                    error?.let { Text(it, color = K.Red, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp)) }
                    when {
                        list == null -> Text("Reading…", color = K.Muted)
                        list.isEmpty() -> Text("Nothing new in the last 60 days. Sessions already imported do not show again.", color = K.Muted, style = MaterialTheme.typography.bodyMedium)
                        else -> {
                            GroupedList {
                                list.forEachIndexed { i, d ->
                                    val ex = defaultExercise(d)
                                    val parts = listOfNotNull("${d.seconds / 60} min", d.distanceM?.let { "${fmt(it / 1000.0)} km" }, d.avgHr?.let { "$it bpm" })
                                    ListRow("${d.date.format(shortDate)} · ${d.typeName} · ${d.originLabel}", secondary = parts.joinToString(" · "), dotColor = ex?.let { exerciseColor(it, modalities) } ?: K.Dim, dotFilled = ex?.intensity == Intensity.HIGH, divider = i > 0) { picking = d }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            PrimaryButton("Import all ${list.size}") { list.forEach { d -> defaultExercise(d)?.let { import(d, it) } } }
                            Text("Each one becomes a session under the exercise shown. Tap a row to pick a different exercise first.", color = K.Dim, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 10.dp))
                        }
                    }
                    if (BuildConfig.DEBUG) {
                        Spacer(Modifier.height(32.dp))
                        val writeLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { vm.run { runCatching { HealthImport.writeTestRun(context) }.onFailure { error = it.message }; refresh++ } }
                        TextButton("Debug: write a test run") { writeLauncher.launch(HealthImport.writePermissions) }
                    }
                }
            }
        }
    }
    picking?.let { d ->
        val current = defaultExercise(d)
        Sheet("${d.date.format(shortDate)} · ${d.typeName}", { picking = null }) {
            Text(listOfNotNull("${d.seconds / 60} min", d.distanceM?.let { "${fmt(it / 1000.0)} km" }, d.avgHr?.let { "avg $it bpm" }, d.laps?.let { "$it laps" }, "from " + d.originLabel).joinToString(" · "), color = K.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))
            SheetGroupTitle("Import as")
            ChoiceList { cardio.forEachIndexed { i, e -> ChoiceRow(e.name, e.id == current?.id, exerciseColor(e, modalities), e.intensity == Intensity.HIGH, divider = i > 0) { import(d, e); picking = null } } }
        }
    }
}
