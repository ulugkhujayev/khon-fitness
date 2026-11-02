package dev.mirzohidkhon.khonfitness.ui.screens

import org.junit.Assert.assertTrue
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

    @Test fun `W5 a merge, replace or restore that worked re-arms the window alarms and widgets, nothing else does`() {
        for (a in listOf(BackupAction.MERGE, BackupAction.REPLACE, BackupAction.RESTORE)) {
            assertTrue(a.name, backupChangesData(a, ok))
            assertTrue(a.name, !backupChangesData(a, fail(IOException("busy"))))
        }
        for (a in listOf(BackupAction.EXPORT, BackupAction.OPEN, BackupAction.LIST)) assertTrue(a.name, !backupChangesData(a, ok))
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

    @Test fun `saved copies list newest first and a missing folder holds none`() {
        val root = java.nio.file.Files.createTempDirectory("ui05-backups").toFile()
        try {
            assertEquals(emptyList<java.io.File>(), savedBackups(java.io.File(root, "backups")).getOrThrow())
            val dir = java.io.File(root, "backups").apply { mkdirs() }
            listOf("a-1.json", "a-3.json", "a-2.json", "notes.txt").forEach { java.io.File(dir, it).writeText("{}") }
            assertEquals(listOf("a-3.json", "a-2.json", "a-1.json"), savedBackups(dir).getOrThrow().map { it.name })
        } finally { root.deleteRecursively() }
    }

    @Test fun `a folder Android cannot list is a red footnote, not a hidden Restore`() {
        val root = java.nio.file.Files.createTempDirectory("ui05-backups").toFile()
        try {
            // A file where the folder should be: listFiles() gives null.
            val notADir = java.io.File(root, "backups").apply { writeText("x") }
            val listing = savedBackups(notADir)
            assertTrue(listing.isFailure)
            assertEquals(BackupStatus("Could not list the saved copies: Android could not list the saved copies", error = true), backupFootnote(BackupAction.LIST, listing))
            assertNull(backupFootnote(BackupAction.LIST, Result.success(emptyList<java.io.File>())))
        } finally { root.deleteRecursively() }
    }
}
