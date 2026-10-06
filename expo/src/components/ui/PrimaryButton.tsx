import { Pressable, StyleSheet, Text, ViewStyle } from 'react-native';
import { K, Space, Type } from '@/theme';

/** The one prominent button of a screen: a 50 pt full-width capsule in accent (or red for a destructive confirm). */
export function PrimaryButton({
  text,
  enabled = true,
  onPress,
  style,
  color = K.Accent,
  textColor = K.AccentInk,
  label,
}: {
  text: string;
  enabled?: boolean;
  onPress: () => void;
  style?: ViewStyle;
  /** K.Red for a destructive confirm, K.White in the stretch player. */
  color?: string;
  textColor?: string;
  /** Spoken name when the visible text is not enough, e.g. "Add bodyweight". */
  label?: string;
}) {
  return (
    <Pressable
      onPress={onPress}
      disabled={!enabled}
      accessibilityRole="button"
      accessibilityLabel={label ?? text}
      accessibilityState={{ disabled: !enabled }}
      style={({ pressed }) => [styles.button, { backgroundColor: enabled ? color : K.Surface2 }, pressed && styles.pressed, style]}>
      <Text style={[Type.headline, { color: enabled ? textColor : K.Muted }]}>{text}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: { height: Space.buttonHeight, borderRadius: Space.buttonHeight / 2, alignItems: 'center', justifyContent: 'center', alignSelf: 'stretch' },
  pressed: { opacity: 0.8 },
});
