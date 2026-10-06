import { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';
import { GroupFooter } from './GroupFooter';

/**
 * Secondary and destructive actions at the bottom of an editor (DESIGN.md, Editors): full-width tinted
 * buttons 12 pt apart (TintedButton, red for a destructive one), and a note under them saying why one is off.
 */
export function BottomActions({ note, children }: { note?: string; children: ReactNode }) {
  return (
    <View style={styles.actions}>
      {children}
      {note ? <GroupFooter text={note} /> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  actions: { marginTop: 32, gap: 12 },
});
