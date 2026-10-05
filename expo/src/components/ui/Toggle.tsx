import { Switch } from 'react-native';
import { K } from '@/theme';

/** 51 x 31 toggle (DESIGN.md, HIG mapping): systemGreen on, Surface3 off, white thumb. */
export function Toggle({ value, onChange, label }: { value: boolean; onChange: (v: boolean) => void; label?: string }) {
  return (
    <Switch
      value={value}
      onValueChange={onChange}
      accessibilityLabel={label}
      trackColor={{ false: K.Surface3, true: K.Green }}
      thumbColor={K.White}
      ios_backgroundColor={K.Surface3}
    />
  );
}
