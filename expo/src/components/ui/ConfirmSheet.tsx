import { StyleSheet, Text } from 'react-native';
import { K, Type } from '@/theme';
import { PrimaryButton } from './PrimaryButton';
import { Sheet } from './Sheet';
import { TextButton } from './TextButton';

/** Confirm for a destructive action (ConfirmSheet in Common.kt): what goes, a red button, Cancel. */
export function ConfirmSheet({
  visible,
  title,
  body,
  action,
  onDismiss,
  onConfirm,
}: {
  visible: boolean;
  title: string;
  body: string;
  action: string;
  onDismiss: () => void;
  onConfirm: () => void;
}) {
  return (
    <Sheet visible={visible} title={title} onDismiss={onDismiss}>
      <Text style={styles.body}>{body}</Text>
      <PrimaryButton text={action} color={K.Red} onPress={onConfirm} style={styles.action} />
      <TextButton text="Cancel" color={K.Muted} onPress={onDismiss} style={styles.cancel} />
    </Sheet>
  );
}

const styles = StyleSheet.create({
  body: { ...Type.subheadline, color: K.Muted },
  action: { marginTop: 16 },
  cancel: { alignSelf: 'center', marginTop: 8 },
});
