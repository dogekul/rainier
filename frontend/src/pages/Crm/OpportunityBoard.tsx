import { useCallback, useEffect, useState } from 'react';
import { StatTiles, StatusChip } from '../../components/board';
import {
  listOpportunities,
  OPP_GATE_STAGES,
  OPP_PHASES,
  OPP_STAGE_LABELS,
  type Opportunity,
  type OpportunityStage,
} from '../../api/opportunity';

/**
 * v0.0.44 — 商机看板 (READ-ONLY progress board). 给监控角色（待定）查看所有商机进展：售前/实施两段泳道 + 10 节点
 * 分列，卡片只读（客户/标题/金额/负责人/赢单标识）。所有「流转操作」(新建/推进/关口决策/立项移交) 都不在看板上 ——
 * 它们分别在「售前流转」「实施流转」两个操作页。看板只看，不动。
 */
export function OpportunityBoard() {
  const [rows, setRows] = useState<Opportunity[]>([]);

  const load = useCallback(() => {
    // size capped at 100 by PageParams; the board shows the active pipeline (most-recent first).
    return listOpportunities({ size: 100 })
      .then((r) => setRows(r.content))
      .catch(() => setRows([]));
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  // 售前进行中 (OPEN) + 实施中 (WON) 在泳道列展示；丢单 (LOST) 仅滚动进磁贴。
  const active = rows.filter((r) => r.status !== 'LOST');
  const open = rows.filter((r) => r.status === 'OPEN');
  const won = rows.filter((r) => r.status === 'WON').length;
  const lost = rows.filter((r) => r.status === 'LOST').length;
  const openAmount = open.reduce((sum, r) => sum + (r.amount ?? 0), 0);

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2 style={{ margin: 0 }}>商机看板</h2>
        <div style={{ flex: 1 }} />
        <span style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }} data-testid="opp-readonly-hint">
          只读 · 全商机进展总览（操作请到 售前/实施 流转）
        </span>
      </div>

      <StatTiles
        testId="opp-summary"
        tiles={[
          { label: '进行中', value: open.length, tier: 'yellow' },
          { label: '赢单', value: won, tier: 'green' },
          { label: '丢单', value: lost, tier: lost > 0 ? 'red' : 'gray' },
          { label: '在谈金额', value: openAmount },
        ]}
      />

      <div style={{ display: 'flex', gap: 16, overflowX: 'auto', paddingBottom: 4 }}>
        {OPP_PHASES.map((phase) => (
          <div
            key={phase.key}
            data-testid={`opp-phase-${phase.key}`}
            style={{ border: '1px solid var(--rainier-border)', borderRadius: 10, padding: 8 }}
          >
            <div style={{ fontWeight: 700, fontSize: 13, marginBottom: 6 }}>
              {phase.label}{' '}
              <span style={{ color: 'var(--rainier-color-text-2)', fontWeight: 400 }}>
                · 负责人：{phase.owner}
              </span>
            </div>
            <div style={{ display: 'flex', gap: 8 }}>
              {phase.stages.map((stage: OpportunityStage) => {
                const col = active.filter((r) => r.stage === stage);
                const isGate = OPP_GATE_STAGES.includes(stage);
                return (
                  <div
                    key={stage}
                    data-testid={`opp-col-${stage}`}
                    style={{
                      minWidth: 168,
                      flex: '1 0 168px',
                      border: '1px solid var(--rainier-border)',
                      borderRadius: 8,
                      padding: 8,
                      background: 'var(--rainier-bg-hover)',
                    }}
                  >
                    <div style={{ fontWeight: 600, fontSize: 13, marginBottom: 6 }}>
                      {OPP_STAGE_LABELS[stage]}{' '}
                      <span style={{ color: 'var(--rainier-color-text-2)' }}>({col.length})</span>
                      {isGate ? <span style={{ marginLeft: 4 }}>⭐</span> : null}
                    </div>
                    {col.map((r) => (
                      <div
                        key={r.id}
                        data-testid={`opp-card-${r.id}`}
                        style={{
                          border: '1px solid var(--rainier-border)',
                          borderRadius: 6,
                          padding: '6px 8px',
                          marginBottom: 6,
                          background: 'var(--rainier-color-bg, #fff)',
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                          <div style={{ fontWeight: 600, fontSize: 13, flex: 1 }}>{r.customerName}</div>
                          {r.status === 'WON' ? (
                            <StatusChip status="WON" label="赢单" tier="green" testId={`opp-status-${r.id}`} />
                          ) : null}
                        </div>
                        <div style={{ fontSize: 13 }}>{r.title}</div>
                        <div style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
                          {r.amount != null ? `¥${r.amount} · ` : ''}
                          {r.pmName ?? r.commercialOwnerName ?? '—'}
                        </div>
                      </div>
                    ))}
                  </div>
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
