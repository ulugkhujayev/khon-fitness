package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

/**
 * The one exit guard of a screen that writes and then leaves: every closing action (Done, Save, Archive, Delete, Merge,
 * Discard, Back, and system Back) takes it ([Once]), so no two of them run or pop the screen under it too. A refused
 * action [release]s it. [pop] pops only while this screen's back stack entry is still the current one, so an action
 * that completes late (after system Back already left) never pops another screen.
 */
class ExitGuard internal constructor(private val nav: NavController, private val entryId: String?) {
    private val once = Once()
    operator fun invoke(action: () -> Unit) = once(action)
    /** A refused action gives the guard back, so the next closing action runs. */
    fun release() = once.release()
    /** Pops this screen while it is on top; [orElse] runs when it is the last entry, so there is nothing to pop to. */
    fun pop(orElse: () -> Unit = {}) {
        if (!onTop()) return
        if (!nav.popBackStack()) orElse()
    }
    /** Runs [go], a navigation away from this screen (the timer's to the cardio form), only while this screen is on top. */
    fun leave(go: () -> Unit) { if (onTop()) go() }
    private fun onTop() = entryId == null || nav.currentBackStackEntry?.id == entryId
}

/**
 * The screen's [ExitGuard]. System Back (and predictive back) goes through it as the screen's Back: [onBack], by
 * default `pop()`, runs under the guard. The callback stays on the dispatcher while the screen is composed, also after
 * the screen popped itself (its entry stops at once, which would drop a `BackHandler`), so a system Back in the same
 * frame as Done meets the taken guard instead of the NavHost's Back. It follows the activity's lifecycle, as the
 * NavHost's does, and is added after it, so it stays first in line after the app comes back to the foreground.
 */
@Composable
fun rememberExitGuard(nav: NavController, onBack: ExitGuard.() -> Unit = { pop() }): ExitGuard {
    val owner = LocalLifecycleOwner.current
    val guard = remember(nav) { ExitGuard(nav, ((owner as? NavBackStackEntry) ?: nav.currentBackStackEntry)?.id) }
    val back by rememberUpdatedState(onBack)
    val callback = remember(guard) { object : OnBackPressedCallback(true) { override fun handleOnBackPressed() { guard { guard.back() } } } }
    val dispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    DisposableEffect(dispatcherOwner, callback) {
        dispatcherOwner?.onBackPressedDispatcher?.addCallback(dispatcherOwner, callback)
        onDispose { callback.remove() }
    }
    return guard
}
