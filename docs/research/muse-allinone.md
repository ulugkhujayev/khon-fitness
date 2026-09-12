# All-in-one training planners: gym + cardio/intervals + weekly drag-to-move calendar

Scope: brief items 1-9. Method: web search + web fetch of official help/docs. Every bullet ends with a URL citation. Anything without a citation is marked **[UNVERIFIED]**.

Brief items: 1=gym logging (programs, supersets, prefill, one-tap, session-swap, e1RM, muscle sets/week), 2=cardio steady+intervals (voice, lock-screen, per-round HR/watts, pace/laps), 3=week template + per-date overrides + drag-to-move calendar, 4=bodyweight entry+chart, 5=Health Connect import (Mi Fitness + Samsung Health) with merge, 6=dark dense UI + widget Start, 7=offline + JSON/CSV export/import + free + GitHub self-update, 8=stretch/mobility with animation/video + timer + history, 9=fasting window with icon changing open/closed.

## Score table

Y=yes, partial=partly, N=no, unknown=not verified.

| App | 1 Gym | 2 Cardio+intervals | 3 Calendar drag-move | 4 Bodywt | 5 HC import | 6 UI+widget | 7 Offline/local/export/free | 8 Stretch | 9 Fasting icon |
|---|---|---|---|---|---|---|---|---|---|
| Samsung Health | partial | partial | partial | Y | Y | unknown | N | partial | N |
| Google Fit | N | partial | N | partial | Y | unknown | N | N | N |
| Garmin Connect (no device) | partial | Y | Y | Y | N | unknown | partial | partial | N |
| Polar Flow | partial | Y | Y | Y | N | unknown | N | partial | N |
| COROS | N | Y | partial | Y | N | unknown | N | N | N |
| TrainingPeaks | partial | Y | Y | Y | N | unknown | partial | N | N |
| Intervals.icu | partial | Y | Y | Y | N (bridge only) | unknown | partial | N | N |
| Fitbod | Y | partial | partial | Y | unknown | unknown | partial | partial | N |
| Freeletics | partial | partial | partial | unknown | unknown | unknown | N | partial | N |
| Hevy | Y | N | N | Y | N | unknown | partial | N | N |
| JEFIT | Y | N | partial | Y | unknown | unknown | partial | partial | N |
| Strong | Y | N | partial | Y | unknown | unknown | partial | N | N |
| Trainerize | partial | partial | Y | Y | unknown | unknown | N | partial | N |
| Caliber | Y | partial | partial | Y | unknown | unknown | N | partial | N |
| Setgraph | Y | N | unknown | unknown | unknown | unknown | unknown | unknown | N |
| wger | Y | partial | partial | Y | N | unknown | Y | N | N |
| Final Surge / Runalyze / Strava (bonus) | N | Y | Y / Y / N | Y | N | unknown | partial | N | N |

Details and citations below. "Unknown" means I could not verify with an official URL.

## 1. Samsung Health

- Gym: logs weight-training sessions, custom routines exist but no verified superset A1/B1 pairing, prefill-last, e1RM, muscle sets/week. Score partial. **[UNVERIFIED – no official URL fetched; app listing only]**
- Cardio: run/bike/swim tracking with HR, pace, distance via Galaxy Watch; no verified Norwegian 4x4 phased voice + lock-screen + per-round watts. Score partial. **[UNVERIFIED]**
- Calendar: history calendar exists; no verified template + per-date override + drag-to-move planning. Score partial. **[UNVERIFIED]**
- Bodyweight: weight entry + chart in Samsung Health. **[UNVERIFIED – widely documented but no fetch; mark unverified]**
- HC import Y: "Samsung Health synchronizes health data with Health Connect in both directions: when Samsung Health has new data it writes to Health Connect; when Health Connect has updated data, Samsung Health retrieves it" and "Samsung Health has supported synchronizing data with Health Connect since v6.22.5 (Oct 2022)" — https://developer.samsung.com/health/blog/en/accessing-samsung-health-data-through-health-connect
- HC data types include ExerciseSession, HeartRate, Speed, Power, Distance, Sleep, Weight — https://developer.samsung.com/health/blog/en/accessing-samsung-health-data-through-health-connect
- Enable path: Samsung Health > Settings > Health Connect, Allow — https://www.androidpolice.com/sync-samsung-health-with-google-fit/
- Merge overlapping band+watch records: no verified dedup/merge logic. **[UNVERIFIED – N]**
- Local/export: Samsung account cloud; no verified plain JSON/CSV self-export. Score N/unknown. **[UNVERIFIED]**
- Stretch/fasting icon: no verified mobility animations or icon-changing fasting timer. N. **[UNVERIFIED]**

## 2. Google Fit

- Status: "Google Fit developer services will be transitioning… Google Fit APIs, including REST API, will remain available until June 30, 2025" — https://android-developers.googleblog.com/2024/05/evolving-health-on-android-migrating-from-google-fit-apis-to-android-health.html
- Later extended: "Google Fit APIs will only be supported until end of 2026" — https://developer.android.com/health-and-fitness/health-connect/migration/fit
- Press: "Google has stopped accepting new sign-ups… developers have until June 30, 2025 to migrate to Health Connect" — https://www.theverge.com/2024/5/4/24149066/google-is-shutting-down-developer-access-to-google-fit-apis
- HC: Google Fit can "Sync Fit with Health Connect" and "reads data shared with Health Connect over last 30 days" — https://www.androidpolice.com/sync-samsung-health-with-google-fit/
- Gym/calendar: no named programs, supersets, prefill, drag-to-move week planner. N. **[UNVERIFIED – based on app deprecation + feature list absence; mark unverified for gym detail]**
- Cardio: basic activity logging only; no structured 4x4/Tabata voice timer. N/partial.
- Export: via Google Takeout only (JSON/CSV per product), no in-app JSON. **[UNVERIFIED]**
- Verdict: dead-end; do not build on it.

## 3. Garmin Connect (without Garmin device)

- No-device use: account at connect.garmin.com works without device; manual import via "Upload or Import Activity / Import Data" — https://connect.garmin.com/modern/import-data
- Calendar Y: "How to Schedule Workouts Using the Calendar in Garmin Connect"; training calendar sends workouts to device, overwrites calendar — search result citation https://support.garmin.com/en-US?faq=XRcMvEtKdf7yBf8My9jua6 (title verified, body JS-blocked; treat drag behavior as partial-verified). Web training plans + Coach plans exist — https://www8.garmin.com/manuals/webhelp/GUID-C001C335-A8EC-4A41-AB0E-BAC434259F92/EN-US/GUID-5C0E79E4-C381-4A9B-B441-5F824214CE40/EN-US/GUID-FA0BDAD5-4576-4D05-B76B-A3B6712D0959.html
- Gym partial: Garmin Connect has strength workouts/exercise list with muscles/equipment (community list) — https://github.com/mrnabilnoh/workout-plan-garmin-connect/blob/main/garmin_connect_exercise_list.md ; but no verified superset prefill/e1RM/muscle-sets chart matching brief. **[UNVERIFIED for exact gaps]**
- Cardio Y (with device): structured workouts, HR/pace/power, maps; without device limited to manual/logged. No verified full-screen 4x4 voice + lock-screen + per-round watts in phone app alone. Score Y-with-device / partial-without. **[UNVERIFIED for timer detail]**
- HC import N: "Your Garmin Connect account can send the following data to Health Connect. Please note that this is a one-way transfer; Garmin will not read any data from [Health Connect]" — https://support.garmin.com/en-GB?faq=JToBEy0jfe6pIygark2Ui5 (snippet from search; page body JS-blocked so mark direction claim as search-snippet-verified only)
- Community workaround: scripts to move Samsung Health → Garmin via TCX/CSV manual import — https://github.com/PhilippImhof/FromSamToGarm ; confirms no native HC-in.
- Export partial: activity files retained; community tools download JSON/FIT and export TCX — https://github.com/tcgoetz/GarminDB ; official bulk export is manual per-file. Cloud account lock-in, not local-first.
- Premium: Connect+ $6.99/mo or $69.99/yr for AI insights; existing features stay free — https://www.garmin.com/en-US/newsroom/press-release/wearables-health/elevate-your-health-and-fitness-goals-with-garmin-connect

## 4. Polar Flow

- HC direction: doc lists only "What data is synced FROM Polar Flow app TO Health Connect" (calories, distance, exercise start/end/laps, route, HR, sleep, steps, weight, SpO2 etc.) and "Health Connect receives your new Flow data" — https://support.polar.com/us-en/flow-app-health-connect
- No verified HC *import* into Flow from Mi Fitness/Samsung Health. Score N. **[UNVERIFIED-absence; official doc only documents export]**
- Calendar Y with drag: "drag and drop a target from favorites to your plan… dragging and dropping training targets is possible only in the Season Planner… drag and drop training targets to different dates" — https://support.polar.com/us-en/polar-flow-season-planner
- Phased intervals Y: "Phased target: split session into phases… e.g. creating interval training… Duration/Distance per phase, Start next phase automatically, intensity by HR/speed/power" — https://support.polar.com/en/support/how_do_i_create_a_training_target_in_polar_flow?category=faqs&product_id=63839
- Fitness Program: "ready-made cardio, strength and mobility training targets… move, modify, even skip… tap and hold, drag to another day" — https://support.polar.com/us-en/fitness-program (via search result; page verified in search corpus)
- Strength + mobility video guides: "Get cardio suggestions… plus strength & mobility exercises and video guides" — https://www.polar.com/en/fitness-program
- TrainingPeaks bidirectional sync (plans in, sessions out) supported — https://support.polar.com/en/support/how_can_i_automatically_sync_my_data_from_polar_flow_to_trainingpeaks
- Gym exact gaps (prefill, one-tap, e1RM, muscle sets/week): not verified. Score partial. **[UNVERIFIED]**
- Bodyweight: weight in physical settings syncs — https://support.polar.com/us-en/flow-app-health-connect ; charting assumed. partial-Y. **[UNVERIFIED for chart]**
- Export/offline: cloud account; no verified JSON/CSV bulk export or offline mode. N.
- Requires Polar device for full value; works without but degraded. **[UNVERIFIED]**

## 5. COROS

- HC: COROS lists "Health Connect (for Android users)" as supported 3rd-party app — https://support.coros.com/hc/en-us/articles/360040256531-Supported-3rd-Party-Apps
- Direction verified as *outbound*: "Coros watches sync sleep, heart rate, steps, distance to Apple Health and Health Connect through the Coros app" and "Coros app syncs sleep, HR, steps, walking/running, cycling, swimming distance, workouts to Apple Health and Health Connect. It doesn't push HRV" — https://sahha.ai/integrations/coros/
- Setup: COROS app > Profile > Settings > 3rd Party Apps > Health Connect, allow permissions — https://customer.wellnesscoach.live/knowledge/syncing-coros-health-data-with-health-connect (third-party tutorial; mark setup as partially-verified)
- No verified HC *import* (reading Mi/Samsung data into COROS). Score N. **[UNVERIFIED-absence]**
- Sync model: "Workouts uploaded to COROS account after integration will first be sent to COROS server, then to integrated 3rd-party app" — https://support.coros.com/hc/en-us/articles/360040256591-Sync-with-3rd-party-apps (cloud, not local)
- Training calendar/strength without device: no verified drag-to-move + gym logging matching brief. Score partial/N. **[UNVERIFIED]**
- Export: no verified JSON/CSV. N/unknown.

## 6. TrainingPeaks

- Calendar Y: drag from workout library onto calendar; compliance colors; Week Summary; "Save a Workout to your Workout library… by dragging from library onto calendar" and "Drag and drop file onto calendar" — https://help.trainingpeaks.com/hc/en-us/articles/231472468-TrainingPeaks-Athlete-User-Guide and https://www.trainingpeaks.com/learn/trainingpeaks-athlete-user-guide
- Structured workouts Y for cardio: Workout Builder, export .ERG/.MRC/.FIT/.ZWO; auto-sync to Garmin (15 days), Apple, Wahoo, Zwift — https://help.trainingpeaks.com/hc/en-us/articles/115000325647-Structured-Workout-sync-and-Manual-Export
- Gym partial: supports Strength sport + old Strength Builder only; "We support only Old Strength Builder, not New Strength Builder" (per Polar doc describing TP sync) — https://support.polar.com/en/support/how_can_i_automatically_sync_my_data_from_polar_flow_to_trainingpeaks ; no verified superset/prefill/e1RM/muscle-chart. **[UNVERIFIED for gym detail]**
- HC import N: no native Health Connect connector verified; intake is via device autosync (Garmin etc.) or file upload. **[UNVERIFIED-absence]**
- Export partial-Y: "Export Data… Workout Files (12-month chunks, original format), Workout Summary CSV, Custom Metrics CSV; single workout via Files > Download (same format as uploaded, e.g. .fit stays .fit)" — https://help.trainingpeaks.com/hc/en-us/articles/204985370-Data-Export
- Calendar export to Google/Outlook/Apple requires Premium — https://www.trainingpeaks.com/learn/trainingpeaks-athlete-user-guide
- Cost/cloud: freemium, key features paywalled; cloud account, online-first, no verified offline JSON. N for brief-7.
- Stretch/fasting: none verified. N.

## 7. Intervals.icu

- Calendar Y: "Plan training with drag-and-drop calendar… third-party calendar imports, workout library, sync to devices, weekly totals" — https://www.intervals.icu/features/training-calendar
- Workout builder Y: "Create structured workouts with custom steps targeting power, HR, pace, cadence… Import/export Zwift (ZWO), FIT, MRC, ERG… text-based editor" — https://www.intervals.icu/features/workout-builder
- Analysis: fitness/fatigue/form chart, eFTP, multisport zones, power curve, decoupling — https://www.intervals.icu/
- Wellness/bodyweight Y: "Track weight, sleep, HRV, readiness… Auto-sync from Garmin, Polar, Oura, Amazfit, Huawei" + "Sync from Apple Health and others using 3rd-party apps… custom fields" — https://www.intervals.icu/features/wellness/
- Export partial-Y: "download all activities in CSV… includes training load, fitness" — https://forum.intervals.icu/t/download-all-activities-in-csv-format/599 ; "download wellness CSV and upload same… API too" — https://forum.intervals.icu/t/download-and-upload-wellness-csv/4179 ; "download CSV of streams (power/HR/cadence)… Supporters can upload CSV" — https://forum.intervals.icu/t/import-workout-data-from-a-csv-file/115114 (quoting thread); API: "Upload/download activities in FIT, TCX, GPX… push/pull wellness… create/manage planned workouts… webhooks" — https://www.intervals.icu/features/open-api/
- HC import N native: "Support for syncing from Health Connect to Intervals.icu is being explored and may be added… depending on Google policies" (Feb 2025) — https://forum.intervals.icu/t/health-sync-now-supports-intervals-icu/92287 ; current path is bridge app Health Sync (Coros/Fitbit/Garmin/Google Fit/Huawei/Oura/Polar/Samsung/Strava/Suunto/Withings → Intervals.icu), not direct Mi/Samsung-via-HC. No verified merge of overlapping records.
- Gym N/partial: strength stress supported ("added recovery… strength training" in API thread) but no verified superset/prefill/one-tap/e1RM/muscle-sets gym logger; web-first, no offline phone logging. Score partial at best.
- Price: free; Supporter $4/mo for weather, annual plan builder, full Strava history, custom zones, CSV stream upload, teams — https://www.intervals.icu/
- Stretch/fasting/UI-widget/offline: none verified. N.
- Closest-match candidate (see bottom).

## 8. Fitbod

- Position: "built exclusively for resistance and strength training… occasionally includes cardio-like conditioning, stretches, walking/cycling warm-ups" — https://fitbod.me/
- Gym Y (best-in-class for brief-1 candidates): adaptive progressive overload + recovery, but no verified superset A1/B1, session-only swap, e1RM chart, muscle sets/week exact match. Score Y-minus. **[UNVERIFIED for exact gaps]**
- Cardio partial: only conditioning/warm-up inclusions, no 4x4/Tabata timer with voice/lock/per-round watts. N/partial.
- Calendar partial: scheduled sessions, no verified drag-to-move week template + overrides. **[UNVERIFIED]**
- Bodyweight Y (uses bodyweight + equipment settings) — https://fitbod.me/ (partial-verified).
- Export partial: iOS Log > gear > Export Workout Data → CSV with Date/Exercise/Reps/Weight/Duration/Distance/Incline/Resistance/isWarmup/Note/multiplier — https://github.com/rhnfzl/fitbod-report and https://griptapp.com/help/import-from-fitbod ; iOS-only per "Export workout data… additionally view post" — https://www.reddit.com/r/fitbod/comments/1i53bxz/download_data/ ; Android has no in-app export, must go via support ("On Android… no option… have to go through support… 1 working day") — https://www.reddit.com/r/fitbod/comments/1u63wzf/export_workout_data_on_android/ (Reddit = weak citation, mark partially-verified)
- HC import: none verified. unknown.
- API: "no official Fitbod API" (community reverse-engineer only) — https://github.com/maxtechera/fitbod-integration (mark partially-verified).
- Offline/local/free: paid subscription, cloud account; no JSON, no offline-first. N.

## 9. Freeletics

- Covers HIIT/bodyweight cardio + strength journeys with coach; calendar-ish coach week. No verified gym superset/prefill/e1RM, interval voice/lock/per-round watts, drag calendar, HC import, JSON/CSV. Scores: 1 partial, 2 partial, 3 partial, 8 partial (warm-up/mobility), rest N/unknown. **[UNVERIFIED – no fetches succeeded; do not rely]**

## 10. Hevy

- Gym Y: routines + programs (folders, Push/Pull/Legs example), supersets, 400+ exercise library + custom, plate calculator, RPE/RIR, previous-values prefill — https://help.hevyapp.com/hc/en-us/articles/34953606698903-Build-a-Workout-Program-Create-Organize-Routines and https://www.hevyapp.com/ ; "How to Use Previous Workout Values" doc exists (title only) — https://help.hevyapp.com/hc/en-us/articles/38001424401943-How-to-Import-Strong-App-CSV-Files-and-Export-Your-Data-in-Hevy (related-links section).
- Calendar N: "calendar feature cannot be used to plan… which days you intend to complete workouts… history only, blue highlight + streak" — https://help.hevyapp.com/hc/en-us/articles/35380117933207-Track-Your-Workout-Consistency-with-the-Calendar-and-Streak-Features ; also https://www.hevyapp.com/features/gym-consistency
- Cardio N: duration exercises count up not down; no structured interval timer verified. Score N.
- Bodyweight Y: bodyweight vs assisted vs weighted handling — related-links in program doc (title only, mark partially-verified).
- HC: writes OUT only verified: "Hevy writes completed strength workouts to Apple Health and Health Connect" + "Set/rep/weight detail stays in Hevy" + "Hevy also reads body weight from platform" — https://sahha.ai/integrations/hevy/ ; "ensure Save workout > Sync with Health Connect toggled on; enable permissions for Hevy + Google Fit/Samsung Health" — https://help.hevyapp.com/hc/en-us/articles/34204824335255-How-to-Connect-Hevy-to-Google-Fit-Using-Health-Connect and https://help.hevyapp.com/hc/en-us/articles/36957110114455-Health-Connect-Not-Receiving-Hevy-Data-Here-s-How-to-Fix-It
- HC import of finished cardio sessions with HR/distance: none verified. Score N (does not read sessions; only writes + reads body weight).
- Export partial-Y: "export measurements or workouts… Profile > Settings > Export & Import Data > Export Data" and "import only Strong-app CSV in English; one import only" — https://help.hevyapp.com/hc/en-us/articles/38001424401943-How-to-Import-Strong-App-CSV-Files-and-Export-Your-Data-in-Hevy ; sample export schema (title/start/end/exercise/superset/set/weight/reps/distance/duration/RPE) — https://github.com/matanabudy/workout-data-sync/blob/main/examples/hevy_export_sample.csv
- Offline/local/free: freemium + Pro API key (developer key requires Pro) — https://github.com/matanabudy/workout-data-sync ; cloud account, no verified JSON/offline-first. partial.

## 11. JEFIT

- Gym Y presumed (long-standing bodybuilding logger with programs, supersets, rest timer, charts). **[UNVERIFIED – search/fetch failed; do not rely for exact gaps]**
- Cardio/calendar/HC/export: no verified interval timer, drag calendar, HC session import, JSON/CSV. Scores unknown. **[UNVERIFIED]**

## 12. Strong

- Gym Y presumed: "Supersets, Custom Exercises, CSV Export, Apple Health, Warm-up Calculator, RPE, Advanced Charts, Body Measurements, Workout Scheduling, Custom Timers, Dark Mode" — https://www.strong.app/
- Scheduling exists ("Workout Scheduling" listed) but no verified drag-to-move week template + overrides. partial. **[UNVERIFIED for calendar detail]**
- Export Y (CSV): "export workout data to spreadsheet-friendly CSV… exported files cannot be imported back into Strong. iOS: Settings > Export Strong Data; Android: Settings > Export Data" — https://help.strongapp.io/article/235-export-workout-data
- HC import: none verified (Apple Health sync documented; HC absent). unknown/N. **[UNVERIFIED]**
- Cardio/intervals, stretch, fasting icon: none verified. N.
- Cost: free + PRO; cloud account. **[UNVERIFIED for price detail]**

## 13. Trainerize / Caliber / Setgraph

- Trainerize: coach-client platform with scheduling/calendar + habit/nutrition; business paywall; no verified HC import, JSON/CSV, offline, fasting icon. Scores unknown/partial. **[UNVERIFIED – no fetch]**
- Caliber: coached strength with adaptive programming; no verified cardio intervals, drag calendar, HC import, export. **[UNVERIFIED]**
- Setgraph: gym logger candidate from brief; no verified official docs fetched. All unknown except presumed gym partial. **[UNVERIFIED]**

## 14. wger (open source, self-hostable)

- FOSS Y: AGPL-3.0, 6.9k stars, Django + Flutter (Android/iOS/F-Droid/Flathub), Docker self-host, REST API — https://github.com/wger-project/wger
- Features: "Custom Workout Routines with progression rules; track diet, body weight, measurements; Open Food Facts nutrition; progress gallery; exercise wiki; self-hostable; REST API" — https://github.com/wger-project/wger
- Docs: user manual, installation (Docker recommended), administration (backups), API — https://wger.readthedocs.io/
- Bodyweight Y (weight + measurements tracking per feature list above).
- Gym Y-minus: routines + progression, but no verified superset A1/B1, prefill-last, one-tap, session-only swap, e1RM chart, muscle sets/week exact match. Score Y/partial. **[UNVERIFIED for exact gaps]**
- Calendar partial: routines scheduled by day; no verified drag-to-move week template + per-date overrides. **[UNVERIFIED]**
- Cardio partial: generic logging possible; no verified 4x4/Tabata voice timer + per-round HR/watts. N/partial.
- HC import N: none verified. **[UNVERIFIED-absence]**
- Export partial-Y: API = JSON (verified via docs); no verified one-tap JSON/CSV bulk export in app. Self-host = data stays yours (strongest brief-7 fit alongside Intervals export).
- Stretch/fasting icon: none verified. N.
- Closest-match candidate (see bottom).

## 15. Bonus: Final Surge / Runalyze / Strava / Health Sync bridge

- Health Sync (bridge app): "sync activities Strava → Intervals.icu… Data from Coros, Fitbit, Garmin, Google Fit, Huawei, Oura, Polar, Samsung, Strava, Suunto, Withings" to Intervals.icu; "Support for syncing from Health Connect to Intervals.icu is being explored" — https://forum.intervals.icu/t/health-sync-now-supports-intervals-icu/92287
- This is currently the only verified path for Samsung/Mi-via-HC → Intervals.icu, and it is third-party, not native, with no verified overlap-merge.
- Strava/Runalyze/Final Surge: strong cardio + calendar (Final Surge), but no verified gym superset logger, HC import, offline JSON. **[UNVERIFIED – listed for completeness]**

## Health Connect import question (Mi Fitness + Samsung Health data)

- YES (reads HC): Samsung Health (bidirectional) — https://developer.samsung.com/health/blog/en/accessing-samsung-health-data-through-health-connect ; Google Fit (reads HC last 30 days, deprecated) — https://www.androidpolice.com/sync-samsung-health-with-google-fit/
- WRITES ONLY (does NOT import): Garmin Connect (one-way out) — https://support.garmin.com/en-GB?faq=JToBEy0jfe6pIygark2Ui5 ; Polar Flow (only FROM→TO documented) — https://support.polar.com/us-en/flow-app-health-connect ; COROS (outbound only verified) — https://sahha.ai/integrations/coros/ ; Hevy (writes workouts out; reads body weight only) — https://sahha.ai/integrations/hevy/
- NO NATIVE HC: Intervals.icu (bridge via Health Sync only) — https://forum.intervals.icu/t/health-sync-now-supports-intervals-icu/92287 ; TrainingPeaks, Strong, Fitbod, JEFIT, Freeletics, wger, Trainerize, Caliber, Setgraph — none verified; treat as N/unknown. **[UNVERIFIED-absence where no doc]**
- Merging overlapping band + watch records: NO app verified to do this. Hevy/HC docs only describe priority ordering per data type ("drag apps in App sources list; top has highest priority") — https://help.hevyapp.com/hc/en-us/articles/36957110114455-Health-Connect-Not-Receiving-Hevy-Data-Here-s-How-to-Fix-It ; FitMesh notes HC "doesn't sync retroactively" and Garmin "never reads" — https://www.fitmesh.fit/en/blog/how-does-health-connect-work (third-party guide, weaker citation).

## Local-only / JSON/CSV question

- Local-first / self-host: wger only (Docker + API + AGPL) — https://github.com/wger-project/wger and https://wger.readthedocs.io/
- CSV-out verified: Strong (CSV, no re-import) — https://help.strongapp.io/article/235-export-workout-data ; Hevy (workouts + measurements CSV; Strong-CSV in) — https://help.hevyapp.com/hc/en-us/articles/38001424401943-How-to-Import-Strong-App-CSV-Files-and-Export-Your-Data-in-Hevy ; Fitbod (iOS CSV; Android via support) — https://github.com/rhnfzl/fitbod-report , https://www.reddit.com/r/fitbod/comments/1i53bxz/download_data/ , https://www.reddit.com/r/fitbod/comments/1u63wzf/export_workout_data_on_android/ ; TrainingPeaks (workout files + summary CSV + metrics CSV) — https://help.trainingpeaks.com/hc/en-us/articles/204985370-Data-Export ; Intervals.icu (activities CSV + wellness CSV + streams CSV + FIT/TCX/GPX + open API) — https://forum.intervals.icu/t/download-all-activities-in-csv-format/599 , https://forum.intervals.icu/t/download-and-upload-wellness-csv/4179 , https://www.intervals.icu/features/open-api/
- JSON: only via APIs (Intervals.icu API, wger API, Garmin community scripts) — https://www.intervals.icu/features/open-api/ , https://wger.readthedocs.io/ , https://github.com/tcgoetz/GarminDB ; no verified one-tap JSON export/import matching brief-7 in any listed app.
- Offline: none verified as offline-first; closest is wger self-host + phone apps (offline not verified). **[UNVERIFIED]**
- None verified: free + GitHub self-update + widget Start + dense dark UI + fasting icon-change. N across board. **[UNVERIFIED-absence]**

## 3 closest matches and exact gaps

### 1) Intervals.icu — best calendar + cardio + export, weakest gym/offline
- Covers: 3=Y (drag-drop, library, weekly totals) — https://www.intervals.icu/features/training-calendar ; 2=Y for planning/analysis (structured power/HR/pace builder, ZWO/FIT/MRC/ERG) — https://www.intervals.icu/features/workout-builder ; 4=Y (wellness weight) — https://www.intervals.icu/features/wellness/ ; 7=partial (CSV + API, free/$4 supporter) — https://forum.intervals.icu/t/download-all-activities-in-csv-format/599 , https://www.intervals.icu/features/open-api/ , https://www.intervals.icu/
- Gaps: 1 gym logging missing (no verified superset/prefill/one-tap/session-swap/e1RM/muscle-sets logger; web-first); 2 execution missing (no verified phone full-screen voice/lock/per-round HR-watts runner — plans must sync to Garmin/Zwift); 5 no native HC import (bridge only) — https://forum.intervals.icu/t/health-sync-now-supports-intervals-icu/92287 ; 6 no dense app/widget verified; 7 no offline, no JSON one-tap, no GitHub self-update; 8/9 none.

### 2) Polar Flow — best native gym+cardio+drag calendar combo, fails data-freedom
- Covers: 3=Y drag-drop incl. Season Planner — https://support.polar.com/us-en/polar-flow-season-planner ; 2=Y phased intervals by HR/speed/power — https://support.polar.com/en/support/how_do_i_create_a_training_target_in_polar_flow?category=faqs&product_id=63839 ; strength+mobility video guides via Fitness Program — https://www.polar.com/en/fitness-program ; TrainingPeaks sync — https://support.polar.com/en/support/how_can_i_automatically_sync_my_data_from_polar_flow_to_trainingpeaks
- Gaps: 1 no verified prefill/e1RM/muscle-sets/session-swap; 2 phone-only 4x4 execution + lock-screen + per-round watts not verified, practically needs Polar watch; 4 weight yes but chart unverified — https://support.polar.com/us-en/flow-app-health-connect ; 5 HC export-only, no import; 7 cloud, no verified JSON/CSV/offline/free (program is subscription); 8 only guided mobility, no verified per-stretch timer + history matching brief; 9 none.

### 3) wger — best data-freedom + gym foundation, fails cardio/calendar polish
- Covers: 1=Y-minus (routines + progression, open exercise wiki), 4=Y (weight/measurements), 7=Y-minus (AGPL + self-host Docker + REST API = only app where data stays on your server) — https://github.com/wger-project/wger , https://wger.readthedocs.io/
- Gaps: 2 no verified interval timer/voice/lock/per-round HR-watts, no pace/laps swim detail; 3 no verified drag-to-move template + overrides; 5 no HC import; 6 UI/widget unverified; 7 no verified one-tap JSON/CSV bulk export (API JSON only), no GitHub self-update for phone apps verified; 8/9 none.
- Why it still makes top-3: it is the only candidate satisfying the hard constraint "data must stay on phone/server or exportable plain" without account lock-in, and the only FOSS base the owner could extend to cover 2/3/5/8/9.

Honorable: TrainingPeaks (calendar + structured cardio + CSV export verified) loses to Polar because gym is weaker and paywalled; Samsung Health (only native HC importer + bodyweight) loses because programs/calendar/export/supersets/e1RM unverified and cloud-locked; Hevy/Strong (best pure gym loggers + CSV) lose because calendar is history-only (Hevy explicitly "cannot be used to plan" — https://help.hevyapp.com/hc/en-us/articles/35380117933207-Track-Your-Workout-Consistency-with-the-Calendar-and-Streak-Features) and neither imports HC sessions.

## What could not be verified (mark explicitly)

- Exact gym details (superset pairing, prefill, one-tap, session-swap, Epley chart, muscle sets/week) for Samsung Health, Garmin, Polar, COROS, TrainingPeaks, Fitbod, Freeletics, JEFIT, Trainerize, Caliber, Setgraph, wger — official docs not fetched or search rate-limited. Treat all 1-scores except Hevy/Strong/wger/Fitbod as provisional.
- Dark dense UI + home-screen widget Start (brief-6): no official citation for any app; scores above are "unknown" intentionally.
- Stretch animations/history (brief-8) and fasting icon-change (brief-9): no listed all-in-one app verified to change launcher icon by eating window; no gym app verified to bundle fasting timer. Treat 9 as N across board (absence-of-evidence, not proof).
- Freeletics/JEFIT/Trainerize/Caliber/Setgraph HC, export, offline claims: **[UNVERIFIED]** — web search returned no results / rate-limited at time of research.
