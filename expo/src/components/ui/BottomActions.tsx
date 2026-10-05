import { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';

/** Secondary and destructive actions at the bottom of an editor (DESIGN.md, Editors), as text buttons in a row. */
export function BottomActions({ children }: { children: ReactNode }) {
  return <View style={styles.row}>{children}</View>;
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', columnGap: 22, paddingTop: 28, paddingLeft: 2 },
});
