import { View } from 'react-native';
import { Bodyweight } from '@/domain/types';
import { BodyweightEntry } from './BodyweightEntry';
import { BodyweightStats } from './BodyweightStats';

/** Bodyweight on Today (DESIGN.md, Today): the statistics card, then the entry card with its ruler. */
export function BodyweightSection({ today, bodyweights, onAdd }: { today: string; bodyweights: Bodyweight[]; onAdd: (kg: number) => void }) {
  return (
    <View>
      <BodyweightStats today={today} bodyweights={bodyweights} />
      <BodyweightEntry today={today} bodyweights={bodyweights} onSave={onAdd} />
    </View>
  );
}
