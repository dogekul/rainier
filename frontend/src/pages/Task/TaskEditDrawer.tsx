import { useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { LinkPanel } from '../../components/LinkPanel';
import { listProjects, type Project } from '../../api/project';
import { listSprints, type Sprint } from '../../api/sprint';
import { listStories, type Story } from '../../api/story';
import { listUsers, type User } from '../../api/user';
import { PRIORITY_LABELS } from '../../api/demand';
import {
  type Task,
  type TaskCreate,
  type TaskStatus,
  type TaskUpdate,
} from '../../api/task';

const STATUS_OPTIONS: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE', 'BLOCKED', 'CANCELLED'];
const PRIORITY_OPTIONS = ['LOW', 'MEDIUM', 'HIGH', 'URGENT', 'LOWEST'] as const;

export interface TaskEditDrawerProps {
  open: boolean;
  editing: Task | null;
  onClose: () => void;
  onCreate: (body: TaskCreate) => Promise<void> | void;
  onUpdate: (id: number, body: TaskUpdate) => Promise<void> | void;
}

export function TaskEditDrawer({
  open,
  editing,
  onClose,
  onCreate,
  onUpdate,
}: TaskEditDrawerProps) {
  const [projects, setProjects] = useState<Project[]>([]);
  const [sprints, setSprints] = useState<Sprint[]>([]);
  const [stories, setStories] = useState<Story[]>([]);
  const [users, setUsers] = useState<User[]>([]);

  const [code, setCode] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [status, setStatus] = useState<TaskStatus>('TODO');
  const [priority, setPriority] = useState<(typeof PRIORITY_OPTIONS)[number]>('MEDIUM');
  const [projectId, setProjectId] = useState<number | ''>('');
  const [sprintId, setSprintId] = useState<number | ''>('');
  const [storyId, setStoryId] = useState<number | ''>('');
  const [assigneeUserId, setAssigneeUserId] = useState<number | ''>('');
  const [dueDate, setDueDate] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      setFormError(null);
      return;
    }
    // v0.0.12.1 A2: server-side filter — open() loads only Projects + Users.
    // Sprints/Stories fetched lazily when Project/Sprint changes.
    void Promise.all([listProjects({ size: 100 }), listUsers({ size: 100 })]).then(
      ([pr, us]) => {
        setProjects(pr.content);
        setUsers(us.content);
      },
    );
    if (editing) {
      setCode(editing.code);
      setTitle(editing.title);
      setDescription(editing.description ?? '');
      setStatus(editing.status);
      setPriority(editing.priority);
      setProjectId(editing.projectId);
      setSprintId(editing.sprintId ?? '');
      setStoryId(editing.storyId ?? '');
      setAssigneeUserId(editing.assigneeUserId ?? '');
      setDueDate(editing.dueDate ?? '');
    } else {
      setCode('');
      setTitle('');
      setDescription('');
      setStatus('TODO');
      setPriority('MEDIUM');
      setProjectId('');
      setSprintId('');
      setStoryId('');
      setSprints([]);
      setStories([]);
      setAssigneeUserId('');
      setDueDate('');
    }
  }, [open, editing]);

  // v0.0.12.1 A2: refetch Sprints whenever projectId changes (server-side filter).
  useEffect(() => {
    if (!open || projectId === '') {
      return;
    }
    void listSprints({ projectId, size: 100 }).then((res) => setSprints(res.content));
  }, [open, projectId]);

  // v0.0.12.1 A2: refetch Stories — narrower by sprintId if present, else by projectId.
  useEffect(() => {
    if (!open || projectId === '') {
      return;
    }
    const params: { projectId?: number; sprintId?: number; size: number } = { size: 100 };
    if (sprintId !== '') {
      params.sprintId = sprintId;
    } else {
      params.projectId = projectId;
    }
    void listStories(params).then((res) => setStories(res.content));
  }, [open, projectId, sprintId]);

  // Decision 9: Sprint/Story 联动 — when project changes, clear sprint/story (effects refetch).
  const handleProjectChange = (next: number | '') => {
    setProjectId(next);
    setSprintId('');
    setStoryId('');
    setSprints([]);
    setStories([]);
    setFormError(null);
  };

  return (
    <Drawer
      open={open}
      title={editing ? '编辑任务' : '新建任务'}
      onClose={onClose}
    >
      <Input
        label="编码 (TASK-...)"
        value={code}
        onChange={(e) => setCode(e.target.value)}
        disabled={editing !== null}
      />
      <Input label="标题" value={title} onChange={(e) => setTitle(e.target.value)} />
      <Input
        label="描述"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
      />
      <div style={{ marginBottom: 12 }}>
        <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>项目</label>
        <select
          className="rainier-treeselect-trigger"
          value={projectId}
          onChange={(e) =>
            handleProjectChange(e.target.value === '' ? '' : Number(e.target.value))
          }
          disabled={editing !== null}
          data-testid="task-project-select"
        >
          <option value="">请选择项目</option>
          {projects.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}（{p.code}）
            </option>
          ))}
        </select>
      </div>
      <div style={{ marginBottom: 12 }}>
        <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
          Sprint（可选）
        </label>
        <select
          className="rainier-treeselect-trigger"
          value={sprintId}
          onChange={(e) => {
            const next = e.target.value === '' ? '' : Number(e.target.value);
            setSprintId(next);
            if (next === '') setStoryId('');
          }}
          disabled={editing !== null}
          data-testid="task-sprint-select"
        >
          <option value="">未选 Sprint</option>
          {sprints.map((s) => (
            <option key={s.id} value={s.id}>
              {s.name}（{s.code}）
            </option>
          ))}
        </select>
      </div>
      <div style={{ marginBottom: 12 }}>
        <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
          Story（可选）
        </label>
        <select
          className="rainier-treeselect-trigger"
          value={storyId}
          onChange={(e) =>
            setStoryId(e.target.value === '' ? '' : Number(e.target.value))
          }
          disabled={editing !== null}
          data-testid="task-story-select"
        >
          <option value="">未选 Story</option>
          {stories.map((s) => (
            <option key={s.id} value={s.id}>
              {s.title}（{s.code}）
            </option>
          ))}
        </select>
      </div>
      <div style={{ marginBottom: 12 }}>
        <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
          指派人（可选 — 留空 = 未分配）
        </label>
        <select
          className="rainier-treeselect-trigger"
          value={assigneeUserId}
          onChange={(e) =>
            setAssigneeUserId(e.target.value === '' ? '' : Number(e.target.value))
          }
          data-testid="task-assignee-select"
        >
          <option value="">待分配</option>
          {users.map((u) => (
            <option key={u.id} value={u.id}>
              {u.name}（{u.loginName}）
            </option>
          ))}
        </select>
      </div>
      <div style={{ marginBottom: 12 }}>
        <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>状态</label>
        <select
          className="rainier-treeselect-trigger"
          value={status}
          onChange={(e) => setStatus(e.target.value as TaskStatus)}
        >
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>
      <div style={{ marginBottom: 12 }}>
        <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>优先级</label>
        <select
          className="rainier-treeselect-trigger"
          value={priority}
          onChange={(e) =>
            setPriority(e.target.value as (typeof PRIORITY_OPTIONS)[number])
          }
        >
          {PRIORITY_OPTIONS.map((p) => (
            <option key={p} value={p}>
              {PRIORITY_LABELS[p]}
            </option>
          ))}
        </select>
      </div>
      <Input
        label="到期日 (可选)"
        value={dueDate}
        onChange={(e) => setDueDate(e.target.value)}
      />
      {formError && (
        <div
          style={{
            padding: '6px 10px',
            marginBottom: 12,
            color: 'var(--rainier-color-danger, #d4380d)',
            fontSize: 12,
            background: 'rgba(212, 56, 13, 0.08)',
            borderRadius: 4,
          }}
          data-testid="task-form-error"
        >
          {formError}
        </div>
      )}
      {/* v0.0.31: relate external artifacts (PR/缺陷/用例…) once the task exists. */}
      {editing && <LinkPanel targetType="TASK" targetId={editing.id} />}
      <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
        <Button type="button" variant="secondary" onClick={onClose}>
          取消
        </Button>
        <Button
          type="button"
          onClick={async () => {
            if (projectId === '' || projectId === null) {
              setFormError('请选择项目');
              return;
            }
            setFormError(null);
            if (editing) {
              await onUpdate(editing.id, {
                code,
                title,
                description: description || undefined,
                status,
                priority,
                assigneeUserId: assigneeUserId === '' ? null : assigneeUserId,
                dueDate: dueDate || undefined,
              });
            } else {
              await onCreate({
                code,
                title,
                description: description || undefined,
                status,
                priority,
                projectId,
                sprintId: sprintId === '' ? undefined : sprintId,
                storyId: storyId === '' ? undefined : storyId,
                assigneeUserId: assigneeUserId === '' ? undefined : assigneeUserId,
                dueDate: dueDate || undefined,
              });
            }
          }}
        >
          保存
        </Button>
      </div>
    </Drawer>
  );
}
