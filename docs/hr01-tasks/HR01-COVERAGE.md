# HR01 coverage

## A. `sessions`

| # | Entry | Kind | Data | What it does | READ/GUESS |
|---|---|---|---|---|---|
| A1 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Entities.kt:77 Session | read | sessions fields | Defines session dates, finish state, cardio metrics, and import source; no max heart rate field exists today. | READ |
| A2 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:203 sessions | read | sessions | Streams sessions by date and start time. | READ |
| A3 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:204 sessionsOnce | read | sessions | Reads every session for one-time calculations and exports. | READ |
| A4 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:205 session | read | sessions.id | Reads one session by ID. | READ |
| A5 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:206 sessionFlow | read | sessions.id | Streams one session by ID. | READ |
| A6 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:207 unfinishedSession | read | sessions.finishedAt | Selects the newest session whose finish time is null. | READ |
| A7 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:208 unfinishedSessionOnce | read | sessions.finishedAt | Reads the newest unfinished session once. | READ |
| A8 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:209 importedSourceIds | read | sessions.sourceId | Reads non-null imported record IDs. | READ |
| A9 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:210 upsert | write | sessions | Inserts or replaces one session. | READ |
| A10 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:211 upsertSessions | write | sessions | Inserts or replaces backup sessions. | READ |
| A11 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:212 deleteSession | delete | sessions.id | Deletes one session row. | READ |
| A12 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:215 setSessionDate | write | sessions.date | Changes one session date. | READ |
| A13 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:216 setSessionEffort | write | sessions.effort | Changes one session effort value. | READ |
| A14 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:217 setSessionNote | write | sessions.note | Changes one session note. | READ |
| A15 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:218 setSessionTime | write | sessions.timeSec | Saves timer duration without replacing other session fields. | READ |
| A16 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:219 setSessionFinishedAt | write | sessions.finishedAt | Sets or clears the finish timestamp. | READ |
| A17 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:223 finishSession | write | sessions.finishedAt, set_logs.status | Skips pending sets and records the finish timestamp in one transaction. | READ |
| A18 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:232 saveCardioEdits | write | sessions, interval_logs | Merges user edits with stored cardio values and finishes an open cardio entry. | READ |
| A19 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:244 insertSessionWithLogs | write | sessions, set_logs, interval_logs | Inserts a session and its logs in one transaction. | READ |
| A20 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:252 startProgramSession | write | sessions, set_logs | Reads a program snapshot and inserts a new gym session with set rows. | READ |
| A21 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:259 previousLogsByKey | read | sessions.finishedAt, set_logs | Uses finished gym sessions and completed sets to prefill a new session. | READ |
| A22 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:271 insertImportedSession | read/write | sessions.sourceId, sessions, interval_logs | Rejects an already imported source ID, then inserts the imported session and intervals. | READ |
| A23 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:318 deleteSessionWithLogs | delete | sessions, set_logs, interval_logs | Deletes one session and both log types in one transaction. | READ |
| A24 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:23 cardioSessionCountFor | read | sessions.exerciseId | Counts cardio sessions that use an exercise. | READ |
| A25 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:28 moveCardioSessions | write | sessions.exerciseId | Moves cardio sessions to a merged exercise ID. | READ |
| A26 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:527 clearSessions | delete | sessions | Deletes all sessions during backup replacement or reset. | READ |
| A27 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:554 importBackup | read/write/delete | sessions | Replaces or merges backup sessions through `upsertSessions`; replace clears first. | READ |
| A28 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:572 backupSnapshot | read | sessions | Includes all sessions in the backup snapshot. | READ |
| A29 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:137 sessions | read | sessions | Exposes the session flow to screens and widgets. | READ |
| A30 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:141 unfinishedSession | read | sessions.finishedAt | Exposes the unfinished session flow. | READ |
| A31 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:142 unfinishedSessionOnce | read | sessions.finishedAt | Reads the current unfinished session for widget start handling. | READ |
| A32 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:143 sessionsOnce | read | sessions | Exposes one-time session reads. | READ |
| A33 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:196 session | read | sessions.id | Exposes a session flow by ID. | READ |
| A34 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:267 startProgramSession | write | sessions.startedAt, sessions.date | Creates a gym session and saves its program origin. | READ |
| A35 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:277 startCardioSession | write | sessions.startedAt, sessions.date, interval_logs | Creates an open cardio session and empty interval rows when needed. | READ |
| A36 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:293 saveCardioEdits | write | sessions, interval_logs | Saves cardio form edits through the DAO merge. | READ |
| A37 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:296 setSessionDate | write | sessions.date | Saves a re-dated session. | READ |
| A38 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:297 setSessionEffort | write | sessions.effort | Saves session effort. | READ |
| A39 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:298 setSessionNote | write | sessions.note | Saves a session note. | READ |
| A40 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:299 setSessionTime | write | sessions.timeSec | Saves timer duration. | READ |
| A41 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:304 finishSession | write | sessions.finishedAt, set_logs.status | Finishes a gym session and removes its saved origin. | READ |
| A42 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:311 deleteSession | delete | sessions, set_logs, interval_logs | Deletes a session and its logs, then removes its saved origin. | READ |
| A43 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:335 importedSourceIds | read | sessions.sourceId | Splits stored source ID lists for import filtering. | READ |
| A44 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:341 importDraft | write | sessions, interval_logs, sessions.sourceId | Builds and inserts a finished cardio session from a Health Connect draft. | READ |
| A45 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:356 exportJson | backup | sessions | Encodes the DAO backup snapshot as JSON. | READ |
| A46 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:370 importJson | backup | sessions | Decodes JSON and imports or replaces database rows. | READ |
| A47 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:382 resetToSeed | delete | sessions | Clears session origins and resets all stored tables. | READ |
| A48 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/AppViewModel.kt:73 startItem | write | sessions | Routes plan starts to gym or cardio session creation. | READ |
| A49 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayScreen.kt:66 start | write | sessions | Starts today's planned item and opens its session screen or timer. | READ |
| A50 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayScreen.kt:67 resume | read | sessions.finishedAt | Resumes the active gym, cardio, or timer session. | READ |
| A51 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayScreen.kt:74 LaunchedEffect | read | sessions.finishedAt, sessions.date | Reads stored active and today's finished sessions before widget start. | READ |
| A52 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayScreen.kt:100 LaunchedEffect | read | Health Connect sessions, sessions.sourceId | Reads the pending Health Connect import count using imported IDs. | READ |
| A53 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayScreen.kt:105 doneToday | read | sessions.date, sessions.finishedAt | Selects today's newest finished session for the Today card. | READ |
| A54 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayScreen.kt:143 HomeRow | ui | pending Health Connect count | Shows a pending import row that opens Import. | READ |
| A55 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayScreen.kt:155 ConfirmSheet | delete | sessions.id | Discards the active session from Today. | READ |
| A56 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/SessionScreen.kt:105 LaunchedEffect | read | sessions, set_logs | Loads previous sets for a session. | READ |
| A57 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/SessionScreen.kt:106 session screen | read | sessions.finishedAt | Chooses live or edit state from the finish value. | READ |
| A58 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/SessionScreen.kt:122 toggleSet | write | set_logs.status | Toggles a completed set back to pending or checks a pending set. | READ |
| A59 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/SessionScreen.kt:149 SetRow callbacks | write | set_logs.weightKg, set_logs.reps | Saves typed weight and reps. | READ |
| A60 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/SessionScreen.kt:161 SessionDetails | write | sessions.date, effort, note | Saves date, effort, and note edits from the gym session screen. | READ |
| A61 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/SessionScreen.kt:176 SwapSheet | write | set_logs.exerciseId, exerciseName, stepKg | Swaps the exercise in the session and may update its program. | READ |
| A62 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/SessionScreen.kt:189 ConfirmSheet | delete | sessions, set_logs | Deletes or discards the session and its logs. | READ |
| A63 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/SessionScreen.kt:207 finish button | write | sessions.finishedAt, set_logs.status | Saves finish-time gym summary and marks pending sets skipped. | READ |
| A64 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CardioScreen.kt:34 CardioScreen | read | sessions.timeSec, avgHr, distanceM | Adds stored metrics to the editable cardio fields. | READ |
| A65 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CardioScreen.kt:56 CardioScreen | read | sessions.finishedAt, sessions.timeSec | Applies required-value checks only to unfinished cardio sessions. | READ |
| A66 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CardioScreen.kt:65 save callback | write | sessions, interval_logs | Saves or finishes cardio edits; finished sessions are edited from History. | READ |
| A67 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CardioScreen.kt:74 DateRow | write | sessions.date | Stores a changed date in the form draft for Save. | READ |
| A68 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CardioScreen.kt:78 NumberRow | ui | sessions.timeSec | Stores edited time in the form draft for Save. | READ |
| A69 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CardioScreen.kt:80 NumberRow | ui | sessions.avgHr | Stores edited average heart rate in the form draft for Save. | READ |
| A70 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CardioScreen.kt:119 Delete button | delete | sessions.id | Opens delete or discard confirmation based on finish state. | READ |
| A71 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CardioScreen.kt:128 ConfirmSheet | delete | sessions, interval_logs | Deletes or discards a cardio session and its logs. | READ |
| A72 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryScreen.kt:83 rememberHistoryData | read | sessions, set_logs | Collects sessions and sets for History and Progress. | READ |
| A73 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryScreen.kt:98 muscleVolume | read | sessions.finishedAt, set_logs | Counts completed sets from finished sessions by week. | READ |
| A74 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryScreen.kt:130 openSession | read | sessions.finishedAt | Routes unfinished sessions to their timer or form. | READ |
| A75 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryRules.kt:35 historyData | read | sessions.date, set_logs.sessionId | Filters future sessions and sets from History. | READ |
| A76 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryRules.kt:97 logRows | read | sessions.date, startedAt, finishedAt | Builds day rows in session start order and marks open rows. | READ |
| A77 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryRules.kt:113 weekSummary | read | sessions.finishedAt, timeSec | Totals finished sessions, sets, cardio seconds, and distance. | READ |
| A78 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryRules.kt:159 weekBar | read | sessions.finishedAt, date | Chooses the first finished session for each day bar. | READ |
| A79 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryRules.kt:213 sessionLine | read | sessions.finishedAt, timeSec, sourceId | Builds the History session summary and imported-source label. | READ |
| A80 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryRules.kt:251 liftHistory | read | sessions.finishedAt, set_logs | Builds exercise history from finished sessions and their sets. | READ |
| A81 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryRules.kt:338 cardioTypes | read | sessions.finishedAt, timeSec | Groups finished cardio sessions and totals duration and distance. | READ |
| A82 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/ExerciseDetail.kt:132 CardioTypeDetail | read | sessions.finishedAt, timeSec | Selects finished sessions for a cardio type and charts time. | READ |
| A83 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/ReviewScreen.kt:32 ReviewScreen | read | sessions, set_logs | Reads one session and its set rows for the finish review. | READ |
| A84 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/widget/TodayWidget.kt:47 provideGlance | read | sessions.finishedAt | Reads the active session for the home-screen widget. | READ |
| A85 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/widget/TodayWidget.kt:58 provideGlance | ui | sessions | Shows the active session title and resume state. | READ |
| A86 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/health/HealthImport.kt:104 drafts | read | Health Connect ExerciseSessionRecord, HeartRateRecord, DistanceRecord | Reads workouts, heart samples, and distance for import drafts. | READ |
| A87 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/health/HealthImport.kt:105 drafts | read | Health Connect session ID, app package | Excludes known IDs and, outside debug, this app's own records. | READ |
| A88 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/health/HealthImport.kt:121 mergeOverlaps | read | Health Connect session time range | Merges overlapping imported drafts by matching modality. | READ |
| A89 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/health/HealthImport.kt:141 pendingCount | read | Health Connect ExerciseSessionRecord, sessions.sourceId | Counts unimported non-strength workouts for Today. | READ |
| A90 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:46 mergeCardioEdits | read/write | sessions.date, timeSec, finishedAt | Preserves concurrent values and sets finish time when saving an open cardio session. | READ |
| A91 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:232 saveCardioEdits | read | sessions.finishedAt | Applies the required cardio fields only while the stored session is unfinished. | READ |
| A92 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:310 swapExercise | read | sessions.finishedAt, startedAt, programId | Allows a program update only for the matching unfinished session origin. | READ |
| A93 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayRules.kt:166 runningLine | read | sessions.timeSec | Shows saved time when the timer stopped. | READ |
| A94 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayRules.kt:179 paceLine | read | sessions.timeSec | Requires positive time and distance before showing pace. | READ |
| A95 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayRules.kt:208 doneToday | read | sessions.date, finishedAt | Selects the latest finished session for the date. | READ |
| A96 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayRules.kt:220 widgetStart | read | sessions.finishedAt, date | Resumes an active session, or stays when a session is already saved today. | READ |
| A97 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/SessionOrigins.kt:117 cardioMissing | read | sessions.timeSec, distanceM, laps | Requires positive time, distance, or laps before new cardio save. | READ |

| A98 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CardioRules.kt:11 timerStopped | read | sessions.finishedAt, timeSec | Treats a cardio session as stopped when it is unfinished and has stored time. | READ |
| A99 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:410 stillOpen | read | sessions.finishedAt | Restores cardio timer only while its session remains unfinished. | READ |
| A100 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryRules.kt:252 liftHistory | read | sessions.finishedAt | Uses finished sessions to form lift histories and records. | READ |
| A101 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryRules.kt:296 liftEntries | read | sessions.finishedAt | Includes saved sets from finished sessions whose exercise is missing. | READ |

| A102 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryRules.kt:180 pace | read | sessions.timeSec | Requires positive time and distance before it builds a pace string. | READ |
| A103 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CardioScreen.kt:87 CardioScreen | read | sessions.timeSec, distanceM | Shows pace only when time and distance are positive. | READ |
| A104 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/health/HealthImport.kt:121 mergeOverlaps | read | Health Connect drafts | Merges overlapping source drafts only; it does not read app sessions. | READ |

| A105 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:147 deleteProgramFully | read | sessions.date | Reads session dates when it freezes past plan dates before deleting a program. | READ |
| A106 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:350 saveWeekPlan | read | sessions.date | Reads session dates when it freezes past plan dates before changing the week plan. | READ |

## B. `set_logs`

| # | Entry | Kind | Data | What it does | READ/GUESS |
|---|---|---|---|---|---|
| B1 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Entities.kt:102 SetLog | read | set_logs fields | Defines saved set values and status; no check-time column exists today. | READ |
| B2 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:278 setLogs | read | set_logs.sessionId | Streams ordered sets for one session. | READ |
| B3 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:279 setLogsOnce | read | set_logs.sessionId | Reads ordered sets once. | READ |
| B4 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:280 allSetLogs | read | set_logs | Streams all set rows. | READ |
| B5 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:281 allSetLogsOnce | read | set_logs | Reads all set rows once for backup. | READ |
| B6 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:282 upsert | write | set_logs | Inserts or replaces one set row. | READ |
| B7 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:283 upsertSetLogs | write | set_logs | Inserts or replaces a list of set rows. | READ |
| B8 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:286 setSetStatus | write | set_logs.status | Sets a set status by ID. | READ |
| B9 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:287 setSetWeight | write | set_logs.weightKg | Saves typed set weight by ID. | READ |
| B10 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:288 setSetReps | write | set_logs.reps | Saves typed reps by ID. | READ |
| B11 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:294 checkSet | write | set_logs.status, reps, weightKg, stepKg | Marks valid stored reps and weight as done. | READ |
| B12 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:295 swapSetExercise | write | set_logs.exerciseId, exerciseName, stepKg | Changes exercise snapshot fields for all sets in one slot. | READ |
| B13 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:220 skipPendingSets | write | set_logs.status | Marks all pending sets skipped at finish. | READ |
| B14 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:319 deleteSetLogsFor | delete | set_logs.sessionId | Deletes all sets for a session. | READ |
| B15 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:528 clearSetLogs | delete | set_logs | Clears all set rows for replace or reset. | READ |
| B16 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:560 importBackup | backup | set_logs | Imports backup set rows after any replace clear. | READ |
| B17 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:572 backupSnapshot | backup | set_logs | Reads every set row into a backup. | READ |
| B18 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:104 programLogs | write | set_logs | Rebuilds new set rows and copies prior weight, reps, and status when keys match. | READ |
| B19 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:195 setLogs | read | set_logs.sessionId | Exposes ordered rows for one session. | READ |
| B20 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:144 allSetLogs | read | set_logs | Exposes all set rows to screens. | READ |
| B21 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:275 previousLogsByKey | read | set_logs, sessions.finishedAt | Reads prior completed sets for prefill. | READ |
| B22 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:284 setSetStatus | write | set_logs.status | Writes a set status change. | READ |
| B23 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:286 checkSet | write | set_logs.status | Returns whether the guarded set check changed a row. | READ |
| B24 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:287 setSetWeight | write | set_logs.weightKg | Saves a set's weight. | READ |
| B25 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:288 setSetReps | write | set_logs.reps | Saves a set's reps. | READ |
| B26 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:326 swapExerciseInSession | write | set_logs exercise snapshot | Requests a slot swap under the session-origin lock. | READ |
| B27 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/SessionScreen.kt:122 toggleSet | ui | set_logs.status | Set-row tap unchecks done sets or calls guarded check. | READ |
| B28 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/SessionScreen.kt:145 SetRow | ui | set_logs | Renders set fields and attaches check, weight, reps, and swap callbacks. | READ |
| B29 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/SessionScreen.kt:149 SetRow callbacks | write | set_logs.weightKg, reps | Saves edited weight and reps from the session screen. | READ |
| B30 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/SessionScreen.kt:176 SwapSheet | ui | set_logs.exerciseId | Lets the user swap a set's exercise, with optional program update. | READ |
| B31 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/SessionScreen.kt:189 delete confirmation | delete | set_logs.sessionId | Deletes session sets with the session. | READ |
| B32 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:252 startProgramSession | write | set_logs | Calls `programLogs` to build a session's rows from program slots and prior values. | READ |
| B33 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:223 finishSession | write | set_logs.status | Finishing skips every still-pending set. | READ |
| B34 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:370 importJson | backup | set_logs | Decodes and imports backup rows without rebuilding them. | READ |
| B35 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryRules.kt:191 setsLine | read | set_logs.status | Builds the done-of-total set summary. | READ |
| B36 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryRules.kt:303 setText | read | set_logs.weightKg, reps | Builds a full set display string. | READ |
| B37 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryRules.kt:306 shortSet | read | set_logs.weightKg, reps, status | Builds a compact set display string. | READ |
| B38 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/ReviewScreen.kt:72 ReviewScreen | read | set_logs | Displays each set and its status in the finish review. | READ |
| B39 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/ExerciseDetail.kt:70 liftHistory | read | set_logs, sessions | Shows finished sets and records for an exercise. | READ |

| B40 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/SessionScreen.kt:106 session screen | ui | set_logs, sessions.finishedAt | History edit mode makes finished sets editable through the same set rows. | READ |
| B41 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:105 programLogs | write | set_logs future columns | Rebuilds each set row; it copies prior weight, reps, and status only. | GUESS |

## C. `interval_logs`

| # | Entry | Kind | Data | What it does | READ/GUESS |
|---|---|---|---|---|---|
| C1 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Entities.kt:122 IntervalLog | read | interval_logs fields | Defines each interval row's heart rate and watts; no start or end time columns exist today. | READ |
| C2 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:277 startCardioSession | write | interval_logs | Creates one empty row per configured round for interval cardio. | READ |
| C3 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:321 intervalLogs | read | interval_logs.sessionId | Streams ordered interval rows for a session. | READ |
| C4 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:322 intervalLogsOnce | read | interval_logs | Reads every interval row once. | READ |
| C5 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:323 intervalLogsFor | read | interval_logs.sessionId | Reads ordered intervals for one session. | READ |
| C6 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:324 allIntervalLogs | read | interval_logs | Streams all interval rows. | READ |
| C7 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:325 upsertIntervalLogs | write | interval_logs | Inserts or replaces a list of interval rows. | READ |
| C8 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:326 setIntervalAvgHr | write | interval_logs.avgHr | Changes one round's average heart rate by ID. | READ |
| C9 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:327 setIntervalWatts | write | interval_logs.watts | Changes one round's watts by ID. | READ |
| C10 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:328 deleteIntervalLogsFor | delete | interval_logs.sessionId | Deletes all intervals for one session. | READ |
| C11 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:529 clearIntervalLogs | delete | interval_logs | Clears intervals for replace or reset. | READ |
| C12 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:560 importBackup | backup | interval_logs | Imports backup interval rows after any replace clear. | READ |
| C13 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:574 backupSnapshot | backup | interval_logs | Reads every interval row into a backup. | READ |
| C14 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:147 allIntervalLogs | read | interval_logs | Exposes all intervals to app state. | READ |
| C15 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:197 intervalLogs | read | interval_logs.sessionId | Exposes one session's interval flow. | READ |
| C16 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:232 saveCardioEdits | write | interval_logs.avgHr, watts | Writes only interval fields the user touched, by row ID. | READ |
| C17 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CardioScreen.kt:98 CardioScreen | ui | interval_logs.avgHr, watts | Shows per-round heart-rate and watts inputs. | READ |
| C18 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CardioScreen.kt:102 edit | write | interval_logs.avgHr, watts | Tracks edits by interval ID before Save. | READ |
| C19 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:279 startCardioSession | write | interval_logs | Creates round rows with generated IDs and indexes. | READ |
| C20 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:351 importDraft | write | interval_logs.avgHr | Builds imported interval rows from Health Connect samples for configured work rounds. | READ |
| C21 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/health/HealthImport.kt:150 intervalHeartRates | read | ImportDraft.hrSamples, interval_logs.avgHr | Averages samples inside each derived work-round range. | READ |
| C22 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CardioScreen.kt:101 intervals.forEachIndexed | read | interval_logs | Reads rounds from the session's interval flow for editing. | READ |
| C23 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:318 deleteSessionWithLogs | delete | interval_logs.sessionId | Deletes intervals with a deleted session. | READ |
| C24 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:370 importJson | backup | interval_logs | Decodes and imports backup interval rows without rebuilding them. | READ |

| C25 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:244 insertSessionWithLogs | write | interval_logs | Writes interval rows with the session in one transaction. | READ |
| C26 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:232 saveCardioEdits | write | interval_logs | Updates touched interval columns by ID; it does not rebuild interval rows. | READ |

## D. Timer clock and phase path

| # | Entry | Kind | Data | What it does | READ/GUESS |
|---|---|---|---|---|---|
| D1 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerState.kt:15 TimerState | timer | TimerState.index, remaining, elapsed, startedAt | Holds phase position, countdown, elapsed seconds, and start wall time. | READ |
| D2 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerState.kt:43 begin | timer | TimerState.awaitingStart, preparation | Begins the first phase after three preparation seconds. | READ |
| D3 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerState.kt:46 skip | timer | TimerState.index, skipped, elapsed | Skips the current phase and advances without adding skipped time. | READ |
| D4 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerState.kt:55 back | timer | TimerState.index, completed, skipped | Stretch-only back resets the prior phase and awaits Start. | READ |
| D5 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerState.kt:64 advance | timer | TimerState.index, remaining, completed | Advances when a phase ends and loads the next duration. | READ |
| D6 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerState.kt:75 tick | timer | TimerState.remaining, elapsed | Consumes service-provided seconds across phases; stretch transitions discard overshoot. | READ |
| D7 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerState.kt:98 buildPhases | timer | Phase.kind, round, seconds | Builds warm-up, work and rest phases; each work phase starts a round. | READ |
| D8 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:87 onStartCommand | timer | TimerState.startedAt, phases | Creates cardio or stretch state and starts the foreground service. | READ |
| D9 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:88 onStartCommand | timer | System.nanoTime | Sets the monotonic tick baseline for the service loop. | READ |
| D10 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:93 ACTION_START | timer | System.currentTimeMillis, TimerState.startedAt | Sets wall-clock timer start for cardio; the workout session already exists. | READ |
| D11 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:103 ACTION_START_STRETCH | timer | System.currentTimeMillis, TimerState.startedAt | Sets wall-clock start for a stretch timer. | READ |
| D12 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:110 ACTION_BEGIN | timer | TimerState.preparation | Starts preparation; onStartCommand resets the monotonic tick baseline. | READ |
| D13 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:115 ACTION_SKIP | timer | TimerState.index, skipped | Skips the phase; onStartCommand resets the monotonic tick baseline. | READ |
| D14 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:124 ACTION_BACK | timer | TimerState.index | Returns to the prior stretch phase; onStartCommand resets the monotonic tick baseline. | READ |
| D15 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:129 ACTION_PAUSE | timer | TimerState.paused | Pauses state; onStartCommand resets the monotonic tick baseline. | READ |
| D16 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:134 ACTION_RESUME | timer | TimerState.paused | Resumes state; onStartCommand resets the monotonic tick baseline. | READ |
| D17 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:139 ACTION_STOP | timer | TimerState.elapsed | Stops early; saved duration uses elapsed monotonic ticks, not wall time. | READ |
| D18 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:154 continueExisting | timer | System.currentTimeMillis, TimerSnapshot | Restores a matching snapshot using the wall-clock gap. | READ |
| D19 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:167 restore | timer | System.currentTimeMillis, TimerSnapshot | Checks session state and snapshot age, then restores or records an ended timer. | READ |
| D20 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:210 runLoop | timer | System.nanoTime, TimerState.elapsed | Ticks from monotonic elapsed nanoseconds every 50 milliseconds. | READ |
| D21 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:270 finish | timer | TimerState.elapsed | Marks timer done, saves a snapshot, then records the result. | READ |
| D22 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:393 recordTimer | write | sessions.timeSec | Writes cardio duration to Room; the service does not write interval rows. | READ |
| D23 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerSnapshot.kt:10 TimerSnapshot | timer | savedAt, TimerState | Saves transition state with a wall-clock timestamp. | READ |
| D24 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerSnapshot.kt:14 advanced | timer | System.currentTimeMillis gap | Advances the state by non-negative wall-clock time after process death. | READ |
| D25 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerSnapshot.kt:16 expired | timer | savedAt, remaining | Rejects snapshots more than one hour past expected completion. | READ |
| D26 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerSnapshot.kt:26 TimerSnapshots | config | SharedPreferences `timer_snapshot`, key `snapshot` | Saves and clears one timer snapshot outside Room. | READ |
| D27 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TimerScreen.kt:63 LaunchedEffect | timer | TimerState.sessionId | Starts the timer service for the selected cardio session. | READ |
| D28 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TimerScreen.kt:71 toForm | timer | TimerScreen route | Leaves the timer for the cardio form after finish or stop. | READ |
| D29 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TimerScreen.kt:94 Stop button | timer | TimerService.ACTION_STOP | Stops the timer early. | READ |
| D30 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TimerScreen.kt:121 PrimaryButton | timer | TimerService.ACTION_BEGIN, PAUSE, RESUME | Starts, pauses, or resumes the service timer. | READ |
| D31 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:57 noisyReceiver | timer | TimerState.paused | Pauses when audio output becomes noisy. | READ |
| D32 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:74 onCreate | timer | TimerSnapshot | Saves only state transitions, not every timer tick. | READ |
| D33 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:385 restore | timer | TimerSnapshots | Starts restore when the app opens and a snapshot exists. | READ |

| D34 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerState.kt:85 tick | timer | PhaseKind.WORK, TimerState.remaining | A round starts when its work phase becomes current and ends at zero or skip; no round timestamp is saved. | READ |
| D35 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:270 finish | write | sessions.timeSec, interval_logs | The service writes duration only; timer phase changes do not write interval rows. | READ |
| D36 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:347 notification | timer | TimerState phase actions | Notification controls begin, pause, resume, stop, and stretch skip; no direct clock call. | READ |

| D37 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerState.kt:85 tick | timer | TimerState phase boundary | `tick` uses supplied monotonic seconds; `advance` itself does not read a clock. | READ |

| D38 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerService.kt:344 notification | timer | System.currentTimeMillis | Uses wall time to set the notification countdown end time. | READ |

## E. Health Connect

| # | Entry | Kind | Data | What it does | READ/GUESS |
|---|---|---|---|---|---|
| E1 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/health/HealthImport.kt:46 readPermissions | config | ExerciseSessionRecord, HeartRateRecord, DistanceRecord | Requests Health Connect read access to workouts, heart rate, and distance. | READ |
| E2 | android/app/src/main/AndroidManifest.xml:16 uses-permission | config | READ_EXERCISE, READ_HEART_RATE, READ_DISTANCE | Declares Health Connect read permissions. | READ |
| E3 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/health/HealthImport.kt:70 hasPermissions | read | Health Connect permissions | Checks every declared read permission. | READ |
| E4 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/health/HealthImport.kt:101 drafts | read | ExerciseSessionRecord | Reads Health Connect workouts in the selected time window. | READ |
| E5 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/health/HealthImport.kt:106 drafts | read | DistanceRecord.DISTANCE_TOTAL | Aggregates distance for each workout's time range. | READ |
| E6 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/health/HealthImport.kt:108 drafts | read | HeartRateRecord.samples | Reads timestamped heart-rate samples inside each workout range. | READ |
| E7 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/health/HealthImport.kt:121 mergeOverlaps | read | imported workout ranges, modalityId | Merges overlapping source drafts of matching modality. | READ |
| E8 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/health/HealthImport.kt:140 pendingCount | read | ExerciseSessionRecord | Counts pending non-strength workouts for the Today row. | READ |
| E9 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/health/HealthImport.kt:146 pendingCount | read | source IDs, strength types | Excludes known IDs, app records, and strength records. | READ |
| E10 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/health/HealthImport.kt:150 intervalHeartRates | read | ImportDraft.hrSamples | Averages samples in each configured work interval. | READ |
| E11 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:341 importDraft | write | sessions, interval_logs | Writes a finished cardio session and optional interval heart-rate rows. | READ |
| E12 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/ImportScreen.kt:57 permLauncher | config | Health Connect read permissions | Requests the declared permissions when the user taps Allow. | READ |
| E13 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/ImportScreen.kt:58 LaunchedEffect | read | Health Connect records, sessions.sourceId | Checks permissions and loads drafts using known imported IDs. | READ |
| E14 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/ImportScreen.kt:67 import | write | sessions, interval_logs | Imports the selected draft under its chosen cardio exercise. | READ |
| E15 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/ImportScreen.kt:84 ImportScreen | ui | Health Connect drafts | Hides strength workouts and shows cardio drafts for import. | READ |
| E16 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/ImportScreen.kt:109 ImportScreen | ui | strength drafts | Shows a count of watch strength workouts that are not imported. | READ |
| E17 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/health/HealthImport.kt:160 writeTestRun | write | ExerciseSessionRecord, HeartRateRecord, DistanceRecord | Debug action writes one test workout, heart-rate record, and distance record. | READ |
| E18 | android/app/src/main/AndroidManifest.xml:41 intent-filter | config | Health Connect rationale action | Exposes MainActivity as the permission rationale entry. | READ |
| E19 | android/app/src/main/AndroidManifest.xml:50 activity-alias | config | Health permission rationale | Registers the Health Connect rationale alias. | READ |
| E20 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayScreen.kt:100 LaunchedEffect | read | pending Health Connect count | Reads the pending count once when Today opens. | READ |
| E21 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayScreen.kt:143 HomeRow | ui | pending Health Connect count | Shows the count and opens the import screen. | READ |
| E22 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/ImportScreen.kt:112 writeLauncher | write | Health Connect records | Launches the debug write permission request and inserts test records. | READ |

| E23 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/health/HealthImport.kt:106 drafts | read | Health Connect workout range | Reads samples for imported Health workouts, not app-session time ranges. | READ |
| E24 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/health/HealthImport.kt:113 drafts | read | HeartRateRecord.samples, avgHr | Computes average heart rate; it does not compute maximum heart rate. | READ |

## F. Backup

| # | Entry | Kind | Data | What it does | READ/GUESS |
|---|---|---|---|---|---|
| F1 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:29 Backup | backup | Backup.sessions, setLogs, intervalLogs | Defines exported session and log collections plus optional newer collections. | READ |
| F2 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:572 backupSnapshot | backup | sessions, set_logs, interval_logs | Reads all tables in one transaction for export. | READ |
| F3 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:356 exportJson | backup | Backup | Encodes the transactional snapshot as pretty JSON. | READ |
| F4 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:359 describeBackup | backup | Backup | Parses and summarizes a selected backup before import. | READ |
| F5 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:370 importJson | backup | Backup, session origins, window_since | Ignores unknown JSON keys, clears origins, imports rows, then clears the window start date. | READ |
| F6 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:554 importBackup | backup | sessions, set_logs, interval_logs | Replace clears all tables; merge upserts backup rows. | READ |
| F7 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:564 importBackup | backup | stretches | Seeds stretch data after replace when backup has none. | READ |
| F8 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:565 importBackup | backup | programs.active | Keeps or repairs one active program after import. | READ |
| F9 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:566 importBackup | backup | stretch_routines.active | Keeps or repairs at most one active routine. | READ |
| F10 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:567 importBackup | delete | routine_stretches | Deletes rows whose routine or stretch is missing. | READ |
| F11 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:568 importBackup | backup | eating_window | Ensures the settings row exists after import. | READ |
| F12 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:31 Backup | backup | Backup.version | Defaults backup version to 1. | READ |
| F13 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:36 Backup | backup | optional backup lists | Defaults stretch collections and window days to empty lists. | READ |
| F14 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:371 importJson | backup | JSON unknown fields | Configures `ignoreUnknownKeys = true` for import decoding. | READ |
| F15 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Entities.kt:77 Session | backup | Session fields | Existing nullable session fields use constructor defaults when omitted. | READ |
| F16 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Entities.kt:103 SetLog | backup | SetLog fields | Existing optional set values and status use constructor defaults when omitted. | READ |
| F17 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Entities.kt:123 IntervalLog | backup | IntervalLog fields | Existing optional interval values use constructor defaults when omitted. | READ |
| F18 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:371 importJson | backup | new HR01 fields | Old backup files should use declared defaults for any new fields with defaults; required fields would fail decoding. | GUESS |
| F19 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt:558 importBackup | backup | database rows | Replace clears first; merge upserts rows and applies active-mark, orphan, and settings repairs. | READ |

| F20 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Repository.kt:30 Backup | backup | future HR sample table | Backup has no separate heart-rate sample collection today; adding one needs an explicit backup field. | GUESS |

## G. Room schema and tests

| # | Entry | Kind | Data | What it does | READ/GUESS |
|---|---|---|---|---|---|
| G1 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Database.kt:11 KhonDatabase | migration | Room schema | Declares schema version 4 and registered entities. | READ |
| G2 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Database.kt:17 KhonDatabase | migration | migrations 1→2, 2→3 | Uses Room auto-migrations for versions 1 to 2 and 2 to 3. | READ |
| G3 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Database.kt:24 MIGRATION_3_4 | migration | day_overrides.auto | Adds the `auto` column with default 0. | READ |
| G4 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Database.kt:28 build | migration | khon.db | Builds the on-device database and registers migration 3 to 4. | READ |
| G5 | android/app/schemas/dev.mirzohidkhon.khonfitness.data.KhonDatabase/1.json:1 | migration | schema version 1 | Stores exported Room schema version 1. | READ |
| G6 | android/app/schemas/dev.mirzohidkhon.khonfitness.data.KhonDatabase/2.json:1 | migration | schema version 2 | Stores exported Room schema version 2. | READ |
| G7 | android/app/schemas/dev.mirzohidkhon.khonfitness.data.KhonDatabase/3.json:1 | migration | schema version 3 | Stores exported Room schema version 3. | READ |
| G8 | android/app/schemas/dev.mirzohidkhon.khonfitness.data.KhonDatabase/4.json:1 | migration | schema version 4 | Stores exported Room schema version 4. | READ |
| G9 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/data/SessionWritesTest.kt:31 open | test | test database | Builds a Room in-memory database with `Room.inMemoryDatabaseBuilder`. | READ |
| G10 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/data/Race.kt:129 raceDatabase | test | test database | Shared race tests use an in-memory Room builder with main-thread queries. | READ |
| G11 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/data/BackupTest.kt:27 newRepo | test | test database | Backup tests build in-memory Room databases. | READ |
| G12 | docs/tasks/ui07/UI07-title-tabs-week-symbols.md:44 test command | config | Gradle unit tests | Documents `android/gradlew -p android :app:testDebugUnitTest --rerun :app:assembleDebug :app:lintVitalRelease`. | READ |

| G13 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Database.kt:15 KhonDatabase | migration | version 4 session pairs | The current schema is version 4; no v4-to-v5 or old pair repair appears in this code. | GUESS |

## H. Session summaries shown to users

| # | Entry | Kind | Data | What it does | READ/GUESS |
|---|---|---|---|---|---|
| H1 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayRules.kt:189 statLine | ui | sessions.finishedAt, timeSec, set_logs | Builds the saved gym or cardio summary string. | READ |
| H2 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayRules.kt:200 doneLine | ui | session summary | Adds the Today `Done` prefix to the summary. | READ |
| H3 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayRules.kt:164 runningLine | ui | sessions.timeSec, set_logs, TimerState | Builds the active workout line, including stopped timer duration. | READ |
| H4 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayRules.kt:32 workoutTime | ui | sessions.startedAt, current time | Builds elapsed time for an active session. | READ |
| H5 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayScreen.kt:106 TodayCard | ui | doneLine, runningLine | Shows today's done or in-progress summary. | READ |
| H6 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/SessionScreen.kt:135 openSummary | ui | set_logs, sessions.startedAt | Builds the gym finish-sheet summary at tap time. | READ |
| H7 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/SessionRules.kt:35 summarize | ui | elapsed, done sets, volume, new bests | Builds the finish summary values and duration string. | READ |
| H8 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/SessionScreen.kt:197 finish sheet | ui | summary.time, sets, volume, bests | Shows the gym finish summary values. | READ |
| H9 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryRules.kt:213 sessionLine | ui | sessions.finishedAt, timeSec, sourceId | Builds each History log row summary and import label. | READ |
| H10 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryLog.kt:143 LogRow | ui | sessionLine | Shows the session summary in each History day row. | READ |
| H11 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryRules.kt:204 sessionMinutes | ui | timeSec | Formats saved duration in minutes. | READ |
| H12 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/ReviewScreen.kt:42 ReviewScreen | ui | session summary | Shows the summary line at the top of Review. | READ |
| H13 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/ReviewScreen.kt:80 CardioReview | ui | sessions.timeSec, avgHr, interval_logs | Builds cardio detail rows for duration, pace, average heart rate, and rounds. | READ |
| H14 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/ReviewScreen.kt:97 CardioReview | ui | interval_logs.avgHr, watts | Builds each Review round's heart-rate and watts string. | READ |
| H15 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryProgress.kt:214 CardioTypeRow | ui | CardioType totals | Shows session count, distance, time, and pace through `cardioLine`. | READ |
| H16 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryRules.kt:353 cardioLine | ui | CardioType seconds, distance, pace | Builds a cardio type's Progress summary string. | READ |
| H17 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/ExerciseDetail.kt:86 ExerciseDetailScreen | ui | set_logs, sessions.date | Shows best set, date, and per-session set rows for gym exercises. | READ |
| H18 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/ExerciseDetail.kt:98 ExerciseDetailScreen | ui | set_logs, sessions.date | Builds the gym exercise session list from sets and dates. | READ |
| H19 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/ExerciseDetail.kt:125 CardioTypeScreen | ui | sessions.timeSec, avgHr, interval_logs | Builds cardio type charts for pace, average heart rate, rounds, or time. | READ |
| H20 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/ExerciseDetail.kt:148 CardioTypeScreen | ui | sessions.avgHr | Plots average heart rate by session date. | READ |
| H21 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/ExerciseDetail.kt:151 CardioTypeScreen | ui | interval_logs.avgHr | Plots average heart rate in work intervals. | READ |
| H22 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/ExerciseDetail.kt:154 CardioTypeScreen | ui | sessions.timeSec | Plots session time when pace and heart-rate charts are absent. | READ |
| H23 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/ExerciseDetail.kt:159 CardioTypeScreen | ui | sessionLine, sessions | Builds each cardio detail session line. | READ |
| H24 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TimerScreen.kt:102 TimerScreen | ui | TimerState.phase | Shows the current phase label and round count. | READ |
| H25 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TimerScreen.kt:105 TimerScreen | ui | TimerState.remaining | Shows the countdown for the current phase. | READ |
| H26 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TimerScreen.kt:110 TimerScreen | ui | TimerState.elapsed, total | Shows timer elapsed and total duration. | READ |
| H27 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/widget/TodayWidget.kt:58 provideGlance | ui | active session title and state | Shows the active session name and resume status; it shows no time or heart rate. | READ |
| H28 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryProgress.kt:97 muscleVolume | read | sessions.finishedAt, set_logs | Uses only finished sessions for weekly set counts. | READ |

| H29 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CardioScreen.kt:78 NumberRow | ui | sessions.timeSec, avgHr | Shows editable cardio time and average heart rate in the session form. | READ |
| H30 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/CardioScreen.kt:105 NumberField | ui | interval_logs.avgHr | Shows editable heart rate for each cardio round. | READ |
| H31 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayRules.kt:85 draftStats | ui | import time, distance, avgHr | Builds the import row's duration, distance, and heart-rate summary. | READ |
| H32 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/ImportScreen.kt:122 import sheet | ui | import time, distance, avgHr | Shows duration, distance, heart rate, laps, and source before import. | READ |

## I. App start and background work

| # | Entry | Kind | Data | What it does | READ/GUESS |
|---|---|---|---|---|---|
| I1 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/KhonApp.kt:27 onCreate | config | Room seed, TodayWidget | Seeds empty data and refreshes the widget when its inputs change. | READ |
| I2 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/MainActivity.kt:11 onCreate | config | TimerService, WindowScheduler | Reschedules window alarms and restores a timer on app launch. | READ |
| I3 | android/app/src/main/AndroidManifest.xml:59 service | config | TimerService | Declares the non-exported timer foreground service. | READ |
| I4 | android/app/src/main/AndroidManifest.xml:65 receiver | config | UpdatedReceiver | Declares the package-replaced update receiver. | READ |
| I5 | android/app/src/main/AndroidManifest.xml:68 receiver | config | WindowReceiver | Declares boot, clock-change, and time-zone-change receiver. | READ |
| I6 | android/app/src/main/AndroidManifest.xml:75 receiver | config | WindowWidgetReceiver | Declares the eating-window widget receiver. | READ |
| I7 | android/app/src/main/AndroidManifest.xml:84 receiver | config | TodayWidgetReceiver | Declares the Today widget receiver. | READ |
| I8 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/window/WindowScheduler.kt:79 WindowReceiver | config | AlarmManager alarms | Handles boundary, reminder, boot, clock, and time-zone events; then reschedules alarms. | READ |
| I9 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/update/UpdatedReceiver.kt:21 UpdatedReceiver | config | package replacement | Reopens the app or posts an update notification after an update. | READ |
| I10 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/widget/TodayWidget.kt:83 TodayWidgetReceiver | config | Today widget | Receives widget updates for the Today tile. | READ |
| I11 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/widget/WindowWidget.kt:77 WindowWidgetReceiver | config | window widget | Receives widget updates for the eating-window tile. | READ |
| I12 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/KhonApp.kt:24 settings | config | DataStore file `settings` | Creates the app's Preferences DataStore. | READ |
| I13 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/SessionOrigins.kt:24 SessionOrigins | config | `session_origin_<sessionId>` | Stores per-session program origin outside Room and backup. | READ |
| I14 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/WindowSince.kt:12 WindowSince | config | `window_since` | Stores the first eating-window date outside Room and backup. | READ |
| I15 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/timer/TimerSnapshot.kt:27 TimerSnapshots | config | SharedPreferences `timer_snapshot`, key `snapshot` | Stores timer restore state outside Room. | READ |
| I16 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/window/WindowScheduler.kt:38 reschedule | timer | AlarmManager | Cancels and arms exact or idle-tolerant RTC alarms for window events. | READ |
| I17 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/window/WindowScheduler.kt:63 set | timer | AlarmManager.RTC_WAKEUP | Uses exact alarms when allowed and inexact idle-tolerant alarms otherwise. | READ |
| I18 | android/app/src/main/AndroidManifest.xml:14 uses-permission | config | exact alarm permissions | Declares exact-alarm and boot permissions for the window scheduler. | READ |
| I19 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayScreen.kt:100 pendingBand | read | Health Connect pending workouts | Runs one pending-count read from Today; no scheduled Health Connect worker is present. | READ |

| I20 | android/app/src/main/java/dev/mirzohidkhon/khonfitness/window/WindowScheduler.kt:43 arm | timer | AlarmManager | Arms RTC alarms for window boundaries and reminders. | READ |

## J. Tests for parts A to G

| # | Entry | Kind | Data | What it does | READ/GUESS |
|---|---|---|---|---|---|
| J1 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/data/SessionWritesTest.kt:26 SessionWritesTest | test | sessions, set_logs | Proves session field writes, finish, skipped sets, and concurrent edits. | READ |
| J2 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/data/SessionStartOneStateTest.kt:28 SessionStartOneStateTest | test | sessions, set_logs | Proves program snapshot and generated set rows stay from one state. | READ |
| J3 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/data/CardioEditsTest.kt:21 CardioEditsTest | test | sessions, interval_logs | Proves cardio edits preserve concurrent timer, import, and interval values. | READ |
| J4 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/data/SetCheckTest.kt:27 SetCheckTest | test | set_logs | Proves set checks require valid reps and weight, including bodyweight sets. | READ |
| J5 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/data/SwapBySlotTest.kt:56 SwapBySlotTest | test | set_logs, program rows | Proves slot swaps and program updates respect stored origins. | READ |
| J6 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/data/ConcurrentWritesTest.kt:23 ConcurrentWritesTest | test | sessions, interval_logs | Proves failed session, cardio, and import writes leave no partial rows. | READ |
| J7 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/data/DeletesKeepHistoryTest.kt:33 DeletesKeepHistoryTest | test | sessions, set_logs | Proves program, block, and exercise deletion preserves past sessions. | READ |
| J8 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/data/BackupTest.kt:24 BackupTest | test | backup import | Proves import merge and replace repair active program selection. | READ |
| J9 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/data/ProgramWritesTest.kt:27 ProgramWritesTest | test | program session source rows | Proves program rows stay valid through create, edit, and delete flows. | READ |
| J10 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/timer/TimerStateTest.kt:6 TimerStateTest | test | TimerState phases | Proves countdown, pause, skip, back, and phase advancement rules. | READ |
| J11 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/timer/TimerSnapshotTest.kt:6 TimerSnapshotTest | test | TimerSnapshot | Proves wall-clock restore, expiry, serialization, and backwards-clock handling. | READ |
| J12 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/health/ImportDefaultTest.kt:9 ImportDefaultTest | test | Health import exercise choice | Proves Health Connect draft exercise defaults for modality and plan matches. | READ |
| J13 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/HistoryRulesTest.kt:27 HistoryRulesTest | test | sessions, set_logs | Proves History rows, finished-session totals, and summaries. | READ |
| J14 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/CardioIntervalsKeptTest.kt:43 CardioIntervalsKeptTest | test | interval_logs | Proves interval edits survive added, removed, and concurrent rounds. | READ |
| J15 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/CardioSaveNeedsTest.kt:56 CardioSaveNeedsTest | test | sessions.timeSec, distanceM | Proves new cardio save rules and saved History edits. | READ |
| J16 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/TimerStopRestartTest.kt:53 TimerStopRestartTest | test | sessions.timeSec, interval_logs | Proves stopped timer time and rounds return in the cardio form. | READ |
| J17 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/TimerStoppedTodayTest.kt:30 TimerStoppedTodayTest | test | sessions.timeSec, finishedAt | Proves Today offers Save and Discard after timer stop. | READ |
| J18 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/FinishSaveOnceTest.kt:49 FinishSaveOnceTest | test | sessions.finishedAt, set_logs | Proves finish and discard taps write or leave only once. | READ |
| J19 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayRulesTest.kt:20 TodayRulesTest | test | Today summaries | Proves import row titles and Today status strings. | READ |
| J20 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/TodayScreenTest.kt:87 TodayScreenTest | test | Today sessions | Proves Today screen behavior for starts, saved sessions, and widget requests. | READ |
| J21 | android/app/src/test/java/dev/mirzohidkhon/khonfitness/WriteGuardTest.kt:26 WriteGuardTest | test | Room writes | Checks source write rules for database entities and DAO paths. | READ |
| J22 | docs/tasks/ui07/UI07-title-tabs-week-symbols.md:44 test command | config | unit tests | Documents `android/gradlew -p android :app:testDebugUnitTest --rerun :app:assembleDebug :app:lintVitalRelease`. | READ |




## Not covered

No explicit migration test exists in the current test suite.

## K. Entries added by attack R1

Paths omit `android/app/src/main/java/dev/mirzohidkhon/khonfitness/` unless they begin with `src/` (then `src/` stands for the repository root).

| File and line | Entry |
|---|---|
| data/Dao.kt:16, :27, :46, :63 | Exercise lookup and edits change modality, distance fields and round shape. Exercise merge reassigns existing sessions and set logs. Revalidate before HR and duplicate writes. |
| data/Dao.kt:282, :539, :581 | Direct set upsert can bypass clock transitions. Reset and clearAll are distinct DAO entry points for HR deletion. A9/A10 use upsert. Keep a regression guard against parent replacement deleting valid HR. |
| data/Repository.kt:46, :335, :341 | Expand A90 to cover user-owned average, distance and cardio dates plus the open-form snapshot rule. A43/A44 also define old source ID semantics and import refusal handling. |
| health/HealthImport.kt:38, :52, :80, :87, :104, :145 | Draft date uses the current zone. Provider checks and separate record reads gate these actions. The plan/modality defaults influence Import as. Both record reads need pagination and consistent grouping coverage. |
| ui/screens/ImportScreen.kt:55, :94, :106, :124, :137, :160 | Default choice, Import all, explicit Import as, permission actions and row accessibility labels. These decide what the user can see and what can save without a fresh pick. |
| ui/screens/TodayScreen.kt:28, :95, :100 | Foreground clock refresh already exists. Today checks updates and reads the pending count once per composition. The count can remain stale after a database change. |
| timer/TimerService.kt:110, :139, :175, :281, :387, :389 | Record both restore-to-completion and normal completion as writer callers. A timer command carries no session or run ID. Note the direct replay and snapshot clear even when recordTimer returns a failed Result. |
| ui/screens/TimerScreen.kt:53, :74, :84, :124 | Session-flow guard, finish handoff, delayed exit and debug speed change. The speed change updates the timer state outside the normal command path. |
| ui/screens/StretchScreen.kt:84, :109, :152, :159 | Shared service controls and debug speed write. Track these as preserved behavior when adding workout clock data. |
| ui/components/Components.kt:254 and ui/screens/CardioScreen.kt:45, :74, :79, :80 | Capture what counts as touching or clearing numeric input. Session scalar edits compare snapshots while round edits record explicit changes. |
| ui/AppViewModel.kt:16, :21, :24, :27, :50 | Session and log streams start from empty values. New data screens and count refresh need a loaded state. The existing update call is a separate network entry point. |
| ui/KhonNav.kt:121, :126, :143, :146 and ui/screens/PlanScreen.kt:363 | Navigation, Review, Import and Settings entries for foreground read, screen replacement and back behavior. |
| ui/screens/PlanScreen.kt:392, :404, :416, :424, :481, :485 and ui/screens/EditorRules.kt:217 | Document input/output, before-import copy, recovery directory, restore and reset. The current recovery list does not look in private internal storage. |
| src/android/app/src/main/AndroidManifest.xml:3, :25 and src/android/app/src/debug/AndroidManifest.xml:3 | Existing Internet permission, platform backup opt-in and debug-only Health Connect write permissions. Distinguish providers from automatic storage and the existing debug write. |
| src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/update/Updater.kt:21 | Existing update URL. Verify the new HR and recovery paths never feed this request or add another outbound path. |
| src/android/app/build.gradle.kts:19, :57 | Version fields and Room schema output configuration. |
| src/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/TimerExitTest.kt:43, TapTargetTest.kt:55, HistoryShotsTest.kt:47 and ../theme/ContrastTest.kt:13 | Existing exit, touch target, screen capture and contrast checks that P3 uses or must extend. Test paths after TimerExitTest are relative to its directory. |

## L. Entries added by attack R2

| ID | Entry | Add to coverage |
|---|---|---|
| L1 | [CardioScreen.kt:39], [Dao.kt:232] | Expand A18 and K from form snapshots to explicit scalar and interval edits. Trace same-value and blank edits to the same write as ownership, including edits before any HR row. |
| L2 | [Dao.kt:554], [Dao.kt:572], [Repository.kt:370] | Expand F2, F5 and F6 to cover which version supplied each surviving HR row, its ownership and range inputs, omitted source claims, and atomic reconciliation. Trace the export snapshot to the deletion transaction's revision check. |
| L3 | [SessionScreen.kt:122], [Dao.kt:223] | Expand A17 and B27 to cover an existing gym session losing its final stamp in History. Trace how the range distinguishes that case from a migrated record. |
| L4 | [TimerService.kt:210], [TimerService.kt:330], [TimerService.kt:355], [TimerSnapshot.kt:35] | Add normal clock jumps, the notification action's identity, suspended app-scope completion after service destruction, and snapshot cleanup for a second run on the same session. |
| L5 | [TimerService.kt:172], [TimerService.kt:410], [CardioScreen.kt:60] | Expand A99 and D19 to distinguish a live snapshot from a failed completed write after the owner saves a typed duration. |
| L6 | [HealthImport.kt:104], [HealthImport.kt:121], [HealthImport.kt:150], [Repository.kt:341] | Expand E5–E7 and E10 to trace raw member ranges through the write check. Trace the existing draft HR, distance, and round calculation through the new reader rules or its removal from the write path. |
| L7 | [TodayScreen.kt:103], [TodayScreen.kt:143], [Dao.kt:232], [Dao.kt:271] | Add the band-first order. Today offers Import while an app session is unfinished. Trace both serialized save orders through the same-workout check. |
| L8 | [ReviewScreen.kt:28], [ReviewScreen.kt:80], [2-HR01-COMMON.md:115] | Add the stale-result and removal paths. Trace the facts shown by Review and the new list and sheet when the current range, stored row, or user action changes. |
