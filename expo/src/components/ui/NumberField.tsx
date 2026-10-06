import { Ref, useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View, ViewStyle } from 'react-native';
import { fmt, parseNumberInput } from '@/domain/format';
import { bound, stepValue } from '@/domain/stepper';
import { tick } from '@/haptics';
import { Font, K, Type } from '@/theme';
import { useNumberText } from './useNumberText';
import { useRepeatPress } from './useRepeatPress';

/** Below this width the field drops its unit and slims the steppers so the value stays visible (Components.kt: 176 dp). */
export const COMPACT_WIDTH = 176;

const isPlainNumber = (s: string) => /^\d+(\.\d+)?$/.test(s);

/**
 * Number widget (DESIGN.md): minus, the typed value with its unit, plus. No caption, no native spinner.
 * Port of NumberField in Components.kt. The text follows useNumberText; steppers set it themselves, click
 * (haptics) on each step, and repeat faster while held.
 */
export function NumberField({
  value,
  onChange,
  step,
  unit,
  decimals = 1,
  min = 0,
  max = Infinity,
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
  /** Bounds for steps and typing, e.g. effort 1 to 10. Zero and up by default. */
  min?: number;
  max?: number;
  height?: number;
  placeholder?: string;
  /** Accessible name, e.g. "A1 weight". The screen shows no caption. */
  label?: string;
  inputRef?: Ref<TextInput>;
  /** Set when the caller already knows the width, so the unit it shows elsewhere matches. Else measured. */
  compact?: boolean;
  style?: ViewStyle;
}) {
  const { text, setText, focus, blur } = useNumberText(value, decimals);
  const [measured, setMeasured] = useState(false);
  const compact = forceCompact ?? measured;

  const commit = (v: number | null) => onChange(v == null ? null : bound(v, min, max));

  // While − or + is held the steps only show; the value is saved once on release, not on every step.
  const held = useRef<number | null>(null);

  const stepBy = (sign: 1 | -1, save: boolean) => {
    // An empty field steps from what its placeholder shows (last time, latest bodyweight), not from zero.
    const shown = placeholder && isPlainNumber(placeholder) ? Number(placeholder) : null;
    const base = held.current ?? parseNumberInput(text).value ?? value ?? shown ?? 0;
    const next = stepValue(base, sign, step, min, max);
    setText(fmt(next, decimals));
    tick(false);
    if (save) commit(next);
    else held.current = next;
  };

  const release = () => {
    if (held.current != null) commit(held.current);
    held.current = null;
  };

  const stepLabel = `${fmt(step, decimals)} ${unit}`.trim();
  const stepWidth = compact ? 36 : 44;

  return (
    <View
      style={[styles.box, { height }, style]}
      // Rounded: a 176-wide field can measure 175.99 on a fractional pixel grid.
      onLayout={(e) => setMeasured(Math.round(e.nativeEvent.layout.width) < COMPACT_WIDTH)}>
      <StepButton glyph="−" label={`Decrease by ${stepLabel}`} width={stepWidth} onStep={(save) => stepBy(-1, save)} onRelease={release} />
      <View style={styles.divider} />
      <TextInput
        ref={inputRef}
        value={text}
        onChangeText={(raw) => {
          const p = parseNumberInput(raw);
          setText(p.text);
          commit(p.value);
        }}
        onFocus={focus}
        // The typed value, not `value`: a database write may not have come back yet.
        onBlur={() => {
          const typed = parseNumberInput(text).value;
          blur(typed == null ? value : bound(typed, min, max));
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
      <StepButton glyph="+" label={`Increase by ${stepLabel}`} width={stepWidth} onStep={(save) => stepBy(1, save)} onRelease={release} />
    </View>
  );
}

/** − or +: a tap steps once and saves; held, it keeps stepping faster until released (useRepeatPress). */
function StepButton({
  glyph,
  label,
  width,
  onStep,
  onRelease,
}: {
  glyph: string;
  label: string;
  width: number;
  /** `save` is false while held: the caller saves once on release. */
  onStep: (save: boolean) => void;
  onRelease: () => void;
}) {
  const press = useRepeatPress(
    () => onStep(true),
    () => onStep(false),
    onRelease,
  );
  return (
    <Pressable {...press} accessibilityRole="button" accessibilityLabel={label} style={({ pressed }) => [styles.step, { width }, pressed && styles.stepPressed]}>
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
