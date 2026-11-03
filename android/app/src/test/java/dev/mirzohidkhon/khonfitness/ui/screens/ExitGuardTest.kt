package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UI06 P5 row 9: the stretch player's exits (the run's end, the 1.5 s delayed close, Stop, system Back) go through
 * [rememberExitGuard], so a late one never pops the screen under the player, and Back with a close in one frame pops once.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ExitGuardTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var nav: NavHostController
    private lateinit var guard: ExitGuard

    /** Today under History under the player, as when a routine opens from a pushed screen. */
    private fun open() {
        compose.setContent {
            nav = rememberNavController()
            NavHost(nav, "today") {
                composable("today") { Text("Today") }
                composable("history") { Text("History") }
                composable("stretch") { guard = rememberExitGuard(nav); Text("Player") }
                composable("other") { Text("Other") }
            }
        }
        compose.runOnIdle { nav.navigate("history"); nav.navigate("stretch") }
        compose.waitForIdle()
    }

    private fun routes() = nav.currentBackStack.value.mapNotNull { it.destination.route }

    @Test fun `a close after Back has left does not pop the screen under the player`() {
        open()
        val late = guard
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertEquals(listOf("today", "history"), routes())
        compose.runOnUiThread { assertFalse(late.leave()) }
        compose.waitForIdle()
        assertEquals(listOf("today", "history"), routes())
    }

    @Test fun `Back and the run's end in one frame pop once`() {
        open()
        compose.runOnUiThread { guard.leave(); compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertEquals(listOf("today", "history"), routes())
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed(); guard.leave() }
        compose.waitForIdle()
        assertEquals(listOf("today"), routes())
    }

    @Test fun `the delayed close does nothing while another screen is on top`() {
        open()
        compose.runOnIdle { nav.navigate("other") }
        compose.waitForIdle()
        compose.runOnUiThread { assertFalse(guard.leave()) }
        assertEquals(listOf("today", "history", "stretch", "other"), routes())
    }

    @Test fun `two closes in one frame pop once`() {
        open()
        compose.runOnUiThread { guard.leave(); guard.leave() }
        compose.waitForIdle()
        assertEquals(listOf("today", "history"), routes())
    }
}
