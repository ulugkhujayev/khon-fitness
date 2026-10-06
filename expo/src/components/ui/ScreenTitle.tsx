import { ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Type } from '@/theme';

/** Large title at the top of a tab screen, bar items trailing. TabScreen keeps it fixed above the content. */
export function ScreenTitle({ text, trailing }: { text: string; trailing?: ReactNode }) {
  return (
    <View style={styles.row}>
      <Text style={[Type.largeTitle, styles.text]} accessibilityRole="header">
        {text}
      </Text>
      {trailing}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', paddingTop: 4, paddingBottom: 12 },
  text: { flex: 1 },
});
