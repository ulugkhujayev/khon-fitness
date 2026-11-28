package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.iso
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

/** A bike ride with a few band metres shows no distance or pace on any screen; a swim shows pace per 100 m. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CardioDisplayTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private val day = LocalDate.now()
    private val start = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val bike = Exercise("test-bike", "Test Bike", Kind.CARDIO, modalityId = "bike", fields = "time,distance")
    private val ride = Session("test-ride", day.iso(), start, start + 2_700_000, ItemType.CARDIO,
        exerciseId = bike.id, exerciseName = bike.name, timeSec = 2700, distanceM = 60)
    private val swim = bike.copy(id = "test-swim", name = "Test Swim", modalityId = "swim", fields = "time,distance")
    private val swum = ride.copy(id = "test-swum", exerciseId = swim.id, exerciseName = swim.name, timeSec = 1800, distanceM = 1000, finishedAt = start + 1_800_000)

    @Before fun open() {
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java)
            .apply { isAccessible = true }.invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
    }

    /** Off at the end: the screen leaves composition and stops its queries before the database closes. */
    private var shown by mutableStateOf(true)

    @After fun close() {
        shown = false
        compose.waitForIdle()
        vm.viewModelScope.cancel()
        app.scope.cancel()
        app.db.close()
    }

    private fun seed(exercise: Exercise = bike, session: Session = ride) = runBlocking(Dispatchers.IO) {
        app.db.dao().upsert(exercise)
        app.db.dao().upsert(session)
    }

    private fun show(content: @Composable (NavHostController) -> Unit) {
        compose.setContent { if (shown) KhonTheme { content(rememberNavController()) } }
    }

    private fun count(text: String, substring: Boolean = false) =
        compose.onAllNodesWithText(text, substring = substring, useUnmergedTree = true).fetchSemanticsNodes().size

    private fun shows(text: String, substring: Boolean = false) = count(text, substring) > 0

    private fun await(text: String, substring: Boolean = false) =
        compose.waitUntil(10_000) { shows(text, substring) }

    @Test fun reviewHidesBandMetres() {
        seed()
        show { ReviewScreen(vm, it, ride.id) }
        await("45 min")
        assertFalse("Review displays the filtered distance", shows("0.06 km"))
        assertFalse("Review displays a pace row", shows("Pace"))
    }

    @Test fun historyWeekExcludesBandMetres() {
        seed()
        show { nav ->
            Column(Modifier.verticalScroll(rememberScrollState())) { HistoryLog(vm, nav, day) }
        }
        await(bike.name)
        assertFalse("History weekly totals include the filtered distance", shows("0.1 km", substring = true))
    }

    @Test fun progressHidesBandMetres() {
        seed()
        show { nav ->
            Column(Modifier.verticalScroll(rememberScrollState())) { HistoryProgress(vm, nav, day) }
        }
        await("1 session · 45 m")
        assertFalse("History displays the filtered distance chart", shows("Distance per session"))
    }

    @Test fun cardioDetailUsesTimeForBandMetres() {
        seed()
        show { CardioTypeScreen(vm, it, "bike") }
        await(bike.name, substring = true)
        assertFalse("History displays a pace chart for the filtered distance", shows("Pace /km"))
        await("Time, min")
    }

    @Test fun cardioEditorHidesComputedPaceForBandMetres() {
        seed()
        show { CardioScreen(vm, it, ride.id) }
        await("Time")
        assertFalse("Cardio editor derives pace from filtered distance", shows("Pace"))
    }

    @Test fun cardioEditorUsesSwimPaceForStoredDistance() {
        val lapSwim = swim.copy(fields = "time,laps,poolLength")
        seed(lapSwim, ride.copy(exerciseId = lapSwim.id, exerciseName = lapSwim.name, distanceM = 1000, timeSec = 1800, laps = 40, poolLength = 25))
        show { CardioScreen(vm, it, ride.id) }
        await("3:00 /100 m")
        assertFalse("Cardio editor displays a swim pace per km", shows("/km", substring = true))
    }

    @Test fun cardioEditorLapDistanceDoesNotWrap() {
        val lapSwim = swim.copy(fields = "time,laps,poolLength")
        seed(lapSwim, ride.copy(exerciseId = lapSwim.id, exerciseName = lapSwim.name, distanceM = null, timeSec = 1800, laps = 70_000, poolLength = 70_000))
        show { CardioScreen(vm, it, ride.id) }
        await("4900000000 m")
        assertFalse("Cardio editor derives a pace from a distance past the Int range", shows("Pace"))
    }

    @Test fun compareTotalsExcludeBandMetres() {
        val totals = periodStats(Period(day, day), listOf(ride), emptyList(), listOf(bike), emptyList(), emptyList())
        assertEquals("Compare totals include the filtered distance", 0, totals.meters)
    }

    @Test fun todayDoneLineGivesSwimPacePer100m() {
        seed(swim, swum)
        show { TodayScreen(vm, it) }
        await("Done · 1 km · 30 min · 3:00 /100 m", substring = true)
        assertFalse("Today displays a swim pace per km", shows("/km", substring = true))
    }

    @Test fun reviewGivesSwimPacePer100mInStatLineAndPaceRow() {
        seed(swim, swum)
        show { ReviewScreen(vm, it, swum.id) }
        await("1 km · 30 min · 3:00 /100 m")
        assertTrue("Review has no Pace row", shows("Pace"))
        assertTrue("Review Pace row is not per 100 m", shows("3:00 /100 m"))
        assertFalse("Review displays a swim pace per km", shows("/km", substring = true))
    }
}
