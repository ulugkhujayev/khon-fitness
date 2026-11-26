# CMP01 verification, round R4

Snapshot `858893b`. Scope: the 23 Android comment lines added against `9d1ba17`.

- **Read.** The comment on the compare route repeats what the screen test shows. The Progress row opens the comparison and displays two periods. Remove the comment. [KhonNav.kt:86](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R4/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/KhonNav.kt:86), [CompareScreenTest.kt:116](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R4/src/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/CompareScreenTest.kt:116)
- **Read.** The other 22 lines document rules, output examples, a line break quirk, or when the screenshot test runs. The fix restores the original Progress comment. [HistoryProgress.kt:49](/Users/mirzohidkhon/.config/agents/state/snapshots/CMP01-compare-periods-d68b5544-R4/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryProgress.kt:49)
- **Ran.** `wc -l inputs/2-cmp01-added-comments.txt` returned 23. The `python3 -B` source check printed `23 added lines uniquely map to the snapshot.` It also confirmed that the restored Progress line matched the fix diff and was absent from the list. F5 has no witness. No Gradle run.

F5=missed-instance
