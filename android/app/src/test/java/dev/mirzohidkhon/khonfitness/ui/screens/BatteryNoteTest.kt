package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** UI06 P5 row 7: the timer's battery note names no phone maker, and its button opens the app's own settings. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class BatteryNoteTest {
    @Test fun `the note is neutral`() {
        assertEquals("If the timer stops when the screen is off, allow Khon to run in the background in battery settings.", BATTERY_NOTE)
        assertFalse(BATTERY_NOTE.contains("Samsung", ignoreCase = true))
    }

    @Test fun `the button opens the app's settings page`() {
        val i = appSettingsIntent("dev.mirzohidkhon.khonfitness")
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, i.action)
        assertEquals("package:dev.mirzohidkhon.khonfitness", i.dataString)
    }
}
