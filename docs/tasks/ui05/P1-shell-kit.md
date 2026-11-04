# UI05 P1: app shell, shared components, charts, app icon

Class: Normal. Read `COMMON.md` in this folder first; it holds the goal,
the behavior decisions, the rules and the checks. Review follows
`~/.config/agents/review-protocol.md`.

Later phases build their screens on what this phase adds, so make each
piece a reusable composable with a clear name.

## Expo reference

- Tab shell: `expo/src/components/layout/TabBar.tsx`, `TabScreen.tsx`,
  `LargeTitleHeader.tsx`, `NavBar.tsx`, `EditorScreen.tsx`,
  `useScrollY.ts`, `expo/src/components/ui/useSlidingCapsule.ts`,
  `Segmented.tsx`, `expo/src/app/(tabs)/_layout.tsx`.
- Kit: `expo/src/components/ui/` `IconTile`, `ActionRow`, `TintedButton`,
  `Capsule`, `ConfirmSheet` + `useConfirm`, `ContentEmpty`, `SearchEmpty`,
  `SearchField`, `ChoiceSheet`, `NumberField` + `useRepeatPress`,
  `GroupFooter`, `IndexBadge`, `SymbolCircle`, `EdgeFade`, `FieldRow`.
- Charts: `expo/src/components/ui/LineChart.tsx`.
- Icon and splash: `expo/assets/images/` (`icon.png`,
  `android-icon-foreground.png`, `android-icon-monochrome.png`,
  `splash-icon.png`, `notification-icon.png`), `expo/app.json`.
- Rules: `expo/docs/DESIGN.md` sections "HIG mapping" (large title, tab
  bar), "Lists" (icon tiles, empty and no-results), "Editors" (pick sheets,
  steppers, action rows, fields in groups, bar hairline), "Buttons",
  "Charts".

## Kotlin targets

`ui/KhonNav.kt`, `ui/components/Components.kt`, `ui/components/LineChart.kt`,
`ui/screens/Common.kt`, `ui/theme/Theme.kt`, `res/mipmap-*`, `res/drawable`,
`res/values*` (splash theme), `AndroidManifest.xml` only for icon or splash
attributes.

## Contract

| # | Action | Data | Expected |
|---|---|---|---|
| 1 | Scroll a tab screen | Today with content | Large title stays at the top and folds: bar bottom 60 -> 48 over the first 12 pt, title shrinks toward its leading edge to about 20 pt over the first 40 pt, hairline fades in once content passes under it |
| 2 | Tap another tab | any tab | The selected capsule slides to it on a quick spring, one light haptic click, screens shift sideways as they swap |
| 3 | Change a segmented control | any segmented control | Selection slides like the tab bar's, one haptic click |
| 4 | Scroll an editor or list screen | any pushed screen | Title stays centered; hairline fades in over the first 16 px of scroll |
| 5 | Show an icon tile row | new `IconTile` + row | 30 px tile, white symbol on a rounded square (radius 7) of its color, like iOS Settings |
| 6 | Show action rows | new `ActionRow` group | 20 px symbol + name in accent; destructive one red in its own group; dim when off; reason as muted footnote under the groups |
| 7 | Show a tinted button | new `TintedButton` | 44 px capsule, accent text on accent at 20 percent; red variant for destructive |
| 8 | Ask before delete | new `ConfirmSheet` (+ a small state helper) | Sheet with what is deleted and a red confirm; cancel does nothing. P3 to P5 use it for every delete |
| 9 | Pick from a long list | `ChoiceSheet` with search | Search field over the list, empty each time the sheet opens; filters as you type |
| 10 | Search finds nothing | screen list or pick sheet | No-results view in the list's card: 32 px magnifying glass muted, "No Results for “q”" Headline, "Check the spelling or try a new search." muted 15, centered |
| 11 | Empty content | new `ContentEmpty` | Card with a symbol, a title, a line and an optional action |
| 12 | Tap or hold a stepper | number stepper (kg, reps, time steppers) | Tap steps once with a light haptic; hold past 350 ms repeats, 160 ms apart, 15 ms faster per step down to 50; saves once on release. Pure timing function unit tested |
| 13 | Show any line chart | Today and History charts | 190 px tall everywhere; axis and date labels 12/500 muted; last value label 13/700 white; value axis as wide as its widest label plus 8 px; right inset 8 px. Axis-width function unit tested |
| 14 | Install and open the app | launcher | New K logo icon (adaptive with monochrome layer), splash with the K logo, notification small icon from `notification-icon.png` if the app posts notifications with an app icon today |
| 15 | Existing labels | TalkBack labels from the 0.4.8 audit | Unchanged or improved; tabs and segments keep spoken names and selected state |

Contract rows 5 to 11 need only the shared composables here; the screens
adopt them in P2 to P5. Adopt them in this phase only where the Expo app
uses them in the shell itself (tab bar, nav bars, chart call sites).

## Done

`COMMON.md` checks pass; `DESIGN.md` sections HIG mapping, Lists, Editors
(shared parts), Buttons and Charts updated; report as `COMMON.md` says.
