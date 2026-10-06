import { ViewStyle } from 'react-native';
import { hexAlpha } from '@/domain/types';
import { K } from '@/theme';
import { PrimaryButton } from './PrimaryButton';

/**
 * A secondary action that should still read as a button (DESIGN.md, Buttons): the primary capsule
 * tinted (HIG), its color as text on that color at 20 percent and 44 pt tall, so the screen's one primary
 * button keeps the lead. Accent by default, K.Red for a destructive one.
 */
export function TintedButton({
  color = K.Accent,
  ...props
}: {
  text: string;
  /** A hex color: the text, and the fill at 20 percent. */
  color?: string;
  enabled?: boolean;
  onPress: () => void;
  label?: string;
  style?: ViewStyle;
}) {
  return <PrimaryButton {...props} color={hexAlpha(color, 0.2)} textColor={color} compact />;
}
