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

/** What stands between the reader and Health Connect: its status, and whether heart rate may be read. */
class HeartAccess(val status: () -> HcStatus, val granted: suspend () -> Boolean) {
    companion object {
        fun of(context: Context) = HeartAccess(
            status = { HealthImport.status(context) },
            granted = { HealthHeartSource.HEART_READ in HealthImport.client(context).permissionController.getGrantedPermissions() },
        )
    }
}

/** The app's label of [pkg] when the system knows the package, else the package name. */
fun appLabel(context: Context, pkg: String): String =
    runCatching { context.packageManager.let { pm -> pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() } }.getOrDefault(pkg)

/**
 * The heart rate of each finished session of the last 30 days, newest first, as [hrFacts] gives it. A tap opens the
 * session's facts with "Read again", "Remove", and for an older gym session "Use this time range".
 */
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
    val rowById = rows.associateBy { it.sessionId }

    var status by remember { mutableStateOf(access.status()) }
    var granted by remember { mutableStateOf<Boolean?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val permLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { refresh++ }
    LaunchedEffect(refresh, status) {
        if (status == HcStatus.AVAILABLE) granted = runCatching { access.granted() }.getOrDefault(false)
    }

    var open by remember { mutableStateOf<String?>(null) }
    var confirmRemove by remember { mutableStateOf(false) }
    var reading by remember { mutableStateOf(setOf<String>()) }
    var confirming by remember { mutableStateOf(setOf<String>()) }
    var results by remember { mutableStateOf(mapOf<String, HrReread>()) }

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
                    val facts = hrFacts(s, rowById[s.id], zone)
                    HeartRow(sessionName(s), s.date.toDate().format(shortDate), hrStateText(s, facts), hrStateSpoken(s, facts), divider = i > 0) { open = s.id }
                }
            }
        }
    }

    val shown = open?.let { id -> sessions.find { it.id == id } }
    LaunchedEffect(open, shown == null, sessions) { if (open != null && shown == null) { open = null; confirmRemove = false } }
    if (shown == null) return
    val id = shown.id
    val row = rowById[id]
    val facts = hrFacts(shown, row, zone)
    fun close() { open = null; confirmRemove = false; results = results - id }

    if (confirmRemove) {
        ConfirmSheet("Remove heart rate?", "Read again brings it back.", "Remove", onDismiss = { confirmRemove = false }) {
            confirmRemove = false
            results = results - id
            vm.run { heart.removeHeartRate(id) }
        }
        return
    }
    Sheet(sessionName(shown), ::close) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(shown.date.toDate().format(shortDate), style = MaterialTheme.typography.bodyMedium, color = K.Muted, modifier = Modifier.padding(bottom = 12.dp))
            val bounds = confirmableBounds(shown, zone)
            val facts2 = hrFactRows(shown, facts, intervals.filter { it.sessionId == id }, zone) { appLabel(context, it) } +
                listOfNotNull(bounds?.let { "Workout" to clockRange(it.fromMs, it.toMs, zone) })
            GroupedList(elevated = true) { facts2.forEachIndexed { i, (k, v) -> FactRow(k, v, divider = i > 0) } }
            val canRead = canReread(facts)
            if (canRead || bounds != null) GroupedList(Modifier.padding(top = 20.dp), elevated = true) {
                if (bounds != null) ActionRow(Icons.Check, "Use this time range", enabled = id !in confirming, divider = false) {
                    if (id in confirming) return@ActionRow
                    confirming = confirming + id
                    vm.run { try { vm.repo.confirmRange(id, bounds.fromMs, bounds.toMs) } finally { confirming = confirming - id } }
                }
                if (canRead) ActionRow(Icons.Restore, "Read again", enabled = id !in reading, divider = bounds != null) {
                    if (id in reading) return@ActionRow
                    reading = reading + id
                    results = results - id
                    vm.run {
                        try { results = results + (id to heart.rereadHeartRate(id)) } finally { reading = reading - id }
                    }
                }
            }
            if (canRemove(row)) GroupedList(Modifier.padding(top = 20.dp), elevated = true) {
                ActionRow(Icons.Trash, "Remove", color = K.Red, divider = false) { confirmRemove = true }
            }
            rereadLine(results[id], shown, facts)?.let { GroupFooter(it) }
        }
    }
}

fun sessionName(s: Session): String = s.programName ?: s.exerciseName ?: "Session"

/** A session on "Heart rate data": the name over the date, the state on the right as a fact, a chevron. */
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
            // A label stays with its number, so a long state breaks after "·", and the date keeps its line.
            Text(state.replace("avg ", "avg\u00A0").replace("max ", "max\u00A0"), style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TABULAR), color = K.Muted,
                textAlign = TextAlign.End, modifier = Modifier.padding(start = 8.dp).widthIn(max = 136.dp))
            Chevron()
        }
    }
}

/** A fact as a label and its value, nothing to tap. */
@Composable
private fun FactRow(label: String, value: String, divider: Boolean) {
    FieldRow(label, divider = divider) { Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR), color = K.Text, textAlign = TextAlign.End) }
}
