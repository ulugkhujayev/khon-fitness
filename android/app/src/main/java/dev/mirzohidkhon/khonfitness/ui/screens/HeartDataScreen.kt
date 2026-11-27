package dev.mirzohidkhon.khonfitness.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.health.HcStatus
import dev.mirzohidkhon.khonfitness.health.HealthHeartSource
import dev.mirzohidkhon.khonfitness.health.HealthImport
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Icons
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.launch

class HeartAccess(val status: () -> HcStatus, val granted: suspend () -> Boolean) {
    companion object {
        fun of(context: Context) = withPermissions(
            status = { HealthImport.status(context) },
            permissions = { HealthImport.client(context).permissionController.getGrantedPermissions() },
        )

        fun withPermissions(status: () -> HcStatus, permissions: suspend () -> Set<String>) =
            HeartAccess(status, granted = { HealthHeartSource.HEART_READ in permissions() })
    }
}

fun appLabel(context: Context, pkg: String): String =
    runCatching { context.packageManager.let { pm -> pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() } }.getOrDefault(pkg)

@Composable
fun HeartDataScreen(
    vm: AppViewModel,
    nav: NavHostController,
    heart: HeartFill = (LocalContext.current.applicationContext as KhonApp).heartFill,
    access: HeartAccess = HeartAccess.of(LocalContext.current.applicationContext),
    clock: Clock = Clock.systemDefaultZone(),
) {
    val context = LocalContext.current
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val rows by vm.allSessionHr.collectAsStateWithLifecycle()
    val intervals by vm.allIntervalLogs.collectAsStateWithLifecycle()
    val today = rememberToday(clock)
    val zone = clock.zone
    val list = hrDataSessions(sessions, today)
    val factsOf = factsById(list, rows, zone)

    var status by remember { mutableStateOf(access.status()) }
    var granted by remember { mutableStateOf<Boolean?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val permLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { refresh++ }
    LaunchedEffect(refresh, status) {
        if (status == HcStatus.AVAILABLE) granted = runCatching { access.granted() }.getOrDefault(false)
    }

    var open by rememberSaveable { mutableStateOf<String?>(null) }
    var sheetNo by rememberSaveable { mutableIntStateOf(0) }
    var confirmRemove by rememberSaveable { mutableStateOf(false) }
    fun openSheet(id: String) { open = id; sheetNo++; confirmRemove = false }
    fun close() { open = null; sheetNo++; confirmRemove = false }

    Column(Modifier.fillMaxSize()) {
        val scroll = rememberScrollState()
        EditorTopBar("Settings", "Heart rate data", scrollPx = { scroll.value }, onBack = { nav.popBackStack() })
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            if (status != HcStatus.AVAILABLE || granted == false) {
                HealthIntro(
                    status, granted == true,
                    onStore = { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                    onAllow = { permLauncher.launch(setOf(HealthHeartSource.HEART_READ)) },
                    onCheckAgain = { status = access.status() },
                    allowTitle = "Read heart rate",
                    allowLine = "Allow this app to read heart rate from Health Connect. Nothing is written back.",
                )
                Spacer(Modifier.height(24.dp))
            }
            GroupedList {
                if (list.isEmpty()) EmptyLine("No sessions in the last 30 days")
                list.forEachIndexed { i, s ->
                    val facts = factsOf.getValue(s.id)
                    HeartRow(sessionName(s), s.date.toDate().format(shortDate), hrStateText(s, facts), hrStateSpoken(s, facts), divider = i > 0) { openSheet(s.id) }
                }
            }
        }
    }

    val shown = open?.let { id -> sessions.find { it.id == id } }
    LaunchedEffect(open, shown == null, sessions) { if (open != null && shown == null) close() }
    if (shown == null) return
    val id = shown.id

    if (confirmRemove) {
        ConfirmSheet("Remove heart rate?", "", "Remove", onDismiss = { confirmRemove = false }) {
            confirmRemove = false
            vm.run { heart.removeHeartRate(id) }
        }
        return
    }
    key(sheetNo) { HeartSheet(vm, heart, shown, rowOf(rows, id), intervals.filter { it.sessionId == id }, zone, onRemove = { confirmRemove = true }, onClose = ::close) }
}

@Composable
private fun HeartSheet(vm: AppViewModel, heart: HeartFill, s: Session, row: SessionHr?, rounds: List<IntervalLog>, zone: ZoneId, onRemove: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val id = s.id
    val range = hrWindow(s, zone)
    var asked by remember(range, row) { mutableStateOf<Any?>(null) }
    var failed by remember(range, row) { mutableStateOf(false) }
    var reading by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheet = hrSheet(s, row, rounds, zone, failed = failed) { appLabel(context, it) }
    Sheet(sessionName(s), onClose) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(s.date.toDate().format(shortDate), style = MaterialTheme.typography.bodyMedium, color = K.Muted, modifier = Modifier.padding(bottom = 12.dp))
            GroupedList(elevated = true) { sheet.facts.forEachIndexed { i, (k, v) -> FactRow(k, v, divider = i > 0) } }
            val bounds = sheet.useRange
            if (sheet.reread || bounds != null) GroupedList(Modifier.padding(top = 20.dp), elevated = true) {
                if (bounds != null) ActionRow(Icons.Check, "Use this time range", enabled = !confirming, divider = false) {
                    if (confirming) return@ActionRow
                    confirming = true
                    asked = null
                    failed = false
                    vm.run { try { vm.repo.confirmRange(id, bounds.fromMs, bounds.toMs) } finally { confirming = false } }
                }
                if (sheet.reread) ActionRow(Icons.Restore, "Read again", enabled = !reading, divider = bounds != null) {
                    if (reading) return@ActionRow
                    val request = Any()
                    asked = request
                    failed = false
                    reading = true
                    scope.launch {
                        try {
                            if (heart.rereadHeartRate(id) == HrReread.FAILED && asked === request) failed = true
                        } finally {
                            reading = false
                        }
                    }
                }
            }
            if (sheet.remove) GroupedList(Modifier.padding(top = 20.dp), elevated = true) {
                ActionRow(Icons.Trash, "Remove", color = K.Red, divider = false, onClick = onRemove)
            }
            sheet.line?.let { GroupFooter(it) }
        }
    }
}

fun sessionName(s: Session): String = s.programName ?: s.exerciseName ?: "Session"

@Composable
private fun HeartRow(name: String, date: String, state: String, spoken: String, divider: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        if (divider) HorizontalDivider(Modifier.padding(start = 16.dp), color = K.Divider, thickness = 0.7.dp)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick)
                .clearAndSetSemantics { contentDescription = "$name, $date, $spoken"; role = Role.Button }
                .padding(start = 16.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                Text(date, style = MaterialTheme.typography.bodyMedium, color = K.Muted, modifier = Modifier.padding(top = 2.dp))
            }
            Text(labelsKeptWithNumbers(state), style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TABULAR), color = K.Muted,
                textAlign = TextAlign.End, modifier = Modifier.padding(start = 8.dp).widthIn(max = 136.dp))
            Chevron()
        }
    }
}

@Composable
private fun FactRow(label: String, value: String, divider: Boolean) {
    FieldRow(label, divider = divider) { Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR), color = K.Text, textAlign = TextAlign.End) }
}

private fun labelsKeptWithNumbers(state: String) = state.replace("avg ", "avg\u00A0").replace("max ", "max\u00A0")
