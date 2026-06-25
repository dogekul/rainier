import { useCallback, useEffect, useState } from 'react';
import { DashboardCard, EmptyState, StatTiles, StatusChip } from '../../components/board';
import { Button } from '../../components/ui/Button';
import { Drawer } from '../../components/ui/Drawer';
import {
  AI_ERROR_STATUS_LABELS,
  fixAiError,
  listAiErrors,
  type AiError,
  type AiErrorStatus,
} from '../../api/aiErrors';
import { isElevated, useAuthStore } from '../../store/auth';
import type { StatusTier } from '../../utils/board';
import '../AiWorkLog/AiWorkLogsPage.css';

const STATUS_FILTERS: { value: '' | AiErrorStatus; label: string }[] = [
  { value: '', label: '全部' },
  { value: 'OPEN', label: '待修复' },
  { value: 'FIXED', label: '已修复' },
];

const STATUS_TIER: Record<AiErrorStatus, StatusTier> = {
  OPEN: 'red',
  FIXED: 'green',
};

/**
 * v0.0.73 (A9) — AI 错误公示板.
 *
 * <p>飞轮信任契约的反面证据：所有用户都能浏览 AI 失误（GET /api/ai/errors 是 all-users）；
 * 仅 admin 看见 "标记修复" 按钮（前端 isElevated 判断 + 后端 AdminPaths TIER_B 兜底）。
 * 字段：occurredAt / aiAction / errorDesc / affectedEntity / status。
 */
export function AiErrorsPage() {
  const currentUser = useAuthStore((s) => s.user);
  const admin = isElevated(currentUser);

  const [rows, setRows] = useState<AiError[]>([]);
  const [statusFilter, setStatusFilter] = useState<'' | AiErrorStatus>('');
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [fixId, setFixId] = useState<number | null>(null);
  const [fixAction, setFixAction] = useState('');
  const [fixError, setFixError] = useState<string | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    return listAiErrors({ status: statusFilter || undefined, size: 100 })
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

  const counts = { OPEN: 0, FIXED: 0 } as Record<AiErrorStatus, number>;
  for (const r of rows) counts[r.status] += 1;

  const openFix = (id: number) => {
    setFixId(id);
    setFixAction('');
    setFixError(null);
  };

  const submitFix = async () => {
    if (fixId == null) return;
    const action = fixAction.trim();
    if (!action) {
      setFixError('请填写修复说明');
      return;
    }
    setBusyId(fixId);
    try {
      await fixAiError(fixId, action);
      setFixId(null);
      await load();
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2>AI 错误公示板</h2>
        <select
          data-testid="ai-error-status-filter"
          className="rainier-select"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as '' | AiErrorStatus)}
        >
          {STATUS_FILTERS.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
      </div>

      <StatTiles
        testId="ai-error-summary"
        tiles={[
          { label: '待修复', value: counts.OPEN, tier: counts.OPEN > 0 ? 'red' : 'gray' },
          { label: '已修复', value: counts.FIXED, tier: 'green' },
        ]}
      />

      <DashboardCard title="AI 错误（飞轮信任契约 — 反面证据公开化）" testId="ai-error-list">
        {!loading && rows.length === 0 ? (
          <EmptyState message="暂无 AI 错误记录。" testId="ai-error-empty" />
        ) : (
          <div className="ai-list">
            {rows.map((r) => {
              const affected = r.affectedEntityType
                ? `${r.affectedEntityType}${r.affectedEntityId != null ? '#' + r.affectedEntityId : ''}`
                : '—';
              return (
                <div key={r.id} className="ai-row" data-testid={`ai-error-row-${r.id}`}>
                  <div className="ai-row-main">
                    <StatusChip
                      status={r.status}
                      tier={STATUS_TIER[r.status]}
                      label={AI_ERROR_STATUS_LABELS[r.status]}
                    />
                    <span className="ai-row-agent">
                      {r.occurredAt} · {r.aiAction}
                    </span>
                    <span className="ai-row-summary">{r.errorDesc}</span>
                    <span className="rainier-spacer" />
                    <span className="ai-row-decider">{affected}</span>
                    {admin && r.status === 'OPEN' ? (
                      <Button
                        type="button"
                        variant="primary"
                        disabled={busyId === r.id}
                        onClick={() => openFix(r.id)}
                        data-testid={`ai-error-fix-${r.id}`}
                      >
                        标记修复
                      </Button>
                    ) : null}
                  </div>
                  {r.status === 'FIXED' && r.fixAction ? (
                    <div className="ai-row-evidence">修复：{r.fixAction}</div>
                  ) : null}
                </div>
              );
            })}
          </div>
        )}
      </DashboardCard>

      <Drawer
        open={fixId !== null}
        title="标记 AI 错误为已修复"
        onClose={() => setFixId(null)}
        footer={
          <>
            <Button type="button" variant="secondary" onClick={() => setFixId(null)}>
              取消
            </Button>
            <Button
              type="button"
              variant="primary"
              disabled={busyId === fixId}
              onClick={() => void submitFix()}
              data-testid="ai-error-fix-submit"
            >
              确认修复
            </Button>
          </>
        }
      >
        <div className="rainier-form-group">
          <label className="rainier-form-label" htmlFor="ai-error-fix-action">
            修复说明（必填）
          </label>
          <textarea
            id="ai-error-fix-action"
            className="rainier-input"
            rows={5}
            value={fixAction}
            onChange={(e) => {
              setFixAction(e.target.value);
              if (fixError) setFixError(null);
            }}
            data-testid="ai-error-fix-action"
          />
        </div>
        {fixError && <div className="rainier-error-banner">{fixError}</div>}
      </Drawer>
    </div>
  );
}
