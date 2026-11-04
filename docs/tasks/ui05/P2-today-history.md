# UI05 P2: Today, History, Import

Class: Normal. Read `COMMON.md` in this folder first. Review follows
`~/.config/agents/review-protocol.md`. P1 added the shared kit (icon tile,
action row, tinted button, confirm sheet, no-results, content empty,
stepper repeat, 190 px chart); use it.

## Expo reference

- Today: `expo/src/app/(tabs)/index.tsx`, `expo/src/components/today/*`
  (`TodayCard`, `TodayRows`, `HomeRow`, `BodyweightSection`,
  `BodyweightEntry`), `expo/src/components/plan/WeekStrip.tsx`,
  `plan/colors.ts`, `plan/icons.ts`, `ui/RulerPicker.tsx`, `ui/EdgeFade.tsx`.
- History: `expo/src/app/(tabs)/history.tsx`, `expo/src/components/history/*`
  (`CalendarDay`, `CalendarGrid`, `MuscleVolumeList`, `DayDetail`,
  `ExerciseProgress`, `CardioTrendsSection`, `BodyweightHistory`).
- Import: `expo/src/app/import.tsx`, `expo/src/components/import/*`.
- Rules: `expo/docs/DESIGN.md` section "Today" (week strip, rows, item
  card, bodyweight), "Lists" (Import), History calendar text inside
  "Today".

## Kotlin targets

`ui/screens/TodayScreen.kt`, `ui/screens/HistoryScreen.kt`,
`ui/screens/ImportScreen.kt`, `ui/components/*`, `ui/AppViewModel.kt` only
for read-only state the new rows need (running time, sets done/total,
weight change since the last weighed day).

## Contract

| # | Action | Data | Expected |
|---|---|---|---|
| 1 | Open Today | any week | Week strip: 7 tiles 6 px apart on the group surface, radius 12; letter 12 muted, number 17, 7 px plan dot (modality color, accent for a program, none for rest); past days muted number, dot at 50 percent; today filled accent with white letter, bold number, white dot |
| 2 | Open Today | planned program, not started | Item card: 40 px symbol circle (dumbbell accent on accent 20 percent), name 22/700, muted line, Start 44 px |
| 3 | Open Today | session running | Line "In progress · 34 min" in accent; program adds the accent progress bar with "12 / 42"; button Resume |
| 4 | Open Today | done today | Green check symbol and the done line in green |
| 5 | Open Today | cardio / rest day | Runner in the modality color / moon |
| 6 | Open Today | routine, window, band sessions | One group of 64 px rows: 40 px symbol circle, name 17/600 over state 15 muted (green for good news). Stretch row "Done today · 10 min · 9 stretches" in green with a check once done; window "Open · closes in 3 h 20 m" green or "Closed · opens 12:00" gray; band "2 new sessions" with a red heart. A row with nothing to say is left out. Update row per `COMMON.md` |
| 7 | Read bodyweight | 30 days of entries | Line chart of the last 30 days, soft area fill, 190 px |
| 8 | Enter bodyweight | today | Own card (radius 20, 16 padding): value 34/700 with "kg" muted 15 (tap to type), change "+0.2 kg since Oct 5" muted 13, 42 px ruler picker to the card edges (ticks 22/15/9 px, 0.1 kg per 10 px, label each kg, accent center line, faded edges, haptic per tick, firmer on whole kg), tinted button "Add" / "Update" (today has another value) / "Saved" disabled (value equals today's entry) |
| 9 | Open History calendar | any month | Days drawn as the week-strip tiles (weekday letters in a row above), number over its dots, selected day on Surface3, other months' days no tile and dim, changed-plan mark top right |
| 10 | Open History | sets per muscle | Clearer bars per `MuscleVolumeList.tsx` |
| 11 | Open History | Import entry | Import as a round symbol button in the title bar |
| 12 | Open Import | sessions waiting | Source card (red heart tile, "Health Connect", "Last 60 days"); "3 new sessions from the band"; two-line rows (runner tile in the exercise color, workout type, target exercise on the right, Choose in accent; "Mon, Oct 5 · 32 min · 5.2 km · 141 bpm"); footnote; "Import 1 session" / "Import all 3 sessions" |
| 13 | Open Import | reading / all caught up / no access | Each a `ContentEmpty` card with its one action |
| 14 | Font scale 1.6 | Today and History | No clipped day numbers, rows stay readable (see the 0.4.8 large-text fixes) |

Unit test the pure text and state functions: Today row state lines
(window, stretch, band count, singular and plural), the weight change line,
the button label rule (Add / Update / Saved), and the import button label.

## Done

`COMMON.md` checks pass; root `DESIGN.md` "Today" and History parts
updated; report as `COMMON.md` says.
