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
  text, the title centered in Headline, the primary action trailing as a
  34 pt accent capsule ("Done") or a round symbol button ("+").
- Large title: 34 bold on the leading edge at the top of a tab screen;
  bar items trailing as round symbol buttons. It stays at the top and
  folds as the content scrolls (Expo app; the Kotlin title scrolls away):
  over the first 12 pt the bar's bottom edge rises with the content (60 pt
  to 48), over the first 40 pt the title shrinks toward its leading edge
  to about 20 pt, and a hairline fades in once content passes under it.
- Tab bar: a floating capsule 60 pt tall, 24 pt from the sides, 10 pt above
  the bottom edge, group surface with a 1 pt hairline and a soft shadow;
  three items with filled symbols and 11 pt labels; the selected item sits
  on a #2C2C2E capsule tinted accent.
- Inset grouped lists: radius 20 pt, rows 48 pt minimum with 17 pt Body,
  hairline separators inset to the text, values in secondary label on the
  right, a small chevron symbol in tertiary label. Section headers are
  Footnote in secondary label, sentence case, inset 16 pt. Field labels in
  editors are Body in label color, values on the right.
- Buttons: one prominent button per screen, a 50 pt full-width capsule in
  accent with white 17 semibold text. Other actions are plain accent text.
  Destructive is red. Nothing else has a fill.
- Sheets: top radius 24, a 36x5 grabber in tertiary label, the sheet on
  #1C1C1E, groups inside on the elevated #2C2C2E. Swipe to dismiss.
- Toggles: 51x31 with systemGreen on, #3A3A3C off, white thumb.
- Materials: Android 9 has no live blur, so bars are opaque group surface
  instead of Liquid Glass. Content does not scroll under them.
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
  editor screen, at the bottom, as text buttons.
- Add is a single "+" text button at the right of the section title.
  Expo app: inside an editor it is a tinted "Add …" button under the list
  instead (Add block, Add exercise, Add stretch), and a program's blocks
  are numbered like a session's (the index in a 24 px accent-tinted
  circle). A routine's order rows carry up, down and remove as bare
  symbols with 44 px targets.
- No dates on list rows unless the row IS a date (history).
- Rows that open a part of the app (Programs tab: Exercises, Stretching,
  Modalities, Week plan, Settings) lead with a 30 px icon tile like iOS
  Settings: a white symbol on a rounded square (radius 7, continuous) of
  its color, grouped under section titles (Library, Plan; Settings on
  its own). Expo app.
- Empty state: one muted line, no illustration. A search that finds
  nothing, on a screen or in a pick sheet, shows the system's no-results
  view in the list's card instead (Expo app): a 32 px magnifying glass in
  muted, "No Results for “row”" in Headline, "Check the spelling or try a
  new search." in muted 15, centered.

## Editors

- Full screen, title at top, Back "‹" at left, Done at right.
- Fields are rows: label on the left in muted 14, value on the right in
  16. Text fields edit inline in the row. Selects open a bottom sheet
  with a checklist. Multi-select is a checklist too. A long pick list
  (exercises, stretches) has a search field over it, empty each time the
  sheet opens (Expo app).
- Numbers use the number widget: minus button, value with unit suffix,
  plus button. No caption above it. No native spinner. A tap on − or +
  steps once with a light haptic click; held past 350 ms it repeats,
  160 ms apart and 15 ms faster each step down to 50, and saves once on
  release (Expo app).
- Bottom of the editor: Archive, Merge into…, Delete as text buttons,
  Delete in red, disabled with a one-line reason when not allowed. Expo
  app: they are action rows in grouped lists, like the actions at the end
  of an iOS contact: a 20 px symbol and the name in accent (Archive,
  Merge into…, Move up, Move down), the destructive one in red in a group
  of its own, both dim when off, the reason as a muted footnote under the
  groups, 32 px below the fields, groups 20 px apart.
- Fields sit in groups with section titles (an exercise: name and kind;
  Muscles; Details), so no label wraps. A select that cannot change shows
  its value muted with a small lock instead of the chevron, and a muted
  footnote under its group says why (an exercise's kind once it is used).
- The bar keeps its title centered and shows a hairline once content
  scrolls under it, fading in over the first 16 px (Expo app).
  Expo app, program and block editors: Add block and Add exercise / Add
  second exercise are full-width 44 px tinted buttons 12 px under their
  list; Move up, Move down, Delete program and Delete block are action
  rows as above. Every delete or remove asks first in a confirm sheet:
  an exercise, a program, a block, an exercise from a block, a stretch
  from a routine. A new routine is a draft too, saved by Done or its first
  stretch. The block's Superset toggle and the
  program's Active toggle have a muted footnote under their group saying
  what they do; the active program's toggle is fixed on (turning another
  program on moves it). A new program or block is a draft: nothing is
  saved, and there is no Delete, until Done saves the program or the
  first exercise saves the block. Add block on a new program saves it so
  the block has a home, but left without Done and without a block it is
  deleted again, and its Active choice waits for Done.

## Buttons

- One primary button per screen at most: 52 px tall (44 on Today's
  card, Expo app), full width, accent
  fill, dark text. Start, Finish, Done.
- All other actions are text buttons, 44 px tap height, no fill, no
  border. Exceptions (Expo app): Today's bodyweight Add is a 44 px
  capsule tinted, accent text on accent at 20 percent, so it reads as a
  button without competing with Start; a session's Discard workout or
  Delete session is the same capsule in red, full width at the end of
  the page.
- Icon buttons only in the top bar or the set-row check. Never floating
  inside a row.

## Set row (live session)

- Header (Expo app; the Kotlin screen has a large title and the date):
  the 56 px navigation bar, Back, the program name in Headline, Finish as
  the trailing capsule; under it the green progress bar with "12 / 42"
  in muted 13. It sits over the list and folds as the sets scroll: over
  the first 28 pt the progress row slides up under the bar with them and
  fades, and a 2 px green progress line fades in on the bar's bottom
  edge, so Back, Finish and the progress stay in view. Finish is the only
  way to finish (no Finish workout at the bottom): it opens the "Workout
  done" sheet with time, sets done, volume and new bests, then effort
  and note (the page's own fields too), then Save.

Two lines, 88 px total, hairline between rows.
- Line 1 (40 px): 32 px check circle, then the set tag "A1" in accent
  12/600, then the full exercise name 17, never truncated (wrap to two
  lines if needed). Right: last time "60 × 8" in muted 14.
- Line 2 (40 px): two number widgets side by side, each half the width:
  [ − 60 kg + ] [ − 8 reps + ]. Value 18/600 tabular, inputs at least
  64 px wide so "100" and "12.5" fit. Delta in green 12 under the widget.
- Blocks: header row 52 px (index in accent, names, sets done of total
  "2 / 6", a chevron or a green check), sets under it. One block is open
  at a time, nothing sticks. Opening and closing animate over 250 ms
  (ease in and out): the card grows or shrinks with its sets revealed from
  the top, they fade in or out, the chevron turns from right to down, and
  an opened block scrolls to the top of the list. A check keeps its block
  open while it
  has a pending set (focus to the next one, else one left behind); after
  its last set the next block with work opens, wrapping to an earlier
  one, and with nothing left every block closes (Expo app).

## Today

- No app bar. The screen title is the first thing.
- Week strip (Expo app; the Kotlin strip is plain cells on the
  background): 7 equal tiles, 6 px apart, each on the group surface,
  rounded (radius 12, continuous corners): the weekday letter 12 muted,
  the day number 17, and a 7 px dot for the planned item: modality color
  (accent for strength), filled for high intensity, ring for low or
  moderate, none for rest. Past days: number muted, dot at 50 percent.
  Today's tile is filled in accent, letter, number and dot in white, the
  number bold. History's month calendar draws its days as the same tiles
  (weekday letters stay in a row above): the number over its dots, the
  selected day on Surface3, other months' days with no tile and a dim
  number, the changed-plan mark in the tile's top right corner.
- 16 px gap. The item on a group-surface card (14 px padding top and
  bottom): a 40 px circle with the
  kind's symbol in its color on that color at 20 percent (dumbbell in
  accent, a runner in the modality color, a moon for rest, a green check
  once done), then the name 22/700 over its line in muted 15. While a
  session runs the line is green "In progress · 34 min" and a program
  adds the green progress bar with "12 / 42"; Resume or Start under it,
  44 px tall (the compact primary button) to keep the card small.
  Expo app; the Kotlin card has no symbol or progress.
- 24 px gap. "Bodyweight" 18 with the latest value 22/600 on the right,
  a line chart of the last 30 days with a soft area fill (accent
  at 20 percent; Expo app, the Kotlin app has a 72 px sparkline), then
  24 px gap and the entry on its own group-surface card (radius 20, 16
  padding; Expo app, the Kotlin app has number widget + "Add" on one
  line), 14 px padding top and bottom: the picked value 34/700 with "kg"
  in muted 15 (tap to type), its change since the last day weighed in
  muted 13 ("+0.2 kg since Oct 5"), a 42 px ruler picker running to the
  card's edges (ticks 22 / 15 / 9 px, 0.1 kg ticks 10 px apart,
  a label every kg, an accent center line, edges fading into the card, a
  haptic click per tick, firmer on a whole kg), and a tinted button (see
  Buttons): "Add", "Update" when today has another value, "Saved" (off)
  when the value is today's entry.

## Stretch player

- Opening a routine shows a ready screen. A 56 dp full-width Start button
  begins a visible 3, 2, 1 preparation countdown. Exercise time starts after 1.
- Each stretch and side change waits for another tap. Skip also opens the
  next ready screen. Cardio work/rest intervals still advance automatically.
- Background is black while ready, preparing, or paused, and green while
  stretching. State is also shown in text.
- Screen margins are 16 dp. Top Stop and Skip controls have equal 64 dp
  widths and at least 48 dp tap targets. The routine title is centered.
- One primary action stays at the bottom: Start, Pause, or Resume. Content
  scrolls above it on short screens or with large text. Controls never overlap.
- The stretch name and side appear above an offline 3D human demonstration.
  Before Start, the guide automatically plays setup, movement, and hold steps.
  Numbered steps pause on a position. Play demo restarts the lesson. Change
  view switches between two camera angles. Each step has a plain instruction.
  The steps are 32 px capsules in one row, 8 px apart (Expo app): the bold
  number, then the title in footnote white on Surface2; the shown step
  filled in accent with white text. The row scrolls sideways when the
  steps do not fit and keeps the shown step in view as the demo plays.
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

## Eating window

- Today row, in the grouped list under the workout: dot (green open,
  muted ring closed), "Eating window", secondary "closes in 2 h 10 m" or
  "opens 12:00", chevron. Tap opens a bottom sheet.
- Sheet: title "Eating window", then a grouped list: "Open now" or
  "Close now" as the first row (accent text), "Hours 12:00–20:00" with a
  chevron to the editor, "History". No buttons in the sheet except the
  rows.
- Widget: 2x1, surface #1c1c1e, radius 14. Left: 12 px dot. Then "Open"
  or "Closed" 17/600 and "2 h 10 m left" 13 muted on one line under it.
  Tap opens the app on Today with the sheet open.
- History: a strip of 8 px cells, one per day, 4 px apart, filled green
  when the window was kept, ring when not, empty when the feature was off.

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
(Today and History alike). Axis and date labels 12/500 in
muted; value labels 12/500 muted, the last one 13/700 in white. The value
axis is as wide as its widest label plus 8 px, the right inset 8 px, so
the chart sits even between the screen margins.
