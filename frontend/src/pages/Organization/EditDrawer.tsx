import { useEffect, useState } from 'react';
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
    } else {
      setParentId(null);
      setType('DEPARTMENT');
      setCode('');
      setName('');
      setDescription('');
      setEnabled(true);
    }
  }, [open, editing]);

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
    </Drawer>
  );
}
