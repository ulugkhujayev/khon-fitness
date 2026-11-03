# UI06 P5: polish across the app

Class: Normal. Read `COMMON.md` first. Review follows `~/.config/agents/review-protocol.md`.
Builder: ui-builder. Starts after the lead merged P1-P4. Code: `UI06-COVERAGE.md` "Polish".

## Contract

| # | Action | Data | Expected |
|---|---|---|---|
| 1 | Any text or icon on an accent fill | Start, Save, chips, checks, segmented control | Dark ink on the accent; contrast 4.5:1 or more. A unit test computes the contrast of each token pair used for text in `Theme.kt` and fails below 4.5:1 (3:1 for 18 sp+ bold). |
| 2 | Placeholder and hint text | empty fields | Contrast 4.5:1 or more against the field fill (same test). |
| 3 | Session exercise name | — | A swap icon next to the name (48 dp target, label "Swap <name>") opens the existing swap sheet. A tap on the name also opens it. |
| 4 | Stretch demo: tap Skip | stretch 3 of 9 | Goes to stretch 4; a "Back" control returns to stretch 3 with its timer reset. Back is off on stretch 1. |
| 5 | Stretch routine count | — | "9 stretches · 10 min" (one stretch: "1 stretch"). Same wording everywhere it shows. |
| 6 | Field labels | font scale 1.0 and 1.6 | Labels in `Components.kt` field rows wrap to a second line, never cut or ellipsize. Compose test at 1.6 checks the full label text is shown. |
| 7 | Timer battery note | any phone | Neutral copy: "If the timer stops when the screen is off, allow Khon to run in the background in battery settings." with a button to the app's battery settings. No brand name. |
| 8 | Tap targets | every screen changed in UI06 | 48 x 48 dp or more. A Compose test per tab asserts `assertTouchHeightIsEqualTo`/min size on its icon buttons. |

List every screen you checked for rows 1, 2 and 8 in the report.

## Decision after the second P5 verify (2026-11-04)

The contrast guarantee is the pair table: `TextPairs` in `Theme.kt` lists every text or symbol color with the fill it
is drawn on, and `ContrastTest` holds each pair to 4.5:1 (3:1 for large bold text and meaningful symbols).
`ColorUseCheck` reads the screen sources and fails on a color that is not a pair's text color. It is a lint that
catches the common mistakes (raw tokens, aliases, split expressions, branches, parameters). A source scan cannot see
every way a color reaches a fill, so it is not the guarantee and its misses are not defects. A new fill or text
color needs a new pair in `TextPairs`.

Review's cardio "Time" row keeps its exact value ("3 min 40 s"): it is a detail value, not a minute count.
