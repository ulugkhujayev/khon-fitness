import { useState } from 'react';
import { Linking, StyleSheet, Text } from 'react-native';
import { EditorScreen } from '@/components/layout';
import {
  BottomActions, Capsule, ChoiceSheet, ConfirmSheet, FieldRow, FieldValue, GroupedList, InlineTextField, ListRow, NumberRow, TextButton,
  ToggleRow,
} from '@/components/ui';
import { clampReps, clampStretchSeconds, figureName, FIGURES, newStretch, withRepsMode } from '@/domain/stretch';
import { Stretch, StretchMode } from '@/domain/types';
import { newId } from '@/db/id';
import { K, Type } from '@/theme';
import { StretchDemonstration } from './StretchDemonstration';

const GUIDES = Object.keys(FIGURES).map((id) => ({ id, title: FIGURES[id] }));

/**
 * Stretch editor (StretchEditorScreen in StretchEditors.kt): the guide's lesson on top, then name, guide, sides,
 * hold or reps, seconds, muscles, cue and a reference link. Edits a draft; Done hands it back.
 * Built-in stretches and ones a routine uses cannot be deleted.
 */
export function StretchForm({
  original,
  useCount,
  onSave,
  onDelete,
}: {
  /** Null for a new stretch. */
  original: Stretch | null;
  /** Routine rows that use it. */
  useCount: number;
  onSave: (draft: Stretch) => void;
  onDelete: (id: string) => void;
}) {
  const [draft, setDraft] = useState<Stretch>(() => original ?? newStretch(newId()));
  const [guideOpen, setGuideOpen] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const reps = draft.mode === StretchMode.REPS;
  const set = (patch: Partial<Stretch>) => setDraft({ ...draft, ...patch });

  return (
    <EditorScreen title={original ? 'Stretch' : 'New stretch'} trailing={<Capsule text="Done" onPress={() => onSave(draft)} />}>
      <StretchDemonstration figure={draft.figure} compact style={styles.demo} />
      <GroupedList>
        <FieldRow label="Name" divider={false}>
          <InlineTextField value={draft.name} onChange={(name) => set({ name })} placeholder="Stretch name" label="Name" />
        </FieldRow>
        <FieldRow label="Guide" onPress={() => setGuideOpen(true)}>
          <FieldValue text={figureName(draft.figure)} />
        </FieldRow>
        <ToggleRow label="Per side" value={draft.sided} onChange={(sided) => set({ sided })} />
        <ToggleRow label="Reps" value={reps} onChange={(on) => setDraft(withRepsMode(draft, on))} />
        {reps ? <NumberRow label="Rep count" value={draft.reps} unit="" step={1} decimals={0} onChange={(v) => set({ reps: clampReps(v) })} /> : null}
        <NumberRow label={reps ? 'Time' : 'Hold'} value={draft.seconds} unit="s" step={5} decimals={0} onChange={(v) => set({ seconds: clampStretchSeconds(v) })} />
        <FieldRow label="Muscles">
          <InlineTextField value={draft.muscles} onChange={(muscles) => set({ muscles })} placeholder="hamstrings, glutes" label="Muscles" />
        </FieldRow>
        <FieldRow label="Cue">
          <InlineTextField value={draft.cue} onChange={(cue) => set({ cue })} placeholder="How to do it" label="Cue" />
        </FieldRow>
        <FieldRow label="Link">
          <InlineTextField value={draft.link} onChange={(link) => set({ link })} placeholder="YouTube URL" label="Link" />
        </FieldRow>
      </GroupedList>
      {draft.link.trim() ? (
        <GroupedList style={styles.link}>
          <ListRow title="Watch the reference" last onPress={() => void Linking.openURL(draft.link.trim()).catch(() => undefined)} />
        </GroupedList>
      ) : null}
      {original ? (
        <>
          <BottomActions>
            <TextButton text="Delete stretch" color={K.Red} enabled={!original.builtin && useCount === 0} onPress={() => setConfirming(true)} />
          </BottomActions>
          {original.builtin || useCount > 0 ? (
            <Text style={styles.reason}>{original.builtin ? 'Built-in stretches cannot be deleted.' : `In use by ${useCount} routine rows.`}</Text>
          ) : null}
        </>
      ) : null}
      <ChoiceSheet
        visible={guideOpen}
        title="Guide"
        choices={GUIDES}
        isSelected={(id) => id === draft.figure}
        onPick={(figure) => {
          set({ figure });
          setGuideOpen(false);
        }}
        onDismiss={() => setGuideOpen(false)}
      />
      <ConfirmSheet
        visible={confirming}
        title="Delete this stretch?"
        body=""
        action="Delete"
        onDismiss={() => setConfirming(false)}
        onConfirm={() => {
          setConfirming(false);
          onDelete(draft.id);
        }}
      />
    </EditorScreen>
  );
}

const styles = StyleSheet.create({
  demo: { marginBottom: 12 },
  link: { marginTop: 16 },
  reason: { ...Type.footnote, color: K.Dim, marginTop: 6, paddingLeft: 2 },
});
