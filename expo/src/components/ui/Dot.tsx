import { View } from 'react-native';

/** A color as a circle (DESIGN.md, Color): filled for strength or high intensity, a ring otherwise. */
export function Dot({ color, filled = true, size = 12 }: { color: string; filled?: boolean; size?: number }) {
  const shape = { width: size, height: size, borderRadius: size / 2 };
  return <View style={[shape, filled ? { backgroundColor: color } : { borderWidth: 2, borderColor: color }]} />;
}
