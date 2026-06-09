import { useCallback, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import {
  createProductModule,
  deleteProductModule,
  listProductModules,
  updateProductModule,
  type ProductModule,
} from '../../api/productModule';
import { usePaginated } from '../../hooks/usePaginated';
import { ProductModuleEditDrawer } from './ProductModuleEditDrawer';

export function ProductModulesPage() {
  const fetcher = useCallback(
    async ({ page, size, search }: { page: number; size: number; search: string }) =>
      listProductModules({ page, size, search: search || undefined }),
    [],
  );
  const list = usePaginated<ProductModule>(fetcher, { initialSize: 20 });

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editing, setEditing] = useState<ProductModule | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<ProductModule | null>(null);

  const columns: TableColumn<ProductModule>[] = [
    { key: 'code', title: '编码', render: (m) => m.code },
    { key: 'name', title: '名称', render: (m) => m.name },
    {
      key: 'product',
      title: '所属产品',
      render: (m) =>
        m.productName ? `${m.productName}（${m.productCode ?? ''}）` : '—',
    },
    { key: 'status', title: '状态', render: (m) => m.status },
    {
      key: 'owner',
      title: '负责人',
      render: (m) =>
        m.ownerName ? `${m.ownerName}（${m.ownerLoginName ?? ''}）` : '—',
    },
    {
      key: 'actions',
      title: '操作',
      render: (m) => (
        <>
          <Button
            type="button"
            variant="secondary"
            onClick={() => {
              setEditing(m);
              setDrawerOpen(true);
            }}
          >
            编辑
          </Button>{' '}
          <Button type="button" variant="secondary" onClick={() => setConfirmDelete(m)}>
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
          data-testid="product-module-new-btn"
        >
          新建产品模块
        </Button>
      </div>
      <Table<ProductModule> columns={columns} dataSource={list.items} rowKey="id" />
      <Pagination
        page={list.page}
        size={list.size}
        total={list.total}
        onPageChange={list.setPage}
      />
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
    </Card>
  );
}
