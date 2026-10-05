import { describe, expect, test } from 'bun:test';
import { dur, preview } from '../src/domain/format';
import { itemFor } from '../src/domain/plan';
import { SEED_BLOCK_EXERCISES, SEED_BLOCKS, SEED_EXERCISES, SEED_MODALITIES, SEED_PROGRAM, SEED_WEEK_PLAN } from '../src/domain/seed';
import { buildSetLogs, epley, previousByKey, ProgramDetail, setKey } from '../src/domain/session';
import { Block, Defaults, Exercise, SetLog, SetStatus } from '../src/domain/types';

const byId = new Map(SEED_EXERCISES.map((e) => [e.id, e]));
const tpv3: ProgramDetail = {
  program: SEED_PROGRAM,
  blocks: SEED_BLOCKS.map((block) => ({
    block,
    exercises: SEED_BLOCK_EXERCISES.filter((be) => be.blockId === block.id).map((be) => ({ be, exercise: byId.get(be.exerciseId)! })),
  })),
};

let n = 0;
const ids = () => `id${n++}`;

describe('buildSetLogs', () => {
  test('a superset alternates A1 B1 A2 B2 and orders across blocks', () => {
    const logs = buildSetLogs(tpv3, 's1', new Map(), ids);
    expect(logs.length).toBe(7 * 2 * 3);
    expect(logs.slice(0, 6).map((l) => `${l.exerciseId}${l.setIndex}`)).toEqual(['bench0', 'legpress0', 'bench1', 'legpress1', 'bench2', 'legpress2']);
    expect(logs.map((l) => l.order)).toEqual(logs.map((_, i) => i));
    expect(logs[6].blockIndex).toBe(1);
    expect(logs.every((l) => l.status === SetStatus.PENDING && l.blockSuperset)).toBe(true);
  });

  test('a straight block runs every set of A before B, and uneven supersets skip missing sets', () => {
    const a: Exercise = { ...Defaults.exercise, id: 'a', name: 'A', kind: 'strength' };
    const b: Exercise = { ...Defaults.exercise, id: 'b', name: 'B', kind: 'strength', stepKg: 5 };
    const be = (id: string, ex: string, slot: number, sets: number) => ({ id, blockId: 'x', slot, exerciseId: ex, sets, minReps: 8, maxReps: 12 });
    const straight: Block = { id: 'x', programId: 'p', sortOrder: 0, superset: false };
    const detail = (block: Block, setsA: number, setsB: number): ProgramDetail => ({
      program: { id: 'p', name: 'P', active: true, sortOrder: 0 },
      blocks: [{ block, exercises: [{ be: be('1', 'a', 0, setsA), exercise: a }, { be: be('2', 'b', 1, setsB), exercise: b }] }],
    });
    expect(buildSetLogs(detail(straight, 2, 2), 's', new Map(), ids).map((l) => l.exerciseId + l.setIndex)).toEqual(['a0', 'a1', 'b0', 'b1']);
    const uneven = buildSetLogs(detail({ ...straight, superset: true }, 3, 1), 's', new Map(), ids);
    expect(uneven.map((l) => l.exerciseId + l.setIndex)).toEqual(['a0', 'b0', 'a1', 'a2']);
    expect(uneven[1].stepKg).toBe(5);
  });

  test('prefills from the newest done set with the same key, else reps = min reps', () => {
    const done = (sessionId: string, exerciseId: string, setIndex: number, weightKg: number, reps: number, status: string = SetStatus.DONE): SetLog => ({
      ...Defaults.setLog, id: `${sessionId}-${exerciseId}-${setIndex}`, sessionId, blockIndex: 0, blockSuperset: true, slot: 0, setIndex, order: 0,
      exerciseId, exerciseName: exerciseId, minReps: 8, maxReps: 12, stepKg: 2.5, weightKg, reps, status,
    });
    const previous = previousByKey([
      { startedAt: 1, logs: [done('old', 'bench', 0, 50, 8), done('old', 'bench', 1, 50, 8)] },
      { startedAt: 2, logs: [done('new', 'bench', 0, 60, 10), done('new', 'bench', 1, 70, 3, SetStatus.SKIPPED)] },
    ]);
    expect(previous.get('bench:0')?.weightKg).toBe(60);
    expect(previous.get('bench:1')?.weightKg).toBe(50); // skipped sets are not a reference

    const logs = buildSetLogs(tpv3, 's2', previous, ids);
    const bench0 = logs.find((l) => setKey(l) === 'bench:0')!;
    expect([bench0.weightKg, bench0.reps]).toEqual([60, 10]);
    const leg0 = logs.find((l) => setKey(l) === 'legpress:0')!;
    expect([leg0.weightKg, leg0.reps]).toEqual([null, 8]);
  });

  test('epley', () => {
    expect(epley(100, 0)).toBe(100);
    expect(epley(60, 10)).toBeCloseTo(80, 10);
  });
});

describe('preview', () => {
  const plan = { plan: SEED_WEEK_PLAN, overrides: [], programs: [SEED_PROGRAM], exercises: SEED_EXERCISES };
  const line = (date: string) => preview(itemFor(plan, date), SEED_BLOCKS, SEED_BLOCK_EXERCISES, SEED_MODALITIES);

  test('program, interval cardio, steady cardio and rest', () => {
    expect(line('2026-09-22')).toBe('7 blocks · 42 sets'); // Tuesday, TP v3
    expect(line('2026-09-24')).toBe('4 × 4 min on bike, 3 min rest'); // Thursday, Norwegian 4x4
    expect(line('2026-09-23')).toBe('Steady on run'); // Wednesday, Zone 2 run
    expect(line('2026-09-27')).toBe('Rest day'); // Sunday
  });

  test('dur', () => {
    expect([dur(240), dur(45), dur(90), dur(20)]).toEqual(['4 min', '45 s', '1:30', '20 s']);
  });
});
