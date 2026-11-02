import { ReactNode } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import { K, Space } from '@/theme';

/** Inset grouped list (DESIGN.md, Lists): one rounded surface; rows draw their own inset separators. */
export function GroupedList({ children, elevated = false, style }: { children: ReactNode; elevated?: boolean; style?: ViewStyle }) {
  return <View style={[styles.group, elevated && styles.elevated, style]}>{children}</View>;
}

const styles = StyleSheet.create({
  group: { backgroundColor: K.Surface, borderRadius: Space.groupRadius, overflow: 'hidden' },
  /** Inside a sheet, groups sit on the elevated fill. */
  elevated: { backgroundColor: K.Elevated },
});
