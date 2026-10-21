import { useEffect } from 'react';
import { StyleSheet, View } from 'react-native';
import { requestWindowSheet } from '@/alerts/windowRequest';
import { K } from '@/theme';

/** The eating-window widget's tap (ACTION_OPEN_WINDOW in Kotlin): Today with the window sheet open. */
export default function OpenWindow() {
  useEffect(() => {
    requestWindowSheet();
  }, []);
  return <View style={styles.screen} />;
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: K.Bg },
});
