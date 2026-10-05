import { SymbolView } from 'expo-symbols';
import { K } from '@/theme';

/** Bold check mark: the chosen row of a choice list, a logged set, a finished block. */
export function Check({ color = K.Accent, size = 18 }: { color?: string; size?: number }) {
  return <SymbolView name={{ ios: 'checkmark', android: 'check', web: 'check' }} tintColor={color} size={size} weight="bold" />;
}
