import {
  Block, BlockExercise, Defaults, EatingWindow, Exercise, Intensity, ItemType, Kind, Modality, Program,
  RoutineStretch, Stretch, StretchMode, StretchRoutine, WeekPlan,
} from './types';

/** Built-in data, the same ids and values as Seed in the Kotlin app (data/Repository.kt). */

const strength = (id: string, name: string, primary: string, secondary = '', step = 2.5): Exercise => ({
  ...Defaults.exercise, id, name, kind: Kind.STRENGTH, primaryMuscle: primary, secondaryMuscles: secondary, stepKg: step, builtin: true,
});

const cardio = (id: string, name: string, fields: Partial<Exercise>): Exercise => ({
  ...Defaults.exercise, id, name, kind: Kind.CARDIO, builtin: true, ...fields,
});

export const SEED_MODALITIES: Modality[] = [
  { id: 'bike', name: 'Bike', color: 0xffb39dff, archived: false },
  { id: 'run', name: 'Run', color: 0xff7ce0a8, archived: false },
  { id: 'swim', name: 'Swim', color: 0xff71b7ff, archived: false },
  { id: 'row', name: 'Row', color: 0xffff8fa3, archived: false },
];

export const SEED_EXERCISES: Exercise[] = [
  strength('bench', 'Barbell Bench Press', 'chest', 'shoulders,triceps'),
  strength('legpress', 'Leg Press', 'quads', 'glutes', 5.0),
  strength('shoulderpress', 'Shoulder Press', 'shoulders', 'triceps'),
  strength('cablerow', 'Cable Seated Row', 'upper back', 'lats,biceps'),
  strength('lunge', 'Walking Lunge', 'quads', 'glutes,hamstrings'),
  strength('latpull', 'Lat Pulldown', 'lats', 'biceps,upper back'),
  strength('latraise', 'Lateral Raise', 'shoulders', '', 1.0),
  strength('legcurl', 'Leg Curl', 'hamstrings', '', 5.0),
  strength('inclinedb', 'Incline DB Press', 'chest', 'shoulders,triceps', 2.0),
  strength('calf', 'Calf Raises', 'calves', '', 5.0),
  strength('curl', 'Biceps Curl', 'biceps', 'forearms', 1.0),
  strength('triext', 'Triceps Extension', 'triceps', '', 2.5),
  strength('rdl', 'Romanian Deadlift', 'hamstrings', 'glutes,lower back'),
  strength('crunch', 'Crunch', 'core', '', 2.5),
  strength('hyperext', 'Hyperextension', 'lower back', 'glutes,hamstrings'),
  strength('goblet', 'Goblet Squat', 'quads', 'glutes,core', 2.0),
  strength('dbrow', 'Dumbbell Row', 'upper back', 'lats,biceps', 2.0),
  strength('pushup', 'Push-up', 'chest', 'triceps,shoulders', 0.0),
  strength('hipthrust', 'Hip Thrust', 'glutes', 'hamstrings', 5.0),
  strength('facepull', 'Face Pull', 'upper back', 'shoulders', 2.5),
  strength('plank', 'Plank', 'core', '', 0.0),
  cardio('nor4x4', 'Norwegian 4x4', {
    modalityId: 'bike', intensity: Intensity.HIGH, intervals: true, workSec: 240, restSec: 180, rounds: 4, fields: 'time,avgHr,watts',
  }),
  cardio('z2run', 'Zone 2 run', { modalityId: 'run', intensity: Intensity.LOW, fields: 'distance,time,avgHr' }),
  cardio('z2swim', 'Zone 2 swim', { modalityId: 'swim', intensity: Intensity.LOW, fields: 'poolLength,laps,time,stroke' }),
  cardio('tabata', 'Tabata bike', {
    modalityId: 'bike', intensity: Intensity.HIGH, intervals: true, workSec: 20, restSec: 10, rounds: 8, fields: 'time,avgHr,watts',
  }),
];

export const SEED_PROGRAM: Program = { id: 'tpv3', name: 'TP v3', active: true, sortOrder: 0 };

const PAIRS: [string, string][] = [
  ['bench', 'legpress'], ['shoulderpress', 'cablerow'], ['lunge', 'latpull'], ['latraise', 'legcurl'],
  ['inclinedb', 'calf'], ['curl', 'triext'], ['rdl', 'crunch'],
];

export const SEED_BLOCKS: Block[] = PAIRS.map((_, i) => ({ id: `tpv3-b${i}`, programId: 'tpv3', sortOrder: i, superset: true }));

export const SEED_BLOCK_EXERCISES: BlockExercise[] = PAIRS.flatMap(([a, b], i) => [
  { ...Defaults.blockExercise, id: `tpv3-b${i}-a`, blockId: `tpv3-b${i}`, slot: 0, exerciseId: a },
  { ...Defaults.blockExercise, id: `tpv3-b${i}-b`, blockId: `tpv3-b${i}`, slot: 1, exerciseId: b },
]);

export const SEED_WEEK_PLAN: WeekPlan[] = [
  { weekday: 1, itemType: ItemType.CARDIO, itemId: 'z2swim' },
  { weekday: 2, itemType: ItemType.PROGRAM, itemId: 'tpv3' },
  { weekday: 3, itemType: ItemType.CARDIO, itemId: 'z2run' },
  { weekday: 4, itemType: ItemType.CARDIO, itemId: 'nor4x4' },
  { weekday: 5, itemType: ItemType.CARDIO, itemId: 'z2swim' },
  { weekday: 6, itemType: ItemType.PROGRAM, itemId: 'tpv3' },
  { weekday: 7, itemType: ItemType.REST, itemId: null },
];

const st = (
  id: string, name: string, sided: boolean, mode: string, seconds: number, reps: number,
  figure: string, muscles: string, cue: string, link: string,
): Stretch => ({
  id, name, sided, mode, seconds, reps, figure, muscles, cue, link: 'https://www.youtube.com/watch?v=' + link, builtin: true, archived: false,
});

/** The owner's daily mobility routine (2026-09-12). Seconds are per side for sided stretches. */
export const SEED_STRETCHES: Stretch[] = [
  st('elephant', 'Elephant walks', false, StretchMode.REPS, 60, 10, 'elephant', 'hamstrings', 'Hands on the floor, pedal the heels down one at a time', 'bIS8e2ZI-u0'),
  st('palmsfloor', 'Hamstring stretch, palms on ground', false, StretchMode.HOLD, 45, 0, 'fold', 'hamstrings,lower back', 'Fold from the hips, reach the palms to the floor', '2nfsR9PC7hQ'),
  st('wgs', "World's greatest stretch", true, StretchMode.REPS, 40, 5, 'wgs', 'glutes,quads,upper back', 'Lunge, elbow to instep, rotate and reach up', '7XheaZERvBQ'),
  st('needle', 'Thread the needle', true, StretchMode.HOLD, 30, 0, 'needle', 'upper back,shoulders', 'On all fours, slide one arm under and rest the shoulder down', 'gyew25Vaqj8'),
  st('hipflexor', 'Hip flexor stretch', true, StretchMode.HOLD, 45, 0, 'hipflexor', 'quads,glutes', 'Half kneel, tuck the pelvis, squeeze the back glute', 'ktgtEWGhFd8'),
  st('ninety', '90/90 stretch', true, StretchMode.HOLD, 45, 0, 'ninety', 'glutes', 'Both knees at 90, sit tall, lean over the front shin', 'FM7-7-a0FLg'),
  st('shoulderir', 'Shoulder internal rotation', true, StretchMode.HOLD, 30, 0, 'shoulderir', 'shoulders', 'Hand behind the back, walk it up the spine', '3Av8-RLNvDk'),
  st('catcow', 'Cat cow', false, StretchMode.REPS, 40, 10, 'catcow', 'lower back,core', 'Round the spine on the exhale, arch on the inhale', '2of247Kt0tU'),
  st('plow', 'Plow pose', false, StretchMode.HOLD, 45, 0, 'plow', 'lower back,hamstrings', 'Legs over the head, toes toward the floor, breathe', 'IoURFlXOuqg'),
];

export const SEED_ROUTINE: StretchRoutine = { id: 'daily', name: 'Daily mobility', active: true, sortOrder: 0 };

export const SEED_ROUTINE_STRETCHES: RoutineStretch[] = SEED_STRETCHES.map((s, i) => ({
  id: 'daily-' + s.id, routineId: 'daily', stretchId: s.id, sortOrder: i, seconds: null,
}));

export const SEED_EATING_WINDOW: EatingWindow = { ...Defaults.eatingWindow };
