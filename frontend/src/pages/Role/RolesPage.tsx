import { useCallback, useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import { createRole, deleteRole, listRoles, updateRole, type Role } from '../../api/role';
import { usePaginated } from '../../hooks/usePaginated';

export function RolesPage() {
  const fetcher = useCallback(
    async ({ page, size, search }: { page: number; size: number; search: string }) =>
      listRoles({ page, size, search: search || undefined }),
    [],
  );
  const list = usePaginated<Role>(fetcher, { initialSize: 20 });

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editing, setEditing] = useState<Role | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<Role | null>(null);

  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [enabled, setEnabled] = useState(true);

  useEffect(() => {
    if (!drawerOpen) return;
    if (editing) {
      setCode(editing.code);
      setName(editing.name);
      setDescription(editing.description ?? '');
      setEnabled(editing.enabled);
    } else {
      setCode('');
      setName('');
      setDescription('');
      setEnabled(true);
    }
  }, [drawerOpen, editing]);

  const columns: TableColumn<Role>[] = [
    { key: 'code', title: '编码', render: (r) => r.code },
    { key: 'name', title: '名称', render: (r) => r.name },
    { key: 'description', title: '描述', render: (r) => r.description ?? '—' },
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
          data-testid="roles-new-btn"
        >
          新建角色
        </Button>
      </div>
      <Table<Role> columns={columns} dataSource={list.items} rowKey="id" />
      <Pagination
        page={list.page}
        size={list.size}
        total={list.total}
        onPageChange={list.setPage}
      />
      <Drawer
        open={drawerOpen}
        title={editing ? '编辑角色' : '新建角色'}
        onClose={() => setDrawerOpen(false)}
      >
        <Input
          label="编码 (e.g. PMO)"
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
              if (editing) {
                await updateRole(editing.id, {
                  name,
                  description: description || undefined,
                  enabled,
                });
              } else {
                await createRole({
                  code,
                  name,
                  description: description || undefined,
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
        title="删除角色"
        message={`确认删除角色「${confirmDelete?.name ?? ''}」？已分配给用户时会被拒绝。`}
        onCancel={() => setConfirmDelete(null)}
        onConfirm={async () => {
          if (confirmDelete) await deleteRole(confirmDelete.id);
          setConfirmDelete(null);
          void list.refetch();
        }}
      />
    </Card>
  );
}
