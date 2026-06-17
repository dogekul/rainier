import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { DashboardCard, EmptyState, StatusBar, StatusChip } from '../../components/board';
import { groupByStatus, isOverdue, rygToTier, RYG_LABEL, todayISO } from '../../utils/board';
import { useAuthStore } from '../../store/auth';
import { listTasks, updateTask, type Task, type TaskStatus } from '../../api/task';
import { listStories, updateStory, type Story, type StoryStatus } from '../../api/story';
import { listSprints, type Sprint } from '../../api/sprint';
import { listRequirements, type Requirement } from '../../api/requirement';
import { listMilestones, type Milestone } from '../../api/milestone';
import { getPortfolio, type PortfolioRow } from '../../api/portfolio';

const TASK_STATUS_OPTIONS: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE', 'BLOCKED', 'CANCELLED'];
const STORY_STATUS_OPTIONS: StoryStatus[] = [
  'DRAFT',
  'READY',
  'IN_PROGRESS',
  'DONE',
  'BLOCKED',
  'CANCELLED',
];

const PAGE = 200;

interface CockpitData {
  requirements: Requirement[];
  sprints: Sprint[];
  stories: Story[];
  tasks: Task[];
  milestones: Milestone[];
}

const EMPTY: CockpitData = { requirements: [], sprints: [], stories: [], tasks: [], milestones: [] };

/**
 * v0.0.23 — PM 项目驾驶舱 (Project Cockpit). A read-first 飞书项目-style 仪表盘 over ONE auto-selected
 * project: status distribution for all 5 work-item types + an overdue/risk panel + 待办/待评审 action
 * lists with inline status quick-edit. Pure client aggregation over existing list endpoints — no backend.
 */
export function CockpitPage() {
  const user = useAuthStore((s) => s.user);
  const projects = useMemo(() => user?.projects ?? [], [user]);
  const [projectId, setProjectId] = useState<number | null>(projects[0]?.id ?? null);
  const [data, setData] = useState<CockpitData>(EMPTY);
  const [portfolio, setPortfolio] = useState<PortfolioRow[]>([]);
  const today = todayISO();

  // Smart default: once the user context hydrates, select the first project if none chosen yet.
  useEffect(() => {
    if (projectId == null && projects.length > 0) setProjectId(projects[0].id);
  }, [projects, projectId]);

  // v0.0.29: cross-project health strip — all my projects at a glance (server-side RYG), worst-first.
  useEffect(() => {
    let active = true;
    void getPortfolio('mine')
      .then((rows) => {
        if (active) setPortfolio(rows);
      })
      .catch(() => {
        if (active) setPortfolio([]);
      });
    return () => {
      active = false;
    };
  }, []);

  const fetchData = useCallback(async (pid: number): Promise<CockpitData> => {
    const [requirements, sprints, stories, tasks, milestones] = await Promise.all([
      listRequirements({ projectId: pid, size: PAGE }),
      listSprints({ projectId: pid, size: PAGE }),
      listStories({ projectId: pid, size: PAGE }),
      listTasks({ projectId: pid, size: PAGE }),
      listMilestones({ projectId: pid, size: PAGE }),
    ]);
    return {
      requirements: requirements.content,
      sprints: sprints.content,
      stories: stories.content,
      tasks: tasks.content,
      milestones: milestones.content,
    };
  }, []);

  // Guard against the project-switch race: a slow earlier load must not overwrite a newer selection.
  useEffect(() => {
    if (projectId == null) return;
    let active = true;
    void fetchData(projectId)
      .then((d) => {
        if (active) setData(d);
      })
      .catch(() => {
        if (active) setData(EMPTY);
      });
    return () => {
      active = false;
    };
  }, [projectId, fetchData]);

  const reload = () => {
    const pid = projectId;
    if (pid == null) return;
    void fetchData(pid)
      .then((d) => {
        // Only apply if the selection hasn't moved on since this reload started.
        if (pid === projectId) setData(d);
      })
      .catch(() => undefined);
  };

  const changeTaskStatus = async (t: Task, status: TaskStatus) => {
    try {
      await updateTask(t.id, {
        code: t.code,
        title: t.title,
        description: t.description ?? undefined,
        status,
        priority: t.priority,
        assigneeUserId: t.assigneeUserId ?? undefined,
        dueDate: t.dueDate ?? undefined,
        closeReason: t.closeReason ?? undefined,
      });
    } catch {
      // server state is the source of truth — the reload below resyncs the select either way.
    }
    reload();
  };

  const changeStoryStatus = async (s: Story, status: StoryStatus) => {
    try {
      await updateStory(s.id, {
        code: s.code,
        title: s.title,
        description: s.description ?? undefined,
        acceptanceCriteria: s.acceptanceCriteria ?? undefined,
        status,
        priority: s.priority,
        complexity: s.complexity ?? undefined,
        ownerUserId: s.ownerUserId,
        closeReason: s.closeReason ?? undefined,
      });
    } catch {
      // ignore — reload resyncs.
    }
    reload();
  };

  // ----- aggregations (pure, from the loaded lists) -----
  const overdueTasks = data.tasks.filter(
    (t) => t.status !== 'DONE' && t.status !== 'CANCELLED' && isOverdue(t.dueDate, today),
  );
  const overdueMilestones = data.milestones.filter(
    (m) => m.status !== 'REACHED' && isOverdue(m.targetDate, today),
  );
  const overdueRequirements = data.requirements.filter(
    (r) =>
      r.status !== 'DELIVERED' && r.status !== 'CLOSED' && isOverdue(r.expectedDate ?? null, today),
  );
  const overdueSprints = data.sprints.filter(
    (s) => s.status !== 'COMPLETED' && s.status !== 'CANCELLED' && isOverdue(s.endDate ?? null, today),
  );
  const overdueCount =
    overdueTasks.length + overdueMilestones.length + overdueRequirements.length + overdueSprints.length;

  const todoTasks = data.tasks.filter((t) => t.status === 'TODO' || t.status === 'IN_PROGRESS');
  const reviewStories = data.stories.filter((s) => s.status === 'DRAFT' || s.status === 'READY');

  if (projects.length === 0) {
    return (
      <EmptyState
        message="你还没有参与任何项目，无法展示驾驶舱。"
        cta={{ label: '去创建项目', to: '/pm/projects' }}
        testId="cockpit-empty"
      />
    );
  }

  const dists: { key: string; title: string; items: { status: string }[]; to: string }[] = [
    { key: 'requirement', title: '需求', items: data.requirements, to: '/pm/requirements' },
    { key: 'sprint', title: '迭代', items: data.sprints, to: '/pm/sprints' },
    { key: 'story', title: 'Story', items: data.stories, to: '/pm/sprints' },
    { key: 'task', title: '任务', items: data.tasks, to: '/pm/tasks' },
    { key: 'milestone', title: '里程碑', items: data.milestones, to: '/pm/projects' },
  ];

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2 style={{ margin: 0 }}>项目驾驶舱</h2>
        <select
          data-testid="cockpit-project-select"
          className="rainier-select"
          value={projectId ?? ''}
          onChange={(e) => setProjectId(Number(e.target.value))}
        >
          {projects.map((p) => (
            <option key={p.id} value={p.id}>
              {p.code} {p.name}
            </option>
          ))}
        </select>
      </div>

      {/* v0.0.29: cross-project health strip — all my projects, worst-first; click to drill in below */}
      {portfolio.length > 1 && (
        <DashboardCard
          title="我的项目（健康总览）"
          extra={`${portfolio.length} 个项目`}
          testId="cockpit-portfolio"
        >
          <div
            style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 8 }}
          >
            {portfolio.map((row) => {
              const selected = row.projectId === projectId;
              return (
                <button
                  key={row.projectId}
                  type="button"
                  data-testid={`cockpit-portfolio-${row.projectId}`}
                  onClick={() => setProjectId(row.projectId)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 8,
                    padding: '8px 10px',
                    borderRadius: 8,
                    border: selected
                      ? '1px solid var(--rainier-color-primary)'
                      : '1px solid var(--rainier-status-gray-bg)',
                    background: selected ? 'var(--rainier-bg-2, #f5f8ff)' : 'var(--rainier-bg-card)',
                    cursor: 'pointer',
                    textAlign: 'left',
                  }}
                >
                  <StatusChip
                    status=""
                    tier={rygToTier(row.ryg)}
                    label={RYG_LABEL[rygToTier(row.ryg)]}
                    testId={`cockpit-portfolio-ryg-${row.projectId}`}
                  />
                  <span style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {row.projectName}
                  </span>
                  <span style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
                    逾期 {row.overdueTasks}
                  </span>
                </button>
              );
            })}
          </div>
        </DashboardCard>
      )}

      {/* 5 status-distribution cards */}
      <div
        style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 12 }}
      >
        {dists.map((d) => (
          <DashboardCard
            key={d.key}
            title={<Link to={d.to}>{d.title}</Link>}
            extra={`共 ${d.items.length}`}
            testId={`cockpit-card-${d.key}`}
          >
            <StatusBar
              testId={`dist-${d.key}`}
              segments={groupByStatus(d.items, (i) => i.status).map((c) => ({
                label: c.status,
                count: c.count,
                status: c.status,
              }))}
            />
          </DashboardCard>
        ))}
      </div>

      {/* Overdue / risk panel */}
      <DashboardCard
        title="逾期 / 风险"
        extra={<StatusChip status={overdueCount > 0 ? 'BLOCKED' : 'DONE'} label={`${overdueCount} 项`} />}
        testId="cockpit-overdue"
      >
        {overdueCount === 0 ? (
          <p style={{ color: 'var(--rainier-color-text-2)', margin: 0 }}>无逾期项，进度健康 🎉</p>
        ) : (
          <ul style={{ margin: 0, paddingLeft: 18 }}>
            {overdueTasks.map((t) => (
              <li key={`t-${t.id}`} data-testid={`cockpit-overdue-task-${t.id}`}>
                <Link to="/pm/tasks">[任务] {t.title}</Link> — 截止 {t.dueDate}
              </li>
            ))}
            {overdueSprints.map((s) => (
              <li key={`s-${s.id}`} data-testid={`cockpit-overdue-sprint-${s.id}`}>
                <Link to="/pm/sprints">[迭代] {s.name}</Link> — 截止 {s.endDate}
              </li>
            ))}
            {overdueRequirements.map((r) => (
              <li key={`r-${r.id}`} data-testid={`cockpit-overdue-requirement-${r.id}`}>
                <Link to="/pm/requirements">[需求] {r.title}</Link> — 期望 {r.expectedDate}
              </li>
            ))}
            {overdueMilestones.map((m) => (
              <li key={`m-${m.id}`} data-testid={`cockpit-overdue-milestone-${m.id}`}>
                <Link to="/pm/projects">[里程碑] {m.name}</Link> — 目标 {m.targetDate}
              </li>
            ))}
          </ul>
        )}
      </DashboardCard>

      {/* 待办 (tasks) */}
      <DashboardCard title="我的待办（任务）" extra={`${todoTasks.length} 项`} testId="cockpit-todo">
        {todoTasks.length === 0 ? (
          <p style={{ color: 'var(--rainier-color-text-2)', margin: 0 }}>暂无待办任务。</p>
        ) : (
          <table className="rainier-list-table">
            <tbody>
              {todoTasks.map((t) => (
                <tr key={t.id} data-testid={`cockpit-todo-${t.id}`}>
                  <td style={{ padding: '4px 8px' }}>
                    <Link to="/pm/tasks">{t.title}</Link>
                  </td>
                  <td style={{ padding: '4px 8px' }}>
                    <select
                      className="rainier-select"
                      data-testid={`cockpit-todo-status-${t.id}`}
                      value={t.status}
                      onChange={(e) => void changeTaskStatus(t, e.target.value as TaskStatus)}
                    >
                      {TASK_STATUS_OPTIONS.map((s) => (
                        <option key={s} value={s}>
                          {s}
                        </option>
                      ))}
                    </select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </DashboardCard>

      {/* 待评审 (stories) */}
      <DashboardCard title="待评审（Story）" extra={`${reviewStories.length} 项`} testId="cockpit-review">
        {reviewStories.length === 0 ? (
          <p style={{ color: 'var(--rainier-color-text-2)', margin: 0 }}>暂无待评审 Story。</p>
        ) : (
          <table className="rainier-list-table">
            <tbody>
              {reviewStories.map((s) => (
                <tr key={s.id} data-testid={`cockpit-review-${s.id}`}>
                  <td style={{ padding: '4px 8px' }}>
                    <Link to="/pm/sprints">{s.title}</Link>
                  </td>
                  <td style={{ padding: '4px 8px' }}>
                    <select
                      className="rainier-select"
                      data-testid={`cockpit-review-status-${s.id}`}
                      value={s.status}
                      onChange={(e) => void changeStoryStatus(s, e.target.value as StoryStatus)}
                    >
                      {STORY_STATUS_OPTIONS.map((st) => (
                        <option key={st} value={st}>
                          {st}
                        </option>
                      ))}
                    </select>
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
