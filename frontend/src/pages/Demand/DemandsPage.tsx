import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import { OwnerChip, StatusChip } from '../../components/board';
import {
  createDemand,
  deleteDemand,
  getDemand,
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
import { DEMAND_STATUS_LABELS } from '../../constants/labels';

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

  // v0.0.59 — 深链 ?openId=N：来自 商机详情页/运营详情页 的「跳转到此诉求」，自动打开编辑抽屉。
  const [searchParams, setSearchParams] = useSearchParams();
  useEffect(() => {
    const openId = Number(searchParams.get('openId'));
    if (Number.isFinite(openId) && openId > 0) {
      void getDemand(openId)
        .then((d) => {
          setEditing(d);
          setDrawerOpen(true);
        })
        .catch(() => {
          // ignore — id 不存在/无权限：保留 URL 不弹抽屉
        })
        .finally(() => {
          // 用完即清，刷新页面不再弹
          const next = new URLSearchParams(searchParams);
          next.delete('openId');
          setSearchParams(next, { replace: true });
        });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

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
    { key: 'submitter', title: '提交人', render: (r) => <OwnerChip name={r.submitterName} loginName={r.submitterLoginName} /> },
    { key: 'status', title: '状态', render: (r) => <StatusChip status={r.status} label={DEMAND_STATUS_LABELS[r.status]} /> },
    { key: 'priority', title: '优先级', render: (r) => PRIORITY_LABELS[r.priority] ?? r.priority },
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
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2>诉求</h2>
        <span className="rainier-spacer" />
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
      <Card>
        <Table<Demand> columns={columns} dataSource={list.items} rowKey="id" />
        <Pagination
          page={list.page}
          size={list.size}
          total={list.total}
          onPageChange={list.setPage}
        />
      </Card>
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
        <div className="rainier-form-group">
          <label className="rainier-form-label">提交人</label>
          <select
            className="rainier-form-select"
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
        <div className="rainier-form-group">
          <label className="rainier-form-label">状态</label>
          <select
            className="rainier-form-select"
            value={status}
            onChange={(e) => setStatus(e.target.value as DemandStatus)}
          >
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {DEMAND_STATUS_LABELS[s] ?? s}
              </option>
            ))}
          </select>
        </div>
        <div className="rainier-form-group">
          <label className="rainier-form-label">优先级</label>
          <select
            className="rainier-form-select"
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
        <div className="rainier-form-group">
          <label className="rainier-form-label">来源</label>
          <select
            className="rainier-form-select"
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
        <div className="rainier-form-footer">
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
    </div>
  );
}
