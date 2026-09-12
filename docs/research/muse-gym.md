# Gym strength loggers — scored vs Khon brief items 1–9

Brief items: **1** gym logging (named programs+active, superset blocks A1/B1, prefill last, one-tap done, session-only vs program swap, muscle tags, volume/best/e1RM chart, sets-per-muscle/week) · **2** cardio (steady time/dist/pace/laps/HR/watts + interval timer w/ voice, beeps, lock-screen, per-round HR/watts) · **3** week plan template + per-date overrides + drag-to-move calendar · **4** bodyweight quick entry + chart · **5** Health Connect session import (Mi Fitness + Samsung Health, merge overlaps) · **6** dark dense UI, no cards/pills/h-scroll, widget w/ today+Start · **7** offline + JSON export/import, free, self-update from GitHub · **8** stretching routines w/ per-stretch timers + animations/video + history · **9** eating-window tracker where the app icon itself shows open/closed + reminders/history.
Scoring: **Y** = verified cover · **partial** = verified part-cover · **N** = verified absent · **unknown** = could not verify (no official doc found).

> Every factual claim below cites its source. Anything without a citable official/store doc is marked **unknown** — not assumed.

## Score table (items 1–9)

| App (platform) | 1 gym | 2 cardio | 3 week plan | 4 bodyweight | 5 HC import | 6 dense UI+widget | 7 offline/JSON/free/GitHub | 8 stretch | 9 fasting icon |
|---|---|---|---|---|---|---|---|---|---|
| Hevy (Android+iOS) | partial | N | partial | partial | unknown (write only verified) | unknown | N | N | N |
| Strong (Android+iOS) | partial | N | partial | partial | unknown (Apple Health/Google Fit only verified) | unknown | N | N | N |
| FitNotes (Android only) | partial | N | N | Y | N | unknown | partial | N | N |
| Liftin' (iOS only) | partial | N | partial | unknown | N (iOS, no HC) | unknown | unknown | N | N |
| GymRun / MazeFX (Android only) | partial | partial | partial | partial | unknown (write verified, read unknown) | unknown | partial | N | N |
| StrengthLog (Android+iOS) | Y* (*charts need Premium) | N | partial | partial | N (write-only verified) | unknown | N | N | N |
| JEFIT (Android+iOS) | partial | N | partial | Y | unknown | unknown | N | partial | N |
| Boostcamp (Android+iOS) | Y* (*muscle heatmap needs Pro) | N | partial | partial | unknown (write verified, read unknown) | unknown | partial | N | N |
| Alpha Progression (Android+iOS) | partial | N | partial | Y | unknown (write verified, read unknown) | unknown | N | N | N |
| Setgraph (Android+iOS) | partial | N | partial | unknown | unknown | unknown | N | N | N |
| Gymtrack / gymtrack.io Viking Tech (Android+iOS) | partial | N | partial | unknown | unknown | unknown | N | N | N |
| Fitbod (Android+iOS) | partial | N | N | Y | N (metrics in, workouts out) | unknown | N | N | N |
| Massive / brandonp2412 (Android+iOS, deprecated) | partial | unknown | partial | unknown | unknown | unknown | partial | unknown | N |
| LiftLog / LiamMorrow liftlog.online (Android+iOS, AGPL) | partial | N | partial | unknown | unknown | unknown | partial | N | N |
| wger (Android+iOS+web+F-Droid, AGPL) | partial | partial | partial | partial | unknown | unknown | partial | N | N |
| Gym Log by Amit Chaudhary | unknown | unknown | unknown | unknown | unknown | unknown | unknown | unknown | unknown |
| Flexify / brandonp2412 Massive successor (Android+iOS, MIT) | partial | partial | unknown | unknown | unknown | unknown | partial | unknown | N |
| FastNFitness / brodeurlv (Android, BSD-3) | partial | partial | unknown | partial | unknown | unknown | partial | unknown | N |

Why these scores (one line each, all cited in detail section below):

- **Hevy item 1 = partial:** supersets + prefill + one-tap + session-swap + e1RM + muscle graphs all verified, but programs are routines+folders with no day-of-week auto-schedule. Items 2,7,8,9 verified absent/unmet; item 5 no read-import doc (only write to HC documented).
- **Strong item 1 = partial:** supersets + prefill + one-tap + session-swap + 1RM/volume graphs verified, but no weekly calendar schedule doc and no per-muscle weekly-sets doc found.
- **FitNotes item 1 = partial:** supersets/groups + prefill + opt-in one-tap + Replace-exercise + e1RM/volume graphs verified, but graphs are per-exercise only (no muscle aggregation) and routines have named days but no auto calendar.
- **Liftin' item 1 = partial, platform blocker:** supersets + plans-with-weeks + graphs/1RM verified, but iOS-only (fails brief owner on Android 9), prefill/swap/export/HC unverified.
- **GymRun item 1 = partial, item 2 = partial:** supersets + plans + Smart Calendar + prefill + 1RM/volume + Tabata stopwatch verified; one-tap + per-muscle chart unverified; Android-only (no iOS from this dev — App Store same-name app is a different app).
- **StrengthLog item 1 = Y***: only app with all of supersets + multi-week programs + copy-last + tap-to-green + session Change + E1RM curve + per-muscle stats/reports verified — but E1RM + per-muscle need Premium, and HC workouts are verified write-only (item 5 = N).
- **JEFIT item 1 = partial:** supersets + plans + day-start + smart-weight/progression + 1RM charts + BodyMap verified, but BodyMap is % focus map not weekly per-muscle sets; one-tap/export/HC/offline unverified.
- **Boostcamp item 1 = Y***: supersets + 11k programs/daily + last-weight + tap-to-log + mid-workout alternatives + e1RM free + per-muscle heatmap (Pro) + offline verified; export unverified, HC read unverified.
- **Alpha Progression item 1 = partial:** supersets + frequency-based generator + algorithmic prefill + sets-per-muscle + volume charts verified, but exact one-tap + session-only swap + "e1RM" label unverified (uses "strength rating").
- **Setgraph item 1 = partial:** supersets/circuits + scheduled lists/AI + swipe-replicate + minimal-tap + 1RM formula verified, but session-swap + per-muscle weekly + bodyweight + HC + offline unverified (CSV export only verified via Reddit, not official docs).
- **Gymtrack.io item 1 = partial:** the closest charts match (e1RM any-window + weekly sets per muscle vs MEV/MRV, Pro) + prefill + scheduler 14-day + offline-first + CSV(Pro) verified, but supersets + bodyweight + session-swap + HC unverified; do not conflate with other same-name apps.
- **Fitbod item 1 = partial, item 3 = N:** supersets/circuits + algorithmic prefill + Estimated Strength (ex-1RM) + Muscle Strength/Volume by week verified, but explicitly "no built-in weekly planner yet"; HC verified as metrics-in/workouts-out (item 5 = N for sessions).
- **Massive:** deprecated/archived (banner points to Flexify); offline + rest timers + graphs + day-planner partially verified; supersets/prefill/one-tap/swap/e1RM/per-muscle/bodyweight-chart/HC/export all unverified.
- **LiftLog (LiamMorrow):** AGPL, local-only, 1-tap, auto rest, prefill, supersets (plan JSON), plaintext CSV+JSON export verified; e1RM/per-muscle/swap/bodyweight/HC/weekly-schedule unverified. Note: brief's "liamcribbs/liftlog.xyz" not found — verified identity is LiamMorrow/liftlog.online.
- **wger:** AGPL, routines with fit-in-week/cycle, auto-superset, gym-mode one-tap, rest timer, weight+measurements, partial offline (2.6 sqlite+sync), REST API verified; prefill/e1RM/per-muscle/HC/export unverified.
- **Gym Log by Amit Chaudhary: unknown** — no verified identity; GitHub/Play searches return unrelated same-name apps; needs package name/repo to re-check.
- **Flexify / FastNFitness:** free + offline + strength+cardio + graphs verified at high level; most Khon-critical specifics (prefill, one-tap, swap, e1RM, per-muscle/week, HC import, JSON) unverified from official pages fetched.

## Detail matrix (what was actually verified)

### Hevy — proprietary, Android+iOS+Web+Watch/Wear
- Supersets Y (2+ exercises, giant sets): https://help.hevyapp.com/hc/en-us/articles/34953606698903-Build-a-Workout-Program-Create-Organize-Routines
- Programs partial (routines+folders+26-program library, no dow auto-schedule): https://help.hevyapp.com/hc/en-us/articles/34953606698903-Build-a-Workout-Program-Create-Organize-Routines · https://www.hevyapp.com/features/gym-workout-routines
- Prefill Y (PREVIOUS col, tap-to-fill): https://help.hevyapp.com/hc/en-us/articles/36011896355479-How-to-Use-Previous-Workout-Values-to-Improve-Performance-in-Hevy
- One-tap Y (checkmark → rest timer): https://www.hevyapp.com/features/workout-rest-timer
- Session swap Y (Update vs Keep Original): https://help.hevyapp.com/hc/en-us/articles/38387296276375-Update-Routine-vs-Keep-Original-Routine
- e1RM Y + muscle graphs Y (set-count-per-muscle = Pro): https://apps.apple.com/us/app/hevy-workout-tracker-gym-log/id1458862350 · https://help.hevyapp.com/hc/en-us/articles/35119778922263-Hevy-Pro-Subscription-How-to-get-Pro-and-What-Does-It-Include
- Bodyweight log Y, chart unknown: https://help.hevyapp.com/hc/en-us/articles/38001424401943-How-to-Import-Strong-App-CSV-Files-and-Export-Your-Data-in-Hevy
- Rest timer Y (5s–5min, sounds): https://www.hevyapp.com/features/workout-rest-timer
- Health Connect: write verified, read/import unknown: https://help.hevyapp.com/hc/en-us/sections/34895885362455-Health-Connect
- Offline unknown (no official doc; 3rd-party claims offline works: https://prpath.app/blog/hevy-app-review-2026.html)
- Export CSV (`workout_data.csv` + `measurement_data.csv`), no JSON: https://help.hevyapp.com/hc/en-us/articles/38001424401943-How-to-Import-Strong-App-CSV-Files-and-Export-Your-Data-in-Hevy
- Price free + Pro ($2.99/mo, $23.99/yr, $74.99 lifetime on hevy.com; App Store US differs $4.99/$38.99/$119.99): https://hevy.com/pricing
- Open source N. Cardio N (rest timer only). Stretch N. Fasting-icon N (unknown — no doc).

### Strong — proprietary Strong Fitness, Android+iOS+Watch
- Supersets Y: https://www.strong.app/ · https://play.google.com/store/apps/details?id=io.strongapp.strong&hl=en
- Programs partial (templates, no weekly calendar doc): https://help.strongapp.io/article/106-edit-template
- Prefill Y: https://help.strongapp.io/article/177-update-template
- One-tap Y (checkbox): https://help.strongapp.io/article/229-my-first-workout
- Session swap Y (Update Template/Values/Keep Original): https://help.strongapp.io/article/177-update-template
- e1RM + volume/1RM graphs Y; per-muscle weekly unknown (heat map listed, no weekly-sets doc): https://play.google.com/store/apps/details?id=io.strongapp.strong&hl=en · https://www.strong.app/
- Bodyweight log Y (Weight/BF/Calories + PRO rest), chart unknown: https://help.strongapp.io/article/238-add-measurements
- Rest timer Y (auto 2:00, per-exercise): https://help.strongapp.io/article/231-rest-timer
- HC import unknown (only Apple Health/Google Fit sync docs): https://help.strongapp.io/article/147-sync-with-apple-health
- Offline unknown. Export CSV (no re-import): https://help.strongapp.io/article/235-export-workout-data
- Price free (3 custom routines) + PRO $4.99/mo or $29.99/yr, Forever ~$89.99–129.99: https://apps.apple.com/es/app/strong-workout-tracker-gym-log/id464254577?l=en-GB
- Open source N.

### FitNotes (James Gay) — proprietary free, Android only
- Supersets Y (Groups→Supersets): https://www.fitnotesapp.com/routines/ · http://www.fitnotesapp.com/release_1_21
- Programs partial (routines w/ named days, Log All, no calendar): http://www.fitnotesapp.com/routines/
- Prefill Y (auto-populate from last): http://www.fitnotesapp.com/workout_tracking/
- One-tap Y opt-in (Mark Sets Complete): http://www.fitnotesapp.com/settings/
- Session swap Y (Replace Exercise, sets transfer): http://www.fitnotesapp.com/release_1_21
- e1RM Y + Max/Volume/Reps graphs Y; per-muscle weekly N (per-exercise only): http://www.fitnotesapp.com/progress_tracking/
- Bodyweight Y + graph Y (Body Tracker/History/Graph): http://www.fitnotesapp.com/body_tracker/
- Rest timer Y: http://www.fitnotesapp.com/workout_tools/
- HC import N (no integration doc). Offline Y (local-only, no account): https://play.google.com/store/apps/details?hl=en_NZ&id=com.github.jamesgay.fitnotes
- Export CSV (not restorable) + `.fitnotes` backup + Drive auto-backup, no JSON: https://www.fitnotesapp.com/settings/
- Price free no ads; optional Supporter one-time: http://www.fitnotesapp.com/faq/
- Open source N (github brstuder/fitnotes is docs mirror only: https://github.com/brstuder/fitnotes/blob/main/docs/howto.md). Platform Android only, no iOS planned: http://www.fitnotesapp.com/faq/

### Liftin' (Valter Hemmi) — proprietary, iOS only
- Supersets Y: https://www.liftinapp.co/
- Programs Y (5x5/Wendler/nSuns, plans with weeks, auto-progression): https://www.liftinapp.co/
- Prefill unknown. One-tap Y (incl. Live Activity/lock): https://www.youtube.com/watch?v=I2T7g1Elcoo
- Session swap unknown. e1RM/graphs Y (weight/volume/1RM/TM): https://www.liftinapp.co/ · https://apps.apple.com/us/app/liftin-gym-workout-tracker/id1445041669
- Per-muscle weekly unknown. Bodyweight unknown (only Progress Photos doc). Rest timer Y: https://www.liftinapp.co/
- HC N/A (iOS; Apple Health+Strava): https://www.liftinapp.co/
- Offline unknown. Export unknown.
- Price free (5 workouts/mo) + Unlimited $24.99/yr: https://apps.apple.com/us/app/liftin-gym-workout-tracker/id1445041669
- Open source N. Platform iOS-only (no Play listing): https://apps.apple.com/us/app/liftin-gym-workout-tracker/id1445041669

### GymRun (MazeFX/Ronny Ebert) — proprietary, Android only
- Supersets Y; programs + Smart Calendar Y; prefill Y (historical autofill); session-swap + plan-edit both Y; 1RM/volume/cal/rest analytics Y; body metrics Y (chart unknown); rest countdown per exercise + stopwatch w/ auto rounds (Tabata) Y; HC/Garmin/Fit/Samsung sync = write verified, read unknown; CSV export + backup Y; free no-ads + PRO IAP (exact price unknown); proprietary; Android+Wear/Garmin/Galaxy, no iOS from this dev: https://play.google.com/store/apps/details?id=com.imperon.android.gymapp&hl=en_US · https://www.gymrun.app/ · caution unrelated iOS same-name: https://apps.apple.com/us/app/gymrun/id6744241382
- One-tap unknown. Per-muscle weekly unknown. Offline unknown.

### StrengthLog (Sweden) — proprietary, Android+iOS
- Supersets Y: https://www.strengthlog.com · https://help.strengthlog.com/help-article/special-sets/
- Programs Y (200+ programs, plan in advance): https://www.strengthlog.com
- Prefill Y (copy-last tick + Retrieve-from-log): https://help.strengthlog.com/help-article/how-to-record-a-workout/ · https://help.strengthlog.com/help-article/retrieve-from-training-log/
- One-tap Y (tap set number → green): https://help.strengthlog.com/help-article/mark-a-set-as-done/
- Session swap Y (swipe Change) + multi-week program builder Y: https://help.strengthlog.com/help-article/how-to-record-a-workout/
- E1RM curve + per-set e1RM Y (Premium); per-muscle stats + anatomy map + Training Reports (most-trained muscles) Y (Premium): https://help.strengthlog.com/help-article/strengthlog-premium/ · https://help.strengthlog.com/help-article/training-reports/
- Bodyweight log Y (free), chart unknown; reads BW+HR from HC: https://www.strengthlog.com · https://play.google.com/store/apps/details?id=com.styrkelabbet.Styrkelabbet
- Rest timer Y (auto + per-exercise + XL): https://help.strengthlog.com/help-article/how-to-use-the-timer/
- HC session import N — explicitly write-only ("does not receive data in return"): https://play.google.com/store/apps/details?id=com.styrkelabbet.Styrkelabbet
- Offline unknown (account/cloud backup: https://www.strengthlog.com). Export unknown.
- Price free + Premium $5.99/1mo, $12.99/3mo, $39.99/yr (App Store USD).
- Open source N.

### JEFIT (Jefit Inc.) — proprietary, Android+iOS+watch
- Supersets Y: https://www.jefit.com/
- Programs Y (1000s plans + custom; start-from-day): https://www.jefit.com/ · https://www.jefit.com/support/faq
- Prefill Y (smart weight/rep + Load Progression Engine): https://www.jefit.com/blog/faqs-tracker-mode · https://www.jefit.com/support/faq
- One-tap unknown. Session swap Y (Instant Workout add/delete/swap) + program edit Y: https://www.jefit.com/support/faq
- 1RM/time/weight charts Y (full Analytics = Elite); BodyMap % focus = partial, not weekly sets: https://www.jefit.com/ · https://www.jefit.com/elite
- Body stats + chart Y: https://www.jefit.com/blog/body-stat-goals-how-to-set-and-track-them
- Rest timer Y (auto-logged, per-plan): https://www.jefit.com/support/faq
- HC import unknown. Offline unknown. Export unknown.
- Price Basic free; Elite $12.99/mo, $69.99/yr: https://www.jefit.com/elite
- Open source N.

### Boostcamp — proprietary, Android+iOS+Watch
- Supersets/drop/warmup-templates Y (free); 11k+ programs/daily workouts/days-filter Y; last-weight + auto-progression Y; tap-to-log + auto timer Y; mid-workout alternatives (weights carry) + fork/build Y; e1RM curve + PRs Y (free); per-muscle heatmap Y (Pro); bodyweight sync w/ Apple Health/HC Y (in-app chart unknown); rest timers + Live Activities Y; HC save verified, read unknown; offline Y (once loaded); export unknown; free + Pro from $4.99/mo, annual $59.99 (was $79.99): https://www.boostcamp.app/features · https://www.boostcamp.app/workout-tracker · https://boostcamp.app/ · https://www.boostcamp.app/campaign · https://play.google.com/store/apps/details?id=com.bpmhealth.boostcamp
- Open source N.

### Alpha Progression (GmbH) — proprietary, Android+iOS
- Supersets Y (incl. dropsets): https://alphaprogression.com/en
- Programs Y (frequency generator, PPL/Upper-Lower, week-by-week + deload): https://alphaprogression.com/en
- Prefill Y algorithmic: https://apps.apple.com/au/app/gym-workout-alpha-progression/id1462277793
- One-tap unknown (only rep-counter log verified). Session swap unknown (one-off workouts vs edit-anytime: https://alphaprogression.com/en).
- Charts: weight/strength-rating/volume Y; sets-per-muscle last-3mo + volume-per-muscle Y; exact "e1RM" label unknown: https://play.google.com/store/apps/details?id=com.alphaprogression.alphaprogression&hl=en_US
- Body measurements Y (free) + charts (Pro): https://alphaprogression.com/en
- Rest timer Y: https://apps.apple.com/au/app/gym-workout-alpha-progression/id1462277793
- HC write verified, import unknown: https://sahha.ai/integrations/alpha-progression/
- Offline unknown. Export CSV Y, JSON unknown: https://alphaprogression.com/en
- Price free + Pro $12.99/mo, $79.99/yr, 14-day trial: https://alphaprogression.com/en/subscribe
- Open source N.

### Setgraph (LLC) — proprietary, Android+iOS+Watch
- Supersets/circuits/multisets Y: https://apps.apple.com/us/app/setgraph-gym-workout-tracker/id1209781676
- Programs Y (lists by Workout/Muscle/Program/Dow + AI schedule→save-to-tracking): https://play.google.com/store/apps/details?id=app.setgraph&hl=en_US · https://setgraph.app/articles/get-a-custom-workout-plan-with-setgraph-ai
- Prefill Y (swipe replicate, history pull, last-session compare): https://setgraph.app/
- One-tap Y (minimal-tap/swipe-to-log): https://apps.apple.com/us/app/setgraph-gym-workout-tracker/id1209781676
- List edit Y; session-only swap unknown.
- 1RM formula choice + % tables + dynamic/correlation graphs Y; per-muscle weekly unknown: https://play.google.com/store/apps/details?id=app.setgraph&hl=en_US · https://setgraph.app/features/workout-tracker
- Bodyweight unknown (only calories/incline/sport metrics listed). Rest auto-start + lock/Dynamic Island Y: https://apps.apple.com/us/app/setgraph-gym-workout-tracker/id1209781676
- HC unknown (only Apple Health+Watch verified). Offline unknown (3rd-party claim only: https://setgraph.app/ai-blog/best-app-for-tracking-workouts).
- Export CSV Y (via Settings→Account→Export; reported on Reddit, not official docs): https://www.reddit.com/r/Setgraph/comments/1rcqoy3/data_export_is_now_live_csv_export_available/ ; JSON unknown.
- Price 5 free workouts + trial; Pro $4.99/mo, $29.99/yr, Lifetime $199.99: https://setgraph.app/articles/setgraph-app-review-2025-complete-workout-tracker-guide-features
- Open source N: https://setgraph.app/terms-service

### Gymtrack (gymtrack.io, Viking Tech) — proprietary, Android+iOS
- Supersets unknown (site lists warm-ups/drop/failure, unilateral — no superset grouping): https://gymtrack.io/
- Programs Y (4–12wk prebuilt, queued day week-by-week, 14-day scheduler, own programs 3 free/unlimited Pro): https://gymtrack.io/
- Prefill Y ("last session's weights filled in"): https://gymtrack.io/
- Set log ~2-tap ("Weight, reps, RPE. Tick. Rest."), exact one-tap unknown: https://gymtrack.io/
- Session swap unknown. e1RM any-window + windowed graphs Y; weekly sets per muscle vs MEV/MRV Y (Pro): https://gymtrack.io/
- Bodyweight unknown. Rest presets 60/90/120/180 + plate calc Y: https://gymtrack.io/
- HC unknown (no claim on site/privacy; stack = own servers+RevenueCat/PostHog/Claude: https://gymtrack.io/privacy).
- Offline-first Y: https://gymtrack.io/
- Export CSV Y (Pro), JSON unknown: https://gymtrack.io/ · https://gymtrack.io/privacy
- Price free (7 exercises/3 programs/5 AI msgs/day) + Pro $9.99/mo or $49.99/yr: https://gymtrack.io/
- Open source N. Note: other same-name apps (e.g. https://apps.apple.com/pk/app/gymtrack-workout-tracker/id6759541849 ) are different apps — do not conflate.

### Fitbod (Inc.) — proprietary, Android+iOS+Watch/Wear
- Supersets+circuits Y: https://help.fitbod.me/hc/en-us/articles/360006427813-Circuits-Supersets
- Weekly planner N ("doesn't have a built-in weekly planner yet" — use Saved Workouts named Monday/etc.): https://help.fitbod.me/hc/en-us/articles/6259258835863-Save-a-Workout
- Prefill Y algorithmic: https://help.fitbod.me/hc/en-us/articles/360004429814-How-Fitbod-Creates-Your-Workout
- One-tap unknown. Session substitute/skip/add Y: https://fitbod.me/faqs
- Estimated Strength (ex-Projected 1RM) + history Y; Muscle Strength/Volume per group by W/M/6M/Y Y: https://fitbod.zendesk.com/hc/en-us/articles/12732749777047-Fitbod-Metrics-Records · https://help.fitbod.me/hc/en-us/articles/16436302450711-Your-Workout-Report
- Body Composition Weight/BF/Lean via HC/Health or manual + W/M/6M/Y dashboard Y: https://help.fitbod.me/hc/en-us/articles/18213217448983-Body-Composition
- Rest timer Y (auto after set, per-exercise, notifications): https://help.fitbod.me/hc/en-us/articles/360006340194-Rest-Timer
- HC: body metrics in, workouts out; session import N: https://help.fitbod.me/hc/en-us/articles/18213217448983-Body-Composition · https://fitbod.me/faqs
- Offline partial (generated workout completable offline): https://fitbod.me/faqs
- Export unknown (no doc). Price $15.99/mo, $95.99/yr, 7-day trial: https://fitbod.me/faqs
- Open source N.

### Massive (brandonp2412) — GPL-3.0, Android+iOS, DEPRECATED
- Deprecated: banner "install Flexify instead (http://github.com/brandonp2412/Flexify)": https://github.com/brandonp2412/Massive ; license GPL-3.0: https://github.com/brandonp2412/Massive
- Offline Y ("completely offline"): https://github.com/brandonp2412/Massive ; rest timers Y; progress graphs Y; day planner + Plan files partial (weekly semantics unknown).
- Supersets/prefill/one-tap/swap/e1RM/per-muscle/HC/export unknown (has `EditWeight/WeightList/ViewWeightGraph` files, semantics unknown).
- Price free (no IAP found). Cardio unknown.

### LiftLog (LiamMorrow, liftlog.online) — AGPL-3.0, Android+iOS
- Identity: site source → https://github.com/LiamMorrow/LiftLog ("React Native + Expo"): https://liftlog.online/ · https://github.com/LiamMorrow/LiftLog (brief's liamcribbs/liftlog.xyz not found — unknown).
- AGPL: https://liftlog.online/ ; free core forever + one-time AI purchase: https://liftlog.online/ ; stores: https://play.google.com/store/apps/details?id=com.limajuice.liftlog · https://apps.apple.com/au/app/liftlog/id6467372581 ; web removed (README).
- Supersets Y (`supersetWithNext`, plan JSON): https://github.com/LiamMorrow/LiftLog/blob/main/docs/PlanFileFormat.md ; programs Y (saved programs, upcoming; `.liftlogplan` JSON): https://liftlog.online/ · weekly-schedule unknown.
- Prefill Y ("remembers last session"): https://liftlog.online/
- One-tap Y ("1 tap", "single tap"): https://liftlog.online/ · https://play.google.com/store/apps/details?id=com.limajuice.liftlog
- Rest auto-start Y: https://liftlog.online/
- Charts generic ("lifts trending") + PRs; e1RM/per-muscle unknown: https://liftlog.online/
- Offline Y (local, no account): https://liftlog.online/
- Export CSV+JSON plaintext (Settings→Export/Backup/Restore→Plaintext; not a backup) + separate `.liftlogbackup` gz-protobuf: https://github.com/LiamMorrow/LiftLog/blob/main/docs/PlaintextExport.md · https://github.com/LiamMorrow/LiftLog/issues/244
- Session swap / bodyweight / HC import unknown.

### wger (wger-project) — AGPL-3.0+, web+Android+iOS+F-Droid+Flathub
- FOSS AGPL-3.0+: https://github.com/wger-project/wger/blob/master/README.md ; docs: https://wger.readthedocs.io/en/latest/ ; Play: https://play.google.com/store/apps/details?id=de.wger.flutter ; free: https://play.google.com/store/apps/details?id=de.wger.flutter
- Routines Y (fit-in-week Mon/Wed/Fri or cycle, needs-logs-to-advance): https://wger.readthedocs.io/en/latest/manual/routines.html
- Auto-superset Y (2+ exercises/set = alternating superset in gym mode): https://wger.readthedocs.io/en/latest/manual/routines.html
- Gym-mode one-tap Y: https://play.google.com/store/apps/details?id=de.wger.flutter ; rest per-set Y (+timed-exercise timer 2.5): https://wger.readthedocs.io/en/latest/manual/routines.html · https://github.com/wger-project/wger/releases/tag/2.5
- Weight + measurements Y (basic): https://play.google.com/store/apps/details?id=de.wger.flutter · https://github.com/wger-project/wger/
- Offline partial (2.6 sqlite+sync; routines/gallery/account exceptions): https://github.com/wger-project/wger/releases ; REST API: https://github.com/wger-project/wger/blob/master/README.md
- Prefill / session-swap / e1RM / per-muscle / HC import / export format unknown.

### Gym Log by Amit Chaudhary — unknown
- No verified identity. GitHub Amit Chaudhary/Choudhary hits (https://github.com/amit9838, https://github.com/amit-choudhary, https://github.com/chaudhary-amit, https://github.com/amitness) show no gym-log app; Play same-name hits are unrelated (Arek Goleń https://play.google.com/store/apps/details?id=com.gym.log&hl=en_GB; Leap https://play.google.com/store/apps/details?id=gymworkout.gym.gymlog.gymtrainer&hl=en_US; LGHDeveloper https://play.google.com/store/apps/details?hl=en&id=com.lghdeveloper.gymlog). All fields unknown — needs package/repo.

### Others found (open source)
- **Flexify** (Massive successor, MIT, Android+iOS+Windows, free no-ads/tracking, offline Y — "doesn't use internet at all", rest timers/alarms Y, strength+cardio+graphs Y; plans-per-day via secondary source only; supersets/prefill/one-tap/swap/e1RM/per-muscle/bodyweight-chart/HC/export unknown): https://github.com/brandonp2412/Flexify · https://brandonp2412.github.io/Flexify/ · https://f-droid.org/en/packages/com.presley.flexify · https://play.google.com/store/apps/details?id=com.presley.flexify · https://apps.apple.com/us/app/flexify/id6503730178
- **FastNFitness** (brodeurlv, BSD-3, Android-only, free; strength+cardio+isometric, custom machines, progress/body graphs, weight tracking; specifics unknown): https://github.com/brodeurlv/fastnfitness · https://f-droid.org/en/packages/com.easyfitness/
- **FitTrackee** excluded as strength logger (outdoor/GPX self-hosted web, AGPL; N/A for sets/reps): https://github.com/SamR1/FitTrackee

## Interval timer, export/price/open-source quick reference

| App | Interval timer (beyond rest) | Export | Price | Open source |
|---|---|---|---|---|
| Hevy | rest only (unknown for intervals) | CSV, no JSON | free + Pro ~$24/yr–$39/yr (region varies) | N |
| Strong | rest only | CSV, no re-import | free + PRO ~$30/yr | N |
| FitNotes | rest only | CSV + `.fitnotes` backup, no JSON | free (Supporter one-time) | N |
| Liftin' | rest only (unknown) | unknown | free 5/mo + $25/yr | N |
| GymRun | stopwatch w/ auto rounds (Tabata) — partial | CSV + backup | free no-ads + PRO (price unknown) | N |
| StrengthLog | rest only | unknown | free + $40/yr | N |
| JEFIT | rest only | unknown | free + Elite $70/yr | N |
| Boostcamp | rest + Live Activities (no Tabata doc) | unknown | free + Pro from $5/mo (~$60/yr) | N |
| Alpha Progression | rest only | CSV | free + $80/yr | N |
| Setgraph | rest auto + Next-Due (no Tabata doc) | CSV (JSON unknown) | 5 free + $30/yr or $200 life | N |
| Gymtrack.io | rest presets only | CSV (Pro), no JSON | free + $50/yr or $10/mo | N |
| Fitbod | rest only | unknown | ~$96/yr | N |
| Massive | rest Y (intervals unknown) | unknown | free | Y (GPL-3.0, deprecated) |
| LiftLog | rest auto (intervals unknown) | CSV + JSON plaintext + `.liftlogbackup` | free core + one-time AI | Y (AGPL-3.0) |
| wger | per-set rest + timed-exercise timer (intervals unknown) | unknown (REST API) | free | Y (AGPL-3.0+) |
| Flexify | rest alarms (intervals unknown) | unknown | free | Y (MIT) |
| FastNFitness | unknown | unknown | free | Y (BSD-3) |

## The 3 closest matches to the full brief (1–9) and their exact gaps

None covers 2 (interval+HR/watts), 3 (drag calendar), 5 (HC import+merge), 6 (dense UI+widget verified), 7 (JSON+GitHub self-update), 8 (stretch anim), 9 (icon-switch fasting). Ranked by item-1 completeness + export/offline openness:

### 1. Boostcamp — best item-1 + offline, but closed + no HC-import/JSON/calendar/stretch/fasting
Gaps: per-muscle heatmap paywalled (Pro); no verified export at all (unknown — assume lock-in); HC verified write-only (read unknown → item 5 unknown); no weekly drag-calendar (daily workouts only → item 3 partial); rest timer only (no Tabata/4x4 voice+lock+per-round HR/watts → item 2 N); bodyweight in-app chart unknown (item 4 partial); UI/widget unknown; account/cloud sync (item 7 = partial at best: offline Y but no JSON/GitHub); stretch N; fasting N.

### 2. StrengthLog — best item-1 verification incl. E1RM + per-muscle reports, but HC import explicitly absent + no export doc
Gaps: E1RM + per-muscle need Premium; item 5 = N by vendor statement (workouts write-only; only BW+HR read); export format unknown (no doc → treat as lock-in); offline unknown (account required); no interval timer (item 2 N); plan-ahead only, no drag-calendar (item 3 partial); bodyweight chart unknown (item 4 partial); UI/widget unknown; no JSON/GitHub (item 7 N); stretch N; fasting N.

### 3. LiftLog (LiamMorrow) — best openness (AGPL + local + CSV&JSON plaintext + offline + both platforms), but weakest charts/calendar/cardio
Gaps: e1RM chart unknown (only generic trend+PRs); per-muscle weekly unknown (item 1 partial); weekly-schedule semantics unknown + session-only swap unknown + bodyweight unknown; interval timer unknown/rest-only (item 2 N); calendar unknown (item 3 partial at best); HC import unknown (item 5 unknown); UI/widget unknown; plaintext export is not a restorable backup (separate `.liftlogbackup` protobuf) and self-update-from-GitHub unknown (item 7 partial); stretch N; fasting N.

**Honourable mentions:** **Gymtrack.io** has the exact charts the brief wants (e1RM any-window + weekly sets vs MEV/MRV) + offline-first + prefill + 14-day scheduler, but supersets/bodyweight/HC/export-JSON/session-swap all unknown and CSV is Pro-only; **FitNotes** is the best free-offline-Android workhorse (prefill/one-tap/swap/e1RM/CSV+backup, no account) but Android-only, no per-muscle weekly, no HC, no JSON, no calendar; **wger/Flexify** are the FOSS paths toward items 7 (+2/+4 partially) but all Khon-critical specifics (prefill, one-tap, swap, e1RM, per-muscle/week, HC import, JSON) remain unknown from official docs and need in-app verification.
