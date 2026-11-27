package dev.mirzohidkhon.khonfitness.health

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
class HealthStatusTest {
    @Test fun `a phone with no Health Connect app reads as not installed`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        assertEquals(HcStatus.NOT_INSTALLED, HealthImport.status(context))
    }
}
