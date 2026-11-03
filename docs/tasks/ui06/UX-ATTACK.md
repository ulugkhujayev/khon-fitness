# UI06 UX attack on the prototype (Sol)

Step: attack. Class: Normal (see ~/.config/agents/review-protocol.md). Report only; change no repo file.

## Goal

The owner wants to rebuild three tabs of Khon Fitness (Android, Kotlin/Compose)
from a clickable HTML prototype. The owner says the prototype still has UX
problems. Find them before any app code is written. Be a harsh, specific UX
attacker: every finding must name the screen, the exact problem, why it hurts
a gym user, and a concrete fix the lead can put in a spec.

## Inputs

- Prototype: `file:///tmp/khon-ux-proto/index.html` (one page, vanilla JS, sample data).
  Open it in the shared Chrome with a context of viewport 360 x 780,
  device_scale_factor 3, is_mobile True, has_touch True (Galaxy S25).
- The owner's decisions (do not argue them, attack how they are drawn):
  - Today: no week strip; "Change" on the today card opens a one-day override
    sheet; the done card has only "Review" (read-only session view); bodyweight
    is a − / + stepper (0.1 kg, hold to repeat, tap to type) with Save; no chart on Today.
  - History: two tabs, Log and Progress. Log is a week view: a "X of Y planned
    done" card with a 7-part bar and totals, then one row per day.
    Progress shows personal records as the real heaviest weight (no
    estimated 1RM), sets per muscle, bodyweight, and cardio, in a new layout.
  - Plan tab (was Programs): one page by activity: this week, gym programs,
    cardio workouts (type Run/Bike/Swim/Row as a pill; no Modalities screen),
    stretch routines, library (exercises, stretches).
- Current app behavior and words: `SPEC.md`, `DESIGN.md`, and the 0.5.0 QA
  reports `/tmp/khon-qa-050/UX/REPORT.md` and `/tmp/khon-qa-050/NAIVE/report.md`.
  Do not repeat their findings unless the prototype still has them.
- Repo root: `/Users/mirzohidkhon/deepvision/personal/khon-fitness`.

## Do

1. Use every screen and flow: Today (Change, Start → log sets → Finish → Save,
   Review, bodyweight steppers incl. a 1.5 s hold and typing), History Log
   (all weeks back and forward, tap rows), History Progress (records, record
   detail, ranges), Plan (week edit, programs, cardio type change, library search),
   Settings, every sheet and Back path.
2. Try mistakes: double taps, Back mid-flow, empty or zero values, typing
   "83,5", leaving a workout and coming back, switching tabs mid-workout.
3. Judge against: one-hand gym use (48 dp targets, thumb zone), clarity of
   labels and numbers, hierarchy, consistency between tabs, state feedback,
   undo for destructive actions, what a first-time user understands.
4. Save screenshots under `/tmp/khon-ux-attack/` (create it).

## Output (under 90 lines)

- Verdict in one line: ready to spec, or needs a redesign of a named part.
- Findings ranked by user impact: severity (high/medium/low), screen, problem,
  fix, screenshot path. Mark each READ (seen) or GUESS (judgment).
- Missing states the spec must define (for example: no program yet, a missed
  day, two sessions on one day, an in-progress workout on another tab).
- Things that work and must stay.
