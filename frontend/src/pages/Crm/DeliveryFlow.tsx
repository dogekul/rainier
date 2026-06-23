import { useCallback, useEffect, useState } from 'react';
import { DashboardCard, EmptyState, StatTiles, StatusChip } from '../../components/board';
import { Button } from '../../components/ui/Button';
import { Drawer } from '../../components/ui/Drawer';
import { listProjects, type Project } from '../../api/project';
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

/**
 * v0.0.44 —「实施流转」(delivery operations). The actionable surface for WON opportunities in 实施环节
 * (stage ∈ 立项..验收): 立项移交(链入交付 Project) + 立项评审(通过→现场调研 / 否决→停在立项) + 逐节点推进 + 验收终态.
 * 售前操作在「售前流转」；只读总览在「商机看板」。「立项移交」是商机→交付 Project 的显式入口 (POST /initiate)。
 */
export function DeliveryFlow() {
  const [rows, setRows] = useState<Opportunity[]>([]);
  const [busyId, setBusyId] = useState<number | null>(null);

  // 立项移交 drawer state
  const [handoffId, setHandoffId] = useState<number | null>(null);
  const [projects, setProjects] = useState<Project[]>([]);
  const [projectId, setProjectId] = useState<number | ''>('');
  const [handoffSaving, setHandoffSaving] = useState(false);

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
      return;
    }
    void listProjects({ size: 100 }).then((r) => setProjects(r.content));
  }, [handoffId]);

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
    if (handoffId == null || projectId === '') return;
    setHandoffSaving(true);
    try {
      await initiateOpportunity(handoffId, projectId, 'PASS');
      setHandoffId(null);
      await load();
    } finally {
      setHandoffSaving(false);
    }
  };

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
                        <>
                          <Button
                            type="button"
                            variant="secondary"
                            disabled={busyId === r.id || handoffId === r.id}
                            onClick={() => setHandoffId(r.id)}
                            data-testid={`delivery-handoff-${r.id}`}
                          >
                            立项移交
                          </Button>
                          <Button
                            type="button"
                            variant="primary"
                            style={{ marginLeft: 6 }}
                            disabled={busyId === r.id}
                            onClick={() => void advance(r.id, 'PASS')}
                            data-testid={`delivery-pass-${r.id}`}
                          >
                            通过
                          </Button>
                          <Button
                            type="button"
                            variant="secondary"
                            style={{ marginLeft: 6 }}
                            disabled={busyId === r.id}
                            onClick={() => void advance(r.id, 'REJECT')}
                            data-testid={`delivery-reject-${r.id}`}
                          >
                            否决
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
        title="立项移交 — 关联交付项目"
        onClose={() => setHandoffId(null)}
      >
        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>交付项目</label>
          {projects.length === 0 ? (
            <div
              style={{ fontSize: 12, color: 'var(--rainier-color-text-2)', padding: '6px 0' }}
              data-testid="delivery-no-projects"
            >
              暂无可用项目，请先在「项目地图」创建交付项目后再移交。
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
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <Button type="button" variant="secondary" onClick={() => setHandoffId(null)}>
            取消
          </Button>
          <Button
            type="button"
            disabled={handoffSaving || projectId === ''}
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
