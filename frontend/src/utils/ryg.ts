/**
 * v0.0.25 team-lead panel — pure red/yellow/green health rules + member-load thresholds. No React,
 * no I/O. Named-constant thresholds so the rules are auditable and unit-tested in isolation.
 */
import type { StatusTier } from './board';

export type RygTier = StatusTier; // red | yellow | green | gray

/** A project goes RED above this overdue ratio (of its open tasks). */
export const RYG_OVERDUE_RED_THRESHOLD = 0.3;
/** Member-load colour bands by open-task count. */
export const LOAD_YELLOW_MIN = 4;
export const LOAD_RED_MIN = 8;

/** Sort order red → yellow → green → gray. */
export const RYG_ORDER: Record<RygTier, number> = { red: 0, yellow: 1, green: 2, gray: 3 };

const CLOSED_TASK = new Set(['DONE', 'CANCELLED']);

/** An open task is anything not DONE/CANCELLED (TODO/IN_PROGRESS/BLOCKED). */
export function isOpenTaskStatus(status: string): boolean {
  return !CLOSED_TASK.has(status.toUpperCase());
}

export interface RygInput {
  openCount: number;
  overdueCount: number;
  anyBlocked: boolean;
}

/**
 * Project health: GRAY if no open work; RED if any open task is BLOCKED or the overdue ratio exceeds
 * the threshold; YELLOW if some (but few) overdue; GREEN if open with nothing overdue/blocked.
 */
export function ryg({ openCount, overdueCount, anyBlocked }: RygInput): RygTier {
  if (openCount === 0) return 'gray';
  if (anyBlocked) return 'red';
  const ratio = overdueCount / openCount;
  if (ratio > RYG_OVERDUE_RED_THRESHOLD) return 'red';
  if (ratio > 0) return 'yellow';
  return 'green';
}

/** Member load colour by open-task count: <=3 green, 4–7 yellow, >7 red. */
export function loadTier(openTasks: number): RygTier {
  if (openTasks >= LOAD_RED_MIN) return 'red';
  if (openTasks >= LOAD_YELLOW_MIN) return 'yellow';
  return 'green';
}
