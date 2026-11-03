# UI06: Today, History and Plan redesign, and the 0.5.0 QA fixes

Class: Normal. Review, tests and fixes follow `~/.config/agents/review-protocol.md`.

## Goal

The owner tested 0.5.0 and asked for a redesign of three tabs and fixes for
every QA finding. It ships as one release, 0.6.0 (0.5.0 is never published).
The work runs in phases, one builder each, in its own git worktree that T3
makes from branch `ui05-port` (0.5.0 + Inter subset + S25 test screen; code
base `1eb3ae1`). P1-P4 run at the same time, so two phases can touch one
file: keep each edit inside the functions your phase owns. The lead merges
the phase branches.

| Phase | File | Builder | Touches |
|---|---|---|---|
| P1 | `P1-data-fixes.md` | opus-worker | session, cardio, timer, merge, window rules, Dao/Repository |
| P2 | `P2-today.md` | ui-builder | Today tab |
| P3 | `P3-history.md` | ui-builder | History tab (Log + Progress) |
| P4 | `P4-plan.md` | ui-builder | Programs tab → Plan tab, library, cardio workout editor |
| P5 | `P5-polish.md` | ui-builder | contrast, labels, stretch Back, battery note (after P1-P4 merge) |

## Sources of truth, in this order

1. The phase file's contract table.
2. `UI06-ATTACK.md` (rules from the Sol UX attack).
3. `prototype.html` in this folder: the look, layout, copy and flows. Open
   it in a browser at 360 x 780. Where the prototype and a rule above differ,
   the rule wins (the prototype predates the attack).
4. `DESIGN.md` (repo root) for tokens, components and everything the
   prototype does not show. Update `DESIGN.md` for every rule the phase changes.
5. `SPEC.md` (repo root) for behavior. Update it when the phase changes behavior.
6. `UI06-COVERAGE.md`: where the current code lives.

## Rules for every phase

- Target screen: Galaxy S25, 360 x 780 dp. Robolectric already runs at
  `w360dp-h780dp-xxhdpi` (`android/app/src/test/resources/robolectric.properties`).
- Every tap target is 48 x 48 dp or more. Layouts stay usable at font scale 1.6.
- No Room schema change and no migration. Keep the backup format. If a rule
  seems to need a schema change, stop and report it as a question.
- Keep every feature the old screen has unless the phase file removes it.
  Before you finish, list what the old screen did (`git show 1eb3ae1:<path>`)
  and check each item on the new one.
- Pure logic goes into plain Kotlin functions with unit tests. A test for new
  behavior fails on `1eb3ae1` and passes after. Every contract row is a test,
  a Compose UI test, or (only for pure looks) a line in the report with the
  `file:line` that meets it.
- Keep existing accessibility labels; new controls get spoken labels.
- Reuse `ui/components/*` and the theme `K`. Match the code style around you.
- Commit in small steps on your phase branch, messages like
  `UI06 P2: Today card with Change`. No push. No AI attribution.
- Change only `android/`, root `DESIGN.md`, root `SPEC.md`, and this folder.

## Checks (run from `android/`)

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
```

Baseline at `1eb3ae1`: 389 tests, 0 failures. Check free disk first (stop
below 30 GB). Do not run an emulator; the lead does the device pass.

The report lists: commits, the command and the tail of its output, the test
count, each contract row with the test or `file:line` that meets it, the old
feature checklist, and every place you chose differently from the prototype
and why. Under 60 lines.
