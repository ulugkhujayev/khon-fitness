package dev.mirzohidkhon.khonfitness.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.time.LocalDate

class TargetsTest {
    private val dir: File = Files.createTempDirectory("targets").toFile()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store = PreferenceDataStoreFactory.create(scope = scope) { File(dir, "settings.preferences_pb") }
    private val targets = Targets(store)
    private val day = LocalDate.of(2026, 11, 20)

    @After fun close() { scope.cancel(); dir.deleteRecursively() }

    @Test fun aLiftTargetComesBackWithItsDateAndClears() = runBlocking {
        assertNull(targets.lift("bench").first())
        assertTrue(targets.setLift("bench", BigDecimal("100.5"), reps = false, date = day))
        assertEquals(LiftTarget(BigDecimal("100.5"), false, day), targets.lift("bench").first())
        assertTrue(targets.setLift("bench", null, reps = false, date = day))
        assertNull(targets.lift("bench").first())
        assertNull(store.data.first()[Targets.liftKey("bench")])
    }

    @Test fun eachExerciseHasItsOwnTarget() = runBlocking {
        targets.setLift("bench", BigDecimal(100), reps = false, date = day)
        targets.setLift("pullup", BigDecimal(15), reps = true, date = day.plusDays(1))
        assertEquals(LiftTarget(BigDecimal(100), false, day), targets.lift("bench").first())
        assertEquals(LiftTarget(BigDecimal(15), true, day.plusDays(1)), targets.lift("pullup").first())
        assertNull(targets.lift("row").first())
    }

    @Test fun theBodyweightTargetKeepsItsAverageAndDate() = runBlocking {
        assertNull(targets.body.first())
        assertTrue(targets.setBody(BigDecimal("80.0"), BigDecimal("83.2"), day))
        assertEquals(BodyTarget(BigDecimal("80.0"), BigDecimal("83.2"), day), targets.body.first())
        assertTrue(targets.setBody(BigDecimal("80.0"), null, day))
        assertEquals(BodyTarget(BigDecimal("80.0"), null, day), targets.body.first())
        assertTrue(targets.setBody(null, null, day))
        assertNull(targets.body.first())
    }

    @Test fun aValueOutsideItsLimitsStoresNothing() = runBlocking {
        targets.setLift("bench", BigDecimal(100), reps = false, date = day)
        for (v in listOf("0.5", "500.5", "100.3", "0")) assertFalse(v, targets.setLift("bench", BigDecimal(v), reps = false, date = day))
        for (v in listOf("0", "501", "7.5")) assertFalse(v, targets.setLift("pullup", BigDecimal(v), reps = true, date = day))
        for (v in listOf("19.9", "300.1", "80.25")) assertFalse(v, targets.setBody(BigDecimal(v), null, day))
        assertEquals(LiftTarget(BigDecimal(100), false, day), targets.lift("bench").first())
        assertNull(targets.lift("pullup").first())
        assertNull(targets.body.first())
        assertTrue(targets.setLift("bench", BigDecimal("500.0"), reps = false, date = day))
        assertTrue(targets.setLift("bench", BigDecimal("1.0"), reps = false, date = day))
        assertTrue(targets.setBody(BigDecimal("300.0"), null, day))
        assertTrue(targets.setBody(BigDecimal("20"), null, day))
    }

    @Test fun aBrokenStoredTextReadsAsNone() = runBlocking {
        val broken = listOf("", "kg|100", "kg|abc|2026-11-20", "lb|100|2026-11-20", "kg|100|2026-13-40", "reps|7.5|2026-11-20", "kg|600|2026-11-20", "kg|NaN|2026-11-20")
        for (text in broken) {
            store.edit { it[Targets.liftKey("bench")] = text }
            assertNull(text, targets.lift("bench").first())
        }
        for (text in listOf("80.0|abc|2026-11-20", "19|83.2|2026-11-20", "80.0|83.2|x", "80.0|-1|2026-11-20")) {
            store.edit { it[Targets.BODY_KEY] = text }
            assertNull(text, targets.body.first())
        }
        store.edit { it.remove(Targets.BODY_KEY); it[intPreferencesKey("target_bodyweight")] = 80 }
        assertNull(targets.body.first())
    }

    @Test fun clearAllRemovesEveryTargetAndKeepsOtherSettings() = runBlocking {
        targets.setLift("bench", BigDecimal(100), reps = false, date = day)
        targets.setBody(BigDecimal(80), null, day)
        store.edit { it[stringPreferencesKey("window_since")] = "kept" }
        targets.clearAll()
        assertNull(targets.lift("bench").first())
        assertNull(targets.body.first())
        assertEquals("kept", store.data.first()[stringPreferencesKey("window_since")])
    }
}
