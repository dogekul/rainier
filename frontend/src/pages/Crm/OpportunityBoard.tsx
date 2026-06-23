import { useCallback, useEffect, useState } from 'react';
import { StatTiles, StatusChip } from '../../components/board';
import { Button } from '../../components/ui/Button';
import { Drawer } from '../../components/ui/Drawer';
import { MarkdownView } from '../../components/ui/MarkdownView';
import {
  listOpportunities,
  OPP_GATE_STAGES,
  OPP_PHASES,
  OPP_STAGE_LABELS,
  type Opportunity,
  type OpportunityStage,
} from '../../api/opportunity';
import {
  exportArtifactDocx,
  listOpportunityArtifacts,
  type OpportunityArtifact,
} from '../../api/opportunityArtifact';

/**
 * v0.0.44 — 商机看板 (READ-ONLY progress board). 给监控角色（待定）查看所有商机进展：售前/实施两段泳道 + 10 节点
 * 分列，卡片只读（客户/标题/金额/负责人/赢单标识）。所有「流转操作」(新建/推进/关口决策/立项移交) 都不在看板上 ——
 * 它们分别在「售前流转」「实施流转」两个操作页。看板只看，不动。
 */
export function OpportunityBoard() {
  const [rows, setRows] = useState<Opportunity[]>([]);
  // 产出物 read-only viewer (查看/导出 are read-only — not 流转 operations)
  const [artifactsForId, setArtifactsForId] = useState<number | null>(null);
  const [arts, setArts] = useState<OpportunityArtifact[]>([]);
  const [artsLoading, setArtsLoading] = useState(false);

  const load = useCallback(() => {
    // size capped at 100 by PageParams; the board shows the active pipeline (most-recent first).
    return listOpportunities({ size: 100 })
      .then((r) => setRows(r.content))
      .catch(() => setRows([]));
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (artifactsForId == null) {
      setArts([]);
      return;
    }
    setArtsLoading(true);
    listOpportunityArtifacts(artifactsForId)
      .then(setArts)
      .catch(() => setArts([]))
      .finally(() => setArtsLoading(false));
  }, [artifactsForId]);

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
                        {r.productName ? (
                          <div
                            style={{ fontSize: 11, color: 'var(--rainier-color-text-2)', marginTop: 2 }}
                            data-testid={`opp-product-${r.id}`}
                          >
                            🏷 {r.productName}
                          </div>
                        ) : null}
                        <div style={{ marginTop: 4 }}>
                          <Button
                            type="button"
                            variant="secondary"
                            onClick={() => setArtifactsForId(r.id)}
                            data-testid={`opp-artifacts-${r.id}`}
                          >
                            产出物
                          </Button>
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

      {/* 产出物 read-only viewer + Word 导出 (查看/导出 are read-only, not 流转 operations) */}
      <Drawer open={artifactsForId != null} title="产出物" onClose={() => setArtifactsForId(null)}>
        {artsLoading ? (
          <div style={{ color: 'var(--rainier-color-text-2)' }}>加载中…</div>
        ) : arts.length === 0 ? (
          <div data-testid="opp-artifacts-empty" style={{ color: 'var(--rainier-color-text-2)' }}>
            暂无产出物
          </div>
        ) : (
          arts.map((a) => (
            <div
              key={a.id}
              data-testid={`opp-artifact-${a.id}`}
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
              <div style={{ fontSize: 12, color: 'var(--rainier-color-text-2)', marginBottom: 4 }}>
                {a.stageFrom ?? '—'}
                {a.decision ? ` · ${a.decision}` : ''} · {a.author ?? '—'}
              </div>
              {a.content ? (
                <div style={{ marginBottom: 6 }}>
                  <MarkdownView content={a.content} testId={`opp-artifact-md-${a.id}`} />
                </div>
              ) : null}
              <Button
                type="button"
                variant="secondary"
                onClick={() =>
                  void exportArtifactDocx(a.opportunityId, a.id, `${a.typeLabel}-${a.id}.docx`)
                }
                data-testid={`opp-export-${a.id}`}
              >
                导出 Word
              </Button>
            </div>
          ))
        )}
      </Drawer>
    </div>
  );
}
