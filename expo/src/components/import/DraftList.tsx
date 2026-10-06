import { StyleSheet } from 'react-native';
import { exerciseColor } from '@/components/plan/colors';
import { GroupedList, GroupFooter, PrimaryButton, SectionTitle } from '@/components/ui';
import { ImportDraft, importAllLabel, pendingLine } from '@/domain/health';
import { Exercise, Modality } from '@/domain/types';
import { K } from '@/theme';
import { DraftRow } from './DraftRow';

/**
 * The band sessions waiting for import, each with the exercise it goes under (ImportScreen.kt), under a title
 * that counts them. A row opens the picker; Import all brings in every matched one.
 */
export function DraftList({
  drafts,
  exerciseFor,
  modalities,
  onPick,
  onImportAll,
}: {
  drafts: ImportDraft[];
  exerciseFor: (d: ImportDraft) => Exercise | null;
  modalities: Modality[];
  onPick: (d: ImportDraft) => void;
  onImportAll: (ready: [ImportDraft, Exercise][]) => void;
}) {
  const ready = drafts.flatMap((d): [ImportDraft, Exercise][] => {
    const ex = exerciseFor(d);
    return ex ? [[d, ex]] : [];
  });
  const all = ready.length === drafts.length;
  return (
    <>
      <SectionTitle text={pendingLine(drafts.length)} style={styles.section} />
      <GroupedList>
        {drafts.map((d, i) => {
          const ex = exerciseFor(d);
          return (
            <DraftRow
              key={d.sourceId}
              draft={d}
              exerciseName={ex?.name ?? null}
              color={ex ? exerciseColor(ex, modalities) : K.Dim}
              last={i === drafts.length - 1}
              onPress={() => onPick(d)}
            />
          );
        })}
      </GroupedList>
      <GroupFooter
        text={
          all
            ? 'Each one becomes a session under the exercise on its right. Tap a row to pick a different one first.'
            : 'The ones marked Choose match no exercise yet: tap one to pick it. The rest import under the exercise shown.'
        }
      />
      {ready.length > 0 ? (
        <PrimaryButton text={importAllLabel(ready.length, drafts.length)} onPress={() => onImportAll(ready)} style={styles.action} />
      ) : null}
    </>
  );
}

const styles = StyleSheet.create({
  section: { marginTop: 24 },
  action: { marginTop: 20 },
});
