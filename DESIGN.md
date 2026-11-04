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
  secondary label and placeholders #AEAEB2 (Apple's increased-contrast
  gray), tertiary #636366 for chevrons, the grabber, dots and off controls
  only, never for text. Accent systemOrange #FF9F0A, green #30D158, blue
  #0A84FF, red #FF6961 (increased contrast). Text and symbols on an
  accent, green or red fill are black.
- Contrast: every text color on every fill it sits on reads at 4.5:1 or
  more (3:1 for 18 sp bold and up, and for symbols), placeholders
  included, the timer's and the stretch player's phase fills too.
  `TextPairs` in `Theme.kt` lists the pairs and `ContrastTest` checks
  them; it also fails a raw color (hex, or white or black at an alpha)
  for text in the screens, so every such color is a token in `K`. Off
  controls are exempt.
- Navigation bar: 56 pt, a round 36 pt Back button with a chevron and no
  text on a 48 pt target (the same round button sits in a tab's large
  title, reaching 2 pt past its 44 pt row), the title centered in Headline between two equal 88 pt sides, the
  primary action trailing as a 34 pt accent capsule ("Done") or a round
  symbol button ("+"). A hairline fades in on its bottom edge over the
  first 16 pt of scroll. A capsule that is off (the cardio form's Save
  without a time or distance) is #2C2C2E with muted text.
- Large title: 34 bold on the leading edge at the top of a tab screen;
  bar items trailing as round symbol buttons. It stays at the top and
  folds as the content scrolls: over the first 12 pt the bar's bottom edge
  rises with the content (60 pt to 48), over the first 40 pt the title
  shrinks toward its leading edge to about 20 pt, and a hairline fades in
  once content passes under it. At a large font scale the title row grows
  to the title's line height and the fold distance stays 12 pt. History's
  title row is the one exception: it does not fold (see History).
- Tab bar: a floating capsule 60 pt tall, 24 pt from the sides, 10 pt above
  the bottom edge, group surface with a 1 pt hairline and a soft shadow;
  three items with filled symbols and 11 pt labels; the selected item sits
  on a #2C2C2E capsule tinted accent. The capsule slides to the tab tapped
  on a quick spring (damping 20, stiffness 240, mass 0.8) with a light
  haptic click, and the screens shift 50 pt sideways and fade over 150 ms
  as they swap. Segmented controls slide and click the same way. A
  screen pushed from a tab belongs to that tab: the bar marks the tab
  under it on the back stack (an exercise opened from History marks
  History).
- Inset grouped lists: radius 20 pt, rows 48 pt minimum with 17 pt Body,
  hairline separators inset to the text, values in secondary label on the
  right, a small chevron symbol in tertiary label. Section headers are
  Footnote in secondary label, sentence case, inset 16 pt. Field labels in
  editors are Body in label color, values on the right.
- Buttons: one prominent button per screen, a 50 pt full-width capsule in
  accent with black 17 semibold text. Other actions are plain accent text,
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
  a full-width 48 px tinted "Add …" button 12 px under its list (Add
  block, Add exercise, Add second exercise, Add stretch). A program's blocks are
  numbered like a session's: the index in a 24 px accent-tinted circle.
  A routine's order rows carry up, down and remove as bare symbols on
  48 px targets (remove in red; up off on the first row, down on the last).
- No dates on list rows unless the row IS a date (history).
- Rows that open a part of the app lead with a 30 px icon tile like iOS
  Settings: a 17 px symbol on a rounded square (radius 7) of its color,
  black or white, whichever reads better (`inkOn`): black on orange,
  green, blue and the cardio type colors, white on gray. The tile is decorative; the row names it. The Plan tab's
  Library holds Exercises and Stretches; Settings opens from the gear on
  Today. Settings rows carry tiles too.
- The Plan tab: the large title "Plan" with a round "+" (a 36 px circle
  on a 48 px target) that opens a sheet of four rows with tiles: Gym
  program, Cardio workout, Stretch routine, Exercise. Then This week with
  an accent "Edit" (48 px target) on its section title, over a strip of
  seven columns, Monday to Sunday, across the screen width with a 10 px
  inset on each side (no card), each 1/7 of the rest (48.6 px at 360)
  and a full tap target. Below 336 px each column is exactly 48 px and the
  strip scrolls sideways, with today in view at first. Each column holds
  the day's letter (M T W T F S S) in 12 px muted, under it a 40 px
  Surface2 circle with the day's symbol at 22 px. A gym program is a
  dumbbell in accent; a cardio workout by its type id, run a runner,
  bike a bike, swim a swimmer, any other or none a heart, in the type's
  own color (muted without a type); rest is a moon in RestSymbol
  (#8E8E93). Every symbol color reads at 3:1 or more on Surface2.
  Today's letter is accent and bold. A day a one-day change set keeps its
  5 px accent dot, under the circle. A tap selects a day (today at first;
  not saved). The selected day's circle gets a 2 px accent ring (3:1 on
  the page and on Surface2), and one line under the strip, 15 medium, names
  it in full ("Wed · Rest", "Thu · Norwegian 4×4"; long names wrap). The
  strip stays one row at every font scale. Each column is a tab with its
  selected state and speaks its day, full item name, "Today" and
  "Changed for this day only".
- Gym programs, cardio workouts and stretch routines are rows with the
  title over a muted line (up to two lines each), 56 px minimum: a dot
  (filled = active) for programs and routines, a type pill (11 px text in
  the type's color on Surface2, radius 9) for cardio workouts. Archived
  workouts follow under an "Archived" title. An empty list says "No …
  yet"; with no programs a "Create program" row in accent follows.
- The Exercises list: search, then strength exercises under muscle
  section titles in alphabetical order, then one "Archived (n)" row,
  closed at first, whose chevron turns down when a tap opens the archived
  exercises in muted text under it. No kind switch and no muscle filter.
- The week plan's section title sums up the template ("5 training days ·
  2 rest days", "1 training day" in the singular). Each weekday leads
  with a tile of what it holds in its color (a dumbbell for a program, a
  runner for cardio, a moon for rest), today's weekday name is in
  accent, and a footnote says a change applies from today on.
- A cardio workout's editor has a Type row alone in its group, with the
  footnote "Changes the color and type of past sessions too." (not on a
  new workout). Type opens a sheet of the live types with their colors
  and "New type…", which opens a sheet with Name, Color and Add type (off
  while the name is blank).
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
- A toggle row gives its label all the width the switch leaves, and the
  whole row is the switch's target. The
  program's Active and the block's Superset have a footnote that says
  what they do. The active program's Active is fixed on; turning
  another program on moves the mark.
- A new type's color is picked in its sheet under a Color title: two rows
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
  that number, not from zero. Each − and + is a 48 x 48 dp tap
  target; the box is drawn 38 or 44 dp tall in the middle of that
  48 dp row, so the look stays slim. An editor field row is 176 dp wide at
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
  stretch, a routine and a stretch taken out of a routine. Merge and
  Discard workout ask the same way, with counts in the line under the
  title ("3 checked sets are lost.").
- A merge has no Undo bar: the confirm sheet is the only step. A refused
  merge or delete shows its reason as the muted footnote under the
  editor's action groups.
- "Update the program too" refused: a sheet titled "Only this workout
  changed" with the reason in muted Body and an OK button. Screen readers
  hear the line when it shows.
- A new program or block is a draft. Nothing is saved, and there is no
  Delete, until Done saves the program or the first exercise saves the
  block. Add block on a new program saves it as Done would (the typed
  name or "New program", and its Active choice) so the block has a home.
  From then on it is a saved program: it shows Delete, and leaving
  without Done keeps it.
- A new routine is not a draft. "Stretch routine" under "+" on Plan saves it at once as
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
  fill, black text. Start, Finish, Done.
- Every tap target is 48 x 48 px or more (`TapTargetTest` checks each
  tab): a smaller symbol or capsule sits in the middle of a 48 px target.
  A text field is 48 px tall; its width is what its steppers leave.
- All other actions are text buttons, 48 px tap height, no fill, no
  border. Exception: an action that must read as a button without
  competing with the primary one is a 48 px tinted capsule, its color as
  text on that color at 16 percent (accent; red for a destructive one).
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
  A check refused for a missing value shows "Enter weight" or "Enter
  reps" in red 12 semibold under its field, read out when it appears.
  Finish is the only way to finish (no Finish workout at the bottom): it
  opens the "Workout done" sheet with time, sets done, volume and new
  bests, then effort and note (the page's own fields too), then Save.
  The figures and fields scroll above the keyboard; Save stays under
  them, in reach.

Two lines, 88 px total, hairline between rows.
- Line 1 (40 px): 32 px check circle, then the set tag "A1" in accent
  12/600, then the full exercise name 17, never truncated (wrap to two
  lines if needed), then a swap symbol (two arrows) in muted on a 48 px
  target, spoken "Swap <name>"; it opens the swap sheet, as a tap on the
  name does. Right: last time "60 × 8" in muted 14.
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
- The item on a group-surface card (16 px padding at the sides, 14 top
  and bottom): a 40 px circle with the kind's symbol in its color on that
  color at 20 percent (dumbbell in accent, a runner in the modality
  color, a moon for rest, a green check once done), then the name 22/700
  over its line in muted 15. A planned day: "6 exercises · 18 sets"
  (gym) or "Bike · 4 × 4 min, 3 min rest" / "Run · Steady" (cardio),
  with " · changed for today" while a one-day change holds it; "Change"
  in accent 600 at the top right on a 48 x 48 px target; Start under it.
  Rest: "Rest day", "Nothing planned. Stretch and recover.", Change, no
  Start. A week of rest days with workouts to pick is a rest day too.
  No program and no cardio workout yet: "No plan for today" with "Set
  up your week" (opens Plan). While a session runs: "In progress · 3 of 12
  sets" in accent (cardio: the interval timer's state, "Round 2 of 4 ·
  Work", "Paused · …", or the time since its start), a program adds the
  progress bar in accent with the time since the start, and Resume; no
  Change. After the interval timer's Stop the line is "Timer stopped · 1:35"
  and Resume gives way to two halves 10 px apart: Save (primary) and
  Discard (red tinted). Saved today: the green check, the last session saved today
  under its saved name (also a run on a gym day), "Done · 12 sets · 3.1 t · 58
  min" in green ("3 of 12 sets" when some were skipped; cardio "5.2 km ·
  30 min · 5:46 /km"), "+1 more today" in accent (48 px target, opens
  History) when other sessions were saved today, and one tinted
  "Review"; no Change, no Start again. Buttons on the card are 50 px
  (the full primary button), so every target is at least 48 px.
- Change opens a sheet "Today, Tue Nov 3" with "Only today changes. The
  week plan stays." in muted, then Gym, Cardio and Other (Rest) choice
  lists with a check on today's item. A pick writes the one-day change;
  picking the week plan's own item clears it. While a change holds, a
  tinted "Back to plan: <item>" clears it.
- Review from Today opens the saved session read-only (a back button, no
  Edit, no Delete): the date 17/600 over the stats line in muted, then
  one group per exercise with "Set 1  80 kg × 10" rows, a set not done
  reading "skipped" in muted; cardio shows its values as label/value rows.
  History's entry keeps Edit.
- 16 px gap. One group of 64 px rows sized for the home screen: the
  symbol in a 40 px circle of its color like the item card's, the name
  17/600 over a line of state 15 muted (green when it is good news), a
  chevron. First, when a newer release is out, "Update to v0.6.0" with
  "Tap to install" in accent (accent download symbol), "Downloading 42%"
  while the APK downloads (a tap then does nothing); Today checks for it
  on open. Then the active stretch routine (green; "Done today · 9 stretches ·
  10 min" in green with a check once stretched), the eating window
  ("Open · closes in 3 h 20 m" in green, or "Closed · opens 12:00",
  gray), and band sessions waiting ("2 new sessions", "From the band,
  ready to import", red heart); a row with nothing to say is left out.
  At large text the name and line wrap instead of cutting.
- 16 px gap. Bodyweight on one group-surface card (radius 20, 16 padding
  at the sides, 14 top and bottom), no chart (History has it):
  "Bodyweight" 17/600, under it on its own line a status in muted 13:
  "No entry yet", "Last: 83.4 kg · Oct 30", or "Saved for today · −0.4 kg
  since Oct 30". Then a row: − and + as 52 px squares (radius 12,
  Surface2, accent glyph 28) around the value on Surface2 (52 px tall,
  radius 12): the number 28/700 right-aligned in a 96 px slot (it grows
  with the font scale) and "kg" in muted 15. A tap steps 0.1 kg, a hold
  repeats (the shared stepper timing), 20 to 250 kg. Tap the number to
  type on the decimal keyboard; "83,5" and "83.5" both read. Text that is
  not a weight (also text longer than 7 characters) stays as typed with
  "Enter 20-250 kg" in red 13 under the
  row, and the button off. The button is the primary one: "Save" for a
  new day, "Update" when today has another value, "Saved" (off) when the
  value is today's entry. A valid typed value saves with one tap on the
  button or the keyboard's Done. The entry starts at the latest value,
  75.0 kg before the first.
- History's large title is the page switch: "Log" and "Progress" side by
  side in the title row, 34 bold, 16 px apart, Import as a round symbol
  button at the row's end. The open word is white with a 3 px accent bar
  6 px under it, as wide as the word and rounded; the closed word is
  TitleOff (#6C6C70, 4.0:1 on the page). Each word is a 48 px target and
  a tab with its selected state; the row is a heading spoken as
  "History". No "History" word shows. When the words do not fit, their
  size steps down a whole size at a time, not below 22. Android scales
  large text less than small text, so at font scale 1.6 on a 360 px
  screen they fit at 34. The title row does not fold. It stays at the top
  at full size and the page scrolls under it, with the hairline fading in.
  Both words keep their size, color and 48 px target at every scroll
  offset, so a switch never needs a scroll back up.
- History Log: the week title 17/600 centered between 48 px chevron
  buttons (accent, Surface3 at the first or last week). The week card on
  the group surface (16 px padding): "<done> of <planned> planned done"
  17/600 with the tail muted on the right, a 7-part bar (6 px tall, 4 px
  gaps, radius 3), the totals in muted 13 with tabular figures. Day rows
  are at least 56 px: a 40 px day column (weekday 12 muted over the day
  number 15/600, accent today), a 10 px dot (filled in the session's
  color; ring for rest, green once stretched or weighed; ring in the item
  color for today's open item; Surface3 for skipped, Surface2 for later),
  the title 17 (muted for rest, skipped and planned) over a muted
  13 line, "<k> PR" in green 600, a chevron on rows that open. Stretch
  runs add one muted 13 line each under the day's last row. Today's
  row is tinted with the accent at 12 percent.
- History Progress: section titles in muted 13 over grouped lists.
  Record rows: name over the date (and a green "New" chip), "62.5 kg × 6"
  bold on the right. Sets per muscle: a legend (8 px swatches), "Secondary
  muscles count half." in muted 11, then per muscle the name (88 px, 112 at
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
- Screen margins are 16 dp. The top bar's sides are equal, 96 dp: Stop
  on the left, Back and Skip on the right, each a 48 dp target. The
  routine title is centered, up to two lines. Stop (an X), Back
  (backward-end) and Skip (forward-end) are 40 px round controls, a
  white symbol on white at 18 percent, readable on black and on green;
  Skip is dim when there is nothing to skip, Back on the first stretch. 10 px under the bar, the run's
  progress: one 3 px segment per stretch and side, 3 px apart (done white
  at 60 percent, the current one white, the rest white at 18 percent) and
  "3 of 12 · 6:30 remaining" in footnote at 75 percent white under it. The line under the
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
  Play/Pause and Change view are 40 px round controls (48 px targets) over the image's
  bottom corners, 10 px in, a white symbol on black at 45 percent, like a
  video player's. The steps are a timeline: 26 px numbered nodes on a
  3 px line, each title under its node in footnote muted. Nodes before
  the shown step are accent with black numbers, the shown one also sits
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
  closed by hand; today is white." The count leaves today out. Days before
  the window was first turned on are filled #3A3A3C and not counted; the
  footer then adds "Grey days came before tracking." With no counted day
  yet it reads "Tracking since Nov 3." Screen readers hear the card as
  "Last 28 days" and the count.
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
  "Elapsed 1:08 of 4:30" in subheadline at 75 percent white (60 percent
  read at 4.4:1 on the work green). Every secondary line on the player
  fills, the battery note included, is white at 75 percent
  (`K.OnPhaseMuted`).
- At the bottom, while the phone may stop the app with the screen off:
  "If the timer stops when the screen is off, allow Khon to run in the
  background in battery settings." (no phone maker named) and a
  "Battery settings" text button that opens the app's settings page.
  Then Start timer, Pause or Resume as the one primary button.

## Frame

- A bar drawn over scrolling content (a tab's title bar, folding or
  pinned; the session bar and its folded progress line) takes the taps on
  its background, so a tap there never opens a row scrolled under it, and
  a drag that starts on its background scrolls the content (the content's
  own scroll, fling and stretch). A drag that starts on one of its buttons
  stays with the button. Its buttons keep their
  taps.
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
