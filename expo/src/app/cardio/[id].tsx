import { useLocalSearchParams } from 'expo-router';
import { CardioForm } from '@/components/cardio/CardioForm';
import { EditorScreen, useLeaveWhenGone } from '@/components/layout';
import { cardioPatch } from '@/domain/cardio';
import { useCardioSession } from '@/db/hooks';
import * as repo from '@/db/repository';

/** A cardio session's form: after the timer, from Today for a steady exercise, or from History to edit. */
export default function CardioScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { data } = useCardioSession(id);
  const leave = useLeaveWhenGone(data != null && data.session == null);
  if (!data?.session) return <EditorScreen />;

  return (
    <CardioForm
      key={id}
      session={data.session}
      exercise={data.exercise}
      intervals={data.intervals}
      onSave={async (draft, intervals) => {
        await repo.patchSession(id, cardioPatch(draft, Date.now()));
        await repo.saveIntervals(intervals);
        leave();
      }}
      onDelete={async () => {
        await repo.deleteSession(id);
        leave();
      }}
    />
  );
}
