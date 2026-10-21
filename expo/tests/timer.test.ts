/** Ports of TimerStateTest.kt, TimerSnapshotTest.kt and CueGateTest.kt, plus the cue and alert rules. */
import { describe, expect, test } from 'bun:test';
import {
  advanced, begin, buildPhases, countdownText, CueGate, currentPhase, decodeSnapshot, elapsedLine, encodeSnapshot, expired, finished, isDone, isPreparing,
  mmss, nextLine, pause, phase, PhaseKind, phaseSpeech, phaseTone, resume, roundLine, skip, tick, timerLabel, TimerState, timerState, transitionCues,
  upcomingAlerts, workRemaining,
} from '../src/domain/timer';

const stretch5 = () =>
  timerState({
    sessionId: 'routine:test',
    title: 'Mobility',
    phases: [phase(PhaseKind.WORK, 5, 0, { side: 'Left' }), phase(PhaseKind.WORK, 5, 0, { side: 'Right' }), phase(PhaseKind.WORK, 5, 0)],
    remaining: 5,
    stretch: true,
  });

describe('TimerState (TimerStateTest)', () => {
  test('opening a stretch waits without consuming time', () => {
    const s = stretch5();
    expect(s.awaitingStart).toBe(true);
    expect(tick(s, 30)).toEqual(s);
  });

  test('start counts three two one before consuming exercise time', () => {
    let s = begin(stretch5());
    expect(s.preparation).toBe(3);
    for (const n of [2, 1, 0]) {
      s = tick(s, 1);
      expect(s.preparation).toBe(n);
      expect(s.remaining).toBe(5);
      expect(s.elapsed).toBe(0);
    }
    expect(tick(s, 1).remaining).toBe(4);
  });

  test('side changes and following exercises wait for a tap', () => {
    let s = tick(tick(begin(stretch5()), 3), 5.5);
    expect(s.index).toBe(1);
    expect(s.completed).toBe(1);
    expect(s.awaitingStart).toBe(true);
    expect(s.remaining).toBe(5);
    expect(tick(s, 120)).toEqual(s);
    s = tick(tick(begin(s), 3), 5);
    expect(s.index).toBe(2);
    expect(s.awaitingStart).toBe(true);
  });

  test('pause freezes preparation and resume preserves it', () => {
    const s = { ...tick(begin(stretch5()), 1), paused: true };
    expect(tick(s, 20)).toEqual(s);
    expect(tick({ ...s, paused: false }, 1).preparation).toBe(1);
  });

  test('double start does not reset countdown or exercise', () => {
    const preparing = tick(begin(stretch5()), 1);
    expect(begin(preparing)).toEqual(preparing);
    const running = tick(tick(preparing, 2), 1);
    expect(begin(running)).toEqual(running);
  });

  test('skip never starts another stretch or inflates completed count', () => {
    const s = skip(tick(tick(begin(stretch5()), 3), 2));
    expect(s.awaitingStart).toBe(true);
    expect(s.completed).toBe(0);
    expect(s.skipped).toBe(1);
    expect(s.elapsed).toBe(2);
    expect(workRemaining(s)).toBe(10);
  });

  test('only completed work counts when stopping early', () => {
    const s = tick(tick(begin(tick(tick(begin(stretch5()), 3), 5)), 3), 2);
    expect(s.completed).toBe(1);
    expect(s.elapsed).toBe(7);
  });

  test('last stretch completes once with no trailing preparation', () => {
    let s = stretch5();
    for (let i = 0; i < 3; i++) s = tick(tick(begin(s), 3), 5);
    expect(isDone(s)).toBe(true);
    expect(s.completed).toBe(3);
    expect(tick(s, 10)).toEqual(s);
    expect(skip(s)).toEqual(s);
  });

  test('cardio phases still advance automatically and account for delayed ticks', () => {
    let s = timerState({ sessionId: 'cardio', title: 'Intervals', phases: buildPhases(0, 5, 2, 2), remaining: 5 });
    expect(s.awaitingStart).toBe(false);
    s = tick(s, 6);
    expect(currentPhase(s).kind).toBe(PhaseKind.REST);
    expect(s.remaining).toBe(1);
    s = tick(s, 8);
    expect(isDone(s)).toBe(true);
    expect(s.elapsed).toBe(12);
  });

  test('test speed does not accelerate the three second preparation', () => {
    const s = tick(begin({ ...stretch5(), speed: 20 }), 1);
    expect(s.preparation).toBe(2);
    expect(s.elapsed).toBe(0);
  });

  test('pause and resume leave a waiting or finished timer alone', () => {
    const ready = stretch5();
    expect(pause(ready)).toEqual(ready);
    const running = tick(begin(ready), 4);
    expect(resume(pause(running))).toEqual(running);
    expect(pause(finished(running)).paused).toBe(false);
  });
});

describe('TimerSnapshot (TimerSnapshotTest)', () => {
  const t0 = 1_760_000_000_000;
  const sec = (s: number) => t0 + s * 1000;
  // Warm-up 60, then work 240 / rest 180 for 4 rounds: 60 + 4*240 + 3*180 = 1560 s.
  const cardio = () => timerState({ sessionId: 'cardio', title: 'Norwegian 4x4', phases: buildPhases(60, 240, 180, 4), remaining: 60, awaitingStart: false });
  const stretch = () =>
    timerState({
      sessionId: 'routine:test',
      title: 'Mobility',
      phases: [phase(PhaseKind.WORK, 30, 0, { side: 'Left' }), phase(PhaseKind.WORK, 30, 0, { side: 'Right' })],
      remaining: 30,
      stretch: true,
    });

  test('a snapshot expires an hour after the timer would have ended', () => {
    const snap = { state: cardio(), savedAt: sec(0) };
    expect(expired(snap, sec(1560 + 3600))).toBe(false);
    expect(expired(snap, sec(1560 + 3601))).toBe(true);
    const stretchSnap = { state: stretch(), savedAt: sec(0) };
    expect(expired(stretchSnap, sec(60 + 3600))).toBe(false);
    expect(expired(stretchSnap, sec(60 + 3601))).toBe(true);
  });

  test('a finished snapshot never expires, so its result is still written', () => {
    expect(expired({ state: finished(cardio()), savedAt: sec(0) }, sec(30 * 24 * 3600))).toBe(false);
  });

  test('running phase counts down by the wall-clock gap', () => {
    const s = advanced({ state: tick(cardio(), 20), savedAt: sec(0) }, sec(15));
    expect(s.index).toBe(0);
    expect(s.remaining).toBeCloseTo(25, 9);
    expect(s.elapsed).toBeCloseTo(35, 9);
  });

  test('paused and unstarted timers do not move', () => {
    const paused = { ...tick(cardio(), 20), paused: true };
    expect(advanced({ state: paused, savedAt: sec(0) }, sec(3600))).toEqual(paused);
    const ready = { ...cardio(), awaitingStart: true };
    expect(advanced({ state: ready, savedAt: sec(0) }, sec(3600))).toEqual(ready);
  });

  test('cardio crosses several phases like the live timer', () => {
    const s = advanced({ state: cardio(), savedAt: sec(0) }, sec(60 + 240 + 180 + 30));
    expect(s.index).toBe(3);
    expect(currentPhase(s).kind).toBe(PhaseKind.WORK);
    expect(currentPhase(s).round).toBe(2);
    expect(s.remaining).toBeCloseTo(210, 9);
    expect(s.elapsed).toBeCloseTo(510, 9);
    expect(s.awaitingStart).toBe(false);
    expect(s).toEqual(tick(cardio(), 510));
  });

  test('countdown then stretch stops at the next side and waits for Start', () => {
    const s = advanced({ state: begin(stretch()), savedAt: sec(0) }, sec(600));
    expect(s.index).toBe(1);
    expect(s.awaitingStart).toBe(true);
    expect(isPreparing(s)).toBe(false);
    expect(s.remaining).toBeCloseTo(30, 9);
    expect(s.elapsed).toBeCloseTo(30, 9);
    expect(s.completed).toBe(1);
    expect(isDone(s)).toBe(false);
    expect(advanced({ state: s, savedAt: sec(600) }, sec(7200))).toEqual(s);
  });

  test('countdown still running is consumed before exercise time', () => {
    const s = advanced({ state: begin(stretch()), savedAt: sec(0) }, sec(2));
    expect(s.preparation).toBeCloseTo(1, 9);
    expect(s.remaining).toBeCloseTo(30, 9);
    expect(s.elapsed).toBeCloseTo(0, 9);
  });

  test('timers that ran out while the app was closed finish', () => {
    const c = advanced({ state: cardio(), savedAt: sec(0) }, sec(5000));
    expect(isDone(c)).toBe(true);
    expect(c.elapsed).toBeCloseTo(1560, 9);
    const lastSide = begin(tick(tick(begin(stretch()), 3), 30));
    const s = advanced({ state: lastSide, savedAt: sec(0) }, sec(120));
    expect(isDone(s)).toBe(true);
    expect(s.completed).toBe(2);
    expect(s.elapsed).toBeCloseTo(60, 9);
  });

  test('a clock set backwards never rewinds the timer', () => {
    const s = tick(cardio(), 20);
    expect(advanced({ state: s, savedAt: sec(100) }, sec(0))).toEqual(s);
  });

  test('snapshot survives a JSON round trip and rejects junk', () => {
    const snap = { state: { ...tick(begin(stretch()), 1.5), skipped: 1, speed: 2, startedAt: t0 }, savedAt: sec(4) };
    expect(decodeSnapshot(encodeSnapshot(snap))).toEqual(snap);
    const ready = { state: stretch(), savedAt: sec(0) };
    expect(decodeSnapshot(encodeSnapshot(ready))?.state.awaitingStart).toBe(true);
    expect(decodeSnapshot('{not json')).toBeNull();
    expect(decodeSnapshot(null)).toBeNull();
    expect(decodeSnapshot(encodeSnapshot({ state: { ...cardio(), phases: [] }, savedAt: sec(0) }))).toBeNull();
    expect(decodeSnapshot(JSON.stringify({ state: { sessionId: 'x', phases: [{ kind: 'NAP', seconds: 1, round: 0 }] }, savedAt: 0 }))).toBeNull();
    // Fields a newer or older build left out take their defaults.
    expect(decodeSnapshot(JSON.stringify({ state: { sessionId: 'x', phases: [{ kind: 'WORK', seconds: 30, round: 1 }] }, savedAt: 5 }))?.state).toMatchObject({
      index: 0, speed: 1, stretch: false, awaitingStart: false,
    });
  });
});

describe('CueGate (CueGateTest)', () => {
  test('a lone cue speaks at once', () => {
    const gate = new CueGate();
    expect(gate.offer('Go', 1000)).toBe('Go');
    expect(gate.dueAt()).toBeNull();
  });

  test('rapid skips speak only the latest cue after the gap', () => {
    const gate = new CueGate();
    expect(gate.offer('Ready for a', 0)).toBe('Ready for a');
    expect(gate.offer('Ready for b', 100)).toBeNull();
    expect(gate.offer('Ready for c', 200)).toBeNull();
    expect(gate.dueAt()).toBe(300);
    expect(gate.poll(299)).toBeNull();
    expect(gate.poll(300)).toBe('Ready for c');
    expect(gate.poll(900)).toBeNull();
  });

  test('cues spaced wider than the gap all speak', () => {
    const gate = new CueGate();
    expect([0, 1000, 2000].map((t, i) => gate.offer(['3', '2', '1'][i], t))).toEqual(['3', '2', '1']);
  });

  test('no burst ever speaks twice inside the gap', () => {
    const gate = new CueGate();
    const spoken: number[] = [];
    for (let t = 0; t <= 3000; t += 10) {
      if (t % 70 === 0 && gate.offer(`cue ${t}`, t) != null) spoken.push(t);
      if (gate.poll(t) != null) spoken.push(t);
    }
    expect(spoken.slice(1).every((t, i) => t - spoken[i] >= 300)).toBe(true);
    expect(spoken.length).toBeGreaterThanOrEqual(9);
  });

  test('cancel drops the waiting cue and asks for stop only after speech', () => {
    const gate = new CueGate();
    expect(gate.cancel()).toBe(false);
    gate.offer('a', 0);
    gate.offer('b', 100);
    expect(gate.cancel()).toBe(true);
    expect(gate.cancel()).toBe(false);
    expect(gate.poll(400)).toBeNull();
    expect(gate.dueAt()).toBeNull();
  });
});

describe('cues and alerts', () => {
  const cardio = (): TimerState => timerState({ sessionId: 'c', title: 'Norwegian 4x4', phases: buildPhases(60, 240, 180, 2), remaining: 60, awaitingStart: true });

  test('speech per phase', () => {
    const s = cardio();
    expect(phaseSpeech(s)).toBe('Warm up, 1 minute');
    expect(phaseSpeech({ ...s, index: 1 })).toBe('Round 1 of 2. Go');
    expect(phaseSpeech({ ...s, index: 2 })).toBe('Rest, 3 minutes');
    expect(phaseSpeech({ ...stretch5(), index: 1 })).toBe(', right');
    expect(mmss(75.2)).toBe('1:16');
    expect(mmss(-3)).toBe('0:00');
  });

  test('preparation beeps each second, then Go', () => {
    const prep = begin(cardio());
    expect(transitionCues(prep, tick(prep, 0.5))).toEqual([]);
    expect(transitionCues(tick(prep, 0.5), tick(prep, 1.1))).toEqual([{ beep: 'short' }]);
    expect(transitionCues(tick(prep, 2.9), tick(prep, 3.1))).toEqual([{ vibrate: 200 }, { say: 'Go' }]);
  });

  test('three short beeps before a switch, ten seconds, a long beep and the next phase', () => {
    const run = tick(begin(cardio()), 3);
    const at = (left: number) => tick(run, 60 - left);
    expect(transitionCues(at(10.2), at(9.9))).toEqual([{ say: 'ten seconds' }]);
    expect(transitionCues(at(3.1), at(2.9))).toEqual([{ beep: 'short' }]);
    expect(transitionCues(at(1.5), at(1.4))).toEqual([]);
    expect(transitionCues(at(0.1), tick(run, 60.1))).toEqual([{ beep: 'long' }, { say: 'Round 1 of 2. Go' }]);
    const end = tick(run, 60 + 240 + 180 + 240);
    expect(transitionCues(tick(run, 719), end)).toEqual([{ beep: 'long' }, { say: 'All rounds done' }]);
    expect(transitionCues(end, end)).toEqual([]);
  });

  test('alerts for every phase ahead and the end, none while paused or waiting', () => {
    const now = 1_000_000;
    const run = tick(begin(cardio()), 3 + 30);
    const alerts = upcomingAlerts(run, now);
    expect(alerts.map((a) => [a.at - now, a.title])).toEqual([
      [30_000, 'Work 1/2 · Norwegian 4x4'],
      [270_000, 'Rest · Norwegian 4x4'],
      [450_000, 'Work 2/2 · Norwegian 4x4'],
      [690_000, 'Norwegian 4x4'],
    ]);
    expect(alerts.at(-1)?.body).toBe('All rounds done');
    expect(upcomingAlerts(pause(run), now)).toEqual([]);
    expect(upcomingAlerts(cardio(), now)).toEqual([]);
    // During the countdown the first alert waits for it too.
    expect(upcomingAlerts(tick(begin(cardio()), 1), now)[0].at - now).toBe(62_000);
  });
});

describe('timer screen text', () => {
  const ready = timerState({ sessionId: 'c', title: '4x4', phases: buildPhases(60, 240, 180, 2), remaining: 60, awaitingStart: true });

  test('label, tone and countdown follow the state', () => {
    expect([timerLabel(null), timerLabel(ready), phaseTone(ready), countdownText(ready)]).toEqual(['Starting', 'Ready', 'idle', '1:00']);
    const prep = tick(begin(ready), 0.4);
    expect([timerLabel(prep), phaseTone(prep), countdownText(prep)]).toEqual(['Get ready', 'idle', '3']);
    const warm = tick(begin(ready), 3.5);
    expect([timerLabel(warm), phaseTone(warm), countdownText(warm), roundLine(warm)]).toEqual(['Warm-up', 'warmup', '1:00', null]);
    const work = tick(begin(ready), 3 + 70);
    expect([timerLabel(work), phaseTone(work), roundLine(work), nextLine(work)]).toEqual(['Work', 'work', 'Round 1 of 2', 'Next: rest 3:00']);
    expect([timerLabel(pause(work)), phaseTone(pause(work))]).toEqual(['Paused', 'idle']);
    expect(elapsedLine(work)).toBe('Elapsed 1:10 of 12:00');
    const last = tick(begin(ready), 3 + 60 + 240 + 180 + 1);
    expect([phaseTone(last), nextLine(last)]).toEqual(['work', 'Last one']);
    expect(timerLabel(finished(work))).toBe('Done');
  });
});
