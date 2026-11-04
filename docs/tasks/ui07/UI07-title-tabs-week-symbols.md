# UI07: History title tabs and Plan week symbols

Class: Normal. Review, tests and fixes follow `~/.config/agents/review-protocol.md`.

## Goal

The owner tested 0.6.0 on a Galaxy S25 and called two parts "horrifying":
the History Log / Progress switch (a 54 dp grey block under the title) and
the Plan "This week" grid (seven 46 dp tiles that cut names mid-word,
"Norwe gian …"). The owner picked a design for each (mockup:
`docs/tasks/ui07/mockup.html`, options C and A). Ship as 0.6.1 (versionCode 28).

Base: branch `ui07` from `main` (aba13b8, 0.6.0). Target screen 360 x 780 dp;
Robolectric already runs at that size.

## Contract

| # | Where | Rule |
|---|---|---|
| 1 | History | The big title is the switch: "Log" and "Progress" sit side by side in the large title row, in the large title's size and weight. The open one is `K.Text` with a 3 dp accent bar under it (as wide as the word, rounded). The closed one uses a muted color that passes 3:1 as large text on `K.Bg` (add the pair to `TextPairs`). No "History" word on screen, and no `Segmented` row. The import button stays at the right of the row. |
| 2 | History | Each word is a tap target of at least 48 x 48 dp. A tap opens that page; all page state rules stay as they are (each page keeps its scroll and saved state across page and tab switches). |
| 3 | History | TalkBack: the row is a heading spoken as "History"; each word is a tab (`Role.Tab`) with its selected state. |
| 4 | History | At font scale 1.6 the two words and the import button stay on one row with nothing cut: when the words do not fit, their size steps down until they fit (not below 22 sp). Compose BOM is 2024.12 (no `autoSize`): measure with `rememberTextMeasurer` or similar. |
| 5 | History | When the large title folds on scroll, the small bar title shows the open page's word ("Log" or "Progress"). Change `TabScreen` / `LargeTitleHeader` with a slot or overload; other tabs keep their look. |
| 6 | Plan | "This week" (title row with Edit stays) becomes a symbol strip: 7 columns, one per day Monday to Sunday. Each column: the one-letter day (M T W T F S S, 12 sp, `K.Muted`), under it a 40 dp circle (`K.Surface2`) with the day's symbol. |
| 7 | Plan | Symbol and color: gym program = `Icons.Dumbbell` in `K.Accent`; cardio = by its type id: `run` → `Icons.Runner`, `bike` → new `Icons.Bike`, `swim` → new `Icons.Swim`, any other or no type → `Icons.Heart`; the color is the type's own color (`Modality.color`), `K.Muted` for no type; rest = `Icons.Moon` in a gray that passes 3:1 on `Surface2`. Draw Bike and Swim as `ImageVector`s in `Icons.kt` in the style of the others. Every symbol color on `Surface2` passes 3:1: add each fixed pair to `TextPairs` and a test for the seeded type colors. |
| 8 | Plan | Today: its letter in `K.Accent` bold and a 2 dp accent ring around its circle. A day with a one-day change keeps the 5 dp accent dot, under the circle. |
| 9 | Plan | The strip spans the full screen width (no side padding, no card), so each column is 1/7 of the width (51 dp at 360) and the whole column is the tap target. A tap selects that day. The selected day (today at first) is shown with a filled `K.Surface3` circle, and one line under the strip reads "Wed · Rest", "Thu · Norwegian 4×4" (full name, wraps if long). The selection is not saved. |
| 10 | Plan | TalkBack: each column keeps the current spoken line (`WeekCell.spoken`), adds "selected" state, and has `Role.Tab` or a click label "Show name". |
| 11 | Plan | At font scale 1.6 the strip stays one row of seven (letters and symbols do not need the two-row wrap). Remove `WeekGrid`, `WEEK_GRID_ONE_ROW_MAX_SCALE` and `WEEK_CELL_NAME_SP` when nothing uses them. |
| 12 | All | versionName 0.6.1, versionCode 28. Update `DESIGN.md` (History header, Plan week) and `SPEC.md` where they describe these parts. |

## Rules

- Keep every other behavior of both screens. Update tests that used the old
  `Segmented` switch or the week grid; do not delete a test's intent.
- New behavior gets a Robolectric test: rows 2, 3, 4 (at font scale 1.6), 9, 10, 11, and the contrast pairs.
- Match the code style around you. No AI attribution in commits. No push.

## Check (from the repo root)

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ANDROID_HOME=~/Library/Android/sdk
android/gradlew -p android :app:testDebugUnitTest --rerun :app:assembleDebug :app:lintVitalRelease
```

Baseline at aba13b8: 635 tests, 0 failures, 14 skipped.

Report (under 60 lines): commits, the command and its output tail, the test
count, each contract row with the test or `file:line` that meets it, and
two Robolectric screenshots at 360 x 780 (History Progress, Plan) saved under
`/tmp/khon-ui07/`.
