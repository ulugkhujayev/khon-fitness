### Evaluation Matrix: Existing Apps vs. Khon Fitness Requirements (Items 1–9)

| App | 1. Gym Logging (Supersets A1/B1, e1RM, Volume) | 2. Cardio (Intervals, 4x4, Voice Cues, Watts/HR) | 3. Week Plan (Template + Calendar Overrides) | 4. Bodyweight (Entry & Chart) | 5. Health Connect (Mi Band + Galaxy Watch Merge) | 6. UI (Dark, Dense, Cardless, Widget) | 7. Data & Distribution (Offline, Free, JSON, GitHub) | 8. Mobility (Animations/Video, Timers, History) | 9. Fasting (Meal Window + Dynamic App Icon) | Verified URL / Source |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **Bend** | **N** | **N** | **partial** | **N** | **N** | **partial** | **N** | **Y** | **N** | [Bend App](https://bend.com/) / [Play Store](https://play.google.com/store/apps/details?id=com.bowerydigital.bend) |
| **STRETCHIT** | **N** | **N** | **partial** | **N** | **N** | **N** | **N** | **Y** | **N** | [StretchIt App](https://stretchitapp.com/en) / [Apple App Store](https://apps.apple.com/app/id1092655552) |
| **Pliability (ROMWOD)** | **N** | **N** | **partial** | **N** | **N** | **partial** | **N** | **Y** | **N** | [Pliability](https://pliability.com/) / [Apple App Store](https://apps.apple.com/gb/app/pliability-stretch-mobility/id1175346453) |
| **GOWOD** | **N** | **N** | **partial** | **N** | **N** | **partial** | **N** | **Y** | **N** | [GOWOD](https://www.gowod.app/) |
| **Leap Fitness Stretching** | **N** | **N** | **partial** | **Y** | **partial** | **partial** | **partial** | **Y** | **N** | [Leap Fitness Group](https://play.google.com/store/apps/dev?id=7860305880854199513) |
| **Fitify: Stretching** | **N** | **N** | **partial** | **partial** | **N** | **N** | **partial** | **Y** | **N** | [Fitify](https://gofitify.com/) / [Play Store](https://play.google.com/store/apps/details?id=com.fitifyapps.bwstretching) |
| **Down Dog Yoga** | **N** | **N** | **N** | **N** | **N** | **N** | **N** | **Y** | **N** | [Down Dog](https://www.downdogapp.com/) |
| **Hevy** | **Y** | **partial** | **partial** | **Y** | **partial** | **partial** | **partial** | **partial** | **N** | [Hevy Features](https://www.hevyapp.com/features/) / [Play Store](https://play.google.com/store/apps/details?id=com.hevy.app) |
| **FitNotes** | **partial** | **partial** | **partial** | **Y** | **N** | **Y** | **Y** | **N** | **N** | [FitNotes](https://www.fitnotesapp.com/) / [GitHub](https://github.com/jamesgay/FitNotes-Workout-Tracker) |
| **wger** | **Y** | **partial** | **Y** | **Y** | **N** | **partial** | **Y** | **partial** | **partial** | [wger.de](https://wger.de/) / [wger GitHub](https://github.com/wger-project/wger) |

---

### Key Domain Findings & Combination Options

#### 1. Gym Loggers with Built-In Mobility / Stretch Animations
* **[Hevy](https://www.hevyapp.com/features/):** Contains animated illustrations for stretching exercises (e.g., Hamstring Stretch, Quad Stretch, Cat Cow).
  * **Gap:** Stretches are logged as regular strength sets (e.g., set for 30 seconds). There is no continuous, hands-free follow-along guided routine mode with automatic countdown beeps and exercise transitions.
* **[Caliber](https://caliberfitness.com/):** Features animated GIF demonstrations for flexibility and warm-ups, but remains purely set/rep-based with mandatory online account creation and no interval audio engine.
* **[Nike Training Club (NTC)](https://www.nike.com/ntc-app):** Offers guided follow-along mobility/yoga routines with video and audio timers, but discontinued its dedicated gym strength tracker, offers no plain JSON/CSV export, and cannot combine with external logs.

#### 2. Feasibility of Multi-App Combinations
Because no single app combines strength logging, cardio intervals, mobility video timers, Health Connect multi-wearable deduplication, and dynamic app-icon meal tracking:
* **Best Commercial Stack:** **Hevy** (Gym logging + A1/B1 supersets + CSV export) paired with **Bend** or **Leap Fitness Stretching** (Follow-along animated stretches with per-exercise audio timers).
* **Best Local-First / Privacy Stack (Android 9 Compatible):** **FitNotes** (Offline gym tracker, dark utilitarian list UI, CSV backup) paired with **Leap Fitness Stretching** (Offline-capable 2D animated stretch routines with sound cues).

---

### The 3 Closest Matches and Their Exact Gaps

#### 1. Hevy (Closest Overall Commercial App)
* **Website:** [hevyapp.com](https://www.hevyapp.com/) | [Features List](https://www.hevyapp.com/features/)
* **Where It Matches:**
  * **Item 1 (Gym Logging):** Full support for named routines, A1/B1 superset grouping ([Supersets Guide](https://www.hevyapp.com/features/what-are-supersets/)), auto-prefilling previous weights/reps, one-tap set completion, exercise swapping, muscle target tags, e1RM calculations, and weekly sets-per-muscle distribution.
  * **Item 4 (Bodyweight):** Built-in bodyweight entry and line graphs in the Profile Measure tab.
  * **Item 7 (Partial):** Free tier available; supports CSV export from user settings.
  * **Item 8 (Partial):** Includes exercise animation clips for stretches in its database.
* **Exact Gaps:**
  * **Item 2 (Cardio):** Only logs passive duration/distance/calories. It does **not** have an interval engine (no warm-up/work/rest rounds, voice cues, beeps, lock-screen countdown, or per-round HR/watts).
  * **Item 3 (Week Plan):** Has a workout history calendar, but lacks an advance multi-sport weekly schedule template (Mon swim, Tue gym, etc.) with date-override swapping.
  * **Item 5 (Health Connect Merge):** Syncs Hevy sessions outward to Health Connect, but **cannot** ingest and deduplicate overlapping sessions recorded by Mi Fitness and Samsung Health.
  * **Item 6 (UI):** Social-first, card-heavy layout with rounded elements, pills, and feeds rather than a compact, cardless table.
  * **Item 7 (Data/Account):** Requires a cloud account; free tier limits custom routines to 4 (Pro requires a subscription); closed source; no GitHub distribution.
  * **Item 8 (Mobility):** Stretches are treated as standard sets; lacks continuous, automated follow-along mobility timers.
  * **Item 9 (Meal Window):** Zero nutrition, fasting, or dynamic launcher icon functionality.

---

#### 2. FitNotes (Closest Offline / Privacy-First Match)
* **Website:** [fitnotesapp.com](https://www.fitnotesapp.com/) | **Source Code:** [jamesgay/FitNotes-Workout-Tracker](https://github.com/jamesgay/FitNotes-Workout-Tracker)
* **Where It Matches:**
  * **Item 1 (Gym Logging - Partial):** Prefills weights and reps from prior sessions, 1-tap set completion, exercise substitution, volume tracking, and Epley 1RM calculation.
  * **Item 4 (Bodyweight):** Built-in "Body Tracker" section with weight logging and trend line charts.
  * **Item 6 (UI):** High-density, cardless, dark utilitarian list interface without social bloat; home-screen widgets supported. Runs natively on Android 9 (Galaxy S8).
  * **Item 7 (Offline / Free):** 100% offline, free, zero ads, no account registration, plain CSV export/import, and direct database backup.
* **Exact Gaps:**
  * **Item 1 (Gym Logging Gaps):** Does not natively support A1/B1 superset pairing or weekly sets-per-muscle allocation charts.
  * **Item 2 (Cardio):** Only provides basic manual distance/time inputs; lacks an interval timer engine, audio prompts, or wattage logging.
  * **Item 3 (Week Plan):** Calendar shows past workouts only; cannot configure an advance multi-sport template with drag-and-drop overrides.
  * **Item 5 (Health Connect Merge):** Completely offline with zero Health Connect or wearable integration.
  * **Item 8 (Mobility):** Text-only exercise database; no animations, videos, or per-stretch automated routines.
  * **Item 9 (Meal Window):** Zero fasting tracking or dynamic icon capabilities.

---

#### 3. Flexibility Training & Stretching by Leap Fitness Group (Closest Mobility Match)
* **Developer Page:** [Leap Fitness Group on Google Play](https://play.google.com/store/apps/dev?id=7860305880854199513)
* **Where It Matches:**
  * **Item 8 (Stretching & Mobility):** 2D animated avatar demonstrations for every stretch, custom routine builder, per-stretch countdown hold timers with voice cues and beeps, and routine history.
  * **Item 4 (Bodyweight):** Built-in quick weight logger and progress graph.
  * **Item 7 (Partial):** Fully functional offline without requiring an account login; backward compatible with Android 9 (Galaxy S8).
* **Exact Gaps:**
  * **Item 1 (Gym Logging):** No gym logging, barbell tracking, supersets, or 1RM calculations.
  * **Item 2 (Cardio):** No cardio logging or interval engine.
  * **Item 3 (Week Plan):** Fixed 30-day flexibility calendar; cannot schedule multi-sport weekly routines with calendar date overrides.
  * **Item 5 (Health Connect Merge):** Syncs basic workout timestamps to Google Fit/Health Connect; cannot ingest, merge, or deduplicate multi-wearable records.
  * **Item 6 (UI):** Banner and card-oriented consumer UI with advertisements in the free tier.
  * **Item 7 (Data/Export):** Proprietary closed source; no plain JSON/CSV export; no GitHub release pipeline.
  * **Item 9 (Meal Window & Dynamic Icon):** Leap's intermittent fasting tools are segregated into a standalone app ("Fasting Tracker"), which relies on standard notification shade bars and widgets. It does **not** dynamically change the launcher app icon.

---

### Technical Note on Item 9 (Dynamic Launcher Icon for Fasting)
Dynamically altering an app's home screen icon on Android requires toggling `<activity-alias>` declarations via [`PackageManager.setComponentEnabledSetting()`](https://developer.android.com/reference/android/content/pm/PackageManager#setComponentEnabledSetting(android.content.ComponentName,%20int,%20int)). 
* **OS / Launcher Behavior on Android 9 (Samsung Galaxy S8):** Enabling or disabling an `activity-alias` restarts the application process and can force the Samsung One UI launcher to refresh its entire desktop grid. 
* Because of this instability and platform-specific caching delays, commercial fasting apps ([Zero](https://zerolongevity.com/), [Fastic](https://fastic.com/)) strictly use **status bar notification icons** or **home-screen widgets**, leaving dynamic app-icon switching virtually non-existent in commercial health apps.
