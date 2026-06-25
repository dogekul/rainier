import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { OwnerChip, StatusChip } from '../../components/board';
import { Button } from '../../components/ui/Button';
import { MarkdownView } from '../../components/ui/MarkdownView';
import { LinkPanel } from '../../components/LinkPanel';
import {
  getStory,
  updateStory,
  type Story,
  type StoryStatus,
  type StoryUpdate,
} from '../../api/story';
import { listTasks } from '../../api/task';
import { PRIORITY_LABELS } from '../../api/demand';
import { formatDateTime } from '../../utils/formatDate';
import { StoryEditDrawer } from './StoryEditDrawer';
import '../Pm/PmDetailPage.css';

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

type Tab = 'info' | 'tasks' | 'links';

/**
 * v0.0.61 — Story 详情页 (/pm/stories/:id). Breadcrumb [需求 / Sprint / Story].
 * Tabs: 基本信息 / 任务 / 关联链接. 任务列表来自 listTasks({ storyId }).
 */
export function StoryDetailPage() {
  const { id: idParam } = useParams<{ id: string }>();
  const id = Number(idParam);
  const navigate = useNavigate();

  const [s, setS] = useState<Story | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [tab, setTab] = useState<Tab>('info');
  const [taskCount, setTaskCount] = useState(0);
  const [drawerOpen, setDrawerOpen] = useState(false);

  const load = useCallback(() => {
    if (!Number.isFinite(id)) {
      setError('无效的 Story ID');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    getStory(id)
      .then((row) => setS(row))
      .catch(() => setError('未能加载该 Story，可能已被删除或无权限'))
      .finally(() => setLoading(false));
    listTasks({ storyId: id, size: 100 })
      .then((res) => setTaskCount(res.total))
      .catch(() => setTaskCount(0));
  }, [id]);

  useEffect(load, [load]);

  if (loading) return <div className="rainier-page">加载中…</div>;
  if (error || !s) {
    return (
      <div className="rainier-page">
        <div className="rainier-error-banner">{error ?? 'Story 不存在'}</div>
      </div>
    );
  }

  return (
    <div className="rainier-page pm-detail-page" data-testid="story-detail-page">
      <div className="pm-breadcrumb">
        <Link to="/pm/requirements">需求</Link>
        <span className="pm-breadcrumb-sep">/</span>
        {s.requirementId ? (
          <Link to={`/pm/requirements/${s.requirementId}`}>
            {s.requirementCode ?? `#${s.requirementId}`}
          </Link>
        ) : (
          <span>—</span>
        )}
        <span className="pm-breadcrumb-sep">/</span>
        {s.sprintId ? (
          <Link to={`/pm/sprints/${s.sprintId}`}>{s.sprintCode ?? `#${s.sprintId}`}</Link>
        ) : (
          <span>—</span>
        )}
        <span className="pm-breadcrumb-sep">/</span>
        <span className="pm-breadcrumb-current">{s.code}</span>
      </div>

      <div className="pm-detail-hero">
        <span className="pm-detail-hero-code">{s.code}</span>
        <span className="pm-detail-hero-title">{s.title}</span>
        <StatusChip
          status={s.status}
          label={STORY_STATUS_LABELS[s.status] ?? s.status}
          tier={STORY_STATUS_TIER[s.status]}
        />
        <StatusChip
          status={s.priority}
          label={`优先级·${PRIORITY_LABELS[s.priority] ?? s.priority}`}
          tier={
            s.priority === 'URGENT' || s.priority === 'HIGH'
              ? 'red'
              : s.priority === 'MEDIUM'
                ? 'yellow'
                : 'gray'
          }
        />
        <span className="pm-detail-hero-actions">
          <Button type="button" variant="secondary" onClick={() => setDrawerOpen(true)} data-testid="story-detail-edit">
            编辑
          </Button>
        </span>
      </div>

      <div className="pm-tabs" data-testid="story-detail-tabs">
        <button
          type="button"
          className="pm-tab"
          data-active={tab === 'info'}
          onClick={() => setTab('info')}
          data-testid="story-tab-info"
        >
          基本信息
        </button>
        <button
          type="button"
          className="pm-tab"
          data-active={tab === 'tasks'}
          onClick={() => setTab('tasks')}
          data-testid="story-tab-tasks"
        >
          任务
          <span className="pm-tab-count">· {taskCount}</span>
        </button>
        <button
          type="button"
          className="pm-tab"
          data-active={tab === 'links'}
          onClick={() => setTab('links')}
          data-testid="story-tab-links"
        >
          关联链接
        </button>
      </div>

      {tab === 'info' && (
        <div className="pm-tab-pane">
          <div className="pm-info-grid">
            <span className="pm-info-label">标题</span>
            <span className="pm-info-value">{s.title}</span>

            <span className="pm-info-label">负责人</span>
            <span className="pm-info-value">
              <OwnerChip name={s.ownerName} loginName={s.ownerLoginName} />
            </span>

            <span className="pm-info-label">项目</span>
            <span className="pm-info-value">
              {s.projectName ? `${s.projectCode ?? ''} ${s.projectName}`.trim() : '—'}
            </span>

            <span className="pm-info-label">所在 Sprint</span>
            <span className="pm-info-value">
              {s.sprintCode ? `${s.sprintName ?? ''}（${s.sprintCode}）` : '—'}
            </span>

            <span className="pm-info-label">复杂度</span>
            <span className="pm-info-value">{s.complexity ?? '—'}</span>

            <span className="pm-info-label">创建/更新</span>
            <span className="pm-info-value" style={{ color: 'var(--rainier-color-text-3)', fontSize: 12 }}>
              {formatDateTime(s.createTime)} · {formatDateTime(s.updateTime)}
            </span>
          </div>

          {s.description && (
            <div className="pm-info-section">
              <div className="pm-info-section-title">描述</div>
              <MarkdownView content={s.description} />
            </div>
          )}

          {s.acceptanceCriteria && (
            <div className="pm-info-section">
              <div className="pm-info-section-title">验收标准</div>
              <MarkdownView content={s.acceptanceCriteria} />
            </div>
          )}
        </div>
      )}

      {tab === 'tasks' && (
        <div className="pm-tab-pane">
          <StoryTaskList storyId={s.id} onCountChange={setTaskCount} />
        </div>
      )}

      {tab === 'links' && (
        <div className="pm-tab-pane">
          <LinkPanel targetType="STORY" targetId={s.id} />
        </div>
      )}

      <StoryEditDrawer
        open={drawerOpen}
        sprintId={s.sprintId}
        sprintCode={s.sprintCode ?? ''}
        sprintName={s.sprintName ?? ''}
        requirementCode={s.requirementCode ?? ''}
        requirementTitle={s.requirementTitle ?? ''}
        editing={s}
        onClose={() => setDrawerOpen(false)}
        onCreate={async () => {
          // not used
        }}
        onUpdate={async (sid, body: StoryUpdate) => {
          await updateStory(sid, body);
          setDrawerOpen(false);
          load();
        }}
      />

      <div>
        <Button
          type="button"
          variant="secondary"
          onClick={() =>
            s.sprintId ? navigate(`/pm/sprints/${s.sprintId}`) : navigate('/pm/requirements')
          }
          data-testid="story-detail-back"
        >
          ← 返回上级
        </Button>
      </div>
    </div>
  );
}

/** Lightweight task-list display under Story → 任务 tab. Each row clicks through to /pm/tasks/:id. */
function StoryTaskList({
  storyId,
  onCountChange,
}: {
  storyId: number;
  onCountChange: (n: number) => void;
}) {
  const navigate = useNavigate();
  const [rows, setRows] = useState<
    Awaited<ReturnType<typeof listTasks>>['content']
  >([]);

  useEffect(() => {
    void listTasks({ storyId, size: 100 })
      .then((res) => {
        setRows(res.content);
        onCountChange(res.total);
      })
      .catch(() => setRows([]));
  }, [storyId, onCountChange]);

  if (rows.length === 0) {
    return <div style={{ color: 'var(--rainier-color-text-3)' }}>暂无任务</div>;
  }
  return (
    <div className="rainier-list-table-wrap">
      {rows.map((t) => (
        <div
          key={t.id}
          className="rainier-table-row-clickable"
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            padding: '8px 4px',
            borderBottom: '1px solid var(--rainier-bg-hover)',
            cursor: 'pointer',
          }}
          onClick={() => navigate(`/pm/tasks/${t.id}`)}
          role="button"
          tabIndex={0}
          data-testid={`story-task-row-${t.id}`}
        >
          <span style={{ fontFamily: 'monospace', fontSize: 12, color: 'var(--rainier-color-text-3)' }}>
            {t.code}
          </span>
          <span style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {t.title}
          </span>
          <StatusChip status={t.status} />
          <span style={{ fontSize: 12, color: 'var(--rainier-color-text-3)' }}>
            {t.assigneeName ?? t.assigneeLoginName ?? '未分配'}
          </span>
        </div>
      ))}
    </div>
  );
}
