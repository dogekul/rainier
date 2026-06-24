import { useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { listUsers, type User } from '../../api/user';
import { useAuthStore } from '../../store/auth';
import {
  type Sprint,
  type SprintCreate,
  type SprintStatus,
  type SprintUpdate,
} from '../../api/sprint';

const STATUS_OPTIONS: SprintStatus[] = ['PLANNING', 'ACTIVE', 'COMPLETED', 'CANCELLED'];

export interface SprintEditDrawerProps {
  open: boolean;
  requirementId: number;
  requirementCode: string;
  requirementTitle: string;
  editing: Sprint | null;
  onClose: () => void;
  onCreate: (body: SprintCreate) => Promise<void> | void;
  onUpdate: (id: number, body: SprintUpdate) => Promise<void> | void;
}

export function SprintEditDrawer({
  open,
  requirementId,
  requirementCode,
  requirementTitle,
  editing,
  onClose,
  onCreate,
  onUpdate,
}: SprintEditDrawerProps) {
  const currentLoginName = useAuthStore((s) => s.user?.username ?? null);

  const [users, setUsers] = useState<User[]>([]);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [goal, setGoal] = useState('');
  const [status, setStatus] = useState<SprintStatus>('PLANNING');
  const [ownerUserId, setOwnerUserId] = useState<number | ''>('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      setFormError(null);
      return;
    }
    void listUsers({ size: 100 }).then((r) => {
      setUsers(r.content);
      if (editing) {
        setOwnerUserId(editing.ownerUserId);
      } else {
        const me = currentLoginName
          ? r.content.find((u) => u.loginName === currentLoginName)
          : undefined;
        setOwnerUserId(me ? me.id : '');
      }
    });
    if (editing) {
      setCode(editing.code);
      setName(editing.name);
      setDescription(editing.description ?? '');
      setGoal(editing.goal ?? '');
      setStatus(editing.status);
      setStartDate(editing.startDate ?? '');
      setEndDate(editing.endDate ?? '');
    } else {
      setCode('');
      setName('');
      setDescription('');
      setGoal('');
      setStatus('PLANNING');
      setStartDate('');
      setEndDate('');
    }
  }, [open, editing, currentLoginName]);

  return (
    <Drawer
      open={open}
      title={editing ? '编辑 Sprint' : '新建 Sprint'}
      onClose={onClose}
    >
      <div style={{ marginBottom: 12 }}>
        <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>所属需求</label>
        <div
          style={{
            padding: '6px 10px',
            background: 'var(--rainier-color-bg-2)',
            borderRadius: 4,
            fontSize: 13,
          }}
          data-testid="sprint-drawer-requirement-display"
        >
          {requirementTitle}（{requirementCode}）— 创建时锁定
        </div>
      </div>
      <Input
        label="编码 (SPR-...)"
        value={code}
        onChange={(e) => setCode(e.target.value)}
        disabled={editing !== null}
      />
      <Input label="名称" value={name} onChange={(e) => setName(e.target.value)} />
      <Input
        label="描述"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
      />
      <Input label="目标 (Goal)" value={goal} onChange={(e) => setGoal(e.target.value)} />
      <div style={{ marginBottom: 12 }}>
        <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>状态</label>
        <select
          className="rainier-form-select"
          value={status}
          onChange={(e) => setStatus(e.target.value as SprintStatus)}
          data-testid="sprint-status-select"
        >
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>
      <div style={{ marginBottom: 12 }}>
        <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
          负责人（默认为当前登录用户，可改）
        </label>
        <select
          className="rainier-form-select"
          value={ownerUserId}
          onChange={(e) => {
            setOwnerUserId(e.target.value === '' ? '' : Number(e.target.value));
            setFormError(null);
          }}
          data-testid="sprint-owner-select"
        >
          <option value="">请选择</option>
          {users.map((u) => (
            <option key={u.id} value={u.id}>
              {u.name}（{u.loginName}）
            </option>
          ))}
        </select>
      </div>
      <Input
        label="开始日期 (可选)"
        value={startDate}
        onChange={(e) => setStartDate(e.target.value)}
      />
      <Input
        label="结束日期 (可选)"
        value={endDate}
        onChange={(e) => setEndDate(e.target.value)}
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
          data-testid="sprint-form-error"
        >
          {formError}
        </div>
      )}
      <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
        <Button type="button" variant="secondary" onClick={onClose}>
          取消
        </Button>
        <Button
          type="button"
          onClick={async () => {
            if (ownerUserId === '' || ownerUserId === null) {
              setFormError('请选择负责人');
              return;
            }
            setFormError(null);
            if (editing) {
              await onUpdate(editing.id, {
                code,
                name,
                description: description || undefined,
                goal: goal || undefined,
                status,
                ownerUserId,
                startDate: startDate || undefined,
                endDate: endDate || undefined,
              });
            } else {
              await onCreate({
                code,
                name,
                description: description || undefined,
                goal: goal || undefined,
                status,
                requirementId,
                ownerUserId,
                startDate: startDate || undefined,
                endDate: endDate || undefined,
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
