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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h3200dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LoadShotsTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val dir = File("build/shots-load")
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 11, 25)
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneId.systemDefault()
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = LocalDateTime.of(2026, 11, 25, 21, 0).atZone(zone).toInstant()
    }

    @Before fun open() {
        assumeTrue(dir.isDirectory)
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
        runBlocking(Dispatchers.IO) { vm.maxHeartRate.set(LOAD_FIXTURE_MAX) }
    }

    @After fun close() {
        if (!::app.isInitialized) return
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

    private fun take(width: Int, scale: Float, set: String, progress: Boolean = false) {
        runBlocking(Dispatchers.IO) { seedLoad(app, if (set == "year") loadYearFixtures(today, zone) else loadFewFixtures(today, zone)) }
        compose.setContent {
            val d = LocalDensity.current
            val config = Configuration(LocalConfiguration.current).apply { fontScale = scale }
            CompositionLocalProvider(LocalDensity provides Density(d.density, scale), LocalConfiguration provides config) {
                KhonTheme {
                    Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) {
                        val nav = rememberNavController()
                        if (progress) HistoryScreen(vm, nav, clock) else LoadScreen(vm, nav, clock)
                    }
                }
            }
        }
        val tag = "${width}dp-x${if (scale == 1f) "1_0" else "1_3"}"
        if (progress) {
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Progress").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Progress").performSemanticsAction(SemanticsActions.OnClick)
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Load").fetchSemanticsNodes().isNotEmpty() }
            shot("progress-$tag")
        } else {
            compose.waitUntil(15_000) { compose.onAllNodesWithTag(loadWeekTag(weekOf(today)), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
            compose.waitUntil(15_000) { compose.onAllNodesWithTag(LOAD_CHART_TAG).fetchSemanticsNodes().isNotEmpty() }
            shot("load-$set-$tag")
        }
    }

    @Test fun year360() = take(360, 1f, "year")
    @Test fun year360x13() = take(360, 1.3f, "year")
    @Test fun few360() = take(360, 1f, "few")
    @Test fun few360x13() = take(360, 1.3f, "few")
    @Test fun progress360() = take(360, 1f, "year", progress = true)
    @Test fun progress360x13() = take(360, 1.3f, "year", progress = true)

    @Config(qualifiers = "w411dp-h3200dp-xxhdpi") @Test fun year411() = take(411, 1f, "year")
    @Config(qualifiers = "w411dp-h3200dp-xxhdpi") @Test fun year411x13() = take(411, 1.3f, "year")
    @Config(qualifiers = "w411dp-h3200dp-xxhdpi") @Test fun few411() = take(411, 1f, "few")
    @Config(qualifiers = "w411dp-h3200dp-xxhdpi") @Test fun few411x13() = take(411, 1.3f, "few")
    @Config(qualifiers = "w411dp-h3200dp-xxhdpi") @Test fun progress411() = take(411, 1f, "year", progress = true)
    @Config(qualifiers = "w411dp-h3200dp-xxhdpi") @Test fun progress411x13() = take(411, 1.3f, "year", progress = true)
}
