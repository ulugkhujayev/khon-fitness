import { StyleSheet, View } from 'react-native';
import { PrimaryButton, TintedButton } from '@/components/ui';
import { Session } from '@/domain/types';
import { K } from '@/theme';
import { SessionDetails } from './SessionDetails';

/**
 * The end of a session's page, under its blocks: date, effort and note, then Finish workout while it runs,
 * and Discard workout (Delete session once finished) as a red button.
 */
export function SessionFooter({
  session,
  live,
  onChange,
  onFinish,
  onDelete,
}: {
  session: Session;
  /** Editable: a running session, or a finished one in Edit. */
  live: boolean;
  onChange: (patch: Partial<Pick<Session, 'date' | 'effort' | 'note'>>) => void;
  onFinish: () => void;
  onDelete: () => void;
}) {
  const finished = session.finishedAt != null;
  return (
    <View style={styles.footer}>
      <SessionDetails session={session} live={live} onChange={onChange} />
      <View style={styles.gap} />
      {!finished ? <PrimaryButton text="Finish workout" onPress={onFinish} /> : null}
      <TintedButton text={finished ? 'Delete session' : 'Discard workout'} color={K.Red} onPress={onDelete} style={styles.delete} />
    </View>
  );
}

const styles = StyleSheet.create({
  footer: { marginTop: 20 },
  gap: { height: 20 },
  delete: { marginTop: 12 },
});
