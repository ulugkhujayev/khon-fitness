# UI05 P3: live session

Class: Normal. Read `COMMON.md` in this folder first. Review follows
`~/.config/agents/review-protocol.md`. P1 added the shared kit; use it.

## Expo reference

- `expo/src/app/session/[id].tsx`, `expo/src/components/session/*`
  (`SessionHeader`, `BlockCard`, `SetRow`, `SetDelta`, `SummarySheet`,
  `SessionDetails`, `SessionFooter`, `DeleteSessionSheet`, `SwapSheet`,
  `useBlockFlow`, `useBlockReveal`, `useFieldFocus`), `ui/EffortRow.tsx`.
- Rules: `expo/docs/DESIGN.md` section "Set row (live session)" (header,
  blocks), "Buttons" (Discard / Delete session capsule).

## Kotlin targets

`ui/screens/SessionScreen.kt`, `ui/components/*`, `ui/AppViewModel.kt`
for summary numbers only. The session data model and the repository stay
as they are; finishing still writes the same rows as today.

## Contract

| # | Action | Data | Expected |
|---|---|---|---|
| 1 | Open a running session | program with blocks | 56 px bar: Back, program name in Headline, Finish as the trailing capsule; under it the green progress bar with "12 / 42" muted 13 |
| 2 | Scroll the sets | any | Over the first 28 pt the progress row slides up under the bar and fades; a 2 px green progress line fades in on the bar's bottom edge; Back, Finish and progress stay in view |
| 3 | Tap Finish | any | "Workout done" sheet: time, sets done, volume, new bests, then effort and note (same values as the page's fields), then Save. Save writes what the old Finish wrote. There is no Finish button at the bottom |
| 4 | Open the session | first time | Only the first block with work is open |
| 5 | Tap a closed block header | another block open | It opens and the other closes, over 250 ms ease in-out: card grows, sets fade in from the top, chevron turns right to down, opened block scrolls to the top |
| 6 | Check a set | block has more pending sets | Block stays open, focus moves to the next pending set (else one left behind) |
| 7 | Check the block's last set | other blocks have work | The next block with work opens, wrapping to an earlier one |
| 8 | Check the last set of the workout | no work left | Every block closes |
| 9 | Read a block header | any | 52 px: index in accent, names, "2 / 6" sets done of total, chevron or a green check when done |
| 10 | Step kg or reps | set row | Stepper per P1 (tap, hold-repeat, one save on release); first sets start at 0 kg when there is no last time |
| 11 | Discard / delete | running or saved session | Red tinted capsule, full width, at the end of the page; asks first in a confirm sheet |
| 12 | Leave and return | running session | State is kept (open block, focus) as today; a deleted session leaves the screen (existing `KhonNav` rule) |

Unit test the block flow as a pure function (rows 4, 6, 7, 8 and the
wrap case) and the summary numbers (time, sets done, volume, new bests).

## Done

`COMMON.md` checks pass; root `DESIGN.md` "Set row (live session)" and
`SPEC.md` session finish updated; report as `COMMON.md` says.
