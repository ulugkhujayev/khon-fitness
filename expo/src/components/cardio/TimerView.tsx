import { StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { PrimaryButton, TextButton } from '@/components/ui';
import { countdownText, elapsedLine, isDone, nextLine, PhaseTone, phaseTone, roundLine, timerLabel, TimerState } from '@/domain/timer';
import { Font, K, Space, Type } from '@/theme';

const BACKGROUND: Record<PhaseTone, string> = { idle: K.Bg, warmup: K.PhaseWarmup, work: K.PhaseWork, rest: K.PhaseRest };

/**
 * Full-screen interval countdown (TimerScreen.kt, SPEC.md Interval timer): the phase in color, the big
 * countdown, round, what comes next and the time so far; Start timer, Pause or Resume at the bottom.
 */
export function TimerView({
  state,
  title,
  onStop,
  onBegin,
  onPause,
  onResume,
  onSpeed,
}: {
  state: TimerState | null;
  title: string;
  onStop: () => void;
  onBegin: () => void;
  onPause: () => void;
  onResume: () => void;
  /** Development builds only: run the timer sixty times faster to test a workout in seconds. */
  onSpeed?: (speed: number) => void;
}) {
  const running = state != null && !isDone(state);
  const round = state && running ? roundLine(state) : null;
  const action = !state ? null : state.awaitingStart ? { text: 'Start timer', run: onBegin } : state.paused ? { text: 'Resume', run: onResume } : { text: 'Pause', run: onPause };
  return (
    <SafeAreaView style={[styles.screen, { backgroundColor: BACKGROUND[phaseTone(state)] }]} edges={['top', 'bottom']}>
      <View style={styles.bar}>
        <TextButton text="Stop" color={K.White} onPress={onStop} style={styles.side} />
        <Text style={styles.title} numberOfLines={1} accessibilityRole="header">
          {title}
        </Text>
        <View style={styles.side} />
      </View>
      <View style={styles.middle} accessibilityLiveRegion="polite">
        <Text style={styles.label}>{timerLabel(state)}</Text>
        <Text style={styles.countdown} accessibilityRole="timer">
          {countdownText(state)}
        </Text>
        {round ? <Text style={styles.round}>{round}</Text> : null}
        {state && running ? (
          <>
            <Text style={styles.next}>{nextLine(state)}</Text>
            <Text style={styles.elapsed}>{elapsedLine(state)}</Text>
          </>
        ) : null}
      </View>
      {running && action ? <PrimaryButton text={action.text} onPress={action.run} /> : null}
      {__DEV__ && onSpeed && state ? (
        <TextButton text={state.speed > 1 ? '×60 on' : '×60'} color={K.White} onPress={() => onSpeed(state.speed > 1 ? 1 : 60)} style={styles.speed} />
      ) : null}
      <View style={styles.bottom} />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, paddingHorizontal: Space.margin },
  bar: { flexDirection: 'row', alignItems: 'center', paddingTop: 8 },
  // Equal sides keep the title centered (DESIGN.md, Stretch player: 64 pt controls).
  side: { width: 64 },
  title: { ...Type.headline, flex: 1, textAlign: 'center', color: K.White },
  middle: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  label: { ...Type.headline, color: K.OnPhaseMuted },
  countdown: { ...Type.countdown, color: K.White, marginVertical: 8 },
  round: { ...Type.round, color: K.White, fontFamily: Font.semibold },
  next: { ...Type.body, color: K.OnPhaseMuted, marginTop: 10 },
  elapsed: { ...Type.subheadline, color: K.OnPhaseDim, marginTop: 4 },
  speed: { alignSelf: 'center' },
  bottom: { height: 32 },
});
