import { useCallback, useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import {
  createPosition,
  deletePosition,
  listPositions,
  updatePosition,
  type Position,
  type PositionCategory,
} from '../../api/position';
import { usePaginated } from '../../hooks/usePaginated';

const CATEGORIES: PositionCategory[] = ['TECH', 'BIZ', 'PM', 'MGMT', 'OTHER'];

export function PositionsPage() {
  const fetcher = useCallback(
    async ({ page, size, search }: { page: number; size: number; search: string }) =>
      listPositions({ page, size, search: search || undefined }),
    [],
  );
  const list = usePaginated<Position>(fetcher, { initialSize: 20 });

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editing, setEditing] = useState<Position | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<Position | null>(null);

  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState<PositionCategory>('TECH');
  const [enabled, setEnabled] = useState(true);

  useEffect(() => {
    if (!drawerOpen) return;
    if (editing) {
      setCode(editing.code);
      setName(editing.name);
      setDescription(editing.description ?? '');
      setCategory(editing.category);
      setEnabled(editing.enabled);
    } else {
      setCode('');
      setName('');
      setDescription('');
      setCategory('TECH');
      setEnabled(true);
    }
  }, [drawerOpen, editing]);

  const columns: TableColumn<Position>[] = [
    { key: 'code', title: '编码', render: (r) => r.code },
    { key: 'name', title: '名称', render: (r) => r.name },
    { key: 'category', title: '类别', render: (r) => r.category },
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
          data-testid="positions-new-btn"
        >
          新建岗位
        </Button>
      </div>
      <Table<Position> columns={columns} dataSource={list.items} rowKey="id" />
      <Pagination
        page={list.page}
        size={list.size}
        total={list.total}
        onPageChange={list.setPage}
      />
      <Drawer
        open={drawerOpen}
        title={editing ? '编辑岗位' : '新建岗位'}
        onClose={() => setDrawerOpen(false)}
      >
        <Input
          label="编码"
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
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>类别</label>
          <select
            className="rainier-treeselect-trigger"
            value={category}
            onChange={(e) => setCategory(e.target.value as PositionCategory)}
          >
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </div>
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
                await updatePosition(editing.id, {
                  name,
                  description: description || undefined,
                  category,
                  enabled,
                });
              } else {
                await createPosition({
                  code,
                  name,
                  description: description || undefined,
                  category,
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
        title="删除岗位"
        message={`确认删除岗位「${confirmDelete?.name ?? ''}」？已分配给用户时会被拒绝。`}
        onCancel={() => setConfirmDelete(null)}
        onConfirm={async () => {
          if (confirmDelete) await deletePosition(confirmDelete.id);
          setConfirmDelete(null);
          void list.refetch();
        }}
      />
    </Card>
  );
}
