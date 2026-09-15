package dev.mirzohidkhon.khonfitness.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Stable figure keys used by saved stretches and backups. */
object Figures {
    private val names = linkedMapOf(
        "elephant" to "Elephant walks", "fold" to "Hamstring stretch", "wgs" to "World's greatest stretch",
        "needle" to "Thread the needle", "hipflexor" to "Hip flexor stretch", "ninety" to "90/90 stretch",
        "shoulderir" to "Shoulder internal rotation", "catcow" to "Cat cow", "plow" to "Plow pose",
    )
    val keys: List<String> get() = names.keys.toList()
    fun name(key: String): String = names[key] ?: "No demonstration"
}

/** Library thumbnail. Full demonstrations use StretchDemonstration. */
@Composable
fun Figure(figure: String, modifier: Modifier = Modifier, mirror: Boolean = false) {
    DemoPoster(figure, modifier, mirror)
}
