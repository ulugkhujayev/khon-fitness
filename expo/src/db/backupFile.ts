import * as DocumentPicker from 'expo-document-picker';
import { File, Paths } from 'expo-file-system';
import * as Sharing from 'expo-sharing';
import { today } from '@/domain/dates';
import { exportJson } from './repository';

/** Writes the backup to the cache and opens the share sheet (Files, Drive, Telegram, mail). */
export async function shareBackup(): Promise<void> {
  const file = new File(Paths.cache, `khon-backup-${today()}.json`);
  if (file.exists) file.delete();
  file.create();
  file.write(await exportJson());
  await Sharing.shareAsync(file.uri, { mimeType: 'application/json', dialogTitle: 'Export backup', UTI: 'public.json' });
}

/** Opens the document picker and returns the chosen file's text, or null when cancelled. */
export async function pickBackupText(): Promise<string | null> {
  const res = await DocumentPicker.getDocumentAsync({ type: ['application/json', 'text/plain', '*/*'], copyToCacheDirectory: true });
  if (res.canceled || !res.assets?.length) return null;
  return new File(res.assets[0].uri).text();
}
