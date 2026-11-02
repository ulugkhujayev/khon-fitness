# UI05: port the Expo app's UI to the Kotlin app

Class: Normal. Review, tests and fixes follow `~/.config/agents/review-protocol.md`.

## Goal

The Expo rewrite in `expo/` (merged from PR #2) has a newer UI than the
Kotlin app in `android/`. The owner keeps the Kotlin app and wants its UI
to look and feel like the Expo app. The port runs in five phases (P1 to P5),
one builder each, in order, on the branch `ui05-port`, in the main checkout
`/Users/mirzohidkhon/deepvision/personal/khon-fitness`. Each phase starts
from the commit the previous phase left.

## Sources of truth

- `expo/docs/DESIGN.md`: every rule marked "Expo app" is the target. A rule
  that also says what "the Kotlin app" does describes the old state.
- `expo/src/components/**` and `expo/src/app/**`: exact sizes, colors,
  timings, spring values and copy. When DESIGN.md and the code differ, the
  code wins; note the difference in your report.
- `DESIGN.md` (repo root) binds the Kotlin app. Each phase moves the rules
  it ports from `expo/docs/DESIGN.md` into the root `DESIGN.md`, without
  the "Expo app" / "Kotlin app" qualifiers. A ported screen whose rule
  stays old in `DESIGN.md` is not done.
- `SPEC.md` (repo root) binds behavior. Change it when a phase changes
  behavior (see the decisions below).

## Behavior decisions (lead, owner said "copy behavior where it makes sense")

Copy:
- A confirm sheet before every delete or remove (exercise, program, block,
  exercise from a block, stretch from a routine, routine, session, modality
  if it can be deleted).
- Drafts: a new program, block or routine is not saved until Done (or the
  first exercise / stretch for a block / routine), as `expo/docs/DESIGN.md`
  "Editors" describes. A draft left without Done is gone; nothing is
  written.
- Steppers: one step per tap with a light haptic click; hold past 350 ms
  repeats (160 ms, 15 ms faster per step, floor 50 ms); save once on
  release.
- Session: Finish only in the header; it opens a "Workout done" sheet with
  time, sets done, volume, new bests, effort and note, then Save. One
  block open at a time; after a block's last set the next block with work
  opens (wrapping), and with nothing left every block closes.
- An exercise's kind is locked once the exercise is used, with the reason
  as a footnote. The active program's Active toggle is fixed on.
- Search in long pick sheets (exercises, stretches), with the no-results
  view.
- Haptic clicks on the tab bar, segmented controls and the ruler picker.

Do not copy:
- The development-only x20 long press in the stretch player, and the debug
  "Write a test run" import action.
- EAS Update. The Kotlin app keeps its APK updater (`update/`). Settings
  and Today show the updater's real state in the Expo layout; if the
  updater has no "update ready" state, leave the Today update row out.
- Expo-only gaps listed in the PR (timer without a foreground service,
  week-ahead reminders, 30-minute widget refresh). Kotlin's behavior stays.

## Rules for every phase

- Change only `android/`, root `DESIGN.md`, root `SPEC.md`, and this task
  folder. Never edit `expo/`; it is the reference until the last phase
  deletes it.
- Keep the data layer (Room schema, backup format, repository API)
  unchanged unless the phase file says otherwise. No Room migration.
- Pure logic you add (block flow, repeat timing, draft lifecycle, summary
  text, week-at-a-glance text, window state text, axis width) goes into
  plain Kotlin functions with unit tests under `android/app/src/test/`. A
  test for new behavior fails on the phase's base commit and passes after.
- Keep accessibility labels that exist today (see `docs/QA-AUDIT-STATE.md`,
  accessibility batch). New controls get spoken labels. Layouts must stay
  usable at font scale 1.6 (`ListRow` stacks at 1.5x and up).
- Match the code style around you. Reuse `ui/components/Components.kt` and
  the theme; add shared pieces there or in a new file in `ui/components/`.
- Commit in small steps on `ui05-port`, messages like `UI05 P2: Today week
  strip as tiles`. No push. No AI attribution in commit messages.

## Checks (run from `android/`)

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
```

Baseline on `main` (dd23a3d): 56 tests, 0 failures. The phase is done when
this passes on its final commit, every contract row in the phase file is
met, `DESIGN.md` is updated, and the report lists: commits, the command
and the tail of its output, the test count, each contract row with the
file:line that meets it, and every place where you chose differently from
the Expo code and why.

Do not run an emulator or a device; the lead does one screenshot pass at
the end. Check the free disk before a build (stop below 30 GB).
