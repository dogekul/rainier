import { useCallback, useEffect, useMemo, useState } from 'react';
import { DashboardCard, EmptyState, OwnerChip, StatTiles, StatusChip } from '../../components/board';
import { Button } from '../../components/ui/Button';
import {
  getPendingReviews,
  submitReview,
  submitTaskReview,
  type PendingReview,
  type ReviewDecision,
} from '../../api/reviews';

/**
 * v0.0.39 + v0.0.82 —「我的评审」 (my review queue). Consumes GET /api/me/pending-reviews which
 * now returns merged Story + Task rows tagged by `kind`. Story tab uses the v0.0.39 one-click
 * decision; Task tab uses v0.0.82 POST /api/tasks/{id}/review (REJECTED requires a reason —
 * prompted inline).
 */
export function ReviewsPage() {
  const [rows, setRows] = useState<PendingReview[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [tab, setTab] = useState<'STORY' | 'TASK'>('STORY');

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

  const storyRows = useMemo(
    () => rows.filter((r) => (r.kind ?? 'STORY') === 'STORY'),
    [rows],
  );
  const taskRows = useMemo(() => rows.filter((r) => r.kind === 'TASK'), [rows]);

  const decideStory = async (storyId: number, decision: ReviewDecision) => {
    const key = `S-${storyId}`;
    setBusyId(key);
    try {
      await submitReview(storyId, decision);
      await load();
    } finally {
      setBusyId(null);
    }
  };

  const decideTask = async (taskId: number, decision: ReviewDecision) => {
    const key = `T-${taskId}`;
    let reason: string | undefined;
    if (decision === 'REJECTED') {
      const input = window.prompt('打回原因（必填，≤500 字）：');
      if (!input || !input.trim()) return;
      reason = input.trim();
    }
    setBusyId(key);
    try {
      await submitTaskReview(taskId, decision, reason);
      await load();
    } finally {
      setBusyId(null);
    }
  };

  const activeRows = tab === 'STORY' ? storyRows : taskRows;
  const allEmpty = !loading && rows.length === 0;

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2>我的评审</h2>
      </div>

      {allEmpty ? (
        <EmptyState message="当前没有待你评审的 Story / Task。" testId="reviews-empty" />
      ) : (
        <>
          <StatTiles
            testId="reviews-summary"
            tiles={[
              {
                label: 'Story 待评审',
                value: storyRows.length,
                tier: storyRows.length > 0 ? 'yellow' : 'gray',
              },
              {
                label: 'Task 待评审',
                value: taskRows.length,
                tier: taskRows.length > 0 ? 'yellow' : 'gray',
              },
            ]}
          />

          <div className="rainier-tabs" data-testid="reviews-tabs" style={{ marginTop: 12 }}>
            <Button
              type="button"
              variant={tab === 'STORY' ? 'primary' : 'secondary'}
              onClick={() => setTab('STORY')}
              data-testid="reviews-tab-story"
            >
              Story ({storyRows.length})
            </Button>
            <Button
              type="button"
              variant={tab === 'TASK' ? 'primary' : 'secondary'}
              style={{ marginLeft: 6 }}
              onClick={() => setTab('TASK')}
              data-testid="reviews-tab-task"
            >
              Task ({taskRows.length})
            </Button>
          </div>

          <DashboardCard
            title={tab === 'STORY' ? '待我评审的 Story' : '待我评审的 Task'}
            testId="reviews-list"
          >
            {activeRows.length === 0 ? (
              <div style={{ padding: 12, color: 'var(--rainier-color-text-2)' }}>
                当前 {tab === 'STORY' ? 'Story' : 'Task'} 队列为空。
              </div>
            ) : (
              <table className="rainier-list-table">
                <tbody>
                  {activeRows.map((r) => {
                    const id =
                      tab === 'STORY' ? (r.storyId as number) : (r.taskId as number);
                    const rowKey = `${tab === 'STORY' ? 'S' : 'T'}-${id}`;
                    return (
                      <tr key={rowKey} data-testid={`reviews-row-${rowKey}`}>
                        <td style={{ padding: '6px 8px', width: 72 }}>
                          <StatusChip
                            status={r.priority}
                            label={r.priority}
                            testId={`reviews-prio-${rowKey}`}
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
                            disabled={busyId === rowKey}
                            onClick={() =>
                              tab === 'STORY'
                                ? void decideStory(id, 'APPROVED')
                                : void decideTask(id, 'APPROVED')
                            }
                            data-testid={`reviews-approve-${rowKey}`}
                          >
                            通过
                          </Button>
                          <Button
                            type="button"
                            variant="secondary"
                            style={{ marginLeft: 6 }}
                            disabled={busyId === rowKey}
                            onClick={() =>
                              tab === 'STORY'
                                ? void decideStory(id, 'REJECTED')
                                : void decideTask(id, 'REJECTED')
                            }
                            data-testid={`reviews-reject-${rowKey}`}
                          >
                            打回
                          </Button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </DashboardCard>
        </>
      )}
    </div>
  );
}
