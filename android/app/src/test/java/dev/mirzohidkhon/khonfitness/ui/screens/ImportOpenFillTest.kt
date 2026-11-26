package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Import screen asks the heart rate fill once per visit, through the app's one runner. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ImportOpenFillTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private var shown by mutableStateOf(true)
    private var tick by mutableIntStateOf(0)

    @Before fun open() {
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
    }

    @After fun close() {
        shown = false
        compose.waitForIdle()
        vm.viewModelScope.cancel()
        app.scope.cancel()
        compose.waitForIdle()
        app.db.close()
    }

    private fun passes() = app.heartFill.passes.value
    /** Waits until the runner ended [n] passes, then a little longer, so a late extra request would show. */
    private fun settlesAt(n: Int) {
        compose.waitUntil(5_000) { passes() >= n }
        Thread.sleep(300)
        compose.waitForIdle()
        assertEquals(n, passes())
    }

    @Test fun `D18 AT24 each visit asks one fill, and recomposition and Check again ask none`() {
        compose.setContent {
            KhonTheme {
                if (shown) {
                    tick
                    ImportScreen(vm, rememberNavController())
                }
            }
        }
        settlesAt(1)
        repeat(3) { tick++; compose.waitForIdle() }
        compose.onAllNodesWithText("Check again").fetchSemanticsNodes().indices.forEach { i ->
            compose.onAllNodesWithText("Check again")[i].performClick()
        }
        settlesAt(1)
        shown = false
        compose.waitForIdle()
        shown = true
        compose.waitForIdle()
        settlesAt(2)
    }
}
