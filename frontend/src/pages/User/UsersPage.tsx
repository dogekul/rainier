import { useCallback, useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import { createUser, deleteUser, listUsers, updateUser, type User } from '../../api/user';
import { listPositions, type Position } from '../../api/position';
import { usePaginated } from '../../hooks/usePaginated';

export function UsersPage() {
  const fetcher = useCallback(
    async ({ page, size, search }: { page: number; size: number; search: string }) =>
      listUsers({ page, size, search: search || undefined }),
    [],
  );
  const list = usePaginated<User>(fetcher, { initialSize: 20 });
  const [searchInput, setSearchInput] = useState('');
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editing, setEditing] = useState<User | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<User | null>(null);

  const [loginName, setLoginName] = useState('');
  const [name, setName] = useState('');
  const [code, setCode] = useState('');
  const [emailAddress, setEmailAddress] = useState('');
  const [isInternal, setIsInternal] = useState(true);
  const [enabled, setEnabled] = useState(true);
  const [positionId, setPositionId] = useState<number | ''>('');
  const [positionOptions, setPositionOptions] = useState<Position[]>([]);

  useEffect(() => {
    setSearchInput(list.search);
  }, [list.search]);

  useEffect(() => {
    if (!drawerOpen) return;
    // PageParams 校验 size <= 100；v0 池大小足够。
    void listPositions({ size: 100, enabled: true }).then((r) => setPositionOptions(r.content));
    if (editing) {
      setLoginName(editing.loginName);
      setName(editing.name);
      setCode(editing.code ?? '');
      setEmailAddress(editing.emailAddress ?? '');
      setIsInternal(editing.isInternal);
      setEnabled(editing.enabled);
      setPositionId(editing.positionId ?? '');
    } else {
      setLoginName('');
      setName('');
      setCode('');
      setEmailAddress('');
      setIsInternal(true);
      setEnabled(true);
      setPositionId('');
    }
  }, [drawerOpen, editing]);

  const columns: TableColumn<User>[] = [
    { key: 'loginName', title: '登录账号', render: (r) => r.loginName },
    { key: 'name', title: '姓名', render: (r) => r.name },
    { key: 'code', title: '工号', render: (r) => r.code ?? '—' },
    {
      key: 'position',
      title: '岗位',
      render: (r) =>
        r.positionName ? `${r.positionName} (${r.positionCategory ?? ''})` : '—',
    },
    { key: 'emailAddress', title: '邮箱', render: (r) => r.emailAddress ?? '—' },
    { key: 'isInternal', title: '内部', render: (r) => (r.isInternal ? '是' : '否') },
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
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2 style={{ margin: 0 }}>用户</h2>
        <div style={{ flex: 1 }} />
        <Input
          label="搜索"
          placeholder="按 login_name / name / code / email"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') list.setSearch(searchInput);
          }}
        />
        <Button type="button" onClick={() => list.setSearch(searchInput)}>
          查询
        </Button>
        <Button
          type="button"
          onClick={() => {
            setEditing(null);
            setDrawerOpen(true);
          }}
          data-testid="users-new-btn"
        >
          新建
        </Button>
      </div>
      <Card>
        <Table<User> columns={columns} dataSource={list.items} rowKey="id" />
        <Pagination
          page={list.page}
          size={list.size}
          total={list.total}
          onPageChange={list.setPage}
        />
      </Card>
      <Drawer open={drawerOpen} title={editing ? '编辑用户' : '新建用户'} onClose={() => setDrawerOpen(false)}>
        <Input
          label="登录账号"
          value={loginName}
          onChange={(e) => setLoginName(e.target.value)}
          disabled={editing !== null}
        />
        <Input label="姓名" value={name} onChange={(e) => setName(e.target.value)} />
        <Input label="工号" value={code} onChange={(e) => setCode(e.target.value)} />
        <Input
          label="邮箱"
          type="email"
          value={emailAddress}
          onChange={(e) => setEmailAddress(e.target.value)}
        />
        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>岗位</label>
          <select
            className="rainier-treeselect-trigger"
            value={positionId}
            onChange={(e) =>
              setPositionId(e.target.value === '' ? '' : Number(e.target.value))
            }
            data-testid="users-position-select"
          >
            <option value="">（未定级）</option>
            {positionOptions.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}（{p.category}）
              </option>
            ))}
          </select>
        </div>
        <div style={{ marginBottom: 12 }}>
          <label style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
            <input
              type="checkbox"
              checked={isInternal}
              onChange={(e) => setIsInternal(e.target.checked)}
            />
            内部员工
          </label>{' '}
          <label style={{ display: 'inline-flex', alignItems: 'center', gap: 6, marginLeft: 16 }}>
            <input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
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
              const positionIdPayload = positionId === '' ? null : positionId;
              if (editing) {
                await updateUser(editing.id, {
                  name,
                  code: code || undefined,
                  emailAddress: emailAddress || undefined,
                  isInternal,
                  enabled,
                  positionId: positionIdPayload,
                });
              } else {
                await createUser({
                  loginName,
                  name,
                  code: code || undefined,
                  emailAddress: emailAddress || undefined,
                  isInternal,
                  enabled,
                  positionId: positionIdPayload,
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
        title="删除用户"
        message={`确认删除「${confirmDelete?.name ?? ''}」？此操作不可撤销。`}
        onCancel={() => setConfirmDelete(null)}
        onConfirm={async () => {
          if (confirmDelete) await deleteUser(confirmDelete.id);
          setConfirmDelete(null);
          void list.refetch();
        }}
      />
    </div>
  );
}
