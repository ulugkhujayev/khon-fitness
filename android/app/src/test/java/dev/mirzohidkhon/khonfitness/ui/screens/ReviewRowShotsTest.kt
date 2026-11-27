package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.graphics.Bitmap
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
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.hrFacts
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
@Config(application = Application::class, qualifiers = "w360dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReviewRowShotsTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val dir = File("build/shots-review-row")
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private val zone = ZoneId.systemDefault()
    private val day = LocalDate.now(zone).minusDays(1)

    @Before fun open() {
        assumeTrue(dir.isDirectory)
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
    }

    @After fun close() {
        if (!::app.isInitialized) return
        compose.runOnIdle { id = "" }
        compose.waitForIdle()
        runBlocking(Dispatchers.IO) { app.db.withTransaction {} }
        if (::vm.isInitialized) vm.viewModelScope.cancel()
        app.scope.cancel()
        compose.waitForIdle()
        app.db.close()
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        compose.runOnIdle {
            val v = compose.activity.window.decorView
            val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
            v.draw(android.graphics.Canvas(bmp))
            File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private var id by mutableStateOf("gym")
    private var scale by mutableStateOf(1f)

    private fun all(width: Int) {
        val gym = gymFixture(day, zone)
        val long = gymFixture(day, zone, id = "long").let { it.copy(row = it.row!!.copy(avgHr = 188, maxHr = 201)) }
        val fixtures = listOf(gym, long, gymFixture(day, zone, heart = false, id = "plain"), cardioFixture(day, zone))
        runBlocking(Dispatchers.IO) {
            val dao = app.db.dao()
            fixtures.forEach { f -> dao.upsert(f.session); dao.upsertSetLogs(f.logs); dao.upsertIntervalLogs(f.rounds); f.row?.let { dao.upsertSessionHr(it) } }
        }
        compose.setContent {
            val d = LocalDensity.current
            val config = Configuration(LocalConfiguration.current).apply { fontScale = scale }
            CompositionLocalProvider(LocalDensity provides Density(d.density, scale), LocalConfiguration provides config) {
                KhonTheme {
                    Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) {
                        key(id, scale) {
                            when (id) {
                                "" -> Unit
                                "cardio" -> CardioScreen(vm, rememberNavController(), id)
                                else -> SessionScreen(vm, rememberNavController(), id)
                            }
                        }
                    }
                }
            }
        }
        for (s in listOf(1f, 1.3f)) for (f in fixtures) {
            val spoken = reviewEntrySpoken(hrSpoken(f.session, hrFacts(f.session, f.row, zone)))
            compose.runOnIdle { id = f.session.id; scale = s }
            compose.waitForIdle()
            compose.waitUntil(5_000) { compose.onAllNodes(hasContentDescription(spoken)).fetchSemanticsNodes().isNotEmpty() }
            shot("review-row-${f.session.id}-${width}dp-x${if (s == 1f) "1_0" else "1_3"}")
        }
    }

    @Test fun at360() = all(360)

    @Config(qualifiers = "w411dp-h900dp-xxhdpi")
    @Test fun at411() = all(411)
}
