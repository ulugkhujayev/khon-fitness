# UI06 P2: Today tab

Class: Normal. Read `COMMON.md` first. Review follows `~/.config/agents/review-protocol.md`.
Builder: ui-builder. Prototype: `prototype.html`, Today tab. Code: `UI06-COVERAGE.md` "Today".

Targets: `ui/screens/TodayScreen.kt`, `TodayParts.kt`, `TodayRules.kt`,
`ui/components/*` (new stepper input), session Review entry. Do not touch
the session screen's internals (P1 changes them).

## Contract

| # | Action | Data | Expected |
|---|---|---|---|
| 1 | Open Today | any | No week strip. Order: title + gear, today card, rows (update row when present, stretch routine, eating window, pending Health Connect import), bodyweight card. |
| 2 | Today card, planned gym or cardio day | not started | Icon, name, one line (gym: exercises · sets; cardio: type · plan), "Change" link (48 dp target) top right, Start button. "changed for today" in the line when an override exists. |
| 3 | Tap Change | — | Sheet "Today, <Wkd> <Mon d>" with "Only today changes. The week plan stays." Groups Gym, Cardio, Other(Rest), check on the current item. Pick writes the existing one-day override (`saveOverride`). When an override exists: "Back to plan: <item>" removes it. Change is hidden while a session runs or after today's session is saved. |
| 4 | Rest day | no session | "Rest day" card with Change, no Start. |
| 5 | Session running | started, not finished | "In progress · d of n sets" (gym) or the timer state (cardio), Resume. No Change. |
| 6 | Session saved today | finished | Check icon, name, "Done · d of n sets · 3.1 t · 58 min" (cardio: km · min · pace). When d < n the line says "d of n sets". One button "Review". No "Start again". |
| 7 | Tap Review | — | Opens the saved session read-only: no Edit button on this path. History's entry keeps Edit. Skipped sets show as skipped, not hidden. |
| 8 | Two sessions today | both finished | The card shows the last one; a line "+1 more today" opens History Log at this week. |
| 9 | Bodyweight card | — | Title "Bodyweight"; under it a status line on its own line: "Last: 83.4 kg · Oct 30" or "Saved for today · −0.4 kg since Oct 30". No chart on Today. |
| 10 | Tap − / + | 83.0 | Steps 0.1 kg; hold repeats (existing stepper repeat timing in `ui/components`); 52 dp square buttons; range 20-250. |
| 11 | Tap the number | — | Opens the decimal keyboard. `83,5` and `83.5` both parse. A valid typed value enables Save at once; one tap on Save saves it (no lost first tap). Keyboard Done also saves. |
| 12 | Invalid text | blank, 0, 19, 251, `abc` | Text stays; line "Enter 20-250 kg"; Save off. Nothing reverts silently. |
| 13 | Save states | — | "Save" (no entry today), "Update" (today's entry differs), "Saved" (off) when equal. |
| 14 | First use | no program, no bodyweight | Card says "No plan for today" with "Set up your week" (opens Plan); bodyweight shows "No entry yet" and the stepper starts at 75.0. |
| 15 | Midnight passes with Today open | — | Today switches to the new date on resume (existing minute clock). |

Remove the ruler picker from Today. If `RulerPicker` has no other caller
after this phase, delete it with its tests (`RulerRulesTest`,
`RulerMotionTest`) and say so in the report.
