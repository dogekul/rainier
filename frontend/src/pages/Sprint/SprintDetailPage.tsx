import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { OwnerChip, StatusChip } from '../../components/board';
import { Button } from '../../components/ui/Button';
import { MarkdownView } from '../../components/ui/MarkdownView';
import {
  getSprint,
  updateSprint,
  type Sprint,
  type SprintStatus,
  type SprintUpdate,
} from '../../api/sprint';
import { listStories } from '../../api/story';
import { formatDate, formatDateTime } from '../../utils/formatDate';
import { SprintEditDrawer } from './SprintEditDrawer';
import { StoryListPanel } from '../Requirement/StoryListPanel';
import { SprintFeaturePanel } from './SprintFeaturePanel';
import '../Pm/PmDetailPage.css';

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

type Tab = 'info' | 'stories' | 'features';

/**
 * v0.0.61 — Sprint 详情页 (/pm/sprints/:id). Breadcrumb [需求 X / Sprint Y].
 * Tabs: 基本信息 / Story / Feature. 编辑通过 SprintEditDrawer.
 */
export function SprintDetailPage() {
  const { id: idParam } = useParams<{ id: string }>();
  const id = Number(idParam);
  const navigate = useNavigate();

  const [sp, setSp] = useState<Sprint | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [tab, setTab] = useState<Tab>('info');
  const [storyCount, setStoryCount] = useState(0);
  const [drawerOpen, setDrawerOpen] = useState(false);

  const load = useCallback(() => {
    if (!Number.isFinite(id)) {
      setError('无效的 Sprint ID');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    getSprint(id)
      .then((s) => setSp(s))
      .catch(() => setError('未能加载该 Sprint，可能已被删除或无权限'))
      .finally(() => setLoading(false));
    listStories({ sprintId: id, size: 100 })
      .then((res) => setStoryCount(res.total))
      .catch(() => setStoryCount(0));
  }, [id]);

  useEffect(load, [load]);

  if (loading) return <div className="rainier-page">加载中…</div>;
  if (error || !sp) {
    return (
      <div className="rainier-page">
        <div className="rainier-error-banner">{error ?? 'Sprint 不存在'}</div>
        <div>
          <Link to="/pm/sprints">← 返回 Sprint 列表</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="rainier-page pm-detail-page" data-testid="sprint-detail-page">
      <div className="pm-breadcrumb">
        <Link to="/pm/requirements">需求</Link>
        <span className="pm-breadcrumb-sep">/</span>
        {sp.requirementId ? (
          <Link to={`/pm/requirements/${sp.requirementId}`}>
            {sp.requirementCode ?? `#${sp.requirementId}`} {sp.requirementTitle ?? ''}
          </Link>
        ) : (
          <span>—</span>
        )}
        <span className="pm-breadcrumb-sep">/</span>
        <span className="pm-breadcrumb-current">{sp.code}</span>
      </div>

      <div className="pm-detail-hero">
        <span className="pm-detail-hero-code">{sp.code}</span>
        <span className="pm-detail-hero-title">{sp.name}</span>
        <StatusChip
          status={sp.status}
          label={SPRINT_STATUS_LABELS[sp.status] ?? sp.status}
          tier={SPRINT_STATUS_TIER[sp.status]}
        />
        <span className="pm-detail-hero-actions">
          <Button type="button" variant="secondary" onClick={() => setDrawerOpen(true)} data-testid="sprint-detail-edit">
            编辑
          </Button>
        </span>
      </div>

      <div className="pm-tabs" data-testid="sprint-detail-tabs">
        <button
          type="button"
          className="pm-tab"
          data-active={tab === 'info'}
          onClick={() => setTab('info')}
          data-testid="sprint-tab-info"
        >
          基本信息
        </button>
        <button
          type="button"
          className="pm-tab"
          data-active={tab === 'stories'}
          onClick={() => setTab('stories')}
          data-testid="sprint-tab-stories"
        >
          Story 列表
          <span className="pm-tab-count">· {storyCount}</span>
        </button>
        <button
          type="button"
          className="pm-tab"
          data-active={tab === 'features'}
          onClick={() => setTab('features')}
          data-testid="sprint-tab-features"
        >
          关联功能
        </button>
      </div>

      {tab === 'info' && (
        <div className="pm-tab-pane">
          <div className="pm-info-grid">
            <span className="pm-info-label">名称</span>
            <span className="pm-info-value">{sp.name}</span>

            <span className="pm-info-label">负责人</span>
            <span className="pm-info-value">
              <OwnerChip name={sp.ownerName} loginName={sp.ownerLoginName} />
            </span>

            <span className="pm-info-label">项目</span>
            <span className="pm-info-value">
              {sp.projectName ? `${sp.projectCode ?? ''} ${sp.projectName}`.trim() : '—'}
            </span>

            <span className="pm-info-label">产品</span>
            <span className="pm-info-value">{sp.productName ?? '—'}</span>

            <span className="pm-info-label">状态</span>
            <span className="pm-info-value">
              <StatusChip
                status={sp.status}
                label={SPRINT_STATUS_LABELS[sp.status] ?? sp.status}
                tier={SPRINT_STATUS_TIER[sp.status]}
              />
            </span>

            <span className="pm-info-label">起止</span>
            <span className="pm-info-value">
              {formatDate(sp.startDate)} → {formatDate(sp.endDate)}
            </span>

            <span className="pm-info-label">Story 数</span>
            <span className="pm-info-value">{sp.storyCount ?? storyCount}</span>

            {sp.goal && (
              <>
                <span className="pm-info-label">目标</span>
                <span className="pm-info-value">{sp.goal}</span>
              </>
            )}

            <span className="pm-info-label">创建/更新</span>
            <span className="pm-info-value" style={{ color: 'var(--rainier-color-text-3)', fontSize: 12 }}>
              {formatDateTime(sp.createTime)} · {formatDateTime(sp.updateTime)}
            </span>
          </div>

          {sp.description && (
            <div className="pm-info-section">
              <div className="pm-info-section-title">描述</div>
              <MarkdownView content={sp.description} />
            </div>
          )}
        </div>
      )}

      {tab === 'stories' && (
        <div className="pm-tab-pane">
          <StoryListPanel
            key={`story-${sp.id}`}
            sprintId={sp.id}
            sprintCode={sp.code}
            sprintName={sp.name}
            requirementCode={sp.requirementCode ?? ''}
            requirementTitle={sp.requirementTitle ?? ''}
            onCountChange={setStoryCount}
          />
        </div>
      )}

      {tab === 'features' && (
        <div className="pm-tab-pane">
          <SprintFeaturePanel sprintId={sp.id} productId={sp.productId ?? null} />
        </div>
      )}

      <SprintEditDrawer
        open={drawerOpen}
        requirementId={sp.requirementId}
        requirementCode={sp.requirementCode ?? ''}
        requirementTitle={sp.requirementTitle ?? ''}
        editing={sp}
        onClose={() => setDrawerOpen(false)}
        onCreate={async () => {
          // not used: detail page never creates
        }}
        onUpdate={async (sid: number, body: SprintUpdate) => {
          await updateSprint(sid, body);
          setDrawerOpen(false);
          load();
        }}
      />

      <div>
        <Button
          type="button"
          variant="secondary"
          onClick={() => navigate('/pm/sprints')}
          data-testid="sprint-detail-back"
        >
          ← 返回 Sprint 列表
        </Button>
      </div>
    </div>
  );
}
