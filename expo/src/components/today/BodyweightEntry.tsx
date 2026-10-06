import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { FigureField, RulerPicker, TextButton } from '@/components/ui';
import { BODYWEIGHT_RULER, changeSince, entryAction, startKg } from '@/domain/bodyweight';
import { Bodyweight } from '@/domain/types';
import { K, tabular, Type } from '@/theme';

/**
 * Today's bodyweight entry: the picked value as a big figure (tap to type), its change since the last
 * day weighed, a ruler to drag, and Add, or Update when today already has an entry. The ruler starts at
 * the latest entry and stays on the saved value after a save.
 */
export function BodyweightEntry({ today, bodyweights, onSave }: { today: string; bodyweights: Bodyweight[]; onSave: (kg: number) => void }) {
  const [kg, setKg] = useState(() => startKg(bodyweights));
  const change = changeSince(bodyweights, today, kg);
  const action = entryAction(bodyweights, today, kg);

  return (
    <View style={styles.entry}>
      <FigureField value={kg} onChange={setKg} unit="kg" label="Bodyweight in kilograms" />
      {change ? <Text style={styles.change}>{change}</Text> : null}
      <RulerPicker ruler={BODYWEIGHT_RULER} value={kg} onChange={setKg} label="Bodyweight ruler" unit="kg" style={styles.ruler} />
      <TextButton
        text={action.text}
        label={action.text === 'Add' ? 'Add bodyweight' : "Update today's bodyweight"}
        enabled={action.enabled}
        onPress={() => onSave(kg)}
        style={styles.save}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  entry: { marginTop: 20, alignItems: 'stretch' },
  change: { ...Type.subheadline, ...tabular, color: K.Muted, textAlign: 'center', marginTop: 2 },
  ruler: { marginTop: 16 },
  save: { alignSelf: 'center', marginTop: 8 },
});
