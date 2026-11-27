# HR01 attack round R5

READ marks rules and base source. GUESS marks results derived from the rules. No app or device tests ran.

## R4 contract check

| R4 row | R5 design | R5 contract |
|---|---|---|
| C36 | READ. The reader collects records that cross either bound and filters the samples separately. [Common](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R5/inputs/2-HR01-COMMON.md:130) | C21 covers a record that crosses only the start, one that crosses only the end, and one that crosses both. [P1](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R5/inputs/4-HR01-P1-data.md:75) |
| C37 | READ. Migration gives a range only to finished band imports. A legacy gym range needs the owner's confirmation of the shown times. [Migration](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R5/inputs/2-HR01-COMMON.md:56), [Confirmation](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R5/inputs/2-HR01-COMMON.md:200) | C2 leaves all old gym ranges null. C7b and E7a cover confirmation. [C2](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R5/inputs/4-HR01-P1-data.md:54), [C7b](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R5/inputs/4-HR01-P1-data.md:61), [E7a](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R5/inputs/6-HR01-P3-ui.md:95) |
| D21 | READ. Old-pair planning uses `startedAt + timeSec` as A's fallback end and ignores Save time. [Common](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R5/inputs/2-HR01-COMMON.md:274) | D9a covers the rule. Keep the original delayed-Save test as AT13. The separate run in D9a overlaps only 40 of A's 100 minutes even with the old rule. [P2](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R5/inputs/5-HR01-P2-import.md:54) |

All three R4 rows have a design rule and a contract row.

## Missing contract rows

| ID | Part | Case | Required result |
|---|---|---|---|
| D21 | Old pairs: legacy start | V4 creates A before Start. A stays on Ready during a separate band run B. A then runs and saves its unchanged timer duration. | Keep both sessions and their logs. The time when the app created A cannot establish when its workout began. |
| D22 | Old pairs: legacy duration | The owner edits A's Time after its v4 timer run. This value extends the inferred range into a separate band run B. | Keep both sessions, their logs, and the typed Time. The edit cannot establish the end of the timer run. |

## Missing coverage entries

- **O1 / D21. READ.** Extend N3 to trace when v4 sets `sessions.startedAt`, when Start leaves Ready, and what Stop saves. Follow those values into candidate selection and deletion. The session exists before Ready. The timer adds no elapsed time while it waits. Stop saves only the duration. [N3](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R5/inputs/3-HR01-COVERAGE.md:469), [TodayScreen.kt](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R5/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayScreen.kt:66), [Repository.kt](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R5/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:277), [TimerState.kt](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R5/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerState.kt:75), [TimerService.kt](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R5/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:393)
- **O2 / D22. READ.** Extend N3 to trace the editable Time field through Save into the same `timeSec` column the timer writes. Follow the edited value into candidate selection and deletion. [CardioScreen.kt](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R5/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CardioScreen.kt:78), [Repository.kt](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R5/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:46), [Dao.kt](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R5/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:218)

## Sequence attacks

GUESS. The expected results follow the R5 text. AT14 and AT15 show the deletions it permits. The required results above keep both workouts.

| ID | Phase | Steps | Expected result |
|---|---|---|---|
| AT1 | P1 | Start gym at 18:00. Check a set at 18:08. At true 18:10 move the phone clock forward 40 minutes. Check another set at true 18:15 and finish at true 18:17. Correct the clock. Supply band samples from 18:24 through 18:54. Read again at 19:00. | C7a. Both checks and the session survive. The later check has no stamp. The gym has no range or measured heart rate. |
| AT2 | P1 | Hold R1 before its commit. Start R2 on the same session with a later anchor. Change the clock during R2 and commit it. Save a typed duration of 25 minutes. Release R1 and replay R2. | C15. Keep 25 minutes and R2's `timerRun`. The run and round clock fields stay null. |
| AT3 | P1 | Open Ready at 18:00. Supply band readings only from 18:03 through 18:35. Start from the notification at 18:40. Stop at 19:00 and save at 19:01. | C11. The stored range starts at 18:40. The read gives no measured average or maximum. |
| AT4 | P1 | Hold cleanup after it selects a stale filled row. Merge a file with a filled row for the current range. Resume cleanup while the provider fails. | C27. The matching row stays filled. `hrFacts` exposes its values. |
| AT5 | P1 | Use a range of 18:00 to 18:30. Supply records from 17:59 to 18:12 and 18:18 to 18:31. Give 130 bpm every minute from 18:00 through 18:11 and 18:18 through 18:29. Add 200 bpm at 17:59 and 18:30. | C21. Both records contribute. The result is filled, with `rawCount = 26` and `sampleCount = 24`. The average and maximum are 130. |
| AT6 | P1 | Export a session with a typed value and an HR row. Change the HR row's state to a JSON number. Open the file through Restore, then Replace. | C33. Preview and import succeed. The session and typed value survive. The malformed HR row is absent. |
| AT7 | P1 | Start a read for a range that begins at 00:15 local time. Hold the complete result. Change to a zone where the start falls on the preceding date. Release the read. | C18 and C26. The current zone invalidates the range. The read writes nothing. |
| AT8 | P2 | Make a unique pair with matching round indexes. Only B has effort 8, session watts 220, Backstroke, and first-round watts 240. Verify the copy and merge. | D11. The survivor carries all four values. |
| AT9 | P2 | Make a pair with matching bounds and modality. Give A a stored timer range. Move B to the previous date in History. Run the pair pass. | D9. Both sessions remain. History keeps B on the date the owner saved. |
| AT10 | P2 | Verify the copy of a unique pair. Change B's first-round watts to 240. Leave both session rows and the candidate relation alone. Run the merge transaction. | D17. Both sessions remain. B's edited round stays at 240 watts. |
| AT11 | P2 | Open a grouped draft for runs from 18:00 to 18:20 and 18:15 to 18:45. End an app run of the same modality from 18:00 to 18:20. Leave its form unsaved. Tap the old grouped draft. | D1 and D7. The old group writes no session. The refreshed list offers only the second record. |
| AT12 | P3 | Reread a filled session. Hold delivery to the sheet after the result commits. Tap Remove. Deliver the old response. | E6. The list and sheet show Removed. The old response supplies no measured fact line. |
| AT13 | P2 | On v4, run A from 18:00 to 18:20. Leave its form unsaved. Record a separate band run B from 18:50 to 20:00. Save A at 20:05 with its timer duration. Import B as a plain run. Upgrade and run the pair pass. | D9a. A's inferred range ends at 18:20. The runs do not overlap. Both sessions and their logs and values stay. |
| AT14 | P2 | On v4, open A's run timer at 18:00 and leave it on Ready. Record a separate band run B from 18:01 to 18:19. Tap Start for A at 18:40. Stop after the timer records 20 minutes. Save A without editing Time. Import B as a plain run. Upgrade and run the pair pass with a valid copy and no other candidates or conflicts. | R5 infers 18:00 to 18:20 for A. The 18-minute overlap qualifies. The pass deletes B and stores 18:01 to 18:19 as A's range. |
| AT15 | P2 | On v4, start A's timer when the session opens at 18:00. Stop after 20 minutes. Record a separate band run B from 18:25 to 18:58. Enter 60 minutes as A's Time and save at 19:05. Import B as a plain run. Upgrade and run the pair pass with a valid copy and no other candidates or conflicts. | R5 infers 18:00 to 19:00 for A. The 33-minute overlap qualifies. The pass keeps A's 60 minutes, deletes B, and stores 18:25 to 18:58 as A's range. |

## Verdict

**Open-ended.** READ. The old-pair rule uses A's range "when set, else `[startedAt, startedAt + timeSec)`". A valid copy and a unique match then permit B's deletion. GUESS. In AT14 one owner leaves the phone on Ready during B, then starts A after B ends. The rule finds 18 minutes of overlap. The actual runs overlap by zero minutes. The pass deletes a separate session even with correct clocks and an untouched timer duration. [Fallback and overlap](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R5/inputs/2-HR01-COMMON.md:274), [Deletion](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R5/inputs/2-HR01-COMMON.md:293)

2 missing contract rows, 2 missing coverage entries, 15 cases.
