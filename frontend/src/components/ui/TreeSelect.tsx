import { useMemo, useState } from 'react';
import './TreeSelect.css';

export interface TreeNode {
  id: string;
  name: string;
  parentId: string | null;
}

export interface TreeSelectProps {
  value: string | null;
  nodes: TreeNode[];
  onChange: (id: string | null) => void;
  placeholder?: string;
  allowClear?: boolean;
}

interface BuiltNode extends TreeNode {
  children: BuiltNode[];
  depth: number;
}

function buildTree(flat: TreeNode[]): BuiltNode[] {
  const map = new Map<string, BuiltNode>();
  flat.forEach((n) => map.set(n.id, { ...n, children: [], depth: 0 }));
  const roots: BuiltNode[] = [];
  map.forEach((n) => {
    if (n.parentId && map.has(n.parentId)) {
      const p = map.get(n.parentId)!;
      n.depth = p.depth + 1;
      p.children.push(n);
    } else {
      roots.push(n);
    }
  });
  return roots;
}

function flatten(roots: BuiltNode[], depth = 0): BuiltNode[] {
  const out: BuiltNode[] = [];
  roots.forEach((r) => {
    out.push({ ...r, depth });
    out.push(...flatten(r.children, depth + 1));
  });
  return out;
}

/** Dropdown tree picker — flat input, hierarchical render. */
export function TreeSelect({
  value,
  nodes,
  onChange,
  placeholder = '请选择',
  allowClear = true,
}: TreeSelectProps) {
  const [open, setOpen] = useState(false);
  const tree = useMemo(() => flatten(buildTree(nodes)), [nodes]);
  const selected = nodes.find((n) => n.id === value) ?? null;

  return (
    <div className="rainier-treeselect" data-testid="rainier-treeselect">
      <button
        type="button"
        className="rainier-treeselect-trigger"
        onClick={() => setOpen((o) => !o)}
      >
        {selected ? selected.name : <span className="rainier-treeselect-placeholder">{placeholder}</span>}
      </button>
      {open && (
        <div className="rainier-treeselect-panel" role="listbox" data-testid="rainier-treeselect-panel">
          {allowClear && (
            <div
              className="rainier-treeselect-item"
              data-testid="rainier-treeselect-item"
              onClick={() => {
                onChange(null);
                setOpen(false);
              }}
            >
              <span className="rainier-treeselect-item-name">— 无父节点 —</span>
            </div>
          )}
          {tree.map((n) => (
            <div
              key={n.id}
              className="rainier-treeselect-item"
              data-testid="rainier-treeselect-item"
              style={{ paddingLeft: 12 + n.depth * 16 }}
              onClick={() => {
                onChange(n.id);
                setOpen(false);
              }}
            >
              <span className="rainier-treeselect-item-name">{n.name}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
