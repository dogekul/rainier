import { useCallback, useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { Pagination } from '../../components/ui/Pagination';
import { EmptyState } from '../../components/board';
import {
  createCustomer,
  deleteCustomer,
  listCustomers,
  updateCustomer,
  type Customer,
} from '../../api/customer';
import { usePaginated } from '../../hooks/usePaginated';
import './CustomerPage.css';

/** Avatar tints (theme-safe CSS vars), picked deterministically by name — avoids red (error semantics). */
const AVATAR_PALETTE: { bg: string; fg: string }[] = [
  { bg: 'var(--rainier-bg-selected)', fg: 'var(--rainier-color-primary)' },
  { bg: 'var(--rainier-status-green-bg)', fg: 'var(--rainier-status-green)' },
  { bg: 'var(--rainier-status-yellow-bg)', fg: 'var(--rainier-status-yellow)' },
  { bg: 'var(--rainier-status-gray-bg)', fg: 'var(--rainier-color-text-2)' },
];
function avatarColor(name: string): { bg: string; fg: string } {
  let h = 0;
  for (let i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) >>> 0;
  return AVATAR_PALETTE[h % AVATAR_PALETTE.length];
}

/**
 * v0.0.45 —「客户」管理页 (CRUD). all-users，客户导航组。客户实体被商机经 customerId 关联。
 * v0.0.51 — 卡片网格视觉改版（头像 + 行业标签 + 联系人/备注 + 悬浮抬升）。
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

  const openNew = () => {
    setEditing(null);
    setDrawerOpen(true);
  };

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
        <span className="cust-count">共 {list.total} 家</span>
        <div style={{ flex: 1 }} />
        <Input
          placeholder="搜索客户/行业/联系人"
          value={list.search}
          onChange={(e) => list.setSearch(e.target.value)}
          data-testid="customers-search"
          style={{ width: 220, flex: 'none' }}
        />
        <Button type="button" onClick={openNew} data-testid="customers-new-btn">
          新建客户
        </Button>
      </div>

      {list.items.length === 0 ? (
        <EmptyState message="暂无客户，点「新建客户」创建。" testId="customers-empty" />
      ) : (
        <div className="cust-grid" data-testid="customers-grid">
          {list.items.map((c) => {
            const color = avatarColor(c.name);
            return (
              <div key={c.id} className="cust-card" data-testid={`customer-card-${c.id}`}>
                <div className="cust-card-head">
                  <span
                    className="cust-avatar"
                    aria-hidden="true"
                    style={{ background: color.bg, color: color.fg }}
                  >
                    {c.name.trim().charAt(0).toUpperCase()}
                  </span>
                  <div className="cust-head-text">
                    <span className="cust-name" title={c.name}>
                      {c.name}
                    </span>
                    <span className={`cust-chip${c.industry ? '' : ' muted'}`}>
                      {c.industry || '未填行业'}
                    </span>
                  </div>
                </div>
                <div className="cust-body">
                  <div className="cust-meta">
                    <span className="label">联系人</span>
                    {c.contactName || '—'}
                  </div>
                  {c.notes ? <div className="cust-notes">{c.notes}</div> : null}
                </div>
                <div className="cust-actions">
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => {
                      setEditing(c);
                      setDrawerOpen(true);
                    }}
                    data-testid={`customer-edit-${c.id}`}
                  >
                    编辑
                  </Button>
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => setConfirmDelete(c)}
                    data-testid={`customer-delete-${c.id}`}
                  >
                    删除
                  </Button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <Pagination
        page={list.page}
        size={list.size}
        total={list.total}
        onPageChange={list.setPage}
      />

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
