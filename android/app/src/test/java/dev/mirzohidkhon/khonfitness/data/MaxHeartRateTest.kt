package dev.mirzohidkhon.khonfitness.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class MaxHeartRateTest {
    private val dir: File = Files.createTempDirectory("max-hr").toFile()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store = PreferenceDataStoreFactory.create(scope = scope) { File(dir, "settings.preferences_pb") }
    private val max = MaxHeartRate(store)

    @After fun close() { scope.cancel(); dir.deleteRecursively() }

    @Test fun storesClearsAndRefuses() = runBlocking {
        assertNull(max.typed.first())
        assertTrue(max.set(186))
        assertEquals(186, max.typed.first())
        assertFalse(max.set(99))
        assertFalse(max.set(231))
        assertEquals(186, max.typed.first())
        assertTrue(max.set(null))
        assertNull(max.typed.first())
        assertFalse(max.set(99))
        assertNull(store.data.first()[MaxHeartRate.KEY])
    }

    @Test fun aValueOfAnotherTypeReadsAsNone() = runBlocking {
        store.edit { it[stringPreferencesKey("max_heart_rate")] = "186" }
        assertNull(max.typed.first())
    }
}
