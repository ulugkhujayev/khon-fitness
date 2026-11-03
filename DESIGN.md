# Khon Fitness design rules

These rules bind the mockup and the Kotlin app. A screen that breaks one
of them is a defect. Since 2026-09-12 the rules follow Apple's Human
Interface Guidelines (Typography, Color, Dark Mode, Layout, Lists,
Toolbars, Tab bars, Buttons, Sheets, Materials) as closely as Android 9
allows. Owner feedback that produced the earlier rules, 2026-09-08:
floating x icons, misaligned name fields, captions on every field, huge
padded cards, per-item buttons, "Updated" lines.

## HIG mapping

- Typeface: Inter, bundled, as the stand-in for SF Pro. Weights Regular,
  Medium, Semibold, Bold only; no Light.
- Text styles at the default size: Large Title 34/41 bold, Title 1 28/34
  bold, Title 2 22/28 bold, Headline 17/22 semibold, Body 17/22, Subheadline
  15/20, Footnote 13/18, Caption 1 12/16, Caption 2 11/13. Tabular figures
  for numbers.
- Dark appearance semantic colors: background #000000, group surface
  #1C1C1E, fill inside a group #2C2C2E, separator #38383A, label white,
  secondary label #8E8E93, tertiary #636366. Accent systemOrange #FF9F0A,
  green #30D158, blue #0A84FF, red #FF453A. Label on an accent fill is white.
- Navigation bar: 56 pt, a round 36 pt Back button with a chevron and no
  text, the title centered in Headline between two equal 88 pt sides, the
  primary action trailing as a 34 pt accent capsule ("Done") or a round
  symbol button ("+"). A hairline fades in on its bottom edge over the
  first 16 pt of scroll.
- Large title: 34 bold on the leading edge at the top of a tab screen;
  bar items trailing as round symbol buttons. It stays at the top and
  folds as the content scrolls: over the first 12 pt the bar's bottom edge
  rises with the content (60 pt to 48), over the first 40 pt the title
  shrinks toward its leading edge to about 20 pt, and a hairline fades in
  once content passes under it. At a large font scale the title row grows
  to the title's line height and the fold distance stays 12 pt.
- Tab bar: a floating capsule 60 pt tall, 24 pt from the sides, 10 pt above
  the bottom edge, group surface with a 1 pt hairline and a soft shadow;
  three items with filled symbols and 11 pt labels; the selected item sits
  on a #2C2C2E capsule tinted accent. The capsule slides to the tab tapped
  on a quick spring (damping 20, stiffness 240, mass 0.8) with a light
  haptic click, and the screens shift 50 pt sideways and fade over 150 ms
  as they swap. Segmented controls slide and click the same way.
- Inset grouped lists: radius 20 pt, rows 48 pt minimum with 17 pt Body,
  hairline separators inset to the text, values in secondary label on the
  right, a small chevron symbol in tertiary label. Section headers are
  Footnote in secondary label, sentence case, inset 16 pt. Field labels in
  editors are Body in label color, values on the right.
- Buttons: one prominent button per screen, a 50 pt full-width capsule in
  accent with white 17 semibold text. Other actions are plain accent text,
  tinted capsules or action rows (see Buttons and Editors). Destructive is
  red.
- Sheets: top radius 24, a 36x5 grabber in tertiary label, the sheet on
  #1C1C1E, groups inside on the elevated #2C2C2E. Swipe to dismiss.
- Toggles: 51x31 with systemGreen on, #3A3A3C off, white thumb.
- Materials: Android 9 has no live blur, so bars are opaque group surface
  instead of Liquid Glass. Content does not show through them.
- Minimum tap target 44x44. Screen margin 16. Rhythm 8.

## Grid

- 4 px base. Horizontal screen padding 16 px. Vertical rhythm 8 px.
- One column. Nothing floats. Every element sits on the left edge, the
  right edge, or in a fixed-width column.
- Frame 390 px wide. No horizontal scroll ever.

## Type

- One family: system sans (Roboto on Android, -apple-system in the
  mockup). Numbers use tabular figures (font-variant-numeric: tabular-nums).
- Sizes come from the HIG mapping above. Nothing else.
- No uppercase tracking labels. No captions above inputs.

## Color

- Values come from the HIG mapping above. Modality colors come from the
  modality list.
- A color is shown as a 12 px filled circle. A color is edited on a
  full-screen editor with a row of 12 preset circles; tap one. No
  native color input.
- Import: the source on a card (a red heart tile, "Health Connect",
  "Last 60 days"); then "3 new sessions from the band" over two-line
  rows: a runner tile in the exercise's color, the workout type with the
  exercise it imports as on the right of the same line (Choose in
  accent), and "Mon, Oct 5 · 32 min · 5.2 km · 141 bpm · Mi Fitness"
  (the app that wrote it) on a line of its own; a footnote; "Import 1
  session" or "Import all 3 sessions" ("Import 2 matched" when some need
  a pick). Reading, all caught up, and missing
  access are each a card with a symbol, a title and a line, with the one
  action. Debug builds add a "Debug: write a test run" text button.

## Lists

- A list is a grouped inset list, iOS Settings style: one rounded
  surface (#1a1a1d, radius 14) that holds 54 px rows separated by inset
  hairlines. No card per item, no outer border, no shadow.
- Row anatomy, left to right: optional 12 px color dot (24 px column),
  primary text, spacer, optional secondary text or value in muted color,
  optional chevron "›" in muted color (24 px column). All vertically
  centered on one baseline.
- Tap the row to open its editor. No buttons inside rows. No x, no
  Open, no Use, no Edit. Destructive and secondary actions live in the
  editor screen, at the bottom, as action rows (see Editors).
- Add on a list screen is a round "+" in the bar. Inside an editor it is
  a full-width 44 px tinted "Add …" button 12 px under its list (Add
  block, Add exercise, Add second exercise, Add stretch). A program's blocks are
  numbered like a session's: the index in a 24 px accent-tinted circle.
  A routine's order rows carry up, down and remove as bare symbols on
  44 px targets (remove in red; up off on the first row, down on the last).
- No dates on list rows unless the row IS a date (history).
- Rows that open a part of the app lead with a 30 px icon tile like iOS
  Settings: a white 17 px symbol on a rounded square (radius 7) of its
  color. The tile is decorative; the row names it. The Programs tab
  groups them under section titles: Exercises, Stretching and Modalities
  under Library, Week plan under Plan, Settings on its own. Settings rows
  carry tiles too.
- The week plan's section title sums up the template ("5 training days ·
  2 rest days", "1 training day" in the singular). Each weekday leads
  with a tile of what it holds in its color (a dumbbell for a program, a
  runner for cardio, a moon for rest), today's weekday name is in
  accent, and a footnote says a change applies from today on.
- The modality list shows how many exercises each modality groups ("2
  exercises", "No exercises"), a footnote on what modalities are for,
  and the archived ones in their own group when shown.
- Settings has three groups: Backup (export, import, restore a saved
  copy), App (version, the APK updater's state, reopen after updates)
  and Reset to built-in data in red on its own. A footnote under each
  says what it holds or does; the backup footnote shows how the last
  export or import went, red on error.
- Empty state: one muted line, no illustration. Where a screen has nothing
  to show yet and an action fixes it, a card shows a 32 px symbol, a title
  in Headline, a muted 15 line and the action (iOS content-unavailable
  view).
- A search that finds nothing, on a screen or in a pick sheet, shows the
  no-results view in the list's card: a 32 px magnifying glass in muted,
  "No Results for “row”" in Headline, "Check the spelling or try a new
  search." in muted 15, centered.

## Editors

- Full screen, title at top, Back "‹" at left, Done at right.
- Fields are rows: label on the left in muted 14, value on the right in
  16. Text fields edit inline in the row. Selects open a bottom sheet
  with a checklist. Multi-select is a checklist too. A long pick list
  (exercises, stretches) has a search field over it, empty each time the
  sheet opens, that filters as you type.
- Fields sit in groups with section titles, so no label wraps (an
  exercise: name and kind; Muscles; Details). A muted footnote under a
  group says what its setting does, or why a value cannot change. A
  select that cannot change shows its value muted with a small lock
  instead of the chevron: an exercise's kind once a session, a program
  or the plan uses it.
- A toggle row gives its label all the width the switch leaves. The
  program's Active and the block's Superset have a footnote that says
  what they do. The active program's Active is fixed on; turning
  another program on moves the mark.
- A modality's color is picked on the page under a Color title: two rows
  of six 34 px circles on a card, the chosen one in a white ring 2.5 px
  wide with a 2 px gap, each on a 48 px target.
- Numbers use the number widget: minus button, value with unit suffix,
  plus button. No caption above it. No native spinner. A tap on − or +
  steps once with a light haptic click; held past 350 ms it repeats,
  160 ms apart and 15 ms faster each step down to 50, and saves once on
  release. The eating-window time steppers do the same. Steps and typed
  numbers stay inside the field's limits (effort 1 to 10, sets 1 to 10),
  so the field never shows a number that is not stored. An empty field
  with a numeric placeholder (last time, latest bodyweight) steps from
  that number, not from zero. An editor field row is 176 dp wide at
  font scale 1.0 and grows with the font scale up to the room it has.
  At font scale 1.5 and up the field row stacks like a list row: the
  label on its own line, the widget under it at the end, full width.
  A value, unit or placeholder that still does not fit shrinks in steps
  down to 75 percent, then the paddings beside it narrow, and then it
  takes the size that fits, with no floor. Typical values keep their
  size; only extreme values get small. The unit stays and the value
  stays on one line.
- Bottom of the editor: actions as rows in grouped lists, like the
  actions at the end of an iOS contact: a 20 px symbol and the name in
  accent (Archive, Merge into…, Move up, Move down), the destructive one
  in red in a group of its own, both dim when off, the reason as a muted
  footnote under the groups, 32 px below the fields, groups 20 px apart.
- Every delete or remove asks first in a confirm sheet: what is deleted,
  a red full-width button, Cancel. Cancel changes nothing. That covers
  an exercise, a program, a block, an exercise taken out of a block, a
  stretch, a routine and a stretch taken out of a routine.
- A new program or block is a draft. Nothing is saved, and there is no
  Delete, until Done saves the program or the first exercise saves the
  block. Add block on a new program saves it as Done would (the typed
  name or "New program", and its Active choice) so the block has a home.
  From then on it is a saved program: it shows Delete, and leaving
  without Done keeps it.
- A new routine is not a draft. "+" on Stretching saves it at once as
  "New routine", with no stretches, active only when it is the first
  routine, and opens it in its editor. "+" does nothing while that save
  runs, so a double tap makes one routine. The editor edits a saved
  routine: each edit writes at once, Done and Back leave, and leaving
  keeps the routine. The user deletes an unwanted one with Delete.
  The routine's Active has a footnote that says what it does;
  turning it off leaves no routine active.
- The stretch editor groups its fields: name and guide; Timing (Per
  side, Reps, Rep count, Hold or Time); Details (Muscles, Cue, Link);
  Watch the reference once there is a link. Delete is off with the reason
  as a footnote for a built-in stretch and for one a routine row uses
  ("In use by 2 routine rows."), counted once the stretch is on screen;
  a delete refused at confirm time keeps the editor open with the reason.
- The bar keeps its title centered and shows a hairline once content
  scrolls under it, fading in over the first 16 px.

## Buttons

- One primary button per screen at most: 50 px tall, full width, accent
  fill, white text. Start, Finish, Done.
- All other actions are text buttons, 44 px tap height, no fill, no
  border. Exception: an action that must read as a button without
  competing with the primary one is a 44 px tinted capsule, its color as
  text on that color at 20 percent (accent; red for a destructive one).
  A session's Discard workout or Delete session is that capsule in red,
  full width at the end of the page.
- Icon buttons only in the top bar or the set-row check. Never floating
  inside a row.

## Set row (live session)

- Header: the 56 px navigation bar, Back, the program name in Headline,
  Finish as the trailing capsule; under it the green progress bar with
  "12 / 42" in muted 13. The progress row is its measured footnote line
  plus 10 pt, at least 28 pt (38.8 pt at font scale 1.6). It sits over
  the list and folds as the sets scroll: over the row's height the
  progress row slides up under the bar with them and fades, and a 2 px green progress line fades in on the
  bar's bottom edge, so Back, Finish and the progress stay in view.
  Finish is the only way to finish (no Finish workout at the bottom): it
  opens the "Workout done" sheet with time, sets done, volume and new
  bests, then effort and note (the page's own fields too), then Save.
  The figures and fields scroll above the keyboard; Save stays under
  them, in reach.

Two lines, 88 px total, hairline between rows.
- Line 1 (40 px): 32 px check circle, then the set tag "A1" in accent
  12/600, then the full exercise name 17, never truncated (wrap to two
  lines if needed). Right: last time "60 × 8" in muted 14.
- Line 2 (40 px): two number widgets side by side, each half the width:
  [ − 60 kg + ] [ − 8 reps + ]. Value 18/600 tabular, inputs at least
  64 px wide so "100" and "12.5" fit. Delta in green 12 under the widget.
  At font scale 1.5 and up the two widgets stack, each the full width.
- Blocks: header row 52 px (index in accent, names, sets done of total
  "2 / 6", a chevron or a green check), sets under it. One block is open
  at a time, nothing sticks. Opening and closing animate over 250 ms
  (ease in and out): the card grows or shrinks with its sets revealed from
  the top, they fade in or out, the chevron turns from right to down, and
  an opened block scrolls to the top of the list. A check keeps its block
  open while it has a pending set (focus to the next one, else one left
  behind); after its last set the next block with work opens, wrapping to
  an earlier one, and with nothing left every block closes. Focus lands
  once the target row is on screen. In a History session's edit mode a
  check moves nothing.
- A first set with no last time starts at 0 kg.

## Today

- No app bar. The screen title is the first thing.
- Week strip: 7 equal tiles, 6 px apart, each on the group surface,
  rounded (radius 12): the weekday letter 12 muted, the day number 17,
  and a 7 px dot for the planned item: modality color (accent for
  strength), filled for high intensity, ring for low or moderate, none
  for rest. Past days: number muted, dot at 50 percent. Today's tile is
  filled in accent, letter, number and dot in white, the number bold.
- 16 px gap. The item on a group-surface card (16 px padding at the
  sides, 14 top and bottom): a 40 px circle with the kind's symbol in
  its color on that color at 20 percent (dumbbell in accent, a runner in
  the modality color, a moon for rest, a green check once done), then
  the name 22/700 over its line in muted 15. While a session runs the
  line is "In progress · 34 min" in accent and a program adds the
  progress bar in accent with "12 / 42" (green stays for done: the done
  line and its check); Resume or Start under it, 44 px tall (the compact
  primary button) to keep the card small.
- 16 px gap. One group of 64 px rows sized for the home screen: the
  symbol in a 40 px circle of its color like the item card's, the name
  17/600 over a line of state 15 muted (green when it is good news), a
  chevron. First, when a newer release is out, "Update to v0.5.0" with
  "Tap to install" in accent (accent download symbol), "Downloading 42%"
  while the APK downloads (a tap then does nothing); Today checks for it
  on open. Then the active stretch routine (green; "Done today · 10 min · 9
  stretches" in green with a check once stretched), the eating window
  ("Open · closes in 3 h 20 m" in green, or "Closed · opens 12:00",
  gray), and band sessions waiting ("2 new sessions", "From the band,
  ready to import", red heart); a row with nothing to say is left out.
  At large text the name and line wrap instead of cutting.
- 28 px gap. "Bodyweight" 17/600 with the latest value 22/700 on the
  right, a line chart of the last 30 days with a soft area fill, then
  24 px gap and the entry on its own group-surface card (radius 20, 16
  padding at the sides, 14 top and bottom): the picked value 34/700 with
  "kg" in muted 15 (tap to type), its change since the last day weighed
  in muted 13 ("+0.2 kg since Oct 5"), a 42 px ruler picker running to
  the card's edges (ticks 22 / 15 / 9 px, 0.1 kg ticks 10 px apart, a
  label every kg, an accent center line, edges fading into the card, a
  haptic click per tick, firmer on a whole kg), and a tinted button (see
  Buttons): "Add", "Update" when today has another value, "Saved" (off)
  when the value is today's entry; while the typed text is blank or not
  above 0 the button is off ("Add" or "Update" dimmed) and saves nothing.
  The ruler starts at the latest entry, 70 kg before the first. One owner
  at a time: a typed value, a screen reader adjust or a reset stops a
  fling and moves the ruler to it, and the fling never writes over it.
- History's title bar carries Import as a round symbol button. Under it
  a segmented Log / Progress, 54 px tall so each segment is a 48 px
  target; it scrolls with the page.
- History Log: the week title 17/600 centered between 48 px chevron
  buttons (accent, Surface3 at the first or last week). The week card on
  the group surface (16 px padding): "<done> of <planned> planned done"
  17/600 with the tail muted on the right, a 7-part bar (6 px tall, 4 px
  gaps, radius 3), the totals in muted 13 with tabular figures. Day rows
  are at least 56 px: a 40 px day column (weekday 12 muted over the day
  number 15/600, accent today), a 10 px dot (filled in the session's
  color; ring for rest, green once stretched or weighed; ring in the item
  color for today's open item; Surface3 for skipped, Surface2 for later),
  the title 17 (muted for rest and skipped, dim for planned) over a muted
  13 line, "<k> PR" in green 600, a chevron on rows that open. Stretch
  runs add one muted 13 line each under the day's last row. Today's
  row is tinted with the accent at 12 percent.
- History Progress: section titles in muted 13 over grouped lists.
  Record rows: name over the date (and a green "New" chip), "62.5 kg × 6"
  bold on the right. Sets per muscle: a legend (8 px swatches), "Secondary
  muscles count half." in dim 11, then per muscle the name (88 px, 112 at
  large text), two 6 px bars (this week accent over last week Surface3,
  shares of the biggest count of either week) and "8 / 6" muted in a
  60 px column (96 at large text). Bodyweight: range chips 1M / 3M / All
  (48 px targets, the selected one on Surface3), a card with the latest
  value 24/700 and the change in green (muted when up), the chart under
  it. Cardio rows: dot, type, the muted line, "Distance per session" dim
  11 over a 28 px sparkline in the type color with the first and last
  value and date in muted 11. A chip is 11/600 on its color at 18 percent.
- Detail screens (exercise, cardio type, the full exercise list) use the
  editor bar with Back. The exercise list: the search field, then list
  rows with the record or "No sets yet" muted on the right.
  Session rows: the date 15/500 (a green "PR" chip) over the sets or the
  stat line in muted 15. Record points on a chart are green.

## Stretch player

- Opening a routine shows a ready screen. A 56 dp full-width Start button
  begins a visible 3, 2, 1 preparation countdown. Exercise time starts after 1.
- Each stretch and side change waits for another tap. Skip also opens the
  next ready screen. Cardio work/rest intervals still advance automatically.
- Background is black while ready, preparing, or paused, and green while
  stretching. State is also shown in text.
- Screen margins are 16 dp. Top Stop and Skip controls have equal 64 dp
  widths and at least 48 dp tap targets. The routine title is centered.
  Stop (an X) and Skip (forward-end) are 40 px round controls, a white
  symbol on white at 18 percent, readable on black and on green; Skip is
  dim when there is nothing to skip. 10 px under the bar, the run's
  progress: one 3 px segment per stretch and side, 3 px apart (done white
  at 60 percent, the current one white, the rest white at 18 percent) and
  "3 of 12 · 6:30 remaining" in footnote under it. The line under the
  stretch's name carries its length ("Left · Hold · 0:40", "10 reps ·
  1:00"); a ready screen shows no status line of its own, and the running
  states say Get ready, Hold, Move or Paused. Debug builds keep a ×20
  button under Start to test a routine quickly.
- One primary action stays at the bottom: Start, Pause, or Resume. Content
  scrolls above it on short screens or with large text. Controls never overlap.
- The stretch name and side appear above an offline 3D human demonstration.
  Before Start, the guide automatically plays setup, movement, and hold steps.
  Numbered steps pause on a position. Play demo restarts the lesson. Change
  view switches between two camera angles. Each step has a plain instruction.
  Play/Pause and Change view are 40 px round controls over the image's
  bottom corners, 10 px in, a white symbol on black at 45 percent, like a
  video player's. The steps are a timeline: 26 px numbered nodes on a
  3 px line, each title under its node in footnote muted. Nodes before
  the shown step are accent with white numbers, the shown one also sits
  in a 34 px accent-tinted halo with its title white and semibold, later
  ones are Surface3 with muted numbers. The line runs between the first
  and the last node in Surface2 and fills in accent as far as the demo
  has come, moving on as the pose moves into each step. A tap on a step
  holds its pose. During the stretch the steps are hidden and the
  instruction follows the nearest step. The same demonstration heads the
  stretch editor.
- Demo playback does not start the exercise clock. A new stretch or side
  opens with its demonstration playing. The Start button stays fixed below
  the scrollable guide, timer, and routine progress.
- Held stretches show the held position. Repeated movements follow the
  exercise clock and freeze when paused. No bouncing through a held pose.
- The continuous human mesh has a gray body, dark shorts from the navel to
  just below the knees, coral muscle highlights, visible hands and feet,
  and floor contact shadows. Right-side
  guides mirror both images and cues.
  Plow places feet beyond the head; 90/90 shows both knee bends.
- Beeps and speech use Android media audio and its selected output. The
  notification is silent. Disconnecting headphones pauses active playback.
- A run's result is written once. A session the user deleted can come back
  only from a stale timer snapshot replayed after the delete. A run restored
  after process death whose routine was deleted meanwhile records nothing.
  Both are accepted limits (`SPEC.md`, Stretching).
- With no active routine (the user turned it off, or an import brought none),
  Today shows the first routine and the widget shows no routine.
- After an import the app shows the repaired data: at most one active routine,
  no routine row without its routine or stretch, and a settings row for the
  eating window (`SPEC.md`, Backup).

## Eating window

- Today row, in the grouped list under the workout: dot (green open,
  muted ring closed), "Eating window", secondary "closes in 2 h 10 m" or
  "opens 12:00", chevron. Tap opens a bottom sheet.
- Sheet, titled "Eating window", scrolls when it does not fit. First an
  elevated card, 16 dp inside, with the state in words: "Open · closes in
  2 h 10 m", "Closed · opens 12:00" or "Off", after a 10 dp dot (filled
  green when open, a muted ring otherwise). While the window is on, a day
  bar sits 14 dp under it.
- Day bar: the day from 00:00 to 24:00 as an 8 dp Surface3 track. The
  fixed hours are green, and a 2 x 16 dp white mark shows now. The opening
  and closing times sit 4 dp under the ends of the green in caption 12
  muted with tabular figures. Each label centers under its end, stays
  inside the bar, and keeps 4 dp from the other one when short hours bring
  them close. The bar is decorative, so screen readers skip it.
- Under the card, while the window is on, a tinted button 12 dp down:
  "Open now" or "Close now". It writes today's manual open or close,
  re-arms the alarms and closes the sheet.
- Then an elevated group 20 dp down: On (switch), Opens, Closes, Remind
  before. Opens and Closes are time steppers, 150 x 38 dp on Surface3,
  − and + on 40 dp sides with dividers, the time 19 bold tabular. They
  step 15 minutes per tap and repeat when held, like the number widget,
  and save once on release. The spoken labels are "Opens 15 minutes
  earlier", "Opens 15 minutes later", and the same for Closes. The hours
  never cross midnight and stay at least an hour long. A large font widens
  the stepper and keeps the time on one line. Remind before is a number
  widget from 0 to 120 minutes in steps of 5.
- The clock in the sheet moves on each minute while the sheet is open.
- Last 28 days, while the window is on: a group title, then an elevated
  card with four weeks of seven 12 dp dots in 28 x 24 dp cells, oldest
  first. A day with no manual open or close is filled green, a changed
  day is a green ring, and today is a white ring. Under the card the
  footer reads "Kept the hours on 20 of 27 days. A ring is a day opened or
  closed by hand; today is white." The count leaves today out. Screen
  readers hear the card as "Last 28 days" and the count.
- Widget: 2x1, surface #1c1c1e, radius 14. Left: 12 px dot. Then "Open"
  or "Closed" 17/600 and "2 h 10 m left" 13 muted on one line under it.
  Tap opens the app on Today with the sheet open.

## Cardio form and interval timer

- The cardio form is an editor screen with Save in the title bar. Its
  first group holds the date, then the exercise's fields in the
  exercise's order, then any field this session stored that the exercise
  no longer lists. Each number field is a number widget. Stroke shows its
  value with a chevron and opens a pick sheet.
- Values the form works out sit at the end of the group in body muted:
  the pace from time and distance ("5:30 /km"), and for a swim the
  distance from laps and pool length ("1000 m") and its pace per 100 m.
- Intervals, when the session has them: one row per work interval,
  "Round 1", with heart rate and watts as two stacked number widgets.
  Their spoken labels are "Round 1 heart rate" and "Round 1 watts".
- Notes: Effort (1 to 10, spoken "Effort, 1 to 10") and Note. Under them
  a red text button, "Delete session" for a saved session or "Discard"
  for a new one, asks in a confirm sheet first.
- The interval timer fills the screen in the phase's color: #14532d for
  work, #1e3a5f for rest, #3a3a40 for warm-up, black while ready,
  counting in, paused or done. Stop sits on the left of the top bar. Both
  sides are 64 dp wide, so the exercise name stays centered as a heading.
- In the middle: the phase word in headline at 75 percent white (screen
  readers hear it when it changes), the countdown 96 bold with tabular
  figures, "Round 2 of 4" 24 semibold, "Next: rest 0:30" in body, and
  "Elapsed 1:08 of 4:30" in subheadline at 60 percent white.
- At the bottom: the background-permission note and "Allow background"
  while the app may be paused with the screen off, then Start timer,
  Pause or Resume as the one primary button.

## Frame

- The bottom nav is pinned to the bottom of the frame at all times. The
  content area scrolls above it. Short screens leave empty space above
  the nav, never below it.

## Forbidden

A card per item, per-row buttons, floating icons, captions above
inputs, uppercase labels, pill badges, "Updated …" lines, native color
and number chrome, helper sentences, more than one primary button, any
element that is not on the grid.

## Charts

Every chart has: dashed grid lines at round values with a label on the
left, a marker on every point, a value label on every point when there
are seven or fewer (else on the last and the highest), the last point
filled in accent with its unit, and dates along the bottom. A line
without markers and labels is a defect. Every chart is 190 px tall
(Today and History alike). Axis and date labels 12/500 in muted; value
labels 12/500 muted, the last one 13/700 in white. The value axis is as
wide as its widest label plus 8 px, the right inset 8 px, so the chart
sits even between the screen margins.
