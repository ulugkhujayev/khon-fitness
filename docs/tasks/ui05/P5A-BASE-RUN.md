# P5A: sequence tests on the base commit

Base 6249229, scratch worktree `../khon-fitness-wt/ui05-p5a-base` (removed after the run). Each test is a
sequence test of `StretchWritesTest` / `StretchRacesTest` with the new command replaced by what the old screen
wrote (`saveStretch`, `saveRoutine`, `saveRoutineStretch(es)`, `saveEatingWindow`, `saveStretchSession`, old
`exportJson`). Command: `./gradlew :app:testDebugUnitTest --tests '*OldStretch*'`. Result: 23 tests, 23 failed.

- `D3_export_is_one_snapshot`: java.lang.AssertionError: expected:<false> but was:<true>
- `D1_active_on_deleted_routine_changes_nothing`: java.lang.AssertionError: expected null, but was:<StretchRoutine(id=pm, name=Evening, active=true, sortOrder=1)>
- `D1_rename_keeps_newer_active`: java.lang.AssertionError: expected:<[pm]> but was:<[am]>
- `D2_merge_keeps_phone_active`: java.lang.AssertionError: expected:<[pm]> but was:<[am]>
- `D2_replace_two_active_keeps_first`: java.lang.AssertionError: expected:<[r2]> but was:<[r0, r2]>
- `D4_orphan_rows_dropped`: java.lang.AssertionError: expected:<[ok]> but was:<[ok, noRoutine, noStretch]>
- `D5_default_settings_row_after_replace`: java.lang.AssertionError: expected:<EatingWindow(id=1, enabled=false, startMinute=720, endMinute=1200, remindBeforeMin=15)> but was:<null>
- `D6_imported_session_stays`: java.lang.AssertionError: expected:<[StretchSession(id=am:s1, routineId=am, routineName=Morning, date=2026-11-01, startedAt=5, totalSec=900,
- `E10_new_routine_after_highest_order`: java.lang.AssertionError: expected:<3> but was:<2>
- `E11_first_stretch_deleted_writes_nothing`: java.lang.AssertionError: expected null, but was:<StretchRoutine(id=new, name=Night, active=true, sortOrder=2)>
- `E1_Done_on_deleted_writes_nothing`: java.lang.AssertionError: expected null, but was:<Stretch(id=fold, name=Fold, sided=false, mode=hold, seconds=45, reps=0, figure=, muscles=,
- `E1_import_rename_survives_Done`: java.lang.AssertionError: expected:<Stretch(id=fold, name=Forward fold, sided=false, mode=hold, seconds=45, reps=0, figure=, muscles=, cue=L
- `E2_add_of_deleted_stretch_writes_no_row`: java.lang.AssertionError
- `E2_used_after_confirm_opened_is_not_deleted`: java.lang.AssertionError: expected:<Stretch(id=cat, name=Cat cow, sided=false, mode=reps, seconds=45, reps=10, figure=, muscles=, cue=, link
- `E6_edits_of_deleted_routine_write_nothing`: java.lang.AssertionError: expected null, but was:<StretchRoutine(id=am, name=Morning, active=true, sortOrder=0)>
- `E7_imported_seconds_stay_on_move`: java.lang.AssertionError: expected:<90> but was:<null>
- `E7_move_swaps_with_current_neighbour`: java.lang.AssertionError: expected:<[C, A, D]> but was:<[A, C, B, D]>
- `E8_remove_then_move_recreates_nothing`: java.lang.AssertionError: expected:<[(A, 0), (B, 1), (D, 3)]> but was:<[(A, 0), (C, 1), (B, 2), (D, 3)]>
- `E9_tied_orders_renumbered`: java.lang.AssertionError: expected:<[(B, 0), (A, 1), (C, 2)]> but was:<[(B, 0), (A, 1), (C, 1)]>
- `W1_On_changes_only_the_switch`: java.lang.AssertionError: expected:<EatingWindow(id=1, enabled=true, startMinute=600, endMinute=1080, remindBeforeMin=30)> but was:<EatingWi
- `W1_crossing_edits_second_refused`: java.lang.AssertionError: expected:<(1140, 1200)> but was:<(720, 780)>
- `ordering_ties_sort_by_id`: java.lang.AssertionError: expected:<[a, z]> but was:<[z, a]>
- `row14_blank_name_saves_nothing`: java.lang.AssertionError: expected null, but was:<Stretch(id=new, name=  , sided=false, mode=hold, seconds=45, reps=0, figure=, muscles=, cu
