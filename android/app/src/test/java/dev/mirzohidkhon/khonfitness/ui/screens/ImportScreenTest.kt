package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.ImportLocal
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.TEST_ZONE
import dev.mirzohidkhon.khonfitness.data.at
import dev.mirzohidkhon.khonfitness.health.HcStatus
import dev.mirzohidkhon.khonfitness.health.HealthImport
import dev.mirzohidkhon.khonfitness.health.ImportDraft
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import java.time.Instant
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

private class FakeImportSource(private val records: List<ImportDraft>) : ImportSource {
    @Volatile var status = HcStatus.AVAILABLE
    @Volatile var granted = true
    val reads = AtomicInteger()
    override fun status(context: Context) = status
    override suspend fun granted(context: Context) = granted
    override suspend fun drafts(context: Context, local: suspend () -> ImportLocal): List<ImportDraft> {
        reads.incrementAndGet()
        return HealthImport.offered(records, local())
    }
}

private class GrantingRegistryOwner : ActivityResultRegistryOwner {
    val launches = AtomicInteger()
    override val activityResultRegistry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
            launches.incrementAndGet()
            dispatchResult(requestCode, emptySet<String>())
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ImportScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private val oldZone = TimeZone.getDefault()
    private var shown by mutableStateOf(true)
    private val ride = Exercise("hill", "Hill rides", Kind.CARDIO, modalityId = "bike", intervals = true, workSec = 240, restSec = 180, rounds = 4)

    @Before fun open() {
        TimeZone.setDefault(TimeZone.getTimeZone(TEST_ZONE))
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        runBlocking { app.db.dao().upsert(ride) }
        vm = AppViewModel(app)
    }

    @After fun close() {
        shown = false
        compose.waitForIdle()
        vm.viewModelScope.cancel()
        app.scope.cancel()
        compose.waitForIdle()
        app.db.close()
        TimeZone.setDefault(oldZone)
    }

    private fun band(id: String, hour: Int, type: String) =
        ImportDraft(id, Instant.ofEpochMilli(at(hour, 0)), Instant.ofEpochMilli(at(hour, 30)), type, "bike", null, null, emptyList(), listOf("com.mi.health"))

    private fun show(source: ImportSource, registry: ActivityResultRegistryOwner = GrantingRegistryOwner()) = compose.setContent {
        CompositionLocalProvider(LocalActivityResultRegistryOwner provides registry) {
            KhonTheme { if (shown) ImportScreen(vm, rememberNavController(), source) }
        }
    }

    private fun row(type: String) = hasContentDescription(type, substring = true)
    private fun shows(type: String) = compose.onAllNodes(row(type)).fetchSemanticsNodes().isNotEmpty()
    private fun waitFor(what: () -> Boolean) = compose.waitUntil(5_000) { ShadowLooper.idleMainLooper(); what() }
    private fun node(text: String): SemanticsNodeInteraction = compose.onNode(hasText(text))
    private fun asked() = app.heartFill.asked.value
    private fun saved() = runBlocking { app.db.dao().sessionsOnce().filter { it.sourceId != null }.map { it.sourceId } }.toSet()

    private fun importOne(type: String, exercise: String = ride.name) {
        compose.onNode(row(type)).performClick()
        waitFor { compose.onAllNodes(hasText(exercise)).fetchSemanticsNodes().isNotEmpty() }
        node(exercise).performSemanticsAction(SemanticsActions.OnClick)
    }

    private fun failRoundsOf(sourceId: String) = app.db.openHelper.writableDatabase.execSQL(
        "CREATE TRIGGER fail_rounds BEFORE INSERT ON interval_logs WHEN (SELECT sourceId FROM sessions WHERE id = NEW.sessionId) = '$sourceId' " +
            "BEGIN SELECT RAISE(ABORT, 'held'); END",
    )

    @Test fun `AT09 D7a a failed write keeps its draft on screen after Import all and after a single import, and the list is read again each time`() {
        val source = FakeImportSource(listOf(band("bad", 10, "Ride bad"), band("ok1", 12, "Ride one"), band("ok2", 14, "Ride two")))
        failRoundsOf("bad")
        show(source)
        waitFor { shows("Ride bad") && shows("Ride one") && shows("Ride two") }
        assertEquals(1, source.reads.get())

        node(importAllLabel(3, 3)).performClick()
        waitFor { source.reads.get() == 2 && !shows("Ride one") }
        assertTrue(shows("Ride bad"))
        assertEquals(false, shows("Ride two"))
        assertEquals(setOf("ok1", "ok2"), saved())

        importOne("Ride bad")
        waitFor { source.reads.get() == 3 }
        compose.waitForIdle()
        assertTrue(shows("Ride bad"))
        assertEquals(setOf("ok1", "ok2"), saved())

        app.db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_rounds")
        importOne("Ride bad")
        waitFor { source.reads.get() == 4 && !shows("Ride bad") }
        node("All caught up").assertExists()
        assertEquals(setOf("ok1", "ok2", "bad"), saved())
    }

    @Test fun `AT05 a draft covered after the list was read is refused by a single import and by Import all, and the reload removes it`() {
        val source = FakeImportSource(listOf(band("m", 10, "Ride m"), band("n", 12, "Ride n"), band("e", 14, "Ride e"), band("s", 16, "Ride s")))
        show(source)
        waitFor { listOf("Ride m", "Ride n", "Ride e", "Ride s").all(::shows) }
        fun cover(id: String, hour: Int) = runBlocking {
            app.db.dao().insertSessionWithLogs(
                Session(id, "2026-11-20", at(hour, 0), finishedAt = at(hour, 31), itemType = ItemType.CARDIO, exerciseId = ride.id,
                    exerciseName = ride.name, timedFrom = at(hour, 0), timedTo = at(hour, 30)),
                emptyList(), emptyList(),
            )
        }
        cover("app-s", 16)

        importOne("Ride s")
        waitFor { source.reads.get() == 2 && !shows("Ride s") }
        assertTrue(listOf("Ride m", "Ride n", "Ride e").all(::shows))
        assertEquals(emptySet<String?>(), saved())

        cover("app-n", 12)

        node(importAllLabel(3, 3)).performClick()
        waitFor { source.reads.get() == 3 && !shows("Ride m") }
        node("All caught up").assertExists()
        assertEquals(setOf("m", "e"), saved())
    }

    @Test fun `D18 AT24 each visit asks one fill at its call, and Check again, the permission result, and a recomposition ask none`() {
        val source = FakeImportSource(listOf(band("one", 12, "Ride one")))
        source.status = HcStatus.NOT_INSTALLED
        source.granted = false
        val registry = GrantingRegistryOwner()
        show(source, registry)
        waitFor { asked() == 1 }

        source.status = HcStatus.AVAILABLE
        node("Check again").performClick()
        waitFor { compose.onAllNodes(hasText("Allow")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, source.reads.get())

        source.granted = true
        node("Allow").performClick()
        waitFor { shows("Ride one") }
        assertEquals(1 to 1, registry.launches.get() to source.reads.get())

        runBlocking { app.db.dao().upsert(ride.copy(name = "Long rides")) }
        waitFor { shows("imports as Long rides") }
        compose.waitForIdle()
        assertEquals(1, asked())

        importOne("Ride one", "Long rides")
        waitFor { source.reads.get() == 2 && !shows("Ride one") }
        compose.waitForIdle()
        assertEquals(2, asked())

        shown = false
        compose.waitForIdle()
        shown = true
        waitFor { asked() == 3 }
        compose.waitForIdle()
        assertEquals(3, asked())
    }
}
