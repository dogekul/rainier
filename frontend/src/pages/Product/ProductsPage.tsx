import { useCallback, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import {
  createProduct,
  deleteProduct,
  listProducts,
  updateProduct,
  type Product,
} from '../../api/product';
import { usePaginated } from '../../hooks/usePaginated';
import { StatusChip } from '../../components/board';
import { ProductEditDrawer } from './ProductEditDrawer';

export function ProductsPage() {
  const fetcher = useCallback(
    async ({ page, size, search }: { page: number; size: number; search: string }) =>
      listProducts({ page, size, search: search || undefined }),
    [],
  );
  const list = usePaginated<Product>(fetcher, { initialSize: 20 });

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editing, setEditing] = useState<Product | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<Product | null>(null);

  const columns: TableColumn<Product>[] = [
    { key: 'code', title: '编码', render: (p) => p.code },
    { key: 'name', title: '名称', render: (p) => p.name },
    { key: 'status', title: '状态', render: (p) => <StatusChip status={p.status} /> },
    {
      key: 'owner',
      title: '负责人',
      render: (p) =>
        p.ownerName ? `${p.ownerName}（${p.ownerLoginName ?? ''}）` : '—',
    },
    {
      key: 'actions',
      title: '操作',
      render: (p) => (
        <>
          <Button
            type="button"
            variant="secondary"
            onClick={() => {
              setEditing(p);
              setDrawerOpen(true);
            }}
          >
            编辑
          </Button>{' '}
          <Button type="button" variant="secondary" onClick={() => setConfirmDelete(p)}>
            删除
          </Button>
        </>
      ),
    },
  ];

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2>产品</h2>
        <span className="rainier-spacer" />
        <Button
          type="button"
          onClick={() => {
            setEditing(null);
            setDrawerOpen(true);
          }}
          data-testid="product-new-btn"
        >
          新建产品
        </Button>
      </div>
      <Card>
        <Table<Product> columns={columns} dataSource={list.items} rowKey="id" />
        <Pagination
          page={list.page}
          size={list.size}
          total={list.total}
          onPageChange={list.setPage}
        />
      </Card>
      <ProductEditDrawer
        key={editing?.id ?? 'new'}
        open={drawerOpen}
        editing={editing}
        onClose={() => setDrawerOpen(false)}
        onCreate={async (body) => {
          await createProduct(body);
          setDrawerOpen(false);
          void list.refetch();
        }}
        onUpdate={async (id, body) => {
          await updateProduct(id, body);
          setDrawerOpen(false);
          void list.refetch();
        }}
      />
      <ConfirmDialog
        open={confirmDelete !== null}
        title="删除产品"
        message={`确认删除产品「${confirmDelete?.code ?? ''}」？`}
        onCancel={() => setConfirmDelete(null)}
        onConfirm={async () => {
          if (confirmDelete) await deleteProduct(confirmDelete.id);
          setConfirmDelete(null);
          void list.refetch();
        }}
      />
    </div>
  );
}
