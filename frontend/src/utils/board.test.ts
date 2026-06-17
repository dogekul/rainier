import { describe, expect, it } from 'vitest';
import { groupByStatus, isOverdue, rygToTier, statusColor, todayISO } from './board';

describe('board utils', () => {
  /** TC-BK-U01: statusColor maps every entity status to the intended tier. */
  it('maps statuses to the right tier (TC-BK-U01)', () => {
    for (const s of ['DONE', 'COMPLETED', 'DELIVERED', 'REACHED']) {
      expect(statusColor(s)).toBe('green');
    }
    for (const s of ['BLOCKED', 'MISSED']) {
      expect(statusColor(s)).toBe('red');
    }
    for (const s of ['DRAFT', 'CANCELLED', 'CLOSED', 'ARCHIVED']) {
      expect(statusColor(s)).toBe('gray');
    }
    for (const s of ['TODO', 'IN_PROGRESS', 'ACTIVE', 'PLANNING', 'READY', 'IN_APPROVAL', 'PLANNED']) {
      expect(statusColor(s)).toBe('yellow');
    }
    expect(statusColor(null)).toBe('gray');
    expect(statusColor(undefined)).toBe('gray');
    expect(statusColor('WAT')).toBe('yellow');
    // case-insensitive
    expect(statusColor('done')).toBe('green');
  });

  /** TC-BK-U02: groupByStatus counts per status in first-seen order. */
  it('counts per status in first-seen order (TC-BK-U02)', () => {
    const items = [{ s: 'TODO' }, { s: 'DONE' }, { s: 'TODO' }, { s: 'BLOCKED' }];
    expect(groupByStatus(items, (i) => i.s)).toEqual([
      { status: 'TODO', count: 2 },
      { status: 'DONE', count: 1 },
      { status: 'BLOCKED', count: 1 },
    ]);
    expect(groupByStatus([], (i: { s: string }) => i.s)).toEqual([]);
  });

  /** TC-BK-U03: isOverdue is a pure date compare; null → false. */
  it('compares dates purely, null is never overdue (TC-BK-U03)', () => {
    expect(isOverdue('2026-06-10', '2026-06-17')).toBe(true);
    expect(isOverdue('2026-06-17', '2026-06-17')).toBe(false);
    expect(isOverdue('2026-06-20', '2026-06-17')).toBe(false);
    expect(isOverdue(null, '2026-06-17')).toBe(false);
    expect(isOverdue(undefined, '2026-06-17')).toBe(false);
    expect(isOverdue('', '2026-06-17')).toBe(false);
    // tolerates a full ISO timestamp by comparing the date prefix
    expect(isOverdue('2026-06-10T08:00:00Z', '2026-06-17')).toBe(true);
  });

  /** TC-BK-U04: todayISO formats a Date as YYYY-MM-DD. */
  it('formats a date as YYYY-MM-DD (TC-BK-U04)', () => {
    expect(todayISO(new Date(2026, 0, 5))).toBe('2026-01-05');
    expect(todayISO(new Date(2026, 11, 31))).toBe('2026-12-31');
  });

  /** TC-BK-U05: rygToTier maps a server RYG verdict to a board tier (v0.0.29). */
  it('maps server RYG to a board tier (TC-BK-U05)', () => {
    expect(rygToTier('RED')).toBe('red');
    expect(rygToTier('YELLOW')).toBe('yellow');
    expect(rygToTier('GREEN')).toBe('green');
    expect(rygToTier('GRAY')).toBe('gray');
    expect(rygToTier('whatever')).toBe('gray');
  });
});
