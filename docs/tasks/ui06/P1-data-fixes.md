# UI06 P1: data and flow fixes

Class: Risky (rewrites stored data). Read `COMMON.md` first. Review follows `~/.config/agents/review-protocol.md`.
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
| 5 | Swap A → B "this session only", then B → C "update the program too" | program row holds A | The program row at that position holds C. The position is the set log's `blockIndex` + `slot` in the program snapshot (blocks by (sortOrder, id), rows by (slot, id), rows with a missing exercise left out), read in one transaction. Other rows with the same exercise stay. |
| 6 | Swap with "update the program" | session started from the program, program edited since in any way (blocks or rows: order, exercise, sets, reps, removed or added) | Session swap works; program unchanged; no crash; the sheet says "The program changed after this workout started. Only this workout changed." The session's origin (DataStore `session_origin_<id>`: programId, startedAt, program fingerprint) decides, under one lock with start, finish, discard, import and reset. |
| 7 | Save a manual cardio entry with every field empty | new cardio session | Save is off; the form says "Enter time or distance"; the form always shows Time. With time, distance or laps above 0 it saves. The Dao transaction checks the same rule (`CardioSaveResult`): a refusal writes nothing and the form stays open with the line. A finished session from History is not checked. |
| 8 | Interval timer: Stop, then Today "Resume" | stopped early | Today offers "Save" (opens the form with the timer's time and intervals filled) and "Discard"; no dead "Resume". Same when the app restarts after Stop. |
| 9 | Exercise Merge: pick a target | source has sets | Confirm sheet: "Merge <A> into <B>? <n> sets and <m> sessions move to <B>. <A> is deleted." Merge / Cancel. Merge runs once, in one transaction, with no Undo (owner). It refuses, writing nothing, when the source or the target is gone or they are the same exercise. |
| 10 | (dropped) | — | Merge Undo is removed (owner: confirm only). |
| 11 | Eating window history | window turned on today, first time | Strip and summary count only days from the enable date; "Kept the hours on 0 of 0 days" never shows: the line reads "Tracking since <date>" until one settled day exists. |
| 12 | Eating window history | existing user, window already on, no enable date stored | Enable date = earliest `WindowDay` date, else today. Stored once in DataStore key `window_since`. Turning the window off and on keeps the first date. |
| 13 | Backup export / import | `window_since` | Not part of the backup (DataStore). After import, rule 12's fallback applies. Existing backup tests still pass. |

## Attack cases (Sol attack rounds 1-3, design v4)

Each case is a test.

D1, swap with "update the program too" (`SwapBySlotTest`, `SessionStartOneStateTest`):
1. Start; reorder blocks; swap with update: refused, session-only swap, message.
2. Start; change that row's exercise in the editor; swap with update: refused.
3. Remove and re-add a block or exercise at the same position; swap with update: refused; the new row unchanged.
4. Duplicate exercises, and a block whose only row has slot 1; swap slot 0 with update: exactly that row changes.
5. A→B session only, B→C with update, restart, C→D with update: the program row holds D.
6. Two sessions from one program; update in the first, then in the second: the second refuses.
7. Import (both modes) or reset while a session runs, also with its origin write pending; swap with update: refused; no origin survives.
8. Origin removed or unreadable, a value of another type under its key, or a store that fails to read: refused, no crash.
   Finish or discard removes only that session's origin.
9. Tied block sortOrder, tied slots, a missing-exercise row: selection by (sortOrder, id) and (slot, id), never a wrong row.
10. A block edit between session start's program reads waits: logs and fingerprint from one database state.
11. Only sets or rep limits changed since the start: refused.
12. A missing-exercise row whose sets changed: refused.

D1a, origin storage errors (`SwapBySlotTest`, "storage a-e"). A swap with update removes the origin before the program changes and writes it after. An origin `write` or `remove` that fails after its
Room commit returns false and never escapes (a DataStore edit is atomic: on error nothing is written). A
CancellationException still propagates. `clearAll` still throws, so an import or a reset stops before its Room writes.

| # | Action | Expected |
|---|---|---|
| a | Start a program session; the origin write fails | Session started and returned, no crash; a later "update the program too" refuses (no origin). |
| b | Swap with update; the origin is removed first; if that fails | Program unchanged, session swapped, the refusal message. |
| b2 | Swap with update; the program row commits; the new-fingerprint write fails | Result ProgramUpdated, no crash; no origin is left, so the next update refuses, also when the program is later back in an earlier state (a swap back, or an edit by hand). |
| b3 | Swap with update refused (program changed) | The origin is written back unchanged; once the program is back in its start state an update works. |
| c | Finish or delete; the origin remove fails | Session finished or deleted, no crash; a stale key never allows an update (session finished or missing). |
| d | Cancellation during write or remove | CancellationException propagates. |
| e | clearAll fails during import (both modes) or reset | No Room write happens. |

D2, merge (`MergeConfirmTest`):
13. Missing source, missing target, or source = target: refused, nothing written.

D3, one exit guard per editor screen (`ExitGuardTest`, `SystemBackGuardTest`):
14. Two taps in one frame on each closing action (Done, Archive, Unarchive, Delete, Merge, Discard, Back) of each editor: one write, one pop.
15. Done and Back in one frame: one pop.
16. A refused Delete, then Done: Done works.
16a. System Back takes the same guard on every editor, the cardio form and the session screen: Done (or Save, Back)
    and system Back in one frame pop once; system Back while a Save, Merge or Delete is pending pops nothing, then the
    action pops once. An action that completes after its screen left by another way pops nothing.

D4, cardio save (`CardioSaveNeedsTest`):
17. An exercise with only heart rate and watts: the form shows Time; Save off while empty, on with a time.
18. Opened with a time; the stored time cleared elsewhere; Save with interval edits: refused, intervals unchanged, form open.
19. Opened empty; the timer stores a time; a note saved: Save on and saved.
20. Zero, negative, laps only, finished History: one rule in the form and the data layer; finished entries always save.

D5, timer (`TimerStoppedTodayTest`):
21. The timer service runs, Stop, the app state is rebuilt: Today shows Save and Discard; Save opens the form with the stored time and intervals.

## Tests

Each row is a test (unit, Room in-memory via Robolectric, or Compose). Rows 1
and 8 need a Compose or navigation test. Rows 5 and 6 run against the real
DAO in memory. Name new test classes after the behavior
(`FinishSaveOnceTest`, `SwapBySlotTest`, `WindowSinceTest`).
