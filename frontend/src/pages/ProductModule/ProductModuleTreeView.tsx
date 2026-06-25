import type { ProductModule } from '../../api/productModule';
import { Button } from '../../components/ui/Button';
import { StatusChip } from '../../components/board';
import { PRODUCT_MODULE_STATUS_LABELS } from '../../constants/labels';
import './ProductModuleTreeView.css';

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
        className="module-tree-row"
        data-depth={depth}
        style={{ paddingLeft: 8 + depth * 24 }}
      >
        <span className="module-tree-code">{node.code}</span>
        <span className="module-tree-name">{node.name}</span>
        <StatusChip status={node.status} label={PRODUCT_MODULE_STATUS_LABELS[node.status]} />
        <span className="module-tree-meta">{node.productName ?? ''}</span>
        <span className="module-tree-meta">
          {node.ownerName ? `${node.ownerName}（${node.ownerLoginName ?? ''}）` : '—'}
        </span>
        <span className="module-tree-actions">
          <Button type="button" variant="secondary" onClick={() => onEdit(node)}>
            编辑
          </Button>
          <Button type="button" variant="secondary" onClick={() => onDelete(node)}>
            删除
          </Button>
        </span>
      </div>
      {node.children.length > 0 && (
        <ul>
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
    <ul data-testid="product-module-tree" className="module-tree">
      {roots.map((root) => (
        <TreeNodeRow key={root.id} node={root} depth={0} onEdit={onEdit} onDelete={onDelete} />
      ))}
    </ul>
  );
}
