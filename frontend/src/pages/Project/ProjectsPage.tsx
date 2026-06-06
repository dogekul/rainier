import { useCallback, useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import {
  createProject,
  deleteProject,
  listProjects,
  updateProject,
  type Project,
  type ProjectStatus,
} from '../../api/project';
import { listUsers, type User } from '../../api/user';
import { useAuthStore } from '../../store/auth';
import { usePaginated } from '../../hooks/usePaginated';

const STATUS_OPTIONS: ProjectStatus[] = [
  'PLANNING',
  'ACTIVE',
  'ON_HOLD',
  'DELIVERED',
  'ARCHIVED',
];

export function ProjectsPage() {
  const fetcher = useCallback(
    async ({ page, size, search }: { page: number; size: number; search: string }) =>
      listProjects({ page, size, search: search || undefined }),
    [],
  );
  const list = usePaginated<Project>(fetcher, { initialSize: 20 });
  const currentLoginName = useAuthStore((s) => s.user?.username ?? null);

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editing, setEditing] = useState<Project | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<Project | null>(null);

  const [users, setUsers] = useState<User[]>([]);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [status, setStatus] = useState<ProjectStatus>('PLANNING');
  const [ownerUserId, setOwnerUserId] = useState<number | ''>('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [enabled, setEnabled] = useState(true);

  useEffect(() => {
    if (!drawerOpen) return;
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
      setCode(editing.code);
      setName(editing.name);
      setDescription(editing.description ?? '');
      setStatus(editing.status);
      setStartDate(editing.startDate ?? '');
      setEndDate(editing.endDate ?? '');
      setEnabled(editing.enabled);
    } else {
      setCode('');
      setName('');
      setDescription('');
      setStatus('PLANNING');
      setStartDate('');
      setEndDate('');
      setEnabled(true);
    }
  }, [drawerOpen, editing, currentLoginName]);

  const columns: TableColumn<Project>[] = [
    { key: 'code', title: '编码', render: (r) => r.code },
    { key: 'name', title: '名称', render: (r) => r.name },
    { key: 'status', title: '状态', render: (r) => r.status },
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
          </Button>
        </>
      ),
    },
  ];

  return (
    <Card>
      <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
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
      <Table<Project> columns={columns} dataSource={list.items} rowKey="id" />
      <Pagination
        page={list.page}
        size={list.size}
        total={list.total}
        onPageChange={list.setPage}
      />
      <Drawer
        open={drawerOpen}
        title={editing ? '编辑项目' : '新建项目'}
        onClose={() => setDrawerOpen(false)}
      >
        <Input
          label="编码 (e.g. PROJ-001)"
          value={code}
          onChange={(e) => setCode(e.target.value)}
          disabled={editing !== null}
        />
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
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
            负责人（默认为当前登录用户，可改）
          </label>
          <select
            className="rainier-treeselect-trigger"
            value={ownerUserId}
            onChange={(e) =>
              setOwnerUserId(e.target.value === '' ? '' : Number(e.target.value))
            }
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
              if (!ownerUserId) return;
              if (editing) {
                await updateProject(editing.id, {
                  name,
                  description: description || undefined,
                  status,
                  ownerUserId,
                  startDate: startDate || undefined,
                  endDate: endDate || undefined,
                  enabled,
                });
              } else {
                await createProject({
                  code,
                  name,
                  description: description || undefined,
                  status,
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
    </Card>
  );
}
