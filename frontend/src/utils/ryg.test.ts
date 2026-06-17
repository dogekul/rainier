import { describe, expect, it } from 'vitest';
import { isOpenTaskStatus, loadTier, RYG_ORDER, ryg } from './ryg';

describe('ryg health rules', () => {
  /** TC-RYG-01: open-task predicate. */
  it('classifies open vs closed task status (TC-RYG-01)', () => {
    expect(isOpenTaskStatus('TODO')).toBe(true);
    expect(isOpenTaskStatus('IN_PROGRESS')).toBe(true);
    expect(isOpenTaskStatus('BLOCKED')).toBe(true);
    expect(isOpenTaskStatus('DONE')).toBe(false);
    expect(isOpenTaskStatus('CANCELLED')).toBe(false);
  });

  /** TC-RYG-02: no open work → gray. */
  it('is gray with no open tasks (TC-RYG-02)', () => {
    expect(ryg({ openCount: 0, overdueCount: 0, anyBlocked: false })).toBe('gray');
  });

  /** TC-RYG-03: any blocked open task → red regardless of ratio. */
  it('is red when anything is blocked (TC-RYG-03)', () => {
    expect(ryg({ openCount: 10, overdueCount: 0, anyBlocked: true })).toBe('red');
  });

  /** TC-RYG-04: overdue ratio > 0.3 → red. */
  it('is red above the overdue threshold (TC-RYG-04)', () => {
    expect(ryg({ openCount: 10, overdueCount: 4, anyBlocked: false })).toBe('red'); // 0.4
    expect(ryg({ openCount: 10, overdueCount: 3, anyBlocked: false })).toBe('yellow'); // 0.3 (not > 0.3)
  });

  /** TC-RYG-05: some overdue (but within threshold) → yellow; none → green. */
  it('is yellow with some overdue, green with none (TC-RYG-05)', () => {
    expect(ryg({ openCount: 10, overdueCount: 1, anyBlocked: false })).toBe('yellow');
    expect(ryg({ openCount: 10, overdueCount: 0, anyBlocked: false })).toBe('green');
  });

  /** TC-RYG-06: member-load thresholds (<=3 green, 4-7 yellow, >7 red). */
  it('bands member load by open-task count (TC-RYG-06)', () => {
    expect(loadTier(0)).toBe('green');
    expect(loadTier(3)).toBe('green');
    expect(loadTier(4)).toBe('yellow');
    expect(loadTier(7)).toBe('yellow');
    expect(loadTier(8)).toBe('red');
  });

  /** TC-RYG-07: sort order red→yellow→green→gray. */
  it('orders tiers red→yellow→green→gray (TC-RYG-07)', () => {
    const tiers = ['gray', 'green', 'red', 'yellow'] as const;
    const sorted = [...tiers].sort((a, b) => RYG_ORDER[a] - RYG_ORDER[b]);
    expect(sorted).toEqual(['red', 'yellow', 'green', 'gray']);
  });
});
