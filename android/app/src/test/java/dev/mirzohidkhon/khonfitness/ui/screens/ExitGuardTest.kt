package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.Block
import dev.mirzohidkhon.khonfitness.data.BlockExercise
import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.data.Modality
import dev.mirzohidkhon.khonfitness.data.Program
import dev.mirzohidkhon.khonfitness.data.RoutineStretch
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.data.Stretch
import dev.mirzohidkhon.khonfitness.data.StretchRoutine
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Every closing action of every editor (Done, Back, Archive, Unarchive, Delete, Merge, Discard) takes the screen's one
 * guard ([ExitGuard]): two taps in one frame, or two different exits in one frame, write once and pop once (attack
 * cases 14, 15), and a refused action gives the guard back (case 16). Real database and back stack; triggers count the row writes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ExitGuardTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private lateinit var nav: NavHostController
    private val dao get() = app.db.dao()
    private val sql get() = app.db.openHelper.writableDatabase

    @Before fun open() {
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
        io {
            dao.upsertExercises(listOf(
                Exercise("a", "Old Bench", Kind.STRENGTH), Exercise("b", "Bench Press", Kind.STRENGTH), Exercise("z", "Shelf Row", Kind.STRENGTH, archived = true),
                Exercise("run", "Run", Kind.CARDIO, fields = "time"),
            ))
            dao.upsertModalities(listOf(Modality("m", "Bike", 0xFFB39DFF), Modality("m2", "Row", 0xFF123456, archived = true)))
            dao.upsertPrograms(listOf(Program("p1", "Push", active = true), Program("p2", "Pull", sortOrder = 1)))
            dao.upsertBlocks(listOf(Block("k0", "p1", 0)))
            dao.upsertBlockExercises(listOf(BlockExercise("r0", "k0", 0, "b")))
            dao.upsertStretches(listOf(Stretch("st", "Calf stretch"), Stretch("st2", "Neck stretch")))
            dao.upsert(StretchRoutine("ro", "Evening", sortOrder = 0))
            dao.upsert(RoutineStretch("ro-1", "ro", "st", 0))
            dao.upsert(Session(id = "c1", date = "2026-11-03", startedAt = 1L, itemType = ItemType.CARDIO, exerciseId = "run", exerciseName = "Run", timeSec = 600))
        }
        // Every row write on the edited tables, by table and kind.
        sql.execSQL("CREATE TABLE test_writes (tbl TEXT, op TEXT)")
        for (t in listOf("exercises", "modalities", "programs", "blocks", "block_exercises", "stretches", "stretch_routines", "routine_stretches", "sessions")) {
            for (op in listOf("INSERT", "UPDATE", "DELETE")) sql.execSQL("CREATE TRIGGER test_${t}_$op AFTER $op ON $t BEGIN INSERT INTO test_writes VALUES ('$t', '$op'); END")
        }
    }

    @After fun close() { app.scope.cancel(); app.db.close() }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }
    private fun shows(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    private fun until(shown: () -> Boolean) {
        repeat(250) { if (shown()) return; compose.waitForIdle(); Thread.sleep(20) }
        throw AssertionError("the screen did not reach the expected state")
    }
    private fun routes() = nav.currentBackStack.value.mapNotNull { it.destination.route }
    private fun writes(): Map<String, Int> {
        val out = sortedMapOf<String, Int>()
        sql.query("SELECT tbl || ' ' || op, COUNT(*) FROM test_writes GROUP BY 1").use { c -> while (c.moveToNext()) out[c.getString(0)] = c.getInt(1) }
        return out
    }

    private fun clickByText(text: String): () -> Boolean = compose.onNodeWithText(text).fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
    private fun clickByLabel(label: String): () -> Boolean = compose.onNodeWithContentDescription(label).fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
    /** Both clicks run in one frame, before anything recomposes. */
    private fun twice(click: () -> Boolean) = compose.runOnUiThread { click(); click() }

    /** The editor at [route] over a home screen; returns once [ready] shows. */
    private fun openEditor(route: String, ready: String) {
        compose.setContent {
            KhonTheme {
                nav = rememberNavController()
                NavHost(nav, startDestination = "home") {
                    composable("home") { Text("Home") }
                    composable("program/{id}") { e -> ProgramEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("block/{programId}/{blockId}") { e -> BlockEditorScreen(vm, nav, e.arguments?.getString("programId") ?: "", e.arguments?.getString("blockId") ?: "") }
                    composable("exercise/{id}") { e -> ExerciseEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("modality/{id}") { e -> ModalityEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("routine/{id}") { e -> RoutineEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("stretcheditor/{id}") { e -> StretchEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("cardio/{id}") { e -> CardioScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                }
            }
        }
        compose.runOnIdle { nav.navigate(route) }
        until { shows(ready) }
        compose.waitForIdle()
        sql.execSQL("DELETE FROM test_writes")
    }

    /** After the exit: the home screen alone on the stack, and exactly [expected] row writes. */
    private fun leftOnceWith(expected: Map<String, Int>) {
        until { routes() == listOf("home") && shows("Home") }
        // Let any second write or pop that was let through land before checking.
        repeat(5) { compose.waitForIdle(); Thread.sleep(20) }
        assertEquals(listOf("home"), routes())
        until { writes() == expected }
        Thread.sleep(100)
        assertEquals(expected, writes())
    }

    private fun typeName(label: String, text: String) = compose.onNodeWithContentDescription(label).performTextInput(text)

    private fun enabled(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().firstOrNull()?.config?.getOrNull(SemanticsProperties.Disabled) == null

    /** Opens the confirm of the [action] row, then taps its [confirm] button (the sheet's, the last node) twice. */
    private fun confirmTwice(action: String, sheetTitle: String, confirm: String) {
        compose.onAllNodesWithText(action).onFirst().performScrollTo().performClick()
        until { shows(sheetTitle) }
        twice(compose.onAllNodesWithText(confirm).fetchSemanticsNodes().last().config[SemanticsActions.OnClick].action!!)
    }

    // ---- program editor

    @Test fun `program - Done`() { openEditor("program/p1", "Blocks"); typeName("Name", "2"); twice(clickByText("Done")); leftOnceWith(mapOf("programs UPDATE" to 1)) }
    @Test fun `program - Back`() { openEditor("program/p1", "Blocks"); twice(clickByLabel("Programs")); leftOnceWith(emptyMap()) }
    @Test fun `program - Delete`() {
        openEditor("program/p1", "Blocks")
        confirmTwice("Delete program", "Delete Push?", "Delete")
        leftOnceWith(mapOf("programs DELETE" to 1, "blocks DELETE" to 1, "block_exercises DELETE" to 1, "programs UPDATE" to 1))
    }

    // ---- block editor

    @Test fun `block - Done`() { openEditor("block/p1/k0", "Superset"); twice(clickByText("Done")); leftOnceWith(emptyMap()) }
    @Test fun `block - Back`() { openEditor("block/p1/k0", "Superset"); twice(clickByLabel("Program")); leftOnceWith(emptyMap()) }
    @Test fun `block - Delete`() {
        openEditor("block/p1/k0", "Superset")
        confirmTwice("Delete block", "Delete block 1?", "Delete")
        leftOnceWith(mapOf("blocks DELETE" to 1, "block_exercises DELETE" to 1))
    }

    // ---- exercise editor

    @Test fun `exercise - Done`() { openEditor("exercise/a", "Merge into…"); typeName("Name", "2"); twice(clickByText("Done")); leftOnceWith(mapOf("exercises UPDATE" to 1)) }
    @Test fun `exercise - Back`() { openEditor("exercise/a", "Merge into…"); twice(clickByLabel("Library")); leftOnceWith(emptyMap()) }
    @Test fun `exercise - Archive`() {
        openEditor("exercise/a", "Merge into…")
        compose.onNodeWithText("Archive").performScrollTo()
        twice(clickByText("Archive"))
        leftOnceWith(mapOf("exercises UPDATE" to 2)) // the page's fields, then the flag, once
    }
    @Test fun `exercise - Unarchive`() {
        openEditor("exercise/z", "Merge into…")
        compose.onNodeWithText("Unarchive").performScrollTo()
        twice(clickByText("Unarchive"))
        leftOnceWith(mapOf("exercises UPDATE" to 2))
    }
    @Test fun `exercise - Delete`() {
        openEditor("exercise/a", "Merge into…")
        until { enabled("Delete") }
        compose.waitForIdle()
        confirmTwice("Delete", "Delete Old Bench?", "Delete")
        leftOnceWith(mapOf("exercises DELETE" to 1))
    }
    @Test fun `exercise - Merge`() {
        openEditor("exercise/a", "Merge into…")
        compose.onNodeWithText("Merge into…").performScrollTo().performClick()
        until { shows("Bench Press") }
        compose.runOnUiThread { clickByText("Bench Press")() }
        until { shows("Merge Old Bench into Bench Press?") }
        twice(clickByText("Merge"))
        leftOnceWith(mapOf("exercises DELETE" to 1))
    }

    @Test fun `case 15 - Done and Back in one frame leave once, and only Done writes`() {
        openEditor("exercise/a", "Merge into…")
        typeName("Name", "2")
        val done = clickByText("Done")
        val back = clickByLabel("Library")
        compose.runOnUiThread { done(); back() }
        leftOnceWith(mapOf("exercises UPDATE" to 1))
    }

    @Test fun `case 16 - a refused Delete gives the guard back, and Done then leaves`() {
        openEditor("exercise/a", "Merge into…")
        until { enabled("Delete") }
        compose.onNodeWithText("Delete").performScrollTo().performClick()
        until { shows("Delete Old Bench?") }
        // A session starts to use the exercise after the confirm opened: the delete is refused when it runs.
        io { dao.upsertSetLogs(listOf(SetLog("l1", "s1", 0, false, 0, 0, 0, "a", "Old Bench", 6, 10, 2.5, 60.0, 8, SetStatus.DONE))) }
        compose.runOnUiThread { compose.onAllNodesWithText("Delete").fetchSemanticsNodes().last().config[SemanticsActions.OnClick].action!!() }
        until { shows("Used in a session, program, or plan: archive or merge it instead.") }
        assertEquals(listOf("home", "exercise/{id}"), routes())
        sql.execSQL("DELETE FROM test_writes")
        typeName("Name", "2")
        twice(clickByText("Done"))
        leftOnceWith(mapOf("exercises UPDATE" to 1))
    }

    // ---- modality editor

    @Test fun `modality - Done`() { openEditor("modality/m", "Color"); typeName("Name", "2"); twice(clickByText("Done")); leftOnceWith(mapOf("modalities UPDATE" to 1)) }
    @Test fun `modality - Back`() { openEditor("modality/m", "Color"); twice(clickByLabel("Modalities")); leftOnceWith(emptyMap()) }
    @Test fun `modality - Archive`() {
        openEditor("modality/m", "Color")
        compose.onNodeWithText("Archive").performScrollTo()
        twice(clickByText("Archive"))
        leftOnceWith(mapOf("modalities UPDATE" to 2))
    }
    @Test fun `modality - Unarchive`() {
        openEditor("modality/m2", "Color")
        compose.onNodeWithText("Unarchive").performScrollTo()
        twice(clickByText("Unarchive"))
        leftOnceWith(mapOf("modalities UPDATE" to 2))
    }

    // ---- routine editor

    @Test fun `routine - Done`() { openEditor("routine/ro", "Add stretch"); twice(clickByText("Done")); leftOnceWith(emptyMap()) }
    @Test fun `routine - Back`() { openEditor("routine/ro", "Add stretch"); twice(clickByLabel("Stretching")); leftOnceWith(emptyMap()) }
    @Test fun `routine - Delete`() {
        openEditor("routine/ro", "Add stretch")
        confirmTwice("Delete routine", "Delete this routine?", "Delete")
        leftOnceWith(mapOf("stretch_routines DELETE" to 1, "routine_stretches DELETE" to 1))
    }

    // ---- stretch editor

    @Test fun `stretch - Done`() { openEditor("stretcheditor/st", "Timing"); typeName("Name", "2"); twice(clickByText("Done")); leftOnceWith(mapOf("stretches UPDATE" to 1)) }
    @Test fun `stretch - Back`() { openEditor("stretcheditor/st", "Timing"); twice(clickByLabel("Stretches")); leftOnceWith(emptyMap()) }
    @Test fun `stretch - Delete`() {
        openEditor("stretcheditor/st2", "Timing")
        until { shows("Delete stretch") && enabled("Delete stretch") }
        confirmTwice("Delete stretch", "Delete this stretch?", "Delete")
        leftOnceWith(mapOf("stretches DELETE" to 1))
    }
    @Test fun `stretch - a refused Delete gives the guard back, and Done then leaves`() {
        openEditor("stretcheditor/st2", "Timing")
        until { shows("Delete stretch") && enabled("Delete stretch") }
        compose.onNodeWithText("Delete stretch").performScrollTo().performClick()
        until { shows("Delete this stretch?") }
        // A routine starts to use the stretch after the confirm opened: the delete is refused when it runs.
        io { dao.upsert(RoutineStretch("ro-2", "ro", "st2", 1)) }
        sql.execSQL("DELETE FROM test_writes")
        compose.runOnUiThread { compose.onAllNodesWithText("Delete").fetchSemanticsNodes().last().config[SemanticsActions.OnClick].action!!() }
        until { shows("In use by 1 routine rows.") }
        assertEquals(listOf("home", "stretcheditor/{id}"), routes())
        typeName("Name", "2")
        twice(clickByText("Done"))
        leftOnceWith(mapOf("stretches UPDATE" to 1))
    }

    // ---- cardio form

    @Test fun `cardio - Save`() { openEditor("cardio/c1", "Notes"); twice(clickByText("Save")); leftOnceWith(mapOf("sessions UPDATE" to 1)) }
    @Test fun `cardio - Discard`() {
        openEditor("cardio/c1", "Notes")
        confirmTwice("Discard", "Discard this session?", "Discard")
        leftOnceWith(mapOf("sessions DELETE" to 1))
    }
    @Test fun `cardio - Back`() { openEditor("cardio/c1", "Notes"); twice(clickByLabel("Today")); leftOnceWith(emptyMap()) }
}
