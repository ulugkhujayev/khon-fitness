import { ConfirmSheet } from '@/components/ui';

/** Confirm before a session goes: "Discard" while it runs, "Delete" once it is saved. */
export function DeleteSessionSheet({ visible, finished, onConfirm, onDismiss }: { visible: boolean; finished: boolean; onConfirm: () => void; onDismiss: () => void }) {
  return (
    <ConfirmSheet
      visible={visible}
      title={finished ? 'Delete this session?' : 'Discard this workout?'}
      body={finished ? 'The sets and the session go away. Charts recompute.' : 'Nothing from today is kept.'}
      action={finished ? 'Delete' : 'Discard'}
      onConfirm={onConfirm}
      onDismiss={onDismiss}
    />
  );
}
