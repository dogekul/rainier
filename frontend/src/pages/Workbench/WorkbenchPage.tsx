import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Card } from '../../components/ui';
import { StatusChip } from '../../components/board';
import { isOverdue, todayISO } from '../../utils/board';
import { useAuthStore } from '../../store/auth';
import { listTasks, updateTask, type Task, type TaskStatus } from '../../api/task';
import { listStories, type Story } from '../../api/story';
import type { Priority } from '../../api/demand';
import './WorkbenchPage.css';

const TASK_STATUS_OPTIONS: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE', 'BLOCKED', 'CANCELLED'];

const PRIORITY_RANK: Record<Priority, number> = {
  URGENT: 0,
  HIGH: 1,
  MEDIUM: 2,
  LOW: 3,
  LOWEST: 4,
};

const CLOSED: TaskStatus[] = ['DONE', 'CANCELLED'];

/** v0.0.32 今日聚焦 — bucket a task by urgency: overdue → today → future-due → no-due; closed sinks. */
function focusBucket(t: Task, today: string): { rank: number; flag: '逾期' | '今天' | null } {
  if (CLOSED.includes(t.status)) return { rank: 9, flag: null };
  const due = t.dueDate ?? null;
  if (isOverdue(due, today)) return { rank: 0, flag: '逾期' };
  if (due && due.slice(0, 10) === today) return { rank: 1, flag: '今天' };
  if (due) return { rank: 2, flag: null };
  return { rank: 3, flag: null };
}

/**
 * 我的工作台 — the personal home (飞书项目 / Linear "My Work" style, v0.0.36): a compact greeting, four
 * KPI tiles (待办/进行中/逾期/本周到期 over my tasks), then a two-column body — my tasks as dense
 * urgency-sorted rows (inline status quick-change) on the left, my Story + my projects on the right.
 * Pure entity aggregation; reads the store user (hydrated by ProtectedRoute), no AI.
 */
export function WorkbenchPage() {
  const ctx = useAuthStore((s) => s.user);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [stories, setStories] = useState<Story[]>([]);

  const loadWork = useCallback(async (userId: number) => {
    const [t, s] = await Promise.all([
      listTasks({ assigneeUserId: userId, size: 100 }),
      listStories({ ownerUserId: userId, size: 100 }),
    ]);
    setTasks(t.content);
    setStories(s.content);
  }, []);

  const userId = ctx?.id;
  useEffect(() => {
    if (userId == null) return;
    void loadWork(userId).catch(() => undefined);
  }, [userId, loadWork]);

  const changeTaskStatus = async (task: Task, status: TaskStatus) => {
    try {
      await updateTask(task.id, {
        code: task.code,
        title: task.title,
        description: task.description ?? undefined,
        status,
        priority: task.priority,
        assigneeUserId: task.assigneeUserId ?? undefined,
        dueDate: task.dueDate ?? undefined,
        closeReason: task.closeReason ?? undefined,
      });
    } catch {
      // server state is the source of truth — the reload below resyncs the select either way.
    }
    if (ctx?.id != null) await loadWork(ctx.id).catch(() => undefined);
  };

  const roles = ctx?.roles ?? [];
  const projects = ctx?.projects ?? [];
  const today = todayISO();

  const sortedTasks = useMemo(() => {
    return [...tasks].sort((a, b) => {
      const ra = focusBucket(a, today);
      const rb = focusBucket(b, today);
      if (ra.rank !== rb.rank) return ra.rank - rb.rank;
      return (PRIORITY_RANK[a.priority] ?? 9) - (PRIORITY_RANK[b.priority] ?? 9);
    });
  }, [tasks, today]);

  // KPI tiles over my tasks.
  const kpi = useMemo(() => {
    const weekEnd = todayISO(new Date(Date.now() + 7 * 86400000));
    const open = tasks.filter((t) => !CLOSED.includes(t.status));
    return {
      todo: tasks.filter((t) => t.status === 'TODO').length,
      progress: tasks.filter((t) => t.status === 'IN_PROGRESS').length,
      overdue: open.filter((t) => isOverdue(t.dueDate, today)).length,
      week: open.filter((t) => {
        const d = t.dueDate?.slice(0, 10);
        return d != null && d >= today && d <= weekEnd;
      }).length,
    };
  }, [tasks, today]);

  return (
    <div className="rainier-page">
      <div className="wb-hello">
        <h2 data-testid="workbench-greeting">你好，{ctx?.name ?? ctx?.username ?? '...'} 👋</h2>
        <div data-testid="workbench-roles" className="wb-roles">
          {roles.length === 0 ? (
            <span style={{ color: 'var(--rainier-color-text-2)', fontSize: 12 }}>（暂无角色）</span>
          ) : (
            roles.map((r, i) => (
              <span key={i} className="wb-role-chip">
                {r.roleName ?? r.roleCode ?? '?'}
                {r.projectName ? `@${r.projectName}` : '（组织级）'}
              </span>
            ))
          )}
        </div>
      </div>

      <div className="wb-kpis">
        <div className="wb-kpi">
          <div className="wb-kpi-num">{kpi.todo}</div>
          <div className="wb-kpi-label">待办</div>
        </div>
        <div className="wb-kpi">
          <div className="wb-kpi-num">{kpi.progress}</div>
          <div className="wb-kpi-label">进行中</div>
        </div>
        <div className={`wb-kpi${kpi.overdue > 0 ? ' is-red' : ''}`}>
          <div className="wb-kpi-num">{kpi.overdue}</div>
          <div className="wb-kpi-label">逾期</div>
        </div>
        <div className={`wb-kpi${kpi.week > 0 ? ' is-yellow' : ''}`}>
          <div className="wb-kpi-num">{kpi.week}</div>
          <div className="wb-kpi-label">本周到期</div>
        </div>
      </div>

      <div className="wb-grid">
        {/* left: my tasks */}
        <Card data-testid="wb-tasks">
          <div className="wb-card-head">
            <h3>我的任务</h3>
            <span className="wb-count">今日聚焦 · {sortedTasks.length}</span>
          </div>
          {sortedTasks.length === 0 ? (
            <p className="wb-empty">暂无指派给我的任务。🎉</p>
          ) : (
            sortedTasks.map((t) => {
              const flag = focusBucket(t, today).flag;
              return (
                <div key={t.id} className="wb-task" data-testid={`my-task-${t.id}`}>
                  {flag && (
                    <span
                      className="wb-flag"
                      data-testid={`my-task-flag-${t.id}`}
                      style={{
                        color: flag === '逾期' ? 'var(--rainier-status-red)' : 'var(--rainier-status-yellow)',
                        background:
                          flag === '逾期' ? 'var(--rainier-status-red-bg)' : 'var(--rainier-status-yellow-bg)',
                      }}
                    >
                      {flag}
                    </span>
                  )}
                  <Link to="/pm/tasks" className="wb-task-title">
                    {t.title}
                  </Link>
                  <div className="wb-task-meta">
                    {t.projectName && <span className="wb-proj-chip">{t.projectName}</span>}
                    <span className="wb-due">{t.dueDate ?? ''}</span>
                    <select
                      className="rainier-select"
                      data-testid={`my-task-status-${t.id}`}
                      value={t.status}
                      onChange={(e) => void changeTaskStatus(t, e.target.value as TaskStatus)}
                    >
                      {TASK_STATUS_OPTIONS.map((s) => (
                        <option key={s} value={s}>
                          {s}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              );
            })
          )}
        </Card>

        {/* right: my story + my projects */}
        <div className="wb-col">
          <Card>
            <div className="wb-card-head">
              <h3>我的 Story</h3>
              <span className="wb-count">{stories.length}</span>
            </div>
            {stories.length === 0 ? (
              <p className="wb-empty">暂无我负责的 Story。</p>
            ) : (
              stories.map((s) => (
                <div key={s.id} className="wb-list-row" data-testid={`my-story-${s.id}`}>
                  <Link to="/pm/sprints" className="wb-list-row-title">
                    {s.title}
                  </Link>
                  <StatusChip status={s.status} />
                </div>
              ))
            )}
          </Card>

          <Card>
            <div className="wb-card-head">
              <h3>我的项目</h3>
              <span className="wb-count">{projects.length}</span>
            </div>
            {projects.length === 0 ? (
              <p className="wb-empty">暂无我参与的项目。</p>
            ) : (
              projects.map((p) => (
                <div key={p.id} className="wb-list-row" data-testid={`my-project-${p.id}`}>
                  <Link to="/pm/projects" className="wb-list-row-title">
                    <span style={{ color: 'var(--rainier-color-text-3)', fontSize: 12 }}>{p.code}</span>{' '}
                    {p.name}
                  </Link>
                </div>
              ))
            )}
          </Card>
        </div>
      </div>
    </div>
  );
}
