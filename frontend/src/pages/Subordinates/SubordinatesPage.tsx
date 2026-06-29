import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { DashboardCard, EmptyState } from '../../components/board';
import { Button } from '../../components/ui/Button';
import { listSubordinates, type Subordinate } from '../../api/subordinates';

/**
 * v0.0.111 (H4) — 我的下属面板. For an org HEAD (team / subgroup lead): one-stop list of direct
 * subordinates with a quick contribution summary, and a 「查看档案」 drill-through to each
 * subordinate's profile page (which is the C3 read-only view, authz-gated server-side).
 */
export function SubordinatesPage() {
  const [rows, setRows] = useState<Subordinate[] | null>(null);

  useEffect(() => {
    let active = true;
    void listSubordinates()
      .then((s) => {
        if (active) setRows(s);
      })
      .catch(() => {
        if (active) setRows([]);
      });
    return () => {
      active = false;
    };
  }, []);

  if (rows == null) {
    return (
      <div className="rainier-page">
        <div className="rainier-page-head">
          <h2>我的下属</h2>
        </div>
        <p style={{ color: 'var(--rainier-color-text-2)' }}>加载中…</p>
      </div>
    );
  }

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2>我的下属</h2>
      </div>

      <DashboardCard title="直接下属" extra={`${rows.length} 人`} testId="subord-list">
        {rows.length === 0 ? (
          <EmptyState
            message="你当前没有直接下属，或不是任何组织的负责人。"
            testId="subord-empty"
          />
        ) : (
          <table className="rainier-list-table">
            <thead>
              <tr>
                <th style={{ padding: '6px 8px', textAlign: 'left' }}>姓名</th>
                <th style={{ padding: '6px 8px', textAlign: 'left' }}>主组织</th>
                <th style={{ padding: '6px 8px', textAlign: 'right' }}>本周完成</th>
                <th style={{ padding: '6px 8px', textAlign: 'right' }}>任务总数</th>
                <th style={{ padding: '6px 8px' }}></th>
              </tr>
            </thead>
            <tbody>
              {rows.map((s) => (
                <tr key={s.id} data-testid={`subord-row-${s.id}`}>
                  <td style={{ padding: '6px 8px' }}>
                    {s.displayName ?? s.loginName ?? `#${s.id}`}
                    {s.loginName && (
                      <span
                        style={{
                          marginLeft: 6,
                          color: 'var(--rainier-color-text-2)',
                          fontSize: 12,
                        }}
                      >
                        @{s.loginName}
                      </span>
                    )}
                  </td>
                  <td style={{ padding: '6px 8px', color: 'var(--rainier-color-text-2)' }}>
                    {s.primaryOrgName ?? '—'}
                  </td>
                  <td
                    style={{ padding: '6px 8px', textAlign: 'right' }}
                    data-testid={`subord-week-${s.id}`}
                  >
                    {s.contributionSummary.weeklyTasksDone}
                  </td>
                  <td
                    style={{ padding: '6px 8px', textAlign: 'right' }}
                    data-testid={`subord-total-${s.id}`}
                  >
                    {s.contributionSummary.totalTasks}
                  </td>
                  <td style={{ padding: '6px 8px', width: 120, textAlign: 'right' }}>
                    <Link to={`/users/${s.id}/profile`} data-testid={`subord-profile-${s.id}`}>
                      <Button type="button" variant="secondary">
                        查看档案
                      </Button>
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </DashboardCard>
    </div>
  );
}
