/** Rules behind the program, block and exercise editors (ui/screens/Editors.kt, ProgramsScreen.kt). */
import { BlockExercise, Defaults, Exercise, Intensity, Kind, Modality, splitList } from './types';

/** Twelve preset colors for a modality, ARGB like Kotlin's PRESET_COLORS. */
export const PRESET_COLORS = [
  0xfff0a35a, 0xff71b7ff, 0xff73d6a2, 0xffb39dff, 0xffff8fa3, 0xffffd166,
  0xff4cd28a, 0xff7ce0a8, 0xffff9f68, 0xff9ad0ff, 0xffe0a3ff, 0xff9b9ba3,
];

/** Fields a cardio exercise can log, in the order the picker lists them. */
export const CARDIO_FIELDS = ['time', 'distance', 'avgHr', 'watts', 'laps', 'poolLength', 'stroke'] as const;

const FIELD_LABELS: Record<string, string> = {
  time: 'Time', distance: 'Distance', avgHr: 'Avg heart rate', watts: 'Watts', laps: 'Laps', poolLength: 'Pool length', stroke: 'Stroke',
};
export const fieldLabel = (f: string) => FIELD_LABELS[f] ?? f;

/** "upper back" -> "Upper back". */
export const cap = (s: string) => (s ? s[0].toUpperCase() + s.slice(1) : s);

const clamp = (v: number, lo: number, hi: number) => Math.min(hi, Math.max(lo, v));

/** Adds the item to a comma list, or takes it out when it is there. */
export function toggleIn(csv: string, item: string): string {
  const list = splitList(csv);
  return (list.includes(item) ? list.filter((x) => x !== item) : [...list, item]).join(',');
}

// ---------------------------------------------------------------- blocks

export const withSets = (be: BlockExercise, v: number | null): BlockExercise => ({ ...be, sets: clamp(Math.trunc(v ?? 1), 1, 10) });

/** Min reps 1..50; max follows up so the range stays valid. */
export function withMinReps(be: BlockExercise, v: number | null): BlockExercise {
  const m = clamp(Math.trunc(v ?? 1), 1, 50);
  return { ...be, minReps: m, maxReps: Math.max(be.maxReps, m) };
}

/** Max reps 1..50; min follows down so the range stays valid. */
export function withMaxReps(be: BlockExercise, v: number | null): BlockExercise {
  const m = clamp(Math.trunc(v ?? 1), 1, 50);
  return { ...be, maxReps: m, minReps: Math.min(be.minReps, m) };
}

/** Slot for a new exercise in a block: A when free, else B. Null when the block is full. */
export function freeSlot(mine: BlockExercise[]): number | null {
  return [0, 1].find((s) => !mine.some((be) => be.slot === s)) ?? null;
}

/** The two rows to save when the row at `index` of an ordered list moves one place up (-1) or down (+1). */
export function moveInOrder<T extends { sortOrder: number }>(list: T[], index: number, dir: -1 | 1): T[] {
  const j = index + dir;
  if (j < 0 || j >= list.length) return [];
  return [
    { ...list[index], sortOrder: j },
    { ...list[j], sortOrder: index },
  ];
}

// ---------------------------------------------------------------- exercises

export const newExercise = (id: string): Exercise => ({ ...Defaults.exercise, id, name: '', kind: Kind.STRENGTH, primaryMuscle: 'chest' });

export const asStrength = (ex: Exercise): Exercise => ({ ...ex, kind: Kind.STRENGTH, primaryMuscle: ex.primaryMuscle ?? 'chest' });

export const asCardio = (ex: Exercise, firstModalityId: string | null): Exercise => ({
  ...ex,
  kind: Kind.CARDIO,
  modalityId: ex.modalityId ?? firstModalityId,
  intensity: ex.intensity ?? Intensity.LOW,
  fields: ex.fields || 'time,distance,avgHr',
});

/** Rounds of an interval exercise: 1..30. */
export const clampRounds = (v: number | null) => clamp(Math.trunc(v ?? 1), 1, 30);

/** Turning intervals on fills an empty structure with 4 x 4 min work, 3 min rest. */
export const withIntervals = (ex: Exercise, on: boolean): Exercise => ({
  ...ex,
  intervals: on,
  rounds: on && ex.rounds === 0 ? 4 : ex.rounds,
  workSec: on && ex.workSec === 0 ? 240 : ex.workSec,
  restSec: on && ex.restSec === 0 ? 180 : ex.restSec,
});

export interface LibraryFilter {
  query: string;
  /** '' for all, else strength or cardio. */
  kind: string;
  /** '' for any muscle. */
  muscle: string;
  showArchived: boolean;
}

/** Library list: kind, muscle (primary or secondary), name search without case, archived only on request. */
export function filterLibrary(list: Exercise[], f: LibraryFilter): Exercise[] {
  const q = f.query.trim().toLowerCase();
  return list.filter(
    (e) =>
      (f.showArchived || !e.archived) &&
      (f.kind === '' || e.kind === f.kind) &&
      (f.muscle === '' || e.primaryMuscle === f.muscle || splitList(e.secondaryMuscles).includes(f.muscle)) &&
      (q === '' || e.name.toLowerCase().includes(q)),
  );
}

/**
 * What a search that found nothing says (iOS ContentUnavailableView.search): "No Results for “row”" and
 * what to try. Without a query only the filters can be to blame.
 */
export function noResults(query: string): { title: string; hint: string } {
  const q = query.trim();
  return q ? { title: `No Results for “${q}”`, hint: 'Check the spelling or try a new search.' } : { title: 'No Results', hint: 'Try another filter.' };
}

/** Whether a text matches a search, ignoring case and the spaces around the query; an empty query matches all. */
export function matchesQuery(text: string, query: string): boolean {
  const q = query.trim().toLowerCase();
  return !q || text.toLowerCase().includes(q);
}

/** Name search without case, for pickers: an empty query keeps everything. */
export const searchByName = <T extends { name: string }>(list: T[], query: string): T[] => list.filter((x) => matchesQuery(x.name, query));

/** A block in the program editor: its exercise names, A first, and its set count. */
export function blockSummary(blockId: string, blockExercises: BlockExercise[], exercises: Exercise[]): { title: string; sets: number } {
  const mine = blockExercises.filter((be) => be.blockId === blockId).sort((a, b) => a.slot - b.slot);
  const names = mine.flatMap((be) => exercises.find((e) => e.id === be.exerciseId)?.name ?? []);
  return { title: names.join(' · '), sets: mine.reduce((n, be) => n + be.sets, 0) };
}

/** Sort order for a new block: after the highest one, since deletes leave gaps a count would collide with. */
export const nextSortOrder = (list: { sortOrder: number }[]) => list.reduce((m, x) => Math.max(m, x.sortOrder + 1), 0);

/** Name of a modality, or empty when it is gone. */
export const modalityName = (modalityId: string | null, modalities: Modality[]) => modalities.find((m) => m.id === modalityId)?.name ?? '';
