# NGT01-nights-screens report

Commits on branch `ngt01` (base 6289886, which holds 49f4992): b221cf02 (screens and tests), d3a59ccd (SPEC.md, DESIGN.md, NightsShotsTest, Review test wait), 06a2a7b2 (Review row text color for ContrastTest). No trunk merge: the lead's brief forbids merges.

Focused command: `timeout 420 android/gradlew -p android :app:testDebugUnitTest --tests '*NightsScreenTest' --tests '*NightsScreenRulesTest' --tests '*NightsShotsTest'` (with `ANDROID_HOME`, `JAVA_HOME` as in `Tests:`).

| # | Status | Check ran | Real output |
|---|---|---|---|
| U1 | done: Log line under the last row of each day, after the stretch lines; spoken form in the row's TalkBack text | NightsScreenTest `U1 the Log shows the night line ...`. Break: show the line under every row (`if (false) null else nightLogLine`) | break: `U1 ... AssertionError: expected:<1> but was:<2>`; revert: NightsScreenTest tests="21" failures="0" |
| U2 | done: no line for not read, no access, failed | NightsScreenTest `U2 ...` (3 tests); NightsShotsTest `logNotRead/logNoAccess/logFailed`; the same three pictures made on base 6289886 in a temporary worktree (removed) | all pass; `shasum`: base-log-hang, base-log-no_access, base-log-fail, log-hang, log-no_access, log-fail all `dc4ebb82d2cd644765b0f5587b97e03c68bca618` (byte-equal to base) |
| U3 | done: route `nights`, row "Nights" right under "Compare two periods" | NightsScreenTest `U3 the Nights row on Progress opens the Nights screen and Back returns` | tests="21" failures="0" |
| U4 | done: HealthIntro card of Heart rate data; title "Read sleep" and the spec text; request holds only `HealthSleepSource.SLEEP_READ`; `refresh(force = true)` after the answer and after Check again | NightsScreenTest `U4 ...`. Break: request `SLEEP_READ` and `HEART_READ` | break: `expected:<[[android.permission.health.READ_SLEEP]]> but was:<[[android.permission.health.READ_SLEEP, android.permission.health.READ_HE...` ; revert: 21 passed |
| U5 | done: figures and captions, dash, empty text, failed text with Try again (forced read) | NightsScreenTest `U5 ...` (4 tests); NightsScreenRulesTest `U5 the captions of the two figures` | 21 passed; NightsScreenRulesTest tests="4" failures="0" |
| U6 | done: 14 bars on one color `K.Night`, axis to the next 2 h (8 h at least), guides every 4 h, weekday letters (today in accent), up to two dots, one TalkBack element per day | NightsScreenTest `U6 ...` (2 tests), NightsScreenRulesTest `U6 ...` (2 tests), NightsShotsTest `loaded10/13` (pixel check: every bar's color is `K.Night`). Breaks: dots `done.forEach` (no limit); axis without the ceiling | break dots: `U6 one bar per night ... expected:<2> but was:<3>`; break axis: `U6 the axis ... expected:<10> but was:<8>` and `U6 a night of 9 h 10 m ... expected:<121.0> but was:<132.0>`; revert: all pass |
| U7 | done: list in the data rule's order, "4 nights", "1 night"; left out with no row | NightsScreenTest `U7 ...` (2 tests); NightsScreenRulesTest `U7 the count of nights` | pass |
| U8 | done: "Night before" and "Asleep" group after the heart picture, before the exercises or the cardio rows; none without a night | NightsScreenTest `U8 ...` (3 tests). Break: move the group below the exercises and cardio rows | break: gym test fails at NightsScreenTest.kt:293, cardio test at :309 (2 of 3 failed, as expected); revert: 21 passed |
| U9 | done: nights passed to `periodStats` for both periods; values use the cell color `K.Text` | NightsScreenTest `U9 ...` (2 tests) | pass; spoken row holds "8 h 35 m" and "6 h 40 m", count "2 in" and "1 in" |
| U10 | done: 360 dp at font 1.0 and 1.3 with "Night 10 h 05 m · 23:10 – 09:15" and the 28-letter title "Upper body strength and core" | NightsShotsTest checks every text node with a layout (no cut line, no ellipsis, no line wider than its box) on Nights loaded, no access, empty, and the Log week; pictures opened by eye for the merged texts | 13 passed. Seen: the Log line wraps at 1.3 ("23:10 –" / "06:15"), the long title wraps to 2 lines, the caption "average · 11 of 14 nights" wraps; nothing cut or overlapping |
| U11 | done: no verdict word, one bar color, no advice, no network call | `git diff 49f4992 -- android/app/src/main \| grep "^+" \| grep -i -E "http\|url\|socket\|network\|INTERNET\|okhttp\|retrofit"`; same with `good\|poor\|bad\|should\|target\|score...` | network grep: one line, the Play Store intent of the shared HealthIntro (`onStore = { url -> ... ACTION_VIEW ...}`), as in Heart rate data; verdict grep: no match |
| U12 | blocked: the full `Tests:` command hung 3 times on the final commit 06a2a7b2 (the known test worker hang) | Run on d3a59ccd: full `Tests:` command. Runs on 06a2a7b2: full command 3 times under `timeout 560`; then `:app:assembleDebug :app:lintVitalRelease` alone; `ContrastTest` and `NightsScreenTest.U8*` after the fix | d3a59ccd: `BUILD FAILED in 1m 57s`, 131 result files, tests=1274 skipped=69 failures=1 (ContrastTest `ReviewScreen.kt:159 color: cannot resolve color`), fixed in 06a2a7b2 and that class passes. 06a2a7b2: each full run exit=124, my "Gradle Test Executor" at 100 % CPU after 3 result files, killed with `kill -9` (the fig05 worker was left alone). assembleDebug + lintVitalRelease: `BUILD SUCCESSFUL in 21s` |

## Pictures

Folder: `android/app/build/shots-nights/` (create it before `NightsShotsTest`). All opened.

- Nights loaded: `nights-loaded-1_0.png`, `nights-loaded-1_3.png`
- Nights no access: `nights-noaccess-1_0.png`, `nights-noaccess-1_3.png`
- Nights empty: `nights-empty-1_0.png`, `nights-empty-1_3.png`
- Log week: `log-week-1_0.png`, `log-week-1_3.png`
- Review with the rows: `review-gym-1_0.png`
- Compare with the group: `compare-1_0.png`
- U2: `log-hang.png`, `log-no_access.png`, `log-fail.png`, and from base `base-log-hang.png`, `base-log-no_access.png`, `base-log-fail.png`

## Local decisions

- Bar color: new token `K.Night` = #5E5CE6 (systemIndigo, the mockup's `--n`). It is not text, so no contrast pair.
- No night in the 14 days: captions "average · 0 of 14 nights" and "shortest".
- Guide lines at 0, 4, 8 (12 …) hours, labelled "0 h" too, as LineChart labels its lowest line. No line at a top of 10 h.
- A bar's spoken text lists the session titles on a day with no night too ("Friday, November 20, no night data, GYM").
- Review: "Asleep" value muted, as in the mockup.
- Section 5: the rationale intent only opens MainActivity, with no list, and README.md has no list. Only SPEC.md got "sleep sessions" (Stack section).
- Defects in the data rules: none found.
