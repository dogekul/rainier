/**
 * v0.0.47 — 金额展示格式化（人民币）。纯函数，无 I/O，便于单测。元为单位：
 * <1万 显示千分位￥，<1亿 显示「X万」，≥1亿 显示「X亿」（≤1 位小数、去尾 0）。null/undefined → 占位「—」。
 * 单位选择在四舍五入「之后」判定，避免 99,999,999 被舍入成「10000万」而非进位「1亿」。
 */

/** Format a 元-denominated amount for compact display. */
export function formatCNY(amount?: number | null): string {
  if (amount == null) return '—';
  const n = Math.round(amount);
  const sign = n < 0 ? '-' : '';
  const abs = Math.abs(n);
  if (abs < 10000) return `${sign}¥${abs.toLocaleString('en-US')}`;
  const wan = Math.round(abs / 1000) / 10; // 万，1 位小数 — 先舍入再选单位
  if (wan < 10000) return `${sign}¥${wan}万`;
  return `${sign}¥${Math.round(abs / 10000000) / 10}亿`;
}
