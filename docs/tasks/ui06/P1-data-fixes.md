# UI06 P1: data and flow fixes

Class: Normal. Read `COMMON.md` first. Review follows `~/.config/agents/review-protocol.md`.
Builder: opus-worker. Code locations: `UI06-COVERAGE.md`.

Do not restyle screens in this phase: P2-P4 rebuild them in parallel. Keep
UI edits to the smallest change that carries the rule (a guard, a sheet, an
error line), so the lead's merge stays easy.

## Contract

| # | Action | Data | Expected |
|---|---|---|---|
| 1 | Double tap Save in the Finish sheet | a running gym session | One `finishSession` call, one back navigation; Today shows, never a blank screen. A guard in the handler (not a delay). Test: two clicks in one frame. |
| 2 | Tap the check on a set | reps empty or 0, or weight empty or 0 on a weighted exercise | The set stays pending; the row shows "Enter reps" / "Enter weight" under the fields; no "New best". A bodyweight exercise (stepKg 0 or its kind says so; read the model) needs reps only. |
| 3 | Finish summary | sets with 0 reps checked by old data | 0-rep sets count as not done and never as a new best. |
| 4 | Discard workout | 3 sets checked | Confirm sheet "Discard this workout? 3 checked sets are lost." Cancel and Discard; nothing is deleted on Cancel. |
| 5 | Swap A → B "this session only", then B → C "update the program too" | program row holds A | The program row at that block and slot holds C. Match the program row by the set log's `blockIndex` + `slot`, not by exercise id. Other rows with the same exercise stay. |
| 6 | Swap with "update the program" | session started from the program, program later edited so the block/slot no longer exists | Session swap works; program unchanged; no crash. |
| 7 | Save a manual cardio entry with every field empty | new cardio session | Save is off; the form says "Enter time or distance". With time or distance it saves. |
| 8 | Interval timer: Stop, then Today "Resume" | stopped early | Today offers "Save" (opens the form with the timer's time and intervals filled) and "Discard"; no dead "Resume". Same when the app restarts after Stop. |
| 9 | Exercise Merge: pick a target | source has sets | Confirm sheet: "Merge <A> into <B>? <n> sets and <m> sessions move to <B>. <A> is deleted." Merge / Cancel. |
| 10 | After a merge | — | A snackbar "Merged into <B>" with Undo for 10 s. Undo restores the source exercise row and every moved row (set logs, cardio sessions, program exercises, week plan, overrides) to the source id. Snapshot in memory before the merge, one transaction each way. |
| 11 | Eating window history | window turned on today, first time | Strip and summary count only days from the enable date; "Kept the hours on 0 of 0 days" never shows: the line reads "Tracking since <date>" until one settled day exists. |
| 12 | Eating window history | existing user, window already on, no enable date stored | Enable date = earliest `WindowDay` date, else today. Stored once in DataStore key `window_since`. Turning the window off and on keeps the first date. |
| 13 | Backup export / import | `window_since` | Not part of the backup (DataStore). After import, rule 12's fallback applies. Existing backup tests still pass. |

## Tests

Each row is a test (unit, Room in-memory via Robolectric, or Compose). Rows 1
and 8 need a Compose or navigation test. Rows 5, 6, 10 run against the real
DAO in memory. Name new test classes after the behavior
(`FinishSaveOnceTest`, `SwapBySlotTest`, `MergeUndoTest`, `WindowSinceTest`).
