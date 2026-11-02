import { StyleSheet, Text, TextInput, View, ViewStyle } from 'react-native';
import { parseNumberInput } from '@/domain/format';
import { Font, K, Type } from '@/theme';
import { useNumberText } from './useNumberText';

/**
 * A big centered figure with its unit, "80.6 kg", above a picker that sets it. A tap selects the number
 * for typing. Text that is not a positive number yet ("", "0.") leaves the value as it was.
 */
export function FigureField({
  value,
  onChange,
  unit,
  label,
  decimals = 1,
  style,
}: {
  value: number;
  onChange: (v: number) => void;
  unit: string;
  label: string;
  decimals?: number;
  style?: ViewStyle;
}) {
  const { text, setText, focus, blur } = useNumberText(value, decimals);

  return (
    <View style={[styles.row, style]}>
      <TextInput
        value={text}
        onChangeText={(raw) => {
          const p = parseNumberInput(raw);
          setText(p.text);
          if (p.value != null && p.value > 0) onChange(p.value);
        }}
        onFocus={focus}
        // `value`, not the typed text: the picker may have moved it since, and "0" was never taken.
        onBlur={() => blur(value)}
        selectTextOnFocus
        keyboardType={decimals > 0 ? 'decimal-pad' : 'number-pad'}
        keyboardAppearance="dark"
        selectionColor={K.Accent}
        accessibilityLabel={label}
        style={styles.input}
      />
      <Text style={styles.unit} importantForAccessibility="no" accessibilityElementsHidden>
        {unit}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'baseline', justifyContent: 'center' },
  input: { ...Type.figure, minWidth: 44, padding: 0, textAlign: 'center' },
  unit: { ...Type.subheadline, fontFamily: Font.medium, color: K.Muted, marginLeft: 4 },
});
