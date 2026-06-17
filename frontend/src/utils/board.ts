/**
 * v0.0.22 board-kit — pure aggregation utils shared by every v1.0 dashboard (PM cockpit, team-lead
 * panel, …). No React, no I/O — trivially unit-testable.
 */

export type StatusTier = 'red' | 'yellow' | 'green' | 'gray';

// Tier membership. Anything active/pending not listed here falls through to yellow.
const GREEN = new Set(['DONE', 'COMPLETED', 'DELIVERED', 'REACHED']);
const RED = new Set(['BLOCKED', 'MISSED']);
const GRAY = new Set(['DRAFT', 'CANCELLED', 'CLOSED', 'ARCHIVED']);

/** Maps any entity status to one of the 4 board tiers. Null/unknown → gray; active/pending → yellow. */
export function statusColor(status: string | null | undefined): StatusTier {
  if (!status) return 'gray';
  const s = status.toUpperCase();
  if (GREEN.has(s)) return 'green';
  if (RED.has(s)) return 'red';
  if (GRAY.has(s)) return 'gray';
  return 'yellow';
}

/** v0.0.29: map a server RYG verdict (RED/YELLOW/GREEN/GRAY) to a board tier. */
export function rygToTier(ryg: string): StatusTier {
  const r = (ryg || '').toLowerCase();
  return r === 'red' || r === 'yellow' || r === 'green' ? r : 'gray';
}

/** Chinese one-char label for an RYG tier. */
export const RYG_LABEL: Record<StatusTier, string> = {
  red: '红',
  yellow: '黄',
  green: '绿',
  gray: '灰',
};

export interface StatusCount {
  status: string;
  count: number;
}

/** Counts items per distinct status, preserving first-seen order. */
export function groupByStatus<T>(items: T[], getStatus: (item: T) => string): StatusCount[] {
  const order: string[] = [];
  const counts = new Map<string, number>();
  for (const item of items) {
    const s = getStatus(item);
    if (!counts.has(s)) {
      counts.set(s, 0);
      order.push(s);
    }
    counts.set(s, (counts.get(s) ?? 0) + 1);
  }
  return order.map((s) => ({ status: s, count: counts.get(s) ?? 0 }));
}

/**
 * Pure date compare on the YYYY-MM-DD prefix. A null/empty date is never overdue. `today` is supplied
 * (YYYY-MM-DD) so callers stay deterministic and testable.
 */
export function isOverdue(dateStr: string | null | undefined, today: string): boolean {
  if (!dateStr) return false;
  return dateStr.slice(0, 10) < today;
}

/** Today as a local YYYY-MM-DD string. Pass a Date in tests for determinism. */
export function todayISO(d: Date = new Date()): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}
