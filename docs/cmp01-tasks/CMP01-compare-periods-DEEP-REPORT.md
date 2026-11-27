# CMP01 deep review, round R1

**Verdict. Merge after fixes.** Head `d9fb265d69995a14d2233359b3caf80def089503`. Base `9d1ba17`.

## Coverage

| Scope | Evidence |
|---|---|
| C1–C5 | **read**, traced. Inclusive endpoints, finished sessions, preset boundaries, totals, rates, and mean. The preset arithmetic handles January, leap days, and month ends. [CompareRules.kt:18](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareRules.kt:18), [CompareRules.kt:111](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareRules.kt:111) |
| C6–C8 | **read**, traced. Checked set weights, missing exercise names, cardio pairing for pace, and both old wrappers. See F3, F4, and F6. [CompareRules.kt:139](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareRules.kt:139), [CompareRules.kt:200](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareRules.kt:200), [HistoryRules.kt:338](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryRules.kt:338) |
| C9–C11 | **read**, traced. Checked the entry, sheet, custom dates, empty groups, and the saved keys. Deletion leaves the session rows and the selected range resolves without a crash in source. The test restores one preset through Compose. See F1. [HistoryProgress.kt:150](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryProgress.kt:150), [CompareScreen.kt:60](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareScreen.kt:60), [CompareScreenTest.kt:173](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareScreenTest.kt:173), [Dao.kt:147](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:147) |
| C12 | **read**, traced. Checked column sizing, labels, and tap targets. The build report lists four visual checks. See F2. [CompareScreen.kt:134](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareScreen.kt:134), [CompareShotsTest.kt:157](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareShotsTest.kt:157), [TapTargetTest.kt:194](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/TapTargetTest.kt:194) |
| C13 and definition of done | **read**, traced. The diff adds a route, UI, rules, tests, and docs. The two existing DateRow calls still use the default label. Changes to old Progress behavior and comment issues are below. [Common.kt:155](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/Common.kt:155), [CardioScreen.kt:74](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CardioScreen.kt:74), [SessionScreen.kt:244](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/SessionScreen.kt:244) |

## Findings

- **F1 P2. Records inside the selected period go missing.** **read**. Compare loads History's data after History drops every record dated after today. A saved program range ends early or vanishes. Custom dates accept those days but omit gym, cardio, stretch, bodyweight, and related set data. Use the selected period as the date filter for Compare. Keep History's filter for its original callers. [CompareScreen.kt:60](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareScreen.kt:60), [CompareScreen.kt:74](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareScreen.kt:74), [CompareScreen.kt:81](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareScreen.kt:81), [CompareScreen.kt:199](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareScreen.kt:199), [HistoryRules.kt:30](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryRules.kt:30), [CompareRules.kt:45](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareRules.kt:45)
- **F2 P2. Compare rows use unequal space for values.** **read**. On both period rows, a long program name can use the available width before the date range gets measured. Give the date range space before the program name. The stacked facts also take 12 dp from the right cell alone. Put the gap between equal-width cells. [CompareScreen.kt:91](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareScreen.kt:91), [Components.kt:170](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/components/Components.kt:170), [CompareScreen.kt:162](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareScreen.kt:162)
- **F3 P3. Weekly muscle results change for tied rows and repeated session IDs.** **read**. The base inserted muscles in log order. The refactor inserts this week's muscles first. Equal totals for chest and forearms can switch order. The base also used the last finished snapshot of each ID. The refactor can count one ID in both weeks. Keep base session selection and map insertion order when sharing the counting rule. [Base diff](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/diff.patch), [HistoryScreen.kt:98](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryScreen.kt:98), [CompareRules.kt:140](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareRules.kt:140)
- **F4 P3. The old cardio range throws for a negative lookback.** **read**. The base kept each historical type with empty sessions and zero totals. A reversed Period now throws before grouping. Keep the old lower-bound test in the wrapper and share the existing type aggregation. The current screen uses the default lookback. [Base diff](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/diff.patch), [HistoryRules.kt:338](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryRules.kt:338), [HistoryProgress.kt:140](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryProgress.kt:140)
- **F5 P3. Comments repeat code and tests.** **read**. The private row and sheet descriptions, the layout summary, and the test summaries repeat named code or tested behavior. Remove the repeated descriptions and keep the text-wrapping explanation. Use `--no-witness F5="Comment wording has no runtime result"`. [CompareScreen.kt:127](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareScreen.kt:127), [CompareScreen.kt:181](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareScreen.kt:181), [CompareScreen.kt:210](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareScreen.kt:210), [CompareScreenTest.kt:47](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareScreenTest.kt:47), [CompareShotsTest.kt:76](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareShotsTest.kt:76), [CompareShotsTest.kt:95](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareShotsTest.kt:95)
- **F6 P3. The C8 test misses per-type seconds.** **read**. C3 checks all cardio seconds, and C8 checks rows and pace. Returning zero seconds for every Compare cardio type would pass both. Assert type totals for a run with time and distance plus a run with time only. Check that pace still uses the paired run alone. Use `--no-witness F6="Missing assertion has no failing product output"`. [CompareRulesTest.kt:68](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareRulesTest.kt:68), [CompareRulesTest.kt:145](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareRulesTest.kt:145), [HistoryRules.kt:348](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryRules.kt:348)

## Checks

- **ran** `witness check F1.json F2.json F3.json F4.json` in the witness directory by absolute path. Exit 0.
- **ran** `witness run <F1–F4>.json --sha d9fb265d69995a14d2233359b3caf80def089503` for each ID, using absolute paths. Each returned `undecided`, `receipt=none`, and exit 3. Creating the receipt directory failed with `Operation not permitted`. Gradle did not start. F1–F4 need the lead's witness run before recording them as found.
- **read**. The builder reports the full test command on `2cd6c42`. I have no test output from `d9fb265`. [Build report](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R1/inputs/1-CMP01-compare-periods-REPORT.md:27)
- **ran**. Compared the diff input with the app and specification section of the full snapshot diff. Output `App and specification diff match: True`. The full diff also adds the task and builder's report.

## Witness fallback

The files are also in [the witness directory](/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/CMP01-compare-periods-W). The complete JSON and patch text follows for the lead's run.

### F1

`F1.json`

```json
{
  "id": "w-CMP01-compare-periods-F1",
  "finding": "F1",
  "repo": "/Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/cmp01",
  "sha": "d9fb265d69995a14d2233359b3caf80def089503",
  "steps": [
    "Save a program with finished sessions on November 20 and November 28.",
    "Open Compare on November 27 and select that program in the right column.",
    "Set the right custom period's To to November 28. Check the gym, cardio, stretch and bodyweight facts."
  ],
  "expected": "The program spans November 20 through November 28 and has two gym sessions. Records on November 28 count in a custom period that includes that day.",
  "actual": "The screen ends the program on November 20 and counts one gym session. A custom range can include November 28, but the screen drops all records from that day.",
  "cmd": [
    "env",
    "JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home",
    "android/gradlew",
    "-p",
    "android",
    ":app:testDebugUnitTest",
    "--tests",
    "dev.mirzohidkhon.khonfitness.ui.screens.CompareProgramEndpointWitness"
  ],
  "test_patch": "/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/CMP01-compare-periods-W/F1.patch"
}
```

`F1.patch`

```diff
diff --git a/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/tests/CompareProgramEndpointWitness.kt b/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/tests/CompareProgramEndpointWitness.kt
new file mode 100644
--- /dev/null
+++ b/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/tests/CompareProgramEndpointWitness.kt
@@ -0,0 +1,125 @@
+package dev.mirzohidkhon.khonfitness.ui.screens
+
+import android.app.Application
+import android.content.Context
+import android.content.ContextWrapper
+import androidx.compose.ui.semantics.SemanticsActions
+import androidx.compose.ui.test.hasContentDescription
+import androidx.compose.ui.test.hasText
+import androidx.compose.ui.test.performScrollTo
+import androidx.compose.ui.test.junit4.createComposeRule
+import androidx.compose.ui.test.onAllNodesWithContentDescription
+import androidx.compose.ui.test.onAllNodesWithText
+import androidx.compose.ui.test.onNodeWithText
+import androidx.compose.ui.test.performSemanticsAction
+import androidx.lifecycle.viewModelScope
+import androidx.navigation.compose.rememberNavController
+import androidx.test.core.app.ApplicationProvider
+import dev.mirzohidkhon.khonfitness.KhonApp
+import dev.mirzohidkhon.khonfitness.data.Bodyweight
+import dev.mirzohidkhon.khonfitness.data.ItemType
+import dev.mirzohidkhon.khonfitness.data.Program
+import dev.mirzohidkhon.khonfitness.data.Session
+import dev.mirzohidkhon.khonfitness.data.StretchSession
+import dev.mirzohidkhon.khonfitness.ui.AppViewModel
+import dev.mirzohidkhon.khonfitness.ui.screens.CompareScreen
+import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
+import kotlinx.coroutines.Dispatchers
+import kotlinx.coroutines.cancel
+import kotlinx.coroutines.runBlocking
+import org.junit.After
+import org.junit.Before
+import org.junit.Rule
+import org.junit.Test
+import org.junit.runner.RunWith
+import org.robolectric.RobolectricTestRunner
+import org.robolectric.annotation.Config
+import java.time.Clock
+import java.time.LocalDateTime
+import java.time.ZoneId
+
+@RunWith(RobolectricTestRunner::class)
+@Config(application = Application::class)
+class CompareProgramEndpointWitness {
+    @get:Rule val compose = createComposeRule()
+    private lateinit var app: KhonApp
+    private lateinit var vm: AppViewModel
+    private val clock = ZoneId.systemDefault().let { zone ->
+        Clock.fixed(LocalDateTime.of(2026, 11, 27, 10, 0).atZone(zone).toInstant(), zone)
+    }
+
+    @Before fun open() {
+        app = KhonApp()
+        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java)
+            .apply { isAccessible = true }
+            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
+        vm = AppViewModel(app)
+    }
+
+    @After fun close() {
+        vm.viewModelScope.cancel()
+        app.scope.cancel()
+        app.db.close()
+    }
+
+    private fun session(id: String, date: String) =
+        Session(id, date, 1L, 2L, ItemType.PROGRAM, programId = "push")
+
+    private fun show() = compose.setContent {
+        KhonTheme { CompareScreen(vm, rememberNavController(), clock) }
+    }
+
+    private fun shows(text: String) =
+        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
+
+    private fun tap(text: String) =
+        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.OnClick)
+
+    @Test fun comparesAllFinishedSessionsOfTheProgram() {
+        runBlocking(Dispatchers.IO) {
+            val dao = app.db.dao()
+            dao.upsert(Program("push", "Push day"))
+            dao.upsertSessions(listOf(session("a", "2026-11-20"), session("b", "2026-11-28")))
+        }
+        show()
+        tap("Oct 31 – Nov 27")
+        compose.waitUntil(5_000) { shows("Push day") }
+        tap("Push day")
+        compose.waitUntil(5_000) { !shows("Right column") }
+        compose.onAllNodesWithContentDescription(
+            "Gym sessions, 0 in Oct 3 – Oct 30, 2 in Nov 20 – Nov 28"
+        )[0].fetchSemanticsNode()
+    }
+
+    @Test fun aCustomPeriodCountsItsLastDayAfterToday() {
+        runBlocking(Dispatchers.IO) {
+            val dao = app.db.dao()
+            dao.upsert(session("future", "2026-11-28"))
+            dao.upsert(Session("run", "2026-11-28", 1L, 2L, ItemType.CARDIO, timeSec = 300))
+            dao.upsert(Bodyweight("2026-11-28", 82.0))
+            dao.upsertStretchSessions(listOf(StretchSession("st", "daily", "Daily", "2026-11-28", 1L, 600, 9, 0)))
+        }
+        show()
+        tap("Oct 31 – Nov 27")
+        tap("Custom dates")
+        compose.onNodeWithText("To").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
+        val day = hasText("November 28", substring = true) or
+            hasContentDescription("November 28", substring = true)
+        compose.waitUntil(5_000) { compose.onAllNodes(day).fetchSemanticsNodes().isNotEmpty() }
+        compose.onAllNodes(day)[0].performSemanticsAction(SemanticsActions.OnClick)
+        tap("Done")
+        compose.waitUntil(5_000) { shows("Oct 31 – Nov 28") }
+        for (expected in listOf(
+            "Gym sessions, 0",
+            "Cardio sessions, 0",
+            "Stretching, 0"
+        )) {
+            compose.onAllNodesWithContentDescription(
+                "$expected in Oct 3 – Oct 30, 1 in Oct 31 – Nov 28"
+            )[0].fetchSemanticsNode()
+        }
+        compose.onAllNodesWithContentDescription(
+            "Average, none in Oct 3 – Oct 30, 82.0 kg in Oct 31 – Nov 28"
+        )[0].fetchSemanticsNode()
+    }
+}
```

### F2

`F2.json`

```json
{
  "id": "w-CMP01-compare-periods-F2",
  "finding": "F2",
  "repo": "/Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/cmp01",
  "sha": "d9fb265d69995a14d2233359b3caf80def089503",
  "steps": [
    "Open Compare at 360 dp and font scale 1.3 with a long program name.",
    "Select the program in the right column and inspect its date range layout."
  ],
  "expected": "The date range is fully visible on the period row.",
  "actual": "The program name uses the row width. The range cannot display its full text.",
  "cmd": [
    "env",
    "JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home",
    "android/gradlew",
    "-p",
    "android",
    ":app:testDebugUnitTest",
    "--tests",
    "dev.mirzohidkhon.khonfitness.ui.screens.CompareRangeLayoutWitness"
  ],
  "test_patch": "/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/CMP01-compare-periods-W/F2.patch"
}
```

`F2.patch`

```diff
diff --git a/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/tests/CompareRangeLayoutWitness.kt b/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/tests/CompareRangeLayoutWitness.kt
new file mode 100644
--- /dev/null
+++ b/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/tests/CompareRangeLayoutWitness.kt
@@ -0,0 +1,93 @@
+package dev.mirzohidkhon.khonfitness.ui.screens
+
+import android.app.Application
+import android.content.Context
+import android.content.ContextWrapper
+import androidx.compose.ui.semantics.SemanticsActions
+import androidx.compose.ui.test.junit4.createComposeRule
+import androidx.compose.ui.test.onAllNodesWithText
+import androidx.compose.ui.test.onNodeWithText
+import androidx.compose.ui.test.performSemanticsAction
+import androidx.lifecycle.viewModelScope
+import androidx.navigation.compose.rememberNavController
+import androidx.test.core.app.ApplicationProvider
+import dev.mirzohidkhon.khonfitness.KhonApp
+import dev.mirzohidkhon.khonfitness.data.ItemType
+import dev.mirzohidkhon.khonfitness.data.Program
+import dev.mirzohidkhon.khonfitness.data.Session
+import dev.mirzohidkhon.khonfitness.ui.AppViewModel
+import dev.mirzohidkhon.khonfitness.ui.screens.CompareScreen
+import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
+import kotlinx.coroutines.Dispatchers
+import kotlinx.coroutines.cancel
+import kotlinx.coroutines.runBlocking
+import org.junit.After
+import org.junit.Before
+import org.junit.Rule
+import org.junit.Test
+import org.junit.runner.RunWith
+import org.robolectric.RobolectricTestRunner
+import org.robolectric.annotation.Config
+import java.time.Clock
+import java.time.LocalDateTime
+import java.time.ZoneId
+import androidx.compose.ui.text.TextLayoutResult
+import org.junit.Assert.assertFalse
+import org.junit.Assert.assertTrue
+
+@RunWith(RobolectricTestRunner::class)
+@Config(application = Application::class, qualifiers = "w360dp-h800dp-xhdpi", fontScale = 1.3f)
+class CompareRangeLayoutWitness {
+    @get:Rule val compose = createComposeRule()
+    private lateinit var app: KhonApp
+    private lateinit var vm: AppViewModel
+    private val clock = ZoneId.systemDefault().let { zone ->
+        Clock.fixed(LocalDateTime.of(2026, 11, 27, 10, 0).atZone(zone).toInstant(), zone)
+    }
+
+    @Before fun open() {
+        app = KhonApp()
+        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java)
+            .apply { isAccessible = true }
+            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
+        vm = AppViewModel(app)
+    }
+
+    @After fun close() {
+        vm.viewModelScope.cancel()
+        app.scope.cancel()
+        app.db.close()
+    }
+
+    private fun session(id: String, date: String) =
+        Session(id, date, 1L, 2L, ItemType.PROGRAM, programId = "push")
+
+    private fun show() = compose.setContent {
+        KhonTheme { CompareScreen(vm, rememberNavController(), clock) }
+    }
+
+    private fun shows(text: String) =
+        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
+
+    private fun tap(text: String) =
+        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.OnClick)
+
+    @Test fun aLongProgramNameLeavesRoomForTheDateRange() {
+        val name = "Long strength program for shoulders and the upper back with extra exercises"
+        runBlocking(Dispatchers.IO) {
+            val dao = app.db.dao()
+            dao.upsert(Program("push", name))
+            dao.upsertSessions(listOf(session("a", "2025-12-20"), session("b", "2026-11-20")))
+        }
+        show()
+        tap("Oct 31 – Nov 27")
+        compose.waitUntil(5_000) { shows(name) }
+        tap(name)
+        compose.waitUntil(5_000) { !shows("Right column") }
+        val results = mutableListOf<TextLayoutResult>()
+        compose.onNodeWithText("Dec 20, 2025 – Nov 20", useUnmergedTree = true)
+            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
+        assertTrue(results.single().size.width > 0)
+        assertFalse(results.single().hasVisualOverflow)
+    }
+}
```

### F3

`F3.json`

```json
{
  "id": "w-CMP01-compare-periods-F3",
  "finding": "F3",
  "repo": "/Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/cmp01",
  "sha": "d9fb265d69995a14d2233359b3caf80def089503",
  "steps": [
    "Pass one last-week chest set before one current-week forearms set.",
    "Pass two finished snapshots with the same session ID, last-week then current-week."
  ],
  "expected": "The tied order remains chest then forearms. Only the last snapshot contributes.",
  "actual": "Forearms appears first. The repeated session contributes in both weeks.",
  "cmd": [
    "env",
    "JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home",
    "android/gradlew",
    "-p",
    "android",
    ":app:testDebugUnitTest",
    "--tests",
    "dev.mirzohidkhon.khonfitness.ui.screens.MuscleVolumeCompatibilityWitness"
  ],
  "test_patch": "/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/CMP01-compare-periods-W/F3.patch"
}
```

`F3.patch`

```diff
diff --git a/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/tests/MuscleVolumeCompatibilityWitness.kt b/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/tests/MuscleVolumeCompatibilityWitness.kt
new file mode 100644
--- /dev/null
+++ b/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/tests/MuscleVolumeCompatibilityWitness.kt
@@ -0,0 +1,37 @@
+package dev.mirzohidkhon.khonfitness.ui.screens
+
+import dev.mirzohidkhon.khonfitness.data.Exercise
+import dev.mirzohidkhon.khonfitness.data.ItemType
+import dev.mirzohidkhon.khonfitness.data.Kind
+import dev.mirzohidkhon.khonfitness.data.Session
+import dev.mirzohidkhon.khonfitness.data.SetLog
+import dev.mirzohidkhon.khonfitness.data.SetStatus
+import org.junit.Assert.assertEquals
+import org.junit.Test
+import java.time.LocalDate
+
+class MuscleVolumeCompatibilityWitness {
+    private val today = LocalDate.of(2026, 11, 27)
+    private val exercises = listOf(
+        Exercise("press", "Press", Kind.STRENGTH, primaryMuscle = "chest"),
+        Exercise("curl", "Curl", Kind.STRENGTH, primaryMuscle = "forearms")
+    )
+
+    private fun session(id: String, date: String) = Session(id, date, 1L, 2L, ItemType.PROGRAM)
+
+    private fun set(session: String, exercise: String) =
+        SetLog(session, session, 0, false, 0, 0, 0, exercise, exercise, 8, 12, 2.5,
+            reps = 8, status = SetStatus.DONE)
+
+    @Test fun tiedMuscleRowsKeepTheBaseOrder() {
+        val sessions = listOf(session("last", "2026-11-20"), session("now", "2026-11-27"))
+        val logs = listOf(set("last", "press"), set("now", "curl"))
+        assertEquals(listOf("chest", "forearms"), muscleVolume(logs, sessions, exercises, today).map { it.muscle })
+    }
+
+    @Test fun theLastFinishedSnapshotOfEachSessionWins() {
+        val sessions = listOf(session("same", "2026-11-20"), session("same", "2026-11-27"))
+        assertEquals(listOf(MuscleVolume("chest", 1.0, 0.0)),
+            muscleVolume(listOf(set("same", "press")), sessions, exercises, today))
+    }
+}
```

### F4

`F4.json`

```json
{
  "id": "w-CMP01-compare-periods-F4",
  "finding": "F4",
  "repo": "/Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/cmp01",
  "sha": "d9fb265d69995a14d2233359b3caf80def089503",
  "steps": [
    "Call cardioTypes with one finished run and days = -1."
  ],
  "expected": "The historical run type remains with an empty period, zero totals and no pace.",
  "actual": "Period rejects the reversed dates with IllegalArgumentException.",
  "cmd": [
    "env",
    "JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home",
    "android/gradlew",
    "-p",
    "android",
    ":app:testDebugUnitTest",
    "--tests",
    "dev.mirzohidkhon.khonfitness.ui.screens.CardioWindowCompatibilityWitness"
  ],
  "test_patch": "/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/CMP01-compare-periods-W/F4.patch"
}
```

`F4.patch`

```diff
diff --git a/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/tests/CardioWindowCompatibilityWitness.kt b/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/tests/CardioWindowCompatibilityWitness.kt
new file mode 100644
--- /dev/null
+++ b/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/tests/CardioWindowCompatibilityWitness.kt
@@ -0,0 +1,20 @@
+package dev.mirzohidkhon.khonfitness.ui.screens
+
+import dev.mirzohidkhon.khonfitness.data.Exercise
+import dev.mirzohidkhon.khonfitness.data.ItemType
+import dev.mirzohidkhon.khonfitness.data.Kind
+import dev.mirzohidkhon.khonfitness.data.Session
+import org.junit.Assert.assertEquals
+import org.junit.Test
+import java.time.LocalDate
+
+class CardioWindowCompatibilityWitness {
+    @Test fun aReversedWindowKeepsTheHistoricalTypes() {
+        val today = LocalDate.of(2026, 11, 27)
+        val run = Session(today.toString(), today.toString(), 1L, 2L, ItemType.CARDIO,
+            exerciseId = "run", timeSec = 300, distanceM = 1000)
+        val exercises = listOf(Exercise("run", "Run", Kind.CARDIO, modalityId = "run"))
+        assertEquals(listOf(CardioType("run", emptyList(), 0, 0, null)),
+            cardioTypes(listOf(run), exercises, today, days = -1))
+    }
+}
```
