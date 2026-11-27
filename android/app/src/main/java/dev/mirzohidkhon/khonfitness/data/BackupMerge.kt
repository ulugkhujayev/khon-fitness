package dev.mirzohidkhon.khonfitness.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder

/** Decodes each heart rate row of a file by itself: a row that does not decode is dropped, never the file. */
object LenientSessionHrList : KSerializer<List<SessionHr>> {
    private val list = ListSerializer(SessionHr.serializer())
    override val descriptor = list.descriptor
    override fun serialize(encoder: Encoder, value: List<SessionHr>) = list.serialize(encoder, value)
    override fun deserialize(decoder: Decoder): List<SessionHr> {
        val json = decoder as? JsonDecoder ?: return list.deserialize(decoder)
        val array = json.decodeJsonElement() as? JsonArray ?: return emptyList()
        return array.mapNotNull { runCatching { json.json.decodeFromJsonElement(SessionHr.serializer(), it) }.getOrNull() }
    }
}

/** Whether a file row moves the session's workout: then its stored range and round times no longer fit. */
fun boundsChanged(local: Session, file: Session): Boolean =
    local.startedAt != file.startedAt || local.finishedAt != file.finishedAt || local.itemType != file.itemType || local.exerciseId != file.exerciseId

/**
 * A file session as an import writes it. New to this database ([local] null): the file's row without its anchor, which
 * is valid only on the boot that took it. Existing: the file's row with the local range, anchor, run numbers, and linked
 * ids; when [boundsChanged], without range and anchor.
 */
fun importedSession(local: Session?, file: Session): Session {
    if (local == null) return file.copy(startedElapsed = null)
    val moved = boundsChanged(local, file)
    return file.copy(
        timedFrom = local.timedFrom.takeUnless { moved }, timedTo = local.timedTo.takeUnless { moved },
        startedElapsed = local.startedElapsed.takeUnless { moved }, timerRun = local.timerRun, timerDone = local.timerDone,
        linkedIds = local.linkedIds,
    )
}

/** A file set as a Merge writes it: an existing set keeps its local stamp; a set that is not done has none. */
fun mergedSet(local: SetLog?, file: SetLog): SetLog {
    val stamp = if (local != null) local.checkedAt else file.checkedAt
    return file.copy(checkedAt = stamp.takeIf { file.status == SetStatus.DONE })
}

/** A file round as a Merge writes it: an existing round keeps its local times. */
fun mergedRound(local: IntervalLog?, file: IntervalLog): IntervalLog =
    if (local == null) file else file.copy(startedAt = local.startedAt, endedAt = local.endedAt)

/** The file's heart rate rows an import takes: valid ([validFileHr]) rows of [sessionIds]; of two with one id the last. */
fun importedHr(rows: List<SessionHr>, sessionIds: Set<String>): List<SessionHr> =
    rows.filter { it.sessionId in sessionIds && validFileHr(it) }.associateBy { it.sessionId }.values.toList()
