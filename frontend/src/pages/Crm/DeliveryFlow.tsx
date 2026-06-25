import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './oppFlow.css';
import { DashboardCard, EmptyState, StatTiles, StatusChip } from '../../components/board';
import { Button } from '../../components/ui/Button';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { listProjects, PROJECT_TYPE_LABELS, type Project } from '../../api/project';
import { listUsers, type User } from '../../api/user';
import { listRequirements } from '../../api/requirement';
import { useAuthStore } from '../../store/auth';
import {
  advanceOpportunity,
  initiateOpportunity,
  listOpportunities,
  OPP_DELIVERY_STAGES,
  OPP_STAGE_LABELS,
  type Opportunity,
} from '../../api/opportunity';
import {
  ARTIFACT_TYPE_LABELS,
  createOpportunityArtifact,
  isLinkArtifact,
  listOpportunityArtifacts,
  STAGE_REQUIRED_ARTIFACTS,
  type ArtifactType,
} from '../../api/opportunityArtifact';

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
  const navigate = useNavigate();
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

  // 实施环节「推进」路由：
  //  - 现场调研：缺件 → 开补充表单；齐备 → 直接推进。
  //  - 产品诉求(v0.0.56)：无任何需求 → 跳转到详情页让用户填写诉求转化；已有需求 → 直接推进。
  //  - 其它非终态：直接推进。
  const requestAdvance = async (r: Opportunity) => {
    if (busyId === r.id || suppOpp?.id === r.id) return; // in-flight / 表单已开 → 防重复触发
    // v0.0.56 产品诉求 → 交付实施：必须先有需求；否则跳详情页。
    if (r.stage === 'REQUIREMENT') {
      setBusyId(r.id);
      let hasRequirement = false;
      try {
        const page = await listRequirements({ opportunityId: r.id, size: 1 });
        hasRequirement = (page.content?.length ?? 0) > 0;
      } catch {
        // lookup failed → fall through to advance; backend gate is the safety net
      }
      setBusyId(null);
      if (!hasRequirement) {
        // v0.0.56 — 带上提示参数，详情页会滚动到「产品诉求 / 需求」卡 + 展示补全提示横幅
        navigate(`/crm/opportunities/${r.id}?action=convert`);
        return;
      }
      void advance(r.id);
      return;
    }
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

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2>实施流转</h2>
      </div>

      <StatTiles
        testId="delivery-summary"
        tiles={[
          { label: '实施中', value: inProgress, tier: inProgress > 0 ? 'yellow' : 'gray' },
          { label: '已验收', value: accepted, tier: accepted > 0 ? 'green' : 'gray' },
        ]}
      />

      {advError && (
        <div className="rainier-error-banner" data-testid="delivery-adv-error">
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
          <div className="oppflow-list">
            {items.map((r) => {
              const isInitiation = r.stage === INITIATION;
              const isTerminal = r.stage === ACCEPTANCE;
              return (
                <div key={r.id} className="oppflow-card" data-testid={`delivery-row-${r.id}`}>
                  <div className="oppflow-stage">
                    <StatusChip
                      status={r.stage}
                      label={OPP_STAGE_LABELS[r.stage] + (isInitiation ? ' ⭐' : '')}
                      testId={`delivery-stage-${r.id}`}
                    />
                  </div>
                  <div className="oppflow-main">
                    <div className="oppflow-customer">{r.customerName}</div>
                    <div className="oppflow-title">{r.title}</div>
                  </div>
                  <div className="oppflow-meta">
                    <div className="oppflow-cell">
                      <span className="oppflow-cell-label">项目经理</span>
                      <span className="oppflow-cell-value">{r.pmName ?? '—'}</span>
                    </div>
                    <div className="oppflow-cell">
                      <span className="oppflow-cell-label">项目</span>
                      <span className={`oppflow-cell-value${r.projectId == null ? ' muted' : ''}`}>
                        {r.projectName
                          ? `${r.projectCode ?? ''} ${r.projectName}`.trim()
                          : r.projectId != null
                            ? `#${r.projectId}`
                            : '未立项'}
                      </span>
                    </div>
                  </div>
                  <div className="oppflow-actions">
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={() => navigate(`/crm/opportunities/${r.id}`)}
                      data-testid={`delivery-detail-${r.id}`}
                    >
                      详情
                    </Button>
                    <div className="oppflow-actions-stage">
                      {isTerminal ? (
                        <span className="oppflow-done" data-testid={`delivery-done-${r.id}`}>
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
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
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
          <div className="rainier-form-group">
            <label className="rainier-form-label">
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
                className="rainier-form-select"
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
          <div className="rainier-form-group">
            <Input
              label="项目名称"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              data-testid="delivery-new-name"
            />
            <div style={{ marginBottom: 8 }}>
              <label className="rainier-form-label">项目负责人</label>
              <select
                className="rainier-form-select"
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
          <div className="rainier-error-banner" data-testid="delivery-handoff-error">
            {handoffError}
          </div>
        )}

        <div className="rainier-form-footer">
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
                  <label className="rainier-form-label">
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
          <div className="rainier-error-banner" data-testid="delivery-supp-error">
            {suppError}
          </div>
        )}
        <div className="rainier-form-footer">
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
