/** The number widget's − and +: one step, and the pace of a held button. */
import { describe, expect, test } from 'bun:test';
import { repeatDelay, stepValue } from '../src/domain/stepper';

describe('stepper', () => {
  test('a step lands on round values and never below zero', () => {
    expect(stepValue(60, 1, 2.5)).toBe(62.5);
    expect(stepValue(0.2, 1, 0.1)).toBe(0.3);
    expect(stepValue(1, -1, 2.5)).toBe(0);
    let v = 0;
    for (let i = 0; i < 30; i++) v = stepValue(v, 1, 0.1);
    expect(v).toBe(3);
  });

  test('a held button speeds up, then holds its pace', () => {
    expect(repeatDelay(0)).toBe(160);
    expect(repeatDelay(1)).toBe(145);
    expect(repeatDelay(7)).toBe(55);
    expect(repeatDelay(8)).toBe(50);
    expect(repeatDelay(100)).toBe(50);
  });
});
