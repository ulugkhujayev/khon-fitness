package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.mirzohidkhon.khonfitness.data.Block
import dev.mirzohidkhon.khonfitness.data.BlockExercise
import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.data.KhonRepository
import dev.mirzohidkhon.khonfitness.data.PlanItem
import dev.mirzohidkhon.khonfitness.data.Program
import dev.mirzohidkhon.khonfitness.update.Updater

/** The name a new program gets when its Name field is left empty. */
const val NEW_PROGRAM_NAME = "New program"

/** What the program editor's Done writes: a new program, or a changed name and a waiting Active choice. */
data class ProgramSave(val create: Program? = null, val rename: String? = null, val activate: Boolean = false)

/**
 * A program editor's draft (DESIGN.md, Editors). A new program ([isNew]) is not saved until Done, so leaving
 * without Done writes nothing and there is no Delete. Add block saves it first so the block has a home
 * ([programForBlock]); saved that way it stays provisional: left without Done it is dropped again unless it
 * holds a block by then ([dropOnLeave]; the database decides that), and its Active choice waits for Done.
 * The typed [name] and a new program's [activeChoice] live here, so the trip to a block editor keeps them.
 */
class ProgramDraft(val isNew: Boolean) {
    /** The id the new program is saved under (by Add block or Done), or null while nothing is saved. */
    var savedId: String? by mutableStateOf(null)
        private set
    /** The Name field, null until edited: Done saves only a changed name. */
    var name: String? by mutableStateOf(null)
    /** A new program's Active switch, null until touched. */
    var activeChoice: Boolean? by mutableStateOf(null)
    private var kept = false

    /** The program the screen edits: the route's, or a new program's once saved; null while it is only a draft. */
    fun programId(routeId: String): String? = if (isNew) savedId else routeId

    /** Delete shows on a saved program only, and never on the last one: Today always plans from a program. */
    fun showDelete(programCount: Int): Boolean = !isNew && programCount > 1

    /** The Active switch's state: a new program's choice, by default on only when it is the only program. */
    fun shownActive(savedActive: Boolean, onlyProgram: Boolean): Boolean =
        if (isNew) activeChoice ?: onlyProgram else savedActive

    /** Fixed on for the active program (turning another one on moves the mark) and for the only program. */
    fun activeLocked(savedActive: Boolean, onlyProgram: Boolean): Boolean = onlyProgram || (!isNew && savedActive)

    /**
     * Add block: the program to save first, or null when it is saved already (or not new). It is saved active only
     * when it is the only program; a chosen Active waits for Done.
     */
    fun programForBlock(id: String, onlyProgram: Boolean): Program? {
        if (!isNew || savedId != null) return null
        savedId = id
        return Program(id, nameOf(name), active = onlyProgram)
    }

    /**
     * Done: what to write. [saved] is the stored row (null while nothing is saved or it has not loaded). The program
     * is kept from here on, and a second Done never creates it twice.
     */
    fun done(newId: String, saved: Program?, onlyProgram: Boolean): ProgramSave {
        kept = true
        val active = shownActive(saved?.active ?: false, onlyProgram)
        if (isNew && savedId == null) { savedId = newId; return ProgramSave(create = Program(newId, nameOf(name), active = active)) }
        val rename = name?.trim()?.takeIf { it.isNotEmpty() && it != saved?.name }
        return ProgramSave(rename = rename, activate = isNew && active && saved?.active == false)
    }

    /** The program to drop when the screen goes away: one Add block saved, left without Done. */
    fun dropOnLeave(): String? = if (isNew && !kept) savedId else null

    private fun nameOf(typed: String?) = typed?.trim()?.takeIf { it.isNotEmpty() } ?: NEW_PROGRAM_NAME
}

/**
 * Holds a [ProgramDraft] for as long as the program editor is on the back stack: a rotation and a trip to a block
 * editor keep it, and when the editor leaves the stack by any path (Back, Done, the system back, a jump to another
 * tab) [drop] gets the program to drop.
 */
class ProgramDraftModel : ViewModel() {
    private var draft: ProgramDraft? = null
    /** Drops a provisional program; set by the screen, so it runs on the app's scope, not this model's. */
    var drop: ((String) -> Unit)? = null

    fun draft(isNew: Boolean): ProgramDraft = draft ?: ProgramDraft(isNew).also { draft = it }

    public override fun onCleared() { draft?.dropOnLeave()?.let { id -> drop?.invoke(id) } }
}

/**
 * A block editor's draft: a new block is not saved until its first exercise is picked, which saves the two
 * together ([firstPick]); backing out before that writes nothing. Afterwards the screen edits the saved block.
 */
class BlockDraft(val isNew: Boolean) {
    var savedId: String? by mutableStateOf(null)
        private set

    /** The block the screen edits: the route's for an existing block, the saved one for a new block, else none yet. */
    fun blockId(routeId: String): String? = if (isNew) savedId else routeId

    /** The first pick on a new block: the block and its first exercise to save together, or null once saved. */
    fun firstPick(blockId: String, programId: String, superset: Boolean, exerciseRowId: String, slot: Int, exerciseId: String): Pair<Block, BlockExercise>? {
        if (!isNew || savedId != null) return null
        savedId = blockId
        return Block(blockId, programId, sortOrder = 0, superset = superset) to BlockExercise(exerciseRowId, blockId, slot, exerciseId)
    }

    companion object {
        /** Keeps the saved id over a rotation, so a saved block is never saved a second time. */
        val Saver = listSaver<BlockDraft, Any?>(save = { listOf(it.isNew, it.savedId) }, restore = { BlockDraft(it[0] as Boolean).apply { savedId = it[1] as String? } })
    }
}

/** The free exercise slot of a block (0 = A, 1 = B), A first; null when both are used. */
fun freeSlot(used: Collection<Int>): Int? = listOf(0, 1).firstOrNull { it !in used }

/** "Exercise A" or "Exercise B": a block exercise's section title. */
fun slotTitle(slot: Int): String = "Exercise ${'A' + slot}"

/** What the superset switch does, under its group. */
fun supersetNote(superset: Boolean): String =
    if (superset) "A and B alternate, set by set: A1, B1, A2, B2…" else "All sets of A, then all sets of B."

/**
 * An exercise's kind cannot change once a session, a program or the plan uses it ([usage] such uses; null while
 * the count loads, which locks it too). A new exercise is never locked.
 */
fun kindLocked(isNew: Boolean, usage: Int?): Boolean = !isNew && (usage == null || usage > 0)

/** Delete is on only when nothing uses the exercise (counted, not loading). */
fun canDeleteExercise(usage: Int?): Boolean = usage == 0

/** What a modality groups: "3 exercises", "1 exercise", "No exercises" (archived exercises not counted). */
fun modalityUse(modalityId: String, exercises: List<Exercise>): String {
    val n = exercises.count { it.kind == Kind.CARDIO && !it.archived && it.modalityId == modalityId }
    return if (n == 0) "No exercises" else "$n ${if (n == 1) "exercise" else "exercises"}"
}

/** What the weekly template holds for [weekday] (1 = Monday), ignoring dated changes; a missing item is rest. */
fun KhonRepository.PlanData.templateItem(weekday: Int): PlanItem {
    val w = plan.find { it.weekday == weekday }
    return when (w?.itemType) {
        ItemType.PROGRAM -> programs.find { it.id == w.itemId }?.let { PlanItem(ItemType.PROGRAM, program = it) }
        ItemType.CARDIO -> exercises.find { it.id == w.itemId }?.let { PlanItem(ItemType.CARDIO, cardio = it) }
        else -> null
    } ?: PlanItem(ItemType.REST)
}

/** The weekly template at a glance: "5 training days · 2 rest days", "1 training day · 6 rest days". */
fun KhonRepository.PlanData.weekSummary(): String {
    val training = (1..7).count { !templateItem(it).isRest }
    fun count(n: Int, what: String) = "$n $what ${if (n == 1) "day" else "days"}"
    return "${count(training, "training")} · ${count(7 - training, "rest")}"
}

/** "1 set", "12 sets". */
fun setsText(n: Int): String = "$n ${if (n == 1) "set" else "sets"}"

/** "1 block", "4 blocks". */
fun blocksText(n: Int): String = "$n ${if (n == 1) "block" else "blocks"}"

/** Settings' update row and the footnote under its group, from the APK updater's state. */
data class UpdateStatus(val title: String, val value: String?, val note: String?, val error: Boolean = false)

/**
 * The update row: "Check for update", "Checking…" while no answer is in ([result] null), the latest version or
 * the error under the group; with a newer release "Update to v0.5.0", and "Downloading 42%" while it downloads.
 * [downloadError] is a failed download, shown until the next check.
 */
fun updateStatus(result: Updater.Result?, progress: Float?, downloadError: String? = null): UpdateStatus = when (result) {
    null -> UpdateStatus("Check for update", "Checking…", null)
    is Updater.Result.UpToDate -> UpdateStatus("Check for update", null, "v${result.current} is the latest version.")
    is Updater.Result.Failed -> UpdateStatus("Check for update", null, result.message, error = true)
    is Updater.Result.Available -> UpdateStatus(
        "Update to ${result.release.tag_name}",
        progress?.let { "Downloading ${(it * 100).toInt()}%" },
        downloadError ?: "Downloads the new APK and installs it over this one; your data stays.",
        error = downloadError != null,
    )
}
