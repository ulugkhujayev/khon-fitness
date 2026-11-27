package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import dev.mirzohidkhon.khonfitness.data.NightsState
import dev.mirzohidkhon.khonfitness.data.NoSleepAccess
import dev.mirzohidkhon.khonfitness.data.SleepSource
import dev.mirzohidkhon.khonfitness.health.HcStatus
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h2400dp-mdpi")
class NightBarHeightTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun `a six minute night has a taller bar than a three minute night`() {
        val app = newApp()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val zone = ZoneId.systemDefault()
        val shorter = NIGHT_TODAY.minusDays(2)
        val longer = NIGHT_TODAY.minusDays(1)
        val source = FakeSleepSource(listOf(nightSpan(shorter, 3, zone), nightSpan(longer, 6, zone)))
        val store = installNights(app, source, scope, zone)
        val vm = AppViewModel(app)
        try {
            compose.setContent {
                KhonTheme {
                    NightsScreen(vm, rememberNavController(), nightTestClock(zone), hcStatus = { HcStatus.AVAILABLE })
                }
            }
            compose.waitUntil(5_000) { store.state.value is NightsState.Loaded }
            fun height(day: java.time.LocalDate) = compose.onNodeWithTag(nightFillTag(day), useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot.height
            val shortHeight = height(shorter)
            val longHeight = height(longer)
            assertTrue("Three minutes: $shortHeight px. Six minutes: $longHeight px.", shortHeight < longHeight)
        } finally {
            compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
            vm.viewModelScope.cancel()
            scope.cancel()
            app.scope.cancel()
            app.db.close()
        }
    }
}
