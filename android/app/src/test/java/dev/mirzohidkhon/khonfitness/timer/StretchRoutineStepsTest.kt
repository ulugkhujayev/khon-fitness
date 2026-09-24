package dev.mirzohidkhon.khonfitness.timer

import dev.mirzohidkhon.khonfitness.data.RoutineStretch
import dev.mirzohidkhon.khonfitness.data.Stretch
import dev.mirzohidkhon.khonfitness.ui.screens.routineSteps
import dev.mirzohidkhon.khonfitness.ui.screens.routineSeconds
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class StretchRoutineStepsTest {
    @Test fun `routine duration accepts punctuation in a stretch name`() {
        val stretch = Stretch(id = "stretch-1", name = "Audit|pipe", seconds = 45, figure = "hipflexor")
        val entry = RoutineStretch(id = "entry-1", routineId = "routine-1", stretchId = stretch.id, sortOrder = 0)

        assertEquals(45, routineSeconds("routine-1", listOf(entry), listOf(stretch)))
    }

    @Test fun `service phase data preserves punctuation and both sides`() {
        val stretch = Stretch(id = "stretch-1", name = "Left|right, breathe", sided = true,
            seconds = 45, figure = "hipflexor")
        val entry = RoutineStretch(id = "entry-1", routineId = "routine-1", stretchId = stretch.id, sortOrder = 0)
        val phases = routineSteps("routine-1", listOf(entry), listOf(stretch))
        val decoded = Json.decodeFromString<List<Phase>>(Json.encodeToString(phases))

        assertEquals(phases, decoded)
        assertEquals(listOf("Left", "Right"), decoded.map { it.side })
        assertEquals(listOf("Left|right, breathe", "Left|right, breathe"), decoded.map { it.label })
        assertEquals(90, routineSeconds("routine-1", listOf(entry), listOf(stretch)))
    }
}
