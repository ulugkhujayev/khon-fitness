package dev.mirzohidkhon.khonfitness.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
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
        assertTrue(targets.setBody(BigDecimal("80.0"), EntryAverage(BigDecimal("240.1"), 3), day))
        assertEquals(BodyTarget(BigDecimal("80.0"), EntryAverage(BigDecimal("240.1"), 3), day), targets.body.first())
        assertEquals("80.0|240.1|3|2026-11-20", store.data.first()[Targets.BODY_KEY])
        assertTrue(targets.setBody(BigDecimal("80.0"), null, day))
        assertEquals(BodyTarget(BigDecimal("80.0"), null, day), targets.body.first())
        assertTrue(targets.setBody(BigDecimal("80"), null, day))
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

    @Test fun aRecordInTheOldFormReadsItsAverageAsExact() = runBlocking {
        store.edit { it[Targets.BODY_KEY] = "80.0|83.2|2026-11-20" }
        assertEquals(BodyTarget(BigDecimal("80.0"), EntryAverage(BigDecimal("83.2"), 1), day), targets.body.first())
        store.edit { it[Targets.BODY_KEY] = "80.0||2026-11-20" }
        assertEquals(BodyTarget(BigDecimal("80.0"), null, day), targets.body.first())
        store.edit { it[Targets.BODY_KEY] = "80.0|||2026-11-20" }
        assertEquals(BodyTarget(BigDecimal("80.0"), null, day), targets.body.first())
    }

    @Test fun storedNumbersHaveLimits() = runBlocking {
        val bodyBroken = listOf(
            "80.0|1e-2147483647|2026-11-20", "80.0|1e+2147483647|2026-11-20", "8E1|83.2|2026-11-20", "80.0|8.32E1|2026-11-20",
            "80.0|240.1|3E0|2026-11-20", "80.0|83.20000000001|2026-11-20", "80.0000000000|83.2|2026-11-20",
            "80.0|240.1|0|2026-11-20", "80.0|240.1|-3|2026-11-20", "80.0|-240.1|3|2026-11-20", "80.0|0|1|2026-11-20",
            "80.0|19|1|2026-11-20", "80.0|301|1|2026-11-20", "80.0|19|2026-11-20", "80.0|301|2026-11-20", "80.0|57|3|2026-11-20",
            "80.0|240.1|1001|2026-11-20", "80.0|240.1|99999999999999999999|2026-11-20", "80.0|240.1||2026-11-20", "80.0||3|2026-11-20",
            "80.0|240.1|3|2026-02-30", "80.0|240.1|3|+9999999999-12-31", "80.0|240.1|3|20261120", "80.0|240.1|3|", "80.0|240.1|3|2026-11-20|x",
            " 80.0|83.2|2026-11-20", "80.0|83.2 |2026-11-20", "80,0|83.2|2026-11-20", "\u0668\u0660|83.2|2026-11-20", "80.0|" + "9".repeat(5000) + "|2026-11-20",
        )
        for (text in bodyBroken) {
            store.edit { it[Targets.BODY_KEY] = text }
            assertNull(text.take(60), targets.body.first())
        }
        for (text in listOf("80.0|20|1|2026-11-20", "80.0|300|1|2026-11-20", "80.0|300000|1000|2026-11-20", "80.0|20|2026-11-20", "80.0|240.1|3|2099-01-01")) {
            store.edit { it[Targets.BODY_KEY] = text }
            assertTrue(text, targets.body.first() != null)
        }
        val liftBroken = listOf("kg|1e2|2026-11-20", "kg|1E+2|2026-11-20", "kg|100.0000000000|2026-11-20", "reps|-5|2026-11-20", "kg|100|2026-02-30", "kg|100|+9999999999-12-31", "kg|" + "1".repeat(5000) + "|2026-11-20")
        for (text in liftBroken) {
            store.edit { it[Targets.liftKey("bench")] = text }
            assertNull(text.take(60), targets.lift("bench").first())
        }
    }

    @Test fun anEntryAverageOutsideItsLimitsIsNotStored() = runBlocking {
        assertTrue(targets.setBody(BigDecimal("80.0"), EntryAverage(BigDecimal("19"), 1), day))
        assertEquals(BodyTarget(BigDecimal("80.0"), null, day), targets.body.first())
        assertTrue(targets.setBody(BigDecimal("80.0"), EntryAverage(BigDecimal("80.123456789012"), 1), day))
        assertEquals(BodyTarget(BigDecimal("80.0"), null, day), targets.body.first())
    }

    @Test fun aStoredDateFarAfterTodayStillReads() = runBlocking {
        store.edit { it[Targets.BODY_KEY] = "80.0|240.1|3|+10000-01-01" }
        assertEquals(BodyTarget(BigDecimal("80.0"), EntryAverage(BigDecimal("240.1"), 3), LocalDate.of(10000, 1, 1)), targets.body.first())
        store.edit { it[Targets.liftKey("bench")] = "kg|100|+999999999-12-31" }
        assertEquals(LiftTarget(BigDecimal(100), false, LocalDate.MAX), targets.lift("bench").first())
    }

    @Test fun writesLandInTheOrderOfTheirCalls() = runBlocking {
        repeat(200) { i ->
            val first = launch(start = CoroutineStart.UNDISPATCHED) { targets.setLift("bench", BigDecimal(100 + i % 50), reps = false, date = day) }
            val second = launch(start = CoroutineStart.UNDISPATCHED) { targets.setLift("bench", BigDecimal(15), reps = true, date = day) }
            joinAll(first, second)
            assertEquals("pair $i", LiftTarget(BigDecimal(15), true, day), targets.lift("bench").first())
        }
    }

    @Test fun aSaveOfTheStoredValueKeepsItsRecord() = runBlocking {
        val later = day.plusDays(3)
        targets.setLift("bench", BigDecimal(100), reps = false, date = day)
        targets.setLift("bench", BigDecimal("100.0"), reps = false, date = later, keepEqual = true)
        assertEquals(LiftTarget(BigDecimal(100), false, day), targets.lift("bench").first())
        targets.setLift("bench", BigDecimal(100), reps = true, date = later, keepEqual = true)
        assertEquals(LiftTarget(BigDecimal(100), true, later), targets.lift("bench").first())
        targets.setLift("bench", BigDecimal(101), reps = true, date = day, keepEqual = true)
        assertEquals(LiftTarget(BigDecimal(101), true, day), targets.lift("bench").first())

        val average = EntryAverage(BigDecimal("240.1"), 3)
        targets.setBody(BigDecimal("80.0"), average, day)
        targets.setBody(BigDecimal("80"), null, later, keepEqual = true)
        assertEquals(BodyTarget(BigDecimal("80.0"), average, day), targets.body.first())
        targets.setBody(BigDecimal("79.9"), null, later, keepEqual = true)
        assertEquals(BodyTarget(BigDecimal("79.9"), null, later), targets.body.first())
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
