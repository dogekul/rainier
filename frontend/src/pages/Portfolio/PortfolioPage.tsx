import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { DashboardCard, EmptyState, StatTiles, StatusChip } from '../../components/board';
import { rygToTier, RYG_LABEL, type StatusTier } from '../../utils/board';
import { getPortfolio, type PortfolioRow, type PortfolioScope } from '../../api/portfolio';

const SCOPES: { value: PortfolioScope; label: string }[] = [
  { value: 'mine', label: '我的项目' },
  { value: 'led', label: '我带的团队' },
  { value: 'all', label: '全公司' },
];

/**
 * v0.0.30 — 项目地图 (Portfolio map). One scoped red/yellow/green view over the
 * GET /api/me/portfolio substrate, serving several roles via the scope toggle: 我的项目 (PM cross-project),
 * 我带的团队 (team/domain/department lead — org-subtree footprint), 全公司 (PMO / exec company map).
 * Pure server-computed RYG, worst-first. All-users.
 */
export function PortfolioPage() {
  const [scope, setScope] = useState<PortfolioScope>('mine');
  const [rows, setRows] = useState<PortfolioRow[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    setLoading(true);
    void getPortfolio(scope)
      .then((r) => {
        if (active) {
          setRows(r);
          setLoading(false);
        }
      })
      .catch(() => {
        if (active) {
          setRows([]);
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, [scope]);

  // RYG distribution for the summary tiles.
  const counts: Record<StatusTier, number> = { red: 0, yellow: 0, green: 0, gray: 0 };
  for (const r of rows) counts[rygToTier(r.ryg)] += 1;

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2 style={{ margin: 0 }}>项目地图</h2>
        <select
          data-testid="portfolio-scope"
          className="rainier-select"
          value={scope}
          onChange={(e) => setScope(e.target.value as PortfolioScope)}
        >
          {SCOPES.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
      </div>

      {!loading && rows.length === 0 ? (
        <EmptyState
          message={
            scope === 'led'
              ? '你当前没有带任何团队的项目。'
              : '这个范围下还没有项目。'
          }
          cta={{ label: '去创建项目', to: '/pm/projects' }}
          testId="portfolio-empty"
        />
      ) : (
        <>
          <StatTiles
            testId="portfolio-summary"
            tiles={[
              { label: '红', value: counts.red, tier: 'red' },
              { label: '黄', value: counts.yellow, tier: 'yellow' },
              { label: '绿', value: counts.green, tier: 'green' },
              { label: '灰', value: counts.gray, tier: 'gray' },
            ]}
          />

          <DashboardCard title="项目（红→绿排序）" testId="portfolio-list">
            <table className="rainier-list-table">
              <tbody>
                {rows.map((r) => (
                  <tr key={r.projectId} data-testid={`portfolio-row-${r.projectId}`}>
                    <td style={{ padding: '6px 8px', width: 60 }}>
                      <StatusChip
                        status=""
                        tier={rygToTier(r.ryg)}
                        label={RYG_LABEL[rygToTier(r.ryg)]}
                        testId={`portfolio-ryg-${r.projectId}`}
                      />
                    </td>
                    <td style={{ padding: '6px 8px' }}>
                      <Link to={`/pm/tasks?projectId=${r.projectId}`}>
                        {r.projectCode} {r.projectName}
                      </Link>
                    </td>
                    <td
                      style={{ padding: '6px 8px', width: 240, color: 'var(--rainier-color-text-2)' }}
                    >
                      开放 {r.openTasks} · 逾期 {r.overdueTasks} · 阻塞 {r.blockedTasks} · 逾期里程碑{' '}
                      {r.overdueMilestones}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </DashboardCard>
        </>
      )}
    </div>
  );
}
