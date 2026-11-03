package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.Bodyweight
import dev.mirzohidkhon.khonfitness.data.EatingWindow
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.data.WindowDay
import dev.mirzohidkhon.khonfitness.health.ImportDraft
import dev.mirzohidkhon.khonfitness.update.Updater
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TodayRulesTest {
    private val start = 1_000_000L
    private fun min(m: Long) = start + m * 60_000

    @Test fun `progress line has no time in the first minute, then minutes, then hours`() {
        assertEquals("In progress", progressLine(start, start + 59_999))
        assertEquals("In progress · 1 min", progressLine(start, min(1)))
        assertEquals("In progress · 34 min", progressLine(start, min(34)))
        assertEquals("In progress · 1 h 5 min", progressLine(start, min(65)))
    }

    private fun log(id: String, session: String, status: String) =
        SetLog(id, session, 0, false, 0, 0, 0, "e", "Bench", 8, 12, 2.5, status = status)

    @Test fun `set progress counts the done sets of the running session only`() {
        val logs = listOf(log("1", "s", SetStatus.DONE), log("2", "s", SetStatus.SKIPPED), log("3", "s", SetStatus.PENDING), log("4", "other", SetStatus.DONE))
        assertEquals(SetProgress(1, 3), setProgress("s", logs))
    }

    @Test fun `routine line says its length and count, singular and plural, and done today`() {
        assertEquals("9 stretches · 10 min", routineSummary(600, 9))
        assertEquals("1 stretch · 1 min", routineSummary(45, 1))
        assertEquals("9 stretches · 10 min", routineLine("9 stretches · 10 min", doneToday = false))
        assertEquals("Done today · 9 stretches · 10 min", routineLine("9 stretches · 10 min", doneToday = true))
    }

    private val window = EatingWindow(enabled = true, startMinute = 12 * 60, endMinute = 20 * 60)
    private val day = LocalDate.of(2026, 10, 5)

    @Test fun `window line is open with time to close, closed with the opening hour, or off`() {
        assertEquals("Open · closes in 3 h 20 m", windowStatus(window, null, day.atTime(16, 40)))
        assertEquals("Closed · opens 12:00", windowStatus(window, null, day.atTime(9, 0)))
        assertEquals("Off", windowStatus(window.copy(enabled = false), null, day.atTime(16, 40)))
        assertEquals("Off", windowStatus(null, null, day.atTime(16, 40)))
    }

    @Test fun `window line follows a manual close`() {
        val closed = WindowDay(day.toString(), closedAt = day.atTime(15, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
        assertEquals("Closed · opens 12:00", windowStatus(window, closed, LocalDateTime.of(day, java.time.LocalTime.of(16, 40))))
    }

    @Test fun `band row and import title count sessions, singular and plural`() {
        assertEquals("1 new session", pendingTitle(1))
        assertEquals("2 new sessions", pendingTitle(2))
        assertEquals("1 new session from the band", pendingLine(1))
        assertEquals("3 new sessions from the band", pendingLine(3))
    }

    @Test fun `import button names one, all, or the matched ones`() {
        assertEquals("Import 1 session", importAllLabel(1, 1))
        assertEquals("Import all 3 sessions", importAllLabel(3, 3))
        assertEquals("Import 2 matched", importAllLabel(2, 3))
    }

    @Test fun `import row line gives the date and the numbers it has`() {
        val zone = ZoneId.systemDefault()
        val from = LocalDateTime.of(2026, 10, 5, 7, 0).atZone(zone).toInstant()
        val d = ImportDraft("x", from, from.plusSeconds(32 * 60 + 40), "Running", "run", 5200, 141, emptyList(), listOf("com.xiaomi.wearable"))
        assertEquals("${LocalDate.of(2026, 10, 5).format(shortDate)} · 32 min · 5.2 km · 141 bpm · Mi Fitness", draftStats(d))
        assertEquals("${LocalDate.of(2026, 10, 5).format(shortDate)} · 32 min · Mi Fitness", draftStats(d.copy(distanceM = null, avgHr = null)))
        // The 0.4.8 row named the app that wrote the workout; two apps merge into one draft.
        assertEquals("${LocalDate.of(2026, 10, 5).format(shortDate)} · 32 min · Mi Fitness + Strava", draftStats(d.copy(distanceM = null, avgHr = null, origins = listOf("com.xiaomi.wearable", "com.strava"))))
    }

    private val today = LocalDate.of(2026, 10, 7)
    private val weights = listOf(Bodyweight("2026-10-01", 80.0), Bodyweight("2026-10-05", 80.4))

    @Test fun `bodyweight status says no entry, the last entry, or saved for today with the change since the day before`() {
        assertEquals("No entry yet", bodyweightStatus(emptyList(), today))
        assertEquals("Last: 80.4 kg · Oct 5", bodyweightStatus(weights, today))
        assertEquals("Saved for today · +0.2 kg since Oct 5", bodyweightStatus(weights + Bodyweight("2026-10-07", 80.6), today))
        assertEquals("Saved for today · −0.4 kg since Oct 5", bodyweightStatus(weights + Bodyweight("2026-10-07", 80.0), today))
        assertEquals("Saved for today · same as Oct 5", bodyweightStatus(weights + Bodyweight("2026-10-07", 80.4), today))
        assertEquals("Saved for today", bodyweightStatus(listOf(Bodyweight("2026-10-07", 81.0)), today))
    }

    @Test fun `entry button is Save for a new day, Update for another value, Saved when equal`() {
        assertEquals(EntryAction("Save", true), entryAction(weights, today, 80.6))
        val withToday = weights + Bodyweight("2026-10-07", 80.6)
        assertEquals(EntryAction("Update", true), entryAction(withToday, today, 80.7))
        assertEquals(EntryAction("Saved", false), entryAction(withToday, today, 80.6))
    }

    @Test fun `entry starts at the latest entry, or at 75 kg before the first`() {
        assertEquals(80.4, startKg(weights), 0.0)
        assertEquals(75.0, startKg(emptyList()), 0.0)
        assertEquals("75.0", kgText(startKg(emptyList())))
    }

    @Test fun `typed weight reads a comma or a dot, and only 20 to 250 kg`() {
        assertEquals(83.5, parseKg("83,5")!!, 0.0)
        assertEquals(83.5, parseKg("83.5")!!, 0.0)
        assertEquals(83.0, parseKg("83")!!, 0.0)
        assertEquals(83.0, parseKg("83.")!!, 0.0)
        assertEquals(20.0, parseKg("20")!!, 0.0)
        assertEquals(250.0, parseKg("250")!!, 0.0)
        for (bad in listOf("", " ", "0", "19", "19.9", "250.1", "251", "abc", "8a", ".", ",", "1e2", "83.5.1", "83.55555", "0083.500", "1234567890")) assertNull(bad, parseKg(bad))
        // Seven characters is the longest weight: "250.0" and a slip.
        assertEquals(83.6, parseKg("83.5555")!!, 0.0)
    }

    @Test fun `entry button is off while the typed text is not a weight`() {
        assertEquals(EntryAction("Save", false), entryAction(weights, today, parseKg("")))
        val withToday = weights + Bodyweight("2026-10-07", 80.6)
        assertEquals(EntryAction("Update", false), entryAction(withToday, today, parseKg("251")))
        assertEquals(EntryAction("Save", false), entryAction(weights, today, parseKg("0")))
    }

    @Test fun `minus and plus step 0_1 kg inside 20 to 250`() {
        assertEquals(83.1, stepKg("83.0", 1, 75.0), 0.0)
        assertEquals(82.9, stepKg("83,0", -1, 75.0), 0.0)
        assertEquals(250.0, stepKg("250.0", 1, 75.0), 0.0)
        assertEquals(20.0, stepKg("20.0", -1, 75.0), 0.0)
        // A number outside the range steps from the nearer end; text that is no number steps from the fallback.
        assertEquals(20.1, stepKg("19", 1, 75.0), 0.0)
        assertEquals(250.0, stepKg("300", 1, 75.0), 0.0)
        assertEquals(75.1, stepKg("abc", 1, 75.0), 0.0)
        assertEquals(74.9, stepKg("", -1, 75.0), 0.0)
    }

    private fun weighted(id: String, kg: Double, reps: Int, status: String = SetStatus.DONE) =
        SetLog(id, "s", 0, false, 0, 0, 0, "e", "Bench", 8, 12, 2.5, weightKg = kg, reps = reps, status = status)

    @Test fun `done line for a program gives sets, tonnes and minutes, and says d of n when sets were skipped`() {
        val program = Session("s", "2026-10-07", start, start + 58 * 60_000, ItemType.PROGRAM, programId = "p")
        assertEquals("Done · 1 set · 0.8 t · 58 min", doneLine(program, listOf(weighted("1", 80.0, 10))))
        val logs = listOf(weighted("1", 80.0, 10), weighted("2", 100.0, 8), weighted("3", 100.0, 8, SetStatus.SKIPPED), weighted("4", 60.0, 10).copy(sessionId = "other"))
        assertEquals("Done · 2 of 3 sets · 1.6 t · 58 min", doneLine(program, logs))
        assertEquals("Done · 2 sets · 1.6 t · 58 min", doneLine(program, logs.take(2)))
        val oneOfTwelve = listOf(weighted("1", 80.0, 10)) + (2..12).map { weighted("$it", 80.0, 10, SetStatus.SKIPPED) }
        assertEquals("Done · 1 of 12 sets · 0.8 t · 58 min", doneLine(program, oneOfTwelve))
        assertEquals("Done · 0 sets · 0 t · 1 h 5 min", doneLine(program.copy(finishedAt = start + 65 * 60_000), emptyList()))
    }

    @Test fun `done line for cardio gives distance, time and pace`() {
        val cardio = Session("c", "2026-10-07", start, start + 1_800_000, ItemType.CARDIO, exerciseId = "run", timeSec = 1830, distanceM = 5200)
        assertEquals("Done · 5.2 km · 30 min · 5:52 /km", doneLine(cardio, emptyList()))
        assertEquals("Done · 30 min", doneLine(cardio.copy(distanceM = null), emptyList()))
        assertEquals("Done · 5.2 km", doneLine(cardio.copy(timeSec = null), emptyList()))
        assertEquals("Done", doneLine(cardio.copy(timeSec = null, distanceM = null), emptyList()))
    }

    @Test fun `done line for stretching says done today with its length, singular and plural`() {
        assertEquals("Done today · 1 stretch · 1 min", routineLine(routineSummary(45, 1), doneToday = true))
        assertEquals("Done today · 10 stretches · 12 min", routineLine(routineSummary(700, 10), doneToday = true))
    }

    private val release = Updater.Release("v0.5.0", assets = listOf(Updater.Asset("khon-fitness.apk", "https://example.invalid/khon-fitness.apk")))
    private val available = Updater.Result.Available(release, release.assets.first())

    @Test fun `update row names the release and says tap to install`() {
        assertEquals(UpdateRow("Update to v0.5.0", "Tap to install", downloading = false), updateRow(available, null))
    }

    @Test fun `update row shows the download in whole percent and takes no tap`() {
        assertEquals(UpdateRow("Update to v0.5.0", "Downloading 0%", downloading = true), updateRow(available, 0f))
        assertEquals(UpdateRow("Update to v0.5.0", "Downloading 42%", downloading = true), updateRow(available, 0.429f))
    }

    @Test fun `no update row without a newer release`() {
        assertNull(updateRow(null, null))
        assertNull(updateRow(Updater.Result.UpToDate("0.5.0"), null))
        assertNull(updateRow(Updater.Result.Failed("offline"), null))
    }

    @Test fun `a second install tap while the first download starts does nothing`() {
        var progress: Float? = null
        var downloads = 0
        // runUpdate's order: check, then set progress, then launch the download.
        fun tap() { if (startsUpdate(available, progress)) { progress = 0f; downloads++ } }
        tap(); tap()
        assertEquals(1, downloads)
        // After a failure the progress clears and a new tap starts again.
        progress = null
        tap()
        assertEquals(2, downloads)
        assertFalse(startsUpdate(null, null))
        assertFalse(startsUpdate(Updater.Result.UpToDate("0.5.0"), null))
    }
}
