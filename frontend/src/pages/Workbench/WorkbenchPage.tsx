import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Card } from '../../components/ui';
import { useAuthStore } from '../../store/auth';
import { listTasks, updateTask, type Task, type TaskStatus } from '../../api/task';
import { listStories, type Story } from '../../api/story';

const TASK_STATUS_OPTIONS: TaskStatus[] = [
  'TODO',
  'IN_PROGRESS',
  'DONE',
  'BLOCKED',
  'CANCELLED',
];

/**
 * v0.0.18 — 我的工作台 (role pivot, step 1). Shows my roles + my tasks (assignee=me, with inline
 * status change) + my stories (owner=me) + my projects. Pure entity aggregation, no AI.
 *
 * <p>v0.0.20: reads the current-user context from the auth store (hydrated app-wide by ProtectedRoute
 * via GET /api/auth/me) instead of calling me() itself — so the page makes no duplicate auth call.
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
    // Leave the workbench in its empty state rather than crashing on a transient list error.
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
      // Swallow — the status <select> is controlled by server state, so the reload below resyncs
      // it to the truth whether the update succeeded or was rejected (e.g. a concurrent edit).
    }
    if (ctx?.id != null) await loadWork(ctx.id).catch(() => undefined);
  };

  const roles = ctx?.roles ?? [];
  const projects = ctx?.projects ?? [];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Card>
        <h2 data-testid="workbench-greeting">你好，{ctx?.name ?? ctx?.username ?? '...'}</h2>
        <div
          data-testid="workbench-roles"
          style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 8 }}
        >
          {roles.length === 0 ? (
            <span style={{ color: 'var(--rainier-color-text-2)' }}>（暂无角色）</span>
          ) : (
            roles.map((r, i) => (
              <span
                key={i}
                style={{
                  padding: '2px 10px',
                  borderRadius: 12,
                  background: 'var(--rainier-color-bg-2)',
                  fontSize: 12,
                }}
              >
                {r.roleName ?? r.roleCode ?? '?'}
                {r.projectName ? `@${r.projectName}` : '（组织级）'}
              </span>
            ))
          )}
        </div>
      </Card>

      <Card>
        <h3>我的任务</h3>
        {tasks.length === 0 ? (
          <p style={{ color: 'var(--rainier-color-text-2)' }}>暂无指派给我的任务。</p>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <tbody>
              {tasks.map((t) => (
                <tr key={t.id} data-testid={`my-task-${t.id}`}>
                  <td style={{ padding: '4px 8px' }}>
                    <Link to="/pm/tasks">{t.title}</Link>
                  </td>
                  <td style={{ padding: '4px 8px', color: 'var(--rainier-color-text-2)' }}>
                    {t.projectName ?? ''}
                  </td>
                  <td style={{ padding: '4px 8px' }}>
                    <select
                      className="rainier-treeselect-trigger"
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
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      <Card>
        <h3>我的 Story</h3>
        {stories.length === 0 ? (
          <p style={{ color: 'var(--rainier-color-text-2)' }}>暂无我负责的 Story。</p>
        ) : (
          <ul style={{ margin: 0, paddingLeft: 18 }}>
            {stories.map((s) => (
              <li key={s.id} data-testid={`my-story-${s.id}`}>
                <Link to="/pm/sprints">{s.title}</Link> — {s.status}
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Card>
        <h3>我的项目</h3>
        {projects.length === 0 ? (
          <p style={{ color: 'var(--rainier-color-text-2)' }}>暂无我参与的项目。</p>
        ) : (
          <ul style={{ margin: 0, paddingLeft: 18 }}>
            {projects.map((p) => (
              <li key={p.id} data-testid={`my-project-${p.id}`}>
                <Link to="/pm/projects">
                  {p.code} {p.name}
                </Link>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}
