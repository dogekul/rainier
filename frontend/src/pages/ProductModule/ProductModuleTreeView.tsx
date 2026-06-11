import type { ProductModule } from '../../api/productModule';
import { Button } from '../../components/ui/Button';

export interface ModuleTreeNode extends ProductModule {
  children: ModuleTreeNode[];
}

/**
 * Assembles a flat module list into a forest. A node whose parent is missing from the
 * current page (pagination cut) degrades gracefully to a root instead of disappearing.
 */
export function buildModuleTree(flat: ProductModule[]): ModuleTreeNode[] {
  const map = new Map<number, ModuleTreeNode>();
  flat.forEach((m) => map.set(m.id, { ...m, children: [] }));
  const roots: ModuleTreeNode[] = [];
  map.forEach((node) => {
    if (node.parentId != null && map.has(node.parentId)) {
      map.get(node.parentId)!.children.push(node);
    } else {
      roots.push(node);
    }
  });
  return roots;
}

interface TreeNodeRowProps {
  node: ModuleTreeNode;
  depth: number;
  onEdit: (m: ProductModule) => void;
  onDelete: (m: ProductModule) => void;
}

function TreeNodeRow({ node, depth, onEdit, onDelete }: TreeNodeRowProps) {
  return (
    <li data-testid={`module-tree-node-${node.id}`}>
      <div
        className="rainier-module-tree-row"
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          padding: '6px 8px',
          paddingLeft: 8 + depth * 24,
          borderBottom: '1px solid var(--rainier-color-border, #eee)',
        }}
      >
        <span style={{ fontFamily: 'monospace', fontSize: 12 }}>{node.code}</span>
        <span style={{ fontWeight: depth === 0 ? 600 : 400 }}>{node.name}</span>
        <span style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>{node.status}</span>
        <span style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
          {node.productName ? `${node.productName}` : ''}
        </span>
        <span style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
          {node.ownerName ? `${node.ownerName}（${node.ownerLoginName ?? ''}）` : '—'}
        </span>
        <span style={{ marginLeft: 'auto', display: 'flex', gap: 8 }}>
          <Button type="button" variant="secondary" onClick={() => onEdit(node)}>
            编辑
          </Button>
          <Button type="button" variant="secondary" onClick={() => onDelete(node)}>
            删除
          </Button>
        </span>
      </div>
      {node.children.length > 0 && (
        <ul style={{ listStyle: 'none', margin: 0, padding: 0 }}>
          {node.children.map((child) => (
            <TreeNodeRow
              key={child.id}
              node={child}
              depth={depth + 1}
              onEdit={onEdit}
              onDelete={onDelete}
            />
          ))}
        </ul>
      )}
    </li>
  );
}

export interface ProductModuleTreeViewProps {
  modules: ProductModule[];
  onEdit: (m: ProductModule) => void;
  onDelete: (m: ProductModule) => void;
}

/** v0.0.13: nested UL/LI module forest with per-depth indent (spec: tree view ≥ 2 levels). */
export function ProductModuleTreeView({ modules, onEdit, onDelete }: ProductModuleTreeViewProps) {
  const roots = buildModuleTree(modules);
  return (
    <ul
      data-testid="product-module-tree"
      style={{ listStyle: 'none', margin: 0, padding: 0 }}
    >
      {roots.map((root) => (
        <TreeNodeRow key={root.id} node={root} depth={0} onEdit={onEdit} onDelete={onDelete} />
      ))}
    </ul>
  );
}
