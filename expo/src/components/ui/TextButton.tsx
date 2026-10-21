import { Pressable, StyleSheet, Text, ViewStyle } from 'react-native';
import { Font, K, Space, Type } from '@/theme';

/** A plain action: accent text, no fill, 44 pt tall. Red for destructive, dim when disabled. */
export function TextButton({
  text,
  color = K.Accent,
  enabled = true,
  onPress,
  label,
  style,
}: {
  text: string;
  color?: string;
  enabled?: boolean;
  onPress: () => void;
  /** Spoken name when the visible text is not enough, e.g. "Remove A". */
  label?: string;
  style?: ViewStyle;
}) {
  return (
    <Pressable
      onPress={onPress}
      disabled={!enabled}
      accessibilityRole="button"
      accessibilityLabel={label ?? text}
      accessibilityState={{ disabled: !enabled }}
      hitSlop={4}
      style={({ pressed }) => [styles.button, pressed && styles.pressed, style]}>
      <Text style={[styles.text, { color: enabled ? color : K.Dim }]}>{text}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: { minHeight: Space.tapMin, justifyContent: 'center', paddingHorizontal: 4 },
  pressed: { opacity: 0.5 },
  text: { ...Type.body, fontFamily: Font.medium },
});
