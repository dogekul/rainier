import { useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { listProductCategories, type ProductCategory } from '../../api/productCategory';
import { listUsers, type User } from '../../api/user';
import { useAuthStore } from '../../store/auth';
import {
  type Product,
  type ProductCreate,
  type ProductStatus,
  type ProductUpdate,
} from '../../api/product';

const STATUS_OPTIONS: ProductStatus[] = ['PLANNING', 'ACTIVE', 'SUNSET', 'ARCHIVED'];

export interface ProductEditDrawerProps {
  open: boolean;
  editing: Product | null;
  onClose: () => void;
  onCreate: (body: ProductCreate) => Promise<void> | void;
  onUpdate: (id: number, body: ProductUpdate) => Promise<void> | void;
}

export function ProductEditDrawer({
  open,
  editing,
  onClose,
  onCreate,
  onUpdate,
}: ProductEditDrawerProps) {
  const currentLoginName = useAuthStore((s) => s.user?.username ?? null);

  const [categories, setCategories] = useState<ProductCategory[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [status, setStatus] = useState<ProductStatus>('PLANNING');
  const [categoryId, setCategoryId] = useState<number | ''>('');
  const [ownerUserId, setOwnerUserId] = useState<number | ''>('');
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      setFormError(null);
      return;
    }
    void Promise.all([
      listProductCategories({ size: 100 }),
      listUsers({ size: 100 }),
    ]).then(([cats, us]) => {
      setCategories(cats.content);
      setUsers(us.content);
      if (editing) {
        setOwnerUserId(editing.ownerUserId);
      } else {
        const me = currentLoginName
          ? us.content.find((u) => u.loginName === currentLoginName)
          : undefined;
        setOwnerUserId(me ? me.id : '');
      }
    });
    if (editing) {
      setCode(editing.code);
      setName(editing.name);
      setDescription(editing.description ?? '');
      setStatus(editing.status);
      setCategoryId(editing.categoryId);
    } else {
      setCode('');
      setName('');
      setDescription('');
      setStatus('PLANNING');
      setCategoryId('');
    }
  }, [open, editing, currentLoginName]);

  return (
    <Drawer
      open={open}
      title={editing ? '编辑产品' : '新建产品'}
      onClose={onClose}
    >
      <Input
        label="编码 (PROD-...)"
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
        <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
          所属分类（创建时锁定）
        </label>
        <select
          className="rainier-treeselect-trigger"
          value={categoryId}
          onChange={(e) => {
            setCategoryId(e.target.value === '' ? '' : Number(e.target.value));
            setFormError(null);
          }}
          disabled={editing !== null}
          data-testid="product-category-select"
        >
          <option value="">请选择产品分类</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}（{c.code}）
            </option>
          ))}
        </select>
      </div>
      <div style={{ marginBottom: 12 }}>
        <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>状态</label>
        <select
          className="rainier-treeselect-trigger"
          value={status}
          onChange={(e) => setStatus(e.target.value as ProductStatus)}
          data-testid="product-status-select"
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
          data-testid="product-owner-select"
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
          data-testid="product-form-error"
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
            if (!editing && (categoryId === '' || categoryId === null)) {
              setFormError('请选择产品分类');
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
                categoryId: categoryId as number,
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
