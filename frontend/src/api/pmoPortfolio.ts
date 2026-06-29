import client from './client';
import type { PortfolioRow } from './portfolio';

/** v0.0.110 (H3) — pivot dimensions for the PMO company project map. */
export type PmoGroupBy = 'organization' | 'owner' | 'none';

/** Group descriptor: organization, owner (USER), or the synthetic "全公司" pseudo-group. */
export interface PmoGroup {
  id: number | null;
  name: string;
  type: string | null;
}

/** RYG tally for a single group. */
export interface PmoRygCount {
  red: number;
  yellow: number;
  green: number;
  gray: number;
}

/** One group on the PMO company project map. */
export interface PmoPortfolioRow {
  group: PmoGroup;
  projects: PortfolioRow[];
  rygCount: PmoRygCount;
}

/**
 * GET /api/pmo/portfolio?groupBy= — company-wide RYG rolled into pivoted groups.
 * Token-gated; same all-users visibility as /api/me/portfolio?scope=all.
 */
export async function getPmoPortfolio(
  groupBy: PmoGroupBy = 'organization',
): Promise<PmoPortfolioRow[]> {
  const res = await client.get<PmoPortfolioRow[]>('/pmo/portfolio', { params: { groupBy } });
  return res.data;
}
