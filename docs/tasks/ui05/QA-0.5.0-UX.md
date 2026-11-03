# UX critique of 0.5.0 (Sol)

Step: check (UX critique on a real device). Report only; change no repo file.

## Goal

Khon Fitness 0.5.0 took its look from the Expo app. Three other agents hunt
functional bugs. You judge the experience: is each screen clear, fast to
use with one hand in a gym, and consistent with the design rules? Report what
a careful product designer would flag. Do not report crashes or data bugs
unless you hit them by chance.

## Inputs

- APK: `/private/tmp/claude-501/-Users-mirzohidkhon-deepvision-personal-khon-fitness-tools/de079724-56e9-4d80-a89f-647336d0de5a/scratchpad/khon-0.5.0-sizecut.apk`
  (package `dev.mirzohidkhon.khonfitness`).
- Design rules: `DESIGN.md`. Product intent: `SPEC.md` ("Why" and "Navigation").
- Repo root: `/Users/mirzohidkhon/deepvision/personal/khon-fitness`.

## Device rules

Same as `docs/tasks/ui05/QA-0.5.0-SOL.md`, "Device rules", with deviceId
`ui05-api28-d`, hostId `local`, serial `emulator-5560`. If `device_open` reports
a different serial, use that one. Never touch another serial.

Start: uninstall any old copy, install the APK, launch. Use the app as the
owner does: set up a program, run a gym day, a cardio day, the stretch
routine, the eating window, look at History and Settings.

## What to judge

- First run: does a new user know what to do in 10 seconds?
- Hierarchy: the main action on each screen is obvious; secondary actions recede.
- Gym use: tap targets 48dp or more, thumb reach, number entry speed,
  how many taps to log a set, readability at arm's length.
- Feedback: every tap shows a result; saves, deletes, and errors are clear;
  destructive actions confirm.
- Consistency with `DESIGN.md`: spacing, type scale, colors, icons, wording,
  headers, sheets, empty states.
- Text: labels plain and short; no truncation; font scale 1.3 still works.
- Motion and speed: anything that feels slow or janky.

## Output

Save screenshots in `/tmp/khon-qa-050/UX/` (create it). Report, under 80 lines:

- Top 5 issues first, ranked by user impact, each with screen, what is wrong,
  a concrete fix, and a screenshot path.
- Then other findings, one line each, tagged high / medium / low.
- Where code is the cause, give `file:line` (READ or GUESS).
- Three things that work well and must stay.

Do not edit, commit, or push anything in the repo. Leave the emulator booted.
