import { GroupedList, ListRow, TextButton } from '@/components/ui';
import { RoutineStretch, Stretch } from '@/domain/types';
import { K } from '@/theme';

/** The routine's stretches in order, each with up, down and Remove (the Order group of RoutineEditorScreen). */
export function RoutineOrderList({
  entries,
  stretches,
  onMove,
  onRemove,
}: {
  /** In order. */
  entries: RoutineStretch[];
  stretches: Stretch[];
  onMove: (index: number, dir: -1 | 1) => void;
  onRemove: (entry: RoutineStretch) => void;
}) {
  return (
    <GroupedList>
      {entries.map((rs, i) => {
        const name = stretches.find((s) => s.id === rs.stretchId)?.name ?? 'Missing stretch';
        return (
          <ListRow
            key={rs.id}
            title={name}
            chevron={false}
            last={i === entries.length - 1}
            trailing={
              <>
                <TextButton text="↑" label={`Move ${name} up`} enabled={i > 0} onPress={() => onMove(i, -1)} />
                <TextButton text="↓" label={`Move ${name} down`} enabled={i < entries.length - 1} onPress={() => onMove(i, 1)} />
                <TextButton text="Remove" label={`Remove ${name}`} color={K.Red} onPress={() => onRemove(rs)} />
              </>
            }
          />
        );
      })}
    </GroupedList>
  );
}
