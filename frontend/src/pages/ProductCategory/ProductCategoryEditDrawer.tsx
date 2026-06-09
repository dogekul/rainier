import { useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { listUsers, type User } from '../../api/user';
import { useAuthStore } from '../../store/auth';
import {
  type ProductCategory,
  type ProductCategoryCreate,
  type ProductCategoryStatus,
  type ProductCategoryUpdate,
} from '../../api/productCategory';

const STATUS_OPTIONS: ProductCategoryStatus[] = ['ACTIVE', 'ARCHIVED'];

export interface ProductCategoryEditDrawerProps {
  open: boolean;
  editing: ProductCategory | null;
  onClose: () => void;
  onCreate: (body: ProductCategoryCreate) => Promise<void> | void;
  onUpdate: (id: number, body: ProductCategoryUpdate) => Promise<void> | void;
}

export function ProductCategoryEditDrawer({
  open,
  editing,
  onClose,
  onCreate,
  onUpdate,
}: ProductCategoryEditDrawerProps) {
  const currentLoginName = useAuthStore((s) => s.user?.username ?? null);

  const [users, setUsers] = useState<User[]>([]);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [status, setStatus] = useState<ProductCategoryStatus>('ACTIVE');
  const [ownerUserId, setOwnerUserId] = useState<number | ''>('');
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
      setStatus(editing.status);
    } else {
      setCode('');
      setName('');
      setDescription('');
      setStatus('ACTIVE');
    }
  }, [open, editing, currentLoginName]);

  return (
    <Drawer
      open={open}
      title={editing ? '编辑产品分类' : '新建产品分类'}
      onClose={onClose}
    >
      <Input
        label="编码 (CAT-...)"
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
      <div style={{ marginBottom: 12 }}>
        <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>状态</label>
        <select
          className="rainier-treeselect-trigger"
          value={status}
          onChange={(e) => setStatus(e.target.value as ProductCategoryStatus)}
          data-testid="product-category-status-select"
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
          className="rainier-treeselect-trigger"
          value={ownerUserId}
          onChange={(e) => {
            setOwnerUserId(e.target.value === '' ? '' : Number(e.target.value));
            setFormError(null);
          }}
          data-testid="product-category-owner-select"
        >
          <option value="">请选择</option>
          {users.map((u) => (
            <option key={u.id} value={u.id}>
              {u.name}（{u.loginName}）
            </option>
          ))}
        </select>
      </div>
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
          data-testid="product-category-form-error"
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
                status,
                ownerUserId,
              });
            } else {
              await onCreate({
                code,
                name,
                description: description || undefined,
                status,
                ownerUserId,
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
