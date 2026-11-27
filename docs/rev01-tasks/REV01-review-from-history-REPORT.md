# REV01-review-from-history: report

Branch `rev01`, base 566b0b4. Commits: `0a5f014` Open Review from a finished session opened in History.

## Contract

| # | Status | Check ran | Real output |
|---|---|---|---|
| V1 | done: `ReviewEntry` (GroupedList + ListRow with chevron) is item 0 of the finished gym session's LazyColumn when `s.finished && !editing`; open-block scroll index shifts by one. | `ReviewFromHistoryTest`: "a finished gym session has the Review row first…", "an unfinished gym session and an unfinished cardio session have no Review row" | both pass; the row's bottom is above every block node; Edit removes it, Done brings it back; unfinished: 0 rows |
| V2 | done: value = `hrLine(s, hrFacts(...))`, the same call Review makes; `secondary = null` when it is null. | "…with Review's heart rate line…", "a waiting read and no read give the row no value…" | pass; visible text equals `hrLine` of the fixture; WAITING row and no row: merged text `["Review"]` only |
| V3 | done: cardio row in its own group above the form while `s.finished && draft == opened && edits.isEmpty()`. | "a finished cardio session has the row only while nothing is typed" | pass: row; typed Note hides it; Save pops to `[today, history]`; reopened saved session shows it; typed "Round 1 heart rate" hides it; unfinished: no row |
| V4 | done: the tap runs `leave.leave { nav.navigate(Routes.review(id)) }` (navigates only while the screen is on top). | gym and cardio tests: two clicks in one frame (`twice(clickRow())`) | pass: back stack `[today, history, session/{id}, review/{id}]` and `[today, history, cardio/{id}, review/{id}]`, one Review |
| V5 | done: Review's back label is `reviewBack(previous route)`; Review's bar Back and system Back go through `rememberExitGuard`. | "the row opens Review once, Back says History…", "the cardio row opens Review once…", "Review from Today says Today" | pass: label "History"/"Today"; bar Back tapped twice in one frame pops one screen; system Back returns; open block (Block 2 Expanded) and block tops equal before/after (scrolled 20 px); cardio "Round 1" top equal; the next Back leaves for History |
| V6 | done: `LeaveIfSessionMissing` now pops to the tab under the screen (was: always Today). | "a session deleted under Review takes Review and its session screen away to History" | pass: `[today, history]` for gym and cardio. Same test with base `KhonNav.kt`: `AssertionError: never: History shows; routes [today]` |
| V7 | done: SessionScreen collects `vm.repo.sessionHr(id)` (CardioScreen already did). | "…a fill that commits while the screen is open shows" | pass: row with no line, then upsert of the filled row; the row speaks and shows the line |
| V8 | deviation: the row stacks the value under the title when the two do not fit on one line. At 360 dp and font 1.3, "avg 188 · max 201 bpm" broke the title into "Revie / w" (seen in the first screenshot). Fix: `ListRow` got an optional `stacked` parameter (null keeps the old font-1.5 rule); `ReviewEntry` measures both texts and sets it. | `ReviewEntryFitTest` (360/411 × 1.0/1.3, native graphics), `TapTargetTest` "a finished session's screens with their Review row have 48 dp targets", `ContrastTest` (static scan, Muted on Surface is a listed pair), speech asserts in `ReviewFromHistoryTest` | 4/4 pass: 1 line each, no ellipsis, no overlap; tap targets pass; spoken "Review, average 119, maximum 164 beats per minute" with role Button, "Review" with no line |
| V9 | done: no existing assertion changed; one test method added to `TapTargetTest`. `comment-guard --base 566b0b4` exit 0. SPEC.md (Navigation) and DESIGN.md (Today/Review) updated. | Tests command below; `~/.config/agents/tools/comment-guard --base 566b0b4` | `BUILD SUCCESSFUL in 2m 11s`; 111 result files, 1074 tests, 0 failures, 0 errors, 51 skipped (shots tests whose folders do not exist); guard=0 |

## Tests

Command (on 0a5f014): `JAVA_HOME=".../jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease`

```
> Task :app:testDebugUnitTest
> Task :app:assembleDebug
> Task :app:lintVitalRelease
BUILD SUCCESSFUL in 2m 11s
71 actionable tasks: 27 executed, 22 from cache, 22 up-to-date
files 111 tests 1074 failures 0 errors 0 skipped 51
```

Base check: with `android/app/src/main` at 566b0b4 the new tests do not compile
(`Unresolved reference 'ReviewEntry'`, `'reviewEntrySpoken'`, `'reviewBack'`). The new TapTargetTest method waits for a "Review" row that base does not have.

## Screenshots

`ReviewRowShotsTest` (runs when `android/app/build/shots-review-row` exists). I looked at all 16:
`android/app/build/shots-review-row/review-row-{gym,long,plain,cardio}-{360,411}dp-x{1_0,1_3}.png`.
gym = filled read (avg 119 · max 164), long = avg 188 · max 201, plain = no read, cardio = Norwegian 4×4 (avg 141 · max 168).
Only long and gym at 360/1.3 and cardio at 360/1.3 stack the value; all other rows show one line.

## Deviations

- deviation: V6 needed a code change. The old `LeaveIfSessionMissing` popped to Today, so a delete under Review showed Today. It now pops to the tab under the screen. A screen opened from History whose session goes away now lands on History, not Today.
- deviation: SessionScreen and CardioScreen draw only after their sets and rounds load. Without this the saved scroll place was clamped to 0 after Review (test showed every block 20 px lower).
- deviation: Review now has an ExitGuard, so a double tap on its Back pops one screen, not Review and the session screen.
- deviation: `ListRow` has a new optional `stacked` parameter (V8). Other callers do not pass it and keep their layout.
- deviation: trunk not merged before the final tests; the lead's brief forbids a merge.
- Known edge, not fixed: a system Back in the same frame as the Review tap (before Review draws) is taken by the session screen's guard and does nothing.
