import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { useAuthStore } from '../../store/auth';
import { createDemand, PRIORITY_LABELS, type Demand, type Priority } from '../../api/demand';

const PRIORITY_OPTIONS: Priority[] = ['URGENT', 'HIGH', 'MEDIUM', 'LOW', 'LOWEST'];

/**
 * v0.0.26 — 业务方 lite 入口「提个诉求」. A deliberately minimal single-form page (飞书项目 quick-create
 * analog) for a non-technical user to file a 诉求: only 主题 (required) + 描述 (optional) + 优先级
 * (default 中). Submitter defaults to the logged-in user (read-only). Reuses POST /api/demands.
 */
export function DemandSubmitPage() {
  const user = useAuthStore((s) => s.user);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<Priority>('MEDIUM');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState<Demand | null>(null);

  const userId = user?.id ?? null;
  const canSubmit = title.trim().length > 0 && userId != null && !submitting;

  const reset = () => {
    setTitle('');
    setDescription('');
    setPriority('MEDIUM');
    setError(null);
    setSubmitted(null);
  };

  const submit = async () => {
    if (!canSubmit || userId == null) return;
    setSubmitting(true);
    setError(null);
    try {
      const created = await createDemand({
        title: title.trim(),
        description: description.trim() || undefined,
        submitterUserId: userId,
        priority,
      });
      setSubmitted(created);
    } catch {
      setError('提交失败，请稍后重试。');
    } finally {
      setSubmitting(false);
    }
  };

  if (submitted) {
    return (
      <Card data-testid="demand-submit-success">
        <h2 style={{ color: 'var(--rainier-status-green)' }}>已收到你的诉求 🎉</h2>
        <p>
          「{submitted.title}」（编号 #{submitted.id}）已提交，我们会尽快处理。
        </p>
        <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
          <Button type="button" onClick={reset} data-testid="demand-submit-again">
            再提一个
          </Button>
          <Link to="/pm/demands" className="rainier-empty-state-cta" data-testid="demand-submit-view">
            查看我提交的
          </Link>
        </div>
      </Card>
    );
  }

  return (
    <Card>
      <h2>提个诉求</h2>
      <p style={{ color: 'var(--rainier-color-text-2)', marginTop: -4 }} data-testid="demand-submit-submitter">
        提交人：{user?.name ?? user?.username ?? '我'}
      </p>

      {userId == null ? (
        <p style={{ color: 'var(--rainier-status-red)' }} data-testid="demand-submit-no-user">
          未找到您的用户档案，请联系管理员。
        </p>
      ) : (
        <>
          <Input
            label="主题"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            data-testid="demand-submit-title"
            placeholder="一句话描述你的诉求"
          />
          <Input
            label="描述（可选）"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            data-testid="demand-submit-desc"
          />
          <div className="rainier-form-group">
            <label className="rainier-input-wrap">
              <span className="rainier-input-label">优先级</span>
              <select
                className="rainier-select"
                value={priority}
                onChange={(e) => setPriority(e.target.value as Priority)}
                data-testid="demand-submit-priority"
              >
                {PRIORITY_OPTIONS.map((p) => (
                  <option key={p} value={p}>
                    {PRIORITY_LABELS[p]}
                  </option>
                ))}
              </select>
            </label>
          </div>
          {error && (
            <p style={{ color: 'var(--rainier-status-red)' }} data-testid="demand-submit-error">
              {error}
            </p>
          )}
          <Button type="button" onClick={submit} disabled={!canSubmit} data-testid="demand-submit-btn">
            {submitting ? '提交中…' : '提交诉求'}
          </Button>
        </>
      )}
    </Card>
  );
}
