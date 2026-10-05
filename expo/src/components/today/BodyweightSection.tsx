import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { LineChart, NumberField, TextButton } from '@/components/ui';
import { addDays } from '@/domain/dates';
import { fmt } from '@/domain/format';
import { Bodyweight } from '@/domain/types';
import { tabular, Type } from '@/theme';

/** The chart covers the last 30 days, today included. */
const CHART_DAYS = 30;

/**
 * Bodyweight on Today (DESIGN.md, Today): the latest value, a chart of the last 30 days, and a number
 * widget with Add. The field starts over empty after each Add.
 */
export function BodyweightSection({ today, bodyweights, onAdd }: { today: string; bodyweights: Bodyweight[]; onAdd: (kg: number) => void }) {
  const [draft, setDraft] = useState<number | null>(null);
  // A new key remounts the field, so it drops the typed text even while it still has focus.
  const [fieldKey, setFieldKey] = useState(0);
  const latest = bodyweights.at(-1);
  const from = addDays(today, 1 - CHART_DAYS);
  const recent = bodyweights.filter((b) => b.date >= from);
  const canAdd = draft != null && draft > 0;

  return (
    <View>
      <View style={styles.header}>
        <Text style={styles.title}>Bodyweight</Text>
        <Text style={styles.latest} accessibilityLabel={latest ? `${fmt(latest.kg)} kg` : 'No entry yet'}>
          {latest ? `${fmt(latest.kg)} kg` : '—'}
        </Text>
      </View>
      {recent.length > 0 ? <LineChart points={recent.map((b) => ({ date: b.date, value: b.kg }))} unit="kg" height={130} style={styles.chart} /> : null}
      <View style={styles.add}>
        <NumberField
          key={fieldKey}
          value={draft}
          onChange={setDraft}
          step={0.1}
          unit="kg"
          label="Bodyweight"
          placeholder={latest ? fmt(latest.kg) : '0.0'}
          style={styles.field}
        />
        <TextButton
          text="Add"
          enabled={canAdd}
          onPress={() => {
            if (draft == null || draft <= 0) return;
            onAdd(draft);
            setDraft(null);
            setFieldKey((k) => k + 1);
          }}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  header: { flexDirection: 'row', alignItems: 'center', marginTop: 28 },
  title: { ...Type.headline, flex: 1 },
  latest: { ...Type.title2, ...tabular },
  chart: { marginTop: 8 },
  add: { flexDirection: 'row', alignItems: 'center', gap: 12, marginTop: 12 },
  field: { flex: 1 },
});
