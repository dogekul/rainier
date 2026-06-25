/**
 * v0.0.60 — shared date formatting helpers.
 *
 * Pages historically rendered raw ISO strings like "2026-06-25T03:14:22Z" verbatim, which is ugly
 * and locale-blind. This util normalizes them to short, readable form (drop seconds, optional
 * relative form for recent events).
 */

const ZH_DATE_OPTS: Intl.DateTimeFormatOptions = { year: 'numeric', month: '2-digit', day: '2-digit' };
const ZH_DATETIME_OPTS: Intl.DateTimeFormatOptions = {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
};

/** Format an ISO 8601 timestamp as YYYY/MM/DD HH:mm. Returns '—' on null/invalid. */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString('zh-CN', ZH_DATETIME_OPTS).replace(/\//g, '/');
}

/** Format as date only (YYYY/MM/DD). Returns '—' on null/invalid. */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString('zh-CN', ZH_DATE_OPTS).replace(/\//g, '/');
}

/** Relative form: "3 分钟前 / 2 小时前 / 5 天前", falls back to formatDateTime past 30 days. */
export function formatRelative(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  const diffMs = Date.now() - d.getTime();
  const diffMin = Math.floor(diffMs / 60_000);
  if (diffMin < 1) return '刚刚';
  if (diffMin < 60) return `${diffMin} 分钟前`;
  const diffHr = Math.floor(diffMin / 60);
  if (diffHr < 24) return `${diffHr} 小时前`;
  const diffDay = Math.floor(diffHr / 24);
  if (diffDay < 30) return `${diffDay} 天前`;
  return formatDateTime(iso);
}
