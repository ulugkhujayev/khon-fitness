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
- Large title: 34 bold on the leading edge at the top of a scrolling tab
  screen; bar items trailing as round symbol buttons.
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
- No dates on list rows unless the row IS a date (history).
- Empty state: one muted line, no illustration.

## Editors

- Full screen, title at top, Back "‹" at left, Done at right.
- Fields are rows: label on the left in muted 14, value on the right in
  16. Text fields edit inline in the row. Selects open a bottom sheet
  with a checklist. Multi-select is a checklist too.
- Numbers use the number widget: minus button, value with unit suffix,
  plus button. No caption above it. No native spinner.
- Bottom of the editor: Archive, Merge into…, Delete as text buttons,
  Delete in red, disabled with a one-line reason when not allowed.

## Buttons

- One primary button per screen at most: 52 px tall, full width, accent
  fill, dark text. Start, Finish, Done.
- All other actions are text buttons, 44 px tap height, no fill, no
  border.
- Icon buttons only in the top bar or the set-row check. Never floating
  inside a row.

## Set row (live session)

Two lines, 88 px total, hairline between rows.
- Line 1 (40 px): 32 px check circle, then the set tag "A1" in accent
  12/600, then the full exercise name 17, never truncated (wrap to two
  lines if needed). Right: last time "60 × 8" in muted 14.
- Line 2 (40 px): two number widgets side by side, each half the width:
  [ − 60 kg + ] [ − 8 reps + ]. Value 18/600 tabular, inputs at least
  64 px wide so "100" and "12.5" fit. Delta in green 12 under the widget.
- Blocks: header row 52 px (index in accent, names, sets count,
  chevron), sets under it.

## Today

- No app bar. The screen title is the first thing.
- Week strip: 7 equal cells, 56 px tall. Weekday letter 12 muted over
  the day number 18. Today's number sits in a 32 px accent-filled circle
  with dark text. One 8 px dot under each number: modality color
  (accent for strength), filled for high intensity, ring for low or
  moderate, none for rest.
- 24 px gap. Item name 30/700, preview 15 muted, Start 56 px.
- 24 px gap. "Bodyweight" 18 with the latest value 22/600 on the right,
  a 72 px sparkline with a soft area fill (accent at 20 percent), and
  one row: number widget + "Add" text button, all centered on one line.

## Stretch player

- Full screen, no nav. Background is the phase color like the timer:
  hold green, side change gray, rest blue.
- Top row: Stop at left as a text button, routine name centered, Skip at
  right. Nothing else in the top row.
- Figure: a line figure, one stroke, white at 90 percent, drawn on a
  240 px square centered on the screen. It moves between two poses in a
  2 s ease and loops. No photo, no illustration style, no shading.
- Under the figure: stretch name 22/600, side "Left" or "Right" 17
  muted, the countdown 96/700 tabular, then "Next: Thread the needle"
  15 at 75 percent, and "4 of 9 · 6:20 left" 15 at 60 percent.
- Pause is a text button at the bottom. A paused player shows "Paused"
  above it. No progress bar, no ring.
- A reps stretch shows "10 reps" in place of the countdown and a small
  rep counter that ticks with the cue.

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
without markers and labels is a defect.
