import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { OwnerChip, StatusChip } from '../../components/board';
import { Button } from '../../components/ui/Button';
import { MarkdownView } from '../../components/ui/MarkdownView';
import {
  getRequirement,
  updateRequirement,
  REQUIREMENT_STATUS_LABELS,
  type Requirement,
  type RequirementUpdate,
} from '../../api/requirement';
import { PRIORITY_LABELS } from '../../api/demand';
import { listSprints } from '../../api/sprint';
import { formatDate, formatDateTime } from '../../utils/formatDate';
import { RequirementEditDrawer } from './RequirementEditDrawer';
import { SprintListPanel } from './SprintListPanel';
import '../Pm/PmDetailPage.css';

type Tab = 'info' | 'sprints';

/**
 * v0.0.61 — Requirement 详情页 (/pm/requirements/:id). Replaces the prior "click 展开 in
 * RequirementsPage to expand SprintListPanel inline + click 编辑 to open drawer" UX. Detail page
 * shows hero + 基本信息 + Sprint 列表 in tabs; editing still uses the existing RequirementEditDrawer.
 */
export function RequirementDetailPage() {
  const { id: idParam } = useParams<{ id: string }>();
  const id = Number(idParam);
  const navigate = useNavigate();

  const [req, setReq] = useState<Requirement | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [tab, setTab] = useState<Tab>('info');
  const [sprintCount, setSprintCount] = useState(0);
  const [drawerOpen, setDrawerOpen] = useState(false);

  const load = useCallback(() => {
    if (!Number.isFinite(id)) {
      setError('无效的需求 ID');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    getRequirement(id)
      .then((r) => setReq(r))
      .catch(() => setError('未能加载该需求，可能已被删除或无权限'))
      .finally(() => setLoading(false));
    listSprints({ requirementId: id, size: 100 })
      .then((res) => setSprintCount(res.total))
      .catch(() => setSprintCount(0));
  }, [id]);

  useEffect(load, [load]);

  if (loading) return <div className="rainier-page">加载中…</div>;
  if (error || !req) {
    return (
      <div className="rainier-page">
        <div className="rainier-error-banner">{error ?? '需求不存在'}</div>
        <div>
          <Link to="/pm/requirements">← 返回需求列表</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="rainier-page pm-detail-page" data-testid="req-detail-page">
      <div className="pm-breadcrumb">
        <Link to="/pm/requirements">需求</Link>
        <span className="pm-breadcrumb-sep">/</span>
        <span className="pm-breadcrumb-current">{req.code}</span>
      </div>

      <div className="pm-detail-hero">
        <span className="pm-detail-hero-code">{req.code}</span>
        <span className="pm-detail-hero-title">{req.title}</span>
        <StatusChip
          status={req.status}
          label={REQUIREMENT_STATUS_LABELS[req.status] ?? req.status}
        />
        <StatusChip
          status={req.priority}
          label={`优先级·${PRIORITY_LABELS[req.priority] ?? req.priority}`}
          tier={
            req.priority === 'URGENT' || req.priority === 'HIGH'
              ? 'red'
              : req.priority === 'MEDIUM'
                ? 'yellow'
                : 'gray'
          }
        />
        <span className="pm-detail-hero-actions">
          <Button type="button" variant="secondary" onClick={() => setDrawerOpen(true)} data-testid="req-detail-edit">
            编辑
          </Button>
        </span>
      </div>

      <div className="pm-tabs" data-testid="req-detail-tabs">
        <button
          type="button"
          className="pm-tab"
          data-active={tab === 'info'}
          onClick={() => setTab('info')}
          data-testid="req-tab-info"
        >
          基本信息
        </button>
        <button
          type="button"
          className="pm-tab"
          data-active={tab === 'sprints'}
          onClick={() => setTab('sprints')}
          data-testid="req-tab-sprints"
        >
          Sprint 列表
          <span className="pm-tab-count">· {sprintCount}</span>
        </button>
      </div>

      {tab === 'info' && (
        <div className="pm-tab-pane">
          <div className="pm-info-grid">
            <span className="pm-info-label">标题</span>
            <span className="pm-info-value">{req.title}</span>

            <span className="pm-info-label">负责人</span>
            <span className="pm-info-value">
              <OwnerChip name={req.ownerName} loginName={req.ownerLoginName} />
            </span>

            <span className="pm-info-label">项目</span>
            <span className="pm-info-value">
              {req.projectName ? `${req.projectCode ?? ''} ${req.projectName}`.trim() : '—'}
            </span>

            <span className="pm-info-label">优先级</span>
            <span className="pm-info-value">{PRIORITY_LABELS[req.priority] ?? req.priority}</span>

            <span className="pm-info-label">复杂度</span>
            <span className="pm-info-value">{req.complexity ?? '—'}</span>

            <span className="pm-info-label">期望日期</span>
            <span className="pm-info-value">{formatDate(req.expectedDate)}</span>

            <span className="pm-info-label">状态</span>
            <span className="pm-info-value">
              <StatusChip
                status={req.status}
                label={REQUIREMENT_STATUS_LABELS[req.status] ?? req.status}
              />
            </span>

            {req.opportunityId != null && (
              <>
                <span className="pm-info-label">来源商机</span>
                <span className="pm-info-value">
                  <Link to={`/crm/opportunities/${req.opportunityId}`} data-testid="req-detail-opp-link">
                    #{req.opportunityId} ↗
                  </Link>
                </span>
              </>
            )}

            {req.closeReason && (
              <>
                <span className="pm-info-label">关闭原因</span>
                <span className="pm-info-value">{req.closeReason}</span>
              </>
            )}

            <span className="pm-info-label">创建/更新</span>
            <span className="pm-info-value" style={{ color: 'var(--rainier-color-text-3)', fontSize: 12 }}>
              {formatDateTime(req.createTime)} · {formatDateTime(req.updateTime)}
            </span>
          </div>

          {req.description && (
            <div className="pm-info-section">
              <div className="pm-info-section-title">描述</div>
              <MarkdownView content={req.description} />
            </div>
          )}
        </div>
      )}

      {tab === 'sprints' && (
        <div className="pm-tab-pane">
          <SprintListPanel
            key={`sprint-${req.id}`}
            requirementId={req.id}
            requirementCode={req.code}
            requirementTitle={req.title}
            onCountChange={setSprintCount}
          />
        </div>
      )}

      <RequirementEditDrawer
        open={drawerOpen}
        editing={req}
        onClose={() => setDrawerOpen(false)}
        onCreate={async () => {
          // not used: detail page never opens drawer in "create" mode
        }}
        onUpdate={async (rid: number, body: RequirementUpdate) => {
          await updateRequirement(rid, body);
          setDrawerOpen(false);
          load();
        }}
      />

      {/* Defensive: if backend hard-deleted this requirement out from under us, surface link to list. */}
      <div>
        <Button
          type="button"
          variant="secondary"
          onClick={() => navigate('/pm/requirements')}
          data-testid="req-detail-back"
        >
          ← 返回需求列表
        </Button>
      </div>
    </div>
  );
}

