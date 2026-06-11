import { useEffect, useState } from 'react';
import { getFeatureSprints, type FeatureSprintView } from '../../api/sprintFeature';

export interface FeatureSprintsPanelProps {
  featureId: number;
}

/** v0.0.14: shows the iterations (sprints) a feature is linked into. */
export function FeatureSprintsPanel({ featureId }: FeatureSprintsPanelProps) {
  const [sprints, setSprints] = useState<FeatureSprintView[]>([]);

  useEffect(() => {
    let cancelled = false;
    void getFeatureSprints(featureId).then((res) => {
      if (!cancelled) setSprints(res);
    });
    return () => {
      cancelled = true;
    };
  }, [featureId]);

  return (
    <div
      style={{
        padding: 12,
        background: 'var(--rainier-color-bg-2)',
        borderRadius: 4,
        marginTop: 8,
      }}
      data-testid={`feature-sprints-panel-${featureId}`}
    >
      <div style={{ fontSize: 12, color: 'var(--rainier-color-text-2)', marginBottom: 8 }}>
        所在迭代
      </div>
      {sprints.length === 0 ? (
        <div style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>暂无关联迭代</div>
      ) : (
        <ul style={{ listStyle: 'none', margin: 0, padding: 0 }}>
          {sprints.map((s) => (
            <li key={s.linkId} data-testid={`feature-sprint-row-${s.sprintId}`}>
              <span style={{ fontFamily: 'monospace', fontSize: 12 }}>{s.code}</span>{' '}
              <span>{s.name}</span>{' '}
              <span style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>{s.status}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
