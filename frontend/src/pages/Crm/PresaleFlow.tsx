import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './oppFlow.css';
import { DashboardCard, EmptyState, StatTiles, StatusChip } from '../../components/board';
import { Button } from '../../components/ui/Button';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { listUsers, type User } from '../../api/user';
import { listProducts, type Product } from '../../api/product';
import { listCustomers, type Customer } from '../../api/customer';
import {
  ARTIFACT_TYPE_LABELS,
  STAGE_REQUIRED_ARTIFACTS,
  createOpportunityArtifact,
  isLinkArtifact,
  listOpportunityArtifacts,
  type ArtifactType,
} from '../../api/opportunityArtifact';
import {
  advanceOpportunity,
  createOpportunity,
  listOpportunities,
  OPP_GATE_STAGES,
  OPP_PRESALE_STAGES,
  OPP_STAGE_LABELS,
  OPP_TRANSITION_ARTIFACT,
  type Opportunity,
} from '../../api/opportunity';

const PRESALE_SET = new Set<string>(OPP_PRESALE_STAGES);

/**
 * v0.0.44 —「售前流转」(pre-sales operations). The actionable surface for 售前 opportunities (status=OPEN,
 * stage ∈ 线索..合同签订): 新建商机 + 逐节点推进 + 关口决策(通过/否决). v0.0.45: transitions in
 * OPP_TRANSITION_ARTIFACT (线索推进→《商机调研报告》; 商机决策→《决策评审纪要》) open a 产出物 form whose submit
 * does「建产出物+流转」atomically; 否决(丢单, 无产出物要求的关口) still confirms first. 只读总览在「商机看板」。
 */
export function PresaleFlow() {
  const navigate = useNavigate();
  const [rows, setRows] = useState<Opportunity[]>([]);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [advError, setAdvError] = useState<string | null>(null);
  // 推进时补充产出物 (multi-doc stages, e.g. POC)
  const [suppOpp, setSuppOpp] = useState<Opportunity | null>(null);
  const [suppDecision, setSuppDecision] = useState<'PASS' | 'REJECT' | undefined>(undefined);
  const [suppTypes, setSuppTypes] = useState<ArtifactType[]>([]);
  // per-type: report kinds use title+content; link kinds use a list of links (可添加多份, 无需标题)
  const [suppData, setSuppData] = useState<
    Record<string, { title: string; content: string; links: string[] }>
  >({});
  const [suppError, setSuppError] = useState<string | null>(null);
  const [suppSaving, setSuppSaving] = useState(false);
  // 否决 → 丢单 confirm (for gates WITHOUT an artifact requirement; artifact-gated 否决 confirms via its form)
  const [rejectId, setRejectId] = useState<number | null>(null);
  // 产出物 form (报告/纪要) — set when a transition requires an artifact
  const [artifactReq, setArtifactReq] = useState<{
    id: number;
    decision?: 'PASS' | 'REJECT';
    label: string;
  } | null>(null);
  const [artTitle, setArtTitle] = useState('');
  const [artContent, setArtContent] = useState('');
  const [artError, setArtError] = useState<string | null>(null);
  const [artSaving, setArtSaving] = useState(false);

  // create-drawer state
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [users, setUsers] = useState<User[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [customerName, setCustomerName] = useState('');
  const [title, setTitle] = useState('');
  const [note, setNote] = useState('');
  const [amount, setAmount] = useState('');
  const [commercial, setCommercial] = useState<number | ''>('');
  const [solution, setSolution] = useState<number | ''>('');
  const [pm, setPm] = useState<number | ''>('');
  const [ops, setOps] = useState<number | ''>('');
  const [product, setProduct] = useState<number | ''>('');
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(() => {
    return listOpportunities({ size: 100 })
      .then((r) => setRows(r.content))
      .catch(() => setRows([]));
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!drawerOpen) {
      setFormError(null);
      return;
    }
    setCustomerName('');
    setTitle('');
    setNote('');
    setAmount('');
    setCommercial('');
    setSolution('');
    setPm('');
    setOps('');
    setProduct('');
    void listUsers({ size: 100 }).then((r) => setUsers(r.content));
    void listProducts({ size: 100 }).then((r) => setProducts(r.content));
    void listCustomers({ size: 100 }).then((r) => setCustomers(r.content));
  }, [drawerOpen]);

  // 售前在办 = OPEN 且 stage ∈ 售前环节。
  const items = rows.filter((r) => r.status === 'OPEN' && PRESALE_SET.has(r.stage));
  const openAmount = items.reduce((sum, r) => sum + (r.amount ?? 0), 0);

  const save = async () => {
    if (!customerName.trim() || !title.trim()) {
      setFormError('请填写客户名称和商机标题');
      return;
    }
    if (amount.trim() && Number.isNaN(Number(amount))) {
      setFormError('金额必须是数字');
      return;
    }
    setFormError(null);
    setSaving(true);
    try {
      // resolve an existing customer by name (combobox); else backend creates one from the typed name.
      const matched = customers.find(
        (c) => c.name.trim().toLowerCase() === customerName.trim().toLowerCase(),
      );
      await createOpportunity({
        customerName: customerName.trim(),
        customerId: matched ? matched.id : undefined,
        title: title.trim(),
        note: note.trim() || undefined,
        amount: amount.trim() ? Number(amount) : undefined,
        commercialOwnerUserId: commercial === '' ? undefined : commercial,
        solutionOwnerUserId: solution === '' ? undefined : solution,
        pmUserId: pm === '' ? undefined : pm,
        opsOwnerUserId: ops === '' ? undefined : ops,
        productId: product === '' ? undefined : product,
      });
      setDrawerOpen(false);
      await load();
    } finally {
      setSaving(false);
    }
  };

  const advance = async (
    id: number,
    decision?: 'PASS' | 'REJECT',
    artifact?: { title: string; content: string },
  ) => {
    setBusyId(id);
    setAdvError(null);
    try {
      await advanceOpportunity(id, decision, undefined, artifact);
      await load();
    } catch (e) {
      const err = e as { response?: { data?: { message?: string } }; message?: string };
      setAdvError(err?.response?.data?.message ?? err?.message ?? '推进失败');
    } finally {
      setBusyId(null);
    }
  };

  // Route an action: single-doc gate → inline 产出物 form; multi-doc gate (POC) → 补充 missing docs then
  // advance; destructive 否决 → confirm; else advance.
  const requestAdvance = async (r: Opportunity, decision?: 'PASS' | 'REJECT') => {
    setAdvError(null);
    const meta = OPP_TRANSITION_ARTIFACT[r.stage];
    if (meta) {
      setArtTitle('');
      setArtContent('');
      setArtError(null);
      setArtifactReq({ id: r.id, decision, label: meta.label });
      return;
    }
    // 多产出物门禁仅前进(PASS/非关口)强制；关口否决(REJECT)跳过补充表单直接确认丢单（镜像后端 PASS-only）。
    const required = STAGE_REQUIRED_ARTIFACTS[r.stage];
    if (required && required.length && decision !== 'REJECT') {
      let have = new Set<string>();
      try {
        have = new Set((await listOpportunityArtifacts(r.id)).map((a) => a.type));
      } catch {
        // if the lookup fails, fall through to advance and let the backend gate decide
      }
      const missing = required.filter((t) => !have.has(t));
      if (missing.length === 0) {
        void advance(r.id, decision);
        return;
      }
      const data: Record<string, { title: string; content: string; links: string[] }> = {};
      missing.forEach((t) => {
        data[t] = { title: '', content: '', links: isLinkArtifact(t) ? [''] : [] };
      });
      setSuppData(data);
      setSuppTypes(missing);
      setSuppDecision(decision);
      setSuppError(null);
      setSuppOpp(r);
      return;
    }
    if (decision === 'REJECT') {
      setRejectId(r.id);
      return;
    }
    void advance(r.id, decision);
  };

  const setSuppField = (type: string, field: 'title' | 'content', value: string) =>
    setSuppData((d) => ({ ...d, [type]: { ...d[type], [field]: value } }));
  const setSuppLink = (type: string, idx: number, value: string) =>
    setSuppData((d) => {
      const links = [...d[type].links];
      links[idx] = value;
      return { ...d, [type]: { ...d[type], links } };
    });
  const addSuppLink = (type: string) =>
    setSuppData((d) => ({ ...d, [type]: { ...d[type], links: [...d[type].links, ''] } }));
  const removeSuppLink = (type: string, idx: number) =>
    setSuppData((d) => {
      const links = d[type].links.filter((_, i) => i !== idx);
      return { ...d, [type]: { ...d[type], links: links.length ? links : [''] } };
    });

  // 推进时补充：为缺失的必需产出物逐个创建（链接类可多份、无标题），然后推进。
  const submitSupplement = async () => {
    if (suppOpp == null) return;
    for (const t of suppTypes) {
      const d = suppData[t];
      if (isLinkArtifact(t)) {
        if (!d.links.some((l) => l.trim())) {
          setSuppError(`请为《${ARTIFACT_TYPE_LABELS[t]}》填写至少一条链接`);
          return;
        }
      } else if (!d.content.trim()) {
        setSuppError(`请填写《${ARTIFACT_TYPE_LABELS[t]}》的正文`);
        return;
      }
    }
    setSuppError(null);
    setSuppSaving(true);
    try {
      for (const t of suppTypes) {
        const d = suppData[t];
        if (isLinkArtifact(t)) {
          for (const l of d.links.map((x) => x.trim()).filter(Boolean)) {
            await createOpportunityArtifact(suppOpp.id, { type: t, link: l });
          }
        } else {
          await createOpportunityArtifact(suppOpp.id, {
            type: t,
            title: d.title.trim() || undefined,
            content: d.content,
          });
        }
      }
      const id = suppOpp.id;
      const dec = suppDecision;
      setSuppOpp(null);
      await advance(id, dec);
    } catch (e) {
      const err = e as { response?: { data?: { message?: string } }; message?: string };
      setSuppError(err?.response?.data?.message ?? err?.message ?? '提交失败');
    } finally {
      setSuppSaving(false);
    }
  };

  const submitArtifact = async () => {
    if (!artifactReq) return;
    if (!artTitle.trim() || !artContent.trim()) {
      setArtError('请填写标题和正文');
      return;
    }
    setArtError(null);
    setArtSaving(true);
    try {
      await advance(artifactReq.id, artifactReq.decision, {
        title: artTitle.trim(),
        content: artContent,
      });
      setArtifactReq(null);
    } finally {
      setArtSaving(false);
    }
  };

  const ownerSelect = (
    label: string,
    value: number | '',
    setter: (v: number | '') => void,
    testId: string,
  ) => (
    <div style={{ marginBottom: 12 }}>
      <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>{label}</label>
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

  const isReject = artifactReq?.decision === 'REJECT';


  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2 style={{ margin: 0 }}>售前流转</h2>
        <div style={{ flex: 1 }} />
        <Button type="button" onClick={() => setDrawerOpen(true)} data-testid="presale-new-btn">
          新建商机
        </Button>
      </div>

      <StatTiles
        testId="presale-summary"
        tiles={[
          { label: '售前在办', value: items.length, tier: items.length > 0 ? 'yellow' : 'gray' },
          { label: '在谈金额', value: openAmount },
        ]}
      />

      {advError && (
        <div
          style={{
            padding: '8px 12px',
            margin: '8px 0',
            color: 'var(--rainier-color-danger, #d4380d)',
            fontSize: 13,
            background: 'rgba(212, 56, 13, 0.08)',
            borderRadius: 4,
          }}
          data-testid="presale-adv-error"
        >
          {advError}（可在「详情 → 添加产出物」补齐后再推进）
        </div>
      )}

      {items.length === 0 ? (
        <EmptyState message="当前没有售前在办的商机。点「新建商机」创建线索。" testId="presale-empty" />
      ) : (
        <DashboardCard title="售前在办商机" testId="presale-list">
          <div className="oppflow-list">
            {items.map((r) => {
              const isGate = OPP_GATE_STAGES.includes(r.stage);
              const needsArtifact = !!OPP_TRANSITION_ARTIFACT[r.stage];
              return (
                <div key={r.id} className="oppflow-card" data-testid={`presale-row-${r.id}`}>
                  <div className="oppflow-stage">
                    <StatusChip
                      status={r.stage}
                      label={
                        OPP_STAGE_LABELS[r.stage] +
                        (isGate ? ' ⭐' : '') +
                        (needsArtifact ? ' 📄' : '')
                      }
                      testId={`presale-stage-${r.id}`}
                    />
                  </div>
                  <div className="oppflow-main">
                    <div className="oppflow-customer">{r.customerName}</div>
                    <div className="oppflow-title">{r.title}</div>
                  </div>
                  <div className="oppflow-meta">
                    <div className="oppflow-cell">
                      <span className="oppflow-cell-label">金额</span>
                      <span className={`oppflow-cell-value${r.amount == null ? ' muted' : ''}`}>
                        {r.amount != null ? `¥${r.amount}` : '—'}
                      </span>
                    </div>
                    <div className="oppflow-cell">
                      <span className="oppflow-cell-label">负责人</span>
                      <span className="oppflow-cell-value">
                        {r.commercialOwnerName ?? r.solutionOwnerName ?? '—'}
                      </span>
                    </div>
                  </div>
                  <div className="oppflow-actions">
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={() => navigate(`/crm/opportunities/${r.id}`)}
                      data-testid={`presale-detail-${r.id}`}
                    >
                      详情
                    </Button>
                    <div className="oppflow-actions-stage">
                      {isGate ? (
                        <>
                          <Button
                            type="button"
                            variant="primary"
                            disabled={busyId === r.id}
                            onClick={() => void requestAdvance(r, 'PASS')}
                            data-testid={`presale-pass-${r.id}`}
                          >
                            通过
                          </Button>
                          <Button
                            type="button"
                            variant="secondary"
                            disabled={busyId === r.id}
                            onClick={() => void requestAdvance(r, 'REJECT')}
                            data-testid={`presale-reject-${r.id}`}
                          >
                            否决
                          </Button>
                        </>
                      ) : (
                        <Button
                          type="button"
                          variant="primary"
                          disabled={busyId === r.id}
                          onClick={() => void requestAdvance(r)}
                          data-testid={`presale-advance-${r.id}`}
                        >
                          推进
                        </Button>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </DashboardCard>
      )}

      {/* 产出物表单（报告/纪要）— 提交即「建产出物 + 流转」 */}
      <Drawer
        open={artifactReq != null}
        title={artifactReq ? `《${artifactReq.label}》${isReject ? ' — 否决（丢单）' : ''}` : ''}
        onClose={() => setArtifactReq(null)}
      >
        <Input
          label="标题"
          value={artTitle}
          onChange={(e) => setArtTitle(e.target.value)}
          data-testid="presale-artifact-title"
        />
        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>正文</label>
          <textarea
            className="rainier-input"
            style={{ width: '100%', minHeight: 160, padding: 8, boxSizing: 'border-box' }}
            value={artContent}
            onChange={(e) => setArtContent(e.target.value)}
            data-testid="presale-artifact-content"
          />
        </div>
        {artError && (
          <div
            style={{
              padding: '6px 10px',
              marginBottom: 12,
              color: 'var(--rainier-color-danger, #d4380d)',
              fontSize: 12,
              background: 'rgba(212, 56, 13, 0.08)',
              borderRadius: 4,
            }}
            data-testid="presale-artifact-error"
          >
            {artError}
          </div>
        )}
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <Button type="button" variant="secondary" onClick={() => setArtifactReq(null)}>
            取消
          </Button>
          <Button
            type="button"
            variant={isReject ? 'secondary' : 'primary'}
            disabled={artSaving}
            onClick={() => void submitArtifact()}
            data-testid="presale-artifact-save"
          >
            {isReject ? '提交并否决' : '提交并推进'}
          </Button>
        </div>
      </Drawer>

      <Drawer open={drawerOpen} title="新建商机" onClose={() => setDrawerOpen(false)}>
        <Input
          label="客户名称（选已有或填新建）"
          value={customerName}
          onChange={(e) => setCustomerName(e.target.value)}
          list="presale-customer-options"
          data-testid="presale-new-customer"
        />
        <datalist id="presale-customer-options">
          {customers.map((c) => (
            <option key={c.id} value={c.name} />
          ))}
        </datalist>
        <Input
          label="商机标题"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          data-testid="presale-new-title"
        />
        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>备注（可空）</label>
          <textarea
            className="rainier-input"
            style={{ width: '100%', minHeight: 80, padding: 8, boxSizing: 'border-box' }}
            value={note}
            onChange={(e) => setNote(e.target.value)}
            data-testid="presale-new-note"
          />
        </div>
        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
            产品（可空，不确定先留空）
          </label>
          <select
            className="rainier-treeselect-trigger"
            value={product}
            onChange={(e) => setProduct(e.target.value === '' ? '' : Number(e.target.value))}
            data-testid="presale-new-product"
          >
            <option value="">（未选择）</option>
            {products.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </select>
        </div>
        {ownerSelect('商务负责人', commercial, setCommercial, 'presale-owner-commercial')}
        {ownerSelect('解决方案负责人', solution, setSolution, 'presale-owner-solution')}
        {ownerSelect('项目经理', pm, setPm, 'presale-owner-pm')}
        {ownerSelect('运营经理', ops, setOps, 'presale-owner-ops')}
        <Input
          label="金额（元，可空）"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          data-testid="presale-new-amount"
        />
        {formError && (
          <div
            style={{
              padding: '6px 10px',
              marginBottom: 12,
              color: 'var(--rainier-color-danger, #d4380d)',
              fontSize: 12,
              background: 'rgba(212, 56, 13, 0.08)',
              borderRadius: 4,
            }}
            data-testid="presale-form-error"
          >
            {formError}
          </div>
        )}
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <Button type="button" variant="secondary" onClick={() => setDrawerOpen(false)}>
            取消
          </Button>
          <Button
            type="button"
            disabled={saving}
            onClick={() => void save()}
            data-testid="presale-save-btn"
          >
            保存
          </Button>
        </div>
      </Drawer>

      {/* 推进时补充必需产出物（多产出物阶段，如 POC） */}
      <Drawer
        open={suppOpp != null}
        title="补充产出物并推进"
        onClose={() => setSuppOpp(null)}
      >
        <div style={{ fontSize: 12, color: 'var(--rainier-color-text-2)', marginBottom: 8 }}>
          推进前需补齐以下必需产出物：
        </div>
        {suppTypes.map((t) => (
          <div
            key={t}
            data-testid={`presale-supp-${t}`}
            style={{
              border: '1px solid var(--rainier-border)',
              borderRadius: 6,
              padding: '8px 10px',
              marginBottom: 8,
            }}
          >
            <div style={{ fontWeight: 600, fontSize: 13, marginBottom: 6 }}>
              {ARTIFACT_TYPE_LABELS[t]}
              {isLinkArtifact(t) ? '（链接，可添加多份）' : '（报告）'}
            </div>
            {isLinkArtifact(t) ? (
              <>
                {(suppData[t]?.links ?? ['']).map((l, idx) => (
                  <div key={idx} style={{ display: 'flex', gap: 6, marginBottom: 6 }}>
                    <input
                      className="rainier-input"
                      style={{ flex: 1 }}
                      placeholder="链接 URL"
                      value={l}
                      onChange={(e) => setSuppLink(t, idx, e.target.value)}
                      data-testid={`presale-supp-link-${t}-${idx}`}
                    />
                    {(suppData[t]?.links?.length ?? 0) > 1 && (
                      <Button
                        type="button"
                        variant="secondary"
                        onClick={() => removeSuppLink(t, idx)}
                        data-testid={`presale-supp-rmlink-${t}-${idx}`}
                      >
                        删除
                      </Button>
                    )}
                  </div>
                ))}
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => addSuppLink(t)}
                  data-testid={`presale-supp-addlink-${t}`}
                >
                  + 添加链接
                </Button>
              </>
            ) : (
              <>
                <Input
                  label="标题（可空）"
                  value={suppData[t]?.title ?? ''}
                  onChange={(e) => setSuppField(t, 'title', e.target.value)}
                  data-testid={`presale-supp-title-${t}`}
                />
                <div style={{ marginBottom: 8 }}>
                  <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
                    正文（支持 Markdown）
                  </label>
                  <textarea
                    className="rainier-input"
                    style={{ width: '100%', minHeight: 70, padding: 8, boxSizing: 'border-box' }}
                    value={suppData[t]?.content ?? ''}
                    onChange={(e) => setSuppField(t, 'content', e.target.value)}
                    data-testid={`presale-supp-content-${t}`}
                  />
                </div>
              </>
            )}
          </div>
        ))}
        {suppError && (
          <div
            style={{
              padding: '6px 10px',
              marginBottom: 8,
              color: 'var(--rainier-color-danger, #d4380d)',
              fontSize: 12,
              background: 'rgba(212, 56, 13, 0.08)',
              borderRadius: 4,
            }}
            data-testid="presale-supp-error"
          >
            {suppError}
          </div>
        )}
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <Button type="button" variant="secondary" onClick={() => setSuppOpp(null)}>
            取消
          </Button>
          <Button
            type="button"
            disabled={suppSaving}
            onClick={() => void submitSupplement()}
            data-testid="presale-supp-save"
          >
            提交并推进
          </Button>
        </div>
      </Drawer>

      <ConfirmDialog
        open={rejectId != null}
        title="确认否决（丢单）"
        message="否决后该商机将标记为「丢单」，不可恢复。确认否决？"
        confirmText="确认否决"
        onConfirm={() => {
          const id = rejectId;
          setRejectId(null);
          if (id != null) void advance(id, 'REJECT');
        }}
        onCancel={() => setRejectId(null)}
      />
    </div>
  );
}
