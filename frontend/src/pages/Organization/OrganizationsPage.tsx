import { useCallback, useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Input } from '../../components/ui/Input';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import {
  createOrganization,
  deleteOrganization,
  listOrganizations,
  type Organization,
  type OrganizationCreate,
  type OrganizationType,
  updateOrganization,
} from '../../api/organization';
import { usePaginated } from '../../hooks/usePaginated';
import { OrganizationEditDrawer } from './EditDrawer';

export function OrganizationsPage() {
  const fetcher = useCallback(
    async ({ page, size, search }: { page: number; size: number; search: string }) =>
      listOrganizations({ page, size, search: search || undefined }),
    [],
  );
  const list = usePaginated<Organization>(fetcher, { initialSize: 20 });
  const [searchInput, setSearchInput] = useState('');
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editing, setEditing] = useState<Organization | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<Organization | null>(null);

  useEffect(() => {
    setSearchInput(list.search);
  }, [list.search]);

  const columns: TableColumn<Organization>[] = [
    { key: 'code', title: '编码', render: (r) => r.code },
    { key: 'name', title: '名称', render: (r) => r.name },
    { key: 'type', title: '类型', render: (r) => r.type },
    { key: 'wholeName', title: '全路径', render: (r) => r.wholeName ?? '' },
    {
      key: 'isPmo',
      title: 'PMO',
      render: (r) => (r.isPmo ? '是' : '—'),
    },
    {
      key: 'actions',
      title: '操作',
      render: (r) => (
        <>
          <Button
            type="button"
            variant="secondary"
            onClick={() => {
              setEditing(r);
              setDrawerOpen(true);
            }}
          >
            编辑
          </Button>{' '}
          <Button type="button" variant="secondary" onClick={() => setConfirmDelete(r)}>
            删除
          </Button>
        </>
      ),
    },
  ];

  return (
    <Card>
      <div style={{ display: 'flex', gap: 12, marginBottom: 16, alignItems: 'center' }}>
        <Input
          label="搜索"
          placeholder="按 code / name / wholeName"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') list.setSearch(searchInput);
          }}
        />
        <Button type="button" onClick={() => list.setSearch(searchInput)}>
          查询
        </Button>
        <div style={{ flex: 1 }} />
        <Button
          type="button"
          onClick={() => {
            setEditing(null);
            setDrawerOpen(true);
          }}
        >
          新建
        </Button>
      </div>
      <Table<Organization> columns={columns} dataSource={list.items} rowKey="id" />
      <Pagination
        page={list.page}
        size={list.size}
        total={list.total}
        onPageChange={list.setPage}
      />
      <OrganizationEditDrawer
        open={drawerOpen}
        editing={editing}
        onClose={() => setDrawerOpen(false)}
        onSubmit={async (req) => {
          if (editing) {
            await updateOrganization(editing.id, {
              name: req.name,
              description: req.description,
              isPmo: req.isPmo,
              enabled: req.enabled,
            });
          } else {
            await createOrganization(req as OrganizationCreate);
          }
          setDrawerOpen(false);
          void list.refetch();
        }}
      />
      <ConfirmDialog
        open={confirmDelete !== null}
        title="删除组织节点"
        message={`确认删除「${confirmDelete?.name ?? ''}」？此操作不可撤销。`}
        onCancel={() => setConfirmDelete(null)}
        onConfirm={async () => {
          if (confirmDelete) await deleteOrganization(confirmDelete.id);
          setConfirmDelete(null);
          void list.refetch();
        }}
      />
    </Card>
  );
}

// Re-export the type so the EditDrawer module is happy with named imports.
export type { OrganizationType };
