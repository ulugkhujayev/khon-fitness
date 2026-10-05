import { Ref } from 'react';
import { Pressable, StyleSheet, Text, TextInput, useWindowDimensions, View } from 'react-native';
import { Check, COMPACT_WIDTH, NumberField } from '@/components/ui';
import { fmt } from '@/domain/format';
import { SetLog, SetStatus } from '@/domain/types';
import { gain, kgDecimals, setTag } from '@/domain/workout';
import { Font, K, Space, tabular, Type } from '@/theme';
import { SetDelta } from './SetDelta';

/** Room around the two widgets: screen margins (32), set padding (26) and the gap between them (10). */
const ROW_CHROME = 68;

/**
 * Set row (DESIGN.md, Set row): check circle, tag, the full exercise name (tap to swap), last time on the
 * right; under it the kg and reps widgets with the gain since last time. Read-only once a session is done.
 */
export function SetRow({
  log,
  prev,
  live,
  inputRef,
  onToggle,
  onWeight,
  onReps,
  onSwap,
}: {
  log: SetLog;
  /** The same set last time, for the hint, the placeholders and the gains. */
  prev: SetLog | undefined;
  /** Editable: a running session, or a finished one in Edit. */
  live: boolean;
  inputRef: Ref<TextInput>;
  onToggle: () => void;
  onWeight: (kg: number | null) => void;
  onReps: (reps: number | null) => void;
  onSwap: () => void;
}) {
  const { width } = useWindowDimensions();
  const compact = (width - ROW_CHROME) / 2 < COMPACT_WIDTH;
  const tag = setTag(log);
  const isDone = log.status === SetStatus.DONE;
  const decimals = kgDecimals(log.stepKg, log.weightKg, prev?.weightKg);
  // Rows saved before sets started at 0 kg may hold no weight; they read as 0.
  const prevKg = fmt(prev?.weightKg ?? 0, decimals);
  const prevReps = prev?.reps ?? 0;

  return (
    <View style={[styles.row, isDone && styles.doneRow]}>
      <View style={styles.line}>
        <Pressable
          onPress={onToggle}
          disabled={!live}
          accessibilityRole="checkbox"
          accessibilityState={{ checked: isDone, disabled: !live }}
          accessibilityLabel={`${tag} ${log.exerciseName}`}
          style={styles.checkTarget}>
          <View style={[styles.check, isDone ? styles.checkDone : styles.checkOpen]}>{isDone ? <Check color={K.GreenInk} size={20} /> : null}</View>
        </Pressable>
        <Text style={styles.tag}>{tag}</Text>
        <Pressable onPress={onSwap} disabled={!live} accessibilityRole={live ? 'button' : undefined} accessibilityHint={live ? 'Swap exercise' : undefined} style={styles.name}>
          <Text style={styles.nameText}>{log.exerciseName}</Text>
          <Text style={styles.range}>{`${log.minReps}–${log.maxReps} reps`}</Text>
        </Pressable>
        {prev ? (
          <Text style={styles.prev} accessibilityLabel={`Last time ${prevKg} kilograms, ${prevReps} reps`}>
            {`${prevKg} × ${prevReps}`}
          </Text>
        ) : null}
      </View>
      {live ? (
        <View style={styles.widgets}>
          <View style={styles.widget}>
            <NumberField
              value={log.weightKg}
              onChange={onWeight}
              step={log.stepKg > 0 ? log.stepKg : 1}
              unit="kg"
              decimals={decimals}
              inputRef={inputRef}
              label={`${tag} weight`}
              placeholder={prev ? prevKg : '0'}
              compact={compact}
            />
            <SetDelta gain={gain(prev?.weightKg, log.weightKg, decimals, 'kg')} unit={compact ? 'kg' : null} />
          </View>
          <View style={styles.widget}>
            <NumberField
              value={log.reps}
              onChange={onReps}
              step={1}
              unit="reps"
              decimals={0}
              label={`${tag} reps`}
              placeholder={String(prev?.reps ?? log.minReps)}
              compact={compact}
            />
            <SetDelta gain={gain(prev?.reps, log.reps, 0, 'reps')} unit={compact ? 'reps' : null} />
          </View>
        </View>
      ) : (
        <Text style={[styles.result, log.status === SetStatus.SKIPPED && styles.skipped]}>
          {log.status === SetStatus.SKIPPED ? 'skipped' : `${fmt(log.weightKg ?? 0, decimals)} kg × ${log.reps ?? 0} reps`}
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { paddingLeft: 10, paddingRight: 16, paddingTop: 8, paddingBottom: 12, borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: K.Divider },
  doneRow: { backgroundColor: K.GreenTint },
  line: { flexDirection: 'row', alignItems: 'center' },
  checkTarget: { width: Space.tapMin, height: Space.tapMin, alignItems: 'center', justifyContent: 'center', marginRight: 8 },
  check: { width: 34, height: 34, borderRadius: 17, alignItems: 'center', justifyContent: 'center' },
  checkDone: { backgroundColor: K.Green },
  checkOpen: { borderWidth: 2, borderColor: K.Surface3 },
  tag: { ...Type.caption1, fontFamily: Font.bold, color: K.Accent, marginRight: 8 },
  name: { flex: 1 },
  nameText: { ...Type.body, fontFamily: Font.semibold },
  range: { ...Type.caption1, color: K.Dim },
  prev: { ...Type.subheadline, color: K.Muted, marginLeft: 8, ...tabular },
  widgets: { flexDirection: 'row', gap: 10, marginTop: 10 },
  widget: { flex: 1 },
  // Under the name: past the 44 pt check target, less the row inset.
  result: { ...Type.subheadline, paddingLeft: 42, marginTop: 4 },
  skipped: { color: K.Dim },
});
