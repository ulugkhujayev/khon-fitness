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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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

/** 240 -> "4 min", 20 -> "20 s", 90 -> "1:30". */
fun dur(sec: Int): String = when { sec >= 60 && sec % 60 == 0 -> "${sec / 60} min"; sec < 60 -> "$sec s"; else -> "${sec / 60}:${"%02d".format(sec % 60)}" }

fun preview(item: PlanItem, blocks: List<Block>, blockExercises: List<BlockExercise>, modalities: List<Modality>): String = when (item.itemType) {
    ItemType.PROGRAM -> {
        val bs = blocks.filter { it.programId == item.program?.id }
        val sets = bs.sumOf { b -> blockExercises.filter { it.blockId == b.id }.sumOf { it.sets } }
        "${bs.size} blocks · $sets sets"
    }
    ItemType.CARDIO -> item.cardio?.let { ex ->
        val mod = modalities.find { it.id == ex.modalityId }?.name?.lowercase() ?: ""
        if (ex.intervals) "${ex.rounds} × ${dur(ex.workSec)} on $mod, " + (if (ex.restSec > 0) "${dur(ex.restSec)} rest" else "no rest") else "Steady on $mod"
    } ?: ""
    else -> "Rest day"
}

/**
 * Navigation bar in the HIG style: a round Back button with a chevron only (no "Back" text), the title centered
 * in Headline, and the primary action on the trailing edge as a small prominent capsule.
 */
@Composable
fun EditorTopBar(back: String, title: String, onBack: () -> Unit, done: String = "Done", onDone: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        BarCircleButton(dev.mirzohidkhon.khonfitness.ui.Icons.ChevronLeft, back, onBack)
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        when {
            onDone == null -> Spacer(Modifier.width(36.dp))
            done == "+" -> BarCircleButton(dev.mirzohidkhon.khonfitness.ui.Icons.Plus, "Add", onDone)
            else -> Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(K.Accent).clickable(onClick = onDone).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                Text(done, color = K.AccentInk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

/** 36 dp round bar button on the group surface, like a Liquid Glass toolbar item without the blur. */
@Composable
fun BarCircleButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Box(Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape).background(K.Surface), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(icon, contentDescription = label, tint = K.Accent, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sheet(title: String?, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = state, containerColor = K.Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { Box(Modifier.padding(top = 6.dp, bottom = 2.dp).width(36.dp).height(5.dp).background(K.Dim, RoundedCornerShape(3.dp))) },
    ) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp, top = 4.dp))
            content()
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun SheetGroupTitle(text: String) { Text(text, color = K.Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp)) }

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


/** Editor row that shows a date and opens the Material date picker on tap. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRow(date: java.time.LocalDate, divider: Boolean = true, onChange: (java.time.LocalDate) -> Unit) {
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    FieldRow("Date", divider = divider, onClick = { open = true }) { Text(date.format(shortDate), fontWeight = FontWeight.SemiBold); Chevron() }
    if (open) {
        val state = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli())
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton("Done") { state.selectedDateMillis?.let { onChange(java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate()) }; open = false } },
            dismissButton = { TextButton("Cancel", color = K.Muted) { open = false } },
            colors = androidx.compose.material3.DatePickerDefaults.colors(containerColor = K.Surface),
        ) { androidx.compose.material3.DatePicker(state = state, colors = androidx.compose.material3.DatePickerDefaults.colors(containerColor = K.Surface, selectedDayContainerColor = K.Accent, selectedDayContentColor = K.AccentInk, todayDateBorderColor = K.Accent, todayContentColor = K.Accent)) }
    }
}

/** Two-button confirm sheet for destructive actions. */
@Composable
fun ConfirmSheet(title: String, body: String, action: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Sheet(title, onDismiss) {
        Text(body, color = K.Muted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp)).background(K.Red).clickable(onClick = onConfirm), contentAlignment = Alignment.Center) {
            Text(action, color = K.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        TextButton("Cancel", color = K.Muted) { onDismiss() }
    }
}
