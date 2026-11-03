package dev.mirzohidkhon.khonfitness.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mirzohidkhon.khonfitness.data.MergeSnapshot
import dev.mirzohidkhon.khonfitness.ui.components.GroupShape
import dev.mirzohidkhon.khonfitness.ui.components.TextButton
import dev.mirzohidkhon.khonfitness.ui.theme.K
import kotlinx.coroutines.delay

/** A merge that just ran: its undo [snapshot] and the target's name for the line. */
class MergeDone(val snapshot: MergeSnapshot, val intoName: String)

/** How long the Undo stays after a merge, in ms. */
const val MERGE_UNDO_MS = 10_000L

/**
 * "Merged into <B>" with Undo, at the bottom of whatever screen shows after a merge, for [MERGE_UNDO_MS]. Undo puts
 * the source exercise and every moved row back ([AppViewModel.undoMerge]). Spoken when it shows.
 */
@Composable
fun MergeUndoBar(vm: AppViewModel, modifier: Modifier = Modifier) {
    val done by vm.lastMerge.collectAsStateWithLifecycle()
    val d = done ?: return
    LaunchedEffect(d) { delay(MERGE_UNDO_MS); if (vm.lastMerge.value === d) vm.lastMerge.value = null }
    Row(
        modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth().clip(GroupShape).background(K.Elevated)
            .padding(start = 16.dp, end = 8.dp).heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Merged into ${d.intoName}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite })
        Box(Modifier.heightIn(min = 48.dp).widthIn(min = 64.dp), contentAlignment = Alignment.Center) { TextButton("Undo") { vm.undoMerge(d) } }
    }
}
