import { useState } from 'react';
import { Button } from '../ui/Button';
import { Drawer } from '../ui/Drawer';
import { Input } from '../ui/Input';
import {
  ARTIFACT_TYPE_LABELS,
  createOpportunityArtifact,
  isLinkArtifact,
  type ArtifactType,
} from '../../api/opportunityArtifact';

/**
 * v0.0.96 (D8) — 共用抽屉：「推进时补充必需产出物」。
 *
 * 抽离自 PresaleFlow / DeliveryFlow 原本一字不差的 ~100 行表单 (suppOpp/suppData/suppSaving)：
 *   - 报告类 (非 link)：标题(可空) + 正文 (markdown)
 *   - 链接类 (link)：多份 URL，可增删
 *   - 提交：为每个缺件类型逐个 createOpportunityArtifact，全部成功后回调 onAdvance(id)
 *
 * Props 由调用方控制：`opportunityId` 决定打开/关闭(null 表示关)；`missingTypes` 是缺失列表；
 * `testIdPrefix` 让两个页面保留原本的 data-testid 命名空间（presale-supp-* / delivery-supp-*）。
 */
export interface ArtifactSupplementDrawerProps {
  opportunityId: number | null;
  missingTypes: ArtifactType[];
  /** Banner text above the form, e.g. "推进前需补齐以下必需产出物：". */
  message?: string;
  /** data-testid prefix; existing pages pass "presale-supp" / "delivery-supp" to keep tests stable. */
  testIdPrefix: string;
  onClose: () => void;
  /** Called after all missing artifacts are persisted; the parent then advances the opportunity. */
  onAdvance: (opportunityId: number) => void | Promise<void>;
}

type RowData = { title: string; content: string; links: string[] };

function initRow(t: ArtifactType): RowData {
  return { title: '', content: '', links: isLinkArtifact(t) ? [''] : [] };
}

function initData(types: ArtifactType[]): Record<string, RowData> {
  const data: Record<string, RowData> = {};
  types.forEach((t) => {
    data[t] = initRow(t);
  });
  return data;
}

export function ArtifactSupplementDrawer({
  opportunityId,
  missingTypes,
  message,
  testIdPrefix,
  onClose,
  onAdvance,
}: ArtifactSupplementDrawerProps) {
  // Re-init form whenever the set of missing types changes (i.e. a new advance attempt opens the drawer).
  // We key the drawer state by joined types so each open starts clean.
  const typesKey = missingTypes.join(',');
  const [keyRef, setKeyRef] = useState<string>('');
  const [data, setData] = useState<Record<string, RowData>>({});
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  if (opportunityId != null && typesKey !== keyRef) {
    setKeyRef(typesKey);
    setData(initData(missingTypes));
    setError(null);
  }

  const setField = (type: string, field: 'title' | 'content', value: string) =>
    setData((d) => ({ ...d, [type]: { ...d[type], [field]: value } }));
  const setLink = (type: string, idx: number, value: string) =>
    setData((d) => {
      const links = [...(d[type]?.links ?? [])];
      links[idx] = value;
      return { ...d, [type]: { ...d[type], links } };
    });
  const addLink = (type: string) =>
    setData((d) => ({
      ...d,
      [type]: { ...d[type], links: [...(d[type]?.links ?? []), ''] },
    }));
  const removeLink = (type: string, idx: number) =>
    setData((d) => {
      const links = (d[type]?.links ?? []).filter((_, i) => i !== idx);
      return { ...d, [type]: { ...d[type], links: links.length ? links : [''] } };
    });

  const submit = async () => {
    if (opportunityId == null) return;
    for (const t of missingTypes) {
      const d = data[t];
      if (!d) {
        setError(`请填写《${ARTIFACT_TYPE_LABELS[t]}》`);
        return;
      }
      if (isLinkArtifact(t)) {
        if (!d.links.some((l) => l.trim())) {
          setError(`请为《${ARTIFACT_TYPE_LABELS[t]}》填写至少一条链接`);
          return;
        }
      } else if (!d.content.trim()) {
        setError(`请填写《${ARTIFACT_TYPE_LABELS[t]}》的正文`);
        return;
      }
    }
    setError(null);
    setSaving(true);
    try {
      for (const t of missingTypes) {
        const d = data[t];
        if (isLinkArtifact(t)) {
          for (const l of d.links.map((x) => x.trim()).filter(Boolean)) {
            await createOpportunityArtifact(opportunityId, { type: t, link: l });
          }
        } else {
          await createOpportunityArtifact(opportunityId, {
            type: t,
            title: d.title.trim() || undefined,
            content: d.content,
          });
        }
      }
      const id = opportunityId;
      // Parent decides what to do after persistence (close + advance, optionally with a decision).
      await onAdvance(id);
    } catch (e) {
      const err = e as { response?: { data?: { message?: string } }; message?: string };
      setError(err?.response?.data?.message ?? err?.message ?? '提交失败');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Drawer open={opportunityId != null} title="补充产出物并推进" onClose={onClose}>
      <div style={{ fontSize: 12, color: 'var(--rainier-color-text-2)', marginBottom: 8 }}>
        {message ?? '推进前需补齐以下必需产出物：'}
      </div>
      {missingTypes.map((t) => (
        <div
          key={t}
          data-testid={`${testIdPrefix}-${t}`}
          style={{
            border: '1px solid var(--rainier-border)',
            borderRadius: 6,
            padding: '8px 10px',
            marginBottom: 8,
          }}
        >
          <div style={{ fontWeight: 600, fontSize: 13, marginBottom: 6 }}>
            {ARTIFACT_TYPE_LABELS[t]}
            {isLinkArtifact(t) ? '（链接，可添加多份）' : '（报告）'}
          </div>
          {isLinkArtifact(t) ? (
            <>
              {(data[t]?.links ?? ['']).map((l, idx) => (
                <div key={idx} style={{ display: 'flex', gap: 6, marginBottom: 6 }}>
                  <input
                    className="rainier-input"
                    style={{ flex: 1 }}
                    placeholder="链接 URL"
                    value={l}
                    onChange={(e) => setLink(t, idx, e.target.value)}
                    data-testid={`${testIdPrefix}-link-${t}-${idx}`}
                  />
                  {(data[t]?.links?.length ?? 0) > 1 && (
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={() => removeLink(t, idx)}
                      data-testid={`${testIdPrefix}-rmlink-${t}-${idx}`}
                    >
                      删除
                    </Button>
                  )}
                </div>
              ))}
              <Button
                type="button"
                variant="secondary"
                onClick={() => addLink(t)}
                data-testid={`${testIdPrefix}-addlink-${t}`}
              >
                + 添加链接
              </Button>
            </>
          ) : (
            <>
              <Input
                label="标题（可空）"
                value={data[t]?.title ?? ''}
                onChange={(e) => setField(t, 'title', e.target.value)}
                data-testid={`${testIdPrefix}-title-${t}`}
              />
              <div style={{ marginBottom: 8 }}>
                <label className="rainier-form-label">正文（支持 Markdown）</label>
                <textarea
                  className="rainier-input"
                  style={{ width: '100%', minHeight: 70, padding: 8, boxSizing: 'border-box' }}
                  value={data[t]?.content ?? ''}
                  onChange={(e) => setField(t, 'content', e.target.value)}
                  data-testid={`${testIdPrefix}-content-${t}`}
                />
              </div>
            </>
          )}
        </div>
      ))}
      {error && (
        <div className="rainier-error-banner" data-testid={`${testIdPrefix}-error`}>
          {error}
        </div>
      )}
      <div className="rainier-form-footer">
        <Button type="button" variant="secondary" onClick={onClose}>
          取消
        </Button>
        <Button
          type="button"
          disabled={saving}
          onClick={() => void submit()}
          data-testid={`${testIdPrefix}-save`}
        >
          提交并推进
        </Button>
      </div>
    </Drawer>
  );
}
