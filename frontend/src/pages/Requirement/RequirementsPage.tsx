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
import { StoryListPanel } from './StoryListPanel';

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
  // v0.0.9: track which Requirement rows are expanded to reveal Story sub-list.
  const [expanded, setExpanded] = useState<Set<number>>(new Set());
  const toggleExpanded = (id: number) => {
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };
  // Memoize so StoryListPanel's effect doesn't re-trigger on every parent render
  // (a fresh-function identity would cause an infinite refetch loop).
  const refetchRequirements = list.refetch;
  const onStoryCountChange = useCallback(() => {
    void refetchRequirements();
  }, [refetchRequirements]);

  const columns: TableColumn<Requirement>[] = [
    {
      key: 'expand',
      title: '',
      render: (r) => (
        <Button
          type="button"
          variant="secondary"
          onClick={() => toggleExpanded(r.id)}
          data-testid={`req-expand-btn-${r.id}`}
        >
          {expanded.has(r.id) ? '收起' : '展开'}
        </Button>
      ),
    },
    { key: 'code', title: '编码', render: (r) => r.code },
    { key: 'title', title: '标题', render: (r) => r.title },
    {
      key: 'owner',
      title: '负责人',
      render: (r) =>
        r.ownerName ? `${r.ownerName}（${r.ownerLoginName ?? ''}）` : r.ownerUserId,
    },
    {
      key: 'project',
      title: '项目',
      render: (r) => (r.projectName ? `${r.projectName}（${r.projectCode ?? ''}）` : '—'),
    },
    {
      key: 'storyCount',
      title: 'Story 数',
      render: (r) => (r.storyCount ?? 0).toString(),
    },
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
      <Table<Requirement>
        columns={columns}
        dataSource={list.items}
        rowKey="id"
        isExpanded={(r) => expanded.has(r.id)}
        renderExpanded={(r) => (
          <StoryListPanel
            requirementId={r.id}
            requirementCode={r.code}
            requirementTitle={r.title}
            onCountChange={onStoryCountChange}
          />
        )}
      />
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
        message={`确认删除需求「${confirmDelete?.code ?? ''}」？有关联诉求或 Story 时会被拒绝。`}
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
