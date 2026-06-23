/**
 * v0.0.47 — 停留时长预警。纯函数，无 I/O；`now` 注入以便确定性单测。基于商机「进入当前阶段时间」
 * (stageEnteredAt)：≤7 天 绿、≤14 天 黄、>14 天 红、无值 灰。阈值具名常量，便于后续按阶段细化。
 */
import type { StatusTier } from './board';

export const DWELL_GREEN_MAX_DAYS = 7;
export const DWELL_YELLOW_MAX_DAYS = 14;

const MS_PER_DAY = 86_400_000;

/** Whole days the opp has dwelt in its current stage; null if stageEnteredAt is absent/unparseable. */
export function dwellDays(stageEnteredAt?: string | null, now: Date = new Date()): number | null {
  if (!stageEnteredAt) return null;
  const t = Date.parse(stageEnteredAt);
  if (Number.isNaN(t)) return null;
  return Math.max(0, Math.floor((now.getTime() - t) / MS_PER_DAY));
}

/** Dwell health tier: gray (unknown) / green (≤7d) / yellow (≤14d) / red (>14d). */
export function dwellTier(stageEnteredAt?: string | null, now: Date = new Date()): StatusTier {
  const d = dwellDays(stageEnteredAt, now);
  if (d == null) return 'gray';
  if (d <= DWELL_GREEN_MAX_DAYS) return 'green';
  if (d <= DWELL_YELLOW_MAX_DAYS) return 'yellow';
  return 'red';
}

/** Chinese label per dwell tier. */
export const DWELL_LABEL: Record<StatusTier, string> = {
  green: '停留正常',
  yellow: '停留偏久',
  red: '超期未动',
  gray: '停留未知',
};
