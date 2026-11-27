# REV01-review-from-history: a finished session in History opens its Review

Task-ID: REV01-review-from-history
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/rev01
Base: 566b0b4
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/rev01/docs/rev01-tasks/REV01-review-from-history-REPORT.md

## Why

The Review screen (`ui/screens/ReviewScreen.kt`) shows the heart rate
line, the heart rate picture with the sets and rounds, the sets in order
with their rest times, and the cardio heart rate figures. Only Today
opens it (`onReview` in `TodayScreen.kt`), and only for the session of
today. The band's data often arrives hours later or the next day. A
session opened from History goes to `SessionScreen` or `CardioScreen`
(`openSession` in `HistoryScreen.kt`), which show none of this. So the
owner cannot see the picture of a past session at all.

Hard rules of the app: everything stays on the phone; no suggestion of
exercises, sets, or loads; facts only. Read `DESIGN.md` before any
layout: no cards with per-row buttons, no pills, no captions, no helper
sentences; the existing row, group, and bar components only.

## Scope

History keeps opening the same screens. Each of them gets one row that
opens Review. No change to the data layer, the timer, Review's content,
the routes of History, a dependency, or the version.

1. `SessionScreen`, a finished program session, not in Edit mode: the
   first item of the list is a group with one row "Review" (the existing
   `GroupedList` and `ListRow`, with the chevron). The value on the
   right is the heart rate line of Review (`hrLine`: "avg 121 · max 163
   bpm") in the muted color; with no line, no value. A tap opens
   `Routes.review(id)`. An unfinished session and Edit mode have no such
   row.
2. `CardioScreen`, a finished session: the same row in its own group
   above the form. It shows only while the form has no unsaved change
   (the draft equals the opened row and no interval edit exists), so a
   tap never loses typed values. An unfinished session has no such row.
3. `ReviewScreen`: the back label of its bar names where it came from:
   "Today" when the entry before it is Today, else "History". Review
   still has no Edit and no Delete.
4. Back from Review (the bar and the system Back) returns to the session
   screen it came from, with its open block and scroll place. The exit
   guards of both screens (`ExitGuard.kt`) keep working: a double tap
   opens one Review, and Back after it pops one screen.
5. A session that is deleted while its Review is open over a History
   screen: both screens leave, and History shows (the existing
   `LeaveIfSessionMissing` of each screen; prove it with a test).
6. The value of the row follows the heart rate fill while the screen is
   open.
7. Speech: "Review, average 121, maximum 163 beats per minute, button";
   with no line "Review, button".

### Documents and pictures

- `SPEC.md` and `DESIGN.md`: the row, where it shows, and the back
  label, in each file's own style. Load the `unslop` skill first.
- A screenshot test in the way the other `*ShotsTest` files do it: the
  finished gym session with the row and a heart rate line, the same
  without a line, the finished cardio session with the row; at 360 and
  411 dp and font scale 1.0 and 1.3. Look at each picture yourself
  before you report, and name the files.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| V1 | Gym row | A finished program session from History; the same in Edit mode; an unfinished session | The row "Review" is the first item; no row; no row. |
| V2 | Gym value | A filled heart rate read; a waiting read; none; a typed-only average does not exist for gym | "avg 121 · max 163 bpm" (the same text as Review shows); no value in the other states. |
| V3 | Cardio row | A finished cardio session; the same after a typed change in a field or in an interval field; after the change is undone by Save; an unfinished session | The row; no row; the row again on the saved session; no row. |
| V4 | Open | A tap on the row in each screen; a double tap | Review of that session opens once, with its picture and figures as from Today. |
| V5 | Back | The bar Back and the system Back on Review opened from a session screen; from Today | The session screen shows again in the same state; Today shows. The label is "History" and "Today". |
| V6 | Delete | The session is deleted while Review is open over the session screen | Both leave; History shows; no crash and no blank screen. |
| V7 | Live | The fill commits while the session screen is open | The value of the row follows. |
| V8 | Layout and speech | 360 and 411 dp, font scale 1.0 and 1.3, the longest value "avg 188 · max 201 bpm" | No clipped or overlapping text; the tap-target and contrast tests cover the row; the spoken texts of Scope 7. |
| V9 | Rules | Whole change | The existing tests of History, the session screens, Review, and the exit guards pass unchanged, or the report names each changed assertion with its reason. No comment that a name, a type, or a test can say; no task or review IDs in code. `SPEC.md` and `DESIGN.md` updated. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- A test for each new rule fails on the base (or does not compile there) and passes on the branch.
- The `Tests:` command passes on the final commit. Commit on branch `rev01` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| V1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
