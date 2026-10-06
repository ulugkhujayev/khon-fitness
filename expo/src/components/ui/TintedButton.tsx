import { ViewStyle } from 'react-native';
import { K } from '@/theme';
import { PrimaryButton } from './PrimaryButton';

/**
 * A secondary action that should still read as a button (DESIGN.md, Buttons): the primary capsule
 * tinted (HIG), accent text on accent at 20 percent, so the screen's one primary button keeps the lead.
 */
export function TintedButton(props: { text: string; enabled?: boolean; onPress: () => void; label?: string; style?: ViewStyle }) {
  return <PrimaryButton {...props} color={K.AccentSoft} textColor={K.Accent} />;
}
