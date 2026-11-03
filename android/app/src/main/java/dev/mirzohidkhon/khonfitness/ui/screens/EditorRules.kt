package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

/** What the program editor's Done writes: a new program, or a changed name. */
data class ProgramSave(val create: Program? = null, val rename: String? = null)

/**
 * A program editor's draft (DESIGN.md, Editors). A new program ([isNew]) stays a draft ([isDraft]) until Done or
 * Add block saves it: leaving before that writes nothing and there is no Delete. Add block saves it exactly as Done
 * would ([programForBlock]), so the block has a home; from then on the screen edits a saved program, with Delete,
 * and leaving without Done keeps it. [Saver] keeps the saved id, the typed [name] and a new program's
 * [activeChoice] over a trip to a block editor, a rotation and process death, so a restored editor edits the same row.
 */
class ProgramDraft(val isNew: Boolean) {
    /** The id the new program is saved under (by Add block or Done), or null while nothing is saved. */
    var savedId: String? by mutableStateOf(null)
        private set
    /** The Name field, null until edited: Done saves only a changed name. */
    var name: String? by mutableStateOf(null)
    /** A new program's Active switch, null until touched. */
    var activeChoice: Boolean? by mutableStateOf(null)

    /** A new program that nothing has saved yet. */
    val isDraft: Boolean get() = isNew && savedId == null

    /** The program the screen edits: the route's, or a new program's once saved; null while it is only a draft. */
    fun programId(routeId: String?): String? = if (isNew) savedId else routeId

    /** Delete shows on a saved program only, and never on the last one: Today always plans from a program. */
    fun showDelete(programCount: Int): Boolean = !isDraft && programCount > 1

    /** The Active switch's state: a draft's choice, by default on only when it is the only program. */
    fun shownActive(savedActive: Boolean, onlyProgram: Boolean): Boolean =
        if (isDraft) activeChoice ?: onlyProgram else savedActive

    /** Fixed on for the active program (turning another one on moves the mark) and for the only program. */
    fun activeLocked(savedActive: Boolean, onlyProgram: Boolean): Boolean = onlyProgram || (!isDraft && savedActive)

    /** Add block: the program to save first, as Done would save it; null once it is saved (or not new). */
    fun programForBlock(id: String, onlyProgram: Boolean): Program? = if (isDraft) save(id, onlyProgram) else null

    /** Done: a draft's program to create, else a saved program's changed name. [saved] is the stored row. */
    fun done(newId: String, saved: Program?, onlyProgram: Boolean): ProgramSave =
        if (isDraft) ProgramSave(create = save(newId, onlyProgram))
        else ProgramSave(rename = name?.trim()?.takeIf { it.isNotEmpty() && it != saved?.name })

    /** The new program with the typed name (or [NEW_PROGRAM_NAME]) and the Active choice; the editor edits it from here on. */
    private fun save(id: String, onlyProgram: Boolean): Program {
        savedId = id
        return Program(id, name?.trim()?.takeIf { it.isNotEmpty() } ?: NEW_PROGRAM_NAME, active = activeChoice ?: onlyProgram)
    }

    companion object {
        val Saver = listSaver<ProgramDraft, Any?>(
            save = { listOf(it.isNew, it.savedId, it.name, it.activeChoice) },
            restore = { ProgramDraft(it[0] as Boolean).apply { savedId = it[1] as String?; name = it[2] as String?; activeChoice = it[3] as Boolean? } },
        )
    }
}

/**
 * A block editor's draft: a new block is not saved until its first exercise is picked, which saves the two
 * together ([firstPick]); backing out before that writes nothing. Afterwards the screen edits the saved block.
 */
class BlockDraft(val isNew: Boolean) {
    var savedId: String? by mutableStateOf(null)
        private set

    /** The block the screen edits: the route's for an existing block, the saved one for a new block, else none yet. */
    fun blockId(routeId: String?): String? = if (isNew) savedId else routeId

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

/** What the backup footnote says after an export, import or restore; red for an error. */
data class BackupStatus(val text: String, val error: Boolean = false)

/** A file operation in Settings' Backup group: export, open a file to import, merge or replace from it, restore a saved copy. */
enum class BackupAction { EXPORT, OPEN, MERGE, REPLACE, RESTORE, LIST }

/**
 * The backup footnote after [action] ended with [result]: every failure is red and names what failed. A file that
 * reads but does not parse is "Not a Khon backup". A successful open says nothing: the import sheet opens instead.
 */
fun backupFootnote(action: BackupAction, result: Result<*>): BackupStatus? {
    val e = result.exceptionOrNull()
    fun failed(what: String) = BackupStatus("$what: ${e?.message ?: e?.javaClass?.simpleName}", error = true)
    return when (action) {
        BackupAction.EXPORT -> if (e == null) BackupStatus("Backup exported.") else failed("Export failed")
        BackupAction.OPEN -> when (e) {
            null -> null
            is IllegalArgumentException -> failed("Not a Khon backup") // the JSON parser's errors
            else -> failed("Could not read the file")
        }
        BackupAction.MERGE -> if (e == null) BackupStatus("Backup merged.") else failed("Import failed")
        BackupAction.REPLACE -> if (e == null) BackupStatus("Backup restored: everything was replaced.") else failed("Import failed")
        BackupAction.RESTORE -> if (e == null) BackupStatus("Backup restored: everything was replaced.") else failed("Restore failed")
        BackupAction.LIST -> if (e == null) null else failed("Could not list the saved copies")
    }
}

/**
 * Whether [action] ending with [result] changed the stored data (a merge, replace or restore that worked), so the
 * caller re-arms the window alarms and refreshes the widgets ([dev.mirzohidkhon.khonfitness.window.WindowScheduler.reschedule]).
 */
fun backupChangesData(action: BackupAction, result: Result<*>): Boolean =
    result.isSuccess && action in setOf(BackupAction.MERGE, BackupAction.REPLACE, BackupAction.RESTORE)

/**
 * The saved copies in [dir], newest first. A folder that does not exist yet holds none; a folder Android cannot list
 * is a failure, so the footnote says so instead of hiding Restore.
 */
fun savedBackups(dir: java.io.File): Result<List<java.io.File>> = runCatching {
    if (!dir.exists()) return@runCatching emptyList()
    val files = dir.listFiles() ?: throw java.io.IOException("Android could not list the saved copies")
    files.filter { it.name.endsWith(".json") }.sortedByDescending { it.name }
}

/** The stream Android opened for a backup file; none is an error, so the footnote never reports a write that did not happen. */
fun <T> T?.orNoFile(): T = this ?: throw java.io.IOException("Android could not open the file")
