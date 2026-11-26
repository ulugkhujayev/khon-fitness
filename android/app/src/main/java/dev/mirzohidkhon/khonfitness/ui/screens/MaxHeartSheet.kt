package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Icons
import dev.mirzohidkhon.khonfitness.ui.components.Chevron
import dev.mirzohidkhon.khonfitness.ui.components.FieldRow
import dev.mirzohidkhon.khonfitness.ui.components.GroupedList
import dev.mirzohidkhon.khonfitness.ui.components.IconTile
import dev.mirzohidkhon.khonfitness.ui.components.NumberField
import dev.mirzohidkhon.khonfitness.ui.components.PrimaryButton
import dev.mirzohidkhon.khonfitness.ui.theme.K

@Composable
fun MaxHeartRow(vm: AppViewModel, divider: Boolean) {
    val typed by vm.typedMaxHr.collectAsStateWithLifecycle()
    val measured = rememberMeasuredMax(vm)
    val shown = maxShown(typed, measured)
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        if (divider) HorizontalDivider(Modifier.padding(start = 58.dp), color = K.Divider, thickness = 0.7.dp)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) { open = true }
                .clearAndSetSemantics { contentDescription = shown.spoken; role = Role.Button }
                .padding(start = 16.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(Icons.HeartSquare, K.Red)
            Spacer(Modifier.width(12.dp))
            Text("Maximum heart rate", style = MaterialTheme.typography.bodyLarge, color = K.Text, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(vertical = 12.dp))
            Text(shown.text, style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TABULAR), color = if (shown.typed) K.Text else K.Muted, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
            Chevron()
        }
    }
    if (open) MaxHeartSheet(typed, measured, onDismiss = { open = false }) { bpm -> vm.run { vm.maxHeartRate.set(bpm) }; open = false }
}

@Composable
private fun MaxHeartSheet(typed: Int?, measured: Int?, onDismiss: () -> Unit, onSave: (Int?) -> Unit) {
    var draft by remember { mutableStateOf(typed?.toDouble()) }
    val entry = maxEntry(draft)
    Sheet("Maximum heart rate", onDismiss) {
        GroupedList(elevated = true) {
            FieldRow("Maximum", divider = false) {
                NumberField(draft, { draft = it }, 1.0, "bpm", width = 176.dp, decimals = 0, height = 38, placeholder = measured?.toString(), elevated = true, label = "Maximum heart rate, beats per minute")
            }
        }
        PrimaryButton("Save", Modifier.padding(top = 20.dp), enabled = entry != MaxEntry.Refused) {
            when (entry) {
                MaxEntry.Clear -> onSave(null)
                is MaxEntry.Store -> onSave(entry.bpm)
                MaxEntry.Refused -> Unit
            }
        }
    }
}
