# UI06 attack result (Sol, 2026-11-03)

Sol attacked the prototype (`prototype.html` in this folder) at 360 x 780 dp.
Evidence: `/tmp/khon-ux-attack/` (77 screenshots, `observations.jsonl`, `verification.txt`).
Verdict: "needs redesign of data entry, workout state, and protection of past history".
Every finding below is now a rule in a phase file (column "Phase").

| # | Sev | Finding | Rule | Phase |
|---|---|---|---|---|
| 1 | High | Bodyweight: typing `83,5` eats the first Save tap; blank/0 silently revert | Comma and dot both parse; one tap saves a valid typed value; invalid text stays with an error line, Save off | P2 |
| 2 | High | A week plan edit rewrote old weeks' "planned" counts | Week plan edits apply from today on; past days keep their planned item (the app freezes past days already: `KhonRepository.saveWeekPlan`; keep and test it) | P4 |
| 3 | High | Changing a cardio workout's type retyped old sessions | A type change asks: "Past sessions stay <old type>" is not possible without a schema change, so the editor shows "Changes the type of past sessions too" before Save (owner may ask for a snapshot later) | P4 |
| 4 | High | Discard workout: one tap, no confirm | Confirm sheet with the count of checked sets; Cancel is the default | P1 |
| 5 | Med | Exercise with no history does not open | Exercise detail opens with its info and "No sets yet" | P3 |
| 6 | Med | A 0-rep set turns green | Check needs reps > 0 and weight > 0 (or a bodyweight exercise); else the row shows "Enter reps" and stays open | P1 |
| 7 | Med | 1 of 12 sets saved shows "Done"; Review drops skipped sets | Done line "Done · 1 of 12 sets · …"; Review lists skipped sets as skipped | P2, P3 |
| 8 | Med | Log says "Not started" during a workout | Today's row in Log shows "In progress · d of n sets" and opens the session | P3 |
| 9 | Med | Review from Today shows Edit | Review from Today is read-only (no Edit); History keeps Edit | P2 |
| 10 | Med | Targets under 48 dp (Change, tabs, week arrows) | Every tap target 48 x 48 dp or more | all |
| 11 | Med | White on orange 2.06:1 | Dark label on accent fills (contrast 4.5:1 or more) | P5 |
| 12 | Med | Plan after a one-day change is unclear | The week grid marks a changed day; program rows say "Every Tue" | P4 |
| 13 | Med | Scroll position leaks between tabs | Each tab and each History page keeps its own scroll position | P3, P4 |
| 14 | Med | Cardio sparkline has no label or values | "Distance per session" label, first and last value with dates | P3 |
| 15 | Low | "Saved for today" wraps into the title | Status line under the title, on its own line | P2 |

Missing states every phase defines: first use with no program, no weight, no
records; empty chart range; two sessions on one day; partial completion;
record ties (same weight and reps: the earlier session keeps the record);
cardio Resume; system Back; keyboard dismissal; process restart; midnight;
a plan change during a workout.

Keep: the one-day Change scope and "Back to plan"; real-weight records; the
summary before Save; values kept after Back and Resume; 0.1 kg steps with
hold; one session from a double Save.
