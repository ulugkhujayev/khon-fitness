# UI07 fix 1 (after the Sol deep review of d291e9f)

Class: Normal. Review, tests and fixes follow `~/.config/agents/review-protocol.md` (step 6: fixer).
Contract: `UI07-title-tabs-week-symbols.md`; this file replaces its rows 5 and 9 where they differ.

## Findings and the decided fix

| # | Class | Finding (Sol) | Fix |
|---|---|---|---|
| F1 | Tap target shrinks with the fold | History's title words shrink as the large title folds; folded "Progress" is 88 x 30 dp; a tap 23 dp below "Log"'s center misses after 10 dp of scroll (`Shell.kt:237`). | History's title row does not fold. It stays pinned at the top at full size; the page content scrolls under it. No small bar title for History (row 5 is dropped for History). Each word keeps a 48 x 48 dp or larger target at every scroll offset. |
| F2 | Visible control loses its action | After 21 dp of scroll the closed word is partly visible but has no click action and no TalkBack tab (`Shell.kt:244`). | Same fix as F1: no fold, no fade, so both words stay tabs with click actions at every scroll offset. |
| F3 | Contrast check ignores opacity | At 10 dp scroll the closed word is drawn at 75% opacity: 2.65:1, below 3:1 (`Shell.kt:248`). | Same fix as F1: no alpha on the words. `K.TitleOff` on `K.Bg` stays the checked pair. |
| F4 | Tap target under 48 dp on narrow screens | At 320 dp width the seven Plan columns are 45.7-46 dp (`PlanScreen.kt:179`). | When the width is under 7 x 48 dp plus 2 x 10 dp, each column is exactly 48 dp and the strip scrolls sideways (`horizontalScroll`), keeping today in view at first. At 336 dp and wider nothing changes. |
| F5 | Selected state not visible | The selected day is Surface3 on Surface2, 1.23:1. | The 2 dp accent ring marks the **selected** day (today at first) instead of today. Today keeps its accent bold letter. Drop the Surface3 fill. Ring color `K.Accent` against `K.Bg` and `K.Surface2` passes 3:1: add to `TextPairs` if not there. |

## Rules

- Per class: search the branch for siblings (other `TabScreen` users, other places that fade or shrink a control on fold) and fix every instance in UI07's diff. Other tabs (Today, Plan) keep their folding large title.
- Tests (each fails before the fix): History words stay 48 x 48 dp, clickable and `Role.Tab` at scroll offsets 0, 10, 21, 60 dp and at the bottom; no alpha below 1 on the words; Plan columns are 48 dp and the strip scrolls at 320 dp width; the ring follows the selection (tap Thursday: Thursday has the ring, Wednesday not); remove or update tests for the dropped folded bar title.
- Update `DESIGN.md` and `SPEC.md` for F1, F4, F5. Commit small, messages like `UI07 fix: ...`. No push. No AI attribution.
- Run only your new tests and the test classes of files you touched; the lead runs the full suite.
- Report under 40 lines: commits, each finding with the test that proves the fix, the tests you ran with output tail. Save Robolectric screenshots (History scrolled 60 dp, Plan with Thursday selected, Plan at 320 dp) to `/tmp/khon-ui07-fix/`.

## Added after verify 1 (design alarm, new class)

| # | Class | Finding (Sol) | Fix |
|---|---|---|---|
| F6 | Taps pass through an opaque bar | The pinned bar (History) and the folding bar (Today, Plan) draw over the content but take no input, so a tap on the bar's background opens the row scrolled under it (`Shell.kt:143`, `:164`). The folding instance predates UI07. | Both bars take every touch on their background (`takeTouches` in `Shell.kt`); their own controls still take taps. `TitleBarTouchTest` covers both bars, so all three tabs. |

Verify 2 found one missed F6 instance: the session's folded progress line (`SessionScreen.kt:287`) drew over the list
without taking input. It now uses the shared `takeTouches`, as the session bar does; `SessionBarTouchTest` taps the line
over a block header. `TitleBarTouchTest` now taps the bar buttons with real touches.

Verify 3 found a side effect of the F6 fix (design alarm, second failed round on F6): the touch shield also blocked
drags, so a swipe that starts on a bar did not scroll (0 px). Redesign: each bar and the session line is a scroll
handle (`scrollHandle` in `Shell.kt`): a plain `draggable` that forwards the drag and its fling to the list's own
scroll state. It is still a hit target, so taps on the background stop there; it adds no scroll semantics. Tests:
`TitleBarTouchTest` and `SessionBarTouchTest` drag from each bar and from the line.

Verify 4 found that `dispatchRawDelta` did not take the list's scroll, so a finger on a bar did not stop a running
fling (P2), and that bar drags skip the list's overscroll effect (P3). Fix: `scrollHandle` takes the list's scroll at
the first touch (`list.scroll(MutatePriority.UserInput)`), as a touch on the list does, moves it by the drag, and
flings on release. Tests: a finger that holds still on each bar and on the line stops a fling (the content is the
control). P3 is left as a known limit: Compose BOM 2024.12 has no way to share a list's overscroll effect, and it only
shows when a drag from a bar reaches the top or bottom end.
