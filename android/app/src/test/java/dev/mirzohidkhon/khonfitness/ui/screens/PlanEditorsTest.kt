package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
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

/** The screens Plan opens (UI06 P4): the Exercises list and the cardio workout editor's Type with New type. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PlanEditorsTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private val dao get() = app.db.dao()

    @Before fun open() {
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
    }

    @After fun close() { app.scope.cancel(); app.db.close() }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }
    private fun shows(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    private fun waitFor(shown: () -> Boolean) = compose.waitUntil(5_000) { shown() }
    private fun top(text: String) = compose.onNodeWithText(text).fetchSemanticsNode().positionInRoot.y
    private fun tap(node: SemanticsNodeInteraction) { node.performSemanticsAction(SemanticsActions.OnClick); compose.waitForIdle() }

    /** The Exercises list with the exercise editor behind its rows. */
    private fun openLibrary() = compose.setContent {
        KhonTheme {
            val nav = rememberNavController()
            NavHost(nav, Routes.LIBRARY) {
                composable(Routes.LIBRARY) { LibraryScreen(vm, nav) }
                composable("exercise/{id}") { e -> ExerciseEditorScreen(vm, nav, e.arguments?.getString("id") ?: "new") }
            }
        }
    }

    private fun openEditor(id: String) = compose.setContent { KhonTheme { ExerciseEditorScreen(vm, rememberNavController(), id) } }

    private val run = Modality("run", "Run", 0xFF7CE0A8)
    private val bike = Modality("bike", "Bike", 0xFFB39DFF)
    private val row = Modality("row", "Row", 0xFFFF8FA3, archived = true)

    @Test fun `Exercises groups strength by muscle, archived last, with search and no kind or muscle filter`() {
        io {
            dao.upsert(run)
            dao.upsert(Exercise("bench", "Bench press", Kind.STRENGTH, primaryMuscle = "chest"))
            dao.upsert(Exercise("fly", "Cable fly", Kind.STRENGTH, primaryMuscle = "chest"))
            dao.upsert(Exercise("rowing", "Barbell row", Kind.STRENGTH, primaryMuscle = "back"))
            dao.upsert(Exercise("dip", "Dip", Kind.STRENGTH, primaryMuscle = "chest", archived = true))
            dao.upsert(Exercise("z2run", "Zone 2 run", Kind.CARDIO, modalityId = "run"))
        }
        openLibrary()
        waitFor { shows("Bench press") }
        assertTrue(top("Back") < top("Barbell row") && top("Barbell row") < top("Chest") && top("Chest") < top("Bench press"))
        assertTrue(top("Cable fly") < top("Archived") && top("Archived") < top("Dip"))
        listOf("Zone 2 run", "All", "Strength", "Cardio", "Muscle", "Show archived").forEach { assertFalse(it, shows(it)) }
        compose.onNodeWithContentDescription("New exercise").assertExists()

        compose.onNodeWithText("Search").performTextInput("fly")
        waitFor { !shows("Bench press") }
        assertTrue(shows("Cable fly"))

        tap(compose.onNodeWithText("Cable fly"))
        waitFor { shows("Merge into…") }
        listOf("Archive", "Delete", "Primary").forEach { assertTrue(it, shows(it)) }
    }

    @Test fun `Type lists live types and New type, and a new type is saved and picked`() {
        io {
            dao.upsert(run); dao.upsert(bike); dao.upsert(row)
            dao.upsert(Exercise("z2run", "Zone 2 run", Kind.CARDIO, modalityId = "run"))
        }
        openEditor("z2run")
        waitFor { shows("Type") }
        assertTrue(shows(TYPE_NOTE))
        assertFalse(shows("Modality"))
        tap(compose.onNodeWithText("Type"))
        waitFor { shows("New type…") }
        assertTrue(shows("Bike"))
        assertFalse("archived type", shows("Row"))

        tap(compose.onNodeWithText("New type…"))
        waitFor { shows("Add type") }
        compose.onNodeWithText("Add type").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Type name").performTextReplacement("Rowing erg")
        tap(compose.onNodeWithContentDescription("Color 2"))
        tap(compose.onNodeWithText("Add type"))
        waitFor { shows("Rowing erg") && !shows("Add type") }
        val added = runBlocking(Dispatchers.IO) { dao.modalitiesOnce() }.single { it.name == "Rowing erg" }
        assertEquals(PRESET_COLORS[1], added.color)

        tap(compose.onNodeWithText("Done"))
        compose.waitUntil(5_000) { runBlocking(Dispatchers.IO) { dao.exercise("z2run") }?.modalityId == added.id }
    }

    @Test fun `a new cardio workout starts as cardio with no note on past sessions`() {
        io { dao.upsert(run) }
        openEditor(NEW_CARDIO)
        waitFor { shows("New cardio workout") }
        assertTrue(shows("Type") && shows("Intensity"))
        assertFalse(shows(TYPE_NOTE))
        assertFalse(shows("Primary"))
    }
}
