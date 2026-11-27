# NGT01-nights-screens: show the nights in the Log, on a Nights screen, on Review, and in Compare

Task-ID: NGT01-nights-screens
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/ngt01
Base: 49f4992
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/ngt01/docs/ngt01-tasks/NGT01-nights-screens-REPORT.md

## Why

The owner approved "nights next to training" in three places. He chose
them from the picture
`/Users/mirzohidkhon/deepvision/personal/khon-fitness/docs/tasks/opt14-15/idea14-options.png`
(source: `mockup.html` in the same folder). Open the picture first: it
is the look to build, with the app's own components and tokens.

The data layer is done on this branch. Read
`docs/ngt01-tasks/NGT01-nights-data.md` (the rules and texts) and
`docs/ngt01-tasks/NGT01-nights-data-REPORT.md` (the public names to use:
the store, its states, the permission constant, the rule and text
functions). Use them; do not write a second copy of a rule or a text.

A lead decision on the data report's open row D7: a whole hour is spoken
"7 hours" and written "7 h 00 m". It stays.

The owner's hard rules:

- All data stays on the phone. No network call.
- Facts only. No score, no verdict word, no color that judges a night
  (one color for every bar and every night value), no advice, no target,
  no notification, no badge.
- The app never tells the owner what to do.

## Scope

### 1. The Log line (History, Log tab)

Each day of the shown week gets the Log line of the data rules as its
last muted line, below its stretch lines. A day with two sessions shows
it once, under the last row. A rest day shows it too. Style: the small
muted style of the stretch lines; the mockup's violet `#B4B2FF` is not
needed, use the muted text color. The line joins the row's TalkBack text
with its spoken form. No line while the nights are not loaded or there
is no access: the Log must look as it does today.

History calls `refresh()` of the store when it resumes.

### 2. The Nights screen

A new route `nights`. A row "Nights" on the Progress tab opens it, next
to "Compare two periods" and "Load". The row always shows.

The screen, top to bottom (top bar as the Load screen, title "Nights"):

- **No access.** Health Connect not installed, needs an update, or the
  sleep permission is not granted: the same block and buttons as the
  Heart data screen uses for those three states. The text for "not
  granted": "Khon Fitness reads your sleep sessions from Health Connect.
  It only reads them." The button asks for the sleep permission only.
  After the answer the screen calls `refresh(force = true)`.
- **Failed.** "Could not read Health Connect" and a "Try again" button.
- **Loaded, no night at all.** "No night data. Health Connect has no
  sleep sessions."
- **Loaded.**
  - Two figures side by side: the average of the last 14 nights with the
    caption "average · last 14 nights" (when fewer have data:
    "average · 11 of 14 nights"), and the shortest with "shortest ·
    Nov 3". With no night in the 14 days: a dash and the captions.
  - A bar chart of the 14 days that end today, oldest left. One bar per
    night, height by asleep time. The value axis starts at 0 and ends at
    the next multiple of 2 hours at or above the longest night, at least
    8 hours; guide lines and labels every 4 hours ("4 h", "8 h"), drawn
    as the other charts of the app draw theirs. A day with no night has
    no bar. Under each bar: the weekday letter (today as the other week
    rows mark today), and under it one dot per finished session of that
    day in the color of the session (`sessionColor`), at most two dots.
    All bars have one color.
  - "Night before a session · last 8 weeks": one row per title with the
    average length and, muted, the count ("7 nights", "1 night"). Left
    out when there is no row.
- TalkBack: each bar is one element: "Tuesday, November 3, 5 hours
  40 minutes, GYM" (titles of that day's finished sessions), or
  "Tuesday, November 3, no night data".

The screen calls `refresh()` when it opens and when it resumes.

### 3. Review

When the day of the session has a night: a group of two rows, "Night
before" and "Asleep", right after the heart picture and before the
exercises or the cardio rows, for gym and cardio sessions. No group
without a night. Review calls `refresh()` when it opens.

### 4. Compare

Pass the loaded nights to `periodStats` for both periods. The group
"Nights" then shows by the data rules. The values have the color of the
other values. Compare calls `refresh()` when it opens.

### 5. Texts about Health Connect

Where the app lists what it reads from Health Connect (the permission
rationale screen, SPEC.md, the README when it has such a list), add
sleep sessions.

### 6. Documents

Update `SPEC.md` and `DESIGN.md` (load the `unslop` skill first): the
night rules in short, the three places, the states of the Nights screen,
and "not stored in the app, not in the backup".

## Not in scope

The Room schema, the DAO, the backup format, the night rules and texts
(report a defect in them, do not change them), a dependency or version
change. No comment that a name, a type, or a test can say; no task or
review IDs in code.

A known fault of this machine: a test worker with native graphics
sometimes hangs at 100 % CPU. Run each Gradle test command under
`timeout` (240 s for one class, 420 s for the suite). On a hang,
`kill -9` the "Gradle Test Executor" process of this worktree only and
run again, up to 3 times. Do not spend time on its cause. Export
`ANDROID_HOME="$HOME/Library/Android/sdk"`.

## Contract

| # | Place | Case | Expected result |
|---|---|---|---|
| U1 | Log | A week with nights, one day with two sessions and a stretch run, a rest day, a day with no night after the first night, a day before the first night, a future day | One line per day under the last row and the stretch lines; "No night data" only for the day after the first night; no line for the old day and the future day. TalkBack text holds the spoken line. |
| U2 | Log | The store is "not read yet", "no access", or "failed" | No night line; the Log is pixel-equal to the base (a test or two pictures prove it). |
| U3 | Progress | The Progress tab | A row "Nights" next to "Compare two periods"; a tap opens the Nights screen; Back returns to Progress. |
| U4 | Nights | The three "no access" states; a tap on the allow button | The texts and buttons of the Heart data screen pattern; the request holds only the sleep permission; after a grant the nights load. |
| U5 | Nights | Loaded with 11 nights of 14; with none in 14 days but older nights; with no night at all; failed | Figures and captions as specified; a dash; the empty text; the failed text and "Try again" starts a forced read. |
| U6 | Nights chart | 14 days; the longest night 7 h 40 m; a night of 9 h 10 m; a day with two finished sessions; a day with three; a day with no night | Axis to 8 h and to 10 h; two dots; two dots; no bar. All bars one color. Each bar speaks its text. |
| U7 | Nights list | Three titles; one title with one night | Rows in the order of the data rule with "7 nights" and "1 night". |
| U8 | Review | A gym session and a cardio session with a night; a session with no night | The two rows after the heart picture; no group. |
| U9 | Compare | Two periods with nights; both without | The group "Nights" with both rows; no group. |
| U10 | Layout | 360 dp wide, font scale 1.0 and 1.3, the longest texts ("Night 10 h 05 m · 23:10 – 09:15", a title of 28 letters in the list) | No clipped or overlapping text; the Log line may wrap. |
| U11 | Hard rules | Every new text and color | No verdict word, no judging color, no advice, no network call (`git diff` shows no new use of the network). |
| U12 | All | The whole suite, the screenshots | The `Tests:` command passes on the final commit. `NightsShotsTest` (as `LoadShotsTest`; create its folder `android/app/build/shots-nights` first) makes pictures of: the Nights screen loaded, no access, empty, each at font 1.0 and 1.3; the Log week; a Review with the rows; Compare with the group. You opened each picture. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- Each new test fails when its result breaks: give a break proof for U1, U4, U6, and U8.
- The report lists the pictures with their paths.
- Commit on branch `ngt01` (no push, no merge). Messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| U1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
