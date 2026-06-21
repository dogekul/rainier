import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { DashboardCard, EmptyState, StatTiles, StatusChip } from '../../components/board';
import { getInbox, type Inbox } from '../../api/inbox';
import { PRIORITY_LABELS, type Priority } from '../../api/demand';
import { REQUIREMENT_STATUS_LABELS, type RequirementStatus } from '../../api/requirement';

/**
 * v0.0.42 — PO 需求收件箱 (my inbox). Consumes GET /api/me/inbox: unconverted demands awaiting triage
 * (no requirement link yet, non-terminal) + the current user's own requirements. All-users; the PO's
 * "what needs my attention" landing the audit found missing. Rows link to the full demand/requirement
 * management pages.
 */
export function InboxPage() {
  const [inbox, setInbox] = useState<Inbox | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    void getInbox()
      .then((d) => {
        if (active) {
          setInbox(d);
          setLoading(false);
        }
      })
      .catch(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  const demands = inbox?.unconvertedDemands ?? [];
  const reqs = inbox?.myRequirements ?? [];

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2 style={{ margin: 0 }}>需求收件箱</h2>
      </div>

      <StatTiles
        testId="inbox-summary"
        tiles={[
          { label: '待处理诉求', value: demands.length, tier: demands.length > 0 ? 'yellow' : 'gray' },
          { label: '我的需求', value: reqs.length },
        ]}
      />

      <DashboardCard title="待处理诉求（未转化）" testId="inbox-demands">
        {!loading && demands.length === 0 ? (
          <EmptyState message="没有待转化的诉求。" testId="inbox-demands-empty" />
        ) : (
          <table className="rainier-list-table">
            <tbody>
              {demands.map((d) => (
                <tr key={d.id} data-testid={`inbox-demand-row-${d.id}`}>
                  <td style={{ padding: '6px 8px', width: 72 }}>
                    <StatusChip status={d.priority} label={PRIORITY_LABELS[d.priority as Priority] ?? d.priority} />
                  </td>
                  <td style={{ padding: '6px 8px' }}>
                    <Link to="/pm/demands">{d.title}</Link>
                  </td>
                  <td style={{ padding: '6px 8px', width: 120, color: 'var(--rainier-color-text-2)' }}>
                    {d.status}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </DashboardCard>

      <DashboardCard title="我的需求" testId="inbox-reqs">
        {!loading && reqs.length === 0 ? (
          <EmptyState message="你还没有负责的需求。" testId="inbox-reqs-empty" />
        ) : (
          <table className="rainier-list-table">
            <tbody>
              {reqs.map((r) => (
                <tr key={r.id} data-testid={`inbox-req-row-${r.id}`}>
                  <td style={{ padding: '6px 8px', width: 96 }}>
                    <StatusChip
                      status={r.status}
                      label={REQUIREMENT_STATUS_LABELS[r.status as RequirementStatus] ?? r.status}
                    />
                  </td>
                  <td style={{ padding: '6px 8px' }}>
                    <Link to="/pm/requirements">
                      {r.code} {r.title}
                    </Link>
                    {r.projectName ? (
                      <span style={{ marginLeft: 8, color: 'var(--rainier-color-text-2)', fontSize: 12 }}>
                        {r.projectName}
                      </span>
                    ) : null}
                  </td>
                  <td style={{ padding: '6px 8px', width: 72, color: 'var(--rainier-color-text-2)' }}>
                    {PRIORITY_LABELS[r.priority as Priority] ?? r.priority}
                  </td>
                  <td style={{ padding: '6px 8px', width: 120, color: 'var(--rainier-color-text-2)' }}>
                    {r.expectedDate ?? ''}
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
