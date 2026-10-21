import { StyleSheet, Text } from 'react-native';
import { exerciseColor } from '@/components/plan/colors';
import { ChoiceSheet } from '@/components/ui';
import { draftDate, draftDetail, ImportDraft } from '@/domain/health';
import { shortDate } from '@/domain/dates';
import { Exercise, Intensity, Modality } from '@/domain/types';
import { K, Type } from '@/theme';

/** Import one band session under a cardio exercise of the user's choice (the picker sheet of ImportScreen.kt). */
export function DraftSheet({
  visible,
  draft,
  current,
  cardio,
  modalities,
  onPick,
  onDismiss,
}: {
  visible: boolean;
  /** Kept while the sheet slides out. */
  draft: ImportDraft | null;
  /** The exercise it would import under now. */
  current: Exercise | null;
  cardio: Exercise[];
  modalities: Modality[];
  onPick: (ex: Exercise) => void;
  onDismiss: () => void;
}) {
  return (
    <ChoiceSheet
      visible={visible}
      title={draft ? `${shortDate(draftDate(draft))} · ${draft.typeName}` : ''}
      header={draft ? <Text style={styles.detail}>{draftDetail(draft)}</Text> : null}
      choices={cardio.map((e) => ({ id: e.id, title: e.name, dot: exerciseColor(e, modalities), dotFilled: e.intensity === Intensity.HIGH }))}
      isSelected={(id) => id === current?.id}
      onPick={(id) => {
        const ex = cardio.find((e) => e.id === id);
        if (ex) onPick(ex);
      }}
      onDismiss={onDismiss}
    />
  );
}

const styles = StyleSheet.create({
  detail: { ...Type.subheadline, color: K.Muted },
});
