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
import dev.mirzohidkhon.khonfitness.ui.Icons
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
    // Null until the permission check answers, so the Allow card does not flash.
    var granted by remember { mutableStateOf<Boolean?>(null) }
    var drafts by remember { mutableStateOf<List<ImportDraft>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var picking by remember { mutableStateOf<ImportDraft?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val cardio = exercises.filter { it.kind == Kind.CARDIO && !it.archived }

    fun defaultExercise(d: ImportDraft): Exercise? = HealthImport.defaultExercise(d.modalityId, plan.itemFor(d.date).cardio, cardio)

    val permLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { refresh++ }
    LaunchedEffect(refresh, status) {
        if (status != HcStatus.AVAILABLE) return@LaunchedEffect
        granted = runCatching { HealthImport.hasPermissions(context) }.getOrDefault(false)
        if (granted == true) {
            error = null
            drafts = runCatching { HealthImport.drafts(context, 60, vm.repo.importedSourceIds().toSet()) }.onFailure { error = it.message ?: "Read failed" }.getOrNull()
        }
    }

    fun import(d: ImportDraft, ex: Exercise) {
        vm.run { vm.repo.importDraft(d, ex); refresh++ }
    }

    Column(Modifier.fillMaxSize()) {
        val barScroll = rememberScrollState()
        EditorTopBar("History", "Import", scrollPx = { barScroll.value }, onBack = { nav.popBackStack() })
        Column(Modifier.weight(1f).verticalScroll(barScroll).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            when {
                status != HcStatus.AVAILABLE || granted == false -> HealthIntro(
                    status, granted == true,
                    onStore = { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                    onAllow = { permLauncher.launch(HealthImport.readPermissions) },
                    onCheckAgain = { status = HealthImport.status(context) },
                )
                granted == null -> Unit
                else -> {
                    val list = drafts?.filter { !it.strength }
                    val strengthCount = drafts?.count { it.strength } ?: 0
                    GroupedList { ListRow("Health Connect", secondary = "Last 60 days", chevron = false, divider = false, leading = { IconTile(Icons.Heart, K.Red) }) }
                    error?.let { GroupFooter(it, error = true) }
                    when {
                        list == null -> Column(Modifier.padding(top = 24.dp)) { ContentEmpty(Icons.DownloadCircle, "Reading workouts…", "From Health Connect.") }
                        list.isEmpty() -> Column(Modifier.padding(top = 24.dp)) {
                            ContentEmpty(Icons.CheckCircle, "All caught up", "Nothing new in the last 60 days. Sessions already imported do not show again.", color = K.Green)
                        }
                        else -> {
                            val ready = list.mapNotNull { d -> defaultExercise(d)?.let { d to it } }
                            SectionTitle(pendingLine(list.size), Modifier.padding(top = 24.dp))
                            GroupedList {
                                list.forEachIndexed { i, d ->
                                    val ex = defaultExercise(d)
                                    DraftRow(d, ex?.name, ex?.let { exerciseColor(it, modalities) } ?: K.Dim, divider = i > 0) { picking = d }
                                }
                            }
                            GroupFooter(
                                if (ready.size == list.size) "Each one becomes a session under the exercise on its right. Tap a row to pick a different one first."
                                else "The ones marked Choose match no exercise yet: tap one to pick it. The rest import under the exercise shown.",
                            )
                            if (ready.isNotEmpty()) PrimaryButton(importAllLabel(ready.size, list.size), Modifier.padding(top = 20.dp)) { ready.forEach { (d, e) -> import(d, e) } }
                        }
                    }
                    if (strengthCount > 0) GroupFooter("$strengthCount strength ${if (strengthCount == 1) "workout" else "workouts"} from the watch not shown: gym sessions are logged in the app.")
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
            Text(listOfNotNull(sessionMinutes(d.seconds), d.distanceM?.let { "${fmt(it / 1000.0)} km" }, d.avgHr?.let { "avg $it bpm" }, d.laps?.let { "$it laps" }, "from " + d.originLabel).joinToString(" · "), color = K.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))
            SheetGroupTitle("Import as")
            ChoiceList { cardio.forEachIndexed { i, e -> ChoiceRow(e.name, e.id == current?.id, exerciseColor(e, modalities), e.intensity == Intensity.HIGH, divider = i > 0) { import(d, e); picking = null } } }
        }
    }
}

/** Where Health Connect lives on Play: its install page, which also offers the update. */
private const val HEALTH_CONNECT_STORE = "market://details?id=com.google.android.apps.healthdata"

/**
 * What stands between the import and the data, on one card: Health Connect missing, outdated, or reading not yet
 * allowed. One action each: get or update it, or ask to read.
 */
@Composable
private fun HealthIntro(status: HcStatus, granted: Boolean, onStore: (String) -> Unit, onAllow: () -> Unit, onCheckAgain: () -> Unit) {
    val again: @Composable ColumnScope.() -> Unit = { Box(Modifier.align(Alignment.CenterHorizontally)) { TextButton("Check again", onClick = onCheckAgain) } }
    when {
        status == HcStatus.NOT_INSTALLED -> ContentEmpty(Icons.HeartSquare, "Get Health Connect", "The band's app writes workouts to Health Connect, and this app reads them from there.", color = K.Red) {
            PrimaryButton("Get Health Connect") { onStore("$HEALTH_CONNECT_STORE&url=healthconnect%3A%2F%2Fonboarding") }
            again()
        }
        status == HcStatus.UPDATE_REQUIRED -> ContentEmpty(Icons.HeartSquare, "Update Health Connect", "Health Connect needs an update before it can share data.", color = K.Red) {
            PrimaryButton("Update Health Connect") { onStore(HEALTH_CONNECT_STORE) }
            again()
        }
        !granted -> ContentEmpty(Icons.HeartSquare, "Read your workouts", "Allow this app to read workouts, heart rate and distance from Health Connect. Nothing is written back.", color = K.Red) {
            PrimaryButton("Allow", onClick = onAllow)
        }
    }
}

/**
 * One band or watch workout waiting for import: a tile in its exercise's color, the workout type with the exercise
 * it goes under on the same line (Choose in accent when none matched), and its date and numbers on a line of their
 * own, so the numbers never get cut.
 */
@Composable
private fun DraftRow(d: ImportDraft, exerciseName: String?, color: Color, divider: Boolean, onClick: () -> Unit) {
    val spoken = "${d.typeName}, ${draftStats(d)}, ${if (exerciseName != null) "imports as $exerciseName" else "choose an exercise"}"
    Column(Modifier.fillMaxWidth()) {
        if (divider) HorizontalDivider(Modifier.padding(start = 58.dp), color = K.Divider, thickness = 0.7.dp)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable(role = Role.Button, onClick = onClick).clearAndSetSemantics { contentDescription = spoken }
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(Icons.Runner, if (exerciseName != null) color else K.Dim)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                BoxWithConstraints {
                    // The exercise never takes more than a little over half of the line.
                    val most = maxWidth * 0.55f
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(d.typeName, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(
                            exerciseName ?: "Choose", style = MaterialTheme.typography.bodyMedium, color = if (exerciseName != null) K.Muted else K.Accent,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End, modifier = Modifier.padding(start = 8.dp).widthIn(max = most),
                        )
                    }
                }
                Text(draftStats(d), style = MaterialTheme.typography.bodySmall, color = K.Muted, maxLines = 2, modifier = Modifier.padding(top = 2.dp))
            }
            Chevron()
        }
    }
}
