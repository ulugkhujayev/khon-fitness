import { StyleSheet, Text } from 'react-native';
import { exerciseColor } from '@/components/plan/colors';
import { GroupedList, ListRow, PrimaryButton } from '@/components/ui';
import { draftLine, draftTitle, ImportDraft } from '@/domain/health';
import { Exercise, Intensity, Modality } from '@/domain/types';
import { K, Type } from '@/theme';

/**
 * The band sessions waiting for import, each with the exercise it goes under (ImportScreen.kt). A row opens the
 * picker; Import all brings in every matched one.
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
      <GroupedList>
        {drafts.map((d, i) => {
          const ex = exerciseFor(d);
          return (
            <ListRow
              key={d.sourceId}
              title={draftTitle(d)}
              value={draftLine(d, ex != null)}
              dot={ex ? exerciseColor(ex, modalities) : K.Dim}
              dotFilled={ex?.intensity === Intensity.HIGH}
              last={i === drafts.length - 1}
              onPress={() => onPick(d)}
            />
          );
        })}
      </GroupedList>
      {ready.length > 0 ? (
        <PrimaryButton text={all ? `Import all ${drafts.length}` : `Import ${ready.length} matched`} onPress={() => onImportAll(ready)} style={styles.action} />
      ) : null}
      <Text style={styles.note}>
        {all
          ? 'Each one becomes a session under the exercise shown. Tap a row to pick a different exercise first.'
          : 'Sessions marked "tap to choose" have no matching exercise; tap one to pick it. The rest import under the exercise shown.'}
      </Text>
    </>
  );
}

const styles = StyleSheet.create({
  action: { marginTop: 16 },
  note: { ...Type.footnote, color: K.Dim, marginTop: 10 },
});
