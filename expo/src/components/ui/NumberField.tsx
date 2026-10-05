import { Ref, useEffect, useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View, ViewStyle } from 'react-native';
import { fmt, parseNumberInput } from '@/domain/format';
import { Font, K, Type } from '@/theme';

/** Below this width the field drops its unit and slims the steppers so the value stays visible (Components.kt: 176 dp). */
export const COMPACT_WIDTH = 176;

const round = (v: number) => Math.round(v * 10000) / 10000;
const isPlainNumber = (s: string) => /^\d+(\.\d+)?$/.test(s);

/**
 * Number widget (DESIGN.md): minus, the typed value with its unit, plus. No caption, no native spinner.
 * Port of NumberField in Components.kt. While focused the field keeps its own text, so a write that
 * comes back from the database never moves the cursor; steppers set the text themselves.
 */
export function NumberField({
  value,
  onChange,
  step,
  unit,
  decimals = 1,
  height = 44,
  placeholder,
  label,
  inputRef,
  compact: forceCompact,
  style,
}: {
  value: number | null;
  onChange: (v: number | null) => void;
  step: number;
  unit: string;
  decimals?: number;
  height?: number;
  placeholder?: string;
  /** Accessible name, e.g. "A1 weight". The screen shows no caption. */
  label?: string;
  inputRef?: Ref<TextInput>;
  /** Set when the caller already knows the width, so the unit it shows elsewhere matches. Else measured. */
  compact?: boolean;
  style?: ViewStyle;
}) {
  const [text, setText] = useState(fmt(value, decimals));
  const [measured, setMeasured] = useState(false);
  const compact = forceCompact ?? measured;
  const focused = useRef(false);

  useEffect(() => {
    if (!focused.current) setText(fmt(value, decimals));
  }, [value, decimals]);

  const commit = (v: number | null) => onChange(v == null ? null : Math.max(0, v));

  const stepBy = (sign: 1 | -1) => {
    // An empty field steps from what its placeholder shows (last time, latest bodyweight), not from zero.
    const shown = placeholder && isPlainNumber(placeholder) ? Number(placeholder) : null;
    const base = parseNumberInput(text).value ?? value ?? shown ?? 0;
    const next = Math.max(0, round(base + sign * step));
    setText(fmt(next, decimals));
    commit(next);
  };

  const stepLabel = `${fmt(step, decimals)} ${unit}`.trim();
  const stepWidth = compact ? 36 : 44;

  return (
    <View
      style={[styles.box, { height }, style]}
      // Rounded: a 176-wide field can measure 175.99 on a fractional pixel grid.
      onLayout={(e) => setMeasured(Math.round(e.nativeEvent.layout.width) < COMPACT_WIDTH)}>
      <StepButton glyph="−" label={`Decrease by ${stepLabel}`} width={stepWidth} onPress={() => stepBy(-1)} />
      <View style={styles.divider} />
      <TextInput
        ref={inputRef}
        value={text}
        onChangeText={(raw) => {
          const p = parseNumberInput(raw);
          setText(p.text);
          commit(p.value);
        }}
        onFocus={() => (focused.current = true)}
        onBlur={() => {
          focused.current = false;
          setText(fmt(parseNumberInput(text).value ?? value, decimals));
        }}
        selectTextOnFocus
        keyboardType={decimals > 0 ? 'decimal-pad' : 'number-pad'}
        keyboardAppearance="dark"
        selectionColor={K.Accent}
        placeholder={placeholder}
        placeholderTextColor={K.Dim}
        accessibilityLabel={label}
        style={styles.input}
      />
      {compact ? <View style={styles.compactGap} /> : <Text style={styles.unit}>{unit}</Text>}
      <View style={styles.divider} />
      <StepButton glyph="+" label={`Increase by ${stepLabel}`} width={stepWidth} onPress={() => stepBy(1)} />
    </View>
  );
}

function StepButton({ glyph, label, width, onPress }: { glyph: string; label: string; width: number; onPress: () => void }) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={label}
      style={({ pressed }) => [styles.step, { width }, pressed && styles.stepPressed]}>
      <Text style={Type.stepGlyph}>{glyph}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  box: { flexDirection: 'row', alignItems: 'stretch', borderRadius: 10, backgroundColor: K.Surface2, overflow: 'hidden' },
  step: { alignItems: 'center', justifyContent: 'center' },
  stepPressed: { backgroundColor: K.Surface3 },
  divider: { width: 1, backgroundColor: K.Divider },
  input: { ...Type.value, flex: 1, minWidth: 0, paddingLeft: 8, paddingVertical: 0, textAlign: 'right' },
  compactGap: { width: 8 },
  unit: { ...Type.footnote, fontFamily: Font.medium, color: K.Muted, alignSelf: 'center', paddingLeft: 4, paddingRight: 8 },
});
