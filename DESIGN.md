# Khon Fitness design rules

These rules bind the mockup and the Kotlin app. A screen that breaks one
of them is a defect. Owner feedback that produced them, 2026-09-08:
floating x icons, misaligned name fields, captions on every field, huge
padded cards, per-item buttons, "Updated" lines.

## Grid

- 4 px base. Horizontal screen padding 16 px. Vertical rhythm 8 px.
- One column. Nothing floats. Every element sits on the left edge, the
  right edge, or in a fixed-width column.
- Frame 390 px wide. No horizontal scroll ever.

## Type

- One family: system sans (Roboto on Android, -apple-system in the
  mockup). Numbers use tabular figures (font-variant-numeric: tabular-nums).
- Sizes: title 30/700, section 18/600, row 17/400, secondary 15/400 in
  muted color, numeric input 18/600, big number 22/600. Nothing else.
- No uppercase tracking labels. No captions above inputs.

## Color

- Background #121212. Surface rows #1c1c1e only where a row needs to be
  set apart (inputs, pressed state). Divider #2a2a2c, 1 px.
- Text #f2f2f2, muted #9a9a9a, accent #f0a35a (strength), delta green
  #4cd28a. Modality colors come from the modality list.
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
