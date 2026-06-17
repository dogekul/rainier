import client from './client';

/** v0.0.28 — one project's health rollup in a scoped portfolio. */
export interface PortfolioRow {
  projectId: number;
  projectCode: string;
  projectName: string;
  projectStatus: string;
  organizationId: number | null;
  openTasks: number;
  overdueTasks: number;
  blockedTasks: number;
  overdueMilestones: number;
  /** RED | YELLOW | GREEN | GRAY (worst-first sorted by the server). */
  ryg: string;
}

export type PortfolioScope = 'mine' | 'led' | 'all';

/**
 * GET /api/me/portfolio?scope= — scoped project-health rollup (open/overdue/blocked + RYG per project).
 * `mine` = my projects, `led` = projects under the orgs I head (subtree), `all` = company-wide.
 */
export async function getPortfolio(scope: PortfolioScope = 'mine'): Promise<PortfolioRow[]> {
  const res = await client.get<PortfolioRow[]>('/me/portfolio', { params: { scope } });
  return res.data;
}
