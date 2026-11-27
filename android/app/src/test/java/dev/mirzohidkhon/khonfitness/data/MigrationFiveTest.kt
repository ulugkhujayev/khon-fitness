package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Databases of versions 1 to 4, made from the exported schemas, open at version 5 through [KhonDatabase.build]. Every
 * old column reads back equal; only a finished imported cardio session gets a range.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MigrationFiveTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private var db: KhonDatabase? = null

    @After fun close() { db?.close() }

    /** An empty database of [version] from its exported schema, as Room left it. */
    private fun createAt(version: Int): SQLiteDatabase {
        val schema = Json.parseToJsonElement(File("schemas/dev.mirzohidkhon.khonfitness.data.KhonDatabase/$version.json").readText()).jsonObject["database"]!!.jsonObject
        val file = context.getDatabasePath("khon.db").also { it.parentFile!!.mkdirs(); it.delete() }
        val sql = SQLiteDatabase.openOrCreateDatabase(file, null)
        for (e in schema["entities"]!!.jsonArray) {
            val entity = e.jsonObject
            val table = entity["tableName"]!!.jsonPrimitive.content
            sql.execSQL(entity["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            entity["indices"]?.jsonArray?.forEach { sql.execSQL(it.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table)) }
        }
        schema["setupQueries"]!!.jsonArray.forEach { sql.execSQL(it.jsonPrimitive.content) }
        sql.version = version
        return sql
    }

    private fun SQLiteDatabase.insertRow(table: String, vararg values: Pair<String, Any?>) {
        val cv = ContentValues()
        for ((name, v) in values) {
            val k = "`$name`"
            when (v) {
                null -> cv.putNull(k)
                is String -> cv.put(k, v)
                is Int -> cv.put(k, v)
                is Long -> cv.put(k, v)
                is Double -> cv.put(k, v)
                is Boolean -> cv.put(k, if (v) 1 else 0)
                else -> error(v)
            }
        }
        assertTrue(insertOrThrow(table, null, cv) >= 0)
    }

    private val start = at(18, 0)

    private fun session(id: String, minutes: Long?, itemType: String = ItemType.PROGRAM, sourceId: String? = null) = Session(
        id = id, date = "2026-11-20", startedAt = start, finishedAt = minutes?.let { start + it * MIN }, itemType = itemType,
        programId = if (itemType == ItemType.PROGRAM) "p1" else null, programName = if (itemType == ItemType.PROGRAM) "Push" else null,
        exerciseId = if (itemType == ItemType.CARDIO) "row" else null, exerciseName = if (itemType == ItemType.CARDIO) "Rowing" else null,
        note = "n-$id", effort = 7, timeSec = 1_200, distanceM = 4_000, avgHr = 141, watts = 180, laps = 3, poolLength = 25, stroke = "free",
        sourceId = sourceId,
    )

    private val sessions = listOf(
        session("g9", 9), session("g10", 10), session("g240", 240), session("g241", 241), session("open", null),
        session("band", 30, ItemType.CARDIO, sourceId = "hc-1,hc-2"), session("app", 30, ItemType.CARDIO),
    )
    private val setLog = SetLog("l1", "g10", 0, true, 1, 2, 3, "bench", "Bench Press", 6, 10, 2.5, 60.0, 8, SetStatus.DONE)
    private val interval = IntervalLog("i1", "app", 0, avgHr = 150, watts = 200)
    private val override = DayOverride("2026-11-21", ItemType.CARDIO, "row", auto = true)

    private fun SQLiteDatabase.fill(version: Int) {
        for (s in sessions) {
            if (version == 1 && s.sourceId != null) continue
            val cols = mutableListOf<Pair<String, Any?>>(
                "id" to s.id, "date" to s.date, "startedAt" to s.startedAt, "finishedAt" to s.finishedAt, "itemType" to s.itemType,
                "programId" to s.programId, "programName" to s.programName, "exerciseId" to s.exerciseId, "exerciseName" to s.exerciseName,
                "note" to s.note, "effort" to s.effort, "timeSec" to s.timeSec, "distanceM" to s.distanceM, "avgHr" to s.avgHr,
                "watts" to s.watts, "laps" to s.laps, "poolLength" to s.poolLength, "stroke" to s.stroke,
            )
            if (version >= 2) cols += "sourceId" to s.sourceId
            insertRow("sessions", *cols.toTypedArray())
        }
        with(setLog) {
            insertRow("set_logs", "id" to id, "sessionId" to sessionId, "blockIndex" to blockIndex, "blockSuperset" to blockSuperset, "slot" to slot,
                "setIndex" to setIndex, "order" to order, "exerciseId" to exerciseId, "exerciseName" to exerciseName, "minReps" to minReps,
                "maxReps" to maxReps, "stepKg" to stepKg, "weightKg" to weightKg, "reps" to reps, "status" to status)
        }
        insertRow("interval_logs", "id" to interval.id, "sessionId" to interval.sessionId, "index" to interval.index, "avgHr" to interval.avgHr, "watts" to interval.watts)
        val o = mutableListOf<Pair<String, Any?>>("date" to override.date, "itemType" to override.itemType, "itemId" to override.itemId)
        if (version >= 4) o += "auto" to override.auto
        insertRow("day_overrides", *o.toTypedArray())
    }

    private fun migrateFrom(version: Int): KhonDao {
        createAt(version).apply { fill(version); close() }
        return KhonDatabase.build(context).also { db = it }.dao()
    }

    private fun expected(version: Int): List<Session> = sessions.filter { version > 1 || it.sourceId == null }.map {
        if (it.sourceId != null) it.copy(timedFrom = it.startedAt, timedTo = it.finishedAt) else it
    }

    @Test fun `a version 4 database opens at 5 with every row and an empty heart rate table`() = runBlocking {
        val dao = migrateFrom(4)
        assertEquals(5, db!!.openHelper.readableDatabase.version)
        assertEquals(expected(4).sortedBy { it.id }, dao.sessionsOnce().sortedBy { it.id })
        assertEquals(listOf(setLog), dao.allSetLogsOnce())
        assertEquals(listOf(interval), dao.intervalLogsOnce())
        assertEquals(listOf(override), dao.overridesOnce())
        assertEquals(emptyList<SessionHr>(), dao.sessionHrOnce())
    }

    @Test fun `only the finished imported cardio session gets the band's bounds as its range`() = runBlocking {
        val byId = migrateFrom(4).sessionsOnce().associateBy { it.id }
        val band = byId.getValue("band")
        assertEquals(band.startedAt, band.timedFrom)
        assertEquals(band.finishedAt, band.timedTo)
        for (id in listOf("g9", "g10", "g240", "g241", "open", "app")) {
            val s = byId.getValue(id)
            assertNull(id, s.timedFrom)
            assertNull(id, s.timedTo)
        }
        for (s in byId.values) {
            assertNull(s.startedElapsed); assertNull(s.timerRun); assertNull(s.timerDone); assertNull(s.linkedIds)
        }
        val log = migrateFromCurrent().allSetLogsOnce().single()
        assertNull(log.checkedAt)
        val round = migrateFromCurrent().intervalLogsOnce().single()
        assertNull(round.startedAt); assertNull(round.endedAt)
    }

    private fun migrateFromCurrent(): KhonDao = db!!.dao()

    @Test fun `databases of versions 1, 2 and 3 reach 5 with their rows`() = runBlocking {
        for (version in 1..3) {
            db?.close()
            val dao = migrateFrom(version)
            assertEquals(5, db!!.openHelper.readableDatabase.version)
            assertEquals("v$version", expected(version).sortedBy { it.id }, dao.sessionsOnce().sortedBy { it.id })
            assertEquals("v$version", listOf(setLog), dao.allSetLogsOnce())
            assertEquals("v$version", listOf(interval), dao.intervalLogsOnce())
            assertEquals("v$version", listOf(override.copy(auto = false)), dao.overridesOnce())
            assertEquals("v$version", emptyList<SessionHr>(), dao.sessionHrOnce())
        }
    }
}
