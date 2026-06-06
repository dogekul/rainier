import { useCallback, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import {
  createRequirement,
  deleteRequirement,
  listRequirements,
  updateRequirement,
  type Requirement,
} from '../../api/requirement';
import { usePaginated } from '../../hooks/usePaginated';
import { RequirementEditDrawer } from './RequirementEditDrawer';

export function RequirementsPage() {
  const fetcher = useCallback(
    async ({ page, size, search }: { page: number; size: number; search: string }) =>
      listRequirements({ page, size, search: search || undefined }),
    [],
  );
  const list = usePaginated<Requirement>(fetcher, { initialSize: 20 });

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editing, setEditing] = useState<Requirement | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<Requirement | null>(null);

  const columns: TableColumn<Requirement>[] = [
    { key: 'code', title: '编码', render: (r) => r.code },
    { key: 'title', title: '标题', render: (r) => r.title },
    { key: 'ownerUserId', title: '负责人', render: (r) => r.ownerUserId },
    { key: 'status', title: '状态', render: (r) => r.status },
    { key: 'priority', title: '优先级', render: (r) => r.priority },
    { key: 'complexity', title: '复杂度', render: (r) => r.complexity ?? '—' },
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
          data-testid="req-new-btn"
        >
          新建需求
        </Button>
      </div>
      <Table<Requirement> columns={columns} dataSource={list.items} rowKey="id" />
      <Pagination
        page={list.page}
        size={list.size}
        total={list.total}
        onPageChange={list.setPage}
      />
      <RequirementEditDrawer
        open={drawerOpen}
        editing={editing}
        onClose={() => setDrawerOpen(false)}
        onCreate={async (body) => {
          await createRequirement(body);
          setDrawerOpen(false);
          void list.refetch();
        }}
        onUpdate={async (id, body) => {
          await updateRequirement(id, body);
          setDrawerOpen(false);
          void list.refetch();
        }}
      />
      <ConfirmDialog
        open={confirmDelete !== null}
        title="删除需求"
        message={`确认删除需求「${confirmDelete?.code ?? ''}」？已有关联诉求时会被拒绝。`}
        onCancel={() => setConfirmDelete(null)}
        onConfirm={async () => {
          if (confirmDelete) await deleteRequirement(confirmDelete.id);
          setConfirmDelete(null);
          void list.refetch();
        }}
      />
    </Card>
  );
}
