import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import {
  createRequirement,
  deleteRequirement,
  listRequirements,
  REQUIREMENT_STATUS_LABELS,
  type Requirement,
} from '../../api/requirement';
import { PRIORITY_LABELS } from '../../api/demand';
import { usePaginated } from '../../hooks/usePaginated';
import { RequirementEditDrawer } from './RequirementEditDrawer';
import { OwnerChip, StatusChip } from '../../components/board';

/**
 * v0.0.61 — 需求列表页。行点击 → /pm/requirements/:id 详情页（替代之前的「展开」+ 编辑抽屉模式）。
 * 「新建」仍用抽屉（创建动作短，不需要全屏）。?openId=N 深链改为 redirect 到 /:id 详情。
 */
export function RequirementsPage() {
  const navigate = useNavigate();
  const fetcher = useCallback(
    async ({ page, size, search }: { page: number; size: number; search: string }) =>
      listRequirements({ page, size, search: search || undefined }),
    [],
  );
  const list = usePaginated<Requirement>(fetcher, { initialSize: 20 });

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<Requirement | null>(null);

  // v0.0.61 — ?openId=N 深链：来自商机/运营详情页的「跳转到此需求」，改为 redirect 到 /pm/requirements/:id 详情页。
  const [searchParams] = useSearchParams();
  useEffect(() => {
    const openId = Number(searchParams.get('openId'));
    if (Number.isFinite(openId) && openId > 0) {
      navigate(`/pm/requirements/${openId}`, { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const columns: TableColumn<Requirement>[] = [
    { key: 'code', title: '编码', render: (r) => r.code },
    { key: 'title', title: '标题', render: (r) => r.title },
    {
      key: 'owner',
      title: '负责人',
      render: (r) => <OwnerChip name={r.ownerName} loginName={r.ownerLoginName} />,
    },
    {
      key: 'project',
      title: '项目',
      render: (r) => (r.projectName ? `${r.projectName}（${r.projectCode ?? ''}）` : '—'),
    },
    {
      key: 'sprintCount',
      title: 'Sprint 数',
      render: (r) => (r.sprintCount ?? 0).toString(),
    },
    {
      key: 'status',
      title: '状态',
      render: (r) => (
        <StatusChip status={r.status} label={REQUIREMENT_STATUS_LABELS[r.status] ?? r.status} />
      ),
    },
    { key: 'priority', title: '优先级', render: (r) => PRIORITY_LABELS[r.priority] ?? r.priority },
    { key: 'complexity', title: '复杂度', render: (r) => r.complexity ?? '—' },
    {
      key: 'actions',
      title: '操作',
      // stopPropagation so clicking action buttons doesn't also navigate the row.
      render: (r) => (
        <span onClick={(e) => e.stopPropagation()}>
          <Button type="button" variant="secondary" onClick={() => setConfirmDelete(r)}>
            删除
          </Button>
        </span>
      ),
    },
  ];

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2>需求</h2>
        <span className="rainier-spacer" />
        <Button
          type="button"
          onClick={() => setDrawerOpen(true)}
          data-testid="req-new-btn"
        >
          新建需求
        </Button>
      </div>
      <Card>
        <Table<Requirement>
          columns={columns}
          dataSource={list.items}
          rowKey="id"
          onRowClick={(r) => navigate(`/pm/requirements/${r.id}`)}
          rowTestId={(r) => `req-row-${r.id}`}
        />
        <Pagination
          page={list.page}
          size={list.size}
          total={list.total}
          onPageChange={list.setPage}
        />
      </Card>
      <RequirementEditDrawer
        open={drawerOpen}
        editing={null}
        onClose={() => setDrawerOpen(false)}
        onCreate={async (body) => {
          await createRequirement(body);
          setDrawerOpen(false);
          void list.refetch();
        }}
        onUpdate={async () => {
          // List page only opens drawer in CREATE mode; edits happen on the detail page.
        }}
      />
      <ConfirmDialog
        open={confirmDelete !== null}
        title="删除需求"
        message={`确认删除需求「${confirmDelete?.code ?? ''}」？有关联诉求或 Sprint 时会被拒绝。`}
        onCancel={() => setConfirmDelete(null)}
        onConfirm={async () => {
          if (confirmDelete) await deleteRequirement(confirmDelete.id);
          setConfirmDelete(null);
          void list.refetch();
        }}
      />
    </div>
  );
}
