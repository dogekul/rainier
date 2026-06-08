import { useCallback, useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Table, type TableColumn } from '../../components/ui/Table';
import {
  createStory,
  deleteStory,
  listStories,
  updateStory,
  type Story,
} from '../../api/story';
import { StoryEditDrawer } from './StoryEditDrawer';

export interface StoryListPanelProps {
  /** v0.0.10: Story belongs to Sprint. */
  sprintId: number;
  sprintCode: string;
  sprintName: string;
  /** Grandparent Requirement display info (for drawer locked-display). */
  requirementCode: string;
  requirementTitle: string;
  refreshKey?: number;
  onCountChange?: (newCount: number) => void;
}

export function StoryListPanel({
  sprintId,
  sprintCode,
  sprintName,
  requirementCode,
  requirementTitle,
  refreshKey,
  onCountChange,
}: StoryListPanelProps) {
  const [stories, setStories] = useState<Story[]>([]);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editing, setEditing] = useState<Story | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<Story | null>(null);

  const refetch = useCallback(async () => {
    const res = await listStories({ sprintId, size: 100 });
    setStories(res.content);
    onCountChange?.(res.total);
  }, [sprintId, onCountChange]);

  useEffect(() => {
    void refetch();
  }, [refetch, refreshKey]);

  const columns: TableColumn<Story>[] = [
    { key: 'code', title: '编码', render: (s) => s.code },
    { key: 'title', title: '标题', render: (s) => s.title },
    { key: 'status', title: '状态', render: (s) => s.status },
    { key: 'priority', title: '优先级', render: (s) => s.priority },
    { key: 'complexity', title: '复杂度', render: (s) => s.complexity ?? '—' },
    {
      key: 'owner',
      title: '负责人',
      render: (s) =>
        s.ownerName ? `${s.ownerName}（${s.ownerLoginName ?? ''}）` : s.ownerUserId,
    },
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
            data-testid={`story-edit-btn-${s.id}`}
          >
            编辑
          </Button>{' '}
          <Button
            type="button"
            variant="secondary"
            onClick={() => setConfirmDelete(s)}
            data-testid={`story-delete-btn-${s.id}`}
          >
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
      data-testid={`story-list-panel-${sprintId}`}
    >
      <div style={{ display: 'flex', gap: 12, marginBottom: 12 }}>
        <Button
          type="button"
          onClick={() => {
            setEditing(null);
            setDrawerOpen(true);
          }}
          data-testid="stories-new-btn"
        >
          新建 Story
        </Button>
      </div>
      <Table<Story> columns={columns} dataSource={stories} rowKey="id" />
      <StoryEditDrawer
        key={editing?.id ?? 'new'}
        open={drawerOpen}
        sprintId={sprintId}
        sprintCode={sprintCode}
        sprintName={sprintName}
        requirementCode={requirementCode}
        requirementTitle={requirementTitle}
        editing={editing}
        onClose={() => setDrawerOpen(false)}
        onCreate={async (body) => {
          await createStory(body);
          setDrawerOpen(false);
          void refetch();
        }}
        onUpdate={async (id, body) => {
          await updateStory(id, body);
          setDrawerOpen(false);
          void refetch();
        }}
      />
      <ConfirmDialog
        open={confirmDelete !== null}
        title="删除 Story"
        message={`确认删除 Story「${confirmDelete?.title ?? ''}」？软删后可由后台恢复。`}
        onCancel={() => setConfirmDelete(null)}
        onConfirm={async () => {
          if (confirmDelete) await deleteStory(confirmDelete.id);
          setConfirmDelete(null);
          void refetch();
        }}
      />
    </div>
  );
}
