import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchAiErrorOverdueCount } from '../api/aiErrors';
import './AiErrorOverdueBanner.css';

const POLL_MS = 30_000;
const THRESHOLD_HOURS = 24;

/**
 * v0.0.104 (F5) — 全局顶部红条. 30s 轮询 GET /api/ai/errors/overdue-count?hours=24，
 * 仅当 count > 0 时渲染。点击「查看公示板」跳 /ai/errors。失败静默（与 NotificationBell 同策略）。
 */
export function AiErrorOverdueBanner() {
  const [count, setCount] = useState(0);

  const refresh = useCallback(async () => {
    try {
      const res = await fetchAiErrorOverdueCount(THRESHOLD_HOURS);
      setCount(res.count);
    } catch {
      // Non-fatal — leave existing state alone.
    }
  }, []);

  useEffect(() => {
    void refresh();
    const t = window.setInterval(() => {
      void refresh();
    }, POLL_MS);
    return () => window.clearInterval(t);
  }, [refresh]);

  if (count <= 0) return null;

  return (
    <div
      className="rainier-ai-overdue-banner"
      role="alert"
      data-testid="ai-error-overdue-banner"
    >
      <span className="rainier-ai-overdue-banner-text">
        <strong data-testid="ai-error-overdue-banner-count">{count}</strong> 个 AI 错误已超过{' '}
        {THRESHOLD_HOURS}h 未处理
      </span>
      <Link
        to="/ai/errors"
        className="rainier-ai-overdue-banner-link"
        data-testid="ai-error-overdue-banner-link"
      >
        查看公示板 →
      </Link>
    </div>
  );
}
