package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.health.HcStatus
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.theme.K
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

/**
 * Screenshots of "Heart rate data", its sheet, Review's line and the cardio form's placeholders, at 360 and 411 dp and
 * font scales 1.0 and 1.3, for a visual check. Runs only when `app/build/shots-hr` exists; the PNGs land there.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HeartShotsTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val dir = File("build/shots-hr")
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private lateinit var scope: CoroutineScope
    private lateinit var fill: HeartFill
    private val dao get() = app.db.dao()
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.now()

    @Before fun open() {
        assumeTrue(dir.isDirectory)
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        fill = app.repo.heartFill(FakeHeart(), scope)
    }

    @After fun close() { if (::app.isInitialized) { vm.viewModelScope.cancel(); scope.cancel(); app.scope.cancel(); app.db.close() } }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }
    private fun t(h: Int, m: Int, day: LocalDate) = day.atTime(h, m).atZone(zone).toInstant().toEpochMilli()

    /** Every window of the app drawn in order, so a sheet over the screen is in the picture. */
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

    private fun seed() = io {
        val y = today.minusDays(1)
        dao.upsert(Exercise("hiit", "Bike intervals", Kind.CARDIO, intervals = true, rounds = 4, fields = "time,avgHr"))
        val push = Session("push", y.iso(), t(18, 2, y), t(19, 14, y), ItemType.PROGRAM, programId = "p", programName = "Push day", timedFrom = t(18, 2, y), timedTo = t(19, 14, y))
        val bike = Session("bike", today.minusDays(2).iso(), t(7, 0, today.minusDays(2)), t(7, 31, today.minusDays(2)), ItemType.CARDIO, exerciseId = "hiit",
            exerciseName = "Bike intervals", timedFrom = t(7, 0, today.minusDays(2)), timedTo = t(7, 30, today.minusDays(2)), timeSec = 1800)
        val pull = Session("pull", today.minusDays(3).iso(), t(18, 0, today.minusDays(3)), t(18, 50, today.minusDays(3)), ItemType.PROGRAM, programId = "p", programName = "Pull day",
            timedFrom = t(18, 0, today.minusDays(3)), timedTo = t(18, 50, today.minusDays(3)))
        val legs = Session("legs", today.minusDays(5).iso(), t(18, 2, today.minusDays(5)), t(18, 42, today.minusDays(5)), ItemType.PROGRAM, programId = "p", programName = "Legs and a long program name")
        val run = Session("run", today.minusDays(6).iso(), t(7, 0, today.minusDays(6)), t(7, 40, today.minusDays(6)), ItemType.CARDIO, exerciseName = "Zone 2 run",
            timedFrom = t(7, 0, today.minusDays(6)), timedTo = t(7, 39, today.minusDays(6)))
        val swim = Session("swim", today.minusDays(8).iso(), t(7, 0, today.minusDays(8)), t(7, 40, today.minusDays(8)), ItemType.CARDIO, exerciseName = "Easy swim",
            timedFrom = t(7, 0, today.minusDays(8)), timedTo = t(7, 39, today.minusDays(8)))
        listOf(push, bike, pull, legs, run, swim).forEach { dao.upsert(it) }
        val bf = hrWindow(bike, zone)!!
        dao.upsertIntervalLogs((0 until 4).map { i -> IntervalLog("bi$i", "bike", i, startedAt = bf.fromMs + i * 420_000L, endedAt = bf.fromMs + i * 420_000L + 240_000L) })
        val samples = (0 until 372).joinToString(",") { "${it * 5}:${120 + (it % 48)}" }
        dao.upsertSessionHr(SessionHr("bike", bf.fromMs, bf.toMs, HrState.FILLED, samples, 412, 398, "com.mi.health", "com.mi.health,com.strava", bf.fromMs + 2_000, bf.toMs - 4_000, 45, 141, 167, fetchedAt = bf.toMs + 60_000))
        val pf = hrWindow(push, zone)!!
        dao.upsertSessionHr(SessionHr("push", pf.fromMs, pf.toMs, HrState.FILLED, "0:120,5:130", 520, 509, "com.mi.health", "com.mi.health", pf.fromMs + 3_000, pf.toMs - 60_000, 30, 128, 164, fetchedAt = pf.toMs + 60_000))
        val ul = hrWindow(pull, zone)!!
        dao.upsertSessionHr(SessionHr("pull", ul.fromMs, ul.toMs, HrState.WAITING, "0:110,960:118,1200:121", 3, 3, "com.mi.health", "com.mi.health", ul.fromMs, ul.fromMs + 1_200_000, 960, fetchedAt = ul.toMs + 60_000))
        val rf = hrWindow(run, zone)!!
        dao.upsertSessionHr(SessionHr("run", rf.fromMs, rf.toMs, HrState.OFF, fetchedAt = rf.toMs + 60_000))
    }

    private fun screen(access: HeartAccess = HeartAccess({ HcStatus.AVAILABLE }, { true })) = compose.setContent {
        KhonTheme { androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) { HeartDataScreen(vm, rememberNavController(), heart = fill, access = access) } }
    }

    private fun waitSaid(text: String) = compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
    private fun waitText(text: String) = compose.waitUntil(5_000) { compose.onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

    private fun listAndSheets(tag: String) {
        seed()
        screen()
        waitSaid("Easy swim,")
        shot("$tag-1-list")
        compose.onNodeWithContentDescription("Bike intervals,", substring = true).performSemanticsAction(SemanticsActions.OnClick)
        waitText("Also seen")
        Thread.sleep(400)
        shot("$tag-2-sheet-filled")
    }

    @Test @Config(qualifiers = "w360dp-h800dp-xxhdpi") fun w360() = listAndSheets("360-f1.0")
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi") fun w411() = listAndSheets("411-f1.0")
    @Test @Config(qualifiers = "w360dp-h800dp-xxhdpi", fontScale = 1.3f) fun w360Large() = listAndSheets("360-f1.3")
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi", fontScale = 1.3f) fun w411Large() = listAndSheets("411-f1.3")

    @Test @Config(qualifiers = "w360dp-h800dp-xxhdpi", fontScale = 1.3f) fun sheetsOfOtherStates() {
        seed()
        screen()
        waitSaid("Legs and")
        compose.onNodeWithContentDescription("Legs and", substring = true).performSemanticsAction(SemanticsActions.OnClick)
        waitText("Use this time range")
        Thread.sleep(400)
        shot("360-f1.3-3-sheet-use-range")
    }

    @Test @Config(qualifiers = "w360dp-h800dp-xxhdpi") fun sheetWaiting() {
        seed()
        screen()
        waitSaid("Pull day,")
        compose.onNodeWithContentDescription("Pull day,", substring = true).performSemanticsAction(SemanticsActions.OnClick)
        waitText("Waiting for data")
        Thread.sleep(400)
        shot("360-f1.0-4-sheet-waiting")
    }

    @Test @Config(qualifiers = "w360dp-h800dp-xxhdpi") fun noPermission() {
        seed()
        screen(HeartAccess({ HcStatus.AVAILABLE }, { false }))
        waitText("Read heart rate")
        shot("360-f1.0-5-no-permission")
    }

    @Test @Config(qualifiers = "w360dp-h800dp-xxhdpi") fun reviewAndForm() {
        seed()
        compose.setContent { KhonTheme { androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) {
            val form = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            if (form.value) CardioScreen(vm, rememberNavController(), "bike") else ReviewScreen(vm, rememberNavController(), "bike")
            androidx.compose.runtime.LaunchedEffect(Unit) { formSwitch = { form.value = true } }
        } } }
        waitText("avg 141 · max 167 bpm")
        shot("360-f1.0-6-review-cardio")
        compose.runOnIdle { formSwitch() }
        waitText("Round 4")
        shot("360-f1.0-7-cardio-form-placeholders")
    }

    private var formSwitch: () -> Unit = {}

    @Test @Config(qualifiers = "w360dp-h800dp-xxhdpi") fun reviewGym() {
        seed()
        compose.setContent { KhonTheme { androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) { ReviewScreen(vm, rememberNavController(), "push") } } }
        waitText("avg 128 · max 164 bpm")
        shot("360-f1.0-8-review-gym")
    }

    @Test @Config(qualifiers = "w360dp-h800dp-xxhdpi") fun settings() {
        compose.setContent { KhonTheme { androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) { SettingsScreen(vm, rememberNavController()) } } }
        waitText("Heart rate data")
        shot("360-f1.0-9-settings")
    }
}
