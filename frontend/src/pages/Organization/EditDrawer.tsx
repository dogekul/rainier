import { useCallback, useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { TreeSelect, type TreeNode } from '../../components/ui/TreeSelect';
import { ORGANIZATION_TYPE_LABELS } from '../../constants/labels';
import {
  getOrganizationTree,
  type Organization,
  type OrganizationCreate,
  type OrganizationType,
} from '../../api/organization';
import {
  addOrganizationPmo,
  listEffectivePmos,
  listOrganizationPmos,
  removeOrganizationPmo,
  type EffectivePmoDetail,
  type OrganizationPmoDetail,
} from '../../api/organizationPmo';
import { listUsers, type User } from '../../api/user';
import { isElevated, useAuthStore } from '../../store/auth';

const types: OrganizationType[] = ['COMPANY', 'DEPARTMENT', 'DOMAIN', 'TEAM', 'SUBGROUP'];

export interface OrganizationEditDrawerProps {
  open: boolean;
  editing: Organization | null;
  onClose: () => void;
  onSubmit: (req: OrganizationCreate) => Promise<void> | void;
}

export function OrganizationEditDrawer({
  open,
  editing,
  onClose,
  onSubmit,
}: OrganizationEditDrawerProps) {
  const [parentId, setParentId] = useState<number | null>(null);
  const [type, setType] = useState<OrganizationType>('DEPARTMENT');
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [enabled, setEnabled] = useState(true);
  const [tree, setTree] = useState<TreeNode[]>([]);

  // v0.0.64 — PMO 管理段（仅 admin 可改，且仅在 editing != null 时显示，因为需要 org id）.
  const currentUser = useAuthStore((s) => s.user);
  const admin = isElevated(currentUser);
  const [ownPmos, setOwnPmos] = useState<OrganizationPmoDetail[]>([]);
  const [effectivePmos, setEffectivePmos] = useState<EffectivePmoDetail[]>([]);
  const [allUsers, setAllUsers] = useState<User[]>([]);
  const [addingPmoUserId, setAddingPmoUserId] = useState<number | ''>('');
  const [pmoError, setPmoError] = useState<string | null>(null);

  const refetchPmos = useCallback(async () => {
    if (!editing) return;
    try {
      const [own, eff] = await Promise.all([
        listOrganizationPmos(editing.id),
        listEffectivePmos(editing.id),
      ]);
      setOwnPmos(own);
      setEffectivePmos(eff);
    } catch {
      setOwnPmos([]);
      setEffectivePmos([]);
    }
  }, [editing]);

  useEffect(() => {
    if (!open) return;
    void getOrganizationTree().then((nodes) =>
      setTree(nodes.map((n) => ({ id: n.id, name: n.name, parentId: n.parentId }))),
    );
    if (editing) {
      setParentId(editing.parentId);
      setType(editing.type);
      setCode(editing.code);
      setName(editing.name);
      setDescription(editing.description ?? '');
      setEnabled(editing.enabled);
      void refetchPmos();
      void listUsers({ size: 100 }).then((r) => setAllUsers(r.content));
    } else {
      setParentId(null);
      setType('DEPARTMENT');
      setCode('');
      setName('');
      setDescription('');
      setEnabled(true);
      setOwnPmos([]);
      setEffectivePmos([]);
    }
    setPmoError(null);
    setAddingPmoUserId('');
  }, [open, editing, refetchPmos]);

  const submit = () => {
    void onSubmit({
      parentId: parentId ?? null,
      type,
      code,
      name,
      description: description || undefined,
      enabled,
    });
  };

  return (
    <Drawer
      open={open}
      title={editing ? '编辑组织节点' : '新建组织节点'}
      onClose={onClose}
      footer={
        <>
          <Button type="button" variant="secondary" onClick={onClose}>
            取消
          </Button>
          <Button type="button" onClick={submit}>
            保存
          </Button>
        </>
      }
    >
      <div className="rainier-form-group">
        <label className="rainier-form-label">父节点</label>
        <TreeSelect value={parentId} nodes={tree} onChange={setParentId} placeholder="选择父节点（留空 = 根）" />
      </div>
      <div className="rainier-form-group">
        <label className="rainier-form-label">类型</label>
        <select
          className="rainier-form-select"
          value={type}
          onChange={(e) => setType(e.target.value as OrganizationType)}
          data-testid="org-type-select"
        >
          {types.map((t) => (
            <option key={t} value={t}>
              {ORGANIZATION_TYPE_LABELS[t] ?? t}
            </option>
          ))}
        </select>
      </div>
      <Input label="编码" value={code} onChange={(e) => setCode(e.target.value)} />
      <Input label="名称" value={name} onChange={(e) => setName(e.target.value)} />
      <Input label="描述" value={description} onChange={(e) => setDescription(e.target.value)} />
      <div className="rainier-form-group">
        <label style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          <input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
          启用
        </label>
      </div>

      {editing && (
        <div
          className="rainier-form-group"
          data-testid="org-pmo-section"
          style={{ borderTop: '1px solid var(--rainier-border)', marginTop: 16, paddingTop: 14 }}
        >
          <div
            style={{
              fontSize: 13,
              fontWeight: 600,
              color: 'var(--rainier-color-text-2)',
              marginBottom: 8,
            }}
          >
            PMO 管理 {admin ? '' : '（仅 admin 可改）'}
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginBottom: 10 }}>
            {effectivePmos.length === 0 ? (
              <span style={{ fontSize: 12, color: 'var(--rainier-color-text-3)' }}>
                暂无 PMO
              </span>
            ) : (
              effectivePmos.map((p) => {
                const isOwn = p.inheritedFromOrgId === editing.id;
                return (
                  <span
                    key={p.userId}
                    data-testid={`org-pmo-chip-${p.userId}`}
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: 4,
                      padding: '3px 10px',
                      borderRadius: 12,
                      background: isOwn ? 'var(--rainier-bg-selected)' : 'var(--rainier-bg-hover)',
                      color: isOwn ? 'var(--rainier-color-primary)' : 'var(--rainier-color-text-3)',
                      fontSize: 12,
                    }}
                  >
                    {p.userName ?? p.userLoginName ?? `#${p.userId}`}
                    {!isOwn && (
                      <em
                        style={{ marginLeft: 4, fontStyle: 'normal', fontSize: 11 }}
                      >
                        继承自 {p.inheritedFromOrgName ?? ''}
                      </em>
                    )}
                    {isOwn && admin && (
                      <button
                        type="button"
                        onClick={async () => {
                          setPmoError(null);
                          try {
                            await removeOrganizationPmo(editing.id, p.userId);
                            await refetchPmos();
                          } catch (e) {
                            const err = e as { response?: { data?: { message?: string } } };
                            setPmoError(err?.response?.data?.message ?? '删除失败');
                          }
                        }}
                        data-testid={`org-pmo-remove-${p.userId}`}
                        aria-label="移除 PMO"
                        style={{
                          background: 'transparent',
                          border: 'none',
                          color: 'inherit',
                          cursor: 'pointer',
                          fontSize: 14,
                          padding: 0,
                          lineHeight: 1,
                        }}
                      >
                        ×
                      </button>
                    )}
                  </span>
                );
              })
            )}
          </div>
          {admin && (
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <select
                className="rainier-form-select"
                value={addingPmoUserId}
                onChange={(e) =>
                  setAddingPmoUserId(e.target.value === '' ? '' : Number(e.target.value))
                }
                data-testid="org-pmo-add-select"
                style={{ flex: 1 }}
              >
                <option value="">选择要添加为 PMO 的用户</option>
                {allUsers
                  .filter((u) => !ownPmos.some((p) => p.userId === u.id))
                  .map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.name}（{u.loginName}）
                    </option>
                  ))}
              </select>
              <Button
                type="button"
                variant="secondary"
                disabled={addingPmoUserId === ''}
                onClick={async () => {
                  if (addingPmoUserId === '') return;
                  setPmoError(null);
                  try {
                    await addOrganizationPmo(editing.id, addingPmoUserId);
                    setAddingPmoUserId('');
                    await refetchPmos();
                  } catch (e) {
                    const err = e as { response?: { data?: { message?: string } } };
                    setPmoError(err?.response?.data?.message ?? '添加失败');
                  }
                }}
                data-testid="org-pmo-add-btn"
              >
                添加 PMO
              </Button>
            </div>
          )}
          {pmoError && (
            <div className="rainier-error-banner" data-testid="org-pmo-error">
              {pmoError}
            </div>
          )}
        </div>
      )}
    </Drawer>
  );
}
