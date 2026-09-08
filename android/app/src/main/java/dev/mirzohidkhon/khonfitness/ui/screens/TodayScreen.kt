package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun TodayScreen(vm: AppViewModel, nav: NavHostController) {
    val plan by vm.planData.collectAsStateWithLifecycle()
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    val blocks by vm.blocks.collectAsStateWithLifecycle()
    val blockExercises by vm.blockExercises.collectAsStateWithLifecycle()
    val bodyweights by vm.bodyweights.collectAsStateWithLifecycle()
    val unfinished by vm.unfinished.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val item = plan.itemFor(today)
    var planDate by remember { mutableStateOf<LocalDate?>(null) }
    var bwDraft by remember { mutableStateOf<Double?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 24.dp)) {
        ScreenTitle("Today")
        WeekStrip(today, plan, modalities) { planDate = it }
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(K.Surface).padding(20.dp, 20.dp, 20.dp, 16.dp)) {
            val active = unfinished
            if (active != null) {
                Text(active.programName ?: active.exerciseName ?: "Session", style = MaterialTheme.typography.headlineMedium)
                Text("In progress", color = K.Green, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
                PrimaryButton("Resume") { nav.navigate(if (active.itemType == ItemType.PROGRAM) Routes.session(active.id) else Routes.cardio(active.id)) }
            } else {
                Text(item.name, style = MaterialTheme.typography.headlineMedium)
                Text(preview(item, blocks, blockExercises, modalities), color = K.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
                if (!item.isRest) PrimaryButton("Start") {
                    vm.startItem(item) { s -> nav.navigate(if (s.itemType == ItemType.PROGRAM) Routes.session(s.id) else Routes.cardio(s.id)) }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        val latest = bodyweights.lastOrNull()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Bodyweight", style = MaterialTheme.typography.titleMedium, color = K.Muted, modifier = Modifier.weight(1f))
            Text(if (latest == null) "—" else "${fmt(latest.kg)} kg", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        val start = today.minusDays(29)
        LineChart(bodyweights.filter { it.date.toDate() >= start }.map { ChartPoint(it.date.toDate(), it.kg) }, "kg", Modifier.padding(top = 8.dp), height = 130)
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            NumberField(bwDraft, { bwDraft = it }, step = 0.1, unit = "kg", modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            TextButton("Add", enabled = bwDraft != null && bwDraft!! > 0) { vm.run { vm.repo.saveBodyweight(today.iso(), bwDraft!!) }; bwDraft = null }
        }
    }
    planDate?.let { date -> PlanSheet(vm, date, plan, modalities) { planDate = null } }
}

@Composable
fun WeekStrip(today: LocalDate, plan: KhonRepository.PlanData, modalities: List<Modality>, onTap: (LocalDate) -> Unit) {
    val monday = today.with(DayOfWeek.MONDAY)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0 until 7) {
            val date = monday.plusDays(i.toLong())
            val item = plan.itemFor(date)
            val color = itemColor(item, modalities)
            val isToday = date == today
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable { onTap(date) }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(date.dayOfWeek.name.take(1), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (isToday) K.Accent else K.Muted)
                Box(Modifier.padding(top = 5.dp).size(36.dp).clip(CircleShape).background(if (isToday) K.Accent else androidx.compose.ui.graphics.Color.Transparent), contentAlignment = Alignment.Center) {
                    Text(date.dayOfMonth.toString(), fontSize = 17.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium, color = if (isToday) K.AccentInk else K.Text)
                }
                Box(Modifier.padding(top = 5.dp).height(8.dp), contentAlignment = Alignment.Center) {
                    if (color != null) Dot(color.first, color.second, size = 7)
                }
            }
        }
    }
}

/** Change what a date holds: a program, a cardio exercise, or rest. */
@Composable
fun PlanSheet(vm: AppViewModel, date: LocalDate, plan: KhonRepository.PlanData, modalities: List<Modality>, onDismiss: () -> Unit) {
    val current = plan.itemFor(date)
    val cardio = plan.exercises.filter { it.kind == Kind.CARDIO && !it.archived }
    fun pick(type: String, id: String?) { vm.run { vm.repo.saveOverride(date.iso(), type, id) }; onDismiss() }
    Sheet(date.format(longDate), onDismiss) {
        SheetGroupTitle("Programs")
        ChoiceList { plan.programs.forEachIndexed { i, p -> ChoiceRow(p.name, current.program?.id == p.id, K.Accent, divider = i > 0) { pick(ItemType.PROGRAM, p.id) } } }
        SheetGroupTitle("Cardio exercises")
        ChoiceList { cardio.forEachIndexed { i, e -> ChoiceRow(e.name, current.cardio?.id == e.id, exerciseColor(e, modalities), e.intensity == Intensity.HIGH, divider = i > 0) { pick(ItemType.CARDIO, e.id) } } }
        SheetGroupTitle("Rest")
        ChoiceList { ChoiceRow("Rest", current.isRest, K.Dim, divider = false) { pick(ItemType.REST, null) } }
        if (current.overridden) Row(Modifier.padding(top = 12.dp)) { TextButton("Back to weekly template") { vm.run { vm.repo.clearOverride(date.iso()) }; onDismiss() } }
    }
}
