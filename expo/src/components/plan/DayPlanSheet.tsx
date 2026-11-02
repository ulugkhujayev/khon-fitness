import { StyleSheet } from 'react-native';
import { TextButton } from '@/components/ui';
import { longDate } from '@/domain/dates';
import { itemFor, itemId, PlanData } from '@/domain/plan';
import { Modality } from '@/domain/types';
import * as repo from '@/db/repository';
import { PlanItemSheet } from './PlanItemSheet';

/** Change what one date holds (PlanSheet in TodayScreen.kt): pick an item, or go back to the weekly template. */
export function DayPlanSheet({ visible, date, plan, modalities, onDismiss }: { visible: boolean; date: string; plan: PlanData; modalities: Modality[]; onDismiss: () => void }) {
  const current = itemFor(plan, date);
  return (
    <PlanItemSheet
      visible={visible}
      title={longDate(date)}
      plan={plan}
      modalities={modalities}
      current={{ itemType: current.itemType, itemId: itemId(current) }}
      onPick={({ itemType, itemId: id }) => {
        repo.saveOverride(date, itemType, id);
        onDismiss();
      }}
      onDismiss={onDismiss}
      footer={
        current.overridden ? (
          <TextButton
            text="Back to weekly template"
            style={styles.back}
            onPress={() => {
              repo.clearOverride(date);
              onDismiss();
            }}
          />
        ) : null
      }
    />
  );
}

const styles = StyleSheet.create({
  back: { alignSelf: 'flex-start', marginTop: 12 },
});
