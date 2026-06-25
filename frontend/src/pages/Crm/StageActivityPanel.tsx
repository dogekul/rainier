import { useCallback, useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import {
  addStageActivity,
  getStageDashboard,
  markStageActivityDone,
  skipStageActivity,
  type StageDashboardView,
} from '../../api/stageActivity';

/**
 * v0.0.90 D2 — 商机当前 stage 的「活动清单」+「关联产出物」整合面板。
 * 活动是过程动作（人在做什么），与产出物（结果文档）互补；纯任务清单视图，无门禁意义。
 */
export function StageActivityPanel({
  opportunityId,
  stageCode,
  stageLabel,
}: {
  opportunityId: number;
  stageCode: string;
  stageLabel?: string;
}) {
  const [data, setData] = useState<StageDashboardView | null>(null);
  const [loading, setLoading] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getStageDashboard(opportunityId, stageCode);
      setData(res);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }, [opportunityId, stageCode]);

  useEffect(() => {
    void load();
  }, [load]);

  const onAdd = useCallback(async () => {
    if (!newTitle.trim()) return;
    try {
      await addStageActivity(opportunityId, stageCode, { activityTitle: newTitle.trim() });
      setNewTitle('');
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }, [newTitle, opportunityId, stageCode, load]);

  const onDone = useCallback(
    async (aid: number) => {
      try {
        await markStageActivityDone(aid);
        await load();
      } catch (e) {
        setError(e instanceof Error ? e.message : String(e));
      }
    },
    [load],
  );

  const onSkip = useCallback(
    async (aid: number) => {
      try {
        await skipStageActivity(aid);
        await load();
      } catch (e) {
        setError(e instanceof Error ? e.message : String(e));
      }
    },
    [load],
  );

  return (
    <div className="opp-card" data-testid="opp-stage-activity-panel">
      <div className="opp-card-head">
        <strong>当前阶段活动 · {stageLabel ?? stageCode}</strong>
      </div>
      {error ? (
        <div className="opp-alert" data-testid="opp-stage-activity-error">
          {error}
        </div>
      ) : null}
      <div
        className="opp-form-row"
        style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 12 }}
      >
        <Input
          value={newTitle}
          onChange={(e) => setNewTitle(e.target.value)}
          placeholder="新增活动标题"
          data-testid="opp-stage-activity-new-title"
        />
        <Button
          type="button"
          onClick={() => void onAdd()}
          disabled={!newTitle.trim()}
          data-testid="opp-stage-activity-add"
        >
          添加
        </Button>
      </div>
      {loading ? (
        <div className="opp-muted">加载中…</div>
      ) : !data || data.activities.length === 0 ? (
        <div className="opp-muted" data-testid="opp-stage-activity-empty">
          暂无活动
        </div>
      ) : (
        <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
          {data.activities.map((a) => (
            <li
              key={a.id}
              data-testid={`opp-stage-activity-${a.id}`}
              style={{
                display: 'flex',
                gap: 8,
                alignItems: 'center',
                padding: '6px 0',
                borderBottom: '1px solid var(--rainier-border-soft, #eee)',
              }}
            >
              <span style={{ flex: 1 }}>
                {a.activityTitle}
                <span className="opp-muted" style={{ marginLeft: 8, fontSize: 12 }}>
                  · {a.status}
                </span>
              </span>
              {a.status === 'PENDING' ? (
                <>
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => void onDone(a.id)}
                    data-testid={`opp-stage-activity-done-${a.id}`}
                  >
                    完成
                  </Button>
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => void onSkip(a.id)}
                    data-testid={`opp-stage-activity-skip-${a.id}`}
                  >
                    跳过
                  </Button>
                </>
              ) : null}
            </li>
          ))}
        </ul>
      )}
      <div style={{ marginTop: 12 }}>
        <div className="opp-muted" style={{ fontSize: 12, marginBottom: 4 }}>
          本阶段产出物（{data?.artifacts.length ?? 0}）
        </div>
        {!data || data.artifacts.length === 0 ? (
          <div className="opp-muted" data-testid="opp-stage-activity-no-arts">
            暂无
          </div>
        ) : (
          <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
            {data.artifacts.map((art) => (
              <li
                key={art.id}
                data-testid={`opp-stage-activity-art-${art.id}`}
                style={{ padding: '4px 0' }}
              >
                {art.typeLabel}
                {art.title && art.title !== art.typeLabel ? ` · ${art.title}` : ''}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
