package dev.mirzohidkhon.khonfitness.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.mirzohidkhon.khonfitness.ui.screens.*
import dev.mirzohidkhon.khonfitness.ui.theme.K

object Routes {
    const val TODAY = "today"; const val HISTORY = "history"; const val PROGRAMS = "programs"
    fun session(id: String) = "session/$id"
    fun program(id: String) = "program/$id"
    fun block(programId: String, blockId: String) = "block/$programId/$blockId"
    fun exercise(id: String) = "exercise/$id"
    fun modality(id: String) = "modality/$id"
    fun cardio(sessionId: String) = "cardio/$sessionId"
}

@Composable
fun KhonNav() {
    val nav = rememberNavController()
    val vm: AppViewModel = viewModel()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Routes.TODAY
    val tab = when { route.startsWith(Routes.HISTORY) -> 1; route.startsWith(Routes.PROGRAMS) || route.startsWith("program") || route.startsWith("block") || route.startsWith("exercise") || route.startsWith("modality") -> 2; else -> 0 }
    val showBar = route == Routes.TODAY || route == Routes.HISTORY || route == Routes.PROGRAMS
    Scaffold(containerColor = K.Bg, bottomBar = { if (showBar) BottomBar(tab) { i -> nav.navigate(listOf(Routes.TODAY, Routes.HISTORY, Routes.PROGRAMS)[i]) { popUpTo(Routes.TODAY) { saveState = true }; launchSingleTop = true; restoreState = true } } }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            NavHost(nav, startDestination = Routes.TODAY, enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None }, popEnterTransition = { EnterTransition.None }, popExitTransition = { ExitTransition.None }) {
                composable(Routes.TODAY) { TodayScreen(vm, nav) }
                composable(Routes.HISTORY) { HistoryScreen(vm, nav) }
                composable(Routes.PROGRAMS) { ProgramsScreen(vm, nav) }
                composable("session/{id}") { e -> SessionScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                composable("cardio/{id}") { e -> CardioScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                composable("program/{id}") { e -> ProgramEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                composable("block/{programId}/{blockId}") { e -> BlockEditorScreen(vm, nav, e.arguments?.getString("programId") ?: "", e.arguments?.getString("blockId") ?: "") }
                composable("exercise/{id}") { e -> ExerciseEditorScreen(vm, nav, e.arguments?.getString("id") ?: "new") }
                composable("modality/{id}") { e -> ModalityEditorScreen(vm, nav, e.arguments?.getString("id") ?: "new") }
            }
        }
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().background(K.Bg)) {
        HorizontalDivider(color = K.Divider)
        Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).height(64.dp)) {
            listOf("Today" to Icons.Today, "History" to Icons.History, "Programs" to Icons.Programs).forEachIndexed { i, (label, icon) ->
                val active = i == selected
                Column(
                    Modifier.weight(1f).fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(i) },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    Icon(icon, contentDescription = label, tint = if (active) K.Accent else K.Dim, modifier = Modifier.size(26.dp))
                    Text(label, fontSize = 12.sp, color = if (active) K.Accent else K.Dim, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
}
