import { useState } from 'react';
import Constants from 'expo-constants';
import * as Updates from 'expo-updates';

/** The app version the update row shows, e.g. "v1.0.0". */
export const appVersion = `v${Constants.expoConfig?.version ?? '0'}`;

/**
 * Over-the-air updates through EAS Update, in place of the Kotlin app's APK updater: the app checks on launch and
 * downloads in the background; Check for update asks now; once an update is downloaded, Restart applies it.
 * Development builds load from Metro, so there updates are off.
 */
export function useAppUpdate() {
  const { isUpdatePending, isChecking, isDownloading } = Updates.useUpdates();
  const [message, setMessage] = useState<string | null>(null);

  const check = async () => {
    if (!Updates.isEnabled) return setMessage('Updates are off in this build.');
    setMessage('Checking…');
    try {
      const result = await Updates.checkForUpdateAsync();
      if (!result.isAvailable) return setMessage(`${appVersion} is the latest.`);
      setMessage('Downloading…');
      await Updates.fetchUpdateAsync();
      setMessage(null);
    } catch (e) {
      setMessage(e instanceof Error ? e.message : 'Update failed.');
    }
  };

  return {
    /** Downloaded and waiting for a restart. */
    ready: isUpdatePending,
    busy: isChecking || isDownloading,
    message,
    check,
    restart: () => Updates.reloadAsync(),
  };
}
