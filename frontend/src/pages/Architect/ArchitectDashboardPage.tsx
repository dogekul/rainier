import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  DashboardCard,
  EmptyState,
  OwnerChip,
  StatTiles,
  StatusChip,
} from '../../components/board';
import { Button } from '../../components/ui/Button';
import {
  getPendingReviews,
  getReviewStats,
  type PendingReview,
  type ReviewStats,
} from '../../api/reviews';

/**
 * v0.0.112 (H5) — 架构师角色落地页。Consumes:
 *
 *  - `GET /api/me/review-stats` → 4 张顶部统计卡（待评审 Story / 待评审 Task / 本周通过 /
 *    本周打回）。本周计数基于 Story/Task `updateTime`（H5 暂用 updateTime 作为
 *    reviewedAt 近似）。
 *  - `GET /api/me/pending-reviews` → Story / Task tab 列表 + 一个「最近决定」List
 *    （从 PENDING 列表反推不到 reviewedAt，此处只展示 PENDING 队列；真·decision
 *    history 留待 reviewedAt schema 落地后再补）。
 *
 * 路径 `/architect`。所有用户可见 nav；具体角色权限校验留待 architect role 概念引入。
 */
export function ArchitectDashboardPage() {
  const [stats, setStats] = useState<ReviewStats | null>(null);
  const [rows, setRows] = useState<PendingReview[]>([]);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<'STORY' | 'TASK'>('STORY');

  useEffect(() => {
    let active = true;
    setLoading(true);
    Promise.all([getReviewStats(), getPendingReviews()])
      .then(([s, r]) => {
        if (!active) return;
        setStats(s);
        setRows(r);
        setLoading(false);
      })
      .catch(() => {
        if (!active) return;
        setStats({
          pendingStoryCount: 0,
          pendingTaskCount: 0,
          approvedThisWeek: 0,
          rejectedThisWeek: 0,
        });
        setRows([]);
        setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  const storyRows = useMemo(
    () => rows.filter((r) => (r.kind ?? 'STORY') === 'STORY'),
    [rows],
  );
  const taskRows = useMemo(() => rows.filter((r) => r.kind === 'TASK'), [rows]);
  const activeRows = tab === 'STORY' ? storyRows : taskRows;

  const tiles = stats
    ? [
        {
          label: '待我评审 Story',
          value: stats.pendingStoryCount,
          tier: (stats.pendingStoryCount > 0 ? 'yellow' : 'gray') as 'yellow' | 'gray',
        },
        {
          label: '待我评审 Task',
          value: stats.pendingTaskCount,
          tier: (stats.pendingTaskCount > 0 ? 'yellow' : 'gray') as 'yellow' | 'gray',
        },
        {
          label: '本周通过',
          value: stats.approvedThisWeek,
          tier: 'green' as const,
        },
        {
          label: '本周打回',
          value: stats.rejectedThisWeek,
          tier: (stats.rejectedThisWeek > 0 ? 'red' : 'gray') as 'red' | 'gray',
        },
      ]
    : [];

  return (
    <div className="rainier-page" data-testid="architect-page">
      <div className="rainier-page-head">
        <h2>架构师工作台</h2>
        <p style={{ color: 'var(--rainier-color-text-2)', margin: '4px 0 0' }}>
          评审吞吐 · 待办队列 · 本周战绩
        </p>
      </div>

      {stats && <StatTiles testId="architect-stats" tiles={tiles} />}

      {loading ? null : rows.length === 0 ? (
        <EmptyState
          message="当前没有待你评审的 Story / Task。"
          testId="architect-empty"
        />
      ) : (
        <>
          <div className="rainier-tabs" data-testid="architect-tabs" style={{ marginTop: 12 }}>
            <Button
              type="button"
              variant={tab === 'STORY' ? 'primary' : 'secondary'}
              onClick={() => setTab('STORY')}
              data-testid="architect-tab-story"
            >
              Story ({storyRows.length})
            </Button>
            <Button
              type="button"
              variant={tab === 'TASK' ? 'primary' : 'secondary'}
              style={{ marginLeft: 6 }}
              onClick={() => setTab('TASK')}
              data-testid="architect-tab-task"
            >
              Task ({taskRows.length})
            </Button>
          </div>

          <DashboardCard
            title={tab === 'STORY' ? '待我评审的 Story' : '待我评审的 Task'}
            testId="architect-pending-list"
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
                      <tr key={rowKey} data-testid={`architect-row-${rowKey}`}>
                        <td style={{ padding: '6px 8px', width: 72 }}>
                          <StatusChip status={r.priority} label={r.priority} />
                        </td>
                        <td style={{ padding: '6px 8px' }}>
                          <div>
                            {r.code} {r.title}
                          </div>
                          <div
                            style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}
                          >
                            {r.projectName ?? '—'}
                            {r.sprintName ? ` · ${r.sprintName}` : ''}
                          </div>
                        </td>
                        <td style={{ padding: '6px 8px', width: 140 }}>
                          <OwnerChip name={r.ownerName} loginName={r.ownerLoginName} />
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

      <div style={{ marginTop: 12 }}>
        <Link to="/reviews" data-testid="architect-go-reviews">
          前往评审看板 →
        </Link>
      </div>
    </div>
  );
}

export default ArchitectDashboardPage;
