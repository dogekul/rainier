import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { DashboardCard, EmptyState, StatusChip } from '../../components/board';
import { rygToTier, RYG_LABEL } from '../../utils/board';
import {
  getPmoPortfolio,
  type PmoGroupBy,
  type PmoPortfolioRow,
} from '../../api/pmoPortfolio';

const GROUP_BYS: { value: PmoGroupBy; label: string }[] = [
  { value: 'organization', label: '按组织' },
  { value: 'owner', label: '按负责人' },
  { value: 'none', label: '不分组' },
];

/**
 * v0.0.110 (H3) — PMO 公司项目地图. Company-wide RYG rolled into pivoted group cards
 * (按组织 / 按负责人 / 不分组). Token-gated; same all-users visibility as PortfolioPage scope=all.
 */
export function PmoPortfolioPage() {
  const [groupBy, setGroupBy] = useState<PmoGroupBy>('organization');
  const [rows, setRows] = useState<PmoPortfolioRow[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    setLoading(true);
    void getPmoPortfolio(groupBy)
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
  }, [groupBy]);

  const totalProjects = rows.reduce((s, g) => s + g.projects.length, 0);

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2>PMO · 公司项目地图</h2>
        <select
          data-testid="pmo-groupby"
          className="rainier-select"
          value={groupBy}
          onChange={(e) => setGroupBy(e.target.value as PmoGroupBy)}
        >
          {GROUP_BYS.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
      </div>

      {!loading && totalProjects === 0 ? (
        <EmptyState
          message="还没有项目可以聚合。"
          cta={{ label: '去创建项目', to: '/pm/projects' }}
          testId="pmo-empty"
        />
      ) : (
        <div data-testid="pmo-groups" style={{ display: 'grid', gap: 16 }}>
          {rows.map((g, idx) => {
            const key = g.group.id != null ? `${g.group.type ?? ''}-${g.group.id}` : `null-${idx}`;
            return (
              <DashboardCard
                key={key}
                title={
                  <span>
                    {g.group.name}
                    <span
                      style={{
                        marginLeft: 8,
                        color: 'var(--rainier-color-text-2)',
                        fontSize: '0.85em',
                      }}
                    >
                      （{g.projects.length} 个项目）
                    </span>
                  </span>
                }
                testId={`pmo-group-${key}`}
                extra={
                  <span style={{ display: 'inline-flex', gap: 6 }}>
                    <StatusChip
                      status=""
                      tier="red"
                      label={`红 ${g.rygCount.red}`}
                      testId={`pmo-group-${key}-red`}
                    />
                    <StatusChip
                      status=""
                      tier="yellow"
                      label={`黄 ${g.rygCount.yellow}`}
                      testId={`pmo-group-${key}-yellow`}
                    />
                    <StatusChip
                      status=""
                      tier="green"
                      label={`绿 ${g.rygCount.green}`}
                      testId={`pmo-group-${key}-green`}
                    />
                    {g.rygCount.gray > 0 && (
                      <StatusChip
                        status=""
                        tier="gray"
                        label={`灰 ${g.rygCount.gray}`}
                        testId={`pmo-group-${key}-gray`}
                      />
                    )}
                  </span>
                }
              >
                <table className="rainier-list-table">
                  <tbody>
                    {g.projects.map((r) => (
                      <tr key={r.projectId} data-testid={`pmo-row-${r.projectId}`}>
                        <td style={{ padding: '6px 8px', width: 60 }}>
                          <StatusChip
                            status=""
                            tier={rygToTier(r.ryg)}
                            label={RYG_LABEL[rygToTier(r.ryg)]}
                            testId={`pmo-ryg-${r.projectId}`}
                          />
                        </td>
                        <td style={{ padding: '6px 8px' }}>
                          <Link to={`/pm/tasks?projectId=${r.projectId}`}>
                            {r.projectCode} {r.projectName}
                          </Link>
                        </td>
                        <td
                          style={{
                            padding: '6px 8px',
                            width: 240,
                            color: 'var(--rainier-color-text-2)',
                          }}
                        >
                          开放 {r.openTasks} · 逾期 {r.overdueTasks} · 阻塞 {r.blockedTasks}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </DashboardCard>
            );
          })}
        </div>
      )}
    </div>
  );
}
