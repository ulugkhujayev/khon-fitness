# Stretching / mobility apps — scored against Khon brief items 1–9

Legend: Y = yes · partial = partly · N = no · unknown = could not verify (no source found).
Brief items: 1 gym logging · 2 cardio + interval timer + per-round HR/watts · 3 weekly template + drag-to-move calendar ·
4 bodyweight entry + chart · 5 Health Connect session import · 6 dark dense UI + widget · 7 offline + JSON/CSV export, free, GitHub releases ·
8 stretch routines with per-stretch timer + animation/video + history · 9 fasting window with home-screen icon change.
Every factual claim has a source URL; anything without one is marked unknown.

## Score table

| App | 1 Gym | 2 Cardio | 3 Week plan | 4 BW | 5 HC import | 6 UI/widget | 7 Offline/export/free/OSS | 8 Stretch timer+video+history | 9 Fasting icon |
|---|---|---|---|---|---|---|---|---|---|
| Bend | N | N | N | N | unknown | unknown | partial (works offline per user review; export unknown; freemium, not OSS) | Y (timer + illustrations/videos + streaks/analytics; see notes on video format dispute) | N |
| StretchIt | N | N | partial (session calendar, no drag-to-move verified) | N | unknown | unknown | partial (offline downloads; export unknown; paid, not OSS) | partial (instructor video classes + history; per-stretch countdown timer not verified) | N |
| Pliability (ex-ROMWOD) | N | N | partial (Build-Your-Program week plan; drag-to-move unknown) | N | unknown (Garmin/Apple Health/Whoop sync yes; Health Connect unknown) | unknown | partial (videos downloadable; export unknown; paid, not OSS) | Y (guided video + on-screen timers + streaks/progress) | N |
| GOWOD | N | N | N | N | unknown (Strava sync yes; Health Connect unknown) | unknown | partial (free test + 300 free videos; offline unknown; export unknown; freemium) | Y (demo video + countdown timer per stretch + premium history) | N |
| Leap "Stretch Exercise - Flexibility" | N | N | N | partial (weight-trend chart; bodyweight entry unknown) | unknown | unknown | unknown offline / unknown export; free, not OSS | partial→Y (animation+video+voice + auto progress; per-stretch timer implied, not explicitly verified) | N |
| Fitify "Flexibility & Stretching" | N (sibling Fitify Gym logs gym; this app alone N) | N | N | N | unknown (sibling app uses Apple Health; HC unknown) | unknown | Y offline; export unknown; freemium, not OSS | Y (HD video + voice coach + duration/rest intervals + history) | N |
| Yoga apps (Down Dog as reference) | N | N | N | N | unknown (Apple Health yes; HC unknown) | unknown | partial (offline downloads; export unknown; paid, not OSS) | partial (generated video+voice, configurable hold length, history/streaks; explicit per-stretch countdown UI not verified) | N |
| Open source: wger | partial (routines + tracking, strength-oriented) | partial (tracks cardio TBD — unknown detail) | partial (day-based plans; drag calendar N) | Y (body-weight tracking) | unknown | unknown | Y (offline-capable apps; free; AGPL; self-hosted; JSON via REST API — export file format unknown) | partial (large exercise library w/ images/videos; dedicated per-stretch timer + mobility history not verified) | N |
| Open source: Flexify | partial (strength + cardio log, graphs; no supersets/programs verified here) | partial (cardio record + rest timers; no interval voice/HR verified) | N | unknown | unknown | unknown | Y (offline, no account; free; MIT; export unknown) | N (no stretch animations) | N |
| Open source: F-Droid timers (TimeR Machine etc.) | N | partial (custom interval timers, no HR/watts) | N | N | N | unknown | Y (offline, free, OSS; no JSON verified) | partial (custom timers + history; no animations) | N |
| Gym loggers w/ mobility: Fitbod | Y (gym logger; detail in gym report) | partial | partial | Y | unknown | unknown | unknown (paid; offline/export in gym report) | partial (warm-up/cool-down static+dynamic+soft-tissue+primers tailored to workout; per-exercise video not verified on help page) | N |
| Gym loggers w/ mobility: JEFIT | Y | partial | partial (routines, 2-day stretch plans) | Y | unknown | unknown | unknown | partial (stretch/mobility routines e.g. Stretch Series v2, Mobility Flexibility Combo from exercise DB w/ demos; dedicated per-stretch countdown unknown) | N |
| Combo: Fitify Workouts/Gym + Stretching | partial (Gym logs reps/weights + charts) | partial (HIIT/Tabata/LISS sessions w/ video; per-round HR/watts unknown) | partial (weekly plan of workout+recovery sessions; drag-to-move unknown) | partial (bodyfat/weight tracking) | unknown | unknown | Y offline; export unknown; freemium | Y (15+ recovery sessions: stretching/yoga/foam-rolling w/ HD video + voice + history) | N |

## Per-app evidence (claim → source)

### Bend
- Per-stretch video + visual countdown timer + sound cue to signal transitions: https://www.nytimes.com/wirecutter/reviews/bend-app-review
- Custom routines (choose stretches + durations), illustrations + timer per routine, streaks & analytics: https://play.google.com/store/apps/details?id=com.bowerydigital.bend&hl=en_US
- Hundreds of exercises/routines with illustrations + videos; streaks/analytics; custom routines; works without wifi (user review "not have to be connected to wifi"): https://bend.com
- Counter-note: one comparison review says Bend routines use "a countdown timer and an illustration of each position, with no follow-along video or voice coaching": https://pliability.com/stories/bend-vs-stretchit — conflicts with Wirecutter/Play-listing video claims, so treat "video per stretch" as present but format varies by content; marked Y with this caveat.
- Pricing varies by promotion; free download with one starter routine, subscription for full library; store listing shows current rates: https://pliability.com/stories/bend-app-cost ; in-app purchases $4.99–$39.99: https://app.sensortower.com/overview/1513988468?country=US ; user report ~$29.99/yr on offer, referral 70% off annual: https://www.youtube.com/watch?v=9ERuU7GX0bQ&vl=en
- Health Connect import, JSON/CSV export, widget, gym/cardio/weekly-plan/bodyweight/fasting: no source found → scored N (brief items clearly out of scope) or unknown where a negative cannot be proven (HC import, export, widget = unknown).

### StretchIt
- Instructor-led video classes (yoga/gymnastics/Pilates/bodyweight blend), goal programs (splits, backbend), 5–45 min, offline downloads, questionnaire-based recommendations, post-class feedback adapts plan: https://play.google.com/store/apps/details?id=com.stretchitapp.stretchit&hl=en_US and https://pliability.com/stories/bend-vs-stretchit
- 100+ hours video, real-human demos, multi-angle, session calendar, challenges/leaderboard, photo progress: https://fitnessdrum.com/stretchit-review
- Price $19.99/mo or $159.99/yr (2026 review): https://fitnessdrum.com/stretchit-review
- Per-stretch countdown timer: not verified in sources → item 8 = partial. Health Connect, export, widget: unknown. Gym/cardio/plan-drag/BW/fasting: N (out of scope).

### Pliability (formerly ROMWOD)
- ROMWOD evolved into pliability; 1,700+ guided routines, 12–20 min sessions, videos downloadable for offline: https://play.google.com/store/apps/details?hl=en&id=com.romwodllc.android
- Daily expert-led routines with on-screen timers and coaching cues; price $22.95/mo or $199.95/yr, 7-day trial: https://pliability.com/mobility
- Integrates with Whoop, Garmin, Apple Health: https://play.google.com/store/apps/details?hl=en&id=com.romwodllc.android ; gym/affiliate integration via SugarWOD (routines into gym programming): https://pliability.com/ ; Health Connect import: unknown.
- History/progress: streaks + "track progress" evidenced by long-streak user reviews on https://pliability.com/stretching and https://pliability.com/mobility ("Track your progress over time", "90+ week streak"). JSON export: unknown.

### GOWOD
- Free mobility assessment + 300+ free mobility videos; premium unlocks personalized flows, classic/essential flows, activation/recovery protocols, progress tracking/history: https://www.gowod.app/pricing and https://www.gowod.app/faq
- Per-stretch demo video + detailed instructions + countdown timer (incl. 5-sec side-switch): https://willpowerstrength.com/injury-prevention/gowod/
- Monthly retest tracks score; premium users +18% mobility in 60 days (vendor claim): https://www.gowod.app/pricing
- Strava auto-sync of mobility sessions: https://www.gowod.app/ ; Health Connect: unknown. Offline downloads: unknown.
- Pricing: monthly $11.99, 6-mo $59.99, yearly $107.99: https://willpowerstrength.com/injury-prevention/gowod/ ; App Store IAPs show $13.99/mo, $124.99/yr tiers: https://apps.apple.com/us/app/gowod-mobility-stretching/id1227834875 ; exact current price varies → confirm on store listing.

### Leap Fitness "Stretch Exercise - Flexibility" (stretching.stretch.exercises.back)
- Voice coach + detailed animation and video demos; custom routines (replace/reorder); reminders; auto records progress; calorie tracking; weight-trend chart: https://play.google.com/store/apps/details?hl=en_US&id=stretching.stretch.exercises.back
- 10M+ installs, free: https://apkte.com/stretch-exercise-flexibility/stretching.stretch.exercises.back (install base) + Play listing (free download).
- Per-stretch timer: implied by guided workouts but not explicitly stated on listing → scored partial→Y with that caveat (not verified). Offline mode, Health Connect, JSON export: unknown (could not verify).

### Fitify "Flexibility & Stretching" (com.fitifyapps.bwstretching)
- 65+ exercises, 4 trainings, no equipment, voice coach, HD video demos, works offline; custom workouts (duration/rest intervals, 1 free): https://play.google.com/store/apps/details?hl=en_US&id=com.fitifyapps.bwstretching
- History/calendar + streaks are platform features ("Track history of your workouts with built-in calendar", "See history and track your awesome progress", "works offline, voice coach, HD video"): https://play.google.com/store/apps/details?id=com.fitifyworkouts.bodyweight.workoutapp&hl=en and https://fitifyapps.com/
- Sibling Fitify Gym logs reps/weights with visual charts, Apple Watch/HealthKit: https://play.google.com/store/apps/details?id=com.fitifyworkouts.personaltrainer&hl=en ; main app Apple Health sync: https://apps.apple.com/us/app/fitify-home-workout-ai-coach/id1463386686 ; Health Connect import and JSON export: unknown.
- 15+ recovery sessions (stretching/yoga/foam rolling) inside Fitify Workouts: https://play.google.com/store/apps/details?id=com.fitifyworkouts.bodyweight.workoutapp&hl=en

### Yoga apps (Down Dog as reference; Daily Yoga / Asana Rebel similar, not individually scored)
- Generated (not pre-recorded) practices, 10+ styles, boost by body area, hold-length/transition-speed settings, 6 voices: https://apps.apple.com/us/app/yoga-down-dog/id983693694
- Offline: save/download practices on mobile: https://www.downdogapp.com/faq
- History/streaks/goals + Apple Health sync: https://apps.apple.com/us/app/yoga-down-dog/id983693694
- Price $9.99/mo or $59.99/yr store, ~$39.99/yr via web (discounted): https://www.garagegymreviews.com/down-dog-app-review ; Health Connect import, JSON export: unknown. Per-stretch countdown UI: not verified → partial.

### Open source options
- wger: free/open (AGPL-3.0), self-hostable Docker, custom routines, tracks diet/body-weight/measurements, REST API, apps on Android/iOS/F-Droid: https://github.com/wger-project/wger and https://github.com/wger-project/flutter ; dedicated per-stretch timer + animation-per-stretch + mobility history: not verified → partial.
- Flexify (brandonp2412): MIT, offline, no account/ads/tracking; logs strength + cardio, rest timers/alarms, graphs/history: https://f-droid.org/packages/com.presley.flexify/ and https://github.com/brandonp2412/Flexify ; no stretch animations → item 8 N.
- F-Droid interval timers (e.g. TimeR Machine: customizable multi-stage timers, background notification, history records): https://f-droid.org/en/packages/io.github.deweyreed.timer.other ; no animations → partial at best.
- GitHub yoga-demo apps (e.g. student "Zen Flow" yoga app: pose library, programs, progress tracking, reminders): https://github.com/ericazpcalaca/Yoga_App — demo-grade, not a maintained product; no evidence of per-stretch timers + history + export → unknown/N.
- Net: no open-source app found that combines animations/video per stretch + per-stretch timers + history. (Searched; absence is a finding, not a proof — marked unknown/N per row.)

### Gym loggers that include mobility (animation/video)
- Fitbod: warm-up/cool-down stretching (soft-tissue, dynamic, static; iOS primers), tailored to muscles/equipment/session length: https://help.fitbod.me/hc/en-us/articles/360019715433-Stretching-Warm-up-Cool-down ; per-exercise demo video not confirmed on that page → partial.
- JEFIT: community/official stretch & mobility routines from its exercise DB (Stretch Series v2: 14 stretches × 30–45 s intervals as superset flows: https://www.jefit.com/routines/302810/stretch-series-v2 ; Mobility Flexibility Combo: https://www.jefit.com/routines/870662/jefit-mobility-flexibility-combo ; example cool-down: https://www.jefit.com/routines/596880/stretching-and-cool-down) + editorial daily-pliability guidance: https://www.jefit.com/blog/daily-mobility-matters-the-benefits-of-training-with-jefit ; dedicated per-stretch countdown UX: unknown → partial.
- Fitify family (above): closest to "gym logger + mobility with animations in one ecosystem" via Gym/Workouts + Stretching apps.

## Combining a stretch app with a gym logger
- No stretch app above logs barbell gym sessions (items 1/N across the board) — pairing is always two apps side by side. Practical pairs: Fitify Gym + Fitify Stretching (same vendor/UI/history); JEFIT or Fitbod alone (gym + built-in mobility, weaker dedicated-stretch UX); any of Bend/GOWOD/Pliability/Leap/Down Dog alongside Hevy/Strong/etc. (manual; no integration verified).
- Verified integrations are elsewhere, not with gym loggers: GOWOD↔Strava: https://www.gowod.app/ ; Pliability↔Garmin/Apple Health/Whoop: https://play.google.com/store/apps/details?hl=en&id=com.romwodllc.android ; Pliability↔SugarWOD gym programming: https://pliability.com/ ; Down Dog→Apple Health: https://apps.apple.com/us/app/yoga-down-dog/id983693694 ; Fitify→Apple Health: https://apps.apple.com/us/app/fitify-home-workout-ai-coach/id1463386686 . Health Connect session import for stretch apps: unknown across the board.

## 3 closest matches to brief item 8 (+ combinability), and exact gaps
1. **Fitify "Flexibility & Stretching" (+ sibling Fitify Gym/Workouts)** — only option covering HD-video-per-stretch + voice coach + duration/rest timers + history calendar + offline, AND a gym logger with reps/weight charts in the same family.
   Gaps: Health Connect import unknown (Apple Health only, verified); no JSON/CSV export verified; no weekly drag-to-move calendar verified (weekly plan of sessions only); no dark-dense-UI/widget verification; no cardio per-round HR/watts; no fasting icon; freemium/closed source (one custom workout free; Pro = subscription: https://play.google.com/store/apps/details?id=com.fitifyworkouts.bodyweight.workoutapp&hl=en).
2. **GOWOD (free test + premium)** — best dedicated mobility loop: free assessment + 300 free videos, per-stretch demo video + countdown timer, personalized/activation/recovery protocols, monthly retest history, Strava sync.
   Gaps: premium-gated personalization/history; offline downloads unknown; no Health Connect import verified; no JSON/CSV export; no gym logging/cardio/week-plan/BW/fasting; closed source, subscription (~$12/mo range; confirm live listing).
3. **Bend** — simplest habit app: per-stretch timer + illustrations (+videos per Wirecutter/listing), custom routines, streaks/analytics, offline per user reviews, cheapest (~$24–40/yr promos).
   Gaps: video-vs-illustration format disputed (see pliability comparison); no gym/cardio/plan/BW/HC-import/export/fasting; account lock-in risk (cloud backup implied, JSON export unknown); closed source, subscription + only one free routine.
- Runner-up premium pick: **Pliability** over GOWOD if you want longer coached daily videos + offline + Garmin/Whoop/Apple sync (gaps: priciest $22.95/mo or $199.95/yr: https://pliability.com/mobility; same no-gym/no-HC/no-export gaps). Runner-up free pick: **Leap Stretch Exercise** (free, animation+video+voice, auto history + weight chart; gaps: timer/offline/export/HC all unverified, ads, no gym combo).
