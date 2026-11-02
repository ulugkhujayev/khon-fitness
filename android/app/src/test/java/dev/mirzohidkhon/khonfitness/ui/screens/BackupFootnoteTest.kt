package dev.mirzohidkhon.khonfitness.ui.screens

import java.io.FileNotFoundException
import java.io.IOException
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupFootnoteTest {
    private fun fail(e: Throwable) = Result.failure<Unit>(e)
    private val ok = Result.success(Unit)

    @Test fun `each action's success says what happened`() {
        assertEquals(BackupStatus("Backup exported."), backupFootnote(BackupAction.EXPORT, ok))
        assertEquals(BackupStatus("Backup merged."), backupFootnote(BackupAction.MERGE, ok))
        assertEquals(BackupStatus("Backup restored: everything was replaced."), backupFootnote(BackupAction.REPLACE, ok))
        assertEquals(BackupStatus("Backup restored: everything was replaced."), backupFootnote(BackupAction.RESTORE, ok))
        assertNull(backupFootnote(BackupAction.OPEN, ok)) // the import sheet opens instead
    }

    @Test fun `every failure is red and names what failed`() {
        assertEquals(BackupStatus("Export failed: disk full", error = true), backupFootnote(BackupAction.EXPORT, fail(IOException("disk full"))))
        assertEquals(BackupStatus("Import failed: busy", error = true), backupFootnote(BackupAction.MERGE, fail(IllegalStateException("busy"))))
        assertEquals(BackupStatus("Import failed: busy", error = true), backupFootnote(BackupAction.REPLACE, fail(IllegalStateException("busy"))))
        assertEquals(BackupStatus("Restore failed: gone", error = true), backupFootnote(BackupAction.RESTORE, fail(FileNotFoundException("gone"))))
    }

    @Test fun `opening tells a file that does not parse from a file that does not read`() {
        assertEquals(BackupStatus("Not a Khon backup: bad json", error = true), backupFootnote(BackupAction.OPEN, fail(SerializationException("bad json"))))
        assertEquals(BackupStatus("Could not read the file: denied", error = true), backupFootnote(BackupAction.OPEN, fail(SecurityException("denied"))))
    }

    @Test fun `a failure without a message names its type`() {
        assertEquals(BackupStatus("Export failed: IOException", error = true), backupFootnote(BackupAction.EXPORT, fail(IOException())))
    }

    @Test fun `no stream from Android is an error, not a written backup`() {
        val export = runCatching { null.orNoFile() }
        assertEquals(BackupStatus("Export failed: Android could not open the file", error = true), backupFootnote(BackupAction.EXPORT, export))
        assertEquals(BackupStatus("Could not read the file: Android could not open the file", error = true), backupFootnote(BackupAction.OPEN, runCatching { null.orNoFile() }))
        assertEquals("x", "x".orNoFile())
    }
}
