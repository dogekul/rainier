import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { StatusChip } from '../../components/board';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { MarkdownView } from '../../components/ui/MarkdownView';
import { listUsers, type User } from '../../api/user';
import { listProducts, type Product } from '../../api/product';
import { listCustomers, type Customer } from '../../api/customer';
import {
  advanceOpportunity,
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
import {
  createDemand,
  listDemands,
  PRIORITY_LABELS,
  type Demand,
  type Priority,
  type Source,
} from '../../api/demand';
import {
  createRequirement,
  listRequirements,
  type Complexity,
  type Requirement,
} from '../../api/requirement';
import { useAuthStore } from '../../store/auth';
import './OpportunityDetailPage.css';

type GenTarget = 'demand' | 'requirement';

const SOURCE_LABELS: Record<Source, string> = {
  WEB: '网页',
  WECHAT: '微信',
  EMAIL: '邮件',
  DINGTALK: '钉钉',
  OTHER: '其它',
};
const COMPLEXITIES: Complexity[] = ['XS', 'S', 'M', 'L', 'XL'];

/** v0.0.56 — 据现场调研产出物 + 产品信息客户端组合草稿（无 LLM）。 */
function composeDraft(
  opp: Opportunity,
  arts: OpportunityArtifact[],
): { title: string; description: string } {
  const reports = arts
    .filter((a) => a.type === 'SURVEY_REPORT' && a.content)
    .map((a) => (a.content ?? '').trim())
    .filter(Boolean);
  const links = arts
    .filter((a) => a.type === 'SURVEY_ATTACHMENT' && a.link)
    .map((a) => a.link as string);
  const lines = [
    `来源：商机 #${opp.id} · ${opp.customerName} · ${opp.title}`,
    `产品：${opp.productName || '—'}`,
    '',
    '【现场调研】',
    reports.length ? reports.join('\n\n') : '（暂无现场调研报告正文）',
  ];
  if (links.length) {
    lines.push('', '附件：', ...links.map((l) => `- ${l}`));
  }
  let description = lines.join('\n');
  // 诉求 description 后端上限 2000 —— 预填超长时截断，避免提交 400（用户可自行补全）。
  const MAX = 1900;
  if (description.length > MAX) {
    description = description.slice(0, MAX) + '\n…（调研内容较长已截断，可按需补全）';
  }
  return { title: `${opp.customerName} · ${opp.title}`, description };
}

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
  const [searchParams, setSearchParams] = useSearchParams();
  const currentUserId = useAuthStore((s) => s.user?.id ?? null);
  // v0.0.56 — 由「产品诉求 推进」跳转过来时（?action=convert），自动滚动到生成卡 + 展示补全提示横幅。
  const convertHint = searchParams.get('action') === 'convert';
  const genCardRef = useRef<HTMLDivElement | null>(null);
  // 自动推进失败时（如仅提交了诉求未提交需求，后端 400），在提示横幅区展示原因。
  const [convertAdvError, setConvertAdvError] = useState<string | null>(null);

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

  // v0.0.56 生成产品诉求/需求 草稿 + 已生成列表
  const [genOpen, setGenOpen] = useState(false);
  const [genTarget, setGenTarget] = useState<GenTarget>('demand');
  const [genTitle, setGenTitle] = useState('');
  const [genDesc, setGenDesc] = useState('');
  const [genPriority, setGenPriority] = useState<Priority>('MEDIUM');
  // 诉求专属：来源渠道。需求专属：负责人(PO) / 复杂度 / 期望交付日期（code 由后端自增生成）。
  const [genSource, setGenSource] = useState<Source>('WEB');
  const [genOwnerId, setGenOwnerId] = useState<number | ''>('');
  const [genComplexity, setGenComplexity] = useState<Complexity | ''>('');
  const [genExpectedDate, setGenExpectedDate] = useState('');
  const [genError, setGenError] = useState<string | null>(null);
  const [genSaving, setGenSaving] = useState(false);
  const [genDemands, setGenDemands] = useState<Demand[]>([]);
  const [genRequirements, setGenRequirements] = useState<Requirement[]>([]);

  const loadGenerated = useCallback(() => {
    listDemands({ opportunityId: id, size: 100 })
      .then((r) => setGenDemands(r.content))
      .catch(() => setGenDemands([]));
    listRequirements({ opportunityId: id, size: 100 })
      .then((r) => setGenRequirements(r.content))
      .catch(() => setGenRequirements([]));
  }, [id]);

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
    setGenOpen(false);
    loadArts();
    loadGenerated();
    void listUsers({ size: 100 }).then((r) => setUsers(r.content));
    void listProducts({ size: 100 }).then((r) => setProducts(r.content));
    void listCustomers({ size: 100 }).then((r) => setCustomers(r.content));
  }, [id, loadArts, loadGenerated]);

  // 由「推进」跳转过来 → 数据加载完后滚动到生成卡，并保留 ?action=convert 直到用户开始填写。
  useEffect(() => {
    if (convertHint && !loading && opp != null && genCardRef.current) {
      genCardRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [convertHint, loading, opp]);

  const dismissConvertHint = () => {
    const next = new URLSearchParams(searchParams);
    next.delete('action');
    setSearchParams(next, { replace: true });
  };

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

  // v0.0.56 据 现场调研 + 产品 客户端组合草稿（无 LLM），用户审改后提交为 诉求/需求。
  const openGenerate = () => {
    if (opp == null) return;
    const draft = composeDraft(opp, arts);
    setGenTitle(draft.title);
    setGenDesc(draft.description);
    setGenTarget('demand');
    setGenPriority('MEDIUM');
    setGenSource('WEB');
    setGenOwnerId(currentUserId ?? opp.pmUserId ?? '');
    setGenComplexity('');
    setGenExpectedDate('');
    setGenError(null);
    setGenOpen(true);
  };

  const submitGenerate = async () => {
    if (opp == null) return;
    if (!genTitle.trim()) {
      setGenError('请填写标题');
      return;
    }
    if (genTarget === 'requirement' && genOwnerId === '') {
      setGenError('请选择负责人(PO)');
      return;
    }
    setGenError(null);
    setGenSaving(true);
    try {
      if (genTarget === 'demand') {
        // 诉求：来源 + 提交人（自动当前用户）
        const submitter = currentUserId ?? opp.pmUserId ?? null;
        if (submitter == null) {
          setGenError('无法确定提交人：请先登录，或在商机指定项目经理');
          setGenSaving(false);
          return;
        }
        await createDemand({
          title: genTitle.trim(),
          description: genDesc,
          priority: genPriority,
          source: genSource,
          submitterUserId: submitter,
          opportunityId: id,
        });
      } else {
        // 需求：负责人(PO) + 复杂度 + 期望交付日期（code 由后端自增生成 REQ-{id}，前端不填）
        await createRequirement({
          title: genTitle.trim(),
          description: genDesc,
          priority: genPriority,
          complexity: genComplexity || undefined,
          expectedDate: genExpectedDate || undefined,
          ownerUserId: genOwnerId as number,
          opportunityId: id,
        });
      }
      setGenOpen(false);
      loadGenerated();
      // v0.0.56 — 据「推进」跳转过来 (?action=convert)：提交后自动推进到下一阶段，免去用户返回流转。
      // 成功 → 关掉提示横幅 + 刷新商机使阶段更新为 DELIVERY；
      // 失败（如仅提交诉求未提交需求，后端 400）→ 在提示横幅区域展示原因，banner 保留。
      if (convertHint) {
        setConvertAdvError(null);
        try {
          await advanceOpportunity(id);
          dismissConvertHint();
          const next = await getOpportunity(id);
          setOpp(next);
          prefill(next);
        } catch (advErr) {
          const ae = advErr as { response?: { data?: { message?: string } }; message?: string };
          setConvertAdvError(
            ae?.response?.data?.message ?? ae?.message ?? '推进失败，请检查后端校验',
          );
        }
      }
    } catch (e) {
      const err = e as { response?: { data?: { message?: string } }; message?: string };
      setGenError(err?.response?.data?.message ?? err?.message ?? '生成失败，请重试');
    } finally {
      setGenSaving(false);
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
        className="rainier-form-select"
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
        <h2>商机详情</h2>
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
          {/* 概览 — order 1 */}
          <div className="opp-card" style={{ order: 1 }}>
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
                    className="rainier-form-select"
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
              <>
                <hr className="opp-hero-sep" />
                <div className="opp-fields">
                  {field('金额', opp.amount != null ? `¥${opp.amount}` : '—')}
                  {field('产品', opp.productName || '—')}
                  {field('项目经理', opp.pmName || '—')}
                  {field('商务', opp.commercialOwnerName || '—')}
                  {field('解决方案', opp.solutionOwnerName || '—')}
                  {field('运营', opp.opsOwnerName || '—')}
                  {field('最近决策人', opp.gateDecidedBy || '—')}
                </div>
                {field('备注', opp.note || '—')}
              </>
            )}
          </div>

          {/* 流转产出物 — order 3（诉求/需求 之后） */}
          <div className="opp-card" style={{ order: 3 }}>
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
                    className="rainier-form-select"
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

          {/* 产品诉求 / 需求 — order 2（紧跟概览，在产出物之前） */}
          <div
            className="opp-card"
            data-testid="opp-gen-card"
            ref={genCardRef}
            style={{ order: 2 }}
          >
          {convertHint && (
            <div
              data-testid="opp-gen-convert-prompt"
              style={{
                padding: '10px 12px',
                marginBottom: 12,
                color: convertAdvError ? 'var(--rainier-status-red)' : 'var(--rainier-status-yellow)',
                background: convertAdvError
                  ? 'var(--rainier-status-red-bg)'
                  : 'var(--rainier-status-yellow-bg)',
                border: `1px solid ${
                  convertAdvError ? 'var(--rainier-status-red)' : 'var(--rainier-status-yellow)'
                }`,
                borderRadius: 'var(--rainier-radius-button)',
                fontSize: 13,
                display: 'flex',
                alignItems: 'center',
                gap: 8,
              }}
            >
              <span style={{ flex: 1 }} data-testid="opp-gen-convert-prompt-text">
                {convertAdvError ? (
                  <>
                    推进未成功：{convertAdvError}
                  </>
                ) : (
                  <>
                    请先据现场调研生成 <b>诉求</b> 或 <b>需求</b>，补齐信息后将自动推进到 交付实施。
                  </>
                )}
              </span>
              <Button type="button" variant="secondary" onClick={dismissConvertHint}>
                知道了
              </Button>
            </div>
          )}
          <div className="opp-card-title">
            产品诉求 / 需求
            {!genOpen && (
              <span className="opp-card-title-extra">
                <Button type="button" variant="secondary" onClick={openGenerate} data-testid="opp-gen-open">
                  据调研+产品生成草稿
                </Button>
              </span>
            )}
          </div>

          {genOpen && (
            <div data-testid="opp-gen-form" className="opp-add-form">
              <div className="opp-form-block">
                <label className="opp-form-label">提交为</label>
                <div style={{ display: 'flex', gap: 8 }}>
                  <button
                    type="button"
                    data-testid="opp-gen-target-demand"
                    onClick={() => setGenTarget('demand')}
                    style={genTabStyle(genTarget === 'demand')}
                  >
                    诉求 Demand
                  </button>
                  <button
                    type="button"
                    data-testid="opp-gen-target-requirement"
                    onClick={() => setGenTarget('requirement')}
                    style={genTabStyle(genTarget === 'requirement')}
                  >
                    需求 Requirement
                  </button>
                </div>
              </div>
              <Input
                label="标题"
                value={genTitle}
                onChange={(e) => setGenTitle(e.target.value)}
                data-testid="opp-gen-title"
              />
              <div className="opp-form-block">
                <label className="opp-form-label">描述（已据现场调研+产品预填，可编辑）</label>
                <textarea
                  className="rainier-input opp-textarea"
                  style={{ minHeight: 160 }}
                  value={genDesc}
                  onChange={(e) => setGenDesc(e.target.value)}
                  data-testid="opp-gen-desc"
                />
              </div>
              <div className="opp-form-block">
                <label className="opp-form-label">优先级</label>
                <select
                  className="rainier-form-select"
                  value={genPriority}
                  onChange={(e) => setGenPriority(e.target.value as Priority)}
                  data-testid="opp-gen-priority"
                >
                  {(Object.keys(PRIORITY_LABELS) as Priority[]).map((p) => (
                    <option key={p} value={p}>
                      {PRIORITY_LABELS[p]}
                    </option>
                  ))}
                </select>
              </div>
              {genTarget === 'demand' ? (
                <div className="opp-form-block">
                  <label className="opp-form-label">来源（渠道）</label>
                  <select
                    className="rainier-form-select"
                    value={genSource}
                    onChange={(e) => setGenSource(e.target.value as Source)}
                    data-testid="opp-gen-source"
                  >
                    {(Object.keys(SOURCE_LABELS) as Source[]).map((s) => (
                      <option key={s} value={s}>
                        {SOURCE_LABELS[s]}
                      </option>
                    ))}
                  </select>
                </div>
              ) : (
                <>
                  <div className="opp-form-block">
                    <label className="opp-form-label">负责人 (PO)</label>
                    <select
                      className="rainier-form-select"
                      value={genOwnerId}
                      onChange={(e) =>
                        setGenOwnerId(e.target.value === '' ? '' : Number(e.target.value))
                      }
                      data-testid="opp-gen-owner"
                    >
                      <option value="">（请选择）</option>
                      {users.map((u) => (
                        <option key={u.id} value={u.id}>
                          {u.name}（{u.loginName}）
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="opp-form-block">
                    <label className="opp-form-label">复杂度（可空）</label>
                    <select
                      className="rainier-form-select"
                      value={genComplexity}
                      onChange={(e) => setGenComplexity(e.target.value as Complexity | '')}
                      data-testid="opp-gen-complexity"
                    >
                      <option value="">（未评估）</option>
                      {COMPLEXITIES.map((c) => (
                        <option key={c} value={c}>
                          {c}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="opp-form-block">
                    <label className="opp-form-label">期望交付日期（可空）</label>
                    <input
                      className="rainier-input"
                      type="date"
                      style={{ width: '100%', boxSizing: 'border-box' }}
                      value={genExpectedDate}
                      onChange={(e) => setGenExpectedDate(e.target.value)}
                      data-testid="opp-gen-expected"
                    />
                  </div>
                </>
              )}
              {genError && (
                <div className="opp-alert" data-testid="opp-gen-error">
                  {genError}
                </div>
              )}
              <div className="opp-actions-end">
                <Button type="button" variant="secondary" onClick={() => setGenOpen(false)}>
                  取消
                </Button>
                <Button
                  type="button"
                  disabled={genSaving}
                  onClick={() => void submitGenerate()}
                  data-testid="opp-gen-save"
                >
                  提交{genTarget === 'demand' ? '诉求' : '需求'}
                </Button>
              </div>
            </div>
          )}

          <div data-testid="opp-gen-list">
            {genDemands.length === 0 && genRequirements.length === 0 ? (
              <div className="opp-muted">暂无生成的诉求/需求</div>
            ) : (
              <>
                {genDemands.map((d) => (
                  <div
                    key={`d-${d.id}`}
                    data-testid={`opp-gen-demand-${d.id}`}
                    className="opp-art"
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
                {genRequirements.map((r) => (
                  <div
                    key={`r-${r.id}`}
                    data-testid={`opp-gen-requirement-${r.id}`}
                    className="opp-art"
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
              </>
            )}
          </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function genTabStyle(active: boolean): React.CSSProperties {
  return {
    fontSize: 13,
    padding: '5px 14px',
    border: '1px solid var(--rainier-border)',
    borderRadius: 'var(--rainier-radius-button)',
    background: active ? 'var(--rainier-bg-selected)' : 'transparent',
    color: active ? 'var(--rainier-color-primary)' : 'var(--rainier-color-text-2)',
    cursor: 'pointer',
  };
}
