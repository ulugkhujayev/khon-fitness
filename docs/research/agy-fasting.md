Based on an in-depth audit of commercial fasting apps, open-source repositories on F-Droid, and hybrid fitness suites, here are the findings regarding the brief for **Khon Fitness**.

---

### Key Questions Answered

#### 1. Which app changes its home-screen icon based on fasting state (eating window open vs. closed)?
**Answer: None.** Across all surveyed apps (commercial and open source), **not a single app dynamically toggles its launcher icon based on eating window state.**
* **On iOS (`setAlternateIconName`)**: Apple's UIKit API ([Apple Developer Documentation](https://developer.apple.com/documentation/uikit/uiapplication/2806818-setalternateiconname)) strictly requires foreground execution (it cannot run from background tasks or push notifications) and triggers an unavoidable modal alert confirmation (*"You have changed the icon for..."*; see [Hacking with Swift Analysis](https://www.hackingwithswift.com/example-code/uikit/how-to-change-your-app-icon-dynamically-with-setalternateiconname)). Silent, real-time background switching on timer expiration is prohibited by iOS.
* **On Android (`<activity-alias>` & `PackageManager`)**: While programmatically toggling an `<activity-alias>` via `setComponentEnabledSetting` is technically permitted ([Android Developers: Activity Alias](https://developer.android.com/guide/topics/manifest/activity-alias-element)), toggling an alias kills the app process and forces OEM launchers (including Samsung One UI on Galaxy devices) to redraw the desktop ([DynamicIcon Technical Overview](https://github.com/oguzhanaslann/DynamicIcon)). This causes home screen icons to flicker, disappear briefly, or reset out of customized folders.
* **Industry Standard Solution**: All commercial and open-source fasting apps abandon dynamic icons in favor of **Home Screen Widgets, Lock Screen Widgets, iOS Live Activities, or Persistent Status Bar Notifications**.

#### 2. Which gym or fitness apps bundle a fasting timer?
Dedicated gym lifting loggers (such as [Hevy](https://www.hevyapp.com/), [Strong](https://www.strong.app/), [FitNotes](http://www.fitnotesapp.com/), and [wger](https://wger.de/)) intentionally omit fasting timers to keep their focus on progressive overload strength training. However, three fitness platforms do bundle a fasting timer:
1. **[Solo Forge](https://f-droid.org/en/packages/com.kbul.spicycrab/)** (Open Source / F-Droid): Bundles an intermittent fasting timer directly with workout interval/session timers, weight tracking, macro analysis, and Health Connect sync.
2. **[Cronometer (Gold)](https://cronometer.com/gold/)**: Bundles a customizable fasting schedule timer (16:8, 18:6, 20:4, OMAD) alongside nutrition logging, biometric charting, and Health Connect exercise imports ([Cronometer Fasting Guide](https://support.cronometer.com/hc/en-us/articles/360036016612-Mobile-Fasting)).
3. **[MyFitnessPal (Premium)](https://www.myfitnesspal.com/premium)**: Includes an Intermittent Fasting Tracker with window start/stop times and history integrated into its food and exercise diary ([MyFitnessPal Fasting FAQ](https://support.myfitnesspal.com/hc/en-us/articles/10983207647117-Track-Intermittent-Fasting-with-MyFitnessPal-Premium)).

---

### Scoring Table (Brief Items 1–9)

> **Scoring Legend:**  
> • **Y**: Fully meets requirement  
> • **Partial**: Meets part of the requirement (details noted)  
> • **N**: Feature absent or fundamentally incompatible  
> • **Unknown**: Not verifiable via public documentation or repository manifests

| App / Platform | 1. Gym Logging | 2. Cardio Engines | 3. Week Plan | 4. Weight Chart | 5. Health Connect | 6. Dense UI & Widget | 7. Offline/JSON/Free | 8. Stretching / Mobility | 9. Fasting & Dynamic Icon | Reference URLs |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **Zero** *(Zero Longevity)* | N | N | N | Y | Partial *(1)* | Partial *(2)* | N *(3)* | N | Partial *(4)* | [Zero Longevity](https://zerolongevity.com/) • [Play Store](https://play.google.com/store/apps/details?id=com.zerofasting.zero) |
| **Fastic** | N | N | N | Y | Partial *(1)* | Partial *(2)* | N *(3)* | N | Partial *(4)* | [Fastic Official](https://fastic.com/en) • [Play Store](https://play.google.com/store/apps/details?id=de.fastic.app) |
| **Simple** *(Simple Life)* | N | N | N | Y | Partial *(1)* | N *(5)* | N *(3)* | N | Partial *(4)* | [Simple Life](https://simple.life/) • [Features](https://help.simple.life/en/articles/9887852-simple-life-overview-of-main-features) |
| **Window** | N | N | N | Y | Partial *(1)* | Partial *(2)* | N *(3)* | N | Partial *(4)* | [Window App](https://windowfasting.co/) • [App Store](https://apps.apple.com/us/app/window-intermittent-fasting/id1112765909) |
| **BodyFast** | N | N | N | Y | Partial *(1)* | Partial *(2)* | N *(3)* | N | Partial *(4)* | [BodyFast](https://www.bodyfast.app/en/) • [Play Store](https://play.google.com/store/apps/details?id=com.bodyfast) |
| **DoFasting** | N *(6)* | N | N | Y | Partial *(1)* | N *(5)* | N *(3)* | Partial *(7)* | Partial *(4)* | [DoFasting App](https://dofasting.com/app) • [Play Store](https://play.google.com/store/apps/details?id=com.kilofasting) |
| **Fasting Tracker** *(Leap)* | N | N | N | Y | Partial *(1)* | Partial *(2)* | Partial *(8)* | N | Partial *(4)* | [Leap Fasting](https://play.google.com/store/apps/details?id=bodyfast.zero.fastingtracker.weightloss) |
| **Solo Forge** *(F-Droid)* | N *(9)* | Partial *(10)* | Partial *(11)* | Y | Partial *(12)* | Partial *(13)* | **Y** *(14)* | N | Partial *(4)* | [F-Droid Package](https://f-droid.org/en/packages/com.kbul.spicycrab/) • [GitHub](https://github.com/kirilan/SoloForge) |
| **MyFastingApp** *(F-Droid)* | N | N | N | Partial *(15)*| N | Partial *(2)* | **Y** | N | Partial *(4)* | [F-Droid Package](https://f-droid.org/en/packages/org.myfastingapp.app/) • [GitHub](https://github.com/rohanwinsor/MyFastingApp) |
| **FastTimes** *(F-Droid)* | N | N | N | N | N | Partial *(16)*| **Y** | N | Partial *(4)* | [F-Droid Package](https://f-droid.org/en/packages/com.tmdev.fasttimes) • [GitHub](https://github.com/tom-murphy-development/FastTimes) |
| **FastTrack** *(F-Droid)* | N | N | N | Y | N | Partial *(2)* | **Y** | N | Partial *(4)* | [F-Droid Package](https://f-droid.org/en/packages/com.darkrockstudios.apps.fasttrack/) • [GitHub](https://github.com/Darkrock-Studios/FastTrack) |
| **Cronometer** *(Gold)* | N *(17)*| Partial *(18)* | Partial *(19)* | Y | Partial *(12)* | Partial *(20)*| N *(3)* | N | Partial *(4)* | [Cronometer Gold](https://cronometer.com/gold/) • [Fasting Guide](https://support.cronometer.com/hc/en-us/articles/360036016612-Mobile-Fasting) |
| **MyFitnessPal** *(Premium)*| Partial *(21)*| Partial *(18)*| Partial *(19)* | Y | Partial *(12)* | N *(5)* | N *(3)* | Partial *(7)* | Partial *(4)* | [MyFitnessPal](https://www.myfitnesspal.com/) • [Fasting FAQ](https://support.myfitnesspal.com/hc/en-us/articles/10983207647117-Track-Intermittent-Fasting-with-MyFitnessPal-Premium) |

---

#### Scoring Notes & Unverified Points
* *(1)* **Health Connect**: Imports basic daily steps/weight; does not import raw workout streams or de-duplicate/merge overlapping sensor streams.
* *(2)* **UI & Widget**: Has home-screen widgets for fasting, but UI is card-heavy, non-dense, or contains promotional banners.
* *(3)* **Offline / Free**: Requires cloud authentication, enforces a paywall subscription ($35–$100+/year), and offers no local JSON schema backup.
* *(4)* **Fasting & Icon**: Full window timer, reminders, and history supported; **dynamic app icon toggle is absent (N)**.
* *(5)* **UI**: Highly spaced, conversational/AI or card-heavy feed with carousels and pills.
* *(6)* **DoFasting Gym**: Offers generic follow-along bodyweight video workouts; lacks structured strength logging (no programs, supersets, A1/B1, prefill, or 1RM).
* *(7)* **Stretching**: Has pre-recorded video routines; lacks structured per-stretch countdown timers, custom routine creation, and stretch history.
* *(8)* **Leap Fasting Offline**: Free tier is ad-supported and functional offline, but export is limited to proprietary cloud sync or CSV, not plain JSON.
* *(9)* **Solo Forge Gym**: Includes workout timers (session, interval, exercise/rest toggle), but does not log sets, reps, supersets, weights, or muscle volumes.
* *(10)* **Solo Forge Cardio**: Has an interval timer with periodic audio beeps, but lacks steady cardio logging (swim laps, pace, watts, HR) or lock-screen countdowns.
* *(11)* **Solo Forge Week Plan**: Features a daily calendar journal and status overview, but lacks a 7-day recurring template with workout rescheduling.
* *(12)* **Health Connect Sync**: Native read/write support for weight and exercise sessions; lacks smart deduplication/merging of overlapping band + watch streams.
* *(13)* **Solo Forge UI**: Offline, dark theme, local-first Compose UI, but lacks a home-screen workout widget with a 1-tap Start button.
* *(14)* **Solo Forge Compatibility**: GPL-3.0, 100% offline, versioned JSON export/import with auto-backup folder, and `minSdk = 26` ([verified in build.gradle.kts](https://raw.githubusercontent.com/kirilan/SoloForge/main/app/build.gradle.kts)), making it fully compatible with Galaxy S8 on Android 9 (API 28).
* *(15)* **MyFastingApp Weight**: Only basic manual weight entry; lacks advanced trend charting.
* *(16)* **FastTimes UI**: Minimalist and dark, but lacks an Android home-screen widget.
* *(17)* **Cronometer Gym**: Exercises are logged purely as flat caloric burn entries; no strength programming or progressive overload tracking.
* *(18)* **Cardio Logging**: Imports cardio workouts and metrics from Health Connect/wearables, but has no built-in interval timer engine (Norwegian 4x4, Tabata, audio cues).
* *(19)* **Week Plan**: Allows scheduling nutrition/fasting targets, but has no workout template or calendar workout mover.
* *(20)* **Cronometer UI**: Dense biometric layout, but card-heavy with bottom tab bars; widget is for food/fasting diary, not workout start.
* *(21)* **MyFitnessPal Strength**: Allows basic logging of sets, reps, and weight, but lacks superset blocks (A1/B1), prefilled weights from last sessions, exercise swapping, Epley 1RM charts, and weekly sets-per-muscle tracking.

---

### The 3 Closest Matches & Their Exact Gaps

### 1. Solo Forge (F-Droid / Open Source)
* **Why it's close**: It is the single existing open-source app that unites an **Intermittent Fasting Timer** (16:8, 18:6, 20:4, 36h) with **Workout Timers**, **Weight Tracking & Charting**, **Health Connect Integration**, and **Versioned JSON Import/Export** ([GitHub: kirilan/SoloForge](https://github.com/kirilan/SoloForge)). It is 100% offline, free (GPL-3.0), and its `minSdk 26` natively supports Android 9 on the Galaxy S8.
* **Exact Gaps Against Khon Fitness**:
  1. **Gym Logging (Item 1)**: Missing entirely. Only offers basic timers; cannot log exercises, sets, reps, prefilled weights, superset pairings (A1/B1), Epley 1RM curves, or weekly sets per muscle.
  2. **Cardio (Item 2)**: Interval timer only emits simple audio beeps; cannot track steady metrics (swim laps, pace, watts, HR) or run lock-screen Norwegian 4x4 countdowns.
  3. **Week Plan (Item 3)**: Has a daily journal/calendar, but lacks a weekly recurring template with draggable workouts.
  4. **Health Connect Merging (Item 5)**: Reads exercises from Health Connect, but does not merge or de-duplicate overlapping Mi Band and Galaxy Watch records.
  5. **UI & Widget (Item 6)**: Uses standard Material cards/tabs; has no home-screen widget with a 1-tap workout Start button.
  6. **Stretching (Item 8)**: No mobility routines, stretch timers, or instructional animations.
  7. **Dynamic Icon (Item 9)**: Launcher icon is static (its only `<activity-alias>` is for Health Connect permissions).

### 2. Cronometer (Gold Tier)
* **Why it's close**: Provides a data-dense interface with comprehensive biometric and weight charting, native Health Connect synchronization, and a built-in **Fasting Schedule Engine** that overlays eating/fasting windows onto daily charts ([Cronometer Fasting](https://support.cronometer.com/hc/en-us/articles/360036016612-Mobile-Fasting)).
* **Exact Gaps Against Khon Fitness**:
  1. **Gym Logging (Item 1)**: Strength activities are treated solely as calorie-expenditure line items; no gym programming, set/rep tracking, supersets, or 1RM tracking.
  2. **Cardio Engine (Item 2)**: Imports external cardio records, but possesses no live interval timer engine (no audio cues, Tabata, or watts tracking).
  3. **Week Plan (Item 3)**: No training plan template or workout calendar management.
  4. **Health Connect Merging (Item 5)**: Does not reconcile overlapping wearable records from Mi Fitness and Samsung Health.
  5. **Licensing & Architecture (Item 7)**: Closed source, requires cloud account login, fasting timer is locked behind a $54.99/year subscription, and data exports as CSV rather than plain JSON.
  6. **Stretching (Item 8)**: Zero mobility tracking or stretch animations.
  7. **Dynamic Icon (Item 9)**: Static app launcher icon.

### 3. MyFitnessPal (Premium Tier)
* **Why it's close**: Bundles exercise logging (both cardio and basic lifting), bodyweight tracking, Health Connect integration, and an official **Intermittent Fasting Tracker** with scheduled window notifications ([MyFitnessPal Fasting Feature](https://blog.myfitnesspal.com/introducing-the-intermittent-fasting-tracker)).
* **Exact Gaps Against Khon Fitness**:
  1. **Gym Logging (Item 1)**: Rudimentary lifting entry only; lacks superset pairing (A1/B1), previous-session weight prefilling, program swapping, volume tracking per muscle, and 1RM calculation.
  2. **Cardio Engine (Item 2)**: No interval timer, voice cues, beeps, or power/lap logging.
  3. **Week Plan (Item 3)**: No workout week schedule template with calendar move tools.
  4. **Health Connect Merging (Item 5)**: Cannot deduplicate simultaneous wearable sessions.
  5. **UI Density (Item 6)**: Cluttered, card-heavy layout with community feeds, upsells, pills, and banner ads.
  6. **Licensing & Architecture (Item 7)**: Fasting tracker is locked behind an $79.99/year subscription; cloud-dependent with no JSON backup.
  7. **Stretching (Item 8)**: General follow-along workout videos only; no dedicated per-stretch timers or mobility history.
  8. **Dynamic Icon (Item 9)**: Static app launcher icon.

---

### Conclusion & Recommendation
No single existing commercial or open-source app satisfies the Khon Fitness brief:
* Dedicated fasting apps (Zero, Fastic, Window, BodyFast) do not handle serious gym or interval cardio training.
* Commercial fitness suites (Cronometer, MyFitnessPal) require expensive recurring subscriptions and lack progressive-overload gym engines and offline JSON ownership.
* Open-source apps (Solo Forge) provide the ideal privacy, offline JSON, and fasting foundation, but do not provide weightlifting program tracking.
* **The dynamic eating-window launcher icon cannot be found anywhere**, because OS-level restrictions make dynamic launcher icon switching either disruptive (Android process kill and desktop redraw) or blocked by user confirmation modals (iOS).

**Recommendation**: Retain your private Android app architecture. If adding items 8 and 9, implement the eating window as a **Dense Home Screen Widget** (which updates silently and smoothly without launcher glitches) rather than an `activity-alias` launcher icon switch.
