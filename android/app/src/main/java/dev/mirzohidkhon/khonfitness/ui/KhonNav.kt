package dev.mirzohidkhon.khonfitness.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import dev.mirzohidkhon.khonfitness.ui.components.SlidingCapsuleRow
import dev.mirzohidkhon.khonfitness.ui.components.rememberTick
import dev.mirzohidkhon.khonfitness.ui.theme.Motion
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
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
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.ui.screens.*
import dev.mirzohidkhon.khonfitness.ui.theme.K

object Routes {
    const val TODAY = "today"; const val HISTORY = "history"; const val PROGRAMS = "programs" // the Plan tab; the route id stays
    fun session(id: String) = "session/$id"
    /** A saved session read-only, from Today's Review: no Edit. */
    fun review(sessionId: String) = "review/$sessionId"
    // A new program, block, exercise or stretch has its own route, never an id inside an edit route: a stored row
    // (a restored backup) can hold any id, "new" too.
    fun program(id: String) = "program/$id"; const val NEW_PROGRAM = "program-new"
    fun block(programId: String, blockId: String) = "block/$programId/$blockId"
    fun newBlock(programId: String) = "block-new/$programId"
    fun exercise(id: String) = "exercise/$id"
    fun newExercise(kind: String) = "exercise-new/$kind"
    fun cardio(sessionId: String) = "cardio/$sessionId"
    fun timer(sessionId: String) = "timer/$sessionId"
    fun stretch(routineId: String) = "stretch/$routineId"
    fun routine(id: String) = "routine/$id"
    fun stretchEditor(id: String) = "stretcheditor/$id"; const val NEW_STRETCH = "stretcheditor-new"
    /** An exercise's records and sessions (History, Progress). */
    fun exerciseDetail(id: String) = "lift/$id"
    /** A cardio type's charts and sessions (History, Progress). */
    fun cardioType(id: String) = "cardiotype/$id"
    /** Every strength exercise, searchable; each opens its exercise detail. */
    const val LIFTS = "lifts"
    const val STRETCHES = "stretches"
    const val IMPORT = "import"
    const val LIBRARY = "library"; const val WEEK_PLAN = "weekplan"; const val SETTINGS = "settings"
}

@Composable
fun KhonNav() {
    val nav = rememberNavController()
    val vm: AppViewModel = viewModel()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Routes.TODAY
    val tab = when { route.startsWith(Routes.HISTORY) -> 1; route.startsWith(Routes.PROGRAMS) || route.startsWith("program") || route.startsWith("block") || route.startsWith("exercise") || route == Routes.LIBRARY || route == Routes.WEEK_PLAN || route == Routes.STRETCHES || route.startsWith("routine") || route.startsWith("stretcheditor") -> 2; route == Routes.IMPORT -> 1; else -> 0 }
    // Widget and notification requests are consumed by Today. Return there first, or a request that lands on
    // another screen waits and fires later with stale data.
    val startRequested by dev.mirzohidkhon.khonfitness.MainActivity.startRequested.collectAsStateWithLifecycle()
    val windowRequested by dev.mirzohidkhon.khonfitness.MainActivity.windowRequested.collectAsStateWithLifecycle()
    LaunchedEffect(startRequested, windowRequested) {
        if ((startRequested || windowRequested) && nav.currentDestination?.route != Routes.TODAY) {
            if (!nav.popBackStack(Routes.TODAY, inclusive = false)) nav.navigate(Routes.TODAY) { launchSingleTop = true }
        }
    }
    val showBar = route in TABS
    // Tab screens shift sideways as they swap, the way the selected capsule slides; pushed screens just appear.
    val shiftPx = with(LocalDensity.current) { Motion.SHIFT_DP.dp.roundToPx() }
    val shiftSpec = tween<IntOffset>(Motion.SHIFT_MS)
    val fadeSpec = tween<Float>(Motion.SHIFT_MS)
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        tabShift(initialState.destination.route, targetState.destination.route)?.let { dir -> slideInHorizontally(shiftSpec) { dir * shiftPx } + fadeIn(fadeSpec) } ?: EnterTransition.None
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        tabShift(initialState.destination.route, targetState.destination.route)?.let { dir -> slideOutHorizontally(shiftSpec) { -dir * shiftPx } + fadeOut(fadeSpec) } ?: ExitTransition.None
    }
    Scaffold(containerColor = K.Bg, bottomBar = { if (showBar) BottomBar(tab) { i -> nav.openTab(TABS[i]) } }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).consumeWindowInsets(pad).imePadding()) {
            NavHost(nav, startDestination = Routes.TODAY, enterTransition = enter, exitTransition = exit, popEnterTransition = enter, popExitTransition = exit) {
                composable(Routes.TODAY) { TodayScreen(vm, nav) }
                composable(Routes.HISTORY) { HistoryScreen(vm, nav) }
                composable(Routes.PROGRAMS) { PlanScreen(vm, nav) }
                composable("session/{id}") { e -> SessionScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                composable("review/{id}") { e -> ReviewScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                composable("cardio/{id}") { e -> CardioScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                composable("program/{id}") { e -> ProgramEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                composable(Routes.NEW_PROGRAM) { ProgramEditorScreen(vm, nav, null) }
                composable("block/{programId}/{blockId}") { e -> BlockEditorScreen(vm, nav, e.arguments?.getString("programId") ?: "", e.arguments?.getString("blockId") ?: "") }
                composable("block-new/{programId}") { e -> BlockEditorScreen(vm, nav, e.arguments?.getString("programId") ?: "", null) }
                composable("exercise/{id}") { e -> ExerciseEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                composable("exercise-new/{kind}") { e -> ExerciseEditorScreen(vm, nav, null, newKind = e.arguments?.getString("kind") ?: Kind.STRENGTH) }
                composable("timer/{id}") { e -> TimerScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                composable("stretch/{id}") { e -> StretchScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                composable("routine/{id}") { e -> RoutineEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                composable("stretcheditor/{id}") { e -> StretchEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                composable(Routes.NEW_STRETCH) { StretchEditorScreen(vm, nav, null) }
                composable("lift/{id}") { e -> ExerciseDetailScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                composable("cardiotype/{id}") { e -> CardioTypeScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                composable(Routes.LIFTS) { LiftListScreen(vm, nav) }
                composable(Routes.STRETCHES) { StretchLibraryScreen(vm, nav) }
                composable(Routes.IMPORT) { ImportScreen(vm, nav) }
                composable(Routes.LIBRARY) { LibraryScreen(vm, nav) }
                composable(Routes.WEEK_PLAN) { WeekPlanScreen(vm, nav) }
                composable(Routes.SETTINGS) { SettingsScreen(vm, nav) }
            }
        }
    }
}

private val TABS = listOf(Routes.TODAY, Routes.HISTORY, Routes.PROGRAMS)

/**
 * Opens a tab from the tab bar. The tab left behind keeps its back stack and saved state (its scroll position among
 * them), and the tab opened gets its own back.
 */
fun NavHostController.openTab(route: String) = navigate(route) { popUpTo(Routes.TODAY) { saveState = true }; launchSingleTop = true; restoreState = true }

/** Direction a tab switch shifts the screens: 1 toward a tab on the right, -1 toward the left, null when not between tabs. */
internal fun tabShift(from: String?, to: String?): Int? {
    val a = TABS.indexOf(from); val b = TABS.indexOf(to)
    return if (a < 0 || b < 0 || a == b) null else if (b > a) 1 else -1
}

/**
 * Floating tab bar in the iOS 26 shape: a capsule that sits above the bottom edge, filled symbols, the selected
 * item on a Surface2 capsule tinted accent. The capsule slides to the tab tapped, with a light haptic click.
 */
@Composable
internal fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val tick = rememberTick()
    Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(start = 24.dp, end = 24.dp, bottom = 10.dp, top = 6.dp)) {
        SlidingCapsuleRow(
            count = 3, selected = selected, shape = RoundedCornerShape(26.dp), color = K.Surface2,
            modifier = Modifier.fillMaxWidth().height(60.dp)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(30.dp), ambientColor = Color.Black, spotColor = Color.Black)
                .clip(RoundedCornerShape(30.dp)).background(K.Surface)
                .border(1.dp, K.Hairline, RoundedCornerShape(30.dp))
                .padding(4.dp),
        ) {
            listOf("Today" to Icons.Today, "History" to Icons.History, "Plan" to Icons.Programs).forEachIndexed { i, (label, icon) ->
                val active = i == selected
                Column(
                    Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(26.dp))
                        .selectable(active, interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Tab) { if (!active) { tick(false); onSelect(i) } },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    // The label names the tab; the symbol is decorative.
                    Icon(icon, contentDescription = null, tint = if (active) K.Accent else K.Muted, modifier = Modifier.size(24.dp))
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (active) K.Accent else K.Muted, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

/** Leave a session screen whose session no longer exists, while that screen is still on top. Without this it draws nothing. */
@Composable
fun LeaveIfSessionMissing(nav: NavHostController, route: String, sessionId: String, loaded: Boolean, missing: Boolean) {
    LaunchedEffect(loaded, missing) {
        val top = nav.currentBackStackEntry
        if (loaded && missing && top?.destination?.route == route && top.arguments?.getString("id") == sessionId) {
            if (!nav.popBackStack(Routes.TODAY, inclusive = false)) nav.navigate(Routes.TODAY) { launchSingleTop = true }
        }
    }
}
