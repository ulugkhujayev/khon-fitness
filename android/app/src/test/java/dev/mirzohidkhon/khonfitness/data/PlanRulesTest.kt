package dev.mirzohidkhon.khonfitness.data

import java.time.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanRulesTest {
    private val today = LocalDate.of(2026, 9, 24) // Thursday
    private val swim = Exercise("z2swim", "Zone 2 swim", Kind.CARDIO)
    private val run = Exercise("z2run", "Zone 2 run", Kind.CARDIO)
    private val tp = Program("tpv3", "TP v3")
    private fun data(vararg ov: DayOverride) = KhonRepository.PlanData(listOf(WeekPlan(4, ItemType.CARDIO, "z2run")), ov.toList(), listOf(tp), listOf(swim, run))

    @Test fun `auto override sets the item but shows no mark`() {
        val item = data(DayOverride("2026-09-17", ItemType.CARDIO, "z2swim", auto = true)).itemFor(LocalDate.of(2026, 9, 17))
        assertEquals("Zone 2 swim", item.name)
        assertFalse(item.overridden)
    }

    @Test fun `user override shows the mark and template days do not`() {
        val plan = data(DayOverride("2026-09-17", ItemType.PROGRAM, "tpv3"))
        assertEquals("TP v3", plan.itemFor(LocalDate.of(2026, 9, 17)).name)
        assertTrue(plan.itemFor(LocalDate.of(2026, 9, 17)).overridden)
        assertEquals("Zone 2 run", plan.itemFor(today).name)
        assertFalse(plan.itemFor(today).overridden)
    }

    @Test fun `auto rest override stays unmarked`() {
        val item = data(DayOverride("2026-09-17", ItemType.REST, null, auto = true)).itemFor(LocalDate.of(2026, 9, 17))
        assertTrue(item.isRest)
        assertFalse(item.overridden)
    }

    @Test fun `first use is the earliest date or null`() {
        assertEquals(LocalDate.of(2026, 8, 3), PlanRules.firstUse(listOf("2026-09-01", "2026-08-03", "", "2026-08-10")))
        assertNull(PlanRules.firstUse(emptyList()))
    }

    @Test fun `freeze starts a year back even when first use is recent or unknown`() {
        assertEquals(LocalDate.of(2025, 9, 24), PlanRules.freezeFrom(listOf("2026-09-23"), today))
        assertEquals(LocalDate.of(2025, 9, 24), PlanRules.freezeFrom(emptyList(), today))
        assertEquals(LocalDate.of(2024, 1, 5), PlanRules.freezeFrom(listOf("2024-01-05", "2026-09-23"), today))
    }

    @Test fun `freeze covers past weekdays from first use to yesterday and skips overrides`() {
        val dates = PlanRules.datesToFreeze(4, LocalDate.of(2026, 9, 1), today, setOf("2026-09-10"))
        assertEquals(listOf(LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 17)), dates)
    }

    @Test fun `freeze writes nothing without a first use date or when first use is today`() {
        assertTrue(PlanRules.datesToFreeze(4, null, today, emptySet()).isEmpty())
        assertTrue(PlanRules.datesToFreeze(4, today, today, emptySet()).isEmpty())
    }

    @Test fun `swap needs two different dates from today on`() {
        assertTrue(PlanRules.canSwap(today, today.plusDays(1), today))
        assertTrue(PlanRules.canSwap(today.plusDays(9), today.plusDays(2), today))
        assertFalse(PlanRules.canSwap(today, today.minusDays(1), today))
        assertFalse(PlanRules.canSwap(today.minusDays(3), today.plusDays(1), today))
        assertFalse(PlanRules.canSwap(today, today, today))
    }

    @Test fun `backup keeps the auto flag and reads old overrides as user edits`() {
        val json = Json { ignoreUnknownKeys = true }
        val auto = DayOverride("2026-09-17", ItemType.CARDIO, "z2swim", auto = true)
        assertEquals(auto, json.decodeFromString<DayOverride>(json.encodeToString(auto)))
        assertFalse(json.decodeFromString<DayOverride>("""{"date":"2026-09-17","itemType":"rest"}""").auto)
    }
}
