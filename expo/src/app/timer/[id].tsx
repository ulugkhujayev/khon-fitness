import { useEffect } from 'react';
import { useLocalSearchParams } from 'expo-router';
import { useKeepAwake } from 'expo-keep-awake';
import { TimerView } from '@/components/cardio/TimerView';
import { useLeaveWhenGone } from '@/components/layout';
import { useCardioSession } from '@/db/hooks';
import { replaceSession } from '@/navigation';
import { beginTimer, getView, pauseTimer, resumeTimer, setTimerSpeed, startTimer, stopTimer } from '@/timer/engine';
import { useTimer } from '@/timer/useTimer';

/**
 * The interval timer of a cardio session (TimerScreen.kt). The engine owns the clock; this screen shows it,
 * keeps the screen on, and moves on to the session form once the timer has written its time.
 */
export default function TimerScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  useKeepAwake();
  const { data } = useCardioSession(id);
  const { state, finished } = useTimer();
  useLeaveWhenGone(data != null && data.session == null);

  // Start this session's timer, or continue it. A saved session, or one with no interval exercise, belongs in its form.
  useEffect(() => {
    const session = data?.session;
    if (!session) return;
    const exercise = data.exercise;
    if (session.finishedAt != null || !exercise?.intervals) return void replaceSession(id, 'cardio');
    const view = getView();
    if (view.state?.sessionId !== id && view.finished?.sessionId !== id) startTimer(session, exercise);
  }, [data, id]);

  // The timer ended and wrote its time: continue in the form. The ended timer stays reported, so it never restarts here.
  useEffect(() => {
    if (finished?.sessionId === id) replaceSession(id, 'cardio');
  }, [finished, id]);

  const mine = state?.sessionId === id ? state : null;
  return (
    <TimerView
      state={mine}
      title={mine?.title ?? data?.exercise?.name ?? ''}
      onStop={() => void stopTimer()}
      onBegin={beginTimer}
      onPause={pauseTimer}
      onResume={resumeTimer}
      onSpeed={setTimerSpeed}
    />
  );
}
