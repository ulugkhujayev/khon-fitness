package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K
import java.time.format.DateTimeFormatter

val longDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

/** Color and fill for a plan item: accent for strength, modality color for cardio, filled = high intensity. */
fun itemColor(item: PlanItem, modalities: List<Modality>): Pair<Color, Boolean>? = when (item.itemType) {
    ItemType.PROGRAM -> K.Accent to true
    ItemType.CARDIO -> item.cardio?.let { ex -> Color(modalities.find { it.id == ex.modalityId }?.color ?: 0xFF9B9BA3) to (ex.intensity == Intensity.HIGH) }
    else -> null
}

fun exerciseColor(ex: Exercise, modalities: List<Modality>): Color =
    if (ex.kind == Kind.STRENGTH) K.Accent else Color(modalities.find { it.id == ex.modalityId }?.color ?: 0xFF9B9BA3)

fun preview(item: PlanItem, blocks: List<Block>, blockExercises: List<BlockExercise>, modalities: List<Modality>): String = when (item.itemType) {
    ItemType.PROGRAM -> {
        val bs = blocks.filter { it.programId == item.program?.id }
        val sets = bs.sumOf { b -> blockExercises.filter { it.blockId == b.id }.sumOf { it.sets } }
        "${bs.size} blocks · $sets sets"
    }
    ItemType.CARDIO -> item.cardio?.let { ex ->
        val mod = modalities.find { it.id == ex.modalityId }?.name?.lowercase() ?: ""
        if (ex.intervals) "${ex.rounds} × ${ex.workSec / 60} min on $mod, ${ex.restSec / 60} min rest" else "Steady on $mod"
    } ?: ""
    else -> "Rest day"
}

@Composable
fun EditorTopBar(back: String, title: String, onBack: () -> Unit, done: String = "Done", onDone: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton("‹ $back", onClick = onBack)
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        if (onDone != null) Box(Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onDone).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            Text(done, color = K.Accent, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        } else Spacer(Modifier.width(60.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sheet(title: String?, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = state, containerColor = K.Surface,
        dragHandle = { Box(Modifier.padding(top = 8.dp, bottom = 4.dp).width(36.dp).height(4.dp).background(K.Surface3, RoundedCornerShape(2.dp))) },
    ) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp, top = 4.dp))
            content()
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun SheetGroupTitle(text: String) { Text(text, color = K.Dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)) }

/** Choice list inside a sheet: rows on a slightly lighter surface. */
@Composable
fun ChoiceList(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(GroupShape).background(K.Surface2)) { content() }
}

@Composable
fun ChoiceRow(title: String, selected: Boolean, dotColor: Color? = null, dotFilled: Boolean = true, divider: Boolean = true, onClick: () -> Unit) {
    ListRow(title, secondary = null, dotColor = dotColor, dotFilled = dotFilled, chevron = false, divider = divider,
        trailing = { if (selected) Text("✓", color = K.Accent, fontWeight = FontWeight.Bold, fontSize = 18.sp) }, onClick = onClick)
}
