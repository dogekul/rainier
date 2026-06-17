import { useCallback, useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Drawer } from '../../components/ui/Drawer';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import { listProjects, type Project } from '../../api/project';
import { listRoles, type Role } from '../../api/role';
import { listUsers, type User } from '../../api/user';
import {
  createUserRole,
  deleteUserRole,
  listUserRoles,
  type UserRoleLink,
} from '../../api/userRole';
import { usePaginated } from '../../hooks/usePaginated';

export function UserRolesPage() {
  const fetcher = useCallback(
    async ({ page, size }: { page: number; size: number; search: string }) =>
      listUserRoles({ page, size }),
    [],
  );
  const list = usePaginated<UserRoleLink>(fetcher, { initialSize: 20 });

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<UserRoleLink | null>(null);

  const [users, setUsers] = useState<User[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [userId, setUserId] = useState<number | ''>('');
  const [roleId, setRoleId] = useState<number | ''>('');
  const [projectId, setProjectId] = useState<number | ''>('');

  useEffect(() => {
    if (!drawerOpen) return;
    // PageParams 校验 size <= 100；v0 池大小足够。
    void listUsers({ size: 100 }).then((r) => setUsers(r.content));
    void listRoles({ size: 100 }).then((r) => setRoles(r.content));
    void listProjects({ size: 100 }).then((r) => setProjects(r.content));
    setUserId('');
    setRoleId('');
    setProjectId('');
  }, [drawerOpen]);

  const columns: TableColumn<UserRoleLink>[] = [
    {
      key: 'user',
      title: '用户',
      render: (r) => `${r.userName ?? ''}（${r.userLoginName ?? ''}）`,
    },
    {
      key: 'role',
      title: '角色',
      render: (r) => `${r.roleName ?? ''}（${r.roleCode ?? ''}）`,
    },
    {
      key: 'project',
      title: '项目',
      render: (r) =>
        r.projectName
          ? `${r.projectName}（${r.projectCode ?? ''}）`
          : r.projectId
            ? `#${r.projectId}`
            : '（公司级）',
    },
    {
      key: 'actions',
      title: '操作',
      render: (r) => (
        <Button type="button" variant="secondary" onClick={() => setConfirmDelete(r)}>
          删除
        </Button>
      ),
    },
  ];

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2 style={{ margin: 0 }}>用户角色</h2>
        <div style={{ flex: 1 }} />
        <Button
          type="button"
          onClick={() => setDrawerOpen(true)}
          data-testid="user-roles-new-btn"
        >
          新建用户角色
        </Button>
      </div>
      <Card>
        <Table<UserRoleLink> columns={columns} dataSource={list.items} rowKey="id" />
        <Pagination
          page={list.page}
          size={list.size}
          total={list.total}
          onPageChange={list.setPage}
        />
      </Card>
      <Drawer open={drawerOpen} title="新建用户角色" onClose={() => setDrawerOpen(false)}>
        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>用户</label>
          <select
            className="rainier-treeselect-trigger"
            value={userId}
            onChange={(e) => setUserId(e.target.value === '' ? '' : Number(e.target.value))}
            data-testid="user-roles-user-select"
          >
            <option value="">请选择</option>
            {users.map((u) => (
              <option key={u.id} value={u.id}>
                {u.name}（{u.loginName}）
              </option>
            ))}
          </select>
        </div>
        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>角色</label>
          <select
            className="rainier-treeselect-trigger"
            value={roleId}
            onChange={(e) => setRoleId(e.target.value === '' ? '' : Number(e.target.value))}
            data-testid="user-roles-role-select"
          >
            <option value="">请选择</option>
            {roles.map((r) => (
              <option key={r.id} value={r.id}>
                {r.name}（{r.code}）
              </option>
            ))}
          </select>
        </div>
        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
            项目（留白 = 公司级 hat）
          </label>
          <select
            className="rainier-treeselect-trigger"
            value={projectId}
            onChange={(e) => setProjectId(e.target.value === '' ? '' : Number(e.target.value))}
            data-testid="user-roles-project-select"
          >
            <option value="">（公司级 / 留白）</option>
            {projects.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}（{p.code}）
              </option>
            ))}
          </select>
        </div>
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <Button type="button" variant="secondary" onClick={() => setDrawerOpen(false)}>
            取消
          </Button>
          <Button
            type="button"
            onClick={async () => {
              if (!userId || !roleId) return;
              const pid = projectId === '' ? null : projectId;
              await createUserRole({ userId, roleId, projectId: pid });
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
        title="删除用户角色"
        message={`确认删除关联（${confirmDelete?.userName ?? ''} → ${confirmDelete?.roleName ?? ''}）？硬删，不可撤销。`}
        onCancel={() => setConfirmDelete(null)}
        onConfirm={async () => {
          if (confirmDelete) await deleteUserRole(confirmDelete.id);
          setConfirmDelete(null);
          void list.refetch();
        }}
      />
    </div>
  );
}
