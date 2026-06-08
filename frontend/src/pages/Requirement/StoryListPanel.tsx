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
  requirementId: number;
  requirementCode: string;
  requirementTitle: string;
  /** Bumped by the parent to force a refetch (e.g. after the Requirement row mutates). */
  refreshKey?: number;
  /** Called whenever the Story count under this Requirement changes (count visible to the parent). */
  onCountChange?: (newCount: number) => void;
}

export function StoryListPanel({
  requirementId,
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
    // PageParams 校验 size <= 100；一个 Requirement 下的 Stories 池小，100 足够 v0.
    const res = await listStories({ requirementId, size: 100 });
    setStories(res.content);
    onCountChange?.(res.total);
  }, [requirementId, onCountChange]);

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
      data-testid={`story-list-panel-${requirementId}`}
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
        // Remount the drawer when switching between new/edit modes (or between different editing
        // targets) so its useEffect-driven state reset always fires from a clean slate.
        // Without this key, the unchanged `open=false` cleanup branch only clears `formError` and
        // leaves stale `code` / `title` / `owner` from the previous session.
        key={editing?.id ?? 'new'}
        open={drawerOpen}
        requirementId={requirementId}
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
