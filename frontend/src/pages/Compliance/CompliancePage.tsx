import { useCallback, useEffect, useState } from 'react';
import { DashboardCard, EmptyState, StatTiles } from '../../components/board';
import {
  getAuditSummary,
  getResidualPermissions,
  revokeResidualRoles,
  type AuditSummary,
  type ResidualPermission,
} from '../../api/compliance';
import { formatDateTime } from '../../utils/formatDate';

/**
 * v0.0.41 — 合规仪表盘 (admin compliance dashboard). Consumes GET /api/compliance/audit-summary +
 * /residual-permissions (both admin-gated). Audit activity rollup (total + by action / entity type +
 * recent feed) and the「停用-残留权限」reconciliation — disabled users still holding role grants that
 * should be revoked. Completes the 系统/管理员 role's missing hook; closes the v0.0.21/38 auth track.
 */
export function CompliancePage() {
  const [summary, setSummary] = useState<AuditSummary | null>(null);
  const [residual, setResidual] = useState<ResidualPermission[]>([]);
  const [loading, setLoading] = useState(true);
  const [pendingId, setPendingId] = useState<number | null>(null);

  const reload = useCallback(async () => {
    const [s, r] = await Promise.all([getAuditSummary(), getResidualPermissions()]);
    setSummary(s);
    setResidual(r);
  }, []);

  useEffect(() => {
    let active = true;
    void reload()
      .catch(() => {})
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [reload]);

  const handleRevoke = useCallback(
    async (userId: number) => {
      setPendingId(userId);
      try {
        await revokeResidualRoles(userId);
        await reload();
      } finally {
        setPendingId(null);
      }
    },
    [reload],
  );

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2>合规仪表盘</h2>
      </div>

      <StatTiles
        testId="compliance-summary"
        tiles={[
          { label: '审计事件总量', value: summary?.total ?? 0 },
          {
            label: '停用-残留权限用户',
            value: residual.length,
            tier: residual.length > 0 ? 'red' : 'green',
          },
        ]}
      />

      <DashboardCard title="停用-残留权限对账（建议回收）" testId="compliance-residual">
        {!loading && residual.length === 0 ? (
          <EmptyState message="没有停用用户残留角色授权。" testId="compliance-residual-empty" />
        ) : (
          <table className="rainier-list-table">
            <tbody>
              {residual.map((r) => (
                <tr key={r.userId} data-testid={`compliance-residual-row-${r.userId}`}>
                  <td style={{ padding: '6px 8px' }}>
                    {r.name ?? r.loginName}
                    <span style={{ marginLeft: 8, color: 'var(--rainier-color-text-2)', fontSize: 12 }}>
                      @{r.loginName}
                    </span>
                  </td>
                  <td style={{ padding: '6px 8px', width: 80 }}>{r.roleCount} 个角色</td>
                  <td style={{ padding: '6px 8px', color: 'var(--rainier-color-text-2)' }}>
                    {r.roleNames.join('、')}
                  </td>
                  <td style={{ padding: '6px 8px', width: 100, textAlign: 'right' }}>
                    <button
                      type="button"
                      data-testid={`compliance-residual-revoke-${r.userId}`}
                      disabled={pendingId === r.userId}
                      onClick={() => void handleRevoke(r.userId)}
                      style={{
                        padding: '2px 10px',
                        fontSize: 12,
                        border: '1px solid var(--rainier-color-border, #d0d7de)',
                        background: 'var(--rainier-color-bg-1, #fff)',
                        borderRadius: 4,
                        cursor: pendingId === r.userId ? 'default' : 'pointer',
                      }}
                    >
                      {pendingId === r.userId ? '回收中…' : '一键回收'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </DashboardCard>

      <DashboardCard title="审计活动（按动作 / 实体类型）" testId="compliance-breakdown">
        <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap', padding: '4px 2px' }}>
          <div data-testid="compliance-by-action">
            <div style={{ fontSize: 12, color: 'var(--rainier-color-text-2)', marginBottom: 4 }}>按动作</div>
            {(summary?.byAction ?? []).map((b) => (
              <div key={b.label}>
                {b.label} · {b.count}
              </div>
            ))}
          </div>
          <div data-testid="compliance-by-entity">
            <div style={{ fontSize: 12, color: 'var(--rainier-color-text-2)', marginBottom: 4 }}>按实体类型</div>
            {(summary?.byEntityType ?? []).map((b) => (
              <div key={b.label}>
                {b.label} · {b.count}
              </div>
            ))}
          </div>
        </div>
      </DashboardCard>

      <DashboardCard title="最近审计活动" testId="compliance-recent">
        <table className="rainier-list-table">
          <tbody>
            {(summary?.recent ?? []).map((a) => (
              <tr key={a.id} data-testid={`compliance-recent-row-${a.id}`}>
                <td style={{ padding: '6px 8px', width: 110 }}>{a.actor ?? '—'}</td>
                <td style={{ padding: '6px 8px' }}>
                  {a.action} {a.entityType}
                  {a.entityId != null ? `#${a.entityId}` : ''}
                </td>
                <td style={{ padding: '6px 8px', color: 'var(--rainier-color-text-2)', width: 200 }}>
                  {formatDateTime(a.createTime)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </DashboardCard>
    </div>
  );
}
