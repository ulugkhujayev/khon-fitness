import { useEffect, useRef } from 'react';
import { StyleSheet, View } from 'react-native';
import { router } from 'expo-router';
import { today as todayIso } from '@/domain/dates';
import { useToday } from '@/db/hooks';
import { resumeSession, startItem } from '@/navigation';
import { K } from '@/theme';

/**
 * The Today widget's button (ACTION_START_TODAY in Kotlin): resumes the running session or starts today's item,
 * in place of this screen; on a rest day it lands on Today.
 */
export default function StartToday() {
  const { data } = useToday(todayIso());
  // The start writes a session, which reloads the data; it must still run only once.
  const handled = useRef(false);

  useEffect(() => {
    if (!data || handled.current) return;
    handled.current = true;
    if (data.unfinished) return resumeSession(data.unfinished, true);
    void startItem(data.item, true).then((started) => started || router.replace('/'));
  }, [data]);

  return <View style={styles.screen} />;
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: K.Bg },
});
