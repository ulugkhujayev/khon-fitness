### 1. The Core Question: Is There a Single App That Covers Cardio Execution and Analysis?

> **Question:** *Which single app both runs an interval workout and stores per-round HR and watts, and also logs steady runs and swims with pace and laps?*

**Answer:** **No single standalone mobile app does this today.**

The market is split into three incompatible categories:

1. **Interval Timers** (e.g., [Seconds Pro](https://play.google.com/store/apps/details?id=com.runloop.seconds), [Intervals Pro](https://apps.apple.com/us/app/intervals-pro-hiit-timer/id957586938), [Deltaworks](https://play.google.com/store/apps/details?id=cc.dreamspark.intervaltimer)): These run full-screen interval protocols (Tabata, Norwegian 4x4) with voice cues and lock-screen alerts. However, they do not support cycling power meters (BLE Cycling Power service / watts), cannot record indoor pool swim laps, and do not function as full GPS endurance trackers.
2. **Mobile Outdoor GPS Trackers** (e.g., [OpenTracks](https://github.com/OpenTracksApp/OpenTracks), [FitoTrack](https://codeberg.org/jannis/FitoTrack), [Strava](https://strava.com)): OpenTracks connects to Bluetooth LE heart rate and cycling power meters (watts) and logs outdoor runs, but lacks a structured interval workout engine (no warm-up/work/rest/rounds timer with voice cues or lock-screen countdown) and cannot log indoor pool swim laps. FitoTrack offers voice-announced interval sets via TTS, but lacks power meter (watts) support and swim lap logging. Strava's mobile app does not provide a structured interval countdown timer or standalone indoor pool swim tracking.
3. **Endurance Analytics Platforms** (e.g., [Intervals.icu](https://intervals.icu), [Runalyze](https://runalyze.com)): These platforms can *store, calculate, and analyze* per-round HR, per-round watts, run pace, and pool swim laps/lengths from imported `.FIT` files. However, **neither app executes or runs workouts** in real time with voice cues or lock-screen countdowns on a phone.
4. **Hardware-Tied Ecosystems** (e.g., Garmin Connect paired with a Garmin Forerunner/Fenix, or Wahoo ELEMNT): While a dedicated multisport watch can run structured interval workouts with power/HR targets and record pool swim laps, **the mobile apps themselves cannot execute this standalone on an Android phone** without the proprietary watch or head unit hardware.

---

### 2. Scoring Table: Apps vs. Brief Items 1–9

*Scoring legend: **Y** = fully supported; **Partial** = partially supported or supports a subset; **N** = not supported; **Unknown** = unverified.*

| App | 1. Gym Logging | 2. Cardio (Intervals + Steady + Watts/Swim) | 3. Week Plan | 4. Bodyweight Entry & Chart | 5. Health Connect Import & Overlap Merge | 6. Dark, Dense UI + Widget | 7. Offline, JSON, Free, GitHub Releases | 8. Stretching Routines + Video | 9. Meal Window (Dynamic Icon) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **[Seconds Pro](https://play.google.com/store/apps/details?id=com.runloop.seconds)** | N | **Partial** | N | N | N | **Partial** | N | **Partial** | N |
| **[Interval Timer (Deltaworks)](https://play.google.com/store/apps/details?id=cc.dreamspark.intervaltimer)** | N | **Partial** | N | N | N | **Partial** | N | **Partial** | N |
| **[Intervals Pro (iOS)](https://apps.apple.com/us/app/intervals-pro-hiit-timer/id957586938)** | N | **Partial** | N | **Partial** | N | **Partial** | N | **Partial** | N |
| **[FitoTrack](https://codeberg.org/jannis/FitoTrack)** | N | **Partial** | N | **Partial** | N | **Partial** | **Partial** | N | N |
| **[OpenTracks](https://github.com/OpenTracksApp/OpenTracks)** | N | **Partial** | N | N | N | **Partial** | **Partial** | N | N |
| **[Intervals.icu](https://intervals.icu)** | **Partial** | **Partial** | **Y** | **Y** | **Partial** | **Partial** | **Partial** | N | N |
| **[Runalyze](https://runalyze.com/)** | N | **Partial** | **Partial** | **Y** | **Partial** | **Partial** | **Partial** | N | N |
| **[Strava](https://strava.com)** | N | **Partial** | **Partial** | **Partial** | **Partial** | N | N | N | N |
| **[Gadgetbridge](https://gadgetbridge.org/)** | N | N | N | **Partial** | N | **Partial** | **Partial** | N | N |
| *Gym Benchmark:* **[Hevy](https://www.hevyapp.com/)** | **Y** | N | **Partial** | **Y** | **Partial** | N | N | **Partial** | N |
| *Offline Benchmark:* **[FitNotes](http://www.fitnotesapp.com/)** | **Partial** | N | **Partial** | **Y** | N | **Partial** | **Partial** | N | N |

---

### 3. Detailed Verification Against Brief Criteria (with Citations)

#### 1. Gym Logging
*Requirements: Named programs, one active; A1/B1 supersets; prefilled weight+reps; one-tap set done; exercise swapping; muscle tags; volume, best sets, e1RM chart; sets per muscle per week.*
- **[Hevy](https://www.hevyapp.com/):** **Y**. Supports named routines, A1/B1 superset pairings, previous set prefilling, one-tap checkoffs, exercise swapping, primary/secondary muscle tags, 1RM charts, and weekly sets-per-muscle tracking.
- **[FitNotes](http://www.fitnotesapp.com/):** **Partial**. Prefills weight/reps, provides one-tap set saving, and calculates Epley e1RM with progression graphs. However, it lacks native A1/B1 superset grouping and weekly sets-per-muscle breakdowns.
- **[Intervals.icu](https://intervals.icu):** **Partial**. Offers a structured text-based workout builder for strength training and tracks training load, but is not designed for set-by-set gym floor execution with superset toggling or e1RM exercise graphs.
- **[Seconds Pro](https://play.google.com/store/apps/details?id=com.runloop.seconds), [Deltaworks](https://play.google.com/store/apps/details?id=cc.dreamspark.intervaltimer), [FitoTrack](https://codeberg.org/jannis/FitoTrack), [OpenTracks](https://github.com/OpenTracksApp/OpenTracks), [Strava](https://strava.com), [Runalyze](https://runalyze.com/):** **N**. None provide structured set/rep/weight gym logging.

#### 2. Cardio Execution and Logging
*Requirements: Steady sessions (time, distance, pace, swim laps, avg HR, avg watts) + interval timer (warm-up, work, rest, rounds, voice cues, beeps, lock-screen countdown, per-round HR and watts).*
- **[Seconds Pro](https://www.intervaltimer.com/help/settings/heart-rate-settings):** **Partial**. Offers an interval timer with warm-up/work/rest/rounds, TTS voice cues, lock-screen notifications, and BLE heart rate sensor connectivity. However, it does not connect to cycling power meters (no watts), does not track swim laps, and does not record GPS outdoor runs.
- **[Intervals Pro (iOS)](https://apps.apple.com/us/app/intervals-pro-hiit-timer/id957586938):** **Partial**. Executes interval timers with audio cues, target HR ranges, and saves interval splits to Apple Health. However, it does not support cycling power meters (watts), does not count pool swim laps, and is iOS-only (incompatible with the Galaxy S8 on Android 9).
- **[OpenTracks](https://raw.githubusercontent.com/OpenTracksApp/OpenTracks/main/README.md):** **Partial**. Supports BLE Heart Rate and BLE Cycling Power meters (watts) and records distance, speed/pace, and elevation. However, it does not have an interval workout runner (no programmed warm-up/rest/work cycles with countdowns) and does not log pool swim laps.
- **[FitoTrack](https://codeberg.org/jannis/FitoTrack/wiki/Settings):** **Partial**. Supports steady GPS runs and has an interval training module with voice announcements using Android's Text-to-Speech engine. However, its BLE support is limited to [Heart Rate Profile](https://codeberg.org/jannis/FitoTrack/wiki/Heartrate) (no watts/power meters), it does not store per-round watt/HR splits, and it cannot track indoor pool swim laps.
- **[Intervals.icu](https://intervals.icu/features/analyze/):** **Partial**. Analyzes steady runs, pool swim laps, and interval splits with per-round HR and watts. However, it is an analysis platform and **cannot execute or run** the timer workout on the phone.
- **[Runalyze](https://runalyze.com/):** **Partial**. Provides analysis of power, HR, and swimming laps from imported files, but has no workout execution engine.

#### 3. Week Plan
*Requirements: Weekly template (Mon swim, Tue gym...) + per-date overrides; drag/move workouts on a calendar.*
- **[Intervals.icu](https://intervals.icu/features/plan/):** **Y**. Features a drag-and-drop training calendar, recurring plan templates, workout libraries, and date overrides.
- **[Runalyze](https://runalyze.com/):** **Partial**. Features a calendar with past and planned activities, but lacks flexible drag-and-drop template reassignment.
- **[Hevy](https://www.hevyapp.com/) & [FitNotes](http://www.fitnotesapp.com/):** **Partial**. Show past workout logs and basic routine assignments on a calendar, but do not support a multisport recurring week template (Mon swim, Tue gym, Wed run) with date overrides.
- **Interval Timers ([Seconds Pro](https://play.google.com/store/apps/details?id=com.runloop.seconds), [Deltaworks](https://play.google.com/store/apps/details?id=cc.dreamspark.intervaltimer)) & Trackers ([OpenTracks](https://github.com/OpenTracksApp/OpenTracks), [FitoTrack](https://codeberg.org/jannis/FitoTrack)):** **N**. No planning calendar functionality.

#### 4. Bodyweight Entry and Chart
*Requirements: Quick entry and trend chart.*
- **[Intervals.icu](https://intervals.icu), [Runalyze](https://runalyze.com/), [Hevy](https://www.hevyapp.com/), [FitNotes](http://www.fitnotesapp.com/):** **Y**. All provide dedicated bodyweight logging with progression charts.
- **[FitoTrack](https://codeberg.org/jannis/FitoTrack/wiki/Settings):** **Partial**. Has a weight field in settings used to calculate calorie expenditure, but no historical weight tracking chart.
- **[Seconds Pro](https://play.google.com/store/apps/details?id=com.runloop.seconds), [Deltaworks](https://play.google.com/store/apps/details?id=cc.dreamspark.intervaltimer), [OpenTracks](https://github.com/OpenTracksApp/OpenTracks):** **N**. No weight logging or charts.

#### 5. Health Connect Import & Deduplication
*Requirements: Import finished sessions with HR/distance from Health Connect (Mi Fitness + Samsung Health), merging overlapping band + watch records.*
- **Android 9 Compatibility Verification:** According to the [Google Health Connect Android Documentation](https://developer.android.google.cn/health-and-fitness/health-connect/get-started?hl=en), the standalone Health Connect APK supports Android 9 (API level 28) and higher.
- **Overlap Merging:** **N across all apps**. While [Strava](https://support.strava.com) and [Hevy](https://www.hevyapp.com/) integrate with Health Connect, they write finished workouts or import distinct activities. When two wearables (e.g., Mi Band 9 via Mi Fitness and Galaxy Watch via Samsung Health) write simultaneous workouts to Health Connect, third-party apps either duplicate the session or drop one entirely. None contain custom deduplication logic to merge the HR stream of one with the GPS stream of the other.
- **[FitoTrack](https://codeberg.org/jannis/FitoTrack) & [OpenTracks](https://github.com/OpenTracksApp/OpenTracks):** **N**. Both operate with local databases and GPX/KML file import/export, without Health Connect integration.

#### 6. UI: Dark, Dense, No Bloat, Home-Screen Widget
*Requirements: No cards with per-row buttons, no captions, no pills, no horizontal scroll rows. Home-screen widget with today's item and a Start button.*
- **[Seconds Pro](https://play.google.com/store/apps/details?id=com.runloop.seconds):** **Partial**. Highly readable, dark, full-screen UI, but lacks a scheduled "today's agenda" home-screen widget.
- **[FitNotes](http://www.fitnotesapp.com/):** **Partial**. High-density, utilitarian list layout with no social feeds, but lacks a "Today's Plan + Start" widget.
- **[Intervals.icu](https://intervals.icu):** **Partial**. High information density with dark mode, but is a web app and cannot provide a native Android widget.
- **[Hevy](https://www.hevyapp.com/) & [Strava](https://strava.com):** **N**. Both rely heavily on social feeds, rounded cards, pills, horizontal carousels, and per-row action buttons.

#### 7. Offline, JSON Export, Free, GitHub Releases
*Requirements: Offline operation, plain JSON/CSV export, free, self-updating from GitHub releases.*
- **[OpenTracks](https://github.com/OpenTracksApp/OpenTracks):** **Partial**. 100% free, GPLv3 open source, offline, exports GPX/KMZ/KML; hosted on GitHub, but updates come through F-Droid / Play Store rather than an in-app GitHub APK self-updater.
- **[FitoTrack](https://codeberg.org/jannis/FitoTrack):** **Partial**. 100% free, GPLv3 open source, offline, exports SQLite backup and GPX; hosted on Codeberg/F-Droid.
- **[Intervals.icu](https://intervals.icu/features/open-api/):** **Partial**. Free core tier, comprehensive REST API with complete JSON import/export, but requires an active internet connection.
- **[Seconds Pro](https://www.intervaltimer.com/app), [Intervals Pro](https://apps.apple.com/us/app/intervals-pro-hiit-timer/id957586938), [Hevy](https://www.hevyapp.com/), [Strava](https://strava.com):** **N**. Proprietary, closed-source, commercial, and distributed through app stores.

#### 8. Stretching / Mobility Tracking
*Requirements: Routines with per-stretch timers, instructional animation/video, history.*
- **[Seconds Pro](https://www.intervaltimer.com/app) & [Deltaworks](https://play.google.com/store/apps/details?id=cc.dreamspark.intervaltimer):** **Partial**. Users can manually build a sequence of timers named after stretches, but there are no instructional videos or animations.
- **[Hevy](https://www.hevyapp.com/):** **Partial**. Contains 3D animated exercise demonstrations and includes static stretches in its database, but does not provide an automated, hands-free guided stretching routine timer.
- **Endurance & Cardio Apps ([Intervals.icu](https://intervals.icu), [OpenTracks](https://github.com/OpenTracksApp/OpenTracks), [FitoTrack](https://codeberg.org/jannis/FitoTrack)):** **N**. No mobility routines or demonstration media.

#### 9. Meal Intake Window (Time-Restricted Eating)
*Requirements: Dynamic app icon changes when the eating window is open or closed, start/end times, reminders, history.*
- **N across all evaluated apps.** While specialized fasting apps like [Zero](https://zerolongevity.com/) track fasting windows, none of the multi-sport, cardio, or gym apps include time-restricted eating trackers. Furthermore, dynamically altering the app launcher icon based on eating window state (via Android `activity-alias`) is absent across all evaluated options.

---

### 4. The 3 Closest Matches and Their Exact Gaps

Because no single app satisfies all requirements, the three closest existing options each cover a different subset of the brief:

#### 1. Intervals.icu (Closest for Cardio Analytics, Planning & Multisport Depth)
- **What it covers:** Best-in-class multi-sport interval analysis (per-round HR, per-round watts), swimming lap tracking with pace, steady run analysis, weekly template planning with drag-and-drop calendar overrides, bodyweight tracking, and a comprehensive open REST API with JSON data export.
- **Exact Gaps:**
  - **Cannot execute interval workouts (Item 2):** It is a web platform, not a mobile execution engine. It cannot display a lock-screen countdown or call audio/voice cues during a Norwegian 4x4 session.
  - **No gym floor logging (Item 1):** Lacks set-by-set gym tracking with A1/B1 superset pairing, previous set prefilling, one-tap set checkoffs, and e1RM charts.
  - **Not an offline Android app (Item 7):** Requires a network connection and browser / PWA container.
  - **No local Health Connect overlap merging (Item 5):** Relies on cloud sync or external bridge apps; cannot merge overlapping raw sensor data streams.
  - **No stretching video routines (Item 8) or eating window tracking (Item 9).**

#### 2. Seconds Pro by Runloop (Closest for Structured Interval Execution)
- **What it covers:** Provides the requested interval execution experience for Norwegian 4x4 and Tabata. Features a full-screen high-contrast display, customizable text-to-speech voice cues, interval countdown beeps, background/lock-screen alerts, and live Bluetooth heart rate monitoring.
- **Exact Gaps:**
  - **No cycling power meter support (Item 2):** Connects only to standard Bluetooth Heart Rate monitors; cannot read or record watts.
  - **No steady cardio logging (Item 2):** Cannot log GPS outdoor runs or pool swim laps and stroke counts.
  - **No gym logging (Item 1):** Zero strength training set/rep/weight tracking.
  - **No week planning or calendar (Item 3).**
  - **No Health Connect integration (Item 5):** Operates standalone; does not read from Android Health Connect.
  - **Commercial, closed-source (Item 7):** Paid license, closed source, no GitHub release self-updating.
  - **No stretching media (Item 8) or meal intake tracking (Item 9).**

#### 3. FitoTrack (Closest for Free, Offline, Open-Source Cardio Tracking)
- **What it covers:** 100% free, open-source (GPLv3), offline, privacy-respecting Android application. Runs on Android 9+, has a dark UI without advertisements or subscription locks, records steady outdoor runs with GPS, pace, and BLE heart rate, and includes interval sets announced via TTS.
- **Exact Gaps:**
  - **No power meter (watts) support (Item 2):** Only supports the Bluetooth Heart Rate Profile; cannot connect to cycling power meters.
  - **No per-round split analysis (Item 2):** Does not record or break down per-interval HR or watt averages.
  - **No pool swim tracking (Item 2):** Does not support indoor pool lap counting.
  - **No gym logging (Item 1):** Contains no gym logging, supersets, or weight/rep tracking.
  - **No Health Connect support (Item 5):** Only exports/imports local GPX and SQLite backups.
  - **Hosted on Codeberg/F-Droid (Item 7):** Does not self-update directly from GitHub releases.
  - **No stretching media (Item 8) or meal intake tracking (Item 9).**

---

### 5. Summary & Practical Recommendation

The owner's private Android app combines four distinct software categories:
1. A dense **gym tracker** (e.g., Hevy/FitNotes)
2. A dedicated **interval workout timer** (e.g., Seconds Pro)
3. A **multisport sensor aggregator and analytics engine** (e.g., OpenTracks + Intervals.icu)
4. A personal **lifestyle utility** (stretching video player + time-restricted eating tracker with dynamic icon switching)

Because no commercial or open-source app currently combines live interval workout execution with per-round watts, pool swim lap tracking, gym supersets, and local Health Connect dual-wearable deduplication, **the owner's existing private app addresses an unserved niche that cannot be replaced by any single existing application.**
