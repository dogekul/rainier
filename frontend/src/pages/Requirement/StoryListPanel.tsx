import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../../components/ui/Button';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Table, type TableColumn } from '../../components/ui/Table';
import { OwnerChip, StatusChip } from '../../components/board';
import { PRIORITY_LABELS } from '../../api/demand';
import {
  createStory,
  deleteStory,
  listStories,
  type Story,
  type StoryStatus,
} from '../../api/story';
import { StoryEditDrawer } from './StoryEditDrawer';

const STORY_STATUS_LABELS: Record<StoryStatus, string> = {
  DRAFT: '草稿',
  READY: '待开发',
  IN_PROGRESS: '进行中',
  DONE: '已完成',
  BLOCKED: '阻塞',
  CANCELLED: '已取消',
};
const STORY_STATUS_TIER: Record<StoryStatus, 'gray' | 'yellow' | 'green' | 'red'> = {
  DRAFT: 'gray',
  READY: 'gray',
  IN_PROGRESS: 'yellow',
  DONE: 'green',
  BLOCKED: 'red',
  CANCELLED: 'red',
};

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

/**
 * v0.0.61 — Story list panel, embedded inside SprintDetailPage's Story tab.
 * Row click → /pm/stories/:id (replaces v0.0.10 inline-edit drawer per row).
 * 新建 Story stays here as a quick CTA. Edits happen on the detail page.
 */
export function StoryListPanel({
  sprintId,
  sprintCode,
  sprintName,
  requirementCode,
  requirementTitle,
  refreshKey,
  onCountChange,
}: StoryListPanelProps) {
  const navigate = useNavigate();
  const [stories, setStories] = useState<Story[]>([]);
  const [drawerOpen, setDrawerOpen] = useState(false);
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
    {
      key: 'status',
      title: '状态',
      render: (s) => (
        <StatusChip
          status={s.status}
          label={STORY_STATUS_LABELS[s.status] ?? s.status}
          tier={STORY_STATUS_TIER[s.status]}
        />
      ),
    },
    { key: 'priority', title: '优先级', render: (s) => PRIORITY_LABELS[s.priority] ?? s.priority },
    { key: 'complexity', title: '复杂度', render: (s) => s.complexity ?? '—' },
    {
      key: 'owner',
      title: '负责人',
      render: (s) => <OwnerChip name={s.ownerName} loginName={s.ownerLoginName} />,
    },
    {
      key: 'actions',
      title: '操作',
      render: (s) => (
        <span onClick={(e) => e.stopPropagation()}>
          <Button
            type="button"
            variant="secondary"
            onClick={() => setConfirmDelete(s)}
            data-testid={`story-delete-btn-${s.id}`}
          >
            删除
          </Button>
        </span>
      ),
    },
  ];

  return (
    <div data-testid={`story-list-panel-${sprintId}`}>
      <div style={{ display: 'flex', gap: 12, marginBottom: 12 }}>
        <Button
          type="button"
          onClick={() => setDrawerOpen(true)}
          data-testid="stories-new-btn"
        >
          新建 Story
        </Button>
      </div>
      <Table<Story>
        columns={columns}
        dataSource={stories}
        rowKey="id"
        onRowClick={(s) => navigate(`/pm/stories/${s.id}`)}
        rowTestId={(s) => `story-list-row-${s.id}`}
        emptyText="该 Sprint 下还没有 Story。点「新建 Story」开始。"
      />
      <StoryEditDrawer
        open={drawerOpen}
        sprintId={sprintId}
        sprintCode={sprintCode}
        sprintName={sprintName}
        requirementCode={requirementCode}
        requirementTitle={requirementTitle}
        editing={null}
        onClose={() => setDrawerOpen(false)}
        onCreate={async (body) => {
          await createStory(body);
          setDrawerOpen(false);
          void refetch();
        }}
        onUpdate={async () => {
          // StoryListPanel only creates here; edits happen on /pm/stories/:id.
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
