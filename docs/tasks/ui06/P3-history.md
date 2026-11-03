# UI06 P3: History tab (Log + Progress)

Class: Normal. Read `COMMON.md` first. Review follows `~/.config/agents/review-protocol.md`.
Builder: ui-builder. Prototype: `prototype.html`, History tab. Code: `UI06-COVERAGE.md` "History".

Targets: `ui/screens/HistoryScreen.kt` (split it into `HistoryLog.kt`,
`HistoryProgress.kt`, `ExerciseDetail.kt` if it helps), rules in plain Kotlin
files with tests, `ui/KhonNav.kt` for the new exercise detail route.

## Contract: Log

| # | Action | Data | Expected |
|---|---|---|---|
| 1 | Open History | — | Title, import button (Health Connect, as today), segmented Log / Progress (48 dp tall). Log is the default. Each page keeps its own scroll position, also across tab switches. |
| 2 | Week header | — | "‹ Nov 2 – 8, 2026 ›" with 48 dp arrows. ‹ stops at the week of the first session (or this week if none). › stops at this week. |
| 3 | Week card | this week | "<done> of <planned> planned done", right side "<n> to go" (this week) or "<n> missed" / "Full week" (past weeks). A 7-part bar: a done day in its item color, a planned day not done gray, a rest day near-black. Under it: "<sets> sets · <t> lifted · <min> min cardio · <km> km". Planned = the item each day had (past days frozen, overrides count). |
| 4 | Day rows, Mon-Sun | — | Day name and number on the left, a dot in the item color, name, stat line (gym: d of n sets · t · min; cardio: km · min · pace), "<k> PR" in green when the session set records, chevron. Today's row tinted. |
| 5 | Day states | — | Done (above); two sessions on one day = two rows; rest = "Rest" + "Stretch ✓ · 83.0 kg" when present; past planned not done = "Skipped · <item>"; today not started = "Today: <item>"; today running = "In progress · d of n sets" (opens the session); future = "Planned: <item>" dim. |
| 6 | Tap a done row | — | Opens the session from History (Edit available, as today). |
| 7 | Health Connect imported cardio | — | Shows as a cardio row with its source; counts in totals. |

## Contract: Progress

| # | Action | Data | Expected |
|---|---|---|---|
| 8 | Personal records | — | Title "Personal records · heaviest weight lifted". Rows: exercise, date of the record, "New" chip when set this week, "62.5 kg × 6" right. Five rows, then "All <n> exercises". Record = heaviest weight; tie on weight → more reps; tie on both → the earlier session keeps it. 0-rep sets never count. No estimated 1RM anywhere. |
| 9 | Tap a record or any exercise | — | Exercise detail: Best card ("62.5 kg × 6 · Oct 29"), chart "Heaviest set per session" (record points green), sessions newest first with every set "60×8 · 60×8 · 62.5×6" (record set green), each row opens the session. |
| 10 | Exercise with no sets | — | Exercise detail opens with name, muscle and "No sets yet". |
| 11 | Sets per muscle | — | "Sets per muscle · this week / last week": one row per muscle, two thin bars (this week accent, last week gray), "8 / 6" right. Legend. Empty: "No sets this week or last week." |
| 12 | Bodyweight | — | Latest value, change since the range start, range chips 1M / 3M / All (48 dp), line chart. A range with one point shows the point; with none: "No entries in this range". |
| 13 | Cardio, last 30 days | — | One row per type (color dot, type, "n sessions · km · h m · pace"), sparkline labeled "Distance per session" with the first and last value and dates. Swim pace per 100 m (keep today's swim rule). Heart-rate and interval charts from today's History move into a cardio type detail screen opened from the row. |
| 14 | First use | no data | Log shows the week with "Planned: …" rows and "0 of n planned done"; Progress shows one empty line per section, no broken charts. |

Remove the month calendar, `dayDots`, `progressFor` / `ProgressPoint` (e1RM)
and their tests when nothing else uses them; say what you removed.
