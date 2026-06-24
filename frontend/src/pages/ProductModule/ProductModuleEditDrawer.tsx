import { useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { PRODUCT_MODULE_STATUS_LABELS } from '../../constants/labels';
import { listProducts, type Product } from '../../api/product';
import { listUsers, type User } from '../../api/user';
import { useAuthStore } from '../../store/auth';
import {
  listProductModules,
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

/**
 * v0.0.13 cascade: Product → optional parentModule (server-side filter, A2 pattern).
 * parentId mutable on edit (reparent); productId locked after creation.
 */
export function ProductModuleEditDrawer({
  open,
  editing,
  onClose,
  onCreate,
  onUpdate,
}: ProductModuleEditDrawerProps) {
  const currentLoginName = useAuthStore((s) => s.user?.username ?? null);

  const [products, setProducts] = useState<Product[]>([]);
  const [parentOptions, setParentOptions] = useState<ProductModule[]>([]);
  const [users, setUsers] = useState<User[]>([]);

  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [status, setStatus] = useState<ProductModuleStatus>('PLANNING');
  const [productId, setProductId] = useState<number | ''>('');
  const [parentId, setParentId] = useState<number | ''>('');
  const [ownerUserId, setOwnerUserId] = useState<number | ''>('');
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      setFormError(null);
      return;
    }
    void Promise.all([listProducts({ size: 100 }), listUsers({ size: 100 })]).then(
      ([prods, us]) => {
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
      },
    );
    if (editing) {
      setCode(editing.code);
      setName(editing.name);
      setDescription(editing.description ?? '');
      setStatus(editing.status);
      setProductId(editing.productId);
      setParentId(editing.parentId ?? '');
    } else {
      setCode('');
      setName('');
      setDescription('');
      setStatus('PLANNING');
      setProductId('');
      setParentId('');
      setParentOptions([]);
    }
  }, [open, editing, currentLoginName]);

  // Refetch parent candidates whenever the product changes (server-side filter).
  useEffect(() => {
    if (!open || productId === '') {
      return;
    }
    void listProductModules({ productId, size: 100 }).then((res) => {
      // A module cannot be its own parent — drop self from the candidate list on edit.
      setParentOptions(
        editing ? res.content.filter((m) => m.id !== editing.id) : res.content,
      );
    });
  }, [open, productId, editing]);

  const handleProductChange = (next: number | '') => {
    setProductId(next);
    setParentId('');
    setParentOptions([]);
    setFormError(null);
  };

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
      <div className="rainier-form-group">
        <label className="rainier-form-label">
          所属产品（创建时锁定）
        </label>
        <select
          className="rainier-form-select"
          value={productId}
          onChange={(e) =>
            handleProductChange(e.target.value === '' ? '' : Number(e.target.value))
          }
          disabled={editing !== null}
          data-testid="product-module-product-select"
        >
          <option value="">请选择产品</option>
          {products.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}（{p.code}）
            </option>
          ))}
        </select>
      </div>
      <div className="rainier-form-group">
        <label className="rainier-form-label">
          父级模块（可选 — 留空为顶层；可改 = 调整层级）
        </label>
        <select
          className="rainier-form-select"
          value={parentId}
          onChange={(e) => {
            setParentId(e.target.value === '' ? '' : Number(e.target.value));
            setFormError(null);
          }}
          data-testid="product-module-parent-select"
        >
          <option value="">顶层模块（无父级）</option>
          {parentOptions.map((m) => (
            <option key={m.id} value={m.id} title={m.pathName ?? m.name}>
              {m.pathName ?? m.name}（{m.code}）
            </option>
          ))}
        </select>
      </div>
      <div className="rainier-form-group">
        <label className="rainier-form-label">状态</label>
        <select
          className="rainier-form-select"
          value={status}
          onChange={(e) => setStatus(e.target.value as ProductModuleStatus)}
          data-testid="product-module-status-select"
        >
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>
              {PRODUCT_MODULE_STATUS_LABELS[s] ?? s}
            </option>
          ))}
        </select>
      </div>
      <div className="rainier-form-group">
        <label className="rainier-form-label">
          负责人（默认为当前登录用户，可改）
        </label>
        <select
          className="rainier-form-select"
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
        <div className="rainier-error-banner" data-testid="product-module-form-error">
          {formError}
        </div>
      )}
      <div className="rainier-form-footer">
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
                parentId: parentId === '' ? undefined : parentId,
                ownerUserId,
              });
            } else {
              await onCreate({
                code,
                name,
                description: description || undefined,
                status,
                productId: productId as number,
                parentId: parentId === '' ? undefined : parentId,
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
