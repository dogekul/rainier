import { useCallback, useEffect, useState } from 'react';
import { Card } from './ui';
import { Button } from './ui/Button';
import {
  acceptWorkLog,
  listMyProposals,
  rejectWorkLog,
  reverseWorkLog,
  type AiWorkLog,
} from '../api/aiWorkLog';
import './AiSuggestionCard.css';

interface AcceptedLocal {
  /** When we accepted; used to gate the 「撤销」 button to the first ~5 seconds. */
  acceptedAt: number;
  reversed: boolean;
}

/**
 * v0.0.103 (F4) — 工作台 “AI 给我提的建议” 卡片. Loads the latest ≤3 PROPOSED AiWorkLogs and lets the
 * user 采纳 / 驳回 (with inline reason textarea, E1-style) / 撤销 (within 5s of accepting, via the F1
 * `/reverse` endpoint). evidence JSON is parsed best-effort to show 事件 #N (SOURCE) as the source
 * back-pointer; missing/malformed evidence is silently ignored.
 *
 * Per-user filter is OutOfScope (AiWorkLog has no targetOwnerUserId yet) — we just show the freshest
 * PROPOSED batch, which is good enough to make the flywheel visible on the workbench.
 */
export function AiSuggestionCard() {
  const [rows, setRows] = useState<AiWorkLog[]>([]);
  const [accepted, setAccepted] = useState<Record<number, AcceptedLocal>>({});
  const [rejectId, setRejectId] = useState<number | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [rejectError, setRejectError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    try {
      const r = await listMyProposals(3);
      setRows(r);
    } catch {
      // Workbench card stays soft — leave the previous list alone on transient errors.
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const showToast = (msg: string) => {
    setToast(msg);
    window.setTimeout(() => setToast(null), 2500);
  };

  const onAccept = async (id: number) => {
    setBusyId(id);
    try {
      await acceptWorkLog(id);
      setAccepted((prev) => ({ ...prev, [id]: { acceptedAt: Date.now(), reversed: false } }));
      showToast('已采纳，任务已更新');
    } catch {
      showToast('采纳失败');
    } finally {
      setBusyId(null);
    }
  };

  const onReverse = async (id: number) => {
    setBusyId(id);
    try {
      await reverseWorkLog(id);
      setAccepted((prev) => ({ ...prev, [id]: { ...prev[id], reversed: true } }));
      showToast('已撤销');
      await refresh();
    } catch {
      showToast('撤销失败');
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
      setRejectError('请填写驳回原因');
      return;
    }
    setBusyId(id);
    try {
      await rejectWorkLog(id, reason);
      cancelReject();
      await refresh();
      showToast('已驳回');
    } catch {
      setRejectError('驳回失败');
    } finally {
      setBusyId(null);
    }
  };

  /** Best-effort: parse evidence JSON for an `eventId` + `source` back-pointer. */
  const evidenceHint = (evidence: string | undefined | null): string | null => {
    if (!evidence) return null;
    try {
      const obj = JSON.parse(evidence) as { eventId?: number; source?: string };
      if (obj && typeof obj.eventId === 'number') {
        const src = obj.source ? ` (${obj.source})` : '';
        return `事件 #${obj.eventId}${src}`;
      }
    } catch {
      // Non-JSON evidence is fine — just skip the hint.
    }
    return null;
  };

  return (
    <Card data-testid="ai-suggest-card">
      <div className="wb-card-head">
        <h3>AI 给我提的建议</h3>
        <span className="wb-count">{rows.length}</span>
      </div>
      {toast && (
        <div className="ai-suggest-toast" data-testid="ai-suggest-toast">
          {toast}
        </div>
      )}
      {rows.length === 0 ? (
        <p className="wb-empty" data-testid="ai-suggest-empty">
          暂无待裁决的 AI 建议。
        </p>
      ) : (
        rows.map((r) => {
          const acc = accepted[r.id];
          const isAccepted = !!acc && !acc.reversed;
          const canUndo = isAccepted && Date.now() - acc.acceptedAt < 5000;
          const hint = evidenceHint(r.evidence);
          return (
            <div
              key={r.id}
              className="ai-suggest-row"
              data-testid={`ai-suggest-row-${r.id}`}
              data-state={isAccepted ? 'accepted' : 'proposed'}
            >
              <div className="ai-suggest-row-head">
                <span className="ai-suggest-chip">
                  {r.agentType} · {r.action}
                </span>
                {isAccepted && <span className="ai-suggest-chip-ok">已采纳</span>}
              </div>
              <div className="ai-suggest-row-body">{r.summary}</div>
              {hint && (
                <div
                  className="ai-suggest-row-evidence"
                  data-testid={`ai-suggest-evidence-${r.id}`}
                >
                  {hint}
                </div>
              )}
              {rejectId === r.id ? (
                <div className="ai-suggest-reject" data-testid={`ai-suggest-reject-form-${r.id}`}>
                  <textarea
                    className="rainier-input"
                    rows={2}
                    placeholder="请填写驳回原因..."
                    value={rejectReason}
                    onChange={(e) => {
                      setRejectReason(e.target.value);
                      if (rejectError) setRejectError(null);
                    }}
                    data-testid={`ai-suggest-reject-reason-${r.id}`}
                  />
                  {rejectError && (
                    <div
                      className="ai-suggest-reject-err"
                      data-testid={`ai-suggest-reject-err-${r.id}`}
                    >
                      {rejectError}
                    </div>
                  )}
                  <div className="ai-suggest-row-actions">
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={cancelReject}
                      disabled={busyId === r.id}
                      data-testid={`ai-suggest-reject-cancel-${r.id}`}
                    >
                      取消
                    </Button>
                    <Button
                      type="button"
                      variant="primary"
                      onClick={() => void submitReject(r.id)}
                      disabled={busyId === r.id}
                      data-testid={`ai-suggest-reject-submit-${r.id}`}
                    >
                      确认驳回
                    </Button>
                  </div>
                </div>
              ) : (
                <div className="ai-suggest-row-actions">
                  {isAccepted ? (
                    canUndo ? (
                      <Button
                        type="button"
                        variant="secondary"
                        onClick={() => void onReverse(r.id)}
                        disabled={busyId === r.id}
                        data-testid={`ai-suggest-undo-${r.id}`}
                      >
                        撤销
                      </Button>
                    ) : null
                  ) : (
                    <>
                      <Button
                        type="button"
                        variant="secondary"
                        onClick={() => openReject(r.id)}
                        disabled={busyId === r.id}
                        data-testid={`ai-suggest-reject-${r.id}`}
                      >
                        驳回
                      </Button>
                      <Button
                        type="button"
                        variant="primary"
                        onClick={() => void onAccept(r.id)}
                        disabled={busyId === r.id}
                        data-testid={`ai-suggest-accept-${r.id}`}
                      >
                        采纳
                      </Button>
                    </>
                  )}
                </div>
              )}
            </div>
          );
        })
      )}
    </Card>
  );
}
