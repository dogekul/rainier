import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { OwnerChip, StatusChip } from '../../components/board';
import { Button } from '../../components/ui/Button';
import { MarkdownView } from '../../components/ui/MarkdownView';
import { LinkPanel } from '../../components/LinkPanel';
import {
  getTask,
  updateTask,
  TASK_STATUS_LABELS,
  type Task,
  type TaskUpdate,
} from '../../api/task';
import { PRIORITY_LABELS } from '../../api/demand';
import { formatDate, formatDateTime } from '../../utils/formatDate';
import { TaskEditDrawer } from './TaskEditDrawer';
import '../Pm/PmDetailPage.css';

type Tab = 'info' | 'links';

/**
 * v0.0.61 — Task 详情页 (/pm/tasks/:id). Breadcrumb chain Project / Sprint / Story / Task.
 * Tabs: 基本信息 / 关联链接. 编辑通过 TaskEditDrawer.
 */
export function TaskDetailPage() {
  const { id: idParam } = useParams<{ id: string }>();
  const id = Number(idParam);
  const navigate = useNavigate();

  const [t, setT] = useState<Task | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [tab, setTab] = useState<Tab>('info');
  const [drawerOpen, setDrawerOpen] = useState(false);

  const load = useCallback(() => {
    if (!Number.isFinite(id)) {
      setError('无效的任务 ID');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    getTask(id)
      .then((row) => setT(row))
      .catch(() => setError('未能加载该任务，可能已被删除或无权限'))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(load, [load]);

  if (loading) return <div className="rainier-page">加载中…</div>;
  if (error || !t) {
    return (
      <div className="rainier-page">
        <div className="rainier-error-banner">{error ?? '任务不存在'}</div>
      </div>
    );
  }

  return (
    <div className="rainier-page pm-detail-page" data-testid="task-detail-page">
      <div className="pm-breadcrumb">
        {t.projectName ? (
          <span>
            {t.projectCode ?? ''} {t.projectName}
          </span>
        ) : (
          <span>项目</span>
        )}
        {t.sprintId && (
          <>
            <span className="pm-breadcrumb-sep">/</span>
            <Link to={`/pm/sprints/${t.sprintId}`}>{t.sprintCode ?? `#${t.sprintId}`}</Link>
          </>
        )}
        {t.storyId && (
          <>
            <span className="pm-breadcrumb-sep">/</span>
            <Link to={`/pm/stories/${t.storyId}`}>{t.storyCode ?? `#${t.storyId}`}</Link>
          </>
        )}
        <span className="pm-breadcrumb-sep">/</span>
        <span className="pm-breadcrumb-current">{t.code}</span>
      </div>

      <div className="pm-detail-hero">
        <span className="pm-detail-hero-code">{t.code}</span>
        <span className="pm-detail-hero-title">{t.title}</span>
        <StatusChip status={t.status} label={TASK_STATUS_LABELS[t.status] ?? t.status} />
        <StatusChip
          status={t.priority}
          label={`优先级·${PRIORITY_LABELS[t.priority] ?? t.priority}`}
          tier={
            t.priority === 'URGENT' || t.priority === 'HIGH'
              ? 'red'
              : t.priority === 'MEDIUM'
                ? 'yellow'
                : 'gray'
          }
        />
        <span className="pm-detail-hero-actions">
          <Button type="button" variant="secondary" onClick={() => setDrawerOpen(true)} data-testid="task-detail-edit">
            编辑
          </Button>
        </span>
      </div>

      <div className="pm-tabs" data-testid="task-detail-tabs">
        <button
          type="button"
          className="pm-tab"
          data-active={tab === 'info'}
          onClick={() => setTab('info')}
          data-testid="task-tab-info"
        >
          基本信息
        </button>
        <button
          type="button"
          className="pm-tab"
          data-active={tab === 'links'}
          onClick={() => setTab('links')}
          data-testid="task-tab-links"
        >
          关联链接
        </button>
      </div>

      {tab === 'info' && (
        <div className="pm-tab-pane">
          <div className="pm-info-grid">
            <span className="pm-info-label">标题</span>
            <span className="pm-info-value">{t.title}</span>

            <span className="pm-info-label">指派人</span>
            <span className="pm-info-value">
              <OwnerChip name={t.assigneeName} loginName={t.assigneeLoginName} />
            </span>

            <span className="pm-info-label">项目</span>
            <span className="pm-info-value">
              {t.projectName ? `${t.projectCode ?? ''} ${t.projectName}`.trim() : '—'}
            </span>

            {t.sprintId && (
              <>
                <span className="pm-info-label">Sprint</span>
                <span className="pm-info-value">
                  <Link to={`/pm/sprints/${t.sprintId}`}>
                    {t.sprintName ?? ''}（{t.sprintCode ?? `#${t.sprintId}`}）
                  </Link>
                </span>
              </>
            )}

            {t.storyId && (
              <>
                <span className="pm-info-label">Story</span>
                <span className="pm-info-value">
                  <Link to={`/pm/stories/${t.storyId}`}>
                    {t.storyTitle ?? ''}（{t.storyCode ?? `#${t.storyId}`}）
                  </Link>
                </span>
              </>
            )}

            <span className="pm-info-label">到期日</span>
            <span className="pm-info-value">{formatDate(t.dueDate)}</span>

            <span className="pm-info-label">优先级</span>
            <span className="pm-info-value">{PRIORITY_LABELS[t.priority] ?? t.priority}</span>

            {t.closeReason && (
              <>
                <span className="pm-info-label">关闭原因</span>
                <span className="pm-info-value">{t.closeReason}</span>
              </>
            )}

            <span className="pm-info-label">创建/更新</span>
            <span className="pm-info-value" style={{ color: 'var(--rainier-color-text-3)', fontSize: 12 }}>
              {formatDateTime(t.createTime)} · {formatDateTime(t.updateTime)}
            </span>
          </div>

          {t.description && (
            <div className="pm-info-section">
              <div className="pm-info-section-title">描述</div>
              <MarkdownView content={t.description} />
            </div>
          )}
        </div>
      )}

      {tab === 'links' && (
        <div className="pm-tab-pane">
          <LinkPanel targetType="TASK" targetId={t.id} />
        </div>
      )}

      <TaskEditDrawer
        open={drawerOpen}
        editing={t}
        onClose={() => setDrawerOpen(false)}
        onCreate={async () => {
          // not used
        }}
        onUpdate={async (tid, body: TaskUpdate) => {
          await updateTask(tid, body);
          setDrawerOpen(false);
          load();
        }}
      />

      <div>
        <Button
          type="button"
          variant="secondary"
          onClick={() =>
            t.storyId
              ? navigate(`/pm/stories/${t.storyId}`)
              : t.sprintId
                ? navigate(`/pm/sprints/${t.sprintId}`)
                : navigate('/pm/tasks')
          }
          data-testid="task-detail-back"
        >
          ← 返回上级
        </Button>
      </div>
    </div>
  );
}
