import { StyleSheet } from 'react-native';
import { GroupedList, ListRow, PrimaryButton, Sheet } from '@/components/ui';
import { fmt } from '@/domain/format';
import { Summary } from '@/domain/workout';

/** Finish sheet (SessionScreen.kt): sets done, volume and new bests, then Save. */
export function SummarySheet({ visible, summary, onSave, onDismiss }: { visible: boolean; summary: Summary | null; onSave: () => void; onDismiss: () => void }) {
  return (
    <Sheet visible={visible} title="Workout done" onDismiss={onDismiss}>
      {summary ? (
        <GroupedList elevated>
          <ListRow title="Sets done" value={`${summary.done} of ${summary.total}`} chevron={false} />
          <ListRow title="Volume" value={`${fmt(summary.volume, 0)} kg`} chevron={false} />
          <ListRow title="New bests" value={String(summary.bests)} chevron={false} last />
        </GroupedList>
      ) : null}
      <PrimaryButton text="Save" onPress={onSave} style={styles.save} />
    </Sheet>
  );
}

const styles = StyleSheet.create({
  save: { marginTop: 16 },
});
