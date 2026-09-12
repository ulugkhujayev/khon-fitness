### Research Summary & Key Architectural Findings

Across the 16 evaluated apps and the broader mobile fitness ecosystem, **no existing commercial, free, or open-source app satisfies all 9 requirements of the Khon Fitness brief**.

Existing apps divide strictly along market silos:
1. **Gym / Strength Loggers** (*Hevy, Strong, Caliber, Setgraph, JEFIT*): Excel at item 1 (A1/B1 supersets, prefilling, e1RM, muscle tracking) and item 4 (bodyweight), but treat cardio as an afterthought (rudimentary manual distance/time entry), have no drag-and-drop multi-sport calendar, and treat Health Connect as **write-only** (exporting calories/workouts outwards, never importing third-party sessions).
2. **Endurance Analytics & Training Planners** (*Intervals.icu, TrainingPeaks, Garmin Connect, Coros*): Excel at item 2 (structured cardio workouts, power curves, HR zones) and item 3 (drag-and-drop weekly calendars with template libraries), but lack a native, offline mobile gym logger for set-by-set weightlifting execution, and rely on bike computers/watches rather than running a live audio-guided interval timer with lock-screen countdown on the phone.
3. **Health Aggregators & B2B Platforms** (*Samsung Health, Google Fit, Trainerize, Freeletics*): Heavy, card-based consumer or coaching interfaces with cloud lock-in.

#### Direct Answers to Key Questions:
* **Which ones import from Health Connect (Mi Fitness + Samsung Health data)?**
  * **None of the gym loggers import workout sessions from Health Connect.** Apps like [Hevy](https://www.hevyapp.com/) and [Strong 6.0](https://help.strongapp.io/article/254-google-fit-health-connect-integration) support Health Connect strictly in **write mode** (exporting their lifting logs to Health Connect).
  * **Zero apps support multi-wearable deduplication and merging.** In Health Connect’s architecture, when Mi Fitness (Mi Band 9) and Samsung Health (Galaxy Watch) write overlapping sessions simultaneously, aggregators ([Google Fit](https://support.google.com/fit/), [Samsung Health](https://www.samsung.com/us/apps/samsung-health/)) either display duplicate workouts or discard one stream according to source priority. Khon Fitness’s automatic merging of dual wearable streams is proprietary and absent in commercial software.
* **Which keep data local or export plain JSON / CSV?**
  * **Local-first / Offline:** Almost all commercial options require cloud accounts. [Strong](https://www.strong.app/) and [Hevy](https://www.hevyapp.com/) support offline logging, but require account sync for full persistence. [wger](https://github.com/wger-project/flutter) is open-source (AGPL-3.0), but its Flutter app depends on a self-hosted or remote server backend.
  * **Plain JSON / CSV Export:** [Hevy](https://www.hevyapp.com/) (CSV export in app settings; full JSON via [Hevy Public REST API](https://api.hevyapp.com/docs/)), [Strong](https://help.strongapp.io/article/147-sync-with-apple-health) (CSV export), [Setgraph](https://setgraph.app/) (CSV export), [Intervals.icu](https://intervals.icu/features/extend/) (CSV, FIT, and open REST API with JSON), [TrainingPeaks](https://www.trainingpeaks.com/) (CSV, FIT, PWX via web), [Garmin Connect](https://connect.garmin.com/) (CSV, TCX, FIT via web).
* **Hardware & OS Reality (Galaxy S8 on Android 9 Pie):**
  * Galaxy S8 is limited to Android 9.0 (API 28).
  * [Samsung Health](https://play.google.com/store/apps/details?id=com.sec.android.app.shealth) has dropped Android 9 support on newer builds (requires Android 10+).
  * [Setgraph](https://setgraph.app/) is **iOS-only** (no Android build exists).
  * Health Connect requires Android 9+ as an APK from Google Play, but newer third-party apps integrating the Health Connect Jetpack SDK increasingly set `minSdkVersion 29` (Android 10+) or `minSdkVersion 30`.

---

### Scorecard Against Brief Items 1–9

**Scoring Key:**
* **Y**: Fully meets the specification out of the box.
* **partial**: Covers some aspects of the item, but lacks critical components.
* **N**: Does not support this item.
* **unknown**: Feature presence could not be verified from public documentation.

| App | 1. Gym Logging | 2. Cardio & Intervals | 3. Week Plan & Drag Calendar | 4. Bodyweight & Chart | 5. Health Connect Import & Merge | 6. Dark Dense UI & Widget | 7. Local / Export JSON/CSV / Free | 8. Stretching & Mobility | 9. Eating Window & Dynamic Icon |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Samsung Health** | partial | partial | partial | Y | N | N | partial | partial | N |
| **Google Fit** | N | partial | N | Y | partial | N | partial | N | N |
| **Garmin Connect** (no device) | partial | partial | Y | Y | N | N | partial | partial | N |
| **Polar Flow** | N | partial | partial | Y | N | N | partial | N | N |
| **Coros** | partial | partial | Y | Y | N | N | partial | N | N |
| **TrainingPeaks** | partial | partial | Y | Y | N | partial | N | N | N |
| **Intervals.icu** | partial | partial | Y | Y | N | partial | partial | N | partial |
| **Fitbod** | partial | partial | N | Y | N | N | N | partial | N |
| **Freeletics** | N | partial | partial | Y | N | N | N | Y | N |
| **Hevy** | Y | partial | partial | Y | N | partial | partial | partial | N |
| **JEFIT** | Y | partial | partial | Y | N | N | partial | partial | N |
| **Strong** | Y | N | N | Y | N | partial | partial | N | N |
| **Trainerize** | partial | partial | Y | Y | partial | N | N | partial | partial |
| **Caliber** | Y | N | partial | Y | N | partial | partial | N | N |
| **Setgraph** | Y | N | N | Y | N | Y | partial | N | N |
| **wger** | partial | N | partial | Y | N | partial | partial | partial | partial |

---

### Per-App Verification & Evidence

* **Samsung Health** ([Samsung Health Product Page](https://www.samsung.com/us/apps/samsung-health/), [Google Play](https://play.google.com/store/apps/details?id=com.sec.android.app.shealth)):
  * *Item 1 (partial):* Tracks individual weight exercises and reps, but lacks program templates, A1/B1 superset blocks, session-only exercise swapping, and weekly sets-per-muscle volume charts.
  * *Item 2 (partial):* Tracks GPS runs, bike rides, and swimming laps/HR, but lacks a full-screen Norwegian 4x4 interval timer with target watts and lock-screen countdown.
  * *Item 3 (partial):* Sets weekly activity targets, but lacks a calendar with drag-to-move workout rescheduling.
  * *Item 4 (Y):* Built-in body composition and weight charting.
  * *Item 5 (N):* Writes to Health Connect, but does not import external workouts from Mi Fitness or merge overlapping wearable tracks.
  * *Item 6 (N):* Card-heavy One UI layout with large cards and pills.
  * *Item 7 (partial):* Free, but cloud-bound to a Samsung Account; export is an account-level data archive (GDPR zip).
  * *Item 8 (partial):* Pre-recorded video fitness programs in the "Fitness" tab, but no customizable per-stretch timer routines.
  * *Item 9 (N):* Logs food intake; no intermittent fasting schedule and no dynamic launcher icon.
* **Google Fit** ([Google Fit Help](https://support.google.com/fit/), [Google Play](https://play.google.com/store/apps/details?id=com.google.android.apps.fitness)):
  * *Item 1 (N):* Only logs generic "Strength Training" duration and estimated calories; no sets, reps, or 1RM.
  * *Item 2 (partial):* Basic GPS tracking for runs and cycling; no structured interval timer with voice cues or swim laps/watts.
  * *Item 3 (N):* No calendar scheduler.
  * *Item 4 (Y):* Direct weight entry with trend charts.
  * *Item 5 (partial):* Reads workouts from Health Connect, but does not merge overlapping band/watch streams (creates duplicate entries).
  * *Item 6 (N):* Circular activity rings and card feed.
  * *Item 7 (partial):* Free, but requires Google Account; APIs are deprecated in favor of Health Connect; export via Google Takeout.
  * *Item 8 & 9 (N):* No stretching routines; no meal window/fasting features.
* **Garmin Connect (Without Garmin Device)** ([Garmin Connect Web](https://connect.garmin.com/), [Google Play](https://play.google.com/store/apps/details?id=com.garmin.android.apps.connectmobile)):
  * *Item 1 (partial):* Strength workout builder allows defining sets, reps, and muscle targets with heatmaps, but the phone app is not designed to execute and log sets in real-time without a paired Garmin watch.
  * *Item 2 (partial):* Supports complex cardio intervals (power, HR, pace targets), but the mobile app cannot record GPS or sensor streams standalone without Garmin hardware.
  * *Item 3 (Y):* Full drag-and-drop training calendar and workout scheduler on web and mobile.
  * *Item 4 (Y):* Weight entry and longitudinal charts.
  * *Item 5 (N):* Connects to Health Connect, but strictly **writes Garmin data outwards**; rejects importing third-party workout sessions from Health Connect.
  * *Item 6 (N):* Card-based dashboard ("My Day" / "At a Glance").
  * *Item 7 (partial):* Free app, but cloud-bound to Garmin servers. Exportable as FIT, TCX, and CSV from Garmin Connect web.
  * *Item 8 (partial):* Guided yoga and mobility workouts with animations, but designed to be followed via a watch screen.
  * *Item 9 (N):* Integrates with MyFitnessPal for calories; no fasting timer or dynamic launcher icon.
* **Polar Flow** ([Polar Flow Web](https://flow.polar.com/), [Google Play](https://play.google.com/store/apps/details?id=fi.polar.polarflow)):
  * *Item 1 (N):* Only supports a high-level "Strength Training" sport profile with no set/rep logging.
  * *Item 2 (partial):* Tracks cardio via Polar devices or the companion Polar Beat app; lacks a standalone Norwegian 4x4 timer with watts on phone.
  * *Item 3 (partial):* Diary and training targets on web, but no drag-and-drop mobile weekly planner.
  * *Item 4 (Y):* Manual weight entry.
  * *Item 5 (N):* Exports outwards to Health Connect; does not import Mi Fitness or Samsung Health workouts.
  * *Item 6 (N):* Activity dials and cards.
  * *Item 7 (partial):* Free, but cloud-bound; CSV/TCX export available on web.
  * *Item 8 & 9 (N):* Generic sport profile for stretching only; no fasting features.
* **Coros** ([Coros Official](https://coros.com/)):
  * *Item 1 (partial):* Strength workout builder with muscle heatmap, but execution requires a Coros watch.
  * *Item 2 (partial):* Cardio analytics, but designed for Coros hardware.
  * *Item 3 (Y):* Coros Training Hub on web offers drag-and-drop calendar planning.
  * *Item 4 (Y):* Weight tracking.
  * *Item 5 (N):* Exports to Health Connect; does not import or merge Health Connect sessions.
  * *Item 6 (N):* Card-based design.
  * *Item 7 (partial):* Free, but cloud-dependent; FIT/TCX export from Training Hub.
  * *Item 8 & 9 (N):* No guided mobility videos; no fasting tracker.
* **TrainingPeaks** ([TrainingPeaks Product](https://www.trainingpeaks.com/), [Google Play](https://play.google.com/store/apps/details?id=com.peaksware.trainingpeaks)):
  * *Item 1 (partial):* Has a "Strength" workout type, but only as descriptive notes/structured text. No live set logger with A1/B1 pairing, 1-tap set completion, previous set prefilling, or e1RM charts.
  * *Item 2 (partial):* Industry gold standard for cardio metrics (TSS, IF, normalized power, zones), but the mobile app is an analysis portal, not a live interval timer with voice cues and lock-screen countdown.
  * *Item 3 (Y):* World-class drag-and-drop training calendar, weekly templates, and date overrides.
  * *Item 4 (Y):* Metric tracking for bodyweight with charts.
  * *Item 5 (N):* Integrates with partner device clouds (Garmin, Wahoo); does not import Health Connect workouts with dual-source merging.
  * *Item 6 (partial):* Data-rich charts, but mobile interface uses standard cards.
  * *Item 7 (N):* Drag-and-drop calendar and future planning require a paid Premium subscription ($19.95/mo or $124.99/yr); cloud-bound.
  * *Item 8 & 9 (N):* No stretching routines or fasting tools.
* **Intervals.icu** ([Intervals.icu Overview](https://intervals.icu/), [Planning Features](https://intervals.icu/features/plan/), [Integration & API Docs](https://intervals.icu/features/extend/)):
  * *Item 1 (partial):* Added a strength workout builder (exercises, sets, reps), but has no mobile execution interface (no A1/B1 superset flow, no 1-tap set completion with prefilled weights).
  * *Item 2 (partial):* Built-in structured workout builder for Norwegian 4x4 (power/HR/pace steps), power duration curves, and swim metrics, but does **not** execute workouts live with audio beeps and voice cues on the phone. Workouts must be pushed to a third-party head unit/watch.
  * *Item 3 (Y):* Full drag-and-drop training calendar with weekly templates, workout libraries, and date overrides.
  * *Item 4 (Y):* Wellness tracking logs weight, resting HR, HRV, and sleep with charting.
  * *Item 5 (N):* Web platform with no native Android app; cannot read Health Connect directly (requires third-party sync bridges like Health Sync); cannot merge dual-wearable streams.
  * *Item 6 (partial):* Highly dense, dark, data-rich desktop/mobile web interface, but no native Android app or home-screen widget.
  * *Item 7 (partial):* Free (community supported), full CSV/FIT export, open REST API with JSON, but hosted in the cloud (not an offline local app).
  * *Item 8 (N):* No guided mobility videos or timers.
  * *Item 9 (partial):* Tracks fasting hours as a daily wellness metric, but lacks a live fasting countdown timer and dynamic launcher icon.
* **Fitbod** ([Fitbod Product](https://www.fitbod.me/), [Google Play](https://play.google.com/store/apps/details?id=com.fitbod.fitbod)):
  * *Item 1 (partial):* Tracks sets, reps, supersets, e1RM, and muscle recovery heatmaps, but workouts are algorithmically generated rather than managed as fixed user-defined programs with manual A1/B1 block configurations.
  * *Item 2 (partial):* Basic cardio exercises (running, rowing), but no Norwegian 4x4 audio interval timer with bike watts.
  * *Item 3 (N):* Generates workouts dynamically day-by-day; lacks a drag-and-drop weekly calendar.
  * *Item 4 (Y):* Weight tracking.
  * *Item 5 (N):* Syncs to Health Connect (write-only); does not import swim/run sessions or merge dual wearable streams.
  * *Item 6 (N):* Card-heavy design with animations and whitespace.
  * *Item 7 (N):* Requires paid subscription ($15.99/mo or $79.99/yr) after a 3-workout trial; cloud-dependent.
  * *Item 8 (partial):* Warm-up and cool-down stretches, but no customizable mobility timer engine.
  * *Item 9 (N):* No fasting tools.
* **Freeletics** ([Freeletics Product](https://www.freeletics.com/), [Google Play](https://play.google.com/store/apps/details?id=com.freeletics.lite)):
  * *Item 1 (N):* Fixed Coach journeys; cannot build custom gym programs with A1/B1 superset pairings and custom exercise swapping.
  * *Item 2 (partial):* Audio-coached HIIT sprint workouts, but no bike watts, swim laps, or Norwegian 4x4 timers.
  * *Item 3 (partial):* Coach schedules weekly sessions, but no drag-and-drop calendar.
  * *Item 4 (Y):* Weight entry.
  * *Item 5 (N):* Writes workouts out to Health Connect only.
  * *Item 6 (N):* Media-heavy card UI.
  * *Item 7 (N):* Requires paid subscription.
  * *Item 8 (Y):* Dedicated guided cool-down and stretching sessions with instructional video.
  * *Item 9 (N):* No eating window tracker or dynamic app icon.
* **Hevy** ([Hevy Homepage](https://www.hevyapp.com/), [Hevy Features](https://www.hevyapp.com/features/), [Google Play](https://play.google.com/store/apps/details?id=com.hevy), [Hevy Public API](https://api.hevyapp.com/docs/)):
  * *Item 1 (Y):* Named programs/routines, supersets (A1/B1), prefilled weights/reps from prior sessions, 1-tap set completion checkmark, session-level exercise swap, muscle group tags, Epley e1RM charts, and weekly sets-per-muscle analytics.
  * *Item 2 (partial):* Basic manual cardio (distance, duration, calories), but no structured interval timer, voice cues, Norwegian 4x4 engine, bike watts, or swim laps.
  * *Item 3 (partial):* Routine folders and calendar history, but lacks a predictive weekly drag-and-drop calendar planner.
  * *Item 4 (Y):* Bodyweight quick entry, measurement logs, and charts.
  * *Item 5 (N):* Integrates with Health Connect, but strictly **exports Hevy workouts outwards**. It cannot import external cardio sessions from Health Connect and does not merge dual-device streams.
  * *Item 6 (partial):* Clean dark mode and home-screen widgets (routine shortcuts/streaks), but includes cards and a social feed.
  * *Item 7 (partial):* Free tier limited to 4 routines; Pro is subscription-based. Has in-app CSV export and a public JSON REST API, but relies on cloud sync.
  * *Item 8 (partial):* Can add stretching exercises to routines, but lacks a dedicated mobility routine runner with per-stretch timers or video demos.
  * *Item 9 (N):* No intermittent fasting tracking.
* **JEFIT** ([JEFIT Product](https://www.jefit.com/), [Google Play](https://play.google.com/store/apps/details?id=je.fit)):
  * *Item 1 (Y):* Named workout programs, supersets, prefilled previous weights/reps, 1-tap set done, 1RM charts, and muscle group volume breakdown.
  * *Item 2 (partial):* Basic cardio logging (time/distance); no interval timer with voice cues, Norwegian 4x4, swim laps, or watts.
  * *Item 3 (partial):* Assigns routines to days of the week, but lacks an interactive drag-and-drop calendar.
  * *Item 4 (Y):* Bodyweight tracking and progress charts.
  * *Item 5 (N):* Exports outwards to Health Connect; does not import external cardio workouts.
  * *Item 6 (N):* Ad-heavy free version, card layouts, cluttered UI.
  * *Item 7 (partial):* Free tier has ads; Pro subscription for full analytics; cloud account required; web CSV export.
  * *Item 8 (partial):* Stretches available in exercise database, but no automated timer player.
  * *Item 9 (N):* No fasting features.
* **Strong** ([Strong App Homepage](https://www.strong.app/), [Google Play](https://play.google.com/store/apps/details?id=io.strongapp.strong), [Strong Health Connect Integration](https://help.strongapp.io/article/254-google-fit-health-connect-integration), [Strong Apple Health / Export](https://help.strongapp.io/article/147-sync-with-apple-health)):
  * *Item 1 (Y):* Industry benchmark for gym logging: custom templates, supersets, prefilled weight/reps, 1-tap completion, exercise swap, 1RM calculations, and muscle breakdowns.
  * *Item 2 (N):* Manual duration/distance logging only; no interval timer, voice cues, watts, or swim laps.
  * *Item 3 (N):* No forward-looking calendar scheduler or drag-to-move interface (only a reverse-chronological workout history).
  * *Item 4 (Y):* Bodyweight and body measurements logging with charts.
  * *Item 5 (N):* Strong 6.0 beta supports Health Connect, but only writes workouts out and reads weight/nutrition; does not import external cardio sessions.
  * *Item 6 (partial):* Minimalist, dark, dense workout logging tables; home-screen widgets available, but no scheduled routine start widget.
  * *Item 7 (partial):* Free tier limited to 3 custom templates (Pro subscription unlocks unlimited); CSV export available; cloud-synced.
  * *Item 8 & 9 (N):* Static stretching exercises only; no fasting tracker.
* **Trainerize** ([Trainerize Product](https://www.trainerize.com/)):
  * *Item 1 (partial):* Supports gym logging, supersets, and rest timers, but configured as a coaching portal for personal trainers.
  * *Item 2 (partial):* Connects to Apple Watch and Garmin; supports assigned interval workouts, but is not a standalone Norwegian 4x4 phone computer with bike watts.
  * *Item 3 (Y):* Interactive drag-and-drop calendar for scheduling client workouts.
  * *Item 4 (Y):* Bodyweight, body fat %, and progress photo charts.
  * *Item 5 (partial):* Connects to wearables and Health Connect beta, but does not merge duplicate wearable streams.
  * *Item 6 (N):* Social and coaching feed layout.
  * *Item 7 (N):* Expensive B2B SaaS ($5–$250+/month); cloud-dependent.
  * *Item 8 (partial):* Can assign video-guided mobility routines.
  * *Item 9 (partial):* Habit tracking for meal windows, but no dynamic launcher icon.
* **Caliber** ([Caliber Official](https://caliberstrong.com/)):
  * *Item 1 (Y):* 100% free strength logging: custom routines, supersets, prefilled previous sets, 1-tap checkmark, exercise swap, and strength score.
  * *Item 2 (N):* Basic cardio duration and distance entry only.
  * *Item 3 (partial):* Weekly workout schedule, but lacks a drag-and-drop calendar.
  * *Item 4 (Y):* Bodyweight tracking and progress charts.
  * *Item 5 (N):* Exports workouts outwards to Health Connect; does not import external cardio sessions.
  * *Item 6 (partial):* Clean dark mode, but uses card containers.
  * *Item 7 (partial):* 100% free gym tracking (coaching is paid), but requires cloud account; no direct in-app CSV export.
  * *Item 8 & 9 (N):* No stretching routines or fasting tools.
* **Setgraph** ([Setgraph Website](https://setgraph.app/)):
  * *Item 1 (Y):* Ultra-fast, dense gym logging, progressive overload tracking, supersets, 1RM, and prefill.
  * *Item 2 (N):* Only basic cardio tracking.
  * *Item 3 (N):* No calendar planner.
  * *Item 4 (Y):* Weight entry.
  * *Item 5 (N):* **iOS only** (app-id 1209781676); does not run on Android and has no Health Connect support.
  * *Item 6 (Y):* Ultra-dense dark table UI with minimal whitespace.
  * *Item 7 (partial):* Subscription for unlimited sets/routines; CSV export supported; cloud-synced.
  * *Item 8 & 9 (N):* No stretching routines or fasting tracker.
* **wger** ([wger Homepage](https://wger.de/), [wger Backend GitHub](https://github.com/wger-project/wger), [wger Flutter Mobile App](https://github.com/wger-project/flutter)):
  * *Item 1 (partial):* Supports workout routines, supersets, weights, reps, progression rules, and 1RM, but the mobile execution flow is less refined than native commercial loggers.
  * *Item 2 (N):* Simple manual cardio entry only.
  * *Item 3 (partial):* Routines support assigned days; calendar view on web, but mobile app lacks drag-and-drop scheduling.
  * *Item 4 (Y):* Comprehensive body weight and measurement tracking with charts.
  * *Item 5 (N):* No Health Connect integration.
  * *Item 6 (partial):* Flutter Material Design app; lacks a specialized dense table and home-screen start widget.
  * *Item 7 (partial):* 100% Free & Open Source (AGPL-3.0) with an open REST API, but the Flutter mobile app requires connection to a wger backend server (wger.de or self-hosted Docker) and is not a standalone local SQLite app.
  * *Item 8 (partial):* Stretches in database, but no automated timer player.
  * *Item 9 (partial):* Has nutrition planning, but no time-restricted eating tracker and no dynamic launcher icon.

---

### The 3 Closest Matches and Their Exact Gaps

Because no single app spans powerlifting logging, endurance interval tracking, dual-device wearable deduplication, and intermittent fasting launcher customization, the 3 closest matches come from different angles:

#### 1. Hevy (Closest to the Core Lifting & Daily Tracking Workflow)
* **What it matches:** Items 1, 4, 6 (partial), 7 (partial). It is the closest match to the owner's gym logging specifications: A1/B1 superset pairing, automatic prefilling of previous weights/reps, 1-tap checkmark, session-level exercise swapping, Epley 1RM charts, and weekly sets per muscle group. It has bodyweight charts, in-app CSV export, a public JSON REST API, dark mode, and home-screen routine shortcuts.
* **Exact Gaps:**
  * **Gap 2 (Cardio):** Cannot run a Norwegian 4x4 or Tabata interval timer (no warm-up/work/rest phases, voice prompts, audio beeps, or lock-screen countdown). Does not capture per-round watts or swim laps.
  * **Gap 3 (Calendar):** Has routine folders and history logs, but lacks a forward-looking weekly planner with drag-to-move workout rescheduling.
  * **Gap 5 (Health Connect):** Only exports lifting sessions outwards; cannot import cardio workouts (swim/run) from Health Connect, and has no dual-wearable deduplication engine.
  * **Gap 7 (Offline / Free):** Requires a cloud account; free tier limits custom routines to 4 (Pro subscription required for unlimited).
  * **Gap 8 (Stretching):** Stretches are just static catalog items; no automated per-stretch timer or video animations.
  * **Gap 9 (Fasting):** No intermittent fasting tracker and no dynamic launcher icon.

#### 2. Intervals.icu (Closest to the Cardio, Analytics & Calendar Workflow)
* **What it matches:** Items 2 (planning/analytics), 3, 4, 7 (partial). It is the only platform that natively understands structured Norwegian 4x4 intervals, power curves, decoupling, HR zones, swim metrics, and watts. It features a full drag-and-drop training calendar with weekly templates and per-date overrides, wellness/weight charting, is free/donation-supported, provides comprehensive CSV/FIT export, and offers an open REST API.
* **Exact Gaps:**
  * **Gap 1 (Gym Execution):** Has a strength workout builder, but lacks a mobile gym execution UI (no A1/B1 superset flow, no 1-tap set completion with prefilled weights while lifting).
  * **Gap 2 (Timer Execution):** It is a planning/analytics platform, **not an on-phone workout execution timer**. It cannot run a full-screen timer with voice cues and beeps on Android; it expects workouts to be synced to a bike computer or Garmin/Wahoo/Apple watch.
  * **Gap 5 (Health Connect):** Web/PWA only with no direct Health Connect client (requires a paid middleman app like Health Sync). Does not merge overlapping band + watch streams.
  * **Gap 6 (Native UI & Widget):** Web app / PWA; no Android home-screen widget with a 1-tap Start button.
  * **Gap 7 (Offline):** Web-based platform; not a local-first offline SQLite database.
  * **Gap 8 & 9:** No stretching video routines; logs fasting hours in wellness, but has no live fasting timer and no dynamic app launcher icon.

#### 3. Garmin Connect (Without Garmin Device) (Closest Broadest Feature Set)
* **What it matches:** Items 1 (builder), 2 (builder), 3, 4, 7 (partial), 8 (partial). It allows building strength routines with target muscle heatmaps, designing complex cardio intervals with target zones, provides a full drag-and-drop calendar, includes bodyweight tracking, and includes guided stretching/yoga routines with animations.
* **Exact Gaps:**
  * **Hardware Execution Wall:** Garmin Connect is architected as a companion for Garmin hardware. The mobile app **cannot execute cardio or interval workouts standalone on the phone**, and real-time set-by-set strength logging is designed to run on a Garmin watch.
  * **Gap 5 (Health Connect):** Garmin Connect **strictly prohibits importing third-party workout sessions from Health Connect** (it only pushes Garmin data outwards to Health Connect).
  * **Gap 6 (UI):** Card-heavy consumer dashboard with significant whitespace.
  * **Gap 7 (Local / Open):** Cloud-bound proprietary ecosystem.
  * **Gap 9 (Fasting):** No intermittent fasting window or dynamic launcher icon.

---

### Conclusion & Recommendation for the Owner

The owner's current private app represents a convergence of **four specialized software categories**:
1. An offline progressive-overload powerlifting tracker ([Hevy](https://www.hevyapp.com/) / [Strong](https://www.strong.app/)).
2. An endurance interval computer ([Intervals.icu](https://intervals.icu/) / Seconds Pro).
3. A multi-wearable Health Connect data reconciliation bridge (custom deduplication logic).
4. An Android system utility (dynamic `activity-alias` launcher icon manipulation for fasting).

**No single existing app in the Android or iOS ecosystem replaces Khon Fitness.** 

* If the owner chooses off-the-shelf apps, they would need a **multi-app stack**: **Hevy** or **Strong** (for gym logging) + **Seconds Pro** or **Intervals.icu via a GPS watch** (for cardio/intervals) + **Health Sync** (for data routing) + **Zero / Fastic** (for fasting).
* However, that multi-app stack would still fail on dual-device wearable stream merging, dense single-screen UI, and the dynamic eating window app icon.
* **Retaining and maintaining the private app (Khon Fitness)** remains the only way to satisfy all 9 requirements under a single, offline, zero-subscription tool.
