import { useCallback, useEffect, useState } from 'react';
import { DashboardCard, EmptyState, StatTiles, StatusChip } from '../../components/board';
import { Button } from '../../components/ui/Button';
import {
  AI_STATUS_LABELS,
  decideAiWorkLog,
  listAiWorkLogs,
  type AiWorkLog,
  type AiWorkLogStatus,
} from '../../api/aiWorkLog';
import type { StatusTier } from '../../utils/board';
import './AiWorkLogsPage.css';

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
 *
 * v0.0.97 — 驳回理由改为内联展开（替换 v0.0.60 Drawer）。同时刻只展开一行；reason 必填。
 */
export function AiWorkLogsPage() {
  const [rows, setRows] = useState<AiWorkLog[]>([]);
  const [statusFilter, setStatusFilter] = useState<'' | AiWorkLogStatus>('');
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<number | null>(null);
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

  const cancelReject = () => {
    setRejectId(null);
    setRejectReason('');
    setRejectError(null);
  };

  const submitReject = async (id: number) => {
    const reason = rejectReason.trim();
    if (!reason) {
      setRejectError('请填写驳回理由');
      return;
    }
    setBusyId(id);
    try {
      await decideAiWorkLog(id, 'REJECTED', reason);
      setRejectId(null);
      setRejectReason('');
      setRejectError(null);
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
          <div className="ai-list">
            {rows.map((r) => (
              <div key={r.id} className="ai-row" data-testid={`ai-row-${r.id}`}>
                <div className="ai-row-main">
                  <StatusChip
                    status={r.status}
                    tier={STATUS_TIER[r.status]}
                    label={AI_STATUS_LABELS[r.status]}
                  />
                  <span className="ai-row-agent">{r.agentType} · {r.action}</span>
                  <span className="ai-row-summary">{r.summary}</span>
                  <span className="rainier-spacer" />
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
                        disabled={busyId === r.id}
                        onClick={() => openReject(r.id)}
                        data-testid={`ai-reject-${r.id}`}
                      >
                        驳回
                      </Button>
                    </>
                  ) : (
                    <span className="ai-row-decider">{r.decidedBy ?? ''}</span>
                  )}
                </div>
                <div className="ai-row-evidence">
                  证据：{r.evidence}
                  {r.status === 'REJECTED' && r.rejectReason ? ` · 驳回：${r.rejectReason}` : ''}
                </div>
                {rejectId === r.id && (
                  <div className="ai-row-reject-form" data-testid={`ai-reject-form-${r.id}`}>
                    <textarea
                      className="rainier-input"
                      rows={3}
                      placeholder="驳回理由（必填）"
                      value={rejectReason}
                      onChange={(e) => {
                        setRejectReason(e.target.value);
                        if (rejectError) setRejectError(null);
                      }}
                      data-testid={`ai-reject-reason-${r.id}`}
                    />
                    {rejectError && (
                      <div
                        className="rainier-error-banner"
                        data-testid={`ai-reject-error-${r.id}`}
                      >
                        {rejectError}
                      </div>
                    )}
                    <div className="ai-row-reject-actions">
                      <Button
                        type="button"
                        variant="secondary"
                        onClick={cancelReject}
                        data-testid={`ai-reject-cancel-${r.id}`}
                      >
                        取消
                      </Button>
                      <Button
                        type="button"
                        variant="primary"
                        disabled={busyId === r.id}
                        onClick={() => void submitReject(r.id)}
                        data-testid={`ai-reject-submit-${r.id}`}
                      >
                        确认驳回
                      </Button>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </DashboardCard>
    </div>
  );
}
