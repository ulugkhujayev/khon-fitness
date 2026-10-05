import { PlanItem } from './plan';
import { Block, BlockExercise, ItemType, Modality } from './types';

/** 240 -> "4 min", 45 -> "45 s", 90 -> "1:30". Same as dur() in Common.kt. */
export function dur(sec: number): string {
  if (sec >= 60 && sec % 60 === 0) return `${sec / 60} min`;
  if (sec < 60) return `${sec} s`;
  return `${Math.floor(sec / 60)}:${String(sec % 60).padStart(2, '0')}`;
}

/** One line under the item name on Today: "7 blocks · 14 sets", "4 × 4 min on bike, 3 min rest", "Rest day". */
export function preview(item: PlanItem, blocks: Block[], blockExercises: BlockExercise[], modalities: Modality[]): string {
  switch (item.itemType) {
    case ItemType.PROGRAM: {
      const bs = blocks.filter((b) => b.programId === item.program?.id);
      const ids = new Set(bs.map((b) => b.id));
      const sets = blockExercises.filter((be) => ids.has(be.blockId)).reduce((n, be) => n + be.sets, 0);
      return `${bs.length} blocks · ${sets} sets`;
    }
    case ItemType.CARDIO: {
      const ex = item.cardio;
      if (!ex) return '';
      const mod = modalities.find((m) => m.id === ex.modalityId)?.name.toLowerCase() ?? '';
      if (ex.intervals) return `${ex.rounds} × ${dur(ex.workSec)} on ${mod}, ` + (ex.restSec > 0 ? `${dur(ex.restSec)} rest` : 'no rest');
      return `Steady on ${mod}`;
    }
    default:
      return 'Rest day';
  }
}
