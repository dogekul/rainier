import { useCallback, useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import {
  createDemand,
  deleteDemand,
  listDemands,
  updateDemand,
  PRIORITY_LABELS,
  type Demand,
  type DemandStatus,
  type Priority,
  type Source,
} from '../../api/demand';
import { listUsers, type User } from '../../api/user';
import { usePaginated } from '../../hooks/usePaginated';

const STATUSES: DemandStatus[] = ['PENDING', 'IN_REVIEW', 'CONVERTED', 'DONE', 'CLOSED'];
const PRIORITIES: Priority[] = ['URGENT', 'HIGH', 'MEDIUM', 'LOW', 'LOWEST'];
const SOURCES: Source[] = ['WEB', 'WECHAT', 'EMAIL', 'DINGTALK', 'OTHER'];

export function DemandsPage() {
  const fetcher = useCallback(
    async ({ page, size, search }: { page: number; size: number; search: string }) =>
      listDemands({ page, size, search: search || undefined }),
    [],
  );
  const list = usePaginated<Demand>(fetcher, { initialSize: 20 });

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editing, setEditing] = useState<Demand | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<Demand | null>(null);

  const [userOptions, setUserOptions] = useState<User[]>([]);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [submitterUserId, setSubmitterUserId] = useState<number | ''>('');
  const [status, setStatus] = useState<DemandStatus>('PENDING');
  const [priority, setPriority] = useState<Priority>('MEDIUM');
  const [source, setSource] = useState<Source>('WEB');
  const [closeReason, setCloseReason] = useState('');

  useEffect(() => {
    if (!drawerOpen) return;
    // PageParams 校验 size <= 100；v0 池大小足够。
    void listUsers({ size: 100 }).then((r) => setUserOptions(r.content));
    if (editing) {
      setTitle(editing.title);
      setDescription(editing.description ?? '');
      setSubmitterUserId(editing.submitterUserId);
      setStatus(editing.status);
      setPriority(editing.priority);
      setSource(editing.source);
      setCloseReason(editing.closeReason ?? '');
    } else {
      setTitle('');
      setDescription('');
      setSubmitterUserId('');
      setStatus('PENDING');
      setPriority('MEDIUM');
      setSource('WEB');
      setCloseReason('');
    }
  }, [drawerOpen, editing]);

  const columns: TableColumn<Demand>[] = [
    { key: 'title', title: '主题', render: (r) => r.title },
    { key: 'submitterUserId', title: '提交人', render: (r) => r.submitterUserId },
    { key: 'status', title: '状态', render: (r) => r.status },
    { key: 'priority', title: '优先级', render: (r) => r.priority },
    { key: 'source', title: '来源', render: (r) => r.source },
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
      <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
        <Button
          type="button"
          onClick={() => {
            setEditing(null);
            setDrawerOpen(true);
          }}
          data-testid="demands-new-btn"
        >
          新建诉求
        </Button>
      </div>
      <Table<Demand> columns={columns} dataSource={list.items} rowKey="id" />
      <Pagination
        page={list.page}
        size={list.size}
        total={list.total}
        onPageChange={list.setPage}
      />
      <Drawer
        open={drawerOpen}
        title={editing ? '编辑诉求' : '新建诉求'}
        onClose={() => setDrawerOpen(false)}
      >
        <Input label="主题" value={title} onChange={(e) => setTitle(e.target.value)} />
        <Input
          label="描述"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>提交人</label>
          <select
            className="rainier-treeselect-trigger"
            value={submitterUserId}
            onChange={(e) =>
              setSubmitterUserId(e.target.value === '' ? '' : Number(e.target.value))
            }
            disabled={editing !== null}
            data-testid="demands-submitter-select"
          >
            <option value="">请选择</option>
            {userOptions.map((u) => (
              <option key={u.id} value={u.id}>
                {u.name}（{u.loginName}）
              </option>
            ))}
          </select>
        </div>
        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>状态</label>
          <select
            className="rainier-treeselect-trigger"
            value={status}
            onChange={(e) => setStatus(e.target.value as DemandStatus)}
          >
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>优先级</label>
          <select
            className="rainier-treeselect-trigger"
            value={priority}
            onChange={(e) => setPriority(e.target.value as Priority)}
          >
            {PRIORITIES.map((p) => (
              <option key={p} value={p}>
                {PRIORITY_LABELS[p]}
              </option>
            ))}
          </select>
        </div>
        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>来源</label>
          <select
            className="rainier-treeselect-trigger"
            value={source}
            onChange={(e) => setSource(e.target.value as Source)}
          >
            {SOURCES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
        {status === 'CLOSED' && (
          <Input
            label="关闭原因"
            value={closeReason}
            onChange={(e) => setCloseReason(e.target.value)}
          />
        )}
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <Button type="button" variant="secondary" onClick={() => setDrawerOpen(false)}>
            取消
          </Button>
          <Button
            type="button"
            onClick={async () => {
              if (editing) {
                await updateDemand(editing.id, {
                  title,
                  description: description || undefined,
                  status,
                  priority,
                  source,
                  closeReason: closeReason || undefined,
                });
              } else {
                if (!submitterUserId) return;
                await createDemand({
                  title,
                  description: description || undefined,
                  submitterUserId,
                  status,
                  priority,
                  source,
                  closeReason: closeReason || undefined,
                });
              }
              setDrawerOpen(false);
              void list.refetch();
            }}
          >
            保存
          </Button>
        </div>
      </Drawer>
      <ConfirmDialog
        open={confirmDelete !== null}
        title="删除诉求"
        message={`确认删除诉求「${confirmDelete?.title ?? ''}」？已有关联需求时会被拒绝。`}
        onCancel={() => setConfirmDelete(null)}
        onConfirm={async () => {
          if (confirmDelete) await deleteDemand(confirmDelete.id);
          setConfirmDelete(null);
          void list.refetch();
        }}
      />
    </Card>
  );
}
