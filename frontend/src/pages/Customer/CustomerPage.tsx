import { useCallback, useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import {
  createCustomer,
  deleteCustomer,
  listCustomers,
  updateCustomer,
  type Customer,
} from '../../api/customer';
import { usePaginated } from '../../hooks/usePaginated';

/**
 * v0.0.45 —「客户」管理页 (CRUD). all-users，客户导航组。客户实体被商机经 customerId 关联（创建商机时可选/新建）。
 */
export function CustomerPage() {
  const fetcher = useCallback(
    async ({ page, size, search }: { page: number; size: number; search: string }) =>
      listCustomers({ page, size, search: search || undefined }),
    [],
  );
  const list = usePaginated<Customer>(fetcher, { initialSize: 20 });

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editing, setEditing] = useState<Customer | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<Customer | null>(null);

  const [name, setName] = useState('');
  const [industry, setIndustry] = useState('');
  const [contactName, setContactName] = useState('');
  const [notes, setNotes] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (!drawerOpen) {
      setFormError(null);
      return;
    }
    setName(editing?.name ?? '');
    setIndustry(editing?.industry ?? '');
    setContactName(editing?.contactName ?? '');
    setNotes(editing?.notes ?? '');
  }, [drawerOpen, editing]);

  const columns: TableColumn<Customer>[] = [
    { key: 'name', title: '客户名称', render: (r) => r.name },
    { key: 'industry', title: '行业', render: (r) => r.industry ?? '—' },
    { key: 'contactName', title: '联系人', render: (r) => r.contactName ?? '—' },
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
            data-testid={`customer-edit-${r.id}`}
          >
            编辑
          </Button>{' '}
          <Button
            type="button"
            variant="secondary"
            onClick={() => setConfirmDelete(r)}
            data-testid={`customer-delete-${r.id}`}
          >
            删除
          </Button>
        </>
      ),
    },
  ];

  const save = async () => {
    if (!name.trim()) {
      setFormError('请填写客户名称');
      return;
    }
    const body = {
      name: name.trim(),
      industry: industry.trim() || undefined,
      contactName: contactName.trim() || undefined,
      notes: notes.trim() || undefined,
    };
    if (editing) {
      await updateCustomer(editing.id, body);
    } else {
      await createCustomer(body);
    }
    setDrawerOpen(false);
    void list.refetch();
  };

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2 style={{ margin: 0 }}>客户</h2>
        <div style={{ flex: 1 }} />
        <Input
          placeholder="搜索客户/行业/联系人"
          value={list.search}
          onChange={(e) => list.setSearch(e.target.value)}
          data-testid="customers-search"
        />
        <Button
          type="button"
          onClick={() => {
            setEditing(null);
            setDrawerOpen(true);
          }}
          data-testid="customers-new-btn"
        >
          新建客户
        </Button>
      </div>
      <Card>
        <Table<Customer>
          columns={columns}
          dataSource={list.items}
          rowKey="id"
          emptyText="暂无客户，点「新建客户」创建。"
        />
        <Pagination
          page={list.page}
          size={list.size}
          total={list.total}
          onPageChange={list.setPage}
        />
      </Card>
      <Drawer
        open={drawerOpen}
        title={editing ? '编辑客户' : '新建客户'}
        onClose={() => setDrawerOpen(false)}
      >
        <Input
          label="客户名称"
          value={name}
          onChange={(e) => setName(e.target.value)}
          data-testid="customer-name"
        />
        <Input
          label="行业（可空）"
          value={industry}
          onChange={(e) => setIndustry(e.target.value)}
          data-testid="customer-industry"
        />
        <Input
          label="联系人（可空）"
          value={contactName}
          onChange={(e) => setContactName(e.target.value)}
          data-testid="customer-contact"
        />
        <Input
          label="备注（可空）"
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          data-testid="customer-notes"
        />
        {formError && (
          <div
            style={{
              padding: '6px 10px',
              marginBottom: 12,
              color: 'var(--rainier-color-danger, #d4380d)',
              fontSize: 12,
              background: 'rgba(212, 56, 13, 0.08)',
              borderRadius: 4,
            }}
            data-testid="customer-form-error"
          >
            {formError}
          </div>
        )}
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <Button type="button" variant="secondary" onClick={() => setDrawerOpen(false)}>
            取消
          </Button>
          <Button type="button" onClick={() => void save()} data-testid="customer-save">
            保存
          </Button>
        </div>
      </Drawer>
      <ConfirmDialog
        open={confirmDelete !== null}
        title="删除客户"
        message={`确认删除客户「${confirmDelete?.name ?? ''}」？已关联的商机仍保留客户名称显示。`}
        onCancel={() => setConfirmDelete(null)}
        onConfirm={async () => {
          if (confirmDelete) await deleteCustomer(confirmDelete.id);
          setConfirmDelete(null);
          void list.refetch();
        }}
      />
    </div>
  );
}
