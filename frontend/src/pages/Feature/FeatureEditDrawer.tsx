import { useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { listProducts, type Product } from '../../api/product';
import {
  getProductModule,
  listProductModules,
  type ProductModule,
} from '../../api/productModule';
import { listUsers, type User } from '../../api/user';
import { useAuthStore } from '../../store/auth';
import {
  type Feature,
  type FeatureCreate,
  type FeatureStatus,
  type FeatureUpdate,
} from '../../api/feature';

const STATUS_OPTIONS: FeatureStatus[] = ['PLANNING', 'ACTIVE', 'DEPRECATED'];

export interface FeatureEditDrawerProps {
  open: boolean;
  editing: Feature | null;
  onClose: () => void;
  onCreate: (body: FeatureCreate) => Promise<void> | void;
  onUpdate: (id: number, body: FeatureUpdate) => Promise<void> | void;
}

export function FeatureEditDrawer({
  open,
  editing,
  onClose,
  onCreate,
  onUpdate,
}: FeatureEditDrawerProps) {
  const currentLoginName = useAuthStore((s) => s.user?.username ?? null);

  const [products, setProducts] = useState<Product[]>([]);
  const [modules, setModules] = useState<ProductModule[]>([]);
  const [users, setUsers] = useState<User[]>([]);

  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [status, setStatus] = useState<FeatureStatus>('PLANNING');
  const [productId, setProductId] = useState<number | ''>('');
  const [moduleId, setModuleId] = useState<number | ''>('');
  const [ownerUserId, setOwnerUserId] = useState<number | ''>('');
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      setFormError(null);
      return;
    }
    // v0.0.12.1 A2: server-side filter — open() loads only Products + Users.
    // Modules fetched lazily when Product changes (or on edit, derived from editing module).
    void Promise.all([listProducts({ size: 100 }), listUsers({ size: 100 })]).then(
      ([prods, us]) => {
        setProducts(prods.content);
        setUsers(us.content);
        if (editing) {
          setOwnerUserId(editing.ownerUserId);
          // Edit mode: resolve productId via the editing module, then load that product's modules.
          void getProductModule(editing.moduleId).then((m) => {
            setProductId(m.productId);
          });
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
      setModuleId(editing.moduleId);
    } else {
      setCode('');
      setName('');
      setDescription('');
      setStatus('PLANNING');
      setProductId('');
      setModuleId('');
      setModules([]);
    }
  }, [open, editing, currentLoginName]);

  // v0.0.12.1 A2: whenever productId is set (user pick OR derived from editing), fetch its modules.
  useEffect(() => {
    if (!open || productId === '') {
      return;
    }
    void listProductModules({ productId, size: 100 }).then((res) => {
      setModules(res.content);
    });
  }, [open, productId]);

  // When Product changes via user pick, clear selected Module (effect above will refetch).
  const handleProductChange = (next: number | '') => {
    setProductId(next);
    setModuleId('');
    setModules([]);
    setFormError(null);
  };

  return (
    <Drawer
      open={open}
      title={editing ? '编辑功能' : '新建功能'}
      onClose={onClose}
    >
      <Input
        label="编码 (FEAT-...)"
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
          所属产品（用于过滤模块下拉）
        </label>
        <select
          className="rainier-treeselect-trigger"
          value={productId}
          onChange={(e) =>
            handleProductChange(e.target.value === '' ? '' : Number(e.target.value))
          }
          disabled={editing !== null}
          data-testid="feature-product-select"
        >
          <option value="">请选择产品</option>
          {products.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}（{p.code}）
            </option>
          ))}
        </select>
      </div>
      <div style={{ marginBottom: 12 }}>
        <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>
          所属模块（创建时锁定）
        </label>
        <select
          className="rainier-treeselect-trigger"
          value={moduleId}
          onChange={(e) => {
            setModuleId(e.target.value === '' ? '' : Number(e.target.value));
            setFormError(null);
          }}
          disabled={editing !== null}
          data-testid="feature-module-select"
        >
          <option value="">请选择模块</option>
          {modules.map((m) => (
            <option key={m.id} value={m.id} title={m.pathName ?? m.name}>
              {m.pathName ?? m.name}（{m.code}）
            </option>
          ))}
        </select>
      </div>
      <div style={{ marginBottom: 12 }}>
        <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>状态</label>
        <select
          className="rainier-treeselect-trigger"
          value={status}
          onChange={(e) => setStatus(e.target.value as FeatureStatus)}
          data-testid="feature-status-select"
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
          data-testid="feature-owner-select"
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
          data-testid="feature-form-error"
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
            if (!editing && (moduleId === '' || moduleId === null)) {
              setFormError('请选择模块');
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
                moduleId: moduleId as number,
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
