import { useCallback, useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import { listDemands, type Demand, type Priority } from '../../api/demand';
import { listUsers, type User } from '../../api/user';
import {
  type Complexity,
  type Requirement,
  type RequirementCreate,
  type RequirementStatus,
  type RequirementUpdate,
} from '../../api/requirement';
import { usePaginated } from '../../hooks/usePaginated';

const STATUSES: RequirementStatus[] = [
  'DRAFT',
  'IN_REVIEW',
  'APPROVED',
  'IN_DEV',
  'DELIVERED',
  'DEPRECATED',
];
const PRIORITIES: Priority[] = ['URGENT', 'HIGH', 'MEDIUM', 'LOW'];
const COMPLEXITIES: Complexity[] = ['XS', 'S', 'M', 'L', 'XL'];

export interface RequirementEditDrawerProps {
  open: boolean;
  editing: Requirement | null;
  onClose: () => void;
  /**
   * Called on create. Payload includes `sourceDemandIds` collected from the multi-select sub-area.
   */
  onCreate: (body: RequirementCreate) => Promise<void> | void;
  /**
   * Called on edit. The drawer does not manage link diff on update for v0 — to change links the
   * user uses the demand-requirements page directly.
   */
  onUpdate: (id: number, body: RequirementUpdate) => Promise<void> | void;
}

export function RequirementEditDrawer({
  open,
  editing,
  onClose,
  onCreate,
  onUpdate,
}: RequirementEditDrawerProps) {
  const [code, setCode] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [ownerUserId, setOwnerUserId] = useState<number | ''>('');
  const [status, setStatus] = useState<RequirementStatus>('DRAFT');
  const [priority, setPriority] = useState<Priority>('MEDIUM');
  const [complexity, setComplexity] = useState<Complexity | ''>('');
  const [projectId, setProjectId] = useState<number | ''>('');
  const [closeReason, setCloseReason] = useState('');

  // sourceDemandIds multi-select state (Set for O(1) toggle); only used on create path.
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [userOptions, setUserOptions] = useState<User[]>([]);

  const demandFetcher = useCallback(
    async ({ page, size, search }: { page: number; size: number; search: string }) =>
      listDemands({ page, size, search: search || undefined }),
    [],
  );
  const demandList = usePaginated<Demand>(demandFetcher, { initialSize: 10 });

  useEffect(() => {
    if (!open) return;
    void listUsers({ size: 200 }).then((r) => setUserOptions(r.content));
    if (editing) {
      setCode(editing.code);
      setTitle(editing.title);
      setDescription(editing.description ?? '');
      setOwnerUserId(editing.ownerUserId);
      setStatus(editing.status);
      setPriority(editing.priority);
      setComplexity(editing.complexity ?? '');
      setProjectId(editing.projectId ?? '');
      setCloseReason(editing.closeReason ?? '');
    } else {
      setCode('');
      setTitle('');
      setDescription('');
      setOwnerUserId('');
      setStatus('DRAFT');
      setPriority('MEDIUM');
      setComplexity('');
      setProjectId('');
      setCloseReason('');
      setSelected(new Set());
    }
  }, [open, editing]);

  const toggle = (id: number) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const demandColumns: TableColumn<Demand>[] = [
    {
      key: 'sel',
      title: '选',
      render: (r) => (
        <input
          type="checkbox"
          checked={selected.has(r.id)}
          onChange={() => toggle(r.id)}
          data-testid={`req-source-checkbox-${r.id}`}
        />
      ),
    },
    { key: 'title', title: '主题', render: (r) => r.title },
    { key: 'status', title: '状态', render: (r) => r.status },
    { key: 'priority', title: '优先级', render: (r) => r.priority },
  ];

  return (
    <Drawer
      open={open}
      title={editing ? '编辑需求' : '新建需求'}
      onClose={onClose}
    >
      <Input label="编码 (REQ-...)" value={code} onChange={(e) => setCode(e.target.value)} />
      <Input label="标题" value={title} onChange={(e) => setTitle(e.target.value)} />
      <Input
        label="描述（含验收标准）"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
      />
      <div style={{ marginBottom: 12 }}>
        <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>负责 PO</label>
        <select
          className="rainier-treeselect-trigger"
          value={ownerUserId}
          onChange={(e) =>
            setOwnerUserId(e.target.value === '' ? '' : Number(e.target.value))
          }
          disabled={editing !== null}
          data-testid="req-owner-select"
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
          onChange={(e) => setStatus(e.target.value as RequirementStatus)}
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
              {p}
            </option>
          ))}
        </select>
      </div>
      <div style={{ marginBottom: 12 }}>
        <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>复杂度</label>
        <select
          className="rainier-treeselect-trigger"
          value={complexity}
          onChange={(e) => setComplexity((e.target.value as Complexity) || '')}
        >
          <option value="">（不评估）</option>
          {COMPLEXITIES.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
      </div>
      <Input
        label="所属 Project ID（占位，可空）"
        value={projectId === '' ? '' : String(projectId)}
        onChange={(e) => {
          const v = e.target.value;
          setProjectId(v === '' ? '' : Number(v));
        }}
      />
      {status === 'DEPRECATED' && (
        <Input
          label="弃用原因"
          value={closeReason}
          onChange={(e) => setCloseReason(e.target.value)}
        />
      )}
      {!editing && (
        <div
          style={{
            marginTop: 16,
            padding: 12,
            background: 'var(--rainier-color-bg-2)',
            borderRadius: 6,
          }}
          data-testid="req-sources-section"
        >
          <h4 style={{ marginTop: 0 }}>源诉求（DERIVED 派生关系）</h4>
          <p style={{ fontSize: 12, color: 'var(--rainier-color-text-2)', margin: '0 0 8px' }}>
            勾选要派生本需求的诉求；保存后将原子创建为本需求的源诉求。
          </p>
          <Table<Demand>
            columns={demandColumns}
            dataSource={demandList.items}
            rowKey="id"
          />
          <Pagination
            page={demandList.page}
            size={demandList.size}
            total={demandList.total}
            onPageChange={demandList.setPage}
          />
        </div>
      )}
      <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 12 }}>
        <Button type="button" variant="secondary" onClick={onClose}>
          取消
        </Button>
        <Button
          type="button"
          onClick={async () => {
            if (editing) {
              await onUpdate(editing.id, {
                code,
                title,
                description: description || undefined,
                status,
                priority,
                complexity: (complexity || undefined) as Complexity | undefined,
                projectId: projectId === '' ? undefined : projectId,
                closeReason: closeReason || undefined,
              });
            } else {
              if (!ownerUserId) return;
              await onCreate({
                code,
                title,
                description: description || undefined,
                ownerUserId,
                status,
                priority,
                complexity: (complexity || undefined) as Complexity | undefined,
                projectId: projectId === '' ? undefined : projectId,
                closeReason: closeReason || undefined,
                sourceDemandIds: Array.from(selected),
              });
            }
          }}
        >
          保存
        </Button>
      </div>
    </Drawer>
  );
}
