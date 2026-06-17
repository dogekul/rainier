import { useCallback, useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import { TreeSelect, type TreeNode } from '../../components/ui/TreeSelect';
import { StatusChip } from '../../components/board';
import { getOrganizationTree } from '../../api/organization';
import { listUsers, type User } from '../../api/user';
import {
  createUserOrganization,
  deleteUserOrganization,
  listUserOrganizations,
  type UserOrgRole,
  type UserOrganization,
  updateUserOrganization,
} from '../../api/userOrganization';
import { usePaginated } from '../../hooks/usePaginated';

const roles: UserOrgRole[] = ['MEMBER', 'HEAD'];

export function UserOrganizationsPage() {
  const fetcher = useCallback(
    async ({ page, size }: { page: number; size: number; search: string }) =>
      listUserOrganizations({ page, size }),
    [],
  );
  const list = usePaginated<UserOrganization>(fetcher, { initialSize: 20 });

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editing, setEditing] = useState<UserOrganization | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<UserOrganization | null>(null);

  const [userOptions, setUserOptions] = useState<User[]>([]);
  const [orgTree, setOrgTree] = useState<TreeNode[]>([]);
  const [userId, setUserId] = useState<number | ''>('');
  const [organizationId, setOrganizationId] = useState<number | null>(null);
  const [role, setRole] = useState<UserOrgRole>('MEMBER');
  const [isPrimary, setIsPrimary] = useState(false);
  const [leftAt, setLeftAt] = useState('');

  useEffect(() => {
    if (!drawerOpen) return;
    void listUsers({ size: 100 }).then((r) => setUserOptions(r.content));
    void getOrganizationTree().then((nodes) =>
      setOrgTree(nodes.map((n) => ({ id: n.id, name: n.name, parentId: n.parentId }))),
    );
    if (editing) {
      setUserId(editing.userId);
      setOrganizationId(editing.organizationId);
      setRole(editing.role);
      setIsPrimary(editing.isPrimary);
      setLeftAt(editing.leftAt ?? '');
    } else {
      setUserId('');
      setOrganizationId(null);
      setRole('MEMBER');
      setIsPrimary(false);
      setLeftAt('');
    }
  }, [drawerOpen, editing]);

  const columns: TableColumn<UserOrganization>[] = [
    {
      key: 'user',
      title: '用户',
      render: (r) => `${r.userName ?? ''}（${r.userLoginName ?? ''}）`,
    },
    {
      key: 'organization',
      title: '组织',
      render: (r) => `${r.organizationName ?? ''}（${r.organizationType ?? ''}）`,
    },
    { key: 'role', title: '角色', render: (r) => r.role },
    { key: 'isPrimary', title: '主属', render: (r) => (r.isPrimary ? '是' : '—') },
    {
      key: 'status',
      title: '状态',
      render: (r) => <StatusChip status={r.leftAt ? '已离职' : '在岗'} />,
    },
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
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2 style={{ margin: 0 }}>用户-组织关系</h2>
        <div style={{ flex: 1 }} />
        <Button
          type="button"
          onClick={() => {
            setEditing(null);
            setDrawerOpen(true);
          }}
          data-testid="uo-new-btn"
        >
          新建归属
        </Button>
      </div>
      <Card>
        <Table<UserOrganization> columns={columns} dataSource={list.items} rowKey="id" />
        <Pagination
          page={list.page}
          size={list.size}
          total={list.total}
          onPageChange={list.setPage}
        />
      </Card>
      <Drawer
        open={drawerOpen}
        title={editing ? '编辑归属' : '新建归属'}
        onClose={() => setDrawerOpen(false)}
      >
        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>用户</label>
          <select
            className="rainier-treeselect-trigger"
            value={userId}
            onChange={(e) => setUserId(e.target.value === '' ? '' : Number(e.target.value))}
            disabled={editing !== null}
            data-testid="uo-user-select"
          >
            <option value="">请选择</option>
            {userOptions.map((u) => (
              <option key={u.id} value={u.id}>
                {u.name}（{u.loginName}）
              </option>
            ))}
          </select>
        </div>
        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>组织</label>
          <TreeSelect
            value={organizationId}
            nodes={orgTree}
            onChange={setOrganizationId}
            allowClear={false}
          />
        </div>
        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>角色</label>
          <select
            className="rainier-treeselect-trigger"
            value={role}
            onChange={(e) => setRole(e.target.value as UserOrgRole)}
          >
            {roles.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>
        </div>
        <div style={{ marginBottom: 8 }}>
          <label style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
            <input
              type="checkbox"
              checked={isPrimary}
              onChange={(e) => setIsPrimary(e.target.checked)}
              data-testid="uo-isprimary-checkbox"
            />
            主属节点
          </label>
        </div>
        {isPrimary && !editing?.isPrimary && (
          <p
            style={{
              fontSize: 12,
              color: 'var(--rainier-color-text-2)',
              marginTop: 0,
              marginBottom: 12,
            }}
            data-testid="uo-isprimary-hint"
          >
            提示：保存后将自动 demote 该用户当前其他主属归属。
          </p>
        )}
        <Input
          label="离职时间（ISO 8601 UTC，空 = 在岗）"
          value={leftAt}
          onChange={(e) => setLeftAt(e.target.value)}
        />
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <Button type="button" variant="secondary" onClick={() => setDrawerOpen(false)}>
            取消
          </Button>
          <Button
            type="button"
            onClick={async () => {
              if (editing) {
                await updateUserOrganization(editing.id, {
                  organizationId: organizationId ?? undefined,
                  role,
                  isPrimary,
                  leftAt: leftAt || null,
                });
              } else {
                if (!userId || !organizationId) return;
                await createUserOrganization({
                  userId,
                  organizationId,
                  role,
                  isPrimary,
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
        title="删除归属"
        message={`确认删除该归属（${confirmDelete?.userName ?? ''} → ${confirmDelete?.organizationName ?? ''}）？`}
        onCancel={() => setConfirmDelete(null)}
        onConfirm={async () => {
          if (confirmDelete) await deleteUserOrganization(confirmDelete.id);
          setConfirmDelete(null);
          void list.refetch();
        }}
      />
    </div>
  );
}
