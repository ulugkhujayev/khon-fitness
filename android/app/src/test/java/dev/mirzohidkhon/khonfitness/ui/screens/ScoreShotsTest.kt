package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.Modality
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.theme.K
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h3200dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScoreShotsTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val dir = File("build/shots-score")
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.now(zone)

    @Before fun open() {
        assumeTrue(dir.isDirectory)
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
    }

    @After fun close() {
        if (!::app.isInitialized) return
        compose.runOnIdle { screen = "" }
        compose.waitForIdle()
        runBlocking(Dispatchers.IO) { app.db.withTransaction {} }
        if (::vm.isInitialized) vm.viewModelScope.cancel()
        app.scope.cancel()
        compose.waitForIdle()
        app.db.close()
    }

    @Suppress("UNCHECKED_CAST")
    private fun shot(name: String) {
        compose.waitForIdle()
        compose.runOnIdle {
            val global = Class.forName("android.view.WindowManagerGlobal").getMethod("getInstance").invoke(null)
            val views = (global.javaClass.getDeclaredField("mViews").apply { isAccessible = true }.get(global) as List<View>).filter { it.isShown }
            val base = compose.activity.window.decorView
            val bmp = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            for (v in views) {
                val at = IntArray(2).also { v.getLocationOnScreen(it) }
                canvas.save(); canvas.translate(at[0].toFloat(), at[1].toFloat()); v.draw(canvas); canvas.restore()
            }
            File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private var screen by mutableStateOf("")
    private var scale by mutableStateOf(1f)

    private fun seed() = runBlocking(Dispatchers.IO) {
        val dao = app.db.dao()
        dao.upsertExercises(listOf(HIGH_EXERCISE, LOW_EXERCISE))
        dao.upsert(Modality("bike", "Bike", 0xFFB39DFF))
        dao.upsert(Modality("run", "Run", 0xFF7CE0A8))
        val high = listOf(1L to 0, 4L to -3, 8L to 2, 11L to -6).mapIndexed { i, (ago, lift) -> intervalFixture(today.minusDays(ago), zone, if (i == 0) "hiit" else "hiit$i", lift = lift) }
        val low = listOf(2L to 0, 5L to 4, 9L to -5, 12L to 7).mapIndexed { i, (ago, lift) -> zoneFixture(today.minusDays(ago), zone, if (i == 0) "z2" else "z2$i", lift = lift) }
        val ten = intervalFixture(today.minusDays(14), zone, "hiit10", rounds = 10)
        (high + low + ten).forEach { f -> dao.upsert(f.session); dao.upsertIntervalLogs(f.rounds); f.row?.let { dao.upsertSessionHr(it) } }
        vm.maxHeartRate.set(186)
    }

    private fun all(width: Int) {
        seed()
        compose.setContent {
            val d = LocalDensity.current
            val config = Configuration(LocalConfiguration.current).apply { fontScale = scale }
            CompositionLocalProvider(LocalDensity provides Density(d.density, scale), LocalConfiguration provides config) {
                KhonTheme {
                    Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) {
                        key(screen, scale) {
                            val nav = rememberNavController()
                            when {
                                screen == "settings" -> SettingsScreen(vm, nav)
                                screen.startsWith("type:") -> CardioTypeScreen(vm, nav, screen.removePrefix("type:"))
                                screen.isNotEmpty() -> ReviewScreen(vm, nav, screen)
                            }
                        }
                    }
                }
            }
        }
        for (s in listOf(1f, 1.3f)) {
            val tag = "${width}dp-x${if (s == 1f) "1_0" else "1_3"}"
            for ((id, marker) in listOf("hiit" to "Round 4", "hiit10" to "Round 10", "z2" to "Above 130 bpm")) {
                compose.runOnIdle { screen = id; scale = s }
                compose.waitUntil(5_000) { compose.onAllNodesWithText(marker).fetchSemanticsNodes().isNotEmpty() }
                compose.waitUntil(5_000) { compose.onAllNodesWithTag(HEART_PICTURE_TAG).fetchSemanticsNodes().isNotEmpty() }
                shot("review-$id-$tag")
            }
            for ((type, chart) in listOf("bike" to HARD_CHART_TAG, "run" to ZONE_CHART_TAG)) {
                compose.runOnIdle { screen = "type:$type"; scale = s }
                compose.waitUntil(5_000) { compose.onAllNodesWithTag(chart).fetchSemanticsNodes().isNotEmpty() }
                if (type == "bike") compose.waitUntil(5_000) { compose.onAllNodesWithText("Heart rate in work intervals").fetchSemanticsNodes().isNotEmpty() }
                shot("type-$type-$tag")
            }
            runBlocking(Dispatchers.IO) { vm.maxHeartRate.set(null) }
            compose.runOnIdle { screen = "settings"; scale = s }
            compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("Maximum heart rate, measured", substring = true).fetchSemanticsNodes().isNotEmpty() }
            shot("settings-measured-$tag")
            runBlocking(Dispatchers.IO) { vm.maxHeartRate.set(186) }
            compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("Maximum heart rate, 186 beats per minute").fetchSemanticsNodes().isNotEmpty() }
            shot("settings-$tag")
            compose.onNodeWithContentDescription("Maximum heart rate, 186 beats per minute").performScrollTo().performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("Maximum heart rate, beats per minute").fetchSemanticsNodes().isNotEmpty() }
            compose.mainClock.advanceTimeBy(1_000)
            shot("settings-sheet-$tag")
            compose.runOnIdle { screen = "" }
            compose.waitForIdle()
        }
    }

    @Test fun at360() = all(360)

    @Config(qualifiers = "w411dp-h3200dp-xxhdpi")
    @Test fun at411() = all(411)
}
