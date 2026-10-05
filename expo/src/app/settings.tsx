import { useState } from 'react';
import { Alert, StyleSheet, Text } from 'react-native';
import { EditorScreen } from '@/components/layout';
import { GroupedList, ListRow } from '@/components/ui';
import { BackupError } from '@/domain/backup';
import { pickBackupText, shareBackup } from '@/db/backupFile';
import { describeBackupText, importJson, resetToSeed } from '@/db/repository';
import { K, Type } from '@/theme';

/** Export, import and reset. Import first says what the file holds, then asks merge or replace. */
export default function SettingsScreen() {
  const [status, setStatus] = useState<{ text: string; error?: boolean } | null>(null);

  const run = async (fn: () => Promise<void>, done: string) => {
    try {
      await fn();
      setStatus({ text: done });
    } catch (e) {
      setStatus({ text: e instanceof Error ? e.message : String(e), error: true });
    }
  };

  const onExport = () => run(shareBackup, 'Backup exported');

  const onImport = async () => {
    let text: string | null;
    let summary: string;
    try {
      text = await pickBackupText();
      if (text === null) return;
      summary = describeBackupText(text);
    } catch (e) {
      setStatus({ text: e instanceof BackupError || e instanceof Error ? e.message : String(e), error: true });
      return;
    }
    const body = text;
    Alert.alert('Import backup', summary, [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Merge', onPress: () => run(() => importJson(body, false), 'Backup merged') },
      { text: 'Replace all', style: 'destructive', onPress: () => run(() => importJson(body, true), 'Backup restored') },
    ]);
  };

  const onReset = () =>
    Alert.alert('Reset to built-in data', 'Every session, bodyweight and edit is deleted.', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Reset', style: 'destructive', onPress: () => run(resetToSeed, 'Reset to built-in data') },
    ]);

  return (
    <EditorScreen title="Settings">
      <GroupedList>
        <ListRow title="Export backup" color={K.Accent} onPress={onExport} chevron={false} />
        <ListRow title="Import backup" color={K.Accent} onPress={onImport} chevron={false} last />
      </GroupedList>
      <GroupedList style={styles.group}>
        <ListRow title="Reset to built-in data" color={K.Red} onPress={onReset} chevron={false} last />
      </GroupedList>
      {status ? <Text style={[styles.status, status.error && styles.error]}>{status.text}</Text> : null}
    </EditorScreen>
  );
}

const styles = StyleSheet.create({
  group: { marginTop: 24 },
  status: { ...Type.footnote, color: K.Muted, paddingHorizontal: 16, marginTop: 24 },
  error: { color: K.Red },
});
