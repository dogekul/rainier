import { type CSSProperties, useCallback, useEffect, useMemo, useState } from 'react';
import { StatTiles, StatusChip } from '../../components/board';
import { Button } from '../../components/ui/Button';
import { Drawer } from '../../components/ui/Drawer';
import { MarkdownView } from '../../components/ui/MarkdownView';
import {
  listOpportunities,
  OPP_GATE_STAGES,
  OPP_PHASES,
  OPP_PRESALE_STAGES,
  OPP_STAGE_LABELS,
  OPP_STAGE_ORDER,
  type Opportunity,
  type OpportunityStage,
} from '../../api/opportunity';
import {
  exportArtifactDocx,
  listOpportunityArtifacts,
  type OpportunityArtifact,
} from '../../api/opportunityArtifact';
import { formatCNY } from '../../utils/money';
import { DWELL_LABEL, dwellDays, dwellTier } from '../../utils/dwell';

const PRESALE_COLOR = '#378ADD';
const DELIVERY_COLOR = '#1D9E75';

function phaseColor(stage: OpportunityStage): string {
  return OPP_PRESALE_STAGES.includes(stage) ? PRESALE_COLOR : DELIVERY_COLOR;
}

/** Display owner = first set of the 4 phase owners. */
function ownerOf(r: Opportunity): string {
  return r.pmName || r.commercialOwnerName || r.solutionOwnerName || r.opsOwnerName || '';
}

type View = 'board' | 'list';
type SortKey = 'stage' | 'amount' | 'dwell';

/**
 * v0.0.47 — 商机看板 (READ-ONLY 进展总览). 改版：上下两条相位泳道带 + 漏斗分布条 + 负责人/产品/客户过滤 +
 * 看板/列表切换 + 停留时长预警点 + 金额格式化。所有流转操作仍在「售前/实施流转」操作页 —— 看板只看，不动。
 */
export function OpportunityBoard() {
  const [rows, setRows] = useState<Opportunity[]>([]);
  const [view, setView] = useState<View>('board');
  const [fOwner, setFOwner] = useState('');
  const [fProduct, setFProduct] = useState('');
  const [fQ, setFQ] = useState('');
  const [includeLost, setIncludeLost] = useState(false);
  const [sortKey, setSortKey] = useState<SortKey>('stage');
  // 产出物 read-only viewer (查看/导出 are read-only — not 流转 operations)
  const [artifactsForId, setArtifactsForId] = useState<number | null>(null);
  const [arts, setArts] = useState<OpportunityArtifact[]>([]);
  const [artsLoading, setArtsLoading] = useState(false);

  const load = useCallback(() => {
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

  const now = new Date();

  // Global metrics (independent of the display filters).
  const open = rows.filter((r) => r.status === 'OPEN');
  const won = rows.filter((r) => r.status === 'WON').length;
  const lost = rows.filter((r) => r.status === 'LOST').length;
  const openAmount = open.reduce((sum, r) => sum + (r.amount ?? 0), 0);

  const ownerOptions = useMemo(
    () => Array.from(new Set(rows.map(ownerOf).filter(Boolean))).sort(),
    [rows],
  );
  const productOptions = useMemo(
    () => Array.from(new Set(rows.map((r) => r.productName).filter((p): p is string => !!p))).sort(),
    [rows],
  );

  const filtered = useMemo(() => {
    const q = fQ.trim().toLowerCase();
    return rows.filter((r) => {
      if (!includeLost && r.status === 'LOST') return false;
      if (fOwner && ownerOf(r) !== fOwner) return false;
      if (fProduct && r.productName !== fProduct) return false;
      if (q && !r.customerName.toLowerCase().includes(q)) return false;
      return true;
    });
  }, [rows, includeLost, fOwner, fProduct, fQ]);

  const funnel = useMemo(() => {
    const m = new Map<OpportunityStage, number>();
    for (const r of filtered) m.set(r.stage, (m.get(r.stage) ?? 0) + 1);
    return m;
  }, [filtered]);

  const listRows = useMemo(() => {
    const copy = [...filtered];
    if (sortKey === 'amount') {
      copy.sort((a, b) => (b.amount ?? -1) - (a.amount ?? -1));
    } else if (sortKey === 'dwell') {
      copy.sort((a, b) => (dwellDays(b.stageEnteredAt, now) ?? -1) - (dwellDays(a.stageEnteredAt, now) ?? -1));
    } else {
      copy.sort((a, b) => OPP_STAGE_ORDER.indexOf(a.stage) - OPP_STAGE_ORDER.indexOf(b.stage));
    }
    return copy;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filtered, sortKey]);

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2>商机看板</h2>
        <div style={{ flex: 1 }} />
        <span style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }} data-testid="opp-readonly-hint">
          只读 · 全商机进展总览（操作请到 售前/实施 流转）
        </span>
      </div>

      {/* Toolbar: view toggle + filters */}
      <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', marginBottom: 10 }}>
        <div style={{ display: 'inline-flex', border: '1px solid var(--rainier-border)', borderRadius: 6, overflow: 'hidden' }}>
          <button
            type="button"
            data-testid="opp-view-board"
            onClick={() => setView('board')}
            style={tabStyle(view === 'board')}
          >
            看板
          </button>
          <button
            type="button"
            data-testid="opp-view-list"
            onClick={() => setView('list')}
            style={tabStyle(view === 'list')}
          >
            列表
          </button>
        </div>
        <div style={{ flex: 1 }} />
        <select
          className="rainier-input"
          data-testid="opp-filter-owner"
          value={fOwner}
          onChange={(e) => setFOwner(e.target.value)}
          style={{ maxWidth: 160 }}
        >
          <option value="">全部负责人</option>
          {ownerOptions.map((o) => (
            <option key={o} value={o}>
              {o}
            </option>
          ))}
        </select>
        <select
          className="rainier-input"
          data-testid="opp-filter-product"
          value={fProduct}
          onChange={(e) => setFProduct(e.target.value)}
          style={{ maxWidth: 160 }}
        >
          <option value="">全部产品</option>
          {productOptions.map((p) => (
            <option key={p} value={p}>
              {p}
            </option>
          ))}
        </select>
        <input
          className="rainier-input"
          data-testid="opp-filter-q"
          placeholder="搜索客户"
          value={fQ}
          onChange={(e) => setFQ(e.target.value)}
          style={{ maxWidth: 160 }}
        />
      </div>

      <StatTiles
        testId="opp-summary"
        tiles={[
          { label: '进行中', value: open.length, tier: 'yellow' },
          { label: '赢单', value: won, tier: 'green' },
          { label: '丢单', value: lost, tier: lost > 0 ? 'red' : 'gray' },
          { label: '在谈金额', value: formatCNY(openAmount) },
        ]}
      />

      <div style={{ margin: '8px 0' }}>
        <button
          type="button"
          data-testid="opp-tile-lost"
          onClick={() => setIncludeLost((v) => !v)}
          style={{
            ...chipStyle,
            color: includeLost ? 'var(--rainier-status-red)' : 'var(--rainier-color-text-2)',
            borderColor: includeLost ? 'var(--rainier-status-red)' : 'var(--rainier-border)',
          }}
        >
          {includeLost ? '隐藏丢单' : `显示丢单（${lost}）`}
        </button>
      </div>

      {/* Funnel distribution strip — 10 stages, counts, phase color, gate ⭐ */}
      <div
        data-testid="opp-funnel"
        style={{
          display: 'grid',
          gridTemplateColumns: `repeat(${OPP_STAGE_ORDER.length}, 1fr)`,
          gap: 4,
          border: '1px solid var(--rainier-border)',
          borderRadius: 'var(--rainier-radius-card)',
          background: 'var(--rainier-bg-card)',
          boxShadow: 'var(--rainier-shadow-card)',
          padding: '12px 14px',
          marginBottom: 12,
          textAlign: 'center',
        }}
      >
        {OPP_STAGE_ORDER.map((stage) => (
          <div key={stage} data-testid={`opp-funnel-${stage}`}>
            <div style={{ fontSize: 16, fontWeight: 600 }}>{funnel.get(stage) ?? 0}</div>
            <div style={{ fontSize: 11, color: 'var(--rainier-color-text-2)' }}>
              {OPP_STAGE_LABELS[stage]}
              {OPP_GATE_STAGES.includes(stage) ? ' ⭐' : ''}
            </div>
            <div style={{ height: 3, borderRadius: 2, background: phaseColor(stage), marginTop: 4 }} />
          </div>
        ))}
      </div>

      {view === 'board' ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {OPP_PHASES.map((phase) => (
            <div
              key={phase.key}
              data-testid={`opp-phase-${phase.key}`}
              style={{
                background: 'var(--rainier-bg-card)',
                border: '1px solid var(--rainier-border)',
                borderLeft: `3px solid ${phaseColor(phase.stages[0])}`,
                borderRadius: 'var(--rainier-radius-card)',
                boxShadow: 'var(--rainier-shadow-card)',
                padding: '12px 14px',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
                <span style={{ fontWeight: 600, fontSize: 14, color: phaseColor(phase.stages[0]) }}>
                  {phase.label}
                </span>
                <span style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
                  负责人：{phase.owner}
                </span>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: `repeat(${phase.stages.length}, 1fr)`, gap: 8 }}>
                {phase.stages.map((stage) => {
                  const col = filtered.filter((r) => r.stage === stage);
                  const isGate = OPP_GATE_STAGES.includes(stage);
                  return (
                    <div
                      key={stage}
                      data-testid={`opp-col-${stage}`}
                      style={{
                        border: '1px solid var(--rainier-border)',
                        borderRadius: 8,
                        padding: 7,
                        background: 'var(--rainier-bg-hover)',
                      }}
                    >
                      <div style={{ fontSize: 12, color: 'var(--rainier-color-text-2)', marginBottom: 6 }}>
                        {OPP_STAGE_LABELS[stage]}
                        {isGate ? ' ⭐' : ''} · {col.length}
                      </div>
                      {col.length === 0 ? (
                        <div style={{ fontSize: 11, color: 'var(--rainier-color-text-3, #bbb)', textAlign: 'center', padding: '8px 0' }}>
                          —
                        </div>
                      ) : (
                        col.map((r) => <OppCard key={r.id} r={r} now={now} onOpen={() => setArtifactsForId(r.id)} />)
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
            <span style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>排序</span>
            <select
              className="rainier-input"
              data-testid="opp-list-sort"
              value={sortKey}
              onChange={(e) => setSortKey(e.target.value as SortKey)}
              style={{ maxWidth: 160 }}
            >
              <option value="stage">按阶段</option>
              <option value="amount">按金额</option>
              <option value="dwell">按停留天数</option>
            </select>
          </div>
          <div className="rainier-card" style={{ padding: '4px 14px', overflowX: 'auto' }}>
          <table data-testid="opp-list" className="rainier-table" style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
            <thead>
              <tr style={{ textAlign: 'left', color: 'var(--rainier-color-text-2)' }}>
                <th style={thStyle}>客户</th>
                <th style={thStyle}>标题</th>
                <th style={thStyle}>阶段</th>
                <th style={thStyle}>金额</th>
                <th style={thStyle}>负责人</th>
                <th style={thStyle}>停留(天)</th>
                <th style={thStyle}>状态</th>
              </tr>
            </thead>
            <tbody>
              {listRows.map((r) => {
                const tier = dwellTier(r.stageEnteredAt, now);
                const days = dwellDays(r.stageEnteredAt, now);
                return (
                  <tr
                    key={r.id}
                    data-testid={`opp-list-row-${r.id}`}
                    tabIndex={0}
                    onClick={() => setArtifactsForId(r.id)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        setArtifactsForId(r.id);
                      }
                    }}
                    style={{ cursor: 'pointer', borderTop: '1px solid var(--rainier-border)' }}
                  >
                    <td style={tdStyle}>{r.customerName}</td>
                    <td style={tdStyle}>{r.title}</td>
                    <td style={tdStyle}>{OPP_STAGE_LABELS[r.stage]}</td>
                    <td style={tdStyle}>{formatCNY(r.amount)}</td>
                    <td style={tdStyle}>{ownerOf(r) || '—'}</td>
                    <td style={tdStyle}>
                      <span data-testid={`opp-dwell-${r.id}`} data-tier={tier} title={DWELL_LABEL[tier]}>
                        <span style={{ ...dotStyle, background: `var(--rainier-status-${tier})` }} />
                        {days == null ? '—' : days}
                      </span>
                    </td>
                    <td style={tdStyle}>
                      {r.status === 'WON' ? (
                        <StatusChip status="WON" label="赢单" tier="green" testId={`opp-status-${r.id}`} />
                      ) : r.status === 'LOST' ? (
                        <StatusChip status="LOST" label="丢单" tier="red" testId={`opp-status-${r.id}`} />
                      ) : (
                        '进行中'
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          </div>
        </div>
      )}

      {/* 产出物 read-only viewer + Word 导出 */}
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
                onClick={() => void exportArtifactDocx(a.opportunityId, a.id, `${a.typeLabel}-${a.id}.docx`)}
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

/** A single read-only opportunity card; whole card opens the 产出物 drawer. */
function OppCard({ r, now, onOpen }: { r: Opportunity; now: Date; onOpen: () => void }) {
  const tier = dwellTier(r.stageEnteredAt, now);
  const days = dwellDays(r.stageEnteredAt, now);
  return (
    <div
      data-testid={`opp-card-${r.id}`}
      role="button"
      tabIndex={0}
      onClick={onOpen}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') onOpen();
      }}
      style={{
        border: '1px solid var(--rainier-border)',
        borderRadius: 6,
        padding: '6px 8px',
        marginBottom: 6,
        background: 'var(--rainier-color-bg, #fff)',
        cursor: 'pointer',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <span
          data-testid={`opp-dwell-${r.id}`}
          data-tier={tier}
          title={`${DWELL_LABEL[tier]}${days == null ? '' : ` · ${days}天`}`}
          style={{ ...dotStyle, background: `var(--rainier-status-${tier})` }}
        />
        <div style={{ fontWeight: 600, fontSize: 13, flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {r.customerName}
        </div>
        {r.status === 'WON' ? (
          <StatusChip status="WON" label="赢单" tier="green" testId={`opp-status-${r.id}`} />
        ) : r.status === 'LOST' ? (
          <StatusChip status="LOST" label="丢单" tier="red" testId={`opp-status-${r.id}`} />
        ) : null}
      </div>
      <div style={{ fontSize: 13 }}>{r.title}</div>
      <div style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
        {formatCNY(r.amount)} · {ownerOf(r) || '—'}
      </div>
      {r.productName ? (
        <div
          style={{ fontSize: 11, color: 'var(--rainier-color-text-2)', marginTop: 2 }}
          data-testid={`opp-product-${r.id}`}
        >
          🏷 {r.productName}
        </div>
      ) : null}
    </div>
  );
}

const dotStyle: CSSProperties = {
  display: 'inline-block',
  width: 8,
  height: 8,
  borderRadius: '50%',
  marginRight: 4,
  verticalAlign: 'middle',
  flex: 'none',
};

const chipStyle: CSSProperties = {
  fontSize: 12,
  padding: '4px 10px',
  border: '1px solid var(--rainier-border)',
  borderRadius: 14,
  background: 'transparent',
  cursor: 'pointer',
};

function tabStyle(active: boolean): CSSProperties {
  return {
    fontSize: 13,
    padding: '5px 14px',
    border: 'none',
    background: active ? 'var(--rainier-bg-hover)' : 'transparent',
    color: active ? 'var(--rainier-color-text-1)' : 'var(--rainier-color-text-2)',
    cursor: 'pointer',
  };
}

const thStyle: CSSProperties = { padding: '6px 8px', fontWeight: 500 };
const tdStyle: CSSProperties = { padding: '6px 8px' };
