import { Pressable, StyleSheet, Text, View } from 'react-native';
import { SymbolView } from 'expo-symbols';
import { monthStart, monthTitle } from '@/domain/dates';
import { K, Space, Type } from '@/theme';

/** "‹ October 2026 ›": the month title between previous and next buttons. `month` is any date in the month. */
export function MonthSwitcher({ month, onChange }: { month: string; onChange: (month: string) => void }) {
  return (
    <View style={styles.row}>
      <StepButton back onPress={() => onChange(monthStart(month, -1))} />
      <Text style={styles.title} accessibilityRole="header">
        {monthTitle(month)}
      </Text>
      <StepButton back={false} onPress={() => onChange(monthStart(month, 1))} />
    </View>
  );
}

function StepButton({ back, onPress }: { back: boolean; onPress: () => void }) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={back ? 'Previous month' : 'Next month'}
      style={({ pressed }) => [styles.button, pressed && styles.pressed]}>
      <SymbolView
        name={back ? { ios: 'chevron.left', android: 'chevron_left', web: 'chevron_left' } : { ios: 'chevron.right', android: 'chevron_right', web: 'chevron_right' }}
        tintColor={K.Accent}
        size={18}
      />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', height: Space.tapMin },
  title: { ...Type.headline, flex: 1, textAlign: 'center' },
  button: { width: Space.tapMin, height: Space.tapMin, alignItems: 'center', justifyContent: 'center' },
  pressed: { opacity: 0.5 },
});
