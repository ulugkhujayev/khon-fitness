package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.data.epley

/** What the finish sheet shows, as of opening it: how long the workout took ("52 min"), sets done of total, volume, new bests. */
data class Summary(val time: String, val done: Int, val total: Int, val volume: Double, val bests: Int)

private fun e1rm(l: SetLog) = epley(l.weightKg ?: 0.0, l.reps ?: 0)

/** The finish figures. A new best is an exercise whose best e1RM today beats every earlier done set of it. */
fun summarize(logs: List<SetLog>, all: List<SetLog>, sessionId: String, startedAt: Long, now: Long): Summary {
    val done = logs.filter { it.status == SetStatus.DONE }
    val volume = done.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
    val before = all.filter { it.sessionId != sessionId && it.status == SetStatus.DONE }.groupBy { it.exerciseId }.mapValues { e -> e.value.maxOf(::e1rm) }
    val bests = done.groupBy { it.exerciseId }.count { (ex, ls) -> ls.maxOf(::e1rm) > (before[ex] ?: 0.0) }
    return Summary(workoutTime(startedAt, now), done.size, logs.size, volume, bests)
}

/** Set logs grouped by block, in block order, each block in execution order. */
fun groupBlocks(logs: List<SetLog>): List<List<SetLog>> =
    logs.sortedBy { it.order }.groupBy { it.blockIndex }.toSortedMap().values.toList()

/** The block open at first: the first one with a pending set, else the first. */
fun firstOpenBlock(blocks: List<List<SetLog>>): Int =
    blocks.indexOfFirst { b -> b.any { it.status == SetStatus.PENDING } }.coerceAtLeast(0)

/** Where a session goes after a check: the block to open (null: none) and the set whose kg field gets focus. */
data class AfterCheck(val open: Int?, val focus: SetLog?)

/**
 * After [checked] is logged: its block stays open while it has a pending set, and focus goes to the next one (later in
 * the order, else one left behind). A finished block hands over to the next block with a pending set, wrapping to an
 * earlier one, focus on its first pending set; with nothing left every block closes. [blocks] come from [groupBlocks]
 * before the check lands, so [checked] may still be pending in them. Only a [live] (running) session moves on; a saved
 * session edited in History gets null: the check changes the set and nothing else.
 */
fun afterCheck(blocks: List<List<SetLog>>, checked: SetLog, live: Boolean): AfterCheck? {
    if (!live) return null
    fun pending(block: List<SetLog>) = block.filter { it.id != checked.id && it.status == SetStatus.PENDING }
    val here = blocks.indexOfFirst { b -> b.any { it.id == checked.id } }
    val left = if (here >= 0) pending(blocks[here]) else emptyList()
    if (left.isNotEmpty()) return AfterCheck(here, left.firstOrNull { it.order > checked.order } ?: left.first())
    val next = ((here + 1 until blocks.size) + (0 until here.coerceAtLeast(0))).firstOrNull { pending(blocks[it]).isNotEmpty() }
    return if (next == null) AfterCheck(null, null) else AfterCheck(next, pending(blocks[next]).first())
}

/** Exercise names of a block, A first: "Bench Press · Leg Press". */
fun blockTitle(logs: List<SetLog>): String = logs.distinctBy { it.slot }.sortedBy { it.slot }.joinToString(" · ") { it.exerciseName }

/** Every set of the block is logged or skipped. */
fun isBlockDone(logs: List<SetLog>): Boolean = logs.all { it.status != SetStatus.PENDING }

/** Sets logged as done. */
fun doneCount(logs: List<SetLog>): Int = logs.count { it.status == SetStatus.DONE }

/** A block header's spoken name: "Block 1, Bench Press, 2 of 6 sets done, block done". */
fun blockLabel(index: Int, title: String, done: Int, total: Int, blockDone: Boolean): String =
    "Block ${index + 1}, $title, $done of $total ${if (total == 1) "set" else "sets"} done" + if (blockDone) ", block done" else ""

/** The block the user or a check picked: [picked] stays false until then; [chosen] null means none open. */
data class OpenPick(val picked: Boolean = false, val chosen: Int? = null)

/**
 * The [OpenPick] of each session, kept for the life of the process ([dev.mirzohidkhon.khonfitness.ui.AppViewModel]
 * holds it): Back and Resume make a new screen, and the session keeps its open block. A session that finishes or is
 * deleted drops its entry ([drop]); a session with none starts from its default.
 */
class OpenPicks {
    private val picks = androidx.compose.runtime.mutableStateMapOf<String, OpenPick>()
    operator fun get(sessionId: String): OpenPick = picks[sessionId] ?: OpenPick()
    operator fun set(sessionId: String, pick: OpenPick) { picks[sessionId] = pick }
    fun drop(sessionId: String) { picks.remove(sessionId) }

    /** One session's entry as a state the screen reads and writes. */
    fun state(sessionId: String): androidx.compose.runtime.MutableState<OpenPick> = object : androidx.compose.runtime.MutableState<OpenPick> {
        override var value: OpenPick
            get() = this@OpenPicks[sessionId]
            set(pick) { this@OpenPicks[sessionId] = pick }
        override fun component1(): OpenPick = value
        override fun component2(): (OpenPick) -> Unit = { value = it }
    }
}

/** The block the session shows open: the one the user or a check picked ([picked]), else [firstOpenBlock]. */
fun openBlock(blocks: List<List<SetLog>>, picked: Boolean, chosen: Int?): Int? =
    if (picked) chosen else if (blocks.isEmpty()) null else firstOpenBlock(blocks)

// The running session's header, in dp: the 56 dp bar, the 28 dp progress row under it, a 2 dp line once folded.
const val SESSION_NAV = 56f
const val SESSION_ROW = 28f
const val SESSION_LINE = 2f
/** Room under the progress row's footnote line, in dp. */
const val SESSION_ROW_GAP = 10f

/**
 * The progress row's height (dp) for a measured footnote [line] height (dp): the line and [SESSION_ROW_GAP] under it,
 * at least [SESSION_ROW]. The row folds over this same distance. An 18 dp line gives 28; at font scale 1.6, 28.8 gives 38.8.
 */
fun sessionRowHeight(line: Float): Float = maxOf(SESSION_ROW, line + SESSION_ROW_GAP)

/** How far the progress row has slid up under the bar after [y] dp of scroll: with the sets, over the first [row] dp. */
fun sessionRowShift(y: Float, row: Float = SESSION_ROW): Float = dev.mirzohidkhon.khonfitness.ui.components.fold(y, row, 0f, row)

/** The progress row fades out over the first 80 percent of its slide. */
fun sessionRowAlpha(y: Float, row: Float = SESSION_ROW): Float = dev.mirzohidkhon.khonfitness.ui.components.fold(y, row * 0.8f, 1f, 0f)

/** The 2 dp progress line on the bar's edge fades in over the last 8 dp of the slide. */
fun sessionLineAlpha(y: Float, row: Float = SESSION_ROW): Float = dev.mirzohidkhon.khonfitness.ui.components.fold(y - (row - 8f), 8f, 0f, 1f)

/**
 * The lazy-list item offset (dp) that scrolls an opened block's card to [above] dp under the [cover] the header still
 * hides. An item at offset 0 starts at the list's [topPadding]; its card starts [cardTop] dp lower.
 */
fun revealOffset(topPadding: Float, cover: Float, cardTop: Float, above: Float = 8f): Float = (topPadding + cardTop - cover - above).coerceAtLeast(0f)
