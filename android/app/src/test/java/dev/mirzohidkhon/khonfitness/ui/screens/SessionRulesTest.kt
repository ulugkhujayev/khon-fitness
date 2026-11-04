package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRulesTest {
    private var order = 0
    private fun set(block: Int, slot: Int = 0, index: Int = 0, status: String = SetStatus.PENDING, kg: Double? = 60.0, reps: Int? = 8, exercise: String = "ex$block$slot", session: String = "s") =
        SetLog("$session-$block-$slot-$index", session, block, false, slot, index, order++, exercise, "Name $block$slot", 6, 10, 2.5, kg, reps, status)

    private fun done(l: SetLog) = l.copy(status = SetStatus.DONE)

    // Three blocks of two sets each.
    private val a1 = set(0, index = 0); private val a2 = set(0, index = 1)
    private val b1 = set(1, index = 0); private val b2 = set(1, index = 1)
    private val c1 = set(2, index = 0); private val c2 = set(2, index = 1)

    @Test fun `opening the session opens only the first block with work`() {
        assertEquals(0, firstOpenBlock(groupBlocks(listOf(a1, a2, b1, b2))))
        assertEquals(1, firstOpenBlock(groupBlocks(listOf(done(a1), done(a2), b1, b2))))
        // Nothing pending: the first block.
        assertEquals(0, firstOpenBlock(groupBlocks(listOf(done(a1), done(b1)))))
        assertEquals(1, openBlock(groupBlocks(listOf(done(a1), done(a2), b1)), picked = false, chosen = null))
        assertNull(openBlock(emptyList(), picked = false, chosen = null))
    }

    @Test fun `a picked block wins over the first with work, and none open stays none`() {
        val blocks = groupBlocks(listOf(a1, b1))
        assertEquals(1, openBlock(blocks, picked = true, chosen = 1))
        assertNull(openBlock(blocks, picked = true, chosen = null))
    }

    @Test fun `blocks group by block index in execution order`() {
        val later = set(0, index = 2).copy(order = -1)
        assertEquals(listOf(listOf(later, a1, a2), listOf(b1)), groupBlocks(listOf(b1, a2, a1, later)))
    }

    @Test fun `a check with sets left keeps the block open and focuses the next pending set`() {
        val a3 = set(0, index = 2)
        val after = afterCheck(groupBlocks(listOf(a1, a2, a3, b1)), a1, live = true)!!
        assertEquals(0, after.open)
        assertEquals(a2.id, after.focus?.id)
    }

    @Test fun `with no later pending set in the block, focus goes to one left behind`() {
        val after = afterCheck(groupBlocks(listOf(a1, a2, b1)), a2, live = true)!!
        assertEquals(0, after.open)
        assertEquals(a1.id, after.focus?.id)
    }

    @Test fun `the block's last set opens the next block with work, focus on its first pending set`() {
        val after = afterCheck(groupBlocks(listOf(done(a1), a2, done(b1), b2, c1, c2)), a2, live = true)!!
        assertEquals(1, after.open)
        assertEquals(b2.id, after.focus?.id)
    }

    @Test fun `a done block is passed over`() {
        val after = afterCheck(groupBlocks(listOf(done(a1), a2, done(b1), done(b2), c1, c2)), a2, live = true)!!
        assertEquals(2, after.open)
        assertEquals(c1.id, after.focus?.id)
    }

    @Test fun `with no later block left it wraps to an earlier one`() {
        val after = afterCheck(groupBlocks(listOf(a1, a2, done(b1), done(b2), done(c1), c2)), c2, live = true)!!
        assertEquals(0, after.open)
        assertEquals(a1.id, after.focus?.id)
    }

    @Test fun `the last set of the workout closes every block`() {
        val after = afterCheck(groupBlocks(listOf(done(a1), done(a2), done(b1), b2)), b2, live = true)!!
        assertNull(after.open)
        assertNull(after.focus)
    }

    @Test fun `skipped sets count as no work`() {
        val after = afterCheck(groupBlocks(listOf(a1, b1.copy(status = SetStatus.SKIPPED))), a1, live = true)!!
        assertNull(after.open)
    }

    @Test fun `in History edit mode a check changes the set and nothing else`() {
        // A saved session's last skipped set, checked while editing: a live session would close every block.
        val blocks = groupBlocks(listOf(done(a1), done(a2), b1.copy(status = SetStatus.SKIPPED)))
        assertEquals(AfterCheck(null, null), afterCheck(blocks, b1.copy(status = SetStatus.SKIPPED), live = true))
        assertNull(afterCheck(blocks, b1.copy(status = SetStatus.SKIPPED), live = false))
        // With sets left in the block and other blocks with work: still nothing moves, no focus.
        assertNull(afterCheck(groupBlocks(listOf(a1, a2, b1)), a2, live = false))
        assertNull(afterCheck(groupBlocks(listOf(done(a1), a2, b1)), a2, live = false))
    }

    @Test fun `block header text`() {
        val bench = set(3, slot = 0).copy(exerciseName = "Bench Press")
        val press = set(3, slot = 1).copy(exerciseName = "Leg Press")
        assertEquals("Bench Press · Leg Press", blockTitle(listOf(press, bench, done(press))))
        assertEquals(1, doneCount(listOf(done(bench), press)))
        assertEquals(false, isBlockDone(listOf(done(bench), press)))
        assertEquals(true, isBlockDone(listOf(done(bench), press.copy(status = SetStatus.SKIPPED))))
        assertEquals("Block 1, Bench Press, 2 of 6 sets done", blockLabel(0, "Bench Press", 2, 6, false))
        assertEquals("Block 2, Row, 1 of 1 set done, block done", blockLabel(1, "Row", 1, 1, true))
    }

    @Test fun `summary counts time, sets done, volume and new bests`() {
        val start = 1_000_000L
        // Earlier session: ex00 best 100 x 5, ex10 best 80 x 10.
        val old = listOf(
            set(0, kg = 100.0, reps = 5, exercise = "ex00", session = "old", status = SetStatus.DONE),
            set(1, kg = 80.0, reps = 10, exercise = "ex10", session = "old", status = SetStatus.DONE),
            // Not done: never a best to beat.
            set(2, kg = 200.0, reps = 10, exercise = "ex20", session = "old", status = SetStatus.SKIPPED),
        )
        val today = listOf(
            set(0, index = 0, kg = 100.0, reps = 6, exercise = "ex00", status = SetStatus.DONE), // beats 100 x 5
            set(1, index = 0, kg = 80.0, reps = 10, exercise = "ex10", status = SetStatus.DONE), // ties: no best
            set(2, index = 0, kg = 20.0, reps = 10, exercise = "ex20", status = SetStatus.DONE), // first counted sets: no best, as in History
            set(2, index = 1, kg = null, reps = null, exercise = "ex20", status = SetStatus.DONE), // checked by old data with no reps: not done
            set(2, index = 2, kg = 50.0, reps = 10, exercise = "ex20"), // pending: not counted
        )
        val sum = summarize(today, old + today, "s", start, start + 52 * 60_000 + 59_000)
        // 52 min 59 s rounds to 53, as History counts it ([sessionTime]).
        assertEquals(Summary("53 min", done = 3, total = 5, volume = 600.0 + 800.0 + 200.0, bests = 1), sum)
        assertEquals("1 h 5 min", summarize(today, today, "s", start, start + 65 * 60_000).time)
    }

    @Test fun `the finish sheet's new best is History's record rule, heaviest weight first, not an estimated 1RM`() {
        val old = set(0, kg = 62.5, reps = 6, exercise = "bench", session = "old", status = SetStatus.DONE)
        fun bests(kg: Double, reps: Int) = summarize(listOf(set(0, kg = kg, reps = reps, exercise = "bench", status = SetStatus.DONE)), listOf(old), "s", 0L, 60_000L).bests
        assertEquals("60 x 12 is lighter: no best (its e1RM, 84, beats 62.5 x 6's 75)", 0, bests(60.0, 12))
        assertEquals("62.5 x 7: as heavy, more reps", 1, bests(62.5, 7))
        assertEquals("62.5 x 6: a tie keeps the old record", 0, bests(62.5, 6))
        // History counts the same session the same way.
        for ((kg, reps) in listOf(60.0 to 12, 62.5 to 7, 62.5 to 6)) {
            val today = set(0, kg = kg, reps = reps, exercise = "bench", status = SetStatus.DONE)
            val sessions = listOf(
                dev.mirzohidkhon.khonfitness.data.Session(id = "old", date = "2026-11-01", startedAt = 1L, finishedAt = 2L, itemType = "program"),
                dev.mirzohidkhon.khonfitness.data.Session(id = "s", date = "2026-11-03", startedAt = 3L, finishedAt = 4L, itemType = "program"),
            )
            assertEquals("$kg x $reps", bests(kg, reps), recordCount("s", liftHistories(listOf(old, today), sessions)))
        }
    }

    @Test fun `the progress row slides up and fades over 28 dp, the line fades in over its last 8`() {
        assertEquals(0f, sessionRowShift(0f), 0f)
        assertEquals(14f, sessionRowShift(14f), 0f)
        assertEquals(28f, sessionRowShift(100f), 0f)
        assertEquals(1f, sessionRowAlpha(0f), 0f)
        assertEquals(0f, sessionRowAlpha(22.4f), 1e-4f)
        assertEquals(0f, sessionLineAlpha(20f), 0f)
        assertEquals(0.5f, sessionLineAlpha(24f), 1e-4f)
        assertEquals(1f, sessionLineAlpha(28f), 0f)
        assertEquals(1f, sessionLineAlpha(Int.MAX_VALUE.toFloat()), 0f)
    }

    @Test fun `at 160 percent font scale the progress row holds its measured line and folds over the taller row`() {
        // Footnote 13/18 sp: an 18 dp line at 1.0, 28.8 dp at 1.6.
        assertEquals(28f, sessionRowHeight(18f), 0f)
        assertEquals(28f, sessionRowHeight(10f), 0f)
        val row = sessionRowHeight(18f * 1.6f)
        assertEquals(38.8f, row, 1e-4f)
        // The text area (the row less the gap under it) holds the whole line.
        assertTrue(row - SESSION_ROW_GAP >= 28.8f - 1e-4f)
        // The fold follows the taller row: the slide ends at 38.8, the fade at 80 percent, the line fades in over the last 8.
        assertEquals(19.4f, sessionRowShift(19.4f, row), 1e-4f)
        assertEquals(row, sessionRowShift(100f, row), 1e-4f)
        assertTrue(sessionRowAlpha(28f, row) > 0f)
        assertEquals(0f, sessionRowAlpha(row * 0.8f, row), 1e-4f)
        assertEquals(0f, sessionLineAlpha(row - 8f, row), 1e-4f)
        assertEquals(0.5f, sessionLineAlpha(row - 4f, row), 1e-4f)
        assertEquals(1f, sessionLineAlpha(row, row), 1e-4f)
        // The list starts under the bar and the taller row; an opened block still lands 8 dp under the folded header.
        assertEquals(56f + row + 10f - 58f - 8f, revealOffset(56f + row, 58f, 10f), 1e-4f)
    }

    @Test fun `an opened block lands 8 dp under what the folded header hides`() {
        // Running: list starts at 84, folded header covers 58; the card is 10 into its item.
        assertEquals(28f, revealOffset(84f, 58f, 10f), 0f)
        // Finished: the bar is above the list, nothing covers it.
        assertEquals(6f, revealOffset(4f, 0f, 10f), 0f)
        assertEquals(0f, revealOffset(4f, 0f, 2f), 0f)
    }
}
