import { useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { listProductCategories, type ProductCategory } from '../../api/productCategory';
import { listProducts, type Product } from '../../api/product';
import { listUsers, type User } from '../../api/user';
import { useAuthStore } from '../../store/auth';
import {
  type ProductModule,
  type ProductModuleCreate,
  type ProductModuleStatus,
  type ProductModuleUpdate,
} from '../../api/productModule';

const STATUS_OPTIONS: ProductModuleStatus[] = ['PLANNING', 'ACTIVE', 'DEPRECATED'];

export interface ProductModuleEditDrawerProps {
  open: boolean;
  editing: ProductModule | null;
  onClose: () => void;
  onCreate: (body: ProductModuleCreate) => Promise<void> | void;
  onUpdate: (id: number, body: ProductModuleUpdate) => Promise<void> | void;
}

export function ProductModuleEditDrawer({
  open,
  editing,
  onClose,
  onCreate,
  onUpdate,
}: ProductModuleEditDrawerProps) {
  const currentLoginName = useAuthStore((s) => s.user?.username ?? null);

  const [categories, setCategories] = useState<ProductCategory[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [users, setUsers] = useState<User[]>([]);

  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [status, setStatus] = useState<ProductModuleStatus>('PLANNING');
  const [categoryId, setCategoryId] = useState<number | ''>('');
  const [productId, setProductId] = useState<number | ''>('');
  const [ownerUserId, setOwnerUserId] = useState<number | ''>('');
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      setFormError(null);
      return;
    }
    void Promise.all([
      listProductCategories({ size: 100 }),
      listProducts({ size: 100 }),
      listUsers({ size: 100 }),
    ]).then(([cats, prods, us]) => {
      setCategories(cats.content);
      setProducts(prods.content);
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
      setProductId(editing.productId);
      setCategoryId('');
    } else {
      setCode('');
      setName('');
      setDescription('');
      setStatus('PLANNING');
      setCategoryId('');
      setProductId('');
    }
  }, [open, editing, currentLoginName]);

  // Category is a filter helper; switching it should clear selected Product.
  const handleCategoryChange = (next: number | '') => {
    setCategoryId(next);
    setProductId('');
    setFormError(null);
  };

  const filteredProducts =
    categoryId === '' ? products : products.filter((p) => p.categoryId === categoryId);

  return (
    <Drawer
      open={open}
      title={editing ? '编辑产品模块' : '新建产品模块'}
      onClose={onClose}
    >
      <Input
        label="编码 (MOD-...)"
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
          产品分类（可选 — 用于过滤产品下拉）
        </label>
        <select
          className="rainier-treeselect-trigger"
          value={categoryId}
          onChange={(e) =>
            handleCategoryChange(e.target.value === '' ? '' : Number(e.target.value))
          }
          disabled={editing !== null}
          data-testid="product-module-category-select"
        >
          <option value="">全部分类</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}（{c.code}）
            </option>
          ))}
        </select>
      </div>
      <div style={{ marginBottom: 12 }}>
        <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
          所属产品（创建时锁定）
        </label>
        <select
          className="rainier-treeselect-trigger"
          value={productId}
          onChange={(e) => {
            setProductId(e.target.value === '' ? '' : Number(e.target.value));
            setFormError(null);
          }}
          disabled={editing !== null}
          data-testid="product-module-product-select"
        >
          <option value="">请选择产品</option>
          {filteredProducts.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}（{p.code}）
            </option>
          ))}
        </select>
      </div>
      <div style={{ marginBottom: 12 }}>
        <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>状态</label>
        <select
          className="rainier-treeselect-trigger"
          value={status}
          onChange={(e) => setStatus(e.target.value as ProductModuleStatus)}
          data-testid="product-module-status-select"
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
          data-testid="product-module-owner-select"
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
          data-testid="product-module-form-error"
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
            if (!editing && (productId === '' || productId === null)) {
              setFormError('请选择产品');
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
                productId: productId as number,
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
