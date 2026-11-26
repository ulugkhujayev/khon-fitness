package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
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
import java.time.LocalDate

/**
 * Screenshots of the exercise detail with setup changes: the bench in three periods at 360 and 411 dp and at font
 * scale 1.3, pull-ups without weight, and a row under a 40-character program name. Runs only when
 * `app/build/shots-setup` exists; the PNGs land there.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h1900dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SetupShotsTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val dir = File("build/shots-setup")
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel

    @Before fun open() {
        assumeTrue(dir.isDirectory)
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
    }

    @After fun close() { if (::app.isInitialized) { if (::vm.isInitialized) vm.viewModelScope.cancel(); app.scope.cancel(); app.db.close() } }

    private fun detail(id: String, name: String) {
        runBlocking(Dispatchers.IO) { seedSetupChanges(app.db.dao(), LocalDate.now()) }
        compose.setContent {
            KhonTheme {
                androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides K.Text) {
                    Box(Modifier.fillMaxSize().background(K.Bg)) { ExerciseDetailScreen(vm, rememberNavController(), id) }
                }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Sessions", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        compose.runOnIdle {
            val v = compose.activity.window.decorView
            val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
            v.draw(android.graphics.Canvas(bmp))
            File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun bench360() = detail("bench", "1-bench-360")
    @Test @Config(qualifiers = "w411dp-h1900dp-xxhdpi") fun bench411() = detail("bench", "2-bench-411")
    @Test @Config(fontScale = 1.3f) fun benchLargeText() = detail("bench", "3-bench-360-font-1.3")
    @Test fun pullups() = detail("pullup", "4-pullup-reps")
    @Test @Config(fontScale = 1.3f) fun longProgram() = detail("row", "5-row-long-program-font-1.3")
}
