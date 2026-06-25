import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card } from '../../components/ui/Card';
import { OwnerChip, StatusChip } from '../../components/board';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import { listSprints, type Sprint, type SprintStatus } from '../../api/sprint';
import { usePaginated } from '../../hooks/usePaginated';

const SPRINT_STATUS_LABELS: Record<SprintStatus, string> = {
  PLANNING: '筹备',
  ACTIVE: '进行中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
};
const SPRINT_STATUS_TIER: Record<SprintStatus, 'gray' | 'yellow' | 'green' | 'red'> = {
  PLANNING: 'gray',
  ACTIVE: 'yellow',
  COMPLETED: 'green',
  CANCELLED: 'red',
};

/**
 * v0.0.61 — Sprint 列表页。行点击 → /pm/sprints/:id 详情页（替代之前的「展开」展示 StoryListPanel 模式）。
 * 创建 Sprint 已挪到需求详情页 Sprint Tab（必须带 requirementId 上下文）；此页面只读列表 + 跳转入口。
 */
export function SprintsPage() {
  const navigate = useNavigate();
  const fetcher = useCallback(
    async ({ page, size, search }: { page: number; size: number; search: string }) =>
      listSprints({ page, size, search: search || undefined }),
    [],
  );
  const list = usePaginated<Sprint>(fetcher, { initialSize: 20 });

  const columns: TableColumn<Sprint>[] = [
    { key: 'code', title: '编码', render: (s) => s.code },
    { key: 'name', title: '名称', render: (s) => s.name },
    {
      key: 'requirement',
      title: '需求',
      render: (s) =>
        s.requirementCode ? `${s.requirementTitle ?? ''}（${s.requirementCode}）` : '—',
    },
    {
      key: 'status',
      title: '状态',
      render: (s) => (
        <StatusChip
          status={s.status}
          label={SPRINT_STATUS_LABELS[s.status] ?? s.status}
          tier={SPRINT_STATUS_TIER[s.status]}
        />
      ),
    },
    {
      key: 'owner',
      title: '负责人',
      render: (s) => <OwnerChip name={s.ownerName} loginName={s.ownerLoginName} />,
    },
    { key: 'storyCount', title: 'Story 数', render: (s) => (s.storyCount ?? 0).toString() },
  ];

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2>Sprint</h2>
        <span className="rainier-spacer" />
      </div>
      <Card>
        <Table<Sprint>
          columns={columns}
          dataSource={list.items}
          rowKey="id"
          onRowClick={(s) => navigate(`/pm/sprints/${s.id}`)}
          rowTestId={(s) => `sprint-row-${s.id}`}
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
