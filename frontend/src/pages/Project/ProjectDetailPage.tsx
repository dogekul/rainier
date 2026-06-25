import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { OwnerChip, StatusChip } from '../../components/board';
import { Button } from '../../components/ui/Button';
import { MarkdownView } from '../../components/ui/MarkdownView';
import { Table, type TableColumn } from '../../components/ui/Table';
import { PROJECT_STATUS_LABELS } from '../../constants/labels';
import { formatDate, formatDateTime } from '../../utils/formatDate';
import {
  getProject,
  PROJECT_TYPE_LABELS,
  updateProject,
  type Project,
  type ProjectStatus,
  type ProjectUpdate,
} from '../../api/project';
import {
  listRequirements,
  REQUIREMENT_STATUS_LABELS,
  type Requirement,
  type RequirementStatus,
} from '../../api/requirement';
import { listSprints, type Sprint, type SprintStatus } from '../../api/sprint';
import { listTasks, TASK_STATUS_LABELS, type Task, type TaskStatus } from '../../api/task';
import { PRIORITY_LABELS } from '../../api/demand';
import { ProjectEditDrawer } from './ProjectEditDrawer';
import { MilestonesPanel } from './MilestonesPanel';
import '../Pm/PmDetailPage.css';

const PROJECT_STATUS_TIER: Record<ProjectStatus, 'gray' | 'yellow' | 'green' | 'red'> = {
  PLANNING: 'gray',
  ACTIVE: 'yellow',
  ON_HOLD: 'red',
  DELIVERED: 'green',
  ARCHIVED: 'gray',
};

const REQUIREMENT_STATUS_TIER: Record<RequirementStatus, 'gray' | 'yellow' | 'green' | 'red'> = {
  DRAFT: 'gray',
  IN_APPROVAL: 'yellow',
  IN_ANALYSIS: 'yellow',
  IN_PROGRESS: 'yellow',
  DELIVERED: 'green',
  CLOSED: 'gray',
};

const SPRINT_STATUS_LABELS: Record<SprintStatus, string> = {
  PLANNING: '筹备',
  ACTIVE: '进行中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
};
const SPRINT_STATUS_TIER: Record<SprintStatus, 'gray' | 'yellow' | 'green' | 'red'> = {
  PLANNING: 'gray',
  ACTIVE: 'yellow',
  COMPLETED: 'green',
  CANCELLED: 'red',
};

const TASK_STATUS_TIER: Record<TaskStatus, 'gray' | 'yellow' | 'green' | 'red'> = {
  TODO: 'gray',
  IN_PROGRESS: 'yellow',
  DONE: 'green',
  BLOCKED: 'red',
  CANCELLED: 'red',
};

type Tab = 'info' | 'milestones' | 'requirements' | 'sprints' | 'tasks';

/**
 * v0.0.62 — Project 详情页 (/pm/projects/:id). Tabs: 基本信息 / 里程碑 / 需求 / Sprint / 任务.
 * Replaces the prior ProjectsPage row-expand-MilestonesPanel + edit-drawer pattern. Child entity
 * lists deep-link into Requirement/Sprint/Task detail pages (v0.0.61).
 */
export function ProjectDetailPage() {
  const { id: idParam } = useParams<{ id: string }>();
  const id = Number(idParam);
  const navigate = useNavigate();

  const [proj, setProj] = useState<Project | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [tab, setTab] = useState<Tab>('info');
  const [reqCount, setReqCount] = useState(0);
  const [sprintCount, setSprintCount] = useState(0);
  const [taskCount, setTaskCount] = useState(0);
  const [drawerOpen, setDrawerOpen] = useState(false);

  const load = useCallback(() => {
    if (!Number.isFinite(id)) {
      setError('无效的项目 ID');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    getProject(id)
      .then((p) => setProj(p))
      .catch(() => setError('未能加载该项目，可能已被删除或无权限'))
      .finally(() => setLoading(false));
    listRequirements({ projectId: id, size: 100 })
      .then((res) => setReqCount(res.total))
      .catch(() => setReqCount(0));
    listSprints({ projectId: id, size: 100 })
      .then((res) => setSprintCount(res.total))
      .catch(() => setSprintCount(0));
    listTasks({ projectId: id, size: 100 })
      .then((res) => setTaskCount(res.total))
      .catch(() => setTaskCount(0));
  }, [id]);

  useEffect(load, [load]);

  if (loading) return <div className="rainier-page">加载中…</div>;
  if (error || !proj) {
    return (
      <div className="rainier-page">
        <div className="rainier-error-banner">{error ?? '项目不存在'}</div>
        <div>
          <Link to="/pm/projects">← 返回项目列表</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="rainier-page pm-detail-page" data-testid="project-detail-page">
      <div className="pm-breadcrumb">
        <Link to="/pm/projects">项目</Link>
        <span className="pm-breadcrumb-sep">/</span>
        <span className="pm-breadcrumb-current">{proj.code}</span>
      </div>

      <div className="pm-detail-hero">
        <span className="pm-detail-hero-code">{proj.code}</span>
        <span className="pm-detail-hero-title">{proj.name}</span>
        <StatusChip
          status={proj.status}
          label={PROJECT_STATUS_LABELS[proj.status] ?? proj.status}
          tier={PROJECT_STATUS_TIER[proj.status]}
        />
        <StatusChip
          status={proj.projectType}
          label={PROJECT_TYPE_LABELS[proj.projectType] ?? proj.projectType}
          tier="gray"
        />
        {!proj.enabled && <StatusChip status="DISABLED" label="已停用" tier="gray" />}
        <span className="pm-detail-hero-actions">
          <Button type="button" variant="secondary" onClick={() => setDrawerOpen(true)} data-testid="project-detail-edit">
            编辑
          </Button>
        </span>
      </div>

      <div className="pm-tabs" data-testid="project-detail-tabs">
        <button type="button" className="pm-tab" data-active={tab === 'info'} onClick={() => setTab('info')} data-testid="project-tab-info">
          基本信息
        </button>
        <button type="button" className="pm-tab" data-active={tab === 'milestones'} onClick={() => setTab('milestones')} data-testid="project-tab-milestones">
          里程碑
        </button>
        <button type="button" className="pm-tab" data-active={tab === 'requirements'} onClick={() => setTab('requirements')} data-testid="project-tab-requirements">
          需求<span className="pm-tab-count">· {reqCount}</span>
        </button>
        <button type="button" className="pm-tab" data-active={tab === 'sprints'} onClick={() => setTab('sprints')} data-testid="project-tab-sprints">
          Sprint<span className="pm-tab-count">· {sprintCount}</span>
        </button>
        <button type="button" className="pm-tab" data-active={tab === 'tasks'} onClick={() => setTab('tasks')} data-testid="project-tab-tasks">
          任务<span className="pm-tab-count">· {taskCount}</span>
        </button>
      </div>

      {tab === 'info' && (
        <div className="pm-tab-pane">
          <div className="pm-info-grid">
            <span className="pm-info-label">名称</span>
            <span className="pm-info-value">{proj.name}</span>

            <span className="pm-info-label">编码</span>
            <span className="pm-info-value">{proj.code}</span>

            <span className="pm-info-label">类型</span>
            <span className="pm-info-value">
              {PROJECT_TYPE_LABELS[proj.projectType] ?? proj.projectType}
            </span>

            <span className="pm-info-label">状态</span>
            <span className="pm-info-value">
              <StatusChip
                status={proj.status}
                label={PROJECT_STATUS_LABELS[proj.status] ?? proj.status}
                tier={PROJECT_STATUS_TIER[proj.status]}
              />
            </span>

            <span className="pm-info-label">负责人</span>
            <span className="pm-info-value">
              <OwnerChip name={proj.ownerName} loginName={proj.ownerLoginName} />
            </span>

            <span className="pm-info-label">起止</span>
            <span className="pm-info-value">
              {formatDate(proj.startDate)} → {formatDate(proj.endDate)}
            </span>

            <span className="pm-info-label">启用</span>
            <span className="pm-info-value">{proj.enabled ? '是' : '否'}</span>

            <span className="pm-info-label">创建/更新</span>
            <span className="pm-info-value" style={{ color: 'var(--rainier-color-text-3)', fontSize: 12 }}>
              {formatDateTime(proj.createTime)} · {formatDateTime(proj.updateTime)}
            </span>
          </div>

          {proj.description && (
            <div className="pm-info-section">
              <div className="pm-info-section-title">描述</div>
              <MarkdownView content={proj.description} />
            </div>
          )}
        </div>
      )}

      {tab === 'milestones' && (
        <div className="pm-tab-pane">
          <MilestonesPanel projectId={proj.id} />
        </div>
      )}

      {tab === 'requirements' && (
        <div className="pm-tab-pane">
          <ProjectRequirementList projectId={proj.id} onCountChange={setReqCount} />
        </div>
      )}

      {tab === 'sprints' && (
        <div className="pm-tab-pane">
          <ProjectSprintList projectId={proj.id} onCountChange={setSprintCount} />
        </div>
      )}

      {tab === 'tasks' && (
        <div className="pm-tab-pane">
          <ProjectTaskList projectId={proj.id} onCountChange={setTaskCount} />
        </div>
      )}

      <ProjectEditDrawer
        open={drawerOpen}
        editing={proj}
        onClose={() => setDrawerOpen(false)}
        onCreate={async () => {
          // not used: detail page never opens drawer in create mode
        }}
        onUpdate={async (pid, body: ProjectUpdate) => {
          await updateProject(pid, body);
          setDrawerOpen(false);
          load();
        }}
      />

      <div>
        <Button
          type="button"
          variant="secondary"
          onClick={() => navigate('/pm/projects')}
          data-testid="project-detail-back"
        >
          ← 返回项目列表
        </Button>
      </div>
    </div>
  );
}

function ProjectRequirementList({
  projectId,
  onCountChange,
}: {
  projectId: number;
  onCountChange: (n: number) => void;
}) {
  const navigate = useNavigate();
  const [rows, setRows] = useState<Requirement[]>([]);

  useEffect(() => {
    void listRequirements({ projectId, size: 100 })
      .then((res) => {
        setRows(res.content);
        onCountChange(res.total);
      })
      .catch(() => setRows([]));
  }, [projectId, onCountChange]);

  const columns: TableColumn<Requirement>[] = [
    { key: 'code', title: '编码', render: (r) => r.code },
    { key: 'title', title: '标题', render: (r) => r.title },
    {
      key: 'status',
      title: '状态',
      render: (r) => (
        <StatusChip
          status={r.status}
          label={REQUIREMENT_STATUS_LABELS[r.status] ?? r.status}
          tier={REQUIREMENT_STATUS_TIER[r.status]}
        />
      ),
    },
    { key: 'priority', title: '优先级', render: (r) => PRIORITY_LABELS[r.priority] ?? r.priority },
    {
      key: 'owner',
      title: '负责人',
      render: (r) => <OwnerChip name={r.ownerName} loginName={r.ownerLoginName} />,
    },
    { key: 'sprintCount', title: 'Sprint', render: (r) => (r.sprintCount ?? 0).toString() },
  ];

  return (
    <Table<Requirement>
      columns={columns}
      dataSource={rows}
      rowKey="id"
      onRowClick={(r) => navigate(`/pm/requirements/${r.id}`)}
      rowTestId={(r) => `project-req-row-${r.id}`}
      emptyText="该项目下还没有需求。"
    />
  );
}

function ProjectSprintList({
  projectId,
  onCountChange,
}: {
  projectId: number;
  onCountChange: (n: number) => void;
}) {
  const navigate = useNavigate();
  const [rows, setRows] = useState<Sprint[]>([]);

  useEffect(() => {
    void listSprints({ projectId, size: 100 })
      .then((res) => {
        setRows(res.content);
        onCountChange(res.total);
      })
      .catch(() => setRows([]));
  }, [projectId, onCountChange]);

  const columns: TableColumn<Sprint>[] = [
    { key: 'code', title: '编码', render: (s) => s.code },
    { key: 'name', title: '名称', render: (s) => s.name },
    {
      key: 'status',
      title: '状态',
      render: (s) => (
        <StatusChip
          status={s.status}
          label={SPRINT_STATUS_LABELS[s.status] ?? s.status}
          tier={SPRINT_STATUS_TIER[s.status]}
        />
      ),
    },
    {
      key: 'requirement',
      title: '所属需求',
      render: (s) => (s.requirementCode ? `${s.requirementTitle ?? ''}（${s.requirementCode}）` : '—'),
    },
    {
      key: 'owner',
      title: '负责人',
      render: (s) => <OwnerChip name={s.ownerName} loginName={s.ownerLoginName} />,
    },
    { key: 'storyCount', title: 'Story', render: (s) => (s.storyCount ?? 0).toString() },
  ];

  return (
    <Table<Sprint>
      columns={columns}
      dataSource={rows}
      rowKey="id"
      onRowClick={(s) => navigate(`/pm/sprints/${s.id}`)}
      rowTestId={(s) => `project-sprint-row-${s.id}`}
      emptyText="该项目下还没有 Sprint。"
    />
  );
}

function ProjectTaskList({
  projectId,
  onCountChange,
}: {
  projectId: number;
  onCountChange: (n: number) => void;
}) {
  const navigate = useNavigate();
  const [rows, setRows] = useState<Task[]>([]);

  useEffect(() => {
    void listTasks({ projectId, size: 100 })
      .then((res) => {
        setRows(res.content);
        onCountChange(res.total);
      })
      .catch(() => setRows([]));
  }, [projectId, onCountChange]);

  const columns: TableColumn<Task>[] = [
    { key: 'code', title: '编码', render: (t) => t.code },
    { key: 'title', title: '标题', render: (t) => t.title },
    {
      key: 'status',
      title: '状态',
      render: (t) => (
        <StatusChip
          status={t.status}
          label={TASK_STATUS_LABELS[t.status] ?? t.status}
          tier={TASK_STATUS_TIER[t.status]}
        />
      ),
    },
    { key: 'priority', title: '优先级', render: (t) => PRIORITY_LABELS[t.priority] ?? t.priority },
    {
      key: 'assignee',
      title: '指派人',
      render: (t) => <OwnerChip name={t.assigneeName} loginName={t.assigneeLoginName} />,
    },
    { key: 'dueDate', title: '到期', render: (t) => formatDate(t.dueDate) },
  ];

  return (
    <Table<Task>
      columns={columns}
      dataSource={rows}
      rowKey="id"
      onRowClick={(t) => navigate(`/pm/tasks/${t.id}`)}
      rowTestId={(t) => `project-task-row-${t.id}`}
      emptyText="该项目下还没有任务。"
    />
  );
}
