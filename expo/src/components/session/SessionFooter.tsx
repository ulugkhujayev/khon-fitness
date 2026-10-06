import { StyleSheet, View } from 'react-native';
import { TintedButton } from '@/components/ui';
import { Session, SessionDetailsPatch } from '@/domain/types';
import { K } from '@/theme';
import { SessionDetails } from './SessionDetails';

/**
 * The end of a session's page, under its blocks: date, effort and note, then Discard workout (Delete
 * session once finished) as a red button. Finish lives in the bar at the top.
 */
export function SessionFooter({
  session,
  live,
  onChange,
  onDelete,
}: {
  session: Session;
  /** Editable: a running session, or a finished one in Edit. */
  live: boolean;
  onChange: (patch: SessionDetailsPatch) => void;
  onDelete: () => void;
}) {
  const finished = session.finishedAt != null;
  return (
    <View style={styles.footer}>
      <SessionDetails session={session} live={live} onChange={onChange} />
      <TintedButton text={finished ? 'Delete session' : 'Discard workout'} color={K.Red} onPress={onDelete} style={styles.delete} />
    </View>
  );
}

const styles = StyleSheet.create({
  footer: { marginTop: 20 },
  delete: { marginTop: 20 },
});
