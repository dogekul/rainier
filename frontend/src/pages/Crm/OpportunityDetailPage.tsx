import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { StatusChip } from '../../components/board';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { MarkdownView } from '../../components/ui/MarkdownView';
import { listUsers, type User } from '../../api/user';
import { listProducts, type Product } from '../../api/product';
import { listCustomers, type Customer } from '../../api/customer';
import {
  getOpportunity,
  updateOpportunity,
  OPP_STAGE_LABELS,
  type Opportunity,
} from '../../api/opportunity';
import {
  ADDABLE_ARTIFACT_TYPES,
  ARTIFACT_TYPE_LABELS,
  createOpportunityArtifact,
  exportArtifactDocx,
  isLinkArtifact,
  listOpportunityArtifacts,
  type ArtifactType,
  type OpportunityArtifact,
} from '../../api/opportunityArtifact';
import './OpportunityDetailPage.css';

const STATUS_LABEL: Record<string, string> = { OPEN: '进行中', WON: '赢单', LOST: '丢单' };
const STATUS_TIER: Record<string, 'green' | 'red' | 'yellow'> = {
  WON: 'green',
  LOST: 'red',
  OPEN: 'yellow',
};

// avatar 色板（按客户名哈希取色，避开红色以免误读为告警）—— 与客户页同语言。
const AVATAR_PALETTE: Array<{ bg: string; fg: string }> = [
  { bg: 'var(--rainier-bg-selected)', fg: 'var(--rainier-color-primary)' },
  { bg: 'var(--rainier-status-green-bg)', fg: 'var(--rainier-status-green)' },
  { bg: 'var(--rainier-status-yellow-bg)', fg: 'var(--rainier-status-yellow)' },
  { bg: 'var(--rainier-status-gray-bg)', fg: 'var(--rainier-color-text-2)' },
];
function avatarColor(seed: string) {
  let h = 0;
  for (let i = 0; i < seed.length; i++) h = (h * 31 + seed.charCodeAt(i)) >>> 0;
  return AVATAR_PALETTE[h % AVATAR_PALETTE.length];
}

/**
 * v0.0.55 — 统一商机详情页（路由 /crm/opportunities/:id）。取代售前/实施流转各自的详情抽屉：
 * 全宽承载 概览(查看/编辑) + 流转产出物(预览/导出/链接/添加)，可深链/刷新/后退。
 * 推进/门禁仍在两个流转列表页的行上，不在此页。
 */
export default function OpportunityDetailPage() {
  const { id: idParam } = useParams<{ id: string }>();
  const id = Number(idParam);
  const navigate = useNavigate();

  const [opp, setOpp] = useState<Opportunity | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [arts, setArts] = useState<OpportunityArtifact[]>([]);
  const [artsLoading, setArtsLoading] = useState(false);

  // dropdown sources for the edit form
  const [users, setUsers] = useState<User[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);

  // edit-form state
  const [editing, setEditing] = useState(false);
  const [dCustomer, setDCustomer] = useState('');
  const [dTitle, setDTitle] = useState('');
  const [dNote, setDNote] = useState('');
  const [dAmount, setDAmount] = useState('');
  const [dProduct, setDProduct] = useState<number | ''>('');
  const [dCommercial, setDCommercial] = useState<number | ''>('');
  const [dSolution, setDSolution] = useState<number | ''>('');
  const [dPm, setDPm] = useState<number | ''>('');
  const [dOps, setDOps] = useState<number | ''>('');
  const [dError, setDError] = useState<string | null>(null);
  const [dSaving, setDSaving] = useState(false);

  // 添加产出物 form
  const [previewArtId, setPreviewArtId] = useState<number | null>(null);
  const [addArtOpen, setAddArtOpen] = useState(false);
  const [aType, setAType] = useState<ArtifactType>('SURVEY_REPORT');
  const [aTitle, setATitle] = useState('');
  const [aContent, setAContent] = useState('');
  const [aLink, setALink] = useState('');
  const [aError, setAError] = useState<string | null>(null);
  const [aSaving, setASaving] = useState(false);

  const prefill = (o: Opportunity) => {
    setDCustomer(o.customerName ?? '');
    setDTitle(o.title ?? '');
    setDNote(o.note ?? '');
    setDAmount(o.amount != null ? String(o.amount) : '');
    setDProduct(o.productId ?? '');
    setDCommercial(o.commercialOwnerUserId ?? '');
    setDSolution(o.solutionOwnerUserId ?? '');
    setDPm(o.pmUserId ?? '');
    setDOps(o.opsOwnerUserId ?? '');
  };

  const loadArts = useCallback(() => {
    setArtsLoading(true);
    listOpportunityArtifacts(id)
      .then(setArts)
      .catch(() => setArts([]))
      .finally(() => setArtsLoading(false));
  }, [id]);

  useEffect(() => {
    if (!Number.isFinite(id)) {
      setError('无效的商机 ID');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    setEditing(false);
    setAddArtOpen(false);
    setPreviewArtId(null);
    getOpportunity(id)
      .then((o) => {
        setOpp(o);
        prefill(o);
      })
      .catch(() => setError('未能加载该商机，可能已被删除或无权限'))
      .finally(() => setLoading(false));
    loadArts();
    void listUsers({ size: 100 }).then((r) => setUsers(r.content));
    void listProducts({ size: 100 }).then((r) => setProducts(r.content));
    void listCustomers({ size: 100 }).then((r) => setCustomers(r.content));
  }, [id, loadArts]);

  const saveDetail = async () => {
    if (opp == null) return;
    if (!dCustomer.trim() || !dTitle.trim()) {
      setDError('请填写客户名称和商机标题');
      return;
    }
    if (dAmount.trim() && Number.isNaN(Number(dAmount))) {
      setDError('金额必须是数字');
      return;
    }
    setDError(null);
    setDSaving(true);
    try {
      const matched = customers.find(
        (c) => c.name.trim().toLowerCase() === dCustomer.trim().toLowerCase(),
      );
      const updated = await updateOpportunity(opp.id, {
        customerName: dCustomer.trim(),
        customerId: matched ? matched.id : undefined,
        title: dTitle.trim(),
        note: dNote.trim() || undefined,
        amount: dAmount.trim() ? Number(dAmount) : undefined,
        commercialOwnerUserId: dCommercial === '' ? undefined : dCommercial,
        solutionOwnerUserId: dSolution === '' ? undefined : dSolution,
        pmUserId: dPm === '' ? undefined : dPm,
        opsOwnerUserId: dOps === '' ? undefined : dOps,
        productId: dProduct === '' ? undefined : dProduct,
      });
      setOpp(updated);
      setEditing(false);
    } catch (e) {
      const err = e as { response?: { data?: { message?: string } }; message?: string };
      setDError(err?.response?.data?.message ?? err?.message ?? '保存失败，请重试');
    } finally {
      setDSaving(false);
    }
  };

  const openAddArtifact = () => {
    setAType('SURVEY_REPORT');
    setATitle('');
    setAContent('');
    setALink('');
    setAError(null);
    setAddArtOpen(true);
  };

  const submitAddArtifact = async () => {
    if (opp == null) return;
    const link = isLinkArtifact(aType);
    if (link && !aLink.trim()) {
      setAError('请填写链接');
      return;
    }
    if (!link && !aContent.trim()) {
      setAError('请填写正文');
      return;
    }
    setAError(null);
    setASaving(true);
    try {
      await createOpportunityArtifact(opp.id, {
        type: aType,
        title: link ? undefined : aTitle.trim() || undefined,
        content: link ? undefined : aContent,
        link: link ? aLink.trim() : undefined,
      });
      setAddArtOpen(false);
      loadArts();
    } catch (e) {
      const err = e as { response?: { data?: { message?: string } }; message?: string };
      setAError(err?.response?.data?.message ?? err?.message ?? '添加失败，请重试');
    } finally {
      setASaving(false);
    }
  };

  const ownerSelect = (
    label: string,
    value: number | '',
    setter: (v: number | '') => void,
    testId: string,
  ) => (
    <div className="opp-form-block">
      <label className="opp-form-label">{label}</label>
      <select
        className="rainier-treeselect-trigger"
        value={value}
        onChange={(e) => setter(e.target.value === '' ? '' : Number(e.target.value))}
        data-testid={testId}
      >
        <option value="">（未指定）</option>
        {users.map((u) => (
          <option key={u.id} value={u.id}>
            {u.name}（{u.loginName}）
          </option>
        ))}
      </select>
    </div>
  );

  const field = (label: string, value: string) => (
    <div className="opp-field">
      <span className="opp-field-label">{label}</span>
      <span className="opp-field-value">{value}</span>
    </div>
  );

  return (
    <div className="rainier-page" data-testid="opp-detail-page">
      <div className="rainier-page-head">
        <Button
          type="button"
          variant="secondary"
          className="opp-detail-back"
          onClick={() => navigate(-1)}
          data-testid="opp-detail-back"
        >
          ← 返回
        </Button>
        <h2 style={{ margin: 0 }}>商机详情</h2>
      </div>

      {loading ? (
        <div data-testid="opp-detail-loading" className="opp-muted">
          加载中…
        </div>
      ) : error ? (
        <div data-testid="opp-detail-error" className="opp-alert">
          {error}
        </div>
      ) : opp ? (
        <div className="opp-detail-grid">
          {/* 概览 */}
          <div className="opp-card">
            <div className="opp-hero">
              <div
                className="opp-hero-avatar"
                style={{
                  background: avatarColor(opp.customerName ?? '').bg,
                  color: avatarColor(opp.customerName ?? '').fg,
                }}
              >
                {(opp.customerName ?? '商').slice(0, 1)}
              </div>
              <div className="opp-hero-main">
                <div className="opp-hero-customer">{opp.customerName}</div>
                <div className="opp-hero-title">{opp.title}</div>
                <div className="opp-hero-chips">
                  <StatusChip status={opp.stage} label={OPP_STAGE_LABELS[opp.stage] ?? opp.stage} />
                  <StatusChip
                    status={opp.status}
                    label={STATUS_LABEL[opp.status] ?? opp.status}
                    tier={STATUS_TIER[opp.status] ?? 'gray'}
                  />
                  {opp.projectId != null ? (
                    <span className="opp-muted">关联项目 #{opp.projectId}</span>
                  ) : null}
                </div>
              </div>
              {!editing && (
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => {
                    prefill(opp);
                    setDError(null);
                    setEditing(true);
                  }}
                  data-testid="opp-detail-edit"
                >
                  编辑
                </Button>
              )}
            </div>

            {editing ? (
              <div style={{ marginTop: 14 }}>
                <Input
                  label="客户名称（选已有或填新建）"
                  value={dCustomer}
                  onChange={(e) => setDCustomer(e.target.value)}
                  list="opp-detail-customer-options"
                  data-testid="opp-detail-customer"
                />
                <datalist id="opp-detail-customer-options">
                  {customers.map((c) => (
                    <option key={c.id} value={c.name} />
                  ))}
                </datalist>
                <Input
                  label="商机标题"
                  value={dTitle}
                  onChange={(e) => setDTitle(e.target.value)}
                  data-testid="opp-detail-title"
                />
                <div className="opp-form-block">
                  <label className="opp-form-label">备注（可空）</label>
                  <textarea
                    className="rainier-input opp-textarea"
                    value={dNote}
                    onChange={(e) => setDNote(e.target.value)}
                    data-testid="opp-detail-note"
                  />
                </div>
                <div className="opp-form-block">
                  <label className="opp-form-label">产品（可空）</label>
                  <select
                    className="rainier-treeselect-trigger"
                    value={dProduct}
                    onChange={(e) => setDProduct(e.target.value === '' ? '' : Number(e.target.value))}
                    data-testid="opp-detail-product"
                  >
                    <option value="">（未选择）</option>
                    {products.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                      </option>
                    ))}
                  </select>
                </div>
                {ownerSelect('商务负责人', dCommercial, setDCommercial, 'opp-detail-owner-commercial')}
                {ownerSelect('解决方案负责人', dSolution, setDSolution, 'opp-detail-owner-solution')}
                {ownerSelect('项目经理', dPm, setDPm, 'opp-detail-owner-pm')}
                {ownerSelect('运营经理', dOps, setDOps, 'opp-detail-owner-ops')}
                <Input
                  label="金额（元，可空）"
                  value={dAmount}
                  onChange={(e) => setDAmount(e.target.value)}
                  data-testid="opp-detail-amount"
                />
                {dError && (
                  <div className="opp-alert" data-testid="opp-detail-edit-error">
                    {dError}
                  </div>
                )}
                <div className="opp-actions-end">
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => {
                      prefill(opp);
                      setDError(null);
                      setEditing(false);
                    }}
                    data-testid="opp-detail-cancel"
                  >
                    取消
                  </Button>
                  <Button
                    type="button"
                    disabled={dSaving}
                    onClick={() => void saveDetail()}
                    data-testid="opp-detail-save"
                  >
                    保存修改
                  </Button>
                </div>
              </div>
            ) : (
              <div className="opp-fields" style={{ marginTop: 14 }}>
                {field('备注', opp.note || '—')}
                {field('金额', opp.amount != null ? `¥${opp.amount}` : '—')}
                {field('产品', opp.productName || '—')}
                {field('商务', opp.commercialOwnerName || '—')}
                {field('解决方案', opp.solutionOwnerName || '—')}
                {field('项目经理', opp.pmName || '—')}
                {field('运营', opp.opsOwnerName || '—')}
                {field('最近决策人', opp.gateDecidedBy || '—')}
              </div>
            )}
          </div>

          {/* 流转产出物 */}
          <div className="opp-card">
            <div className="opp-card-title">
              流转产出物
              {!addArtOpen && (
                <span className="opp-card-title-extra">
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={openAddArtifact}
                    data-testid="opp-detail-add-artifact"
                  >
                    添加产出物
                  </Button>
                </span>
              )}
            </div>
            {addArtOpen && (
              <div data-testid="opp-detail-add-form" className="opp-add-form">
                <div className="opp-form-block">
                  <label className="opp-form-label">类型</label>
                  <select
                    className="rainier-treeselect-trigger"
                    value={aType}
                    onChange={(e) => setAType(e.target.value as ArtifactType)}
                    data-testid="opp-detail-add-type"
                  >
                    {ADDABLE_ARTIFACT_TYPES.map((t) => (
                      <option key={t} value={t}>
                        {ARTIFACT_TYPE_LABELS[t]}
                      </option>
                    ))}
                  </select>
                </div>
                {isLinkArtifact(aType) ? (
                  <Input
                    label="链接 URL"
                    value={aLink}
                    onChange={(e) => setALink(e.target.value)}
                    data-testid="opp-detail-add-link"
                  />
                ) : (
                  <>
                    <Input
                      label="标题（可空）"
                      value={aTitle}
                      onChange={(e) => setATitle(e.target.value)}
                      data-testid="opp-detail-add-title"
                    />
                    <div className="opp-form-block">
                      <label className="opp-form-label">正文（支持 Markdown）</label>
                      <textarea
                        className="rainier-input opp-textarea"
                        value={aContent}
                        onChange={(e) => setAContent(e.target.value)}
                        data-testid="opp-detail-add-content"
                      />
                    </div>
                  </>
                )}
                {aError && (
                  <div className="opp-alert" data-testid="opp-detail-add-error">
                    {aError}
                  </div>
                )}
                <div className="opp-actions-end">
                  <Button type="button" variant="secondary" onClick={() => setAddArtOpen(false)}>
                    取消
                  </Button>
                  <Button
                    type="button"
                    disabled={aSaving}
                    onClick={() => void submitAddArtifact()}
                    data-testid="opp-detail-add-save"
                  >
                    保存
                  </Button>
                </div>
              </div>
            )}
            {artsLoading ? (
              <div className="opp-muted">加载中…</div>
            ) : arts.length === 0 ? (
              <div data-testid="opp-detail-no-arts" className="opp-muted">
                暂无产出物
              </div>
            ) : (
              arts.map((a) => (
                <div key={a.id} data-testid={`opp-detail-artifact-${a.id}`} className="opp-art">
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
                      <a
                        href={a.link}
                        target="_blank"
                        rel="noreferrer"
                        data-testid={`opp-detail-link-${a.id}`}
                      >
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
                          data-testid={`opp-detail-preview-${a.id}`}
                        >
                          {previewArtId === a.id ? '收起' : '预览'}
                        </Button>
                        <Button
                          type="button"
                          variant="secondary"
                          onClick={() =>
                            void exportArtifactDocx(a.opportunityId, a.id, `${a.typeLabel}-${a.id}.docx`)
                          }
                          data-testid={`opp-detail-export-${a.id}`}
                        >
                          导出 Word
                        </Button>
                      </div>
                      {previewArtId === a.id ? (
                        <div className="opp-art-preview">
                          <MarkdownView content={a.content} testId={`opp-detail-md-${a.id}`} />
                        </div>
                      ) : null}
                    </>
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      ) : null}
    </div>
  );
}
