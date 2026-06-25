import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { DashboardCard, EmptyState } from '../../components/board';
import { getCrmMetrics, type CrmMetrics } from '../../api/metrics';

function formatPct(v: number | null): string {
  if (v == null) return '—';
  return (v * 100).toFixed(1) + '%';
}

function formatDays(v: number | null): string {
  if (v == null) return '—';
  return v.toFixed(1) + ' 天';
}

/**
 * v0.0.93 (D5) — CRM 度量看板. Pure-number cards for winRate / dealRate / avgDeliveryCycleDays,
 * plus a table of 逾期项目 needing follow-up. Reads GET /api/metrics/crm (all-users).
 */
export function MetricsPage() {
  const [data, setData] = useState<CrmMetrics | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    setLoading(true);
    void getCrmMetrics()
      .then((d) => {
        if (active) {
          setData(d);
          setLoading(false);
        }
      })
      .catch(() => {
        if (active) {
          setData(null);
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2>度量看板</h2>
      </div>

      {loading ? (
        <div data-testid="metrics-loading" style={{ padding: 16 }}>
          加载中...
        </div>
      ) : data == null ? (
        <EmptyState message="无法加载度量数据。" testId="metrics-error" />
      ) : (
        <>
          <div
            data-testid="metrics-cards"
            style={{ display: 'flex', gap: 16, marginBottom: 16, flexWrap: 'wrap' }}
          >
            <MetricCard label="中标率" value={formatPct(data.winRate)} testId="metric-win-rate" />
            <MetricCard label="成单率" value={formatPct(data.dealRate)} testId="metric-deal-rate" />
            <MetricCard
              label="平均交付周期"
              value={formatDays(data.avgDeliveryCycleDays)}
              testId="metric-avg-cycle"
            />
            <MetricCard
              label="逾期项目"
              value={String(data.overdueProjects.length)}
              testId="metric-overdue-count"
            />
          </div>

          <DashboardCard title="逾期项目督办" testId="overdue-list">
            {data.overdueProjects.length === 0 ? (
              <div style={{ padding: 12, color: 'var(--rainier-color-text-2)' }}>
                目前没有逾期项目。
              </div>
            ) : (
              <table className="rainier-list-table">
                <thead>
                  <tr>
                    <th style={{ textAlign: 'left', padding: '6px 8px' }}>项目</th>
                    <th style={{ textAlign: 'left', padding: '6px 8px' }}>状态</th>
                    <th style={{ textAlign: 'left', padding: '6px 8px' }}>预计结束</th>
                    <th style={{ textAlign: 'left', padding: '6px 8px' }}>逾期天数</th>
                  </tr>
                </thead>
                <tbody>
                  {data.overdueProjects.map((r) => (
                    <tr key={r.projectId} data-testid={`overdue-row-${r.projectId}`}>
                      <td style={{ padding: '6px 8px' }}>
                        <Link to={`/pm/projects/${r.projectId}`}>
                          {r.code} {r.name}
                        </Link>
                      </td>
                      <td style={{ padding: '6px 8px' }}>{r.status}</td>
                      <td style={{ padding: '6px 8px' }}>{r.expectedEndDate ?? '—'}</td>
                      <td style={{ padding: '6px 8px' }}>{r.daysOverdue}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </DashboardCard>
        </>
      )}
    </div>
  );
}

function MetricCard(props: { label: string; value: string; testId: string }) {
  return (
    <div
      data-testid={props.testId}
      style={{
        flex: '1 1 180px',
        minWidth: 180,
        padding: 16,
        border: '1px solid var(--rainier-color-border)',
        borderRadius: 8,
        background: 'var(--rainier-color-surface)',
      }}
    >
      <div style={{ fontSize: 13, color: 'var(--rainier-color-text-2)' }}>{props.label}</div>
      <div style={{ fontSize: 26, fontWeight: 600, marginTop: 6 }}>{props.value}</div>
    </div>
  );
}
