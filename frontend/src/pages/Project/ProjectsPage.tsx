import { useCallback, useEffect, useRef, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import { StatusChip } from '../../components/board';
import {
  createProject,
  deleteProject,
  listProjects,
  PROJECT_TYPE_LABELS,
  PROJECT_TYPE_OPTIONS,
  updateProject,
  type Project,
  type ProjectStatus,
  type ProjectType,
} from '../../api/project';
import { listUsers, type User } from '../../api/user';
import { useAuthStore } from '../../store/auth';
import { usePaginated } from '../../hooks/usePaginated';
import { MilestonesPanel } from './MilestonesPanel';

const STATUS_OPTIONS: ProjectStatus[] = [
  'PLANNING',
  'ACTIVE',
  'ON_HOLD',
  'DELIVERED',
  'ARCHIVED',
];

export function ProjectsPage() {
  // v0.0.16 — type filter is folded into the fetcher closure; usePaginated only auto-refetches on
  // page/size/search, so a dedicated effect re-queries when the filter changes.
  const [typeFilter, setTypeFilter] = useState<ProjectType | ''>('');
  const fetcher = useCallback(
    async ({ page, size, search }: { page: number; size: number; search: string }) =>
      listProjects({
        page,
        size,
        search: search || undefined,
        projectType: typeFilter || undefined,
      }),
    [typeFilter],
  );
  const list = usePaginated<Project>(fetcher, { initialSize: 20 });
  const typeFilterMounted = useRef(false);
  useEffect(() => {
    if (!typeFilterMounted.current) {
      typeFilterMounted.current = true;
      return;
    }
    void list.refetch();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [typeFilter]);
  const currentLoginName = useAuthStore((s) => s.user?.username ?? null);

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editing, setEditing] = useState<Project | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<Project | null>(null);
  // v0.0.17: per-row inline 里程碑 panel toggle (SprintsPage expand pattern).
  const [milestonesExpanded, setMilestonesExpanded] = useState<Set<number>>(new Set());
  const toggleMilestones = (id: number) => {
    setMilestonesExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const [users, setUsers] = useState<User[]>([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [status, setStatus] = useState<ProjectStatus>('PLANNING');
  const [projectType, setProjectType] = useState<ProjectType>('CASUAL');
  const [ownerUserId, setOwnerUserId] = useState<number | ''>('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [enabled, setEnabled] = useState(true);
  // v0.0.8.1 Code-M7: surface validation errors so the save button isn't a silent no-op.
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (!drawerOpen) {
      setFormError(null);
      return;
    }
    // PageParams 校验 size <= 100；v0 池大小足够。
    void listUsers({ size: 100 }).then((r) => {
      setUsers(r.content);
      if (editing) {
        setOwnerUserId(editing.ownerUserId);
      } else {
        // Default to currently logged-in user (match by loginName); editable.
        const me = currentLoginName
          ? r.content.find((u) => u.loginName === currentLoginName)
          : undefined;
        setOwnerUserId(me ? me.id : '');
      }
    });
    if (editing) {
      setName(editing.name);
      setDescription(editing.description ?? '');
      setStatus(editing.status);
      setProjectType(editing.projectType);
      setStartDate(editing.startDate ?? '');
      setEndDate(editing.endDate ?? '');
      setEnabled(editing.enabled);
    } else {
      setName('');
      setDescription('');
      setStatus('PLANNING');
      setProjectType('CASUAL');
      setStartDate('');
      setEndDate('');
      setEnabled(true);
    }
  }, [drawerOpen, editing, currentLoginName]);

  const columns: TableColumn<Project>[] = [
    { key: 'code', title: '编码', render: (r) => r.code },
    { key: 'name', title: '名称', render: (r) => r.name },
    { key: 'status', title: '状态', render: (r) => <StatusChip status={r.status} /> },
    {
      key: 'projectType',
      title: '类型',
      render: (r) => PROJECT_TYPE_LABELS[r.projectType] ?? r.projectType,
    },
    {
      key: 'owner',
      title: '负责人',
      render: (r) => `${r.ownerName ?? ''}（${r.ownerLoginName ?? ''}）`,
    },
    { key: 'startDate', title: '开始', render: (r) => r.startDate ?? '—' },
    { key: 'endDate', title: '结束', render: (r) => r.endDate ?? '—' },
    { key: 'enabled', title: '启用', render: (r) => (r.enabled ? '是' : '否') },
    {
      key: 'actions',
      title: '操作',
      render: (r) => (
        <>
          <Button
            type="button"
            variant="secondary"
            onClick={() => {
              setEditing(r);
              setDrawerOpen(true);
            }}
          >
            编辑
          </Button>{' '}
          <Button type="button" variant="secondary" onClick={() => setConfirmDelete(r)}>
            删除
          </Button>{' '}
          <Button
            type="button"
            variant="secondary"
            onClick={() => toggleMilestones(r.id)}
            data-testid={`projects-milestones-btn-${r.id}`}
          >
            里程碑
          </Button>
        </>
      ),
    },
  ];

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2 style={{ margin: 0 }}>项目</h2>
        <div style={{ flex: 1 }} />
        <select
          className="rainier-select"
          value={typeFilter}
          onChange={(e) => setTypeFilter(e.target.value as ProjectType | '')}
          data-testid="projects-type-filter"
          aria-label="类型过滤"
        >
          <option value="">全部类型</option>
          {PROJECT_TYPE_OPTIONS.map((t) => (
            <option key={t} value={t}>
              {PROJECT_TYPE_LABELS[t]}
            </option>
          ))}
        </select>
        <Button
          type="button"
          onClick={() => {
            setEditing(null);
            setDrawerOpen(true);
          }}
          data-testid="projects-new-btn"
        >
          新建项目
        </Button>
      </div>
      <Card>
        <Table<Project>
          columns={columns}
          dataSource={list.items}
          rowKey="id"
          isExpanded={(r) => milestonesExpanded.has(r.id)}
          renderExpanded={(r) => <MilestonesPanel projectId={r.id} />}
        />
        <Pagination
          page={list.page}
          size={list.size}
          total={list.total}
          onPageChange={list.setPage}
        />
      </Card>
      <Drawer
        open={drawerOpen}
        title={editing ? '编辑项目' : '新建项目'}
        onClose={() => setDrawerOpen(false)}
      >
        <Input label="名称" value={name} onChange={(e) => setName(e.target.value)} />
        <Input
          label="描述"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>状态</label>
          <select
            className="rainier-treeselect-trigger"
            value={status}
            onChange={(e) => setStatus(e.target.value as ProjectStatus)}
            data-testid="projects-status-select"
          >
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>项目类型</label>
          <select
            className="rainier-treeselect-trigger"
            value={projectType}
            onChange={(e) => setProjectType(e.target.value as ProjectType)}
            data-testid="projects-type-select"
          >
            {PROJECT_TYPE_OPTIONS.map((t) => (
              <option key={t} value={t}>
                {PROJECT_TYPE_LABELS[t]}
              </option>
            ))}
          </select>
        </div>
        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
            负责人（默认为当前登录用户，可改）
          </label>
          <select
            className="rainier-treeselect-trigger"
            value={ownerUserId}
            onChange={(e) => {
              setOwnerUserId(e.target.value === '' ? '' : Number(e.target.value));
              setFormError(null);
            }}
            data-testid="projects-owner-select"
          >
            <option value="">请选择</option>
            {users.map((u) => (
              <option key={u.id} value={u.id}>
                {u.name}（{u.loginName}）
              </option>
            ))}
          </select>
        </div>
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
            data-testid="projects-form-error"
          >
            {formError}
          </div>
        )}
        <Input
          label="开始日期 (YYYY-MM-DD)"
          value={startDate}
          onChange={(e) => setStartDate(e.target.value)}
        />
        <Input
          label="结束日期 (YYYY-MM-DD)"
          value={endDate}
          onChange={(e) => setEndDate(e.target.value)}
        />
        <div style={{ marginBottom: 12 }}>
          <label style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
            <input
              type="checkbox"
              checked={enabled}
              onChange={(e) => setEnabled(e.target.checked)}
            />
            启用
          </label>
        </div>
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <Button type="button" variant="secondary" onClick={() => setDrawerOpen(false)}>
            取消
          </Button>
          <Button
            type="button"
            onClick={async () => {
              // v0.0.8.1 Code-M7: explicit validation instead of silent no-op.
              if (ownerUserId === '' || ownerUserId === null) {
                setFormError('请选择负责人');
                return;
              }
              setFormError(null);
              if (editing) {
                await updateProject(editing.id, {
                  name,
                  // v0.0.8.1: always send description as a string (incl. "") so an emptied field
                  // round-trips to the backend as a clear instead of being preserved.
                  description,
                  status,
                  projectType,
                  ownerUserId,
                  startDate: startDate || undefined,
                  endDate: endDate || undefined,
                  enabled,
                });
              } else {
                await createProject({
                  name,
                  description: description || undefined,
                  status,
                  projectType,
                  ownerUserId,
                  startDate: startDate || undefined,
                  endDate: endDate || undefined,
                  enabled,
                });
              }
              setDrawerOpen(false);
              void list.refetch();
            }}
          >
            保存
          </Button>
        </div>
      </Drawer>
      <ConfirmDialog
        open={confirmDelete !== null}
        title="删除项目"
        message={`确认删除项目「${confirmDelete?.name ?? ''}」？有关联需求/用户角色时会被拒绝。`}
        onCancel={() => setConfirmDelete(null)}
        onConfirm={async () => {
          if (confirmDelete) await deleteProject(confirmDelete.id);
          setConfirmDelete(null);
          void list.refetch();
        }}
      />
    </div>
  );
}
