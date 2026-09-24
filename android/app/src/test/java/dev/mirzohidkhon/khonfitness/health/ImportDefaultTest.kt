package dev.mirzohidkhon.khonfitness.health

import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImportDefaultTest {
    private val nor = Exercise("nor4x4", "Norwegian 4x4", Kind.CARDIO, modalityId = "bike", intervals = true)
    private val z2bike = Exercise("z2bike", "Zone 2 bike", Kind.CARDIO, modalityId = "bike")
    private val z2run = Exercise("z2run", "Zone 2 run", Kind.CARDIO, modalityId = "run")
    private val cardio = listOf(nor, z2bike, z2run)

    @Test fun `a workout with no modality gets no exercise, even when cardio is planned`() {
        assertNull(HealthImport.defaultExercise(null, nor, cardio))
        assertNull(HealthImport.defaultExercise(null, null, cardio))
    }

    @Test fun `a modality with no exercise gets none instead of the first cardio`() {
        assertNull(HealthImport.defaultExercise("row", null, cardio))
    }

    @Test fun `planned cardio wins when its modality matches, else the plainest of that modality`() {
        assertEquals(nor, HealthImport.defaultExercise("bike", nor, cardio))
        assertEquals(z2bike, HealthImport.defaultExercise("bike", z2run, cardio))
        assertEquals(z2run, HealthImport.defaultExercise("run", null, cardio))
    }
}
