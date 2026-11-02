import { StyleSheet } from 'react-native';
import { Empty, GroupedList, NumberRow, SectionTitle, TintedButton } from '@/components/ui';
import { clampStretchSeconds, entrySeconds } from '@/domain/stretch';
import { RoutineStretch, Stretch } from '@/domain/types';
import { RoutineOrderList } from './RoutineOrderList';

/**
 * A routine's stretches (RoutineEditorScreen): each with its seconds, then Add stretch, then their order
 * with up, down and remove.
 */
export function RoutineStretches({
  entries,
  stretches,
  onSeconds,
  onAdd,
  onMove,
  onRemove,
}: {
  /** In order. */
  entries: RoutineStretch[];
  stretches: Stretch[];
  onSeconds: (entry: RoutineStretch, seconds: number) => void;
  onAdd: () => void;
  onMove: (index: number, dir: -1 | 1) => void;
  onRemove: (entry: RoutineStretch) => void;
}) {
  return (
    <>
      <SectionTitle text="Stretches" style={styles.section} />
      <GroupedList>
        {entries.map((rs, i) => {
          const st = stretches.find((s) => s.id === rs.stretchId);
          return (
            <NumberRow
              key={rs.id}
              label={st?.name ?? 'Missing stretch'}
              note={st?.sided ? 'per side' : undefined}
              value={entrySeconds(rs, st)}
              unit="s"
              step={5}
              decimals={0}
              divider={i > 0}
              onChange={(v) => onSeconds(rs, clampStretchSeconds(v))}
            />
          );
        })}
        {entries.length === 0 ? <Empty text="No stretches yet" /> : null}
      </GroupedList>
      <TintedButton text="Add stretch" onPress={onAdd} style={styles.add} />
      {entries.length > 0 ? (
        <>
          <SectionTitle text="Order" style={styles.section} />
          <RoutineOrderList entries={entries} stretches={stretches} onMove={onMove} onRemove={onRemove} />
        </>
      ) : null}
    </>
  );
}

const styles = StyleSheet.create({
  section: { marginTop: 24 },
  add: { marginTop: 12 },
});
