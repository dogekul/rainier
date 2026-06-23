import { describe, expect, it } from 'vitest';
import { dwellDays, dwellTier } from './dwell';

const now = new Date('2026-06-23T12:00:00Z');
function daysAgo(d: number): string {
  return new Date(now.getTime() - d * 86_400_000).toISOString();
}

describe('dwellDays (TC-DWL-02)', () => {
  it('computes whole days; null/unparseable → null', () => {
    expect(dwellDays(null, now)).toBeNull();
    expect(dwellDays('not-a-date', now)).toBeNull();
    expect(dwellDays(daysAgo(0), now)).toBe(0);
    expect(dwellDays(daysAgo(10), now)).toBe(10);
  });

  it('future timestamp clamps to 0', () => {
    expect(dwellDays(new Date(now.getTime() + 86_400_000).toISOString(), now)).toBe(0);
  });
});

describe('dwellTier (TC-DWL-01)', () => {
  it('grades by dwell days', () => {
    expect(dwellTier(null, now)).toBe('gray');
    expect(dwellTier(daysAgo(3), now)).toBe('green');
    expect(dwellTier(daysAgo(7), now)).toBe('green');
    expect(dwellTier(daysAgo(10), now)).toBe('yellow');
    expect(dwellTier(daysAgo(14), now)).toBe('yellow');
    expect(dwellTier(daysAgo(20), now)).toBe('red');
  });
});
