import { useState } from 'react';
import { Alert, StyleSheet } from 'react-native';
import { SymbolViewProps } from 'expo-symbols';
import { rescheduleWindow } from '@/alerts/windowReminders';
import { EditorScreen } from '@/components/layout';
import { GroupedList, GroupFooter, IconTile, ListRow, SectionTitle } from '@/components/ui';
import { BackupError } from '@/domain/backup';
import { pickBackupText, shareBackup } from '@/db/backupFile';
import { describeBackupText, importJson, resetToSeed } from '@/db/repository';
import { K } from '@/theme';
import { appVersion, useAppUpdate } from '@/updates/useAppUpdate';

/** Each row's icon tile, as in iOS Settings. */
const ICONS = {
  export: { ios: 'square.and.arrow.up', android: 'upload', web: 'upload' },
  import: { ios: 'square.and.arrow.down', android: 'download', web: 'download' },
  version: { ios: 'info.circle.fill', android: 'info', web: 'info' },
  update: { ios: 'arrow.down.circle.fill', android: 'system_update', web: 'system_update' },
  reset: { ios: 'arrow.counterclockwise', android: 'restart_alt', web: 'restart_alt' },
} satisfies Record<string, SymbolViewProps['name']>;

/**
 * Settings in groups with icon tiles, as in iOS Settings: Backup (export, import, and what a backup holds or
 * how the last one went), App (version, updates) and Reset on its own, each with a footnote. Import first
 * says what the file holds, then asks merge or replace; import and reset re-plan the window's reminders.
 */
export default function SettingsScreen() {
  const [status, setStatus] = useState<{ text: string; error?: boolean } | null>(null);
  const update = useAppUpdate();

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
      { text: 'Merge', onPress: () => run(() => importJson(body, false).then(rescheduleWindow), 'Backup merged') },
      { text: 'Replace all', style: 'destructive', onPress: () => run(() => importJson(body, true).then(rescheduleWindow), 'Backup restored') },
    ]);
  };

  const onReset = () =>
    Alert.alert('Reset to built-in data', 'Every session, bodyweight and edit is deleted.', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Reset', style: 'destructive', onPress: () => run(() => resetToSeed().then(rescheduleWindow), 'Reset to built-in data') },
    ]);

  return (
    <EditorScreen title="Settings">
      <SectionTitle text="Backup" />
      <GroupedList>
        <ListRow leading={<IconTile icon={ICONS.export} color={K.Blue} />} title="Export backup" onPress={onExport} chevron={false} />
        <ListRow leading={<IconTile icon={ICONS.import} color={K.Green} />} title="Import backup" onPress={onImport} chevron={false} last />
      </GroupedList>
      <GroupFooter
        text={status?.text ?? 'One JSON file with every program, session, bodyweight and plan. Import shows what a file holds, then merges or replaces.'}
        error={status?.error}
      />

      <SectionTitle text="App" style={styles.section} />
      <GroupedList>
        <ListRow leading={<IconTile icon={ICONS.version} color={K.Dim} />} title="Version" value={appVersion} chevron={false} />
        <ListRow
          leading={<IconTile icon={ICONS.update} color={K.Accent} />}
          title={update.ready ? 'Restart to update' : 'Check for update'}
          value={update.busy ? 'Checking…' : undefined}
          onPress={update.ready ? update.restart : update.check}
          chevron={false}
          last
        />
      </GroupedList>
      {update.message ? <GroupFooter text={update.message} /> : null}

      <GroupedList style={styles.reset}>
        <ListRow leading={<IconTile icon={ICONS.reset} color={K.Red} />} title="Reset to built-in data" color={K.Red} onPress={onReset} chevron={false} last />
      </GroupedList>
      <GroupFooter text="Deletes every session, bodyweight and edit, and brings back the built-in programs and library." />
    </EditorScreen>
  );
}

const styles = StyleSheet.create({
  section: { marginTop: 24 },
  reset: { marginTop: 32 },
});
