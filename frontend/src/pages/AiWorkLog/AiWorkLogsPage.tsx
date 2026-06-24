import { useCallback, useEffect, useState } from 'react';
import { DashboardCard, EmptyState, StatTiles, StatusChip } from '../../components/board';
import { Button } from '../../components/ui/Button';
import { Drawer } from '../../components/ui/Drawer';
import {
  AI_STATUS_LABELS,
  decideAiWorkLog,
  listAiWorkLogs,
  type AiWorkLog,
  type AiWorkLogStatus,
} from '../../api/aiWorkLog';
import type { StatusTier } from '../../utils/board';

const STATUS_FILTERS: { value: '' | AiWorkLogStatus; label: string }[] = [
  { value: '', label: '全部' },
  { value: 'PROPOSED', label: '待裁决' },
  { value: 'ACCEPTED', label: '已采纳' },
  { value: 'REJECTED', label: '已驳回' },
];

const STATUS_TIER: Record<AiWorkLogStatus, StatusTier> = {
  PROPOSED: 'yellow',
  ACCEPTED: 'green',
  REJECTED: 'red',
};

/**
 * v0.0.43 — AI 工作日志 (the flywheel base). Lists AI proposals (agent / action / summary / evidence)
 * and lets a human accept or reject each PROPOSED one (POST /api/ai-work-logs/{id}/decision). Rejections
 * carry a reason — the KPI signal for AI quality. Seed-driven until real AI/integration arrives.
 */
export function AiWorkLogsPage() {
  const [rows, setRows] = useState<AiWorkLog[]>([]);
  const [statusFilter, setStatusFilter] = useState<'' | AiWorkLogStatus>('');
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<number | null>(null);
  // v0.0.60 — reject reason drawer (replaces window.prompt). Required-non-empty enforced.
  const [rejectId, setRejectId] = useState<number | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [rejectError, setRejectError] = useState<string | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    return listAiWorkLogs({ status: statusFilter || undefined, size: 100 })
      .then((r) => {
        setRows(r.content);
        setLoading(false);
      })
      .catch(() => {
        setRows([]);
        setLoading(false);
      });
  }, [statusFilter]);

  useEffect(() => {
    void load();
  }, [load]);

  const counts = { PROPOSED: 0, ACCEPTED: 0, REJECTED: 0 } as Record<AiWorkLogStatus, number>;
  for (const r of rows) counts[r.status] += 1;

  const accept = async (id: number) => {
    setBusyId(id);
    try {
      await decideAiWorkLog(id, 'ACCEPTED');
      await load();
    } finally {
      setBusyId(null);
    }
  };

  const openReject = (id: number) => {
    setRejectId(id);
    setRejectReason('');
    setRejectError(null);
  };

  const submitReject = async () => {
    if (rejectId == null) return;
    const reason = rejectReason.trim();
    if (!reason) {
      setRejectError('请填写驳回理由');
      return;
    }
    setBusyId(rejectId);
    try {
      await decideAiWorkLog(rejectId, 'REJECTED', reason);
      setRejectId(null);
      await load();
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2>AI 工作日志</h2>
        <select
          data-testid="ai-status-filter"
          className="rainier-select"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as '' | AiWorkLogStatus)}
        >
          {STATUS_FILTERS.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
      </div>

      <StatTiles
        testId="ai-summary"
        tiles={[
          { label: '待裁决', value: counts.PROPOSED, tier: counts.PROPOSED > 0 ? 'yellow' : 'gray' },
          { label: '已采纳', value: counts.ACCEPTED, tier: 'green' },
          { label: '已驳回', value: counts.REJECTED, tier: counts.REJECTED > 0 ? 'red' : 'gray' },
        ]}
      />

      <DashboardCard title="AI 提议（可见 / 可采纳 / 可驳回）" testId="ai-list">
        {!loading && rows.length === 0 ? (
          <EmptyState message="暂无 AI 工作日志。" testId="ai-empty" />
        ) : (
          <table className="rainier-list-table">
            <tbody>
              {rows.map((r) => (
                <tr key={r.id} data-testid={`ai-row-${r.id}`}>
                  <td style={{ padding: '6px 8px', width: 96 }}>
                    <StatusChip
                      status={r.status}
                      tier={STATUS_TIER[r.status]}
                      label={AI_STATUS_LABELS[r.status]}
                    />
                  </td>
                  <td style={{ padding: '6px 8px' }}>
                    <div>
                      <span style={{ color: 'var(--rainier-color-text-2)', fontSize: 12 }}>
                        {r.agentType} · {r.action}
                      </span>
                    </div>
                    <div>{r.summary}</div>
                    <div style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
                      证据：{r.evidence}
                      {r.status === 'REJECTED' && r.rejectReason ? ` · 驳回：${r.rejectReason}` : ''}
                    </div>
                  </td>
                  <td style={{ padding: '6px 8px', width: 150, textAlign: 'right' }}>
                    {r.status === 'PROPOSED' ? (
                      <>
                        <Button
                          type="button"
                          variant="primary"
                          disabled={busyId === r.id}
                          onClick={() => void accept(r.id)}
                          data-testid={`ai-accept-${r.id}`}
                        >
                          采纳
                        </Button>
                        <Button
                          type="button"
                          variant="secondary"
                          style={{ marginLeft: 6 }}
                          disabled={busyId === r.id}
                          onClick={() => openReject(r.id)}
                          data-testid={`ai-reject-${r.id}`}
                        >
                          驳回
                        </Button>
                      </>
                    ) : (
                      <span style={{ color: 'var(--rainier-color-text-2)', fontSize: 12 }}>
                        {r.decidedBy ?? ''}
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </DashboardCard>

      <Drawer
        open={rejectId !== null}
        title="驳回 AI 提议"
        onClose={() => setRejectId(null)}
        footer={
          <>
            <Button type="button" variant="secondary" onClick={() => setRejectId(null)}>
              取消
            </Button>
            <Button
              type="button"
              variant="primary"
              disabled={busyId === rejectId}
              onClick={() => void submitReject()}
              data-testid="ai-reject-submit"
            >
              确认驳回
            </Button>
          </>
        }
      >
        <div className="rainier-form-group">
          <label className="rainier-form-label" htmlFor="ai-reject-reason">
            驳回理由（必填）
          </label>
          <textarea
            id="ai-reject-reason"
            className="rainier-input"
            rows={5}
            value={rejectReason}
            onChange={(e) => {
              setRejectReason(e.target.value);
              if (rejectError) setRejectError(null);
            }}
            data-testid="ai-reject-reason"
          />
        </div>
        {rejectError && <div className="rainier-error-banner">{rejectError}</div>}
      </Drawer>
    </div>
  );
}
