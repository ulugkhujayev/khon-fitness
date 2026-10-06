import { StyleSheet, ViewStyle } from 'react-native';
import { K, Space } from '@/theme';
import { PrimaryButton } from './PrimaryButton';

/**
 * A secondary action that should still read as a button (DESIGN.md, Buttons): the primary capsule
 * tinted (HIG), accent text on accent at 20 percent and 44 pt tall, so the screen's one primary button
 * keeps the lead.
 */
export function TintedButton({ style, ...props }: { text: string; enabled?: boolean; onPress: () => void; label?: string; style?: ViewStyle }) {
  return <PrimaryButton {...props} color={K.AccentSoft} textColor={K.Accent} style={StyleSheet.flatten([styles.button, style])} />;
}

const styles = StyleSheet.create({
  button: { height: Space.tapMin, borderRadius: Space.tapMin / 2 },
});
