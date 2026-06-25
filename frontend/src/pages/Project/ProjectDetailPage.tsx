import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { OwnerChip, StatusChip } from '../../components/board';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { MarkdownView } from '../../components/ui/MarkdownView';
import { Table, type TableColumn } from '../../components/ui/Table';
import { TreeSelect, type TreeNode } from '../../components/ui/TreeSelect';
import { PROJECT_STATUS_LABELS } from '../../constants/labels';
import { formatDate, formatDateTime } from '../../utils/formatDate';
import {
  getProject,
  PROJECT_TYPE_LABELS,
  PROJECT_TYPE_OPTIONS,
  updateProject,
  type Project,
  type ProjectStatus,
  type ProjectType,
  type ProjectUpdate,
} from '../../api/project';
import { getOrganizationTree } from '../../api/organization';
import { listEffectivePmos, type EffectivePmoDetail } from '../../api/organizationPmo';
import { listUserOrganizations } from '../../api/userOrganization';
import {
  listRequirements,
  REQUIREMENT_STATUS_LABELS,
  type Requirement,
  type RequirementStatus,
} from '../../api/requirement';
import { listSprints, type Sprint, type SprintStatus } from '../../api/sprint';
import { listTasks, TASK_STATUS_LABELS, type Task, type TaskStatus } from '../../api/task';
import { PRIORITY_LABELS } from '../../api/demand';
import {
  addProjectMember,
  listProjectMembers,
  PROJECT_MEMBER_ROLE_LABELS,
  PROJECT_MEMBER_ROLE_OPTIONS,
  removeProjectMember,
  updateProjectMemberRole,
  type ProjectMemberDetail,
  type ProjectMemberRole,
} from '../../api/projectMember';
import { listUsers, type User } from '../../api/user';
import { isElevated, useAuthStore } from '../../store/auth';
import { MilestonesPanel } from './MilestonesPanel';
import {
  getProjectImplementation,
  upsertProjectImplementation,
  type ProjectImplementation as PIType,
} from '../../api/projectImplementation';
import '../Pm/PmDetailPage.css';

const STATUS_OPTIONS: ProjectStatus[] = [
  'PLANNING',
  'ACTIVE',
  'ON_HOLD',
  'DELIVERED',
  'ARCHIVED',
];

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

type Tab =
  | 'info'
  | 'members'
  | 'milestones'
  | 'implementation'
  | 'requirements'
  | 'sprints'
  | 'tasks';

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
  const [memberCount, setMemberCount] = useState(0);
  const [reqCount, setReqCount] = useState(0);
  const [sprintCount, setSprintCount] = useState(0);
  const [taskCount, setTaskCount] = useState(0);

  // v0.0.64 → revised — 编辑改为「基本信息」Tab 内联编辑（用户：项目的编辑页 不要使用侧抽屉）
  const [editing, setEditing] = useState(false);
  const [users, setUsers] = useState<User[]>([]);
  const [orgTree, setOrgTree] = useState<TreeNode[]>([]);
  const [eName, setEName] = useState('');
  const [eDescription, setEDescription] = useState('');
  const [eStatus, setEStatus] = useState<ProjectStatus>('PLANNING');
  const [eProjectType, setEProjectType] = useState<ProjectType>('CASUAL');
  const [eOwnerUserId, setEOwnerUserId] = useState<number | ''>('');
  const [eOrganizationId, setEOrganizationId] = useState<number | null>(null);
  const [ePmoUserId, setEPmoUserId] = useState<number | ''>('');
  const [pmoCandidates, setPmoCandidates] = useState<EffectivePmoDetail[]>([]);
  const [pmoLoading, setPmoLoading] = useState(false);
  const [eStartDate, setEStartDate] = useState('');
  const [eEndDate, setEEndDate] = useState('');
  const [eEnabled, setEEnabled] = useState(true);
  const [eError, setEError] = useState<string | null>(null);
  const [eSaving, setESaving] = useState(false);
  const lastPmoReqId = useRef(0);

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
    listProjectMembers(id)
      .then((res) => setMemberCount(res.length))
      .catch(() => setMemberCount(0));
  }, [id]);

  useEffect(load, [load]);

  // v0.0.64 — Edit-mode: prefill 表单 + 拉 users / org tree
  const startEdit = useCallback(() => {
    if (!proj) return;
    setEName(proj.name);
    setEDescription(proj.description ?? '');
    setEStatus(proj.status);
    setEProjectType(proj.projectType);
    setEOwnerUserId(proj.ownerUserId);
    setEOrganizationId(proj.organizationId ?? null);
    setEPmoUserId(proj.pmoUserId ?? '');
    setEStartDate(proj.startDate ?? '');
    setEEndDate(proj.endDate ?? '');
    setEEnabled(proj.enabled);
    setEError(null);
    setEditing(true);
    void listUsers({ size: 100 }).then((r) => setUsers(r.content));
    void getOrganizationTree().then((nodes) =>
      setOrgTree(nodes.map((n) => ({ id: n.id, name: n.name, parentId: n.parentId }))),
    );
  }, [proj]);

  // v0.0.64 — Edit-mode: owner change → auto-fill team to owner's primary org
  useEffect(() => {
    if (!editing || eOwnerUserId === '') return;
    void listUserOrganizations({ userId: Number(eOwnerUserId), activeOnly: true, size: 100 }).then(
      (r) => {
        const primary = r.content.find((uo) => uo.isPrimary);
        if (primary) {
          setEOrganizationId(primary.organizationId);
        }
      },
    );
  }, [editing, eOwnerUserId]);

  // v0.0.64 — Edit-mode: team change → refresh PMO candidates + default to first
  useEffect(() => {
    if (!editing) return;
    if (eOrganizationId == null) {
      setPmoCandidates([]);
      return;
    }
    const reqId = ++lastPmoReqId.current;
    setPmoLoading(true);
    void listEffectivePmos(eOrganizationId)
      .then((res) => {
        if (reqId !== lastPmoReqId.current) return;
        setPmoCandidates(res);
      })
      .catch(() => {
        if (reqId === lastPmoReqId.current) setPmoCandidates([]);
      })
      .finally(() => {
        if (reqId === lastPmoReqId.current) setPmoLoading(false);
      });
  }, [editing, eOrganizationId]);

  const cancelEdit = () => {
    setEditing(false);
    setEError(null);
  };

  const saveEdit = async () => {
    if (proj == null) return;
    if (eOwnerUserId === '') {
      setEError('请选择负责人');
      return;
    }
    setEError(null);
    setESaving(true);
    try {
      const body: ProjectUpdate = {
        name: eName,
        description: eDescription,
        status: eStatus,
        projectType: eProjectType,
        ownerUserId: eOwnerUserId,
        organizationId: eOrganizationId,
        pmoUserId: ePmoUserId === '' ? null : ePmoUserId,
        startDate: eStartDate || undefined,
        endDate: eEndDate || undefined,
        enabled: eEnabled,
      };
      await updateProject(proj.id, body);
      setEditing(false);
      load();
    } catch (e) {
      const err = e as { response?: { data?: { message?: string } }; message?: string };
      setEError(err?.response?.data?.message ?? err?.message ?? '保存失败');
    } finally {
      setESaving(false);
    }
  };

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
        {proj.organizationName && (
          <StatusChip
            status="TEAM"
            label={`🏢 ${proj.organizationName}`}
            tier="gray"
            testId="project-detail-team-chip"
          />
        )}
        {proj.pmoName && (
          <StatusChip
            status="PMO"
            label={`👤PMO ${proj.pmoName}`}
            tier="green"
            testId="project-detail-pmo-chip"
          />
        )}
        <span className="pm-detail-hero-actions">
          {!editing ? (
            <Button
              type="button"
              variant="secondary"
              onClick={() => {
                setTab('info');
                startEdit();
              }}
              data-testid="project-detail-edit"
            >
              编辑
            </Button>
          ) : (
            <>
              <Button type="button" variant="secondary" onClick={cancelEdit} data-testid="project-detail-cancel">
                取消
              </Button>
              <Button
                type="button"
                disabled={eSaving || pmoLoading}
                onClick={() => void saveEdit()}
                data-testid="project-detail-save"
              >
                {eSaving ? '保存中…' : '保存'}
              </Button>
            </>
          )}
        </span>
      </div>

      <div className="pm-tabs" data-testid="project-detail-tabs">
        <button type="button" className="pm-tab" data-active={tab === 'info'} onClick={() => setTab('info')} data-testid="project-tab-info">
          基本信息
        </button>
        <button type="button" className="pm-tab" data-active={tab === 'members'} onClick={() => setTab('members')} data-testid="project-tab-members">
          成员<span className="pm-tab-count">· {memberCount}</span>
        </button>
        <button type="button" className="pm-tab" data-active={tab === 'milestones'} onClick={() => setTab('milestones')} data-testid="project-tab-milestones">
          里程碑
        </button>
        <button type="button" className="pm-tab" data-active={tab === 'implementation'} onClick={() => setTab('implementation')} data-testid="project-tab-implementation">
          施工内容
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

      {tab === 'info' && !editing && (
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

            <span className="pm-info-label">负责团队</span>
            <span className="pm-info-value">{proj.organizationName ?? '—'}</span>

            <span className="pm-info-label">项目 PMO</span>
            <span className="pm-info-value">
              {proj.pmoUserId ? (
                <OwnerChip name={proj.pmoName} loginName={proj.pmoLoginName} />
              ) : (
                '—'
              )}
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

      {tab === 'info' && editing && (
        <div className="pm-tab-pane" data-testid="project-detail-edit-form">
          <Input label="名称" value={eName} onChange={(e) => setEName(e.target.value)} />
          <Input
            label="描述"
            value={eDescription}
            onChange={(e) => setEDescription(e.target.value)}
          />
          <div className="rainier-form-group">
            <label className="rainier-form-label">状态</label>
            <select
              className="rainier-form-select"
              value={eStatus}
              onChange={(e) => setEStatus(e.target.value as ProjectStatus)}
            >
              {STATUS_OPTIONS.map((s) => (
                <option key={s} value={s}>
                  {PROJECT_STATUS_LABELS[s] ?? s}
                </option>
              ))}
            </select>
          </div>
          <div className="rainier-form-group">
            <label className="rainier-form-label">项目类型</label>
            <select
              className="rainier-form-select"
              value={eProjectType}
              onChange={(e) => setEProjectType(e.target.value as ProjectType)}
            >
              {PROJECT_TYPE_OPTIONS.map((t) => (
                <option key={t} value={t}>
                  {PROJECT_TYPE_LABELS[t]}
                </option>
              ))}
            </select>
          </div>
          <div className="rainier-form-group">
            <label className="rainier-form-label">负责人</label>
            <select
              className="rainier-form-select"
              value={eOwnerUserId}
              onChange={(e) => setEOwnerUserId(e.target.value === '' ? '' : Number(e.target.value))}
              data-testid="project-detail-edit-owner"
            >
              <option value="">请选择</option>
              {users.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.name}（{u.loginName}）
                </option>
              ))}
            </select>
          </div>
          <div className="rainier-form-group">
            <label className="rainier-form-label">负责团队（owner 变化时会自动取主组织，可改）</label>
            <TreeSelect
              value={eOrganizationId}
              nodes={orgTree}
              onChange={(id) => setEOrganizationId(id)}
              placeholder="选择团队"
            />
          </div>
          <div className="rainier-form-group">
            <label className="rainier-form-label">
              项目 PMO {pmoLoading ? '· 加载中…' : ''}
            </label>
            <select
              className="rainier-form-select"
              value={ePmoUserId}
              onChange={(e) => setEPmoUserId(e.target.value === '' ? '' : Number(e.target.value))}
              disabled={pmoLoading}
              data-testid="project-detail-edit-pmo"
            >
              <option value="">未指定</option>
              {pmoCandidates.map((c) => {
                const label =
                  c.inheritedFromOrgId === eOrganizationId
                    ? c.userName ?? c.userLoginName ?? `#${c.userId}`
                    : `${c.userName ?? c.userLoginName ?? '#' + c.userId}（继承自 ${c.inheritedFromOrgName ?? ''}）`;
                return (
                  <option key={c.userId} value={c.userId}>
                    {label}
                  </option>
                );
              })}
            </select>
          </div>
          <Input
            label="开始日期 (YYYY-MM-DD)"
            value={eStartDate}
            onChange={(e) => setEStartDate(e.target.value)}
          />
          <Input
            label="结束日期 (YYYY-MM-DD)"
            value={eEndDate}
            onChange={(e) => setEEndDate(e.target.value)}
          />
          <div className="rainier-form-group">
            <label style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
              <input
                type="checkbox"
                checked={eEnabled}
                onChange={(e) => setEEnabled(e.target.checked)}
              />
              启用
            </label>
          </div>
          {eError && (
            <div className="rainier-error-banner" data-testid="project-detail-edit-error">
              {eError}
            </div>
          )}
          <div className="rainier-form-footer">
            <Button type="button" variant="secondary" onClick={cancelEdit}>
              取消
            </Button>
            <Button
              type="button"
              disabled={eSaving || pmoLoading}
              onClick={() => void saveEdit()}
              data-testid="project-detail-edit-save"
            >
              {eSaving ? '保存中…' : '保存修改'}
            </Button>
          </div>
        </div>
      )}

      {tab === 'members' && (
        <div className="pm-tab-pane">
          <ProjectMemberTab project={proj} onCountChange={setMemberCount} />
        </div>
      )}

      {tab === 'milestones' && (
        <div className="pm-tab-pane">
          <MilestonesPanel projectId={proj.id} />
        </div>
      )}

      {tab === 'implementation' && (
        <div className="pm-tab-pane">
          <ProjectImplementationPanel projectId={proj.id} />
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

/**
 * v0.0.64 — ProjectDetailPage 「成员」Tab 内容。UNION 行（owner/pmo 合成 + 真实成员）+ 添加/移除按钮
 * 按 currentUser ∈ {owner, project.pmo, admin} 显示。owner/pmo 合成行不显示移除按钮。
 */
function ProjectMemberTab({
  project,
  onCountChange,
}: {
  project: Project;
  onCountChange: (n: number) => void;
}) {
  const currentUser = useAuthStore((s) => s.user);
  const admin = isElevated(currentUser);
  const canManage =
    admin ||
    (currentUser?.id != null &&
      (currentUser.id === project.ownerUserId || currentUser.id === project.pmoUserId));

  const [members, setMembers] = useState<ProjectMemberDetail[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [addOpen, setAddOpen] = useState(false);
  const [addingUserId, setAddingUserId] = useState<number | ''>('');
  const [addingRole, setAddingRole] = useState<ProjectMemberRole>('DEV');
  const [error, setError] = useState<string | null>(null);

  const refetch = useCallback(async () => {
    try {
      const list = await listProjectMembers(project.id);
      setMembers(list);
      onCountChange(list.length);
    } catch {
      setMembers([]);
    }
  }, [project.id, onCountChange]);

  useEffect(() => {
    void refetch();
    if (canManage) {
      void listUsers({ size: 100 }).then((r) => setUsers(r.content));
    }
  }, [refetch, canManage]);

  const synthRoles = new Set(['OWNER', 'PMO']);

  return (
    <div data-testid="project-member-tab">
      {canManage && (
        <div style={{ marginBottom: 12 }}>
          {!addOpen ? (
            <Button type="button" variant="secondary" onClick={() => setAddOpen(true)} data-testid="project-member-add-btn">
              + 添加成员
            </Button>
          ) : (
            <div
              style={{
                display: 'flex',
                gap: 8,
                alignItems: 'center',
                padding: 12,
                border: '1px solid var(--rainier-border)',
                borderRadius: 6,
              }}
            >
              <select
                className="rainier-form-select"
                value={addingUserId}
                onChange={(e) =>
                  setAddingUserId(e.target.value === '' ? '' : Number(e.target.value))
                }
                data-testid="project-member-add-user"
                style={{ flex: 1 }}
              >
                <option value="">选择用户</option>
                {users
                  .filter(
                    (u) =>
                      u.id !== project.ownerUserId &&
                      !members.some((m) => m.userId === u.id && !synthRoles.has(m.role)),
                  )
                  .map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.name}（{u.loginName}）
                    </option>
                  ))}
              </select>
              <select
                className="rainier-form-select"
                value={addingRole}
                onChange={(e) => setAddingRole(e.target.value as ProjectMemberRole)}
                data-testid="project-member-add-role"
                style={{ width: 120 }}
              >
                {PROJECT_MEMBER_ROLE_OPTIONS.map((r) => (
                  <option key={r} value={r}>
                    {PROJECT_MEMBER_ROLE_LABELS[r]}
                  </option>
                ))}
              </select>
              <Button
                type="button"
                onClick={async () => {
                  if (addingUserId === '') return;
                  setError(null);
                  try {
                    await addProjectMember(project.id, addingUserId, addingRole);
                    setAddOpen(false);
                    setAddingUserId('');
                    setAddingRole('DEV');
                    await refetch();
                  } catch (e) {
                    const err = e as { response?: { data?: { message?: string } } };
                    setError(err?.response?.data?.message ?? '添加失败');
                  }
                }}
                data-testid="project-member-add-confirm"
              >
                确认
              </Button>
              <Button type="button" variant="secondary" onClick={() => setAddOpen(false)}>
                取消
              </Button>
            </div>
          )}
        </div>
      )}
      {error && (
        <div className="rainier-error-banner" data-testid="project-member-error">
          {error}
        </div>
      )}
      {members.length === 0 ? (
        <div style={{ color: 'var(--rainier-color-text-3)' }}>暂无成员</div>
      ) : (
        members.map((m, i) => {
          const isSynth = synthRoles.has(m.role);
          return (
            <div
              key={`${m.userId}-${m.role}-${i}`}
              data-testid={`project-member-row-${m.userId}`}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                padding: '10px 4px',
                borderBottom: '1px solid var(--rainier-bg-hover)',
              }}
            >
              <OwnerChip name={m.userName} loginName={m.userLoginName} />
              <StatusChip
                status={m.role}
                label={m.displayLabel}
                tier={m.role === 'OWNER' ? 'green' : m.role === 'PMO' ? 'yellow' : 'gray'}
              />
              <span style={{ flex: 1 }} />
              {m.joinedBy && (
                <span style={{ fontSize: 12, color: 'var(--rainier-color-text-3)' }}>
                  添加人 {m.joinedBy}
                </span>
              )}
              {canManage && !isSynth && (
                <>
                  <select
                    className="rainier-form-select"
                    value={m.role}
                    onChange={async (e) => {
                      const newRole = e.target.value as ProjectMemberRole;
                      setError(null);
                      try {
                        await updateProjectMemberRole(project.id, m.userId, newRole);
                        await refetch();
                      } catch (err) {
                        const ex = err as { response?: { data?: { message?: string } } };
                        setError(ex?.response?.data?.message ?? '更新失败');
                      }
                    }}
                    data-testid={`project-member-role-${m.userId}`}
                    style={{ width: 100 }}
                  >
                    {PROJECT_MEMBER_ROLE_OPTIONS.map((r) => (
                      <option key={r} value={r}>
                        {PROJECT_MEMBER_ROLE_LABELS[r]}
                      </option>
                    ))}
                  </select>
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={async () => {
                      setError(null);
                      try {
                        await removeProjectMember(project.id, m.userId);
                        await refetch();
                      } catch (err) {
                        const ex = err as { response?: { data?: { message?: string } } };
                        setError(ex?.response?.data?.message ?? '移除失败');
                      }
                    }}
                    data-testid={`project-member-remove-${m.userId}`}
                  >
                    移除
                  </Button>
                </>
              )}
            </div>
          );
        })
      )}
    </div>
  );
}

/**
 * v0.0.89 — 施工内容表单 Tab。简单 markdown 文本域 + estimated/risk/milestones JSON。upsert 幂等。
 */
function ProjectImplementationPanel({ projectId }: { projectId: number }) {
  const [loaded, setLoaded] = useState(false);
  const [scope, setScope] = useState('');
  const [manDays, setManDays] = useState<string>('');
  const [risk, setRisk] = useState('');
  const [milestonesJson, setMilestonesJson] = useState('');
  const [saving, setSaving] = useState(false);
  const [savedAt, setSavedAt] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoaded(false);
    setError(null);
    void getProjectImplementation(projectId)
      .then((p: PIType | null) => {
        if (cancelled) return;
        if (p) {
          setScope(p.scopeMarkdown);
          setManDays(p.estimatedManDays == null ? '' : String(p.estimatedManDays));
          setRisk(p.riskNotes ?? '');
          setMilestonesJson(p.keyMilestonesJson ?? '');
          setSavedAt(p.updateTime ?? null);
        } else {
          setScope('');
          setManDays('');
          setRisk('');
          setMilestonesJson('');
          setSavedAt(null);
        }
      })
      .catch(() => setError('加载施工内容失败'))
      .finally(() => {
        if (!cancelled) setLoaded(true);
      });
    return () => {
      cancelled = true;
    };
  }, [projectId]);

  const onSave = async () => {
    if (!scope.trim()) {
      setError('施工范围不能为空');
      return;
    }
    setError(null);
    setSaving(true);
    try {
      const md = manDays.trim() === '' ? null : Number(manDays);
      const result = await upsertProjectImplementation(projectId, {
        scopeMarkdown: scope,
        estimatedManDays: md,
        riskNotes: risk || null,
        keyMilestonesJson: milestonesJson || null,
      });
      setSavedAt(result.updateTime ?? new Date().toISOString());
    } catch (e) {
      const err = e as { response?: { data?: { message?: string } }; message?: string };
      setError(err?.response?.data?.message ?? err?.message ?? '保存失败');
    } finally {
      setSaving(false);
    }
  };

  if (!loaded) return <div>加载中…</div>;

  return (
    <div data-testid="project-implementation-panel">
      <div className="rainier-form-group">
        <label className="rainier-form-label">施工范围（Markdown）</label>
        <textarea
          className="rainier-form-input"
          value={scope}
          onChange={(e) => setScope(e.target.value)}
          rows={10}
          style={{ width: '100%', fontFamily: 'monospace' }}
          data-testid="project-implementation-scope"
        />
      </div>
      <Input
        label="估算工时（人日）"
        value={manDays}
        onChange={(e) => setManDays(e.target.value.replace(/[^0-9]/g, ''))}
        data-testid="project-implementation-mandays"
      />
      <Input
        label="风险备注"
        value={risk}
        onChange={(e) => setRisk(e.target.value)}
        data-testid="project-implementation-risk"
      />
      <div className="rainier-form-group">
        <label className="rainier-form-label">关键里程碑（JSON 数组，可选）</label>
        <textarea
          className="rainier-form-input"
          value={milestonesJson}
          onChange={(e) => setMilestonesJson(e.target.value)}
          rows={4}
          placeholder='[{"name":"M1","date":"2026-07-15"}]'
          style={{ width: '100%', fontFamily: 'monospace' }}
          data-testid="project-implementation-milestones"
        />
      </div>
      {error && (
        <div className="rainier-error-banner" data-testid="project-implementation-error">
          {error}
        </div>
      )}
      <div className="rainier-form-footer">
        {savedAt && (
          <span style={{ color: 'var(--rainier-color-text-3)', fontSize: 12, marginRight: 12 }}>
            上次保存 {formatDateTime(savedAt)}
          </span>
        )}
        <Button
          type="button"
          disabled={saving}
          onClick={() => void onSave()}
          data-testid="project-implementation-save"
        >
          {saving ? '保存中…' : '保存'}
        </Button>
      </div>
    </div>
  );
}
