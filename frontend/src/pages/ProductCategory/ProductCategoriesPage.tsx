import { useCallback, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import {
  createProductCategory,
  deleteProductCategory,
  listProductCategories,
  updateProductCategory,
  type ProductCategory,
} from '../../api/productCategory';
import { usePaginated } from '../../hooks/usePaginated';
import { ProductCategoryEditDrawer } from './ProductCategoryEditDrawer';

export function ProductCategoriesPage() {
  const fetcher = useCallback(
    async ({ page, size, search }: { page: number; size: number; search: string }) =>
      listProductCategories({ page, size, search: search || undefined }),
    [],
  );
  const list = usePaginated<ProductCategory>(fetcher, { initialSize: 20 });

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editing, setEditing] = useState<ProductCategory | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<ProductCategory | null>(null);

  const columns: TableColumn<ProductCategory>[] = [
    { key: 'code', title: '编码', render: (c) => c.code },
    { key: 'name', title: '名称', render: (c) => c.name },
    { key: 'status', title: '状态', render: (c) => c.status },
    {
      key: 'owner',
      title: '负责人',
      render: (c) =>
        c.ownerName ? `${c.ownerName}（${c.ownerLoginName ?? ''}）` : '—',
    },
    {
      key: 'actions',
      title: '操作',
      render: (c) => (
        <>
          <Button
            type="button"
            variant="secondary"
            onClick={() => {
              setEditing(c);
              setDrawerOpen(true);
            }}
          >
            编辑
          </Button>{' '}
          <Button type="button" variant="secondary" onClick={() => setConfirmDelete(c)}>
            删除
          </Button>
        </>
      ),
    },
  ];

  return (
    <Card>
      <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
        <Button
          type="button"
          onClick={() => {
            setEditing(null);
            setDrawerOpen(true);
          }}
          data-testid="product-category-new-btn"
        >
          新建产品分类
        </Button>
      </div>
      <Table<ProductCategory> columns={columns} dataSource={list.items} rowKey="id" />
      <Pagination
        page={list.page}
        size={list.size}
        total={list.total}
        onPageChange={list.setPage}
      />
      <ProductCategoryEditDrawer
        key={editing?.id ?? 'new'}
        open={drawerOpen}
        editing={editing}
        onClose={() => setDrawerOpen(false)}
        onCreate={async (body) => {
          await createProductCategory(body);
          setDrawerOpen(false);
          void list.refetch();
        }}
        onUpdate={async (id, body) => {
          await updateProductCategory(id, body);
          setDrawerOpen(false);
          void list.refetch();
        }}
      />
      <ConfirmDialog
        open={confirmDelete !== null}
        title="删除产品分类"
        message={`确认删除产品分类「${confirmDelete?.code ?? ''}」？`}
        onCancel={() => setConfirmDelete(null)}
        onConfirm={async () => {
          if (confirmDelete) await deleteProductCategory(confirmDelete.id);
          setConfirmDelete(null);
          void list.refetch();
        }}
      />
    </Card>
  );
}
