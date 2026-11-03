package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController

/**
 * Leaves its own screen once, and only while that screen's back stack entry is on top. A late close (a run's end, a
 * delayed close) after Back has already left, or Back and a close in one frame, never pops the screen under it.
 */
class ExitGuard(private val nav: NavHostController, private val own: NavBackStackEntry?) {
    private var left = false

    /** Pops [own] if it is still on top and has not left yet; true when it popped. */
    fun leave(): Boolean {
        if (left || own == null || nav.currentBackStackEntry?.id != own.id) return false
        left = true
        return nav.popBackStack()
    }
}

/**
 * An [ExitGuard] for the screen composed here. System Back goes through it too, so it counts as the screen's one exit.
 * Its callback lives on the activity, not on the screen's entry: a popped entry drops its own callbacks at once, and
 * NavHost's Back, still on until it recomposes, would then pop the screen under it.
 */
@Composable
fun rememberExitGuard(nav: NavHostController): ExitGuard {
    val owner = LocalLifecycleOwner.current
    val guard = remember(nav) { ExitGuard(nav, owner as? NavBackStackEntry ?: nav.currentBackStackEntry) }
    val back = LocalOnBackPressedDispatcherOwner.current
    DisposableEffect(back, guard) {
        val callback = object : OnBackPressedCallback(true) { override fun handleOnBackPressed() { guard.leave() } }
        // Added after NavHost's, so it goes first; re-added after it when the activity starts again.
        val dispatcher = back?.onBackPressedDispatcher
        if (back is LifecycleOwner) dispatcher?.addCallback(back, callback) else dispatcher?.addCallback(callback)
        onDispose { callback.remove() }
    }
    return guard
}
