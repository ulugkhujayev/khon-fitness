import { useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { EditorScreen } from '@/components/EditorScreen';
import { Sheet } from '@/components/Sheet';
import { BottomActions, Capsule, Chevron, Dot, FieldRow, GroupedList, InlineTextField, TextButton } from '@/components/ui';
import { PRESET_COLORS } from '@/domain/editors';
import { argbToHex, Modality } from '@/domain/types';
import * as repo from '@/db/repository';
import { newId } from '@/db/id';
import { TABLES as T } from '@/db/schema';
import { useLive } from '@/db/useLive';

/** Modality editor: name and one of twelve preset colors, archive at the bottom. No native color input. */
export default function ModalityEditor() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { data } = useLive(() => repo.modalities(), [T.modalities]);
  const original = data?.find((m) => m.id === id) ?? null;
  if (!data || (id !== 'new' && !original)) return <EditorScreen title="">{null}</EditorScreen>;
  return <ModalityForm key={id} original={original} />;
}

function ModalityForm({ original }: { original: Modality | null }) {
  const isNew = original === null;
  const [draft, setDraft] = useState<Modality>(() => original ?? { id: newId(), name: '', color: PRESET_COLORS[3], archived: false });
  const [picking, setPicking] = useState(false);

  return (
    <EditorScreen
      title={isNew ? 'New modality' : (original?.name ?? '')}
      trailing={
        <Capsule
          text="Done"
          onPress={() => {
            if (draft.name.trim()) repo.saveModality({ ...draft, name: draft.name.trim() });
            router.back();
          }}
        />
      }>
      <GroupedList>
        <FieldRow label="Name" divider={false}>
          <InlineTextField value={draft.name} onChange={(name) => setDraft({ ...draft, name })} placeholder="Bike, rowing…" label="Name" />
        </FieldRow>
        <FieldRow label="Color" onPress={() => setPicking(true)}>
          <Dot color={argbToHex(draft.color)} />
          <Chevron />
        </FieldRow>
      </GroupedList>
      {!isNew ? (
        <BottomActions>
          <TextButton
            text={draft.archived ? 'Unarchive' : 'Archive'}
            onPress={async () => {
              await repo.saveModality({ ...draft, archived: !draft.archived });
              router.back();
            }}
          />
        </BottomActions>
      ) : null}

      <Sheet visible={picking} title="Color" onDismiss={() => setPicking(false)}>
        {[PRESET_COLORS.slice(0, 6), PRESET_COLORS.slice(6)].map((row, r) => (
          <View key={r} style={styles.swatches}>
            {row.map((c) => (
              <Pressable
                key={c}
                onPress={() => {
                  setDraft({ ...draft, color: c });
                  setPicking(false);
                }}
                accessibilityRole="radio"
                accessibilityState={{ selected: draft.color === c }}
                accessibilityLabel={argbToHex(c)}
                style={[styles.swatch, draft.color === c && { backgroundColor: '#FFFFFF' }]}>
                <View style={[styles.swatchFill, { backgroundColor: argbToHex(c) }]} />
              </Pressable>
            ))}
          </View>
        ))}
      </Sheet>
    </EditorScreen>
  );
}

const styles = StyleSheet.create({
  swatches: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 8 },
  swatch: { width: 44, height: 44, borderRadius: 22, padding: 3 },
  swatchFill: { flex: 1, borderRadius: 19 },
});
