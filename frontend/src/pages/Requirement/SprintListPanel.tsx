import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../../components/ui/Button';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Table, type TableColumn } from '../../components/ui/Table';
import { OwnerChip, StatusChip } from '../../components/board';
import {
  createSprint,
  deleteSprint,
  listSprints,
  type Sprint,
  type SprintStatus,
} from '../../api/sprint';
import { SprintEditDrawer } from '../Sprint/SprintEditDrawer';

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

export interface SprintListPanelProps {
  requirementId: number;
  requirementCode: string;
  requirementTitle: string;
  onCountChange?: (newCount: number) => void;
}

/**
 * v0.0.61 — Sprint list panel, now embedded inside RequirementDetailPage's Sprint tab.
 * Row click → /pm/sprints/:id (replaces v0.0.10「展开」inline expansion). The「编辑/功能」row buttons
 * are gone; those actions live on the Sprint detail page. 新建 Sprint stays here as a quick CTA
 * inside the requirement's tab.
 */
export function SprintListPanel({
  requirementId,
  requirementCode,
  requirementTitle,
  onCountChange,
}: SprintListPanelProps) {
  const navigate = useNavigate();
  const [sprints, setSprints] = useState<Sprint[]>([]);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<Sprint | null>(null);

  const refetch = useCallback(async () => {
    const res = await listSprints({ requirementId, size: 100 });
    setSprints(res.content);
    onCountChange?.(res.total);
  }, [requirementId, onCountChange]);

  useEffect(() => {
    void refetch();
  }, [refetch]);

  const columns: TableColumn<Sprint>[] = [
    { key: 'code', title: '编码', render: (s) => s.code },
    { key: 'name', title: '名称', render: (s) => s.name },
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
    {
      key: 'actions',
      title: '操作',
      render: (s) => (
        <span onClick={(e) => e.stopPropagation()}>
          <Button type="button" variant="secondary" onClick={() => setConfirmDelete(s)}>
            删除
          </Button>
        </span>
      ),
    },
  ];

  return (
    <div data-testid={`sprint-list-panel-${requirementId}`}>
      <div style={{ display: 'flex', gap: 12, marginBottom: 12 }}>
        <Button
          type="button"
          onClick={() => setDrawerOpen(true)}
          data-testid="sprints-new-btn"
        >
          新建 Sprint
        </Button>
      </div>
      <Table<Sprint>
        columns={columns}
        dataSource={sprints}
        rowKey="id"
        onRowClick={(s) => navigate(`/pm/sprints/${s.id}`)}
        rowTestId={(s) => `sprint-list-row-${s.id}`}
        emptyText="该需求下还没有 Sprint。点「新建 Sprint」开始。"
      />
      <SprintEditDrawer
        open={drawerOpen}
        requirementId={requirementId}
        requirementCode={requirementCode}
        requirementTitle={requirementTitle}
        editing={null}
        onClose={() => setDrawerOpen(false)}
        onCreate={async (body) => {
          await createSprint(body);
          setDrawerOpen(false);
          void refetch();
        }}
        onUpdate={async () => {
          // SprintListPanel only creates here; edits happen on /pm/sprints/:id.
        }}
      />
      <ConfirmDialog
        open={confirmDelete !== null}
        title="删除 Sprint"
        message={`确认删除 Sprint「${confirmDelete?.name ?? ''}」？有关联 Story 时会被拒绝。`}
        onCancel={() => setConfirmDelete(null)}
        onConfirm={async () => {
          if (confirmDelete) await deleteSprint(confirmDelete.id);
          setConfirmDelete(null);
          void refetch();
        }}
      />
    </div>
  );
}
