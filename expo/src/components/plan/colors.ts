import { PlanItem } from '@/domain/plan';
import { argbToHex, Exercise, Intensity, ItemType, Kind, Modality, Session } from '@/domain/types';
import { K } from '@/theme';

/** Gray for a cardio exercise whose modality is gone. */
const FALLBACK = 0xff9b9ba3;

export const modalityColor = (modalityId: string | null, modalities: Modality[]) =>
  argbToHex(modalities.find((m) => m.id === modalityId)?.color ?? FALLBACK);

/** Accent for strength, the modality color for cardio. Same as exerciseColor in Common.kt. */
export const exerciseColor = (ex: Exercise, modalities: Modality[]) =>
  ex.kind === Kind.STRENGTH ? K.Accent : modalityColor(ex.modalityId, modalities);

/** Dot for a plan item: accent filled for a program, modality color for cardio (filled when high intensity), none for rest. */
export function itemColor(item: PlanItem, modalities: Modality[]): { color: string; filled: boolean } | null {
  if (item.itemType === ItemType.PROGRAM) return { color: K.Accent, filled: true };
  if (item.itemType === ItemType.CARDIO && item.cardio) {
    return { color: modalityColor(item.cardio.modalityId, modalities), filled: item.cardio.intensity === Intensity.HIGH };
  }
  return null;
}

/** Dot for a done session: accent for a program, the exercise color for cardio (filled when high intensity), muted when the exercise is gone. */
export function sessionColor(s: Session, exercises: Exercise[], modalities: Modality[]): { color: string; filled: boolean } {
  if (s.itemType === ItemType.PROGRAM) return { color: K.Accent, filled: true };
  const ex = exercises.find((e) => e.id === s.exerciseId);
  return ex ? { color: exerciseColor(ex, modalities), filled: ex.intensity === Intensity.HIGH } : { color: K.Muted, filled: true };
}
