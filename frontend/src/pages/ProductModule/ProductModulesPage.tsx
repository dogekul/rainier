import { useCallback, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Pagination } from '../../components/ui/Pagination';
import {
  createProductModule,
  deleteProductModule,
  listProductModules,
  updateProductModule,
  type ProductModule,
} from '../../api/productModule';
import { usePaginated } from '../../hooks/usePaginated';
import { ProductModuleEditDrawer } from './ProductModuleEditDrawer';
import { ProductModuleTreeView } from './ProductModuleTreeView';

export function ProductModulesPage() {
  const fetcher = useCallback(
    async ({ page, size, search }: { page: number; size: number; search: string }) =>
      listProductModules({ page, size, search: search || undefined }),
    [],
  );
  // v0.0.13: size=100 keeps whole trees on one page in the common case; the tree view
  // degrades orphaned children to roots when a parent falls outside the page.
  const list = usePaginated<ProductModule>(fetcher, { initialSize: 100 });

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editing, setEditing] = useState<ProductModule | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<ProductModule | null>(null);

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2>产品模块</h2>
        <span className="rainier-spacer" />
        <Button
          type="button"
          onClick={() => {
            setEditing(null);
            setDrawerOpen(true);
          }}
          data-testid="product-module-new-btn"
        >
          新建产品模块
        </Button>
      </div>
      <Card>
        <ProductModuleTreeView
          modules={list.items}
          onEdit={(m) => {
            setEditing(m);
            setDrawerOpen(true);
          }}
          onDelete={(m) => setConfirmDelete(m)}
          onReparent={async (id, newParentId) => {
            // v0.0.98 — full-replace PUT semantics; carry over current fields the
            // backend update DTO requires so only parentId is meaningfully changed.
            const m = list.items.find((x) => x.id === id);
            if (!m) return;
            await updateProductModule(id, {
              code: m.code,
              name: m.name,
              description: m.description ?? undefined,
              status: m.status,
              parentId: newParentId,
              ownerUserId: m.ownerUserId,
            });
            void list.refetch();
          }}
        />
        <Pagination
          page={list.page}
          size={list.size}
          total={list.total}
          onPageChange={list.setPage}
        />
      </Card>
      <ProductModuleEditDrawer
        key={editing?.id ?? 'new'}
        open={drawerOpen}
        editing={editing}
        onClose={() => setDrawerOpen(false)}
        onCreate={async (body) => {
          await createProductModule(body);
          setDrawerOpen(false);
          void list.refetch();
        }}
        onUpdate={async (id, body) => {
          await updateProductModule(id, body);
          setDrawerOpen(false);
          void list.refetch();
        }}
      />
      <ConfirmDialog
        open={confirmDelete !== null}
        title="删除产品模块"
        message={`确认删除产品模块「${confirmDelete?.code ?? ''}」？`}
        onCancel={() => setConfirmDelete(null)}
        onConfirm={async () => {
          if (confirmDelete) await deleteProductModule(confirmDelete.id);
          setConfirmDelete(null);
          void list.refetch();
        }}
      />
    </div>
  );
}
