import * as Haptics from 'expo-haptics';
import { Platform } from 'react-native';

/** Haptics are a nicety: on a device without them, or with them off, nothing happens and nothing fails. */
const quietly = (p: Promise<void>) => void p.catch(() => undefined);

/**
 * A detent as a picker passes a value: a light click, a firmer one on a labelled value. Android uses the
 * view's haptic constants, which follow the system touch-feedback setting; these two exist on every API
 * level (the segment ticks need Android 14).
 */
export function tick(major: boolean) {
  if (Platform.OS === 'android') {
    quietly(Haptics.performAndroidHapticsAsync(major ? Haptics.AndroidHaptics.Context_Click : Haptics.AndroidHaptics.Clock_Tick));
  } else {
    quietly(major ? Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light) : Haptics.selectionAsync());
  }
}
