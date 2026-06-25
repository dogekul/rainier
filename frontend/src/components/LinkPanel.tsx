import { useEffect, useState } from 'react';
import { Button } from './ui/Button';
import { Input } from './ui/Input';
import { StatusChip } from './board';
import './LinkPanel.css';
import {
  createLink,
  deleteLink,
  listLinks,
  LINK_TYPE_LABELS,
  type EntityLink,
  type LinkTargetType,
  type LinkType,
} from '../api/link';

const LINK_TYPES: LinkType[] = ['PRD', 'DESIGN', 'DEFECT', 'TESTCASE', 'PR', 'OTHER'];

export interface LinkPanelProps {
  targetType: LinkTargetType;
  targetId: number;
}

/**
 * v0.0.31 关联面板 — a deliberately minimal external-artifact link list for a Story/Task: type chip +
 * label/url + delete, plus a one-line add row (type + url, label optional). Per the role cards'
 * 「关联必须极简否则没人维护」 trap, this stays url+type only — the PM system relates, never hosts.
 */
export function LinkPanel({ targetType, targetId }: LinkPanelProps) {
  const [links, setLinks] = useState<EntityLink[]>([]);
  const [linkType, setLinkType] = useState<LinkType>('PRD');
  const [label, setLabel] = useState('');
  const [url, setUrl] = useState('');

  const reload = () => {
    void listLinks(targetType, targetId)
      .then(setLinks)
      .catch(() => undefined);
  };

  useEffect(() => {
    let active = true;
    void listLinks(targetType, targetId)
      .then((l) => {
        if (active) setLinks(l);
      })
      .catch(() => undefined);
    return () => {
      active = false;
    };
  }, [targetType, targetId]);

  const add = async () => {
    if (!url.trim()) return;
    try {
      await createLink({ targetType, targetId, linkType, label: label.trim() || undefined, url: url.trim() });
      setLabel('');
      setUrl('');
      setLinkType('PRD');
      reload();
    } catch {
      // surfaced by the global axios error path; keep the typed values for retry.
    }
  };

  const remove = async (id: number) => {
    try {
      await deleteLink(id);
    } catch {
      // ignore — reload resyncs.
    }
    reload();
  };

  return (
    <div className="link-panel" data-testid="link-panel">
      <div className="link-panel-title">关联产物（{links.length}）</div>
      {links.length > 0 && (
        <ul className="link-panel-list">
          {links.map((l) => (
            <li key={l.id} className="link-panel-item" data-testid={`link-item-${l.id}`}>
              <StatusChip status="" tier="gray" label={LINK_TYPE_LABELS[l.linkType]} />
              <a
                href={l.url}
                target="_blank"
                rel="noreferrer"
                className="link-panel-url"
              >
                {l.label || l.url}
              </a>
              <button
                type="button"
                className="link-panel-delete"
                data-testid={`link-delete-${l.id}`}
                onClick={() => void remove(l.id)}
                aria-label="删除关联"
              >
                ✕
              </button>
            </li>
          ))}
        </ul>
      )}
      <div className="link-panel-add">
        <select
          className="rainier-select"
          data-testid="link-add-type"
          value={linkType}
          onChange={(e) => setLinkType(e.target.value as LinkType)}
        >
          {LINK_TYPES.map((t) => (
            <option key={t} value={t}>
              {LINK_TYPE_LABELS[t]}
            </option>
          ))}
        </select>
        <Input
          placeholder="链接 URL"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          data-testid="link-add-url"
        />
        <Input
          placeholder="说明（可选）"
          value={label}
          onChange={(e) => setLabel(e.target.value)}
          data-testid="link-add-label"
        />
        <Button type="button" onClick={add} disabled={!url.trim()} data-testid="link-add-btn">
          关联
        </Button>
      </div>
    </div>
  );
}
