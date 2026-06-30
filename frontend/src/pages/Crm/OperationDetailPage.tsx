import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { StatusChip } from '../../components/board';
import { Button } from '../../components/ui/Button';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Input } from '../../components/ui/Input';
import { MarkdownView } from '../../components/ui/MarkdownView';
import { listUsers, type User } from '../../api/user';
import {
  getOperation,
  updateOperation,
  OPR_STAGE_LABELS,
  type Operation,
  type OperationStage,
  type OperationStatus,
} from '../../api/operation';
import {
  exportArtifactDocx,
  isLinkArtifact,
  listOpportunityArtifacts,
  type OpportunityArtifact,
} from '../../api/opportunityArtifact';
import { listDemands, type Demand } from '../../api/demand';
import { listRequirements, type Requirement } from '../../api/requirement';
import {
  convertOperationIssueToTask,
  createOperationIssue,
  deleteOperationIssue,
  ISSUE_SEVERITY_LABELS,
  ISSUE_STATUS_LABELS,
  issueStatusTier,
  listOperationIssues,
  updateOperationIssue,
  type IssueSeverity,
  type IssueStatus,
  type OperationIssue,
} from '../../api/operationIssue';
import { listProjects, type Project } from '../../api/project';
import { useAuthStore } from '../../store/auth';
import './OpportunityDetailPage.css';

const STAGE_TIER: Record<OperationStage, 'gray' | 'yellow' | 'green'> = {
  MAINTENANCE: 'yellow',
  OPERATING: 'green',
  REPURCHASE: 'gray',
};
const STATUS_LABEL: Record<OperationStatus, string> = { ACTIVE: '进行中', CLOSED: '已关闭' };
const STATUS_TIER: Record<OperationStatus, 'green' | 'gray'> = {
  ACTIVE: 'green',
  CLOSED: 'gray',
};


const SEVERITIES: IssueSeverity[] = ['HIGH', 'MEDIUM', 'LOW'];
const STATUSES: IssueStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

/**
 * v0.0.58 — 运营详情页（/crm/operations/:id）：
 *  - Hero：客户·标题 + 阶段/状态 chip + 编辑入口
 *  - 概览：来源商机/项目/负责人/客户/标题，可编辑
 *  - 流转产出物：按 opportunityId 复用 listOpportunityArtifacts，与 v0.0.55 卡片同样式
 *  - 诉求/需求：按 opportunityId 复用 listDemands/listRequirements
 *  - 问题清单：CRUD + 状态切换
 */
export default function OperationDetailPage() {
  const { id: idParam } = useParams<{ id: string }>();
  const id = Number(idParam);
  const navigate = useNavigate();
  const currentUserId = useAuthStore((s) => s.user?.id ?? null);

  const [op, setOp] = useState<Operation | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // edit form
  const [editing, setEditing] = useState(false);
  const [eCustomer, setECustomer] = useState('');
  const [eTitle, setETitle] = useState('');
  const [eOwnerId, setEOwnerId] = useState<number | ''>('');
  const [eError, setEError] = useState<string | null>(null);
  const [eSaving, setESaving] = useState(false);

  const [users, setUsers] = useState<User[]>([]);

  // linked content
  const [arts, setArts] = useState<OpportunityArtifact[]>([]);
  const [previewArtId, setPreviewArtId] = useState<number | null>(null);
  const [demands, setDemands] = useState<Demand[]>([]);
  const [requirements, setRequirements] = useState<Requirement[]>([]);

  // v0.0.58 — 来源信息（产出物/诉求需求）默认折叠，让问题清单成为页面主舞台。
  const [artsCollapsed, setArtsCollapsed] = useState(true);
  const [drCollapsed, setDrCollapsed] = useState(true);
  // v0.0.59 — 问题清单状态过滤：null=全部，否则按 status 过滤
  const [issueFilter, setIssueFilter] = useState<IssueStatus | null>(null);
  // v0.0.95 D7 — 客户端分页：默认 size=10，下一页按钮加载更多。
  const [issuePage, setIssuePage] = useState(0);
  const ISSUE_PAGE_SIZE = 10;

  // issues
  const [issues, setIssues] = useState<OperationIssue[]>([]);
  const [issueOpen, setIssueOpen] = useState(false);
  const [iTitle, setITitle] = useState('');
  const [iDesc, setIDesc] = useState('');
  const [iSeverity, setISeverity] = useState<IssueSeverity>('MEDIUM');
  const [iAssignee, setIAssignee] = useState<number | ''>('');
  const [iError, setIError] = useState<string | null>(null);
  const [iSaving, setISaving] = useState(false);
  // per-issue inline edit (status / assignee / closeReason)
  const [editIssueId, setEditIssueId] = useState<number | null>(null);
  const [iEditStatus, setIEditStatus] = useState<IssueStatus>('OPEN');
  const [iEditAssignee, setIEditAssignee] = useState<number | ''>('');
  const [iEditClose, setIEditClose] = useState('');
  const [convertIssueId, setConvertIssueId] = useState<number | null>(null);
  const [convertProjects, setConvertProjects] = useState<Project[]>([]);
  const [convertProjectId, setConvertProjectId] = useState<number | ''>('');
  const [convertLoading, setConvertLoading] = useState(false);
  const [convertSaving, setConvertSaving] = useState(false);
  const [convertError, setConvertError] = useState<string | null>(null);

  const prefill = (o: Operation) => {
    setECustomer(o.customerName ?? '');
    setETitle(o.title ?? '');
    setEOwnerId(o.opsOwnerUserId ?? '');
  };

  const loadIssues = useCallback(() => {
    listOperationIssues(id)
      .then(setIssues)
      .catch(() => setIssues([]));
  }, [id]);

  const loadLinked = useCallback(
    (opportunityId: number | null | undefined) => {
      if (opportunityId == null) {
        setArts([]);
        setDemands([]);
        setRequirements([]);
        return;
      }
      listOpportunityArtifacts(opportunityId)
        .then(setArts)
        .catch(() => setArts([]));
      listDemands({ opportunityId, size: 100 })
        .then((r) => setDemands(r.content))
        .catch(() => setDemands([]));
      listRequirements({ opportunityId, size: 100 })
        .then((r) => setRequirements(r.content))
        .catch(() => setRequirements([]));
    },
    [],
  );

  useEffect(() => {
    if (!Number.isFinite(id)) {
      setError('无效的运营 ID');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    setEditing(false);
    setIssueOpen(false);
    setConvertIssueId(null);
    setConvertProjectId('');
    setConvertError(null);
    getOperation(id)
      .then((o) => {
        setOp(o);
        prefill(o);
        loadLinked(o.opportunityId ?? null);
      })
      .catch(() => setError('未能加载该运营，可能已被删除或无权限'))
      .finally(() => setLoading(false));
    loadIssues();
    void listUsers({ size: 100 }).then((r) => setUsers(r.content));
  }, [id, loadIssues, loadLinked]);

  const saveEdit = async () => {
    if (op == null) return;
    if (!eCustomer.trim() || !eTitle.trim()) {
      setEError('请填写客户名称和运营单标题');
      return;
    }
    setEError(null);
    setESaving(true);
    try {
      const updated = await updateOperation(op.id, {
        customerName: eCustomer.trim(),
        title: eTitle.trim(),
        opsOwnerUserId: eOwnerId === '' ? null : eOwnerId,
        projectId: op.projectId ?? null,
      });
      setOp(updated);
      setEditing(false);
    } catch (e) {
      const err = e as { response?: { data?: { message?: string } }; message?: string };
      setEError(err?.response?.data?.message ?? err?.message ?? '保存失败，请重试');
    } finally {
      setESaving(false);
    }
  };

  const openIssue = () => {
    setITitle('');
    setIDesc('');
    setISeverity('MEDIUM');
    setIAssignee('');
    setIError(null);
    setIssueOpen(true);
  };

  const submitIssue = async () => {
    if (op == null) return;
    if (!iTitle.trim()) {
      setIError('请填写问题标题');
      return;
    }
    const reporter = currentUserId ?? op.opsOwnerUserId ?? null;
    if (reporter == null) {
      setIError('无法确定上报人：请先登录');
      return;
    }
    setIError(null);
    setISaving(true);
    try {
      await createOperationIssue(op.id, {
        title: iTitle.trim(),
        description: iDesc || undefined,
        severity: iSeverity,
        reporterUserId: reporter,
        assigneeUserId: iAssignee === '' ? undefined : iAssignee,
      });
      setIssueOpen(false);
      loadIssues();
    } catch (e) {
      const err = e as { response?: { data?: { message?: string } }; message?: string };
      setIError(err?.response?.data?.message ?? err?.message ?? '提交失败，请重试');
    } finally {
      setISaving(false);
    }
  };

  const startEditIssue = (issue: OperationIssue) => {
    setConvertIssueId(null);
    setEditIssueId(issue.id);
    setIEditStatus(issue.status);
    setIEditAssignee(issue.assigneeUserId ?? '');
    setIEditClose(issue.closeReason ?? '');
  };

  const saveEditIssue = async (issue: OperationIssue) => {
    try {
      await updateOperationIssue(issue.id, {
        title: issue.title,
        description: issue.description ?? undefined,
        severity: issue.severity,
        status: iEditStatus,
        assigneeUserId: iEditAssignee === '' ? null : iEditAssignee,
        closeReason: iEditClose || undefined,
      });
      setEditIssueId(null);
      loadIssues();
    } catch {
      // keep panel open on failure
    }
  };

  // v0.0.60 — replace window.confirm with state-managed ConfirmDialog (Feishu pattern, mirrors CustomerPage).
  const [confirmDeleteIssueId, setConfirmDeleteIssueId] = useState<number | null>(null);
  const removeIssue = (issueId: number) => setConfirmDeleteIssueId(issueId);

  const openConvertIssue = async (issue: OperationIssue) => {
    setEditIssueId(null);
    setConvertIssueId(issue.id);
    setConvertProjectId('');
    setConvertProjects([]);
    setConvertError(null);
    setConvertLoading(true);
    try {
      const page = await listProjects({ size: 200 });
      const rows = page.content ?? [];
      setConvertProjects(rows);
      const fallback = op?.projectId ?? null;
      const preferred = fallback != null && rows.some((p) => p.id === fallback) ? fallback : rows[0]?.id;
      setConvertProjectId(preferred ?? '');
      if (rows.length === 0) {
        setConvertError('暂无可选项目，请先创建或关联项目');
      }
    } catch (e) {
      const err = e as { response?: { data?: { message?: string } }; message?: string };
      setConvertError(err?.response?.data?.message ?? err?.message ?? '项目列表加载失败');
    } finally {
      setConvertLoading(false);
    }
  };

  const submitConvertIssue = async (issue: OperationIssue) => {
    const projectId = Number(convertProjectId);
    if (!Number.isFinite(projectId) || projectId <= 0) {
      setConvertError('请选择目标项目');
      return;
    }
    setConvertError(null);
    setConvertSaving(true);
    try {
      const task = await convertOperationIssueToTask(issue.id, projectId);
      window.alert(`已创建工单 ${task.code}（项目 ${task.projectId}）。`);
      setConvertIssueId(null);
      loadIssues();
    } catch (e) {
      const err = e as { response?: { data?: { message?: string } }; message?: string };
      setConvertError(err?.response?.data?.message ?? err?.message ?? '转工单失败');
    } finally {
      setConvertSaving(false);
    }
  };

  return (
    <div className="rainier-page" data-testid="op-detail-page">
      <div
        className="rainier-page-head"
        style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}
      >
        <Button
          type="button"
          variant="secondary"
          className="opp-detail-back"
          onClick={() => navigate(-1)}
          data-testid="op-detail-back"
        >
          ← 返回
        </Button>
        <h2>运营详情</h2>
        {op && (
          <>
            <span
              style={{
                fontSize: 14,
                color: 'var(--rainier-color-text-2)',
                paddingLeft: 8,
                borderLeft: '1px solid var(--rainier-border)',
              }}
            >
              {op.customerName} · {op.title}
            </span>
            <StatusChip
              status={op.stage}
              label={OPR_STAGE_LABELS[op.stage] ?? op.stage}
              tier={STAGE_TIER[op.stage]}
            />
            <StatusChip
              status={op.status}
              label={STATUS_LABEL[op.status] ?? op.status}
              tier={STATUS_TIER[op.status]}
            />
          </>
        )}
      </div>

      {loading ? (
        <div data-testid="op-detail-loading" className="opp-muted">
          加载中…
        </div>
      ) : error ? (
        <div data-testid="op-detail-error" className="opp-alert">
          {error}
        </div>
      ) : op ? (
        <div className="opp-detail-grid">
          {/* v0.0.59 — 紧凑概览条（取代原大卡）：一行 meta + 编辑按钮，编辑时下方展开表单 */}
          <div
            className="opp-card"
            style={{
              order: 2,
              padding: editing ? '14px 16px' : '10px 14px',
            }}
          >
            <div className="rainier-meta-row">
              <span data-testid="op-detail-meta-owner">
                运营负责人 <strong>{op.opsOwnerName || '未指定'}</strong>
              </span>
              <span className="rainier-meta-sep">·</span>
              <span>项目 <strong>{op.projectId != null ? `#${op.projectId}` : '—'}</strong></span>
              <span className="rainier-meta-sep">·</span>
              <span>
                来源商机{' '}
                {op.opportunityId != null ? (
                  <Link to={`/crm/opportunities/${op.opportunityId}`} data-testid="op-detail-opp-link">
                    #{op.opportunityId} ↗
                  </Link>
                ) : (
                  <strong>—</strong>
                )}
              </span>
              <span className="rainier-spacer" />
              {!editing && (
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => {
                    prefill(op);
                    setEError(null);
                    setEditing(true);
                  }}
                  data-testid="op-detail-edit"
                >
                  编辑
                </Button>
              )}
            </div>

            {editing && (
              <div style={{ marginTop: 14 }}>
                <Input
                  label="客户名称"
                  value={eCustomer}
                  onChange={(e) => setECustomer(e.target.value)}
                  data-testid="op-detail-customer"
                />
                <Input
                  label="运营单标题"
                  value={eTitle}
                  onChange={(e) => setETitle(e.target.value)}
                  data-testid="op-detail-title"
                />
                <div className="opp-form-block">
                  <label className="opp-form-label">运营负责人</label>
                  <select
                    className="rainier-form-select"
                    value={eOwnerId}
                    onChange={(e) =>
                      setEOwnerId(e.target.value === '' ? '' : Number(e.target.value))
                    }
                    data-testid="op-detail-owner"
                  >
                    <option value="">（未指定）</option>
                    {users.map((u) => (
                      <option key={u.id} value={u.id}>
                        {u.name}（{u.loginName}）
                      </option>
                    ))}
                  </select>
                </div>
                {eError && (
                  <div className="opp-alert" data-testid="op-detail-edit-error">
                    {eError}
                  </div>
                )}
                <div className="opp-actions-end">
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => {
                      prefill(op);
                      setEError(null);
                      setEditing(false);
                    }}
                  >
                    取消
                  </Button>
                  <Button
                    type="button"
                    disabled={eSaving}
                    onClick={() => void saveEdit()}
                    data-testid="op-detail-save"
                  >
                    保存修改
                  </Button>
                </div>
              </div>
            )}
          </div>

          {/* 流转产出物 — 默认折叠（来源商机参考资料），order 3 */}
          <div className="opp-card" style={{ order: 3 }}>
            <div className="opp-card-title">
              <span>流转产出物（来自来源商机）</span>
              <span className="opp-muted" style={{ marginLeft: 6, fontSize: 12 }}>
                {op.opportunityId == null ? '· 无来源商机' : `· ${arts.length} 项`}
              </span>
              <span className="opp-card-title-extra">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setArtsCollapsed((v) => !v)}
                  data-testid="op-detail-arts-toggle"
                >
                  {artsCollapsed ? '展开' : '收起'}
                </Button>
              </span>
            </div>
            {artsCollapsed ? null : op.opportunityId == null ? (
              <div className="opp-muted">无来源商机</div>
            ) : arts.length === 0 ? (
              <div data-testid="op-detail-no-arts" className="opp-muted">
                暂无产出物
              </div>
            ) : (
              arts.map((a) => (
                <div key={a.id} data-testid={`op-detail-artifact-${a.id}`} className="opp-art">
                  <div className="opp-art-head">
                    {a.typeLabel}
                    {a.title && a.title !== a.typeLabel ? ` · ${a.title}` : ''}
                  </div>
                  <div className="opp-art-meta">
                    {a.stageFrom ?? '—'}
                    {a.decision ? ` · ${a.decision}` : ''} · {a.author ?? '—'}
                  </div>
                  {isLinkArtifact(a.type) ? (
                    a.link ? (
                      <a href={a.link} target="_blank" rel="noreferrer">
                        打开链接 ↗
                      </a>
                    ) : null
                  ) : (
                    <>
                      <div className="opp-art-actions">
                        <Button
                          type="button"
                          variant="secondary"
                          onClick={() => setPreviewArtId(previewArtId === a.id ? null : a.id)}
                        >
                          {previewArtId === a.id ? '收起' : '预览'}
                        </Button>
                        <Button
                          type="button"
                          variant="secondary"
                          onClick={() =>
                            void exportArtifactDocx(
                              a.opportunityId,
                              a.id,
                              `${a.typeLabel}-${a.id}.docx`,
                            )
                          }
                        >
                          导出 Word
                        </Button>
                      </div>
                      {previewArtId === a.id ? (
                        <div className="opp-art-preview">
                          <MarkdownView content={a.content} />
                        </div>
                      ) : null}
                    </>
                  )}
                </div>
              ))
            )}
          </div>

          {/* 诉求 / 需求 — 默认折叠（来源商机的产品诉求阶段沉淀），order 4 */}
          <div className="opp-card" style={{ order: 4 }}>
            <div className="opp-card-title">
              <span>来自商机的 诉求 / 需求</span>
              <span className="opp-muted" style={{ marginLeft: 6, fontSize: 12 }}>
                {op.opportunityId == null
                  ? '· 无来源商机'
                  : `· 诉求 ${demands.length} / 需求 ${requirements.length}`}
              </span>
              <span className="opp-card-title-extra">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setDrCollapsed((v) => !v)}
                  data-testid="op-detail-dr-toggle"
                >
                  {drCollapsed ? '展开' : '收起'}
                </Button>
              </span>
            </div>
            {drCollapsed ? null : op.opportunityId == null ? (
              <div className="opp-muted">无来源商机</div>
            ) : demands.length === 0 && requirements.length === 0 ? (
              <div className="opp-muted">暂无</div>
            ) : (
              <div data-testid="op-detail-dr-list">
                {demands.map((d) => (
                  <div
                    key={`d-${d.id}`}
                    className="opp-art"
                    data-testid={`op-detail-demand-${d.id}`}
                    role="link"
                    tabIndex={0}
                    style={{ cursor: 'pointer' }}
                    onClick={() => navigate(`/pm/demands?openId=${d.id}`)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') navigate(`/pm/demands?openId=${d.id}`);
                    }}
                    title="打开诉求"
                  >
                    <div className="opp-art-head">
                      <StatusChip status="DEMAND" label="诉求" tier="yellow" /> {d.title}{' '}
                      <span style={{ color: 'var(--rainier-color-text-3)', fontWeight: 400 }}>↗</span>
                    </div>
                    <div className="opp-art-meta">{d.status}</div>
                  </div>
                ))}
                {requirements.map((r) => (
                  <div
                    key={`r-${r.id}`}
                    className="opp-art"
                    data-testid={`op-detail-requirement-${r.id}`}
                    role="link"
                    tabIndex={0}
                    style={{ cursor: 'pointer' }}
                    onClick={() => navigate(`/pm/requirements?openId=${r.id}`)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') navigate(`/pm/requirements?openId=${r.id}`);
                    }}
                    title="打开需求"
                  >
                    <div className="opp-art-head">
                      <StatusChip status="REQ" label="需求" tier="green" /> {r.code} · {r.title}{' '}
                      <span style={{ color: 'var(--rainier-color-text-3)', fontWeight: 400 }}>↗</span>
                    </div>
                    <div className="opp-art-meta">{r.status}</div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* 问题清单 — 主操作区，order 1（置顶）。带 OPEN/IN_PROGRESS 计数 + 状态过滤 chips。 */}
          <div className="opp-card" data-testid="op-detail-issues" style={{ order: 1 }}>
            <div className="opp-card-title">
              <span>运营问题清单</span>
              <span
                className="opp-muted"
                style={{ marginLeft: 6, fontSize: 12 }}
                data-testid="op-issue-counts"
              >
                · 共 {issues.length}
              </span>
              {!issueOpen && (
                <span className="opp-card-title-extra">
                  <Button type="button" variant="secondary" onClick={openIssue} data-testid="op-issue-add">
                    + 新建问题
                  </Button>
                </span>
              )}
            </div>

            {/* v0.0.59/60 — 状态过滤 chips（点击切换；OPEN/IN_PROGRESS 计数高亮）—— 用全局 .rainier-filter-bar/.rainier-filter-chip */}
            <div className="rainier-filter-bar" data-testid="op-issue-filter-chips">
              {([
                { key: null, label: '全部', count: issues.length },
                { key: 'OPEN', label: '待处理', count: issues.filter((i) => i.status === 'OPEN').length },
                { key: 'IN_PROGRESS', label: '处理中', count: issues.filter((i) => i.status === 'IN_PROGRESS').length },
                { key: 'RESOLVED', label: '已解决', count: issues.filter((i) => i.status === 'RESOLVED').length },
                { key: 'CLOSED', label: '已关闭', count: issues.filter((i) => i.status === 'CLOSED').length },
              ] as { key: IssueStatus | null; label: string; count: number }[]).map((c) => (
                <button
                  key={c.key ?? 'all'}
                  type="button"
                  className="rainier-filter-chip"
                  data-active={issueFilter === c.key}
                  onClick={() => setIssueFilter(c.key)}
                  data-testid={`op-issue-filter-${c.key ?? 'all'}`}
                >
                  {c.label} · {c.count}
                </button>
              ))}
            </div>

            {issueOpen && (
              <div data-testid="op-issue-form" className="opp-add-form">
                <Input
                  label="标题"
                  value={iTitle}
                  onChange={(e) => setITitle(e.target.value)}
                  data-testid="op-issue-title"
                />
                <div className="opp-form-block">
                  <label className="opp-form-label">描述（可空，支持 Markdown）</label>
                  <textarea
                    className="rainier-input opp-textarea"
                    value={iDesc}
                    onChange={(e) => setIDesc(e.target.value)}
                    data-testid="op-issue-desc"
                  />
                </div>
                <div className="opp-form-block">
                  <label className="opp-form-label">严重度</label>
                  <select
                    className="rainier-form-select"
                    value={iSeverity}
                    onChange={(e) => setISeverity(e.target.value as IssueSeverity)}
                    data-testid="op-issue-severity"
                  >
                    {SEVERITIES.map((s) => (
                      <option key={s} value={s}>
                        {ISSUE_SEVERITY_LABELS[s]}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="opp-form-block">
                  <label className="opp-form-label">负责人（可空）</label>
                  <select
                    className="rainier-form-select"
                    value={iAssignee}
                    onChange={(e) =>
                      setIAssignee(e.target.value === '' ? '' : Number(e.target.value))
                    }
                    data-testid="op-issue-assignee"
                  >
                    <option value="">（未指派）</option>
                    {users.map((u) => (
                      <option key={u.id} value={u.id}>
                        {u.name}（{u.loginName}）
                      </option>
                    ))}
                  </select>
                </div>
                {iError && (
                  <div className="opp-alert" data-testid="op-issue-error">
                    {iError}
                  </div>
                )}
                <div className="opp-actions-end">
                  <Button type="button" variant="secondary" onClick={() => setIssueOpen(false)}>
                    取消
                  </Button>
                  <Button
                    type="button"
                    disabled={iSaving}
                    onClick={() => void submitIssue()}
                    data-testid="op-issue-save"
                  >
                    提交问题
                  </Button>
                </div>
              </div>
            )}

            {issues.length === 0 ? (
              <div data-testid="op-issue-empty" className="opp-muted">
                暂无问题
              </div>
            ) : (
              // 排序：活跃工作（OPEN→IN_PROGRESS）置顶，再 RESOLVED，再 CLOSED；同状态按 id DESC（新→旧，由后端保证）。
              // v0.0.59 过滤后若空，提示当前状态分组无内容。
              (() => {
                const filtered = [...issues]
                  .filter((i) => issueFilter === null || i.status === issueFilter)
                  .sort((a, b) => {
                    const ord: Record<IssueStatus, number> = {
                      OPEN: 0,
                      IN_PROGRESS: 1,
                      RESOLVED: 2,
                      CLOSED: 3,
                      CONVERTED: 4,
                    };
                    return ord[a.status] - ord[b.status];
                  });
                if (filtered.length === 0) {
                  return (
                    <div className="opp-muted" data-testid="op-issue-filter-empty">
                      当前筛选下无问题
                    </div>
                  );
                }
                // v0.0.95 D7 — 客户端分页
                const totalPages = Math.max(1, Math.ceil(filtered.length / ISSUE_PAGE_SIZE));
                const pageSafe = Math.min(issuePage, totalPages - 1);
                const start = pageSafe * ISSUE_PAGE_SIZE;
                const pageItems = filtered.slice(start, start + ISSUE_PAGE_SIZE);
                return (
                  <>
                    {pageItems.map((issue) => (
                      <div
                        key={issue.id}
                        data-testid={`op-issue-${issue.id}`}
                        className="op-issue"
                        data-sev={issue.severity}
                      >
                        <div className="op-issue-main">
                          <StatusChip
                            status={issue.status}
                            label={ISSUE_STATUS_LABELS[issue.status]}
                            tier={issueStatusTier(issue.status)}
                          />
                          <StatusChip
                            status={issue.severity}
                            label={ISSUE_SEVERITY_LABELS[issue.severity]}
                            tier={issue.severity === 'HIGH' ? 'red' : issue.severity === 'MEDIUM' ? 'yellow' : 'gray'}
                          />
                          <span className="op-issue-title">{issue.title}</span>
                          <span className="op-issue-meta">
                            <span>上报 {issue.reporterName ?? `#${issue.reporterUserId}`}</span>
                            {issue.assigneeName && (
                              <>
                                <span className="op-issue-meta-sep">·</span>
                                <span>负责 {issue.assigneeName}</span>
                              </>
                            )}
                          </span>
                          {editIssueId !== issue.id && (
                            <span className="op-issue-actions">
                              <Button
                                type="button"
                                variant="secondary"
                                onClick={() => startEditIssue(issue)}
                                data-testid={`op-issue-edit-${issue.id}`}
                              >
                                编辑
                              </Button>
                              {issue.status !== 'CONVERTED' && (
                                <Button
                                  type="button"
                                  variant="secondary"
                                  onClick={() => void openConvertIssue(issue)}
                                  data-testid={`op-issue-convert-${issue.id}`}
                                >
                                  转工单
                                </Button>
                              )}
                              <Button
                                type="button"
                                variant="secondary"
                                onClick={() => void removeIssue(issue.id)}
                                data-testid={`op-issue-delete-${issue.id}`}
                              >
                                删除
                              </Button>
                            </span>
                          )}
                        </div>
                        {issue.description ? (
                          <div className="op-issue-desc">
                            <MarkdownView content={issue.description} />
                          </div>
                        ) : null}
                        {convertIssueId === issue.id && (
                          <div
                            className="op-issue-edit"
                            data-testid={`op-issue-convert-panel-${issue.id}`}
                          >
                            <div className="opp-form-block">
                              <label className="opp-form-label">目标项目</label>
                              <select
                                className="rainier-form-select"
                                value={convertProjectId}
                                disabled={convertLoading || convertProjects.length === 0}
                                onChange={(e) =>
                                  setConvertProjectId(e.target.value === '' ? '' : Number(e.target.value))
                                }
                                data-testid={`op-issue-convert-project-${issue.id}`}
                              >
                                <option value="">（请选择）</option>
                                {convertProjects.map((p) => (
                                  <option key={p.id} value={p.id}>
                                    {p.code} · {p.name}
                                  </option>
                                ))}
                              </select>
                            </div>
                            {convertError && (
                              <div className="opp-alert" data-testid={`op-issue-convert-error-${issue.id}`}>
                                {convertError}
                              </div>
                            )}
                            <div className="opp-actions-end">
                              <Button
                                type="button"
                                variant="secondary"
                                onClick={() => {
                                  setConvertIssueId(null);
                                  setConvertError(null);
                                }}
                              >
                                取消
                              </Button>
                              <Button
                                type="button"
                                disabled={convertLoading || convertSaving || convertProjectId === ''}
                                onClick={() => void submitConvertIssue(issue)}
                                data-testid={`op-issue-convert-submit-${issue.id}`}
                              >
                                {convertSaving ? '转换中…' : '确认转工单'}
                              </Button>
                            </div>
                          </div>
                        )}
                        {editIssueId === issue.id && (
                          <div className="op-issue-edit">
                            <div className="opp-form-block">
                              <label className="opp-form-label">状态</label>
                              <select
                                className="rainier-form-select"
                                value={iEditStatus}
                                onChange={(e) => setIEditStatus(e.target.value as IssueStatus)}
                                data-testid={`op-issue-edit-status-${issue.id}`}
                              >
                                {STATUSES.map((s) => (
                                  <option key={s} value={s}>
                                    {ISSUE_STATUS_LABELS[s]}
                                  </option>
                                ))}
                              </select>
                            </div>
                            <div className="opp-form-block">
                              <label className="opp-form-label">负责人</label>
                              <select
                                className="rainier-form-select"
                                value={iEditAssignee}
                                onChange={(e) =>
                                  setIEditAssignee(e.target.value === '' ? '' : Number(e.target.value))
                                }
                              >
                                <option value="">（未指派）</option>
                                {users.map((u) => (
                                  <option key={u.id} value={u.id}>
                                    {u.name}（{u.loginName}）
                                  </option>
                                ))}
                              </select>
                            </div>
                            {(iEditStatus === 'CLOSED' || iEditStatus === 'RESOLVED') && (
                              <Input
                                label="关闭/解决原因（可空）"
                                value={iEditClose}
                                onChange={(e) => setIEditClose(e.target.value)}
                                data-testid={`op-issue-edit-close-${issue.id}`}
                              />
                            )}
                            <div className="opp-actions-end">
                              <Button type="button" variant="secondary" onClick={() => setEditIssueId(null)}>
                                取消
                              </Button>
                              <Button
                                type="button"
                                onClick={() => void saveEditIssue(issue)}
                                data-testid={`op-issue-edit-save-${issue.id}`}
                              >
                                保存
                              </Button>
                            </div>
                          </div>
                        )}
                      </div>
                    ))}
                    {totalPages > 1 && (
                      <div className="rainier-filter-bar" data-testid="op-issue-pagination" style={{ marginTop: 8 }}>
                        <button
                          type="button"
                          className="rainier-filter-chip"
                          disabled={pageSafe <= 0}
                          onClick={() => setIssuePage((p) => Math.max(0, p - 1))}
                          data-testid="op-issue-page-prev"
                        >
                          上一页
                        </button>
                        <span className="opp-muted" style={{ alignSelf: 'center', fontSize: 12 }}>
                          第 {pageSafe + 1} / {totalPages} 页 · 共 {filtered.length}
                        </span>
                        <button
                          type="button"
                          className="rainier-filter-chip"
                          disabled={pageSafe >= totalPages - 1}
                          onClick={() => setIssuePage((p) => Math.min(totalPages - 1, p + 1))}
                          data-testid="op-issue-page-next"
                        >
                          下一页
                        </button>
                      </div>
                    )}
                  </>
                );
              })()
            )}
          </div>
        </div>
      ) : null}

      <ConfirmDialog
        open={confirmDeleteIssueId !== null}
        title="删除问题"
        message="确认删除该问题？删除后不可恢复。"
        confirmText="删除"
        onCancel={() => setConfirmDeleteIssueId(null)}
        onConfirm={async () => {
          const target = confirmDeleteIssueId;
          setConfirmDeleteIssueId(null);
          if (target != null) {
            await deleteOperationIssue(target);
            loadIssues();
          }
        }}
      />
    </div>
  );
}
