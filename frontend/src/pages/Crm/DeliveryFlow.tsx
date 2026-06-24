import { useCallback, useEffect, useState } from 'react';
import { DashboardCard, EmptyState, StatTiles, StatusChip } from '../../components/board';
import { Button } from '../../components/ui/Button';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { listProjects, PROJECT_TYPE_LABELS, type Project } from '../../api/project';
import { listUsers, type User } from '../../api/user';
import { useAuthStore } from '../../store/auth';
import {
  advanceOpportunity,
  initiateOpportunity,
  listOpportunities,
  OPP_DELIVERY_STAGES,
  OPP_STAGE_LABELS,
  type Opportunity,
} from '../../api/opportunity';

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

  const advance = async (id: number, decision?: 'PASS' | 'REJECT') => {
    setBusyId(id);
    try {
      await advanceOpportunity(id, decision);
      await load();
    } finally {
      setBusyId(null);
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
        <h2 style={{ margin: 0 }}>实施流转</h2>
      </div>

      <StatTiles
        testId="delivery-summary"
        tiles={[
          { label: '实施中', value: inProgress, tier: inProgress > 0 ? 'yellow' : 'gray' },
          { label: '已验收', value: accepted, tier: accepted > 0 ? 'green' : 'gray' },
        ]}
      />

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
                    <td style={{ padding: '6px 8px', width: 250, textAlign: 'right' }}>
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
                          disabled={busyId === r.id}
                          onClick={() => void advance(r.id)}
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
