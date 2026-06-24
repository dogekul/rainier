import { useCallback, useEffect, useState } from 'react';
import { DashboardCard, EmptyState, StatTiles, StatusChip } from '../../components/board';
import { Button } from '../../components/ui/Button';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { MarkdownView } from '../../components/ui/MarkdownView';
import { listProjects, PROJECT_TYPE_LABELS, type Project } from '../../api/project';
import { listUsers, type User } from '../../api/user';
import { listProducts, type Product } from '../../api/product';
import { listCustomers, type Customer } from '../../api/customer';
import { useAuthStore } from '../../store/auth';
import {
  advanceOpportunity,
  initiateOpportunity,
  listOpportunities,
  updateOpportunity,
  OPP_DELIVERY_STAGES,
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
  STAGE_REQUIRED_ARTIFACTS,
  type ArtifactType,
  type OpportunityArtifact,
} from '../../api/opportunityArtifact';

const STATUS_LABEL: Record<string, string> = { OPEN: '进行中', WON: '赢单', LOST: '丢单' };

const DELIVERY_SET = new Set<string>(OPP_DELIVERY_STAGES);
const INITIATION = 'INITIATION';
const ACCEPTANCE = 'ACCEPTANCE';

type HandoffMode = 'link' | 'create';

/**
 * v0.0.44 —「实施流转」(delivery operations). v0.0.48 立项移交 改为「关联或新建」对外-交付项目：
 * 关联模式从 `EXTERNAL_DELIVERY` 项目下拉里选；新建模式填 名称 + 负责人（v0.0.49 编号自动生成、不再手填），类型固定对外-交付。
 * 默认「新建」模式（立项主流动作）。后端 initiate 原子接受二选一；无项目时不再死路。
 */
export function DeliveryFlow() {
  const [rows, setRows] = useState<Opportunity[]>([]);
  const [busyId, setBusyId] = useState<number | null>(null);
  const currentLoginName = useAuthStore((s) => s.user?.username ?? null);

  // 立项移交 drawer state
  const [handoffId, setHandoffId] = useState<number | null>(null);
  const [projects, setProjects] = useState<Project[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  // 立项的主流动作 = 为本次赢单新建一个交付项目（1:1），所以默认「新建」；「关联已有」是次要例外。
  // 不按项目数量切默认值 —— 真实使用中对外-交付项目会很多，按数量判断会让「新建」长期被埋。
  const [mode, setMode] = useState<HandoffMode>('create');
  const [projectId, setProjectId] = useState<number | ''>('');
  const [newName, setNewName] = useState('');
  const [newOwnerUserId, setNewOwnerUserId] = useState<number | ''>('');
  const [handoffSaving, setHandoffSaving] = useState(false);
  const [handoffError, setHandoffError] = useState<string | null>(null);

  const load = useCallback(() => {
    return listOpportunities({ size: 100 })
      .then((r) => setRows(r.content))
      .catch(() => setRows([]));
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (handoffId == null) {
      setProjectId('');
      setNewName('');
      setNewOwnerUserId('');
      setMode('create');
      setHandoffError(null);
      return;
    }
    void listProjects({ size: 100, projectType: 'EXTERNAL_DELIVERY' }).then((r) =>
      setProjects(r.content),
    );
    // 新建对外-交付项目需指定负责人；候选用户 + 默认当前登录用户（商机多无 PM）。
    void listUsers({ size: 100 }).then((r) => {
      setUsers(r.content);
      const me = r.content.find((u) => u.loginName === currentLoginName);
      const opp = rows.find((x) => x.id === handoffId);
      setNewOwnerUserId(opp?.pmUserId ?? (me ? me.id : ''));
    });
  }, [handoffId, currentLoginName, rows]);

  // 实施中 = WON 且 stage ∈ 实施环节。
  const items = rows.filter((r) => r.status === 'WON' && DELIVERY_SET.has(r.stage));
  const inProgress = items.filter((r) => r.stage !== ACCEPTANCE).length;
  const accepted = items.filter((r) => r.stage === ACCEPTANCE).length;

  // v0.0.53 现场调研推进补充表单 state（多产出物门禁，镜像 PresaleFlow）。
  const [suppOpp, setSuppOpp] = useState<Opportunity | null>(null);
  const [suppTypes, setSuppTypes] = useState<ArtifactType[]>([]);
  const [suppData, setSuppData] = useState<
    Record<string, { title: string; content: string; links: string[] }>
  >({});
  const [suppError, setSuppError] = useState<string | null>(null);
  const [suppSaving, setSuppSaving] = useState(false);
  const [advError, setAdvError] = useState<string | null>(null);

  // v0.0.54 商机详情抽屉（查看/编辑 + 流转产出物）。推进仍在行上，抽屉不含推进。
  const [detailOpp, setDetailOpp] = useState<Opportunity | null>(null);
  const [detailArts, setDetailArts] = useState<OpportunityArtifact[]>([]);
  const [detailArtsLoading, setDetailArtsLoading] = useState(false);
  const [detailEditing, setDetailEditing] = useState(false);
  const [previewArtId, setPreviewArtId] = useState<number | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  // 详情 edit-form（prefill 自 detailOpp）
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
  // 添加产出物 form（详情内）
  const [addArtOpen, setAddArtOpen] = useState(false);
  const [aType, setAType] = useState<ArtifactType>('SURVEY_REPORT');
  const [aTitle, setATitle] = useState('');
  const [aContent, setAContent] = useState('');
  const [aLink, setALink] = useState('');
  const [aError, setAError] = useState<string | null>(null);
  const [aSaving, setASaving] = useState(false);

  // v0.0.54 详情打开：复位编辑/添加态、prefill 编辑表单、拉产出物、懒加载下拉源（编辑用）。
  useEffect(() => {
    if (detailOpp == null) {
      setDetailArts([]);
      setDError(null);
      setDetailEditing(false);
      setPreviewArtId(null);
      setAddArtOpen(false);
      return;
    }
    setDetailEditing(false);
    setPreviewArtId(null);
    setAddArtOpen(false);
    setAError(null);
    setAType('SURVEY_REPORT');
    setATitle('');
    setAContent('');
    setALink('');
    prefillDetail(detailOpp);
    setDError(null);
    if (users.length === 0) void listUsers({ size: 100 }).then((r) => setUsers(r.content));
    if (products.length === 0) void listProducts({ size: 100 }).then((r) => setProducts(r.content));
    if (customers.length === 0) void listCustomers({ size: 100 }).then((r) => setCustomers(r.content));
    setDetailArtsLoading(true);
    listOpportunityArtifacts(detailOpp.id)
      .then(setDetailArts)
      .catch(() => setDetailArts([]))
      .finally(() => setDetailArtsLoading(false));
    // 仅在切换 detailOpp 时运行；下拉源为懒加载守卫，故意不入依赖。
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [detailOpp]);

  const advance = async (id: number, decision?: 'PASS' | 'REJECT') => {
    setBusyId(id);
    setAdvError(null);
    try {
      await advanceOpportunity(id, decision);
      await load();
    } catch (e) {
      // surface the backend's friendly 400 (e.g. 此转换需先提交产出物：《...》) instead of swallowing it
      const err = e as { response?: { data?: { message?: string } }; message?: string };
      setAdvError(err?.response?.data?.message ?? err?.message ?? '推进失败，请重试');
    } finally {
      setBusyId(null);
    }
  };

  // 实施环节「推进」路由：有产出物门禁(现场调研)且缺件 → 开补充表单；否则直接推进。
  const requestAdvance = async (r: Opportunity) => {
    if (busyId === r.id || suppOpp?.id === r.id) return; // in-flight / 表单已开 → 防重复触发
    const required = STAGE_REQUIRED_ARTIFACTS[r.stage];
    if (required && required.length) {
      setBusyId(r.id); // guard the lookup window against double-clicks
      let have = new Set<string>();
      try {
        have = new Set((await listOpportunityArtifacts(r.id)).map((a) => a.type));
      } catch {
        // lookup failed → fall through to advance and let the backend gate decide
      }
      const missing = required.filter((t) => !have.has(t));
      if (missing.length === 0) {
        setBusyId(null);
        void advance(r.id);
        return;
      }
      const data: Record<string, { title: string; content: string; links: string[] }> = {};
      missing.forEach((t) => {
        data[t] = { title: '', content: '', links: isLinkArtifact(t) ? [''] : [] };
      });
      setSuppData(data);
      setSuppTypes(missing);
      setSuppError(null);
      setBusyId(null); // 表单接管；按钮改由 suppOpp 禁用
      setSuppOpp(r);
      return;
    }
    void advance(r.id);
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

  // 推进时补充：为缺失的必需产出物逐个建档（链接类可多份、无标题），然后推进。
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
      setSuppOpp(null);
      await advance(id);
    } catch (e) {
      const err = e as { response?: { data?: { message?: string } }; message?: string };
      setSuppError(err?.response?.data?.message ?? err?.message ?? '提交失败');
    } finally {
      setSuppSaving(false);
    }
  };

  // ---- v0.0.54 详情：prefill / 保存编辑 / 添加产出物 ----
  const prefillDetail = (o: Opportunity) => {
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

  const saveDetail = async () => {
    if (detailOpp == null) return;
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
      const updated = await updateOpportunity(detailOpp.id, {
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
      setDetailOpp(updated);
      setDetailEditing(false);
      await load();
    } catch (e) {
      // surface backend 400 (e.g. 校验失败) instead of a silent no-op
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
    if (detailOpp == null) return;
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
      await createOpportunityArtifact(detailOpp.id, {
        type: aType,
        title: link ? undefined : aTitle.trim() || undefined,
        content: link ? undefined : aContent,
        link: link ? aLink.trim() : undefined,
      });
      setAddArtOpen(false);
      const arts = await listOpportunityArtifacts(detailOpp.id);
      setDetailArts(arts);
    } finally {
      setASaving(false);
    }
  };

  const doHandoff = async () => {
    if (handoffId == null) return;
    setHandoffError(null);
    if (mode === 'link') {
      if (projectId === '') {
        setHandoffError('请选择一个对外-交付项目');
        return;
      }
    } else {
      if (!newName.trim()) {
        setHandoffError('请填写新建项目的名称');
        return;
      }
      if (newOwnerUserId === '') {
        setHandoffError('请选择项目负责人');
        return;
      }
    }
    setHandoffSaving(true);
    try {
      const body =
        mode === 'link'
          ? { decision: 'PASS' as const, projectId: projectId as number }
          : {
              decision: 'PASS' as const,
              projectName: newName.trim(),
              projectOwnerUserId: newOwnerUserId as number,
            };
      await initiateOpportunity(handoffId, body);
      setHandoffId(null);
      await load();
    } catch (e) {
      // surface the backend's friendly 400 message (e.g. 项目编号已存在 / 须关联对外-交付项目), not axios's generic string
      const err = e as { response?: { data?: { message?: string } }; message?: string };
      setHandoffError(err?.response?.data?.message ?? err?.message ?? '立项失败，请检查项目信息后重试');
    } finally {
      setHandoffSaving(false);
    }
  };

  const noProjects = projects.length === 0;

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

  const detailRow = (label: string, value: string) => (
    <div style={{ display: 'flex', gap: 8, fontSize: 13, padding: '3px 0' }}>
      <span style={{ width: 88, color: 'var(--rainier-color-text-2)', flexShrink: 0 }}>{label}</span>
      <span style={{ whiteSpace: 'pre-wrap' }}>{value}</span>
    </div>
  );

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2 style={{ margin: 0 }}>实施流转</h2>
      </div>

      <StatTiles
        testId="delivery-summary"
        tiles={[
          { label: '实施中', value: inProgress, tier: inProgress > 0 ? 'yellow' : 'gray' },
          { label: '已验收', value: accepted, tier: accepted > 0 ? 'green' : 'gray' },
        ]}
      />

      {advError && (
        <div
          style={{
            padding: '6px 10px',
            marginBottom: 8,
            color: 'var(--rainier-color-danger, #d4380d)',
            fontSize: 12,
            background: 'rgba(212, 56, 13, 0.08)',
            borderRadius: 4,
          }}
          data-testid="delivery-adv-error"
        >
          {advError}
        </div>
      )}

      {items.length === 0 ? (
        <EmptyState
          message="当前没有进入实施的商机（合同签订赢单后进入立项）。"
          testId="delivery-empty"
        />
      ) : (
        <DashboardCard title="实施中商机" testId="delivery-list">
          <table className="rainier-list-table">
            <tbody>
              {items.map((r) => {
                const isInitiation = r.stage === INITIATION;
                const isTerminal = r.stage === ACCEPTANCE;
                return (
                  <tr key={r.id} data-testid={`delivery-row-${r.id}`}>
                    <td style={{ padding: '6px 8px', width: 130 }}>
                      <StatusChip
                        status={r.stage}
                        label={OPP_STAGE_LABELS[r.stage] + (isInitiation ? ' ⭐' : '')}
                        testId={`delivery-stage-${r.id}`}
                      />
                    </td>
                    <td style={{ padding: '6px 8px' }}>
                      <div style={{ fontWeight: 600 }}>{r.customerName}</div>
                      <div style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>{r.title}</div>
                    </td>
                    <td style={{ padding: '6px 8px', width: 110 }}>{r.pmName ?? '—'}</td>
                    <td style={{ padding: '6px 8px', width: 110 }}>
                      {r.projectId != null ? (
                        `#${r.projectId}`
                      ) : (
                        <span style={{ color: 'var(--rainier-color-text-2)' }}>未立项</span>
                      )}
                    </td>
                    <td style={{ padding: '6px 8px', width: 320, textAlign: 'right' }}>
                      <Button
                        type="button"
                        variant="secondary"
                        style={{ marginRight: 6 }}
                        onClick={() => setDetailOpp(r)}
                        data-testid={`delivery-detail-${r.id}`}
                      >
                        详情
                      </Button>
                      {isTerminal ? (
                        <span
                          style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}
                          data-testid={`delivery-done-${r.id}`}
                        >
                          已验收
                        </span>
                      ) : isInitiation ? (
                        // v0.0.52 立项移交（关联/新建项目）即完成立项并推进到现场调研；否决=驳回立项（停留）。
                        <>
                          <Button
                            type="button"
                            variant="primary"
                            disabled={busyId === r.id || handoffId === r.id}
                            onClick={() => setHandoffId(r.id)}
                            data-testid={`delivery-handoff-${r.id}`}
                          >
                            立项移交
                          </Button>
                          <Button
                            type="button"
                            variant="secondary"
                            style={{ marginLeft: 6 }}
                            disabled={busyId === r.id}
                            onClick={() => void advance(r.id, 'REJECT')}
                            data-testid={`delivery-reject-${r.id}`}
                          >
                            驳回
                          </Button>
                        </>
                      ) : (
                        <Button
                          type="button"
                          variant="primary"
                          disabled={busyId === r.id || suppOpp?.id === r.id}
                          onClick={() => void requestAdvance(r)}
                          data-testid={`delivery-advance-${r.id}`}
                        >
                          推进
                        </Button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </DashboardCard>
      )}

      <Drawer
        open={handoffId != null}
        title="立项移交 — 关联或新建对外-交付项目"
        onClose={() => setHandoffId(null)}
      >
        {/* 新建为主（默认），关联已有为次要例外 */}
        <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
          <button
            type="button"
            data-testid="delivery-mode-create"
            onClick={() => setMode('create')}
            style={modeTabStyle(mode === 'create')}
          >
            新建对外-交付
          </button>
          <button
            type="button"
            data-testid="delivery-mode-link"
            onClick={() => setMode('link')}
            style={modeTabStyle(mode === 'link')}
            disabled={noProjects}
            title={noProjects ? '暂无对外-交付项目可关联' : ''}
          >
            关联已有
          </button>
        </div>

        {mode === 'link' ? (
          <div style={{ marginBottom: 12 }}>
            <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
              交付项目（仅列对外-交付）
            </label>
            {noProjects ? (
              <div
                style={{ fontSize: 12, color: 'var(--rainier-color-text-2)', padding: '6px 0' }}
                data-testid="delivery-no-projects"
              >
                暂无对外-交付项目，切到「新建对外-交付」即可同时创建并移交。
              </div>
            ) : (
              <select
                className="rainier-treeselect-trigger"
                value={projectId}
                onChange={(e) => setProjectId(e.target.value === '' ? '' : Number(e.target.value))}
                data-testid="delivery-project-select"
              >
                <option value="">（选择项目）</option>
                {projects.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.code} {p.name}
                  </option>
                ))}
              </select>
            )}
          </div>
        ) : (
          <div style={{ marginBottom: 12 }}>
            <Input
              label="项目名称"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              data-testid="delivery-new-name"
            />
            <div style={{ marginBottom: 8 }}>
              <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>项目负责人</label>
              <select
                className="rainier-treeselect-trigger"
                value={newOwnerUserId}
                onChange={(e) => setNewOwnerUserId(e.target.value === '' ? '' : Number(e.target.value))}
                data-testid="delivery-new-owner"
              >
                <option value="">（选择负责人）</option>
                {users.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.name ?? u.loginName}
                  </option>
                ))}
              </select>
            </div>
            <div style={{ fontSize: 12, color: 'var(--rainier-color-text-2)', marginTop: 4 }}>
              类型：{PROJECT_TYPE_LABELS.EXTERNAL_DELIVERY}（固定）
            </div>
          </div>
        )}

        {handoffError && (
          <div
            style={{
              padding: '6px 10px',
              marginBottom: 8,
              color: 'var(--rainier-color-danger, #d4380d)',
              fontSize: 12,
              background: 'rgba(212, 56, 13, 0.08)',
              borderRadius: 4,
            }}
            data-testid="delivery-handoff-error"
          >
            {handoffError}
          </div>
        )}

        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <Button type="button" variant="secondary" onClick={() => setHandoffId(null)}>
            取消
          </Button>
          <Button
            type="button"
            disabled={handoffSaving}
            onClick={() => void doHandoff()}
            data-testid="delivery-handoff-save"
          >
            移交
          </Button>
        </div>
      </Drawer>

      {/* v0.0.53 现场调研推进时补充必需产出物（报告 + 附件） */}
      <Drawer
        open={suppOpp != null}
        title="补充产出物并推进"
        onClose={() => setSuppOpp(null)}
      >
        <div style={{ fontSize: 12, color: 'var(--rainier-color-text-2)', marginBottom: 8 }}>
          推进前需补齐以下现场调研产出物：
        </div>
        {suppTypes.map((t) => (
          <div
            key={t}
            data-testid={`delivery-supp-${t}`}
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
                      data-testid={`delivery-supp-link-${t}-${idx}`}
                    />
                    {(suppData[t]?.links?.length ?? 0) > 1 && (
                      <Button
                        type="button"
                        variant="secondary"
                        onClick={() => removeSuppLink(t, idx)}
                        data-testid={`delivery-supp-rmlink-${t}-${idx}`}
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
                  data-testid={`delivery-supp-addlink-${t}`}
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
                  data-testid={`delivery-supp-title-${t}`}
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
                    data-testid={`delivery-supp-content-${t}`}
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
            data-testid="delivery-supp-error"
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
            data-testid="delivery-supp-save"
          >
            提交并推进
          </Button>
        </div>
      </Drawer>

      {/* v0.0.54 商机详情（查看/编辑 + 流转产出物）。推进仍在行上，此处不含推进。 */}
      <Drawer open={detailOpp != null} title="商机详情" onClose={() => setDetailOpp(null)}>
        {detailOpp && (
          <div data-testid="delivery-detail-body">
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
              <div style={{ fontWeight: 700, fontSize: 15, flex: 1 }}>
                {detailOpp.customerName} · {detailOpp.title}
              </div>
              {!detailEditing && (
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => {
                    prefillDetail(detailOpp);
                    setDError(null);
                    setDetailEditing(true);
                  }}
                  data-testid="delivery-detail-edit"
                >
                  编辑
                </Button>
              )}
            </div>
            {detailRow('阶段', OPP_STAGE_LABELS[detailOpp.stage] ?? detailOpp.stage)}
            {detailRow('状态', STATUS_LABEL[detailOpp.status] ?? detailOpp.status)}
            {detailRow('项目', detailOpp.projectId != null ? `#${detailOpp.projectId}` : '未立项')}
            {detailOpp.gateDecidedBy ? detailRow('最近决策人', detailOpp.gateDecidedBy) : null}

            {detailEditing ? (
              <>
                <div style={{ fontWeight: 600, margin: '14px 0 6px' }}>编辑</div>
                <Input
                  label="客户名称（选已有或填新建）"
                  value={dCustomer}
                  onChange={(e) => setDCustomer(e.target.value)}
                  list="delivery-detail-customer-options"
                  data-testid="delivery-detail-customer"
                />
                <datalist id="delivery-detail-customer-options">
                  {customers.map((c) => (
                    <option key={c.id} value={c.name} />
                  ))}
                </datalist>
                <Input
                  label="商机标题"
                  value={dTitle}
                  onChange={(e) => setDTitle(e.target.value)}
                  data-testid="delivery-detail-title"
                />
                <div style={{ marginBottom: 12 }}>
                  <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
                    备注（可空）
                  </label>
                  <textarea
                    className="rainier-input"
                    style={{ width: '100%', minHeight: 70, padding: 8, boxSizing: 'border-box' }}
                    value={dNote}
                    onChange={(e) => setDNote(e.target.value)}
                    data-testid="delivery-detail-note"
                  />
                </div>
                <div style={{ marginBottom: 12 }}>
                  <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
                    产品（可空）
                  </label>
                  <select
                    className="rainier-treeselect-trigger"
                    value={dProduct}
                    onChange={(e) => setDProduct(e.target.value === '' ? '' : Number(e.target.value))}
                    data-testid="delivery-detail-product"
                  >
                    <option value="">（未选择）</option>
                    {products.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                      </option>
                    ))}
                  </select>
                </div>
                {ownerSelect('商务负责人', dCommercial, setDCommercial, 'delivery-detail-owner-commercial')}
                {ownerSelect('解决方案负责人', dSolution, setDSolution, 'delivery-detail-owner-solution')}
                {ownerSelect('项目经理', dPm, setDPm, 'delivery-detail-owner-pm')}
                {ownerSelect('运营经理', dOps, setDOps, 'delivery-detail-owner-ops')}
                <Input
                  label="金额（元，可空）"
                  value={dAmount}
                  onChange={(e) => setDAmount(e.target.value)}
                  data-testid="delivery-detail-amount"
                />
                {dError && (
                  <div
                    style={{
                      padding: '6px 10px',
                      marginBottom: 12,
                      color: 'var(--rainier-color-danger, #d4380d)',
                      fontSize: 12,
                      background: 'rgba(212, 56, 13, 0.08)',
                      borderRadius: 4,
                    }}
                    data-testid="delivery-detail-error"
                  >
                    {dError}
                  </div>
                )}
                <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginBottom: 8 }}>
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => {
                      prefillDetail(detailOpp);
                      setDError(null);
                      setDetailEditing(false);
                    }}
                    data-testid="delivery-detail-cancel"
                  >
                    取消
                  </Button>
                  <Button
                    type="button"
                    disabled={dSaving}
                    onClick={() => void saveDetail()}
                    data-testid="delivery-detail-save"
                  >
                    保存修改
                  </Button>
                </div>
              </>
            ) : (
              <>
                {detailRow('备注', detailOpp.note || '—')}
                {detailRow('金额', detailOpp.amount != null ? `¥${detailOpp.amount}` : '—')}
                {detailRow('产品', detailOpp.productName || '—')}
                {detailRow('商务', detailOpp.commercialOwnerName || '—')}
                {detailRow('解决方案', detailOpp.solutionOwnerName || '—')}
                {detailRow('项目经理', detailOpp.pmName || '—')}
                {detailRow('运营', detailOpp.opsOwnerName || '—')}
              </>
            )}

            <div style={{ display: 'flex', alignItems: 'center', margin: '14px 0 6px' }}>
              <div style={{ fontWeight: 600, flex: 1 }}>产出物</div>
              {!addArtOpen && (
                <Button
                  type="button"
                  variant="secondary"
                  onClick={openAddArtifact}
                  data-testid="delivery-detail-add-artifact"
                >
                  添加产出物
                </Button>
              )}
            </div>
            {addArtOpen && (
              <div
                data-testid="delivery-add-artifact-form"
                style={{
                  border: '1px solid var(--rainier-border)',
                  borderRadius: 6,
                  padding: '8px 10px',
                  marginBottom: 8,
                }}
              >
                <div style={{ marginBottom: 8 }}>
                  <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>类型</label>
                  <select
                    className="rainier-treeselect-trigger"
                    value={aType}
                    onChange={(e) => setAType(e.target.value as ArtifactType)}
                    data-testid="delivery-add-type"
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
                    data-testid="delivery-add-link"
                  />
                ) : (
                  <>
                    <Input
                      label="标题（可空）"
                      value={aTitle}
                      onChange={(e) => setATitle(e.target.value)}
                      data-testid="delivery-add-title"
                    />
                    <div style={{ marginBottom: 12 }}>
                      <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
                        正文（支持 Markdown）
                      </label>
                      <textarea
                        className="rainier-input"
                        style={{ width: '100%', minHeight: 70, padding: 8, boxSizing: 'border-box' }}
                        value={aContent}
                        onChange={(e) => setAContent(e.target.value)}
                        data-testid="delivery-add-content"
                      />
                    </div>
                  </>
                )}
                {aError && (
                  <div
                    style={{ color: 'var(--rainier-color-danger, #d4380d)', fontSize: 12, marginBottom: 8 }}
                    data-testid="delivery-add-error"
                  >
                    {aError}
                  </div>
                )}
                <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
                  <Button type="button" variant="secondary" onClick={() => setAddArtOpen(false)}>
                    取消
                  </Button>
                  <Button
                    type="button"
                    disabled={aSaving}
                    onClick={() => void submitAddArtifact()}
                    data-testid="delivery-add-save"
                  >
                    保存
                  </Button>
                </div>
              </div>
            )}
            {detailArtsLoading ? (
              <div style={{ color: 'var(--rainier-color-text-2)' }}>加载中…</div>
            ) : detailArts.length === 0 ? (
              <div data-testid="delivery-detail-no-arts" style={{ color: 'var(--rainier-color-text-2)' }}>
                暂无产出物
              </div>
            ) : (
              detailArts.map((a) => (
                <div
                  key={a.id}
                  data-testid={`delivery-detail-artifact-${a.id}`}
                  style={{
                    border: '1px solid var(--rainier-border)',
                    borderRadius: 6,
                    padding: '8px 10px',
                    marginBottom: 8,
                  }}
                >
                  <div style={{ fontWeight: 600, fontSize: 13 }}>
                    {a.typeLabel}
                    {a.title && a.title !== a.typeLabel ? ` · ${a.title}` : ''}
                  </div>
                  <div style={{ fontSize: 12, color: 'var(--rainier-color-text-2)', marginBottom: 6 }}>
                    {a.stageFrom ?? '—'}
                    {a.decision ? ` · ${a.decision}` : ''} · {a.author ?? '—'}
                  </div>
                  {isLinkArtifact(a.type) ? (
                    a.link ? (
                      <a
                        href={a.link}
                        target="_blank"
                        rel="noreferrer"
                        data-testid={`delivery-detail-link-${a.id}`}
                      >
                        打开链接 ↗
                      </a>
                    ) : null
                  ) : (
                    <>
                      <Button
                        type="button"
                        variant="secondary"
                        onClick={() => setPreviewArtId(previewArtId === a.id ? null : a.id)}
                        data-testid={`delivery-detail-preview-${a.id}`}
                      >
                        {previewArtId === a.id ? '收起' : '预览'}
                      </Button>{' '}
                      <Button
                        type="button"
                        variant="secondary"
                        onClick={() =>
                          void exportArtifactDocx(a.opportunityId, a.id, `${a.typeLabel}-${a.id}.docx`)
                        }
                        data-testid={`delivery-detail-export-${a.id}`}
                      >
                        导出 Word
                      </Button>
                      {previewArtId === a.id ? (
                        <div
                          style={{
                            marginTop: 8,
                            padding: '8px 10px',
                            background: 'var(--rainier-bg-hover)',
                            borderRadius: 6,
                          }}
                        >
                          <MarkdownView content={a.content} testId={`delivery-detail-md-${a.id}`} />
                        </div>
                      ) : null}
                    </>
                  )}
                </div>
              ))
            )}
          </div>
        )}
      </Drawer>
    </div>
  );
}

function modeTabStyle(active: boolean): React.CSSProperties {
  return {
    fontSize: 13,
    padding: '5px 14px',
    border: '1px solid var(--rainier-border)',
    borderRadius: 4,
    background: active ? 'var(--rainier-bg-hover)' : 'transparent',
    color: active ? 'var(--rainier-color-text-1)' : 'var(--rainier-color-text-2)',
    cursor: 'pointer',
  };
}
