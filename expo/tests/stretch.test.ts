/** Ports of StretchDemoTest.kt, StretchRoutineStepsTest.kt and DemoClockTest.kt, plus the player and editor rules. */
import { describe, expect, test } from 'bun:test';
import {
  activeRoutine, clampReps, clampStretchSeconds, DemoClock, entrySeconds, exerciseFrame, forDemoSide, guideFor, nearestStep, newRoutine, newStretch, playerAction,
  playerProgress, playerStatus, previewFrame, repMotion, routineOf, routineRun, routineSeconds, routineSteps, routineSummary, sideLine, stepAt, StretchDemo, timelineFill,
  stretchLine, stretchSessionFor, withRepsMode,
} from '../src/domain/stretch';
import { begin, finished, skip, tick, timerState } from '../src/domain/timer';
import { Defaults, RoutineStretch, Stretch } from '../src/domain/types';

const guide: StretchDemo = {
  name: 'Test', frameCount: 64, columns: 8, frameWidth: 384, frameHeight: 304, loopStart: 32, moving: true, repHoldFraction: 0,
  steps: [
    { title: 'Set up', instruction: 'Left leg', frame: 0 },
    { title: 'Move', instruction: 'Move left', frame: 32 },
    { title: 'Hold', instruction: 'Hold', frame: 63 },
  ],
};

describe('StretchDemo (StretchDemoTest)', () => {
  test('preview starts in setup and pauses at each instruction', () => {
    expect([0, 2500, 3400, 4400, 7600, 7800].map((ms) => previewFrame(guide, ms))).toEqual([0, 0, 16, 32, 63, 0]);
  });

  test('all preview frames stay inside the sheet', () => {
    for (let ms = 0; ms <= 20000; ms += 33) expect(previewFrame(guide, ms)).toBeWithin(0, 64);
  });

  test('instruction describes the movement being demonstrated', () => {
    expect([0, 1, 32, 33].map((f) => stepAt(guide, f))).toEqual([0, 1, 1, 2]);
  });

  test('the timeline fills as the pose moves from one step into the next', () => {
    expect([0, 16, 32, 47.5, 63].map((f) => timelineFill(guide, f))).toEqual([0, 0.25, 0.5, 0.75, 1]);
    expect(timelineFill({ ...guide, steps: [guide.steps[0]] }, 40)).toBe(0);
  });

  test('exercise repetitions skip the introductory setup', () => {
    expect([exerciseFrame(guide, 0), exerciseFrame(guide, 1), exerciseFrame(guide, -1)]).toEqual([32, 63, 32]);
  });

  test('timed holds show the held position', () => {
    expect(exerciseFrame(guide, null)).toBe(63);
  });

  test('guides without pause metadata keep their original repetition timing', () => {
    const legacy = { ...guide, steps: [{ title: 'Move', instruction: 'Move left', frame: 32 }] };
    expect([0, 0.25, 0.5, 0.75, 1].map((p) => exerciseFrame(legacy, p))).toEqual([32, 40, 48, 55, 63]);
  });

  test('elephant pauses at each side then moves through the middle', () => {
    const paused = { ...guide, repHoldFraction: 0.2 };
    for (const p of [-1, 0, 0.1, 0.2]) expect(exerciseFrame(paused, p)).toBe(32);
    for (const p of [0.8, 0.9, 1, 2]) expect(exerciseFrame(paused, p)).toBe(63);
    expect(exerciseFrame(paused, 0.3)).toBeGreaterThan(32);
    expect(exerciseFrame(paused, 0.5)).toBe(48);
    expect(exerciseFrame(paused, 0.7)).toBeLessThan(63);
    expect(exerciseFrame(paused, null)).toBe(63);
  });

  test('both repetition styles advance without reversing or leaving the movement frames', () => {
    for (const demo of [guide, { ...guide, repHoldFraction: 0.2 }]) {
      const frames = Array.from({ length: 1001 }, (_, i) => exerciseFrame(demo, i / 1000));
      expect(frames.every((f) => f >= 32 && f <= 63)).toBe(true);
      expect(frames.slice(1).every((f, i) => f >= frames[i])).toBe(true);
      expect([...new Set(frames)]).toEqual(Array.from({ length: 32 }, (_, i) => 32 + i));
    }
  });

  test('right side reverses instructions without damaging other words', () => {
    expect(forDemoSide('Left hand under your right arm. Stay upright.', true)).toBe('Right hand under your left arm. Stay upright.');
    expect(forDemoSide('Left hand', false)).toBe('Left hand');
  });

  test('every bundled guide has a name, steps inside its sheet and a known figure', () => {
    for (const key of ['elephant', 'fold', 'wgs', 'needle', 'hipflexor', 'ninety', 'shoulderir', 'catcow', 'plow']) {
      const g = guideFor(key);
      expect(g?.steps.every((s) => s.frame >= 0 && s.frame < g.frameCount)).toBe(true);
    }
    expect(guideFor('nope')).toBeNull();
  });
});

describe('DemoClock (DemoClockTest)', () => {
  // Whole nanoseconds per 120 Hz frame, as in the Kotlin test: the last frame lands at 7799 ms, still on the hold.
  const vsync = Math.floor(1e9 / 120) / 1e6;
  const clockGuide = { ...guide, frameWidth: 512, frameHeight: 400, steps: guide.steps.map((s) => ({ ...s, instruction: '' })) };

  test('first frame starts where the demo stopped', () => {
    expect(new DemoClock(4200).elapsedMs(987_654.321)).toBe(4200);
  });

  test('ten seconds of 120 Hz frames do not drift', () => {
    const clock = new DemoClock(0);
    let last = 0;
    for (let i = 0; i <= 1200; i++) last = clock.elapsedMs(5_000_000 + i * vsync);
    expect(last).toBeWithin(9999, 10001);
  });

  test('a 120 Hz clock shows every sprite frame in order and changes it at most 20 times a second', () => {
    const clock = new DemoClock(0);
    const changes: [number, number][] = [];
    let shown = -1;
    for (let i = 0; i <= (7800 * 120) / 1000; i++) {
      const ms = clock.elapsedMs(i * vsync);
      const frame = previewFrame(clockGuide, ms);
      if (frame !== shown) {
        changes.push([ms, frame]);
        shown = frame;
      }
    }
    const frames = changes.map((c) => c[1]);
    expect([...new Set(frames)]).toEqual(Array.from({ length: 64 }, (_, i) => i));
    expect(frames.slice(1).every((f, i) => f === frames[i] + 1)).toBe(true);
    expect(changes.slice(1).every((c, i) => c[0] - changes[i][0] >= 40)).toBe(true);
  });
});

const stretch = (p: Partial<Stretch>): Stretch => ({ ...Defaults.stretch, id: 'stretch-1', name: 'S', figure: 'hipflexor', ...p });
const entry = (p: Partial<RoutineStretch> = {}): RoutineStretch => ({ ...Defaults.routineStretch, id: 'entry-1', routineId: 'routine-1', stretchId: 'stretch-1', sortOrder: 0, ...p });

describe('routine steps (StretchRoutineStepsTest)', () => {
  test('routine duration accepts punctuation in a stretch name', () => {
    expect(routineSeconds('routine-1', [entry()], [stretch({ name: 'Audit|pipe', seconds: 45 })])).toBe(45);
  });

  test('phase data preserves punctuation and both sides', () => {
    const st = stretch({ name: 'Left|right, breathe', sided: true, seconds: 45 });
    const phases = routineSteps('routine-1', [entry()], [st]);
    expect(JSON.parse(JSON.stringify(phases))).toEqual(phases);
    expect(phases.map((p) => p.side)).toEqual(['Left', 'Right']);
    expect(phases.map((p) => p.label)).toEqual(['Left|right, breathe', 'Left|right, breathe']);
    expect(routineSeconds('routine-1', [entry()], [st])).toBe(90);
  });

  test('order, routine seconds, reps, missing stretches and the summary', () => {
    const entries = [entry({ id: 'b', stretchId: 'two', sortOrder: 1, seconds: 30 }), entry({ id: 'a', sortOrder: 0 }), entry({ id: 'c', stretchId: 'gone', sortOrder: 2 })];
    const list = [stretch({ seconds: 60 }), stretch({ id: 'two', name: 'Cat cow', mode: 'reps', reps: 10 })];
    const phases = routineSteps('routine-1', entries, list);
    expect(phases.map((p) => [p.label, p.seconds, p.reps])).toEqual([['S', 60, 0], ['Cat cow', 30, 10]]);
    expect(routineSummary('routine-1', entries, list)).toBe('2 min · 3 stretches');
    expect(stretchLine(stretch({ sided: true, seconds: 45 }))).toBe('per side · 45 s');
    expect(stretchLine(stretch({ mode: 'reps', reps: 12 }))).toBe('12 reps');
    expect(withRepsMode(stretch({}), true)).toMatchObject({ mode: 'reps', reps: 10 });
  });
});

describe('player', () => {
  const run = () =>
    timerState({
      sessionId: 'routine-1:x', title: 'Daily', stretch: true, startedAt: 7,
      phases: routineSteps('routine-1', [entry()], [stretch({ name: 'Hip flexor', sided: true, seconds: 40 })]),
      remaining: 40,
    });

  test('words for each state', () => {
    const s = run();
    const p = s.phases[0];
    expect([sideLine(p), playerStatus(s, p), playerAction(s, p), playerProgress(s)]).toEqual(['Left · Hold', 'Ready · 0:40', 'Start left side', '1 of 2 · 1:20 remaining']);
    const going = tick(begin(s), 4);
    expect([playerStatus(going, p), playerAction(going, p)]).toEqual(['Hold', 'Pause']);
    const next = tick(begin(s), 3 + 40);
    expect([playerStatus(next, next.phases[1]), playerAction(next, next.phases[1])]).toEqual(['Next stretch · 0:40', 'Start right side']);
  });

  test('rep motion goes there and back once per rep', () => {
    const p = { ...run().phases[0], seconds: 20, reps: 10 };
    expect([20, 19.5, 19, 18].map((r) => repMotion(p, r))).toEqual([0, 0.5, 1, 0]);
  });

  test('a run is a session after one stretch or 30 s, never from skipping', () => {
    expect(stretchSessionFor(finished(skip(run())), '2026-10-06')).toBeNull();
    const one = tick(tick(begin(run()), 3), 40);
    expect(stretchSessionFor(one, '2026-10-06')).toEqual({
      id: 'routine-1:x', routineId: 'routine-1', routineName: 'Daily', date: '2026-10-06', startedAt: 7, totalSec: 40, completed: 1, skipped: 0,
    });
  });
});

describe('runs and editors', () => {
  const run = (sessionId: string) => timerState({ sessionId, title: 'Daily', stretch: true, phases: routineSteps('daily', [entry({ routineId: 'daily' })], [stretch({})]) });

  test('a run belongs to the routine before the colon, also while it ends; cardio timers never do', () => {
    expect(routineOf('daily:ab12cd34')).toBe('daily');
    expect(routineRun(run('daily:ab12cd34'), 'daily')).not.toBeNull();
    expect(routineRun(finished(run('daily:ab12cd34')), 'daily')).not.toBeNull();
    expect(routineRun(run('other:ab12cd34'), 'daily')).toBeNull();
    expect(routineRun({ ...run('daily:x'), stretch: false }, 'daily')).toBeNull();
    expect(routineRun(null, 'daily')).toBeNull();
  });

  test('during the stretch the instruction follows the step nearest the shown frame', () => {
    expect([0, 15, 17, 40, 63].map((f) => nearestStep(guide, f))).toEqual([0, 0, 1, 1, 2]);
  });

  test('Today offers the active routine, else the first', () => {
    const r = (id: string, active: boolean) => ({ id, name: id, active, sortOrder: 0 });
    expect(activeRoutine([r('a', false), r('b', true)])?.id).toBe('b');
    expect(activeRoutine([r('a', false)])?.id).toBe('a');
    expect(activeRoutine([])).toBeNull();
  });

  test('editor limits, a new stretch, and routine seconds over the stretch default', () => {
    expect([clampStretchSeconds(null), clampStretchSeconds(1), clampStretchSeconds(900), clampStretchSeconds(42.7)]).toEqual([45, 5, 600, 42]);
    expect([clampReps(null), clampReps(0), clampReps(80)]).toEqual([1, 1, 50]);
    expect(newStretch('n1')).toMatchObject({ id: 'n1', name: '', figure: 'hipflexor', mode: 'hold', seconds: 45, sided: false, builtin: false });
    expect(entrySeconds(entry({ seconds: 30 }), stretch({ seconds: 60 }))).toBe(30);
    expect(entrySeconds(entry(), stretch({ seconds: 60 }))).toBe(60);
    expect(entrySeconds(entry(), undefined)).toBe(45);
  });

  test('a new routine goes after the others and is active only as the first', () => {
    expect(newRoutine('r', [])).toEqual({ id: 'r', name: 'New routine', active: true, sortOrder: 0 });
    const routines = [{ id: 'a', name: 'A', active: true, sortOrder: 0 }, { id: 'b', name: 'B', active: false, sortOrder: 4 }];
    expect(newRoutine('r', routines)).toEqual({ id: 'r', name: 'New routine', active: false, sortOrder: 5 });
  });
});
