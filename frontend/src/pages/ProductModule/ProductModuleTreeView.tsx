import { useState, type DragEvent } from 'react';
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

/** v0.0.98 — descendant id set incl. the node itself, used to block self/cycle drops. */
function collectDescendantIds(node: ModuleTreeNode, acc: Set<number>): Set<number> {
  acc.add(node.id);
  node.children.forEach((c) => collectDescendantIds(c, acc));
  return acc;
}

const DND_MIME = 'application/x-rainier-module';

interface DragPayload {
  id: number;
  productId: number;
  descendantIds: number[];
}

interface TreeNodeRowProps {
  node: ModuleTreeNode;
  depth: number;
  onEdit: (m: ProductModule) => void;
  onDelete: (m: ProductModule) => void;
  dragPayload: DragPayload | null;
  onDragStart: (n: ModuleTreeNode) => void;
  onDragEnd: () => void;
  onDropOn: (target: ModuleTreeNode) => void;
  hoverTargetId: number | null;
  setHoverTargetId: (id: number | null) => void;
}

function TreeNodeRow({
  node,
  depth,
  onEdit,
  onDelete,
  dragPayload,
  onDragStart,
  onDragEnd,
  onDropOn,
  hoverTargetId,
  setHoverTargetId,
}: TreeNodeRowProps) {
  const isInvalidTarget =
    dragPayload != null &&
    (dragPayload.productId !== node.productId ||
      dragPayload.descendantIds.includes(node.id));

  const handleDragOver = (e: DragEvent<HTMLDivElement>) => {
    if (dragPayload == null) return;
    if (isInvalidTarget) {
      e.dataTransfer.dropEffect = 'none';
      return;
    }
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    if (hoverTargetId !== node.id) setHoverTargetId(node.id);
  };
  const handleDragLeave = () => {
    if (hoverTargetId === node.id) setHoverTargetId(null);
  };
  const handleDrop = (e: DragEvent<HTMLDivElement>) => {
    if (dragPayload == null || isInvalidTarget) return;
    e.preventDefault();
    e.stopPropagation();
    setHoverTargetId(null);
    onDropOn(node);
  };

  const isHover = hoverTargetId === node.id && !isInvalidTarget;

  return (
    <li data-testid={`module-tree-node-${node.id}`}>
      <div
        className={`module-tree-row${isHover ? ' module-tree-row--drop-target' : ''}`}
        data-depth={depth}
        style={{ paddingLeft: 8 + depth * 24 }}
        draggable
        onDragStart={(e) => {
          e.stopPropagation();
          e.dataTransfer.effectAllowed = 'move';
          e.dataTransfer.setData(DND_MIME, String(node.id));
          onDragStart(node);
        }}
        onDragEnd={() => {
          setHoverTargetId(null);
          onDragEnd();
        }}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
      >
        <span className="module-tree-drag-handle" aria-hidden="true">⋮⋮</span>
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
              dragPayload={dragPayload}
              onDragStart={onDragStart}
              onDragEnd={onDragEnd}
              onDropOn={onDropOn}
              hoverTargetId={hoverTargetId}
              setHoverTargetId={setHoverTargetId}
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
  /** v0.0.98 — drag-drop reparent. newParentId === null means top-level. */
  onReparent?: (id: number, newParentId: number | null) => void;
}

/**
 * v0.0.13: nested UL/LI module forest with per-depth indent (spec: tree view ≥ 2 levels).
 * v0.0.98: HTML5 drag-and-drop reparent. Cross-product drops and self/descendant drops
 * are short-circuited in the UI (effect=none); backend (product-module update) has the
 * cross-product + cycle + depth guards as defense-in-depth.
 */
export function ProductModuleTreeView({
  modules,
  onEdit,
  onDelete,
  onReparent,
}: ProductModuleTreeViewProps) {
  const roots = buildModuleTree(modules);
  const [dragPayload, setDragPayload] = useState<DragPayload | null>(null);
  const [hoverTargetId, setHoverTargetId] = useState<number | null>(null);
  const [rootHover, setRootHover] = useState(false);

  const handleDragStart = (n: ModuleTreeNode) => {
    setDragPayload({
      id: n.id,
      productId: n.productId,
      descendantIds: Array.from(collectDescendantIds(n, new Set<number>())),
    });
  };
  const handleDragEnd = () => {
    setDragPayload(null);
    setHoverTargetId(null);
    setRootHover(false);
  };
  const handleDropOn = (target: ModuleTreeNode) => {
    if (dragPayload == null || !onReparent) return;
    if (target.id === dragPayload.id) return;
    onReparent(dragPayload.id, target.id);
  };

  // Root dropzone — drop here to clear parent (become top-level).
  const handleRootDragOver = (e: DragEvent<HTMLDivElement>) => {
    if (dragPayload == null) return;
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    if (!rootHover) setRootHover(true);
  };
  const handleRootDrop = (e: DragEvent<HTMLDivElement>) => {
    if (dragPayload == null || !onReparent) return;
    e.preventDefault();
    setRootHover(false);
    // No-op if already top-level (parentId already null).
    const dragged = modules.find((m) => m.id === dragPayload.id);
    if (dragged && dragged.parentId == null) return;
    onReparent(dragPayload.id, null);
  };

  return (
    <>
      <div
        data-testid="product-module-root-dropzone"
        className={`module-tree-root-dropzone${rootHover ? ' module-tree-root-dropzone--hover' : ''}`}
        onDragOver={handleRootDragOver}
        onDragLeave={() => setRootHover(false)}
        onDrop={handleRootDrop}
      >
        拖到此处成为顶级
      </div>
      <ul data-testid="product-module-tree" className="module-tree">
        {roots.map((root) => (
          <TreeNodeRow
            key={root.id}
            node={root}
            depth={0}
            onEdit={onEdit}
            onDelete={onDelete}
            dragPayload={dragPayload}
            onDragStart={handleDragStart}
            onDragEnd={handleDragEnd}
            onDropOn={handleDropOn}
            hoverTargetId={hoverTargetId}
            setHoverTargetId={setHoverTargetId}
          />
        ))}
      </ul>
    </>
  );
}
