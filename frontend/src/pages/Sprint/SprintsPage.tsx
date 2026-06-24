import { useCallback, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import { StatusChip } from '../../components/board';
import { listSprints, type Sprint } from '../../api/sprint';
import { usePaginated } from '../../hooks/usePaginated';
import { StoryListPanel } from '../Requirement/StoryListPanel';

export function SprintsPage() {
  const fetcher = useCallback(
    async ({ page, size, search }: { page: number; size: number; search: string }) =>
      listSprints({ page, size, search: search || undefined }),
    [],
  );
  const list = usePaginated<Sprint>(fetcher, { initialSize: 20 });
  const [expanded, setExpanded] = useState<Set<number>>(new Set());
  const toggleExpanded = (id: number) => {
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };
  const refetchSprints = list.refetch;
  const onStoryCountChange = useCallback(() => {
    void refetchSprints();
  }, [refetchSprints]);

  const columns: TableColumn<Sprint>[] = [
    {
      key: 'expand',
      title: '',
      render: (s) => (
        <Button
          type="button"
          variant="secondary"
          onClick={() => toggleExpanded(s.id)}
          data-testid={`sprint-expand-btn-${s.id}`}
        >
          {expanded.has(s.id) ? '收起' : '展开'}
        </Button>
      ),
    },
    { key: 'code', title: '编码', render: (s) => s.code },
    { key: 'name', title: '名称', render: (s) => s.name },
    {
      key: 'requirement',
      title: '需求',
      render: (s) => `${s.requirementTitle ?? ''}（${s.requirementCode ?? ''}）`,
    },
    { key: 'status', title: '状态', render: (s) => <StatusChip status={s.status} /> },
    {
      key: 'owner',
      title: '负责人',
      render: (s) =>
        s.ownerName ? `${s.ownerName}（${s.ownerLoginName ?? ''}）` : s.ownerUserId,
    },
    { key: 'storyCount', title: 'Story 数', render: (s) => (s.storyCount ?? 0).toString() },
  ];

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2>Sprint</h2>
        <div style={{ flex: 1 }} />
      </div>
      <Card>
        <Table<Sprint>
          columns={columns}
          dataSource={list.items}
          rowKey="id"
          isExpanded={(s) => expanded.has(s.id)}
          renderExpanded={(s) => (
            <StoryListPanel
              sprintId={s.id}
              sprintCode={s.code}
              sprintName={s.name}
              requirementCode={s.requirementCode ?? ''}
              requirementTitle={s.requirementTitle ?? ''}
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
      </Card>
    </div>
  );
}
