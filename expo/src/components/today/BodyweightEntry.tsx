import { useState } from 'react';
import { StyleSheet, Text } from 'react-native';
import { FigureField, GroupedList, RulerPicker, TintedButton } from '@/components/ui';
import { BODYWEIGHT_RULER, changeSince, entryAction, startKg } from '@/domain/bodyweight';
import { Bodyweight } from '@/domain/types';
import { K, Space, tabular, Type } from '@/theme';

/** What a screen reader says for each state of the button. */
const SPOKEN = { Add: 'Add bodyweight', Update: "Update today's bodyweight", Saved: "Today's bodyweight is saved" } as const;

/**
 * Today's bodyweight entry on its own card, apart from the chart above: the picked value as a big figure
 * (tap to type), its change since the last day weighed, a ruler to drag, and a tinted Add, Update or
 * Saved. The ruler starts at the latest entry and stays on the saved value after a save.
 */
export function BodyweightEntry({ today, bodyweights, onSave }: { today: string; bodyweights: Bodyweight[]; onSave: (kg: number) => void }) {
  const [kg, setKg] = useState(() => startKg(bodyweights));
  const change = changeSince(bodyweights, today, kg);
  const action = entryAction(bodyweights, today, kg);

  return (
    <GroupedList style={styles.card}>
      <FigureField value={kg} onChange={setKg} unit="kg" label="Bodyweight in kilograms" />
      {change ? <Text style={styles.change}>{change}</Text> : null}
      <RulerPicker ruler={BODYWEIGHT_RULER} value={kg} onChange={setKg} label="Bodyweight ruler" unit="kg" surface={K.Surface} style={styles.ruler} />
      <TintedButton text={action.text} label={SPOKEN[action.text]} enabled={action.enabled} onPress={() => onSave(kg)} style={styles.save} />
    </GroupedList>
  );
}

const styles = StyleSheet.create({
  card: { marginTop: 24, paddingTop: 20, paddingBottom: Space.margin, paddingHorizontal: Space.margin },
  change: { ...Type.subheadline, ...tabular, color: K.Muted, textAlign: 'center', marginTop: 2 },
  // The ruler runs to the card's edges and fades into it.
  ruler: { marginTop: 16, marginHorizontal: -Space.margin },
  save: { marginTop: 16 },
});
