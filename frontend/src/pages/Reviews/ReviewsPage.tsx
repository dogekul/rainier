import { useCallback, useEffect, useState } from 'react';
import { DashboardCard, EmptyState, OwnerChip, StatTiles, StatusChip } from '../../components/board';
import { Button } from '../../components/ui/Button';
import {
  getPendingReviews,
  submitReview,
  type PendingReview,
  type ReviewDecision,
} from '../../api/reviews';

/**
 * v0.0.39 —「我的评审」(my review queue). Consumes GET /api/me/pending-reviews: the stories where the
 * current user is the assigned reviewer and the review is PENDING. One-click 通过/打回 records a decision
 * (POST /api/stories/{id}/review) and refetches, so the story leaves the queue. All-users — serves the
 * architect review board + tester gate + PM/PO review queues. (Story has no detail route, so the title
 * renders as text rather than a dead link.)
 */
export function ReviewsPage() {
  const [rows, setRows] = useState<PendingReview[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<number | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    return getPendingReviews()
      .then((r) => {
        setRows(r);
        setLoading(false);
      })
      .catch(() => {
        setRows([]);
        setLoading(false);
      });
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const decide = async (storyId: number, decision: ReviewDecision) => {
    setBusyId(storyId);
    try {
      await submitReview(storyId, decision);
      await load();
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2 style={{ margin: 0 }}>我的评审</h2>
      </div>

      {!loading && rows.length === 0 ? (
        <EmptyState message="当前没有待你评审的 Story。" testId="reviews-empty" />
      ) : (
        <>
          <StatTiles
            testId="reviews-summary"
            tiles={[{ label: '待评审', value: rows.length, tier: rows.length > 0 ? 'yellow' : 'gray' }]}
          />

          <DashboardCard title="待我评审的 Story" testId="reviews-list">
            <table className="rainier-list-table">
              <tbody>
                {rows.map((r) => (
                  <tr key={r.storyId} data-testid={`reviews-row-${r.storyId}`}>
                    <td style={{ padding: '6px 8px', width: 72 }}>
                      <StatusChip
                        status={r.priority}
                        label={r.priority}
                        testId={`reviews-prio-${r.storyId}`}
                      />
                    </td>
                    <td style={{ padding: '6px 8px' }}>
                      <div>
                        {r.code} {r.title}
                      </div>
                      <div style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
                        {r.projectName ?? '—'}
                        {r.sprintName ? ` · ${r.sprintName}` : ''}
                      </div>
                    </td>
                    <td style={{ padding: '6px 8px', width: 140 }}>
                      <OwnerChip name={r.ownerName} loginName={r.ownerLoginName} />
                    </td>
                    <td style={{ padding: '6px 8px', width: 150, textAlign: 'right' }}>
                      <Button
                        type="button"
                        variant="primary"
                        disabled={busyId === r.storyId}
                        onClick={() => void decide(r.storyId, 'APPROVED')}
                        data-testid={`reviews-approve-${r.storyId}`}
                      >
                        通过
                      </Button>
                      <Button
                        type="button"
                        variant="secondary"
                        style={{ marginLeft: 6 }}
                        disabled={busyId === r.storyId}
                        onClick={() => void decide(r.storyId, 'REJECTED')}
                        data-testid={`reviews-reject-${r.storyId}`}
                      >
                        打回
                      </Button>
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
