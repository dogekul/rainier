import { useCallback, useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { listFeatures, type Feature } from '../../api/feature';
import { listProductModules } from '../../api/productModule';
import {
  createSprintFeature,
  deleteSprintFeature,
  getSprintFeatures,
  type SprintFeatureView,
} from '../../api/sprintFeature';

export interface SprintFeaturePanelProps {
  sprintId: number;
  /** The sprint's product; when set, the mount dropdown only offers that product's features. */
  productId?: number | null;
}

/**
 * v0.0.14: 关联功能 panel for a Sprint — lists linked features, mounts a feature (dropdown
 * filtered to the sprint's product when known), and unbinds.
 */
export function SprintFeaturePanel({ sprintId, productId }: SprintFeaturePanelProps) {
  const [linked, setLinked] = useState<SprintFeatureView[]>([]);
  const [options, setOptions] = useState<Feature[]>([]);
  const [selected, setSelected] = useState<number | ''>('');

  const refetch = useCallback(async () => {
    setLinked(await getSprintFeatures(sprintId));
  }, [sprintId]);

  useEffect(() => {
    void refetch();
  }, [refetch]);

  // Mount-dropdown candidates: all features, narrowed to the sprint's product (via its modules).
  useEffect(() => {
    let cancelled = false;
    void (async () => {
      const all = (await listFeatures({ size: 100 })).content;
      if (productId == null) {
        if (!cancelled) setOptions(all);
        return;
      }
      const modules = (await listProductModules({ productId, size: 100 })).content;
      const moduleIds = new Set(modules.map((m) => m.id));
      if (!cancelled) setOptions(all.filter((f) => moduleIds.has(f.moduleId)));
    })();
    return () => {
      cancelled = true;
    };
  }, [productId]);

  const linkedFeatureIds = new Set(linked.map((l) => l.featureId));
  const mountable = options.filter((f) => !linkedFeatureIds.has(f.id));

  return (
    <div
      style={{
        padding: 12,
        background: 'var(--rainier-color-bg-2)',
        borderRadius: 4,
        marginTop: 8,
      }}
      data-testid={`sprint-feature-panel-${sprintId}`}
    >
      <div style={{ display: 'flex', gap: 8, marginBottom: 12, alignItems: 'center' }}>
        <select
          className="rainier-treeselect-trigger"
          value={selected}
          onChange={(e) => setSelected(e.target.value === '' ? '' : Number(e.target.value))}
          data-testid="sprint-feature-mount-select"
        >
          <option value="">选择功能挂载</option>
          {mountable.map((f) => (
            <option key={f.id} value={f.id}>
              {f.name}（{f.code}）
            </option>
          ))}
        </select>
        <Button
          type="button"
          disabled={selected === ''}
          data-testid="sprint-feature-mount-btn"
          onClick={async () => {
            if (selected === '') return;
            await createSprintFeature({ sprintId, featureId: selected });
            setSelected('');
            void refetch();
          }}
        >
          挂载功能
        </Button>
      </div>
      <ul style={{ listStyle: 'none', margin: 0, padding: 0 }} data-testid="sprint-feature-list">
        {linked.map((f) => (
          <li
            key={f.linkId}
            data-testid={`sprint-feature-row-${f.featureId}`}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 12,
              padding: '4px 0',
            }}
          >
            <span style={{ fontFamily: 'monospace', fontSize: 12 }}>{f.code}</span>
            <span>{f.name}</span>
            <span style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>{f.status}</span>
            <Button
              type="button"
              variant="secondary"
              style={{ marginLeft: 'auto' }}
              data-testid={`sprint-feature-unbind-${f.featureId}`}
              onClick={async () => {
                await deleteSprintFeature(f.linkId);
                void refetch();
              }}
            >
              解绑
            </Button>
          </li>
        ))}
      </ul>
    </div>
  );
}
