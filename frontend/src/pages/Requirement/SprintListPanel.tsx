import { useCallback, useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Table, type TableColumn } from '../../components/ui/Table';
import {
  createSprint,
  deleteSprint,
  listSprints,
  updateSprint,
  type Sprint,
} from '../../api/sprint';
import { SprintEditDrawer } from '../Sprint/SprintEditDrawer';

export interface SprintListPanelProps {
  requirementId: number;
  requirementCode: string;
  requirementTitle: string;
  onCountChange?: (newCount: number) => void;
}

export function SprintListPanel({
  requirementId,
  requirementCode,
  requirementTitle,
  onCountChange,
}: SprintListPanelProps) {
  const [sprints, setSprints] = useState<Sprint[]>([]);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editing, setEditing] = useState<Sprint | null>(null);
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
    { key: 'status', title: '状态', render: (s) => s.status },
    {
      key: 'owner',
      title: '负责人',
      render: (s) =>
        s.ownerName ? `${s.ownerName}（${s.ownerLoginName ?? ''}）` : s.ownerUserId,
    },
    { key: 'storyCount', title: 'Story 数', render: (s) => (s.storyCount ?? 0).toString() },
    {
      key: 'actions',
      title: '操作',
      render: (s) => (
        <>
          <Button
            type="button"
            variant="secondary"
            onClick={() => {
              setEditing(s);
              setDrawerOpen(true);
            }}
          >
            编辑
          </Button>{' '}
          <Button type="button" variant="secondary" onClick={() => setConfirmDelete(s)}>
            删除
          </Button>
        </>
      ),
    },
  ];

  return (
    <div
      style={{
        padding: 12,
        background: 'var(--rainier-color-bg-2)',
        borderRadius: 4,
        marginTop: 8,
      }}
      data-testid={`sprint-list-panel-${requirementId}`}
    >
      <div style={{ display: 'flex', gap: 12, marginBottom: 12 }}>
        <Button
          type="button"
          onClick={() => {
            setEditing(null);
            setDrawerOpen(true);
          }}
          data-testid="sprints-new-btn"
        >
          新建 Sprint
        </Button>
      </div>
      <Table<Sprint> columns={columns} dataSource={sprints} rowKey="id" />
      <SprintEditDrawer
        key={editing?.id ?? 'new'}
        open={drawerOpen}
        requirementId={requirementId}
        requirementCode={requirementCode}
        requirementTitle={requirementTitle}
        editing={editing}
        onClose={() => setDrawerOpen(false)}
        onCreate={async (body) => {
          await createSprint(body);
          setDrawerOpen(false);
          void refetch();
        }}
        onUpdate={async (id, body) => {
          await updateSprint(id, body);
          setDrawerOpen(false);
          void refetch();
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
