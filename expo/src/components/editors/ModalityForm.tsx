import { useState } from 'react';
import { StyleSheet } from 'react-native';
import { EditorScreen } from '@/components/layout';
import { ACTION_ICONS, ActionRow, BottomActions, Capsule, FieldRow, GroupedList, InlineTextField, SectionTitle } from '@/components/ui';
import { PRESET_COLORS } from '@/domain/editors';
import { Modality } from '@/domain/types';
import { newId } from '@/db/id';
import { ColorGrid } from './ColorGrid';

/**
 * Modality editor (ModalityEditorScreen in Editors.kt): the name, its color picked right on the page from the
 * presets, and Archive at the bottom with what archiving does.
 */
export function ModalityForm({ original, onSave, onArchive }: { original: Modality | null; onSave: (m: Modality) => void; onArchive: (m: Modality) => void }) {
  const [draft, setDraft] = useState<Modality>(() => original ?? { id: newId(), name: '', color: PRESET_COLORS[3], archived: false });
  return (
    <EditorScreen title={original ? original.name : 'New modality'} trailing={<Capsule text="Done" onPress={() => onSave(draft)} />}>
      <GroupedList>
        <FieldRow label="Name" divider={false}>
          <InlineTextField value={draft.name} onChange={(name) => setDraft({ ...draft, name })} placeholder="Bike, rowing…" label="Name" />
        </FieldRow>
      </GroupedList>
      <SectionTitle text="Color" style={styles.section} />
      <ColorGrid selected={draft.color} onPick={(color) => setDraft({ ...draft, color })} />
      {original ? (
        <BottomActions note="Archived, it leaves the pickers; past sessions keep it and its color.">
          <GroupedList>
            <ActionRow
              icon={draft.archived ? ACTION_ICONS.unarchive : ACTION_ICONS.archive}
              title={draft.archived ? 'Unarchive' : 'Archive'}
              last
              onPress={() => onArchive(draft)}
            />
          </GroupedList>
        </BottomActions>
      ) : null}
    </EditorScreen>
  );
}

const styles = StyleSheet.create({
  section: { marginTop: 24 },
});
