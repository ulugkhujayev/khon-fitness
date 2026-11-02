import { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';
import { GroupFooter } from './GroupFooter';

/**
 * The actions at the bottom of an editor (DESIGN.md, Editors): groups of ActionRows, the destructive one in
 * a group of its own, and a note under them saying why an action is off.
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
  actions: { marginTop: 32, gap: 20 },
});
