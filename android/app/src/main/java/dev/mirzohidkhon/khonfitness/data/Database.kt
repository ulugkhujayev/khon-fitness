package dev.mirzohidkhon.khonfitness.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Exercise::class, Modality::class, Program::class, Block::class, BlockExercise::class,
        Session::class, SetLog::class, IntervalLog::class, Bodyweight::class, WeekPlan::class, DayOverride::class,
        Stretch::class, StretchRoutine::class, RoutineStretch::class, StretchSession::class, EatingWindow::class, WindowDay::class,
        SessionHr::class],
    version = 5,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class KhonDatabase : RoomDatabase() {
    abstract fun dao(): KhonDao

    companion object {
        /** Day overrides get the auto flag; existing rows are user edits. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `day_overrides` ADD COLUMN `auto` INTEGER NOT NULL DEFAULT 0") }
        }

        /**
         * Clock times and the heart rate table. Only a finished imported cardio session gets a range: the band's own
         * bounds. No older app session gets one: its clocks were never verified.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `set_logs` ADD COLUMN `checkedAt` INTEGER")
                db.execSQL("ALTER TABLE `interval_logs` ADD COLUMN `startedAt` INTEGER")
                db.execSQL("ALTER TABLE `interval_logs` ADD COLUMN `endedAt` INTEGER")
                for (column in listOf("`timedFrom` INTEGER", "`timedTo` INTEGER", "`startedElapsed` INTEGER", "`timerRun` INTEGER", "`timerDone` INTEGER", "`linkedIds` TEXT")) {
                    db.execSQL("ALTER TABLE `sessions` ADD COLUMN $column")
                }
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `session_hr` (`sessionId` TEXT NOT NULL, `fromMs` INTEGER NOT NULL, `toMs` INTEGER NOT NULL, " +
                        "`state` TEXT NOT NULL, `samples` TEXT NOT NULL, `rawCount` INTEGER NOT NULL, `sampleCount` INTEGER NOT NULL, " +
                        "`origin` TEXT, `origins` TEXT NOT NULL, `firstMs` INTEGER, `lastMs` INTEGER, `maxGapSec` INTEGER, `avgHr` INTEGER, " +
                        "`maxHr` INTEGER, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`sessionId`))"
                )
                db.execSQL(
                    "UPDATE `sessions` SET `timedFrom` = `startedAt`, `timedTo` = `finishedAt` " +
                        "WHERE `itemType` = '${ItemType.CARDIO}' AND `sourceId` IS NOT NULL AND `finishedAt` IS NOT NULL"
                )
            }
        }

        fun build(context: Context): KhonDatabase =
            Room.databaseBuilder(context, KhonDatabase::class.java, "khon.db").addMigrations(MIGRATION_3_4, MIGRATION_4_5).build()
    }
}
