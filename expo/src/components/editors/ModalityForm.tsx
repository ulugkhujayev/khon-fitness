import { useState } from 'react';
import { EditorScreen } from '@/components/layout';
import { BottomActions, Capsule, Chevron, Dot, FieldRow, GroupedList, InlineTextField, TintedButton } from '@/components/ui';
import { PRESET_COLORS } from '@/domain/editors';
import { argbToHex, Modality } from '@/domain/types';
import { newId } from '@/db/id';
import { ColorSheet } from './ColorSheet';

/** Modality editor (ModalityEditorScreen in Editors.kt): name and a preset color; Archive at the bottom. */
export function ModalityForm({ original, onSave, onArchive }: { original: Modality | null; onSave: (m: Modality) => void; onArchive: (m: Modality) => void }) {
  const [draft, setDraft] = useState<Modality>(() => original ?? { id: newId(), name: '', color: PRESET_COLORS[3], archived: false });
  const [picking, setPicking] = useState(false);
  return (
    <EditorScreen title={original ? original.name : 'New modality'} trailing={<Capsule text="Done" onPress={() => onSave(draft)} />}>
      <GroupedList>
        <FieldRow label="Name" divider={false}>
          <InlineTextField value={draft.name} onChange={(name) => setDraft({ ...draft, name })} placeholder="Bike, rowing…" label="Name" />
        </FieldRow>
        <FieldRow label="Color" onPress={() => setPicking(true)}>
          <Dot color={argbToHex(draft.color)} />
          <Chevron />
        </FieldRow>
      </GroupedList>
      {original ? (
        <BottomActions>
          <TintedButton text={draft.archived ? 'Unarchive' : 'Archive'} onPress={() => onArchive(draft)} />
        </BottomActions>
      ) : null}
      <ColorSheet
        visible={picking}
        selected={draft.color}
        onPick={(color) => {
          setDraft({ ...draft, color });
          setPicking(false);
        }}
        onDismiss={() => setPicking(false)}
      />
    </EditorScreen>
  );
}
