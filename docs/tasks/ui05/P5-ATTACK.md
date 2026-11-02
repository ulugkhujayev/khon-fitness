# UI05 P5 attack, round 1 (Sol, 2026-11-02)

Verdict: **open-ended**. The Writes section named single fixes but did not
define rules for competing writes: delete guards, activation, import
repair and result replay were unspecified. The lead redesigned the Writes
section as one set of guarded DAO commands (see the task file) and runs
the attack again.

## Findings and where they went

- Protocol: P5 rewrites stored data, so it is Risky, not Normal. Fixed in
  the class line.
- Row 1 named archived routines; routines have no archive field
  (`data/Stretching.kt:27,32`). Row 1 rewritten.
- Stretch sessions hold the routine name and totals, no stretch ids
  (`data/Stretching.kt:48-56`). Row 13 defines "keeps history".
- Missing rows: stretch editor fields, library search, rejected deletes,
  routine Done without stretches, activation, import, result replay, demo,
  0.4.8 cardio features. Rows 12-22 added.
- Activation: routine save and activation were separate calls
  (`data/Repository.kt:98`); import repaired programs only
  (`data/Dao.kt:298-307`). Zero active is legal (switch Off; Today falls
  back, `ui/screens/TodayScreen.kt:96`; widget needs an active one,
  `widget/TodayWidget.kt:62`). Rows 17, 18 and commands
  `setRoutineActive`, `createRoutine`.
- Export read tables one by one (`data/Repository.kt:275-283`). Command
  `backupSnapshot`.
- Player result was an upsert (`timer/TimerService.kt:392`). Command
  `recordStretchSession` (insert once); replay after delete is an accepted
  limit.
- Window: whole-row settings save (`ui/screens/WindowSheet.kt:46`);
  Open/Close now read then replace (moved to P4-FIX2); two overlapping
  reschedules (`window/WindowScheduler.kt:31-47`). Targeted commands and a
  `Mutex`.
- Cardio intervals upserted whole (`data/Dao.kt:183`). Moved to P4-FIX2.
- Scope: result and alarm fixes cross the "timer and scheduler stay"
  line. Two narrow changes allowed in the task file.

## Sequence attacks

E1-E7 (stretch and routine editors), D1-D3 (activation, import, export),
P1-P3 (player result), W1-W4 (window), C1-C3 (cardio), demo. The kept ones
are listed in the task file's "Attack cases"; C1, C2 and W2 are covered by
P4-FIX2; C3 and the demo became rows 21 and 22. W3 (alarm delivered while
disabled) is already safe: the receiver checks `enabled`
(`window/WindowScheduler.kt:77`).

# Round 2 (Sol, 2026-11-02)

Verdict: **open-ended**. The command table closed the screen edits; import,
seed, ordering ties, activation order, a missing settings row, import
versus insert-once, orphan routine rows, alarms after import, and restored
snapshots of a deleted routine were still unspecified. The lead added
rules for each (task file: "Ordering", "Activation", "Import and seed",
"Guard", attack cases E7-E11, D1, D4-D6, P2, P4, W1, W5, row 20, and a
coverage list) and runs the attack again.

# Round 3 (Sol, 2026-11-02)

Verdict: **closed**. Every round 2 finding has a rule. Three guard
conflicts (`saveStretchEdits`, replay through `recordStretchSession`, the
`.dao()` call in `KhonApp.kt`) and one ordering conflict (renumber only on
a tie of the two swapped rows versus E9) were fixed in the task file.

# P5B-FIX3 design attack (Sol, 2026-11-02)

Verdict on the first redesign (one `RoutineWriter` per routine with
phases Draft, Creating, Saved): **open-ended**. Writer reuse, start-up
values, delete, shared Active state and late callbacks had no rule. The
root cause was a first save that can fail (E11 on the screen). The lead
replaced the design: the first stretch commits the draft and cannot fail,
the phase flips in the click handler, and one queue orders every write of
the screen (`P5B-FIX3.md`). The attack runs again on it.
