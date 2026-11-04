package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * The set whose kg field takes focus next ([target]). A check sets it ([aim]) while the list may still scroll to a wrap
 * target off screen, so focus waits until that field is placed ([rememberFieldFocus]). A block header tap cancels it.
 */
@Stable
class PendingFocus {
    var target by mutableStateOf<String?>(null)
        private set
    fun aim(id: String?) { target = id }
    fun cancel() { target = null }
    /** Clears the target once [id]'s field took focus; a newer target stays. */
    fun taken(id: String) { if (target == id) target = null }
}

/** One field's share of [PendingFocus]: [requester] goes on the field, [placed] on the field or its parent. */
class FieldFocus(val requester: FocusRequester, val placed: Modifier)

/** Focus lands on field [id] once, when it is the target and has been placed (attached wherever the list scrolled from); then the target clears. */
@Composable
fun rememberFieldFocus(id: String, pending: PendingFocus): FieldFocus {
    val requester = remember { FocusRequester() }
    var placed by remember { mutableStateOf(false) }
    if (pending.target == id && placed) LaunchedEffect(Unit) { runCatching { requester.requestFocus() }; pending.taken(id) }
    return remember(requester) { FieldFocus(requester, Modifier.onGloballyPositioned { if (!placed) placed = true }) }
}
