# HR01 attack round R4

**READ.** The findings cite the snapshot rules and base source. **GUESS.** The table gives expected results from those rules. No app or device tests ran.

## R3 contract check

| R3 row | R4 design | R4 contract |
|---|---|---|
| C36 | [Verified clocks and Gym Finish](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/2-HR01-COMMON.md:172) | [C7a](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/4-HR01-P1-data.md:60) covers a clock change after Start. |
| C37 | [Timer commit order](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/2-HR01-COMMON.md:218) | [C15](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/4-HR01-P1-data.md:68) covers a late write and replay after an untrusted commit. |
| C38 | [Anchor at Start](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/2-HR01-COMMON.md:198) | [C11](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/4-HR01-P1-data.md:64) covers both Start actions and Stop on Ready. |
| C39 | [Cleanup transaction](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/2-HR01-COMMON.md:148) | [C27](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/4-HR01-P1-data.md:80) covers a current row from Merge and a row changed to `off`. |
| C40 | [Records crossing either endpoint](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/2-HR01-COMMON.md:126) | [C21](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/4-HR01-P1-data.md:74) describes records that cross both endpoints. It misses a separate record at each endpoint. Add C36 below. |
| D21 and D22 | [Dates of both copies](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/2-HR01-COMMON.md:264), [Owner-only fields](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/2-HR01-COMMON.md:277) | [D11 and D9](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/5-HR01-P2-import.md:53) cover B-only values and B's moved date. |
| E12 | [Current facts for every screen](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/2-HR01-COMMON.md:106) | [E6](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/6-HR01-P3-ui.md:89) covers a late result after Remove or a range change. |

## Missing contract rows

| ID | Case | Required result |
|---|---|---|
| C36 | A record crosses only the start. Another crosses only the end. | Both contribute. Samples outside the range count only in `rawCount`. |
| C37 | Migration sees a legacy gym row whose finish time includes a wall clock jump. Both endpoints pass C2. | Keep the legacy session and its logs. Leave its HR range empty when the old data cannot verify it. Change C2 with the design. |
| C38 | The phone clock is already one hour slow at gym Start. It stays slow through Finish. | Keep the session and its sets. Withhold the range when the anchor cannot place it on the same timeline as the band readings. |
| D21 | A legacy timer workout ended before a separate band workout began. A was saved after B ended. The two rows make a unique pair by saved bounds. | Keep both sessions, their logs and their values. A form Save time cannot establish that A's workout overlapped B. |

## Missing coverage entries

- **N1 / C37.** Trace legacy gym start and Finish through the v4-to-v5 migration, then through `hrWindow`, the fill and `hrFacts`. Include a clock change before upgrade. [G13](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/3-HR01-COVERAGE.md:315), [Repository.kt:267](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:267), [Dao.kt:223](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:223)
- **N2 / C38.** Extend M1 from clock changes during a session to time already wrong when the app takes the anchor. Trace the result against data that Health Connect already holds. Check the shared anchor rule for the timer too. [M1](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/3-HR01-COVERAGE.md:456), [Shared clock helper](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/2-HR01-COMMON.md:172)
- **N3 / D21.** Trace a legacy timer Stop, the later form Save and a separate band import into old-pair planning and deletion. Distinguish the duration written by Stop from the endpoint written by Save. [TimerService.kt:393](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:393), [Repository.kt:46](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:46), [Repository.kt:341](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:341)
- M5 already covers the two record endpoints for C36. Its common design reference moved to the reader section. [M5](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/3-HR01-COVERAGE.md:460), [Reading](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/2-HR01-COMMON.md:126)

## Sequence attacks

AT13–AT15 record the results the current design requires. They show why the rules need to change before the safe results above can pass.

| ID | Phase | Steps | Expected result |
|---|---|---|---|
| AT1 | P1 | Start gym at 18:00. Check a set at 18:08. At true 18:10 advance the phone clock 40 minutes. Check another set at true 18:15 and finish at 18:17. Correct the clock. Supply later band samples from 18:24 through 18:54. Read again at 19:00. | C7a. Both checks and the session survive. The later check has no stamp. The gym has no range or measured heart rate. |
| AT2 | P1 | Hold R1 before its commit. Start R2 on the same session as in C15, with a later anchor. Change the clock during R2 and commit it. Save a typed duration of 25 minutes. Release R1 and replay R2. | C15. Keep 25 minutes and R2's `timerRun`. The run and round clock fields stay null. |
| AT3 | P1 | Open Ready at 18:00. Supply band readings only from 18:03 through 18:35. Start from the notification at 18:40, stop at 19:00 and save at 19:01. | C11. The stored range starts at 18:40. The read gives no measured average or maximum. |
| AT4 | P1 | Hold cleanup after it selects a stale filled row. Merge a file with a filled row for the current range. Resume cleanup with the provider failing. | C27. The matching row stays filled. `hrFacts` exposes its values. |
| AT5 | P1 | Use a range of 18:00 to 18:30. Supply records from 17:59 to 18:12 and 18:18 to 18:31. Give 130 bpm every minute from 18:00 through 18:11 and 18:18 through 18:29. Add 200 bpm at 17:59 and 18:30. | C36. Both records contribute. The result is filled, with `rawCount = 26` and `sampleCount = 24`. The average and maximum are 130. |
| AT6 | P1 | Export a session with a typed value and an HR row. Change the HR row's state to a JSON number. Open the file through Restore, then Replace. | C33. Preview and import succeed. The session and typed value survive. The malformed HR row is absent. |
| AT7 | P1 | Start a read for a range that begins at 00:15 local time. Hold the complete result. Change to a zone where the start falls on the preceding date. Release the read. | C18 and C26. The current zone invalidates the range. The read writes nothing. |
| AT8 | P2 | Make a unique pair with matching round indexes. Only B has effort 8, session watts 220, Backstroke and first-round watts 240. Verify the copy and merge. | D11. The survivor carries all four values. |
| AT9 | P2 | Make a legacy pair with matching bounds and modality. Move B to the previous date in History. Run the pair pass. | D9. Both sessions remain. History keeps B on the date the owner saved. |
| AT10 | P2 | Verify the copy of a unique pair. Change B's first-round watts to 240. Leave both session rows and the candidate relation alone. Run the merge transaction. | D17. Both sessions remain. B's edited round stays at 240 watts. |
| AT11 | P2 | Open a grouped draft for runs from 18:00 to 18:20 and 18:15 to 18:45. End an app run from 18:00 to 18:20. Leave its form unsaved. Tap the old grouped draft. | D1 and D7. The old group writes no session. The refreshed list offers only the second record. |
| AT12 | P3 | Reread a filled session. Hold delivery to the sheet after the result commits. Tap Remove. Deliver the old response. | E6. The list and sheet show Removed. The old response supplies no measured fact line. |
| AT13 | P1 | Repeat the gym workout and clock change in AT1 on v4. Finish at displayed 18:57. Correct the clock before the later band activity. Upgrade to v5 and read again at 19:00. Supply 160 bpm once per minute from 18:24 through 18:54, all from that later activity. | C2 as written. Migration stores 18:00 to 18:57. The read is filled, with average and maximum of 160 from the later activity. |
| AT14 | P1 | Sync 160 bpm once per minute from 18:01 through 18:18 during a band ride. At true 19:00 set the phone to 18:00 and start gym. Check at displayed 18:15 and finish at 18:20. Keep the band disconnected during gym. Correct the clock and read before further sync. | The clocks move together after the anchor. The gym gets the 18:00 to 18:20 range. The prior ride supplies the filled average and maximum of 160. |
| AT15 | P2 | On v4, run from 18:00 to 18:20 with the app timer. Leave its form unsaved. Record a second run on the band from 18:50 to 20:00. Save A at 20:05. Import only the second run as B under a running exercise without intervals. Leave owner-only fields empty. Upgrade and run the pair pass with a valid copy and no other candidates. | A's saved bounds overlap all 70 minutes of B and more than half of A's 125 minutes. The pass deletes B and attaches B's 18:50 to 20:00 range to A. |

## Verdict

**Open-ended.** The legacy pair rule uses A's range “when set, else `[startedAt, finishedAt)`”. AT15 turns time spent waiting to Save into proof that two separate runs were one. The verified copy makes the deletion recoverable. It cannot prove that the workouts match. [Legacy candidate range](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/2-HR01-COMMON.md:264), [Delete after copy check](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/2-HR01-COMMON.md:284)

The same clock problem reaches both legacy and live gym writes. Migration accepts elapsed wall time alone. The live check measures agreement only after Start. Both ranges can pass `hrFacts` with readings from another workout. [Migration](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/2-HR01-COMMON.md:56), [Anchor comparison](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/2-HR01-COMMON.md:174), [Matching range](/Users/mirzohidkhon/.config/agents/state/snapshots/HR01-P1-data-453ace09-R4/inputs/2-HR01-COMMON.md:106)

4 contract rows, 3 coverage entries, 15 cases.
