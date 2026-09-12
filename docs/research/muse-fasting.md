# Fasting / TRE apps vs Khon brief items 1-9

Brief items: 1 gym logging (supersets, programs, prefill, one-tap, swap, e1RM, muscle-week charts) · 2 cardio (steady + intervals, voice, lock-screen, per-round HR/watts) · 3 week plan calendar drag-move · 4 bodyweight chart · 5 Health Connect session import · 6 dense UI + widget · 7 offline + JSON/CSV + free + GitHub releases · 8 stretching · 9 eating-window timer with **icon showing open/closed**, reminders, history.
Score: Y / partial / N / unknown. Every factual claim has a source URL; unsourced = marked unverified.

## Key answers first

- **No verified app changes its home-screen icon by fasting state.** Web searches for `"fasting app" "alternate icon"`, `"activity-alias" fasting`, `fasting tracker "app icon changes"` return zero fasting apps advertising it; commercial apps instead offer notifications/widgets/Live Activities (e.g. Window "get notified when eating window is opened" — https://play.google.com/store/apps/details?hl=en_US&id=com.dailyburn.fasting.tracker ; Zero "reminders" — https://play.google.com/store/apps/details?hl=en_US&id=com.zerofasting.zero ; Simple iOS "Home Screen widget" — https://apps.apple.com/us/app/simple-ai-weight-loss-coach/id1467720176). iOS `setAlternateIconName` exists but needs foreground + pre-declared icons + shows a system alert (https://www.hackingwithswift.com/example-code/uikit/how-to-change-your-app-icon-dynamically-with-setalternateiconname, https://stackoverflow.com/questions/43356570/alternate-icon-in-ios-10-3-avoid-notification-dialog-for-icon-change, https://bryce.co/animated-app-icons/), so background auto-switch is unreliable — inference, mark as analysis. Android `activity-alias` mechanism exists (https://github.com/oguzhanaslann/DynamicIcon) but **no fasting app ties to it (unverified)**. Open-source manifests checked contain no state-driven launcher alias: FastTrack (https://raw.githubusercontent.com/Darkrock-Studios/FastTrack/main/app/src/main/AndroidManifest.xml), FastTimes (https://raw.githubusercontent.com/tom-murphy-development/FastTimes/main/app/src/main/AndroidManifest.xml), MyFastingApp (https://raw.githubusercontent.com/rohanwinsor/MyFastingApp/main/app/src/main/AndroidManifest.xml), ONE (https://raw.githubusercontent.com/charliesbot/one/main/app/src/main/AndroidManifest.xml); Solo Forge's only alias is Health Connect permission routing, not an icon (https://raw.githubusercontent.com/kirilan/SoloForge/main/app/src/main/AndroidManifest.xml).
- **Gym apps bundling a fasting timer (verified): MyFitnessPal Premium** — "Intermittent Fasting Tracker is a MyFitnessPal Premium feature that times your fasting and eating windows… Start Fast / Stop Fast… 12:12, 14:10, 16:8" (https://support.myfitnesspal.com/hc/en-us/articles/10983207647117-Track-Intermittent-Fasting-with-MyFitnessPal-Premium, https://blog.myfitnesspal.com/introducing-the-intermittent-fasting-tracker); **Cronometer Gold** — "Upgrade to Cronometer Gold to gain access to our Fasting Timer… New Fast… schedule… history" (https://cronometer.com/blog/fasting/, https://support.cronometer.com/hc/en-us/articles/360036016612-Mobile-Fasting, https://cronometer.com/gold/); **Solo Forge (FOSS)** — fasting timer + calorie/macros + weight + workout timers in one app (https://f-droid.org/en/packages/com.kbul.spicycrab/); **OpenNutriTracker (FOSS)** — "optional intermittent-fasting timer… home chip… completion notification" + JSON/CSV export (https://github.com/simonoppowa/OpenNutriTracker/blob/main/README.md). **JEFIT verified NO fasting timer** — Play (https://play.google.com/store/apps/details?hl=en&id=je.fit) and App Store (https://apps.apple.com/us/app/jefit-workout-plan-gym-tracker/id449810000) listings show no fasting; its fasting blog is diet advice only (https://www.jefit.com/blog/no-better-time-than-now-to-try-intermittent-fasting). **Leap covers both only as separate installs** — fasting app by Leap Fitness Group (https://play.google.com/store/apps/details?id=bodyfast.zero.fastingtracker.weightloss&hl=en_US) vs gym apps same vendor (https://apps.apple.com/tr/developer/leap-health/id1687598595) — not bundled.

## Commercial fasting apps

### Zero (zerolongevity.com)
- Timer/patterns/custom up to 7 days, editable start/end, reminders, history/stats/streaks, water goal: https://zerolongevity.com/, https://zerolongevity.com/blog/home, https://play.google.com/store/apps/details?hl=en_US&id=com.zerofasting.zero
- Price: free core + Plus $69.99/yr, $9.99/mo: https://zerofasting.zendesk.com/hc/en-us/articles/4402526584091-How-Much-Does-A-Zero-Plus-Subscription-Cost, https://fortune.com/article/best-intermittent-fasting-apps/
- Sync: Apple Health + Google Fit (+Fitbit/Oura on Plus); **no Health Connect documented**: https://zerofasting.zendesk.com/hc/en-us/articles/360047782774-Connecting-to-Apple-Health-and-Google-Fit
- Offline/export: none documented (unverified → unknown/N). No icon switching (unverified absence).

### Fastic (fastic.com/en)
- Timer + body-status phases + 12:12…20:4, reminders, progress insights, water/steps: https://fastic.com/en, https://apps.apple.com/us/app/fastic-weight-loss-fasting/id1459260306, https://play.google.com/store/apps/details?hl=en_US&id=de.fastic.app
- Price: free tier + Plus ~$12.99–14.99/mo, ~$27.99–29.99/3mo, $79.99/yr: https://nutriscan.app/blog/posts/fastic-pricing-2026-free-vs-plus-trial-refund-e13d2834f0
- Sync: Apple Health + Google Fit only; no Health Connect claimed: https://apps.apple.com/us/app/fastic-weight-loss-fasting/id1459260306, https://play.google.com/store/apps/details?hl=en_US&id=de.fastic.app
- Offline/export/icon: none documented.

### Simple (simple.life)
- Fasting tracker + flexible schedule, reminders, Success Score/weight graph/streaks, hydration: https://help.simple.life/en/articles/9887852-simple-life-overview-of-main-features, https://play.google.com/store/apps/details?id=life.simple&hl=en_AU, https://help.simple.life/en/articles/10221447-subscription-plans-with-simple
- Price: free basic + Premium (~$29.99/mo per Fortune; App Store $14.99–59.99 tiers): https://fortune.com/article/best-intermittent-fasting-apps, https://apps.apple.com/us/app/simple-ai-weight-loss-coach/id1467720176
- Sync Apple Health/Watch + Google Fit/Fitbit; Health Connect not documented: https://apps.apple.com/us/app/simple-ai-weight-loss-coach/id1467720176, https://play.google.com/store/apps/details?id=life.simple&hl=en_AU
- Widget: iOS Home Screen widget: https://apps.apple.com/us/app/simple-ai-weight-loss-coach/id1467720176. Offline/export/icon: none documented.

### Window (Mosaic/Bending Spoons; prompt's "Wunderkind" not found on current listings — unverified)
- Listings: https://apps.apple.com/us/app/window-intermittent-fasting/id1112765909, https://play.google.com/store/apps/details?hl=en_US&id=com.dailyburn.fasting.tracker
- Custom eating-window duration/start, "notified when eating window is opened", timeline/journal/photos/notes, weight graph, water: same Play/App Store pages.
- Price: free + Premium (1mo $14.99, weekly $4.99, yearly $39.99–89.99 variants): https://apps.apple.com/us/app/window-intermittent-fasting/id1112765909
- Sync Apple Health/Watch; Google-side undocumented. Offline/export/icon: none documented.

### BodyFast (bodyfast.app/en/)
- Timer+editor, start/end notifications (free), 12 plans free / 50+ with Coach, stats, weight/measurements, water: https://help.bodyfast.app/hc/en-001/articles/26281048221842-How-much-is-the-app, https://play.google.com/store/apps/details?hl=en_US&id=com.bodyfast
- Price: free basic + Coach (1mo $11.99–16.99, 3mo $24.49–34.99, 12mo $34.99–69.99 region-dependent): https://apps.apple.com/us/app/bodyfast-intermittent-fasting/id1189568780
- Health-platform sync: none documented in help/listings (unverified). Offline/export/icon: none documented.

### DoFasting (dofasting.com/app)
- 7 fasting schedules, agenda, weekly reminders/insights, calorie/water/steps/macro trackers, 5000+ recipes: https://dofasting.com/app, https://apps.apple.com/us/app/dofasting-intermittent-fasting/id1456288628, https://play.google.com/store/apps/details?hl=en_US&id=com.kilofasting
- Price: subscription-only quiz-gated ~1mo $37/mo, 3mo ~$16/mo, 6mo ~$11.50/mo: https://dofasting.com/app, https://help.dofasting.com/hc/en-us/articles/360013473657-How-much-does-the-app-subscription-cost
- Sync Apple Health + Google Fit (via https://fortune.com/article/best-intermittent-fasting-apps + https://apps.apple.com/us/app/dofasting-intermittent-fasting/id1456288628); Health Connect not found. Offline/export/icon: none documented.

### Fasting Tracker – Leap Fitness Group (bodyfast.zero.fastingtracker.weightloss, 50M+ downloads)
- One-tap start/end, custom periods, plans 14:10…23:1/5:2/28-day, notifications, graphs/weight/body-status, water (+AI food scanner new): https://play.google.com/store/apps/details?id=bodyfast.zero.fastingtracker.weightloss&hl=en_US, https://apps.apple.com/qa/app/intermittent-fasting-16-8/id1498018285
- Price: free with ads + IAP ($2.99–49.99; e.g. $12.99/mo, $39.99/yr): https://apps.apple.com/qa/app/intermittent-fasting-16-8/id1498018285, https://play.google.com/store/apps/details?id=bodyfast.zero.fastingtracker.weightloss&hl=en_US
- Sync Apple Health + Google Fit; Health Connect not documented. Offline/export/icon: none documented.

## Open-source (F-Droid) options

F-Droid search for "fasting" returns MyFastingApp, FastTrack, FastTimes, Qadaa (religious make-up, not TRE), Solo Forge, Mihrab (prayer, not TRE) — https://search.f-droid.org/?lang=en&q=fasting. "OpenFasting/Fasten/Luncheon/Interval Fasting Tracker" as OSS projects: no matching repos found (unverified / likely confusions).

- **FastTrack** (MIT; https://github.com/Darkrock-Studios/FastTrack, https://f-droid.org/en/packages/com.darkrockstudios.apps.fasttrack/): timer + body stages, calendar log, session notifications, widget (`FastingWidgetReceiver`), dark mode; privacy: "doesn't even have the INTERNET permission… operates entirely locally" (README). Export: none documented (unknown). No activity-alias icon.
- **FastTimes** (GPL-3.0; https://github.com/tom-murphy-development/FastTimes, https://f-droid.org/en/packages/com.tmdev.fasttimes/): plans 16:8/20:4/OMAD/custom, fg-service timer, local start/end alarms, editable history/streaks/stats, Material You, "100% Offline… Room & DataStore"; "Export and Import your history" but **format unspecified** (README). Widget: none found. No activity-alias icon.
- **MyFastingApp** (GPL-3.0; https://github.com/rohanwinsor/MyFastingApp, https://f-droid.org/en/packages/org.myfastingapp.app/): plans 13:11/16:8/18:6/20:4/OMAD/24h/custom, editable sessions, streaks/averages, phase labels, weight log + target + projection, trends, milestone notifications (25–100%), reboot-resilient, home-screen widget w/ idle+active states; "no network permission… local Room + DataStore"; **JSON backup import/export + CSV session export** via SAF (README + F-Droid page). No activity-alias icon.
- **Solo Forge** (GPL-3.0; https://github.com/kirilan/SoloForge, https://f-droid.org/en/packages/com.kbul.spicycrab/): fasting 16:8/18:6/20:4/36h + state reminders, calorie/macro manual, weight charts, simple/interval/exercise-rest workout timers, dashboard/calendar/journal, optional Health Connect sync for weight/workouts; "No backend, no accounts, no analytics" (only opt-in OpenRouter food-photo network); **append-only CSV export + full-file backup/restore** (README/F-Droid). Only activity-alias is Health Connect rationale, not icon.
- **ONE** (MIT; https://github.com/charliesbot/one, not on F-Droid): plans + countdown + weekly progress + widgets/complications + Wear OS + local-first; export none documented. No activity-alias icon.
- **interfast/Husseinfo** (GPL-3.0; https://github.com/Husseinfo/interfast, IzzyOnDroid only): eating-period calculator only; no reminders/widget/history/export documented.

## Score table vs brief 1-9

| App | 1 gym | 2 cardio | 3 week plan | 4 bodyweight | 5 HC import | 6 dense UI+widget | 7 offline/export/free | 8 stretch | 9 window+icon |
|---|---|---|---|---|---|---|---|---|---|
| Zero | N | N | N | partial (weight via Health sync) | N (Health+Fit only, no HC) | unknown | N (account, sub, no export found) | N | partial (timer/remind/history Y; icon N) |
| Fastic | N | N (steps only) | N | partial (weight/steps/water) | N (Health+Fit only) | unknown | N (account, sub, no export) | N | partial (timer/remind/history Y; icon N) |
| Simple | N | N (activity log only) | N | partial (weight graph/streaks) | N (Health/Fit/Fitbit only) | partial (iOS widget Y; dense unknown) | N (account, Premium, no export) | N (workouts are fitness, not mobility — unverified) | partial (schedule Y; icon N) |
| Window | N | N | N | partial (weight journal+graph) | N (Health only; Google undoc.) | unknown | N (account, Premium, no export) | N | partial (custom window + open/close reminders Y; icon N) |
| BodyFast | N | N ("compatible w/ workouts" marketing only) | N | partial (weight/measurements) | N (none documented) | unknown | N (account, Coach sub, no export) | N | partial (timer/editor/remind/stats Y; icon N) |
| DoFasting | N | N (steps only) | N | partial (weight tracking) | N (Health+Fit only) | unknown | N (sub-only, no export) | N | partial (schedules/reminders Y; icon N) |
| Leap Fasting Tracker | N (same vendor has separate gym apps, not bundled) | N | N | partial (weight trends) | N (Health+Fit only) | unknown | N (ads+IAP, no export) | N | partial (1-tap+custom/remind/graphs Y; icon N) |
| FastTrack (FOSS) | N | N | N | partial (weight log) | N | partial (widget Y; dense unknown) | partial (offline Y, free Y, GitHub Y; export unknown) | N | partial (timer/stages/log Y; icon N) |
| FastTimes (FOSS) | N | N | N | N | N | unknown (no widget found) | partial (offline Y, free Y, GitHub Y; export format unspecified) | N | partial (timer/alarms/history Y; icon N) |
| MyFastingApp (FOSS) | N | N | N | Y (log+target+projection) | N | partial (widget Y; dense unknown) | Y-minus (offline Y, JSON+CSV Y, free Y, GitHub Y; self-update unverified) | N | partial (plans/edit/trends/remind Y; icon N) |
| Solo Forge (FOSS) | partial (workout timers, not full logger) | partial (interval/exercise-rest timers; no per-round HR/watts) | N (calendar is journal, not drag-move plan) | Y (weight history+charts) | partial (HC sync weight/workouts; session-merge unknown) | unknown | partial (mostly-offline Y, CSV+backup Y, free Y, GitHub Y) | N | partial (fasting timer+reminders Y; icon N) |
| MyFitnessPal Premium (bundler) | N (food+fitness log, no superset/e1RM gym) | partial (workout log + Health Connect workouts/HR) | N | Y (weight) | partial (Health Connect sync steps/weight/workouts; merge unknown) | unknown | N (account+Premium $79.99–99.99/yr, no JSON; reports/PDF only — https://www.myfitnesspal.com/premium?legacy=true) | N | partial (fasting tracker Y; icon N) |
| Cronometer Gold (bundler) | N (nutrition+exercise diary, no gym programs) | partial (exercise diary + Health Connect workouts/HR/sleep) | N | Y (weight + custom biometrics) | partial-Y (Health Connect sync incl. workouts/HR — https://play.google.com/store/apps/details?hl=en_US&id=com.cronometer.android.gold) | partial (diary/dashboard widgets; dense unknown) | partial (free tier Y; Gold sub; trends/reports/PDF; JSON/CSV unverified) | N | partial (schedule/track/edit/history + charts overlay Y; icon N) |
| OpenNutriTracker (FOSS bundler) | N | partial (activities diary) | N | Y (weight history) | unknown | unknown | Y-minus (FOSS, JSON+CSV+QR per README; offline/self-update unverified) | N | partial (fasting timer chip+notification Y; icon N) |

## 3 closest matches and exact gaps

1. **Solo Forge (FOSS, F-Droid)** — https://f-droid.org/en/packages/com.kbul.spicycrab/ — only app here bundling fasting + workouts + weight + Health Connect + CSV in one offline FOSS package. Gaps: 1 no supersets/programs/prefill/one-tap/swap/e1RM/muscle charts; 2 interval timers but no lock-screen voice countdown or per-round HR/watts store, no pace/laps swim-run log; 3 no drag-move week template; 5 HC sync exists but session-merge of band+watch unverified; 6 widget none, dense-UI unverified; 8 no mobility routines; 9 icon-switch missing (only notifications).
2. **MyFastingApp (FOSS, F-Droid)** — https://f-droid.org/en/packages/org.myfastingapp.app/ — only FOSS fasting app with JSON backup + CSV sessions + widget + fully offline + phase labels. Gaps: 1–3 entirely missing; 5 no Health Connect; 6 widget yes but dense-UI unverified and no today+Start widget semantics; 7 self-update from GitHub releases unverified; 8 none; 9 timer/reminders/history yes but icon-switch missing.
3. **Cronometer Gold (proprietary bundler)** — https://cronometer.com/blog/fasting/, https://cronometer.com/gold/ — only subscription app combining fasting scheduler + food diary + exercise diary + Health Connect (workouts/HR/sleep) + weight/biometrics + chart overlays. Gaps: 1 no gym programs/supersets/e1RM; 2 no interval runner with voice/lock-screen/per-round watts; 3 no drag-move week calendar; 6 dense-UI unverified; 7 account-locked, paid Gold, JSON/CSV export unverified (PDF/reports only confirmed); 8 no stretching video routines; 9 schedule/track/edit/history yes but icon-switch missing.

*Nothing surveyed covers brief item 9's icon requirement; treat any "X app changes its icon" claim as unverified until an APK manifest (`activity-alias`) or iOS `Info.plist` (`CFBundleAlternateIcons`) dump proves it.*
