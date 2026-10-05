import { useEffect, useMemo, useState } from 'react';
import { router, useLocalSearchParams } from 'expo-router';
import { useKeepAwake } from 'expo-keep-awake';
import { useLeaveWhenGone } from '@/components/layout';
import { StretchPlayerView } from '@/components/stretch/StretchPlayerView';
import { routineOf, routineRun, routineSteps } from '@/domain/stretch';
import { isDone } from '@/domain/timer';
import { useStretching } from '@/db/hooks';
import { beginTimer, pauseTimer, resumeTimer, setTimerSpeed, skipTimer, startStretch, stopTimer } from '@/timer/engine';
import { useTimer } from '@/timer/useTimer';

/**
 * The stretch player of a routine (StretchScreen.kt). The engine owns the clock and writes the stretch session;
 * this screen opens the routine (or continues its run), keeps the screen on, and goes back when the run ends.
 */
export default function StretchPlayer() {
  const { routineId } = useLocalSearchParams<{ routineId: string }>();
  useKeepAwake();
  const { data } = useStretching();
  const { state, finished } = useTimer();
  // A run of this routine that ended before the screen opened is old news; only one ending here sends it back.
  const [earlier] = useState(finished?.sessionId ?? null);
  const routine = data?.routines.find((r) => r.id === routineId) ?? null;
  const steps = useMemo(() => (routine && data ? routineSteps(routine.id, data.routineStretches, data.stretches) : []), [routine, data]);
  const ended = finished != null && finished.sessionId !== earlier && routineOf(finished.sessionId) === routineId;
  useLeaveWhenGone(data != null && routine == null);

  // Open the routine at its first ready screen, or continue its run. The engine never replaces another timer.
  useEffect(() => {
    if (routine && steps.length > 0 && !ended) startStretch(routine.id, routine.name, steps);
  }, [routine, steps, ended]);

  // The run ended and wrote its session: back to where the player was opened from.
  useEffect(() => {
    if (ended) router.back();
  }, [ended]);

  const mine = routineRun(state, routineId);
  const run = mine && !isDone(mine) ? mine : null;
  const emptyText = !data || mine ? '' : state ? 'Another timer is running' : steps.length === 0 ? 'No stretches to play' : '';
  return (
    <StretchPlayerView
      state={run}
      title={run?.title ?? routine?.name ?? 'Stretching'}
      emptyText={emptyText}
      onStop={() => (run ? void stopTimer() : router.back())}
      onSkip={skipTimer}
      onBegin={beginTimer}
      onPause={pauseTimer}
      onResume={resumeTimer}
      onSpeed={setTimerSpeed}
    />
  );
}
