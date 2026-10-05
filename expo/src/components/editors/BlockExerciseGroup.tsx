import { StyleSheet, View } from 'react-native';
import { FieldRow, FieldValue, GroupedList, NumberRow, SectionTitle, TextButton } from '@/components/ui';
import { withMaxReps, withMinReps, withSets } from '@/domain/editors';
import { BlockExercise } from '@/domain/types';
import { K } from '@/theme';

type RepsPatch = Partial<Pick<BlockExercise, 'sets' | 'minReps' | 'maxReps'>>;

/** One exercise of a block (BlockEditorScreen in Editors.kt): A or B with Remove, the exercise, sets and rep range. */
export function BlockExerciseGroup({
  be,
  exerciseName,
  onChoose,
  onRemove,
  onChange,
}: {
  be: BlockExercise;
  /** Null when the exercise is gone from the library. */
  exerciseName: string | null;
  onChoose: () => void;
  onRemove: () => void;
  onChange: (patch: RepsPatch) => void;
}) {
  const letter = String.fromCharCode(65 + be.slot);
  return (
    <View>
      <SectionTitle text={letter} style={styles.title} trailing={<TextButton text="Remove" color={K.Red} label={`Remove ${letter}`} onPress={onRemove} />} />
      <GroupedList>
        <FieldRow label="Exercise" divider={false} onPress={onChoose}>
          <FieldValue text={exerciseName ?? 'Choose'} />
        </FieldRow>
        <NumberRow label="Sets" value={be.sets} unit="" step={1} decimals={0} onChange={(v) => onChange({ sets: withSets(be, v).sets })} />
        <NumberRow
          label="Min reps"
          value={be.minReps}
          unit=""
          step={1}
          decimals={0}
          onChange={(v) => {
            const { minReps, maxReps } = withMinReps(be, v);
            onChange({ minReps, maxReps });
          }}
        />
        <NumberRow
          label="Max reps"
          value={be.maxReps}
          unit=""
          step={1}
          decimals={0}
          onChange={(v) => {
            const { minReps, maxReps } = withMaxReps(be, v);
            onChange({ minReps, maxReps });
          }}
        />
      </GroupedList>
    </View>
  );
}

const styles = StyleSheet.create({
  title: { marginTop: 20 },
});
