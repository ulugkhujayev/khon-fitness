# TGT02-stale-date: no target date from old data, and no Android backup copy

Task-ID: TGT02-stale-date
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/tgt02
Base: 5567185
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/tgt02/docs/tgt02-tasks/TGT02-stale-date-REPORT.md

## Why

The owner saw "At +50.0 kg/week, Nov 27" for a lift whose newest session
was on Nov 3: the trend line had passed the target, and the app showed
today as the date. He decided on 2026-11-27: no date when the newest
session of that lift is older than 28 days, or when the trend says the
target is already passed; the row then says "No session since Nov 3".

He also decided that `android:allowBackup` goes to false, because all data
stays offline. The export to a file he picks stays.

The lead made both changes. This file is the review rubric.

## Scope and lead decisions

1. `liftForecast` returns `Forecast.Stale(newest)` when the newest valued
   session on or before today is more than 28 days before today. This
   check comes after "Reached" and before the trend line, so it also
   replaces the empty second row of a target whose sessions are all old.
2. `liftForecast` returns `Forecast.Passed(rate)` when the line is at or
   above the target today or on an earlier day and no session reached the
   target. The row shows the rate with "No date". Before, it showed today.
3. The bodyweight forecast does not change: it already needs entries of
   the last 7 days and of the 7 days before, and its date is never today.
4. The manifest sets `android:allowBackup="false"` and data extraction
   rules that leave out every domain for the cloud backup and for the
   device transfer (Android 12 and later ignore `allowBackup` for a
   device transfer).

No Room, DAO, backup file format, permission, or dependency change.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| S1 | Stale | Four sessions, the newest 28 days before today; the same with today one day later; a session dated after today next to them | 28 days: a date as before. 29 days: `Stale` with the newest date on or before today; the row reads "No session since Nov 11" with no value; TalkBack reads "No session since November 11". |
| S2 | Stale before the line | Three sessions, the newest 56 days before today (no line in the last 56 days) | `Stale`, not an empty second row. |
| S3 | Passed | The line reaches the target exactly today; the line reached it 7 days ago; the line reaches it tomorrow | Today and earlier: `Passed`, the row shows the rate and "No date", TalkBack reads "At plus 1.0 kilograms per week, no date". Tomorrow: that date. |
| S4 | Reached first | A session at or above the target on or after the entry date, older than 28 days | "Reached" with that session's date, as before. |
| S5 | Other states | No line, not at this rate, more than 2 years, the reps unit, the bodyweight target | Unchanged. |
| S6 | Backup off | The merged manifest; the rules file | `FLAG_ALLOW_BACKUP` is not set. Each of the nine domains is excluded with path "." in `cloud-backup` and in `device-transfer`; no include rule. The release lint passes. |
| S7 | Texts | `SPEC.md`, `DESIGN.md` | Both name the two new rows. |

## Definition of done

- Every contract row holds, each with a test that fails without the change.
- The `Tests:` command passes on the final commit.
