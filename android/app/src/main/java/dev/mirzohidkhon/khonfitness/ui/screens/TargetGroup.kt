package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mirzohidkhon.khonfitness.data.Bodyweight
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.components.Chevron
import dev.mirzohidkhon.khonfitness.ui.components.FieldRow
import dev.mirzohidkhon.khonfitness.ui.components.GroupedList
import dev.mirzohidkhon.khonfitness.ui.components.NumberField
import dev.mirzohidkhon.khonfitness.ui.components.PrimaryButton
import dev.mirzohidkhon.khonfitness.ui.components.SectionTitle
import dev.mirzohidkhon.khonfitness.ui.components.fmt
import dev.mirzohidkhon.khonfitness.ui.theme.K
import java.math.BigDecimal
import java.time.LocalDate

/** The "Target" group of a strength exercise: the typed target, then the date that the present trend gives. */
@Composable
fun LiftTargetGroup(vm: AppViewModel, exerciseId: String, history: List<LiftSession>, today: LocalDate, modifier: Modifier = Modifier) {
    val stored by remember(exerciseId) { vm.targets.lift(exerciseId) }.collectAsStateWithLifecycle(null)
    val rows = remember(stored, history, today) { liftRows(stored, history, today) }
    val kind = if (withWeight(history)) TargetKind.LIFT_KG else TargetKind.LIFT_REPS
    val current = liftTargetFor(stored, kind == TargetKind.LIFT_KG)?.value
    TargetGroup(rows, kind, current, stored != null, modifier) { value ->
        vm.run { vm.targets.setLift(exerciseId, value, kind == TargetKind.LIFT_REPS, today) }
    }
}

/** The "Target" group of the bodyweight section. */
@Composable
fun BodyTargetGroup(vm: AppViewModel, bodyweights: List<Bodyweight>, today: LocalDate, modifier: Modifier = Modifier) {
    val stored by vm.bodyTarget.collectAsStateWithLifecycle()
    val rows = remember(stored, bodyweights, today) { bodyRows(stored, bodyweights, today) }
    TargetGroup(rows, TargetKind.BODY, stored?.kg, stored != null, modifier) { value ->
        vm.run { vm.targets.setBody(value, value?.let { bodyStart(bodyweights, today) }, today) }
    }
}

/**
 * [current] is the stored target in [kind]'s unit; [hasRecord] is true while any target is stored, also one of the other
 * unit. A sheet's draft belongs to its unit: a new unit closes the sheet and drops the draft.
 */
@Composable
private fun TargetGroup(rows: TargetRows, kind: TargetKind, current: BigDecimal?, hasRecord: Boolean, modifier: Modifier, onSave: (BigDecimal?) -> Unit) {
    var open by remember(kind) { mutableStateOf(false) }
    TargetRowsGroup(rows, kind, modifier) { open = true }
    if (open) TargetSheet(kind, current, onDismiss = { open = false }) { value ->
        open = false
        val same = if (value == null) !hasRecord else current != null && value.compareTo(current) == 0
        if (!same) onSave(value)
    }
}

@Composable
internal fun TargetRowsGroup(rows: TargetRows, kind: TargetKind, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    SectionTitle(if (kind == TargetKind.LIFT_KG) kind.title else "Target", modifier)
    GroupedList {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onOpen)
                .clearAndSetSemantics { contentDescription = rows.target.spoken; role = Role.Button }
                .padding(start = 16.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(rows.target.label, style = MaterialTheme.typography.bodyLarge, color = K.Text, modifier = Modifier.weight(1f).padding(vertical = 12.dp))
            Text(rows.target.value.orEmpty(), style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TABULAR), color = if (rows.set) K.Text else K.Muted, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
            Chevron()
        }
        rows.result?.let { r ->
            HorizontalDivider(Modifier.padding(start = 16.dp), color = K.Divider, thickness = 0.7.dp)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clearAndSetSemantics { contentDescription = r.spoken }.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(r.label.replace("/", "\u2060/\u2060"), style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR), color = K.Text, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(vertical = 12.dp))
                if (r.value != null) Text(r.value, style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TABULAR), color = K.Text, maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun TargetSheet(kind: TargetKind, current: BigDecimal?, onDismiss: () -> Unit, onSave: (BigDecimal?) -> Unit) {
    var draft by remember { mutableStateOf(fmt(current?.toDouble(), kind.decimals)) }
    val entry = targetEntry(draft, kind)
    Sheet(kind.title, onDismiss) {
        GroupedList(elevated = true) {
            FieldRow("Target", divider = false) {
                NumberField(
                    current?.toDouble(), {}, kind.step, kind.unit, width = 176.dp, decimals = kind.decimals, height = 38, min = kind.min, max = kind.max,
                    elevated = true, label = "${kind.spokenTitle}, ${kind.spokenUnit}", onTextChange = { draft = it },
                )
            }
        }
        PrimaryButton("Save", Modifier.padding(top = 20.dp), enabled = entry != TargetEntry.Refused) {
            when (entry) {
                TargetEntry.Clear -> onSave(null)
                is TargetEntry.Store -> onSave(entry.value)
                TargetEntry.Refused -> Unit
            }
        }
    }
}
