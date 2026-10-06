import { ScrollView, StyleSheet, Text, useWindowDimensions, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { PrimaryButton, TextButton } from '@/components/ui';
import { playerAction, playerProgress, playerStatus, repMotion, sideLine } from '@/domain/stretch';
import { countdownText, currentPhase, isActive, isDone, TimerState } from '@/domain/timer';
import { K, Space, Type } from '@/theme';
import { PlayerButton } from './PlayerButton';
import { RoutineProgress } from './RoutineProgress';
import { StretchDemonstration } from './StretchDemonstration';

/** Under this height the demonstration and the countdown shrink (StretchScreen.kt: 680 dp). */
const SHORT_SCREEN = 680;

/**
 * Full-screen stretch player (StretchScreen.kt, DESIGN.md Stretch player): round Stop and Skip on top, the run's
 * progress under them, the stretch, its demonstration, the state and countdown in a scroll, one white button
 * fixed at the bottom.
 * Black while ready, preparing or paused; green while stretching.
 */
export function StretchPlayerView({
  state,
  title,
  emptyText,
  onStop,
  onSkip,
  onBegin,
  onPause,
  onResume,
  onSpeed,
}: {
  /** This routine's run, or null before it starts. */
  state: TimerState | null;
  title: string;
  /** Shown in place of the stretch when there is none to play. */
  emptyText: string;
  onStop: () => void;
  onSkip: () => void;
  onBegin: () => void;
  onPause: () => void;
  onResume: () => void;
  /** Development builds only: run twenty times faster to test a routine quickly. */
  onSpeed?: (speed: number) => void;
}) {
  const { height } = useWindowDimensions();
  const compact = height < SHORT_SCREEN;
  const p = state && !isDone(state) ? currentPhase(state) : null;
  const action = !state || !p ? null : state.awaitingStart ? onBegin : state.paused ? onResume : onPause;

  return (
    <SafeAreaView style={[styles.screen, { backgroundColor: state && isActive(state) ? K.PhaseWork : K.Bg }]} edges={['top', 'bottom']}>
      <View style={styles.bar}>
        <View style={styles.side}>
          <PlayerButton icon={{ ios: 'xmark', android: 'close', web: 'close' }} label="Stop" onPress={onStop} />
        </View>
        <Text style={styles.title} numberOfLines={1} accessibilityRole="header">
          {title}
        </Text>
        <View style={[styles.side, styles.trailing]}>
          <PlayerButton icon={{ ios: 'forward.end.fill', android: 'skip_next', web: 'skip_next' }} label="Skip" enabled={p != null} onPress={onSkip} />
        </View>
      </View>
      {state && p ? <RoutineProgress count={state.phases.length} index={state.index} text={playerProgress(state)} /> : null}
      <ScrollView style={styles.scroll} contentContainerStyle={styles.middle} showsVerticalScrollIndicator={false}>
        {state && p ? (
          <>
            <Text style={styles.name}>{p.label}</Text>
            <Text style={styles.line}>{sideLine(p)}</Text>
            <StretchDemonstration
              // Each stretch and side opens with its own lesson playing.
              key={`${state.sessionId}-${state.index}`}
              figure={p.figure}
              mirror={p.side === 'Right'}
              preview={state.awaitingStart}
              exerciseProgress={p.reps > 0 ? repMotion(p, state.remaining) : null}
              compact={compact}
              style={styles.demo}
            />
            <Text style={styles.line} accessibilityLiveRegion="polite">
              {playerStatus(state, p)}
            </Text>
            {!state.awaitingStart ? (
              <Text style={[compact ? Type.stretchCountdownCompact : Type.stretchCountdown, styles.countdown]} accessibilityRole="timer">
                {countdownText(state)}
              </Text>
            ) : null}
          </>
        ) : (
          <Text style={styles.empty}>{emptyText}</Text>
        )}
      </ScrollView>
      {state && p && action ? (
        <PrimaryButton text={playerAction(state, p)} onPress={action} color={K.White} textColor={K.Black} style={styles.action} />
      ) : null}
      {__DEV__ && onSpeed && p && state ? (
        <TextButton text={state.speed > 1 ? '×20 on' : '×20'} color={K.White} onPress={() => onSpeed(state.speed > 1 ? 1 : 20)} style={styles.speed} />
      ) : null}
      <View style={styles.bottom} />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, paddingHorizontal: Space.margin },
  bar: { flexDirection: 'row', alignItems: 'center', minHeight: 56 },
  // Equal 64 pt sides keep the title centered (DESIGN.md, Stretch player).
  side: { width: 64, alignItems: 'flex-start' },
  trailing: { alignItems: 'flex-end' },
  title: { ...Type.headline, flex: 1, textAlign: 'center' },
  scroll: { flex: 1 },
  middle: { flexGrow: 1, alignItems: 'center', justifyContent: 'center', paddingVertical: 16 },
  name: { ...Type.title2, textAlign: 'center', alignSelf: 'stretch' },
  line: { ...Type.body, color: K.OnPhaseMuted, marginTop: 8 },
  demo: { alignSelf: 'stretch', marginTop: 12 },
  countdown: { textAlign: 'center', alignSelf: 'stretch', marginVertical: 8 },
  empty: { ...Type.body, textAlign: 'center' },
  // 56 pt with 16 pt corners (DESIGN.md: a 56 dp full-width Start button).
  action: { height: 56, borderRadius: 16 },
  speed: { alignSelf: 'center' },
  bottom: { height: 16 },
});
