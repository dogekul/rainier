import { useEffect, useRef, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { TreeSelect, type TreeNode } from '../../components/ui/TreeSelect';
import {
  PROJECT_TYPE_LABELS,
  PROJECT_TYPE_OPTIONS,
  type Project,
  type ProjectCreate,
  type ProjectStatus,
  type ProjectType,
  type ProjectUpdate,
} from '../../api/project';
import { listUsers, type User } from '../../api/user';
import { listUserOrganizations } from '../../api/userOrganization';
import { getOrganizationTree } from '../../api/organization';
import { listEffectivePmos, type EffectivePmoDetail } from '../../api/organizationPmo';
import { useAuthStore } from '../../store/auth';
import { PROJECT_STATUS_LABELS } from '../../constants/labels';

const STATUS_OPTIONS: ProjectStatus[] = [
  'PLANNING',
  'ACTIVE',
  'ON_HOLD',
  'DELIVERED',
  'ARCHIVED',
];

export interface ProjectEditDrawerProps {
  open: boolean;
  editing: Project | null;
  onClose: () => void;
  onCreate: (body: ProjectCreate) => Promise<void> | void;
  onUpdate: (id: number, body: ProjectUpdate) => Promise<void> | void;
}

/**
 * v0.0.64 — 负责团队 (TreeSelect) + 项目 PMO（联动）。owner 变 → 自动填团队为 owner 主组织；
 * team 变 → 拉 effective-PMOs 重列 + 自动选首条。AbortController-style lastRequestId 防异步竞态。
 */
export function ProjectEditDrawer({
  open,
  editing,
  onClose,
  onCreate,
  onUpdate,
}: ProjectEditDrawerProps) {
  const currentLoginName = useAuthStore((s) => s.user?.username ?? null);

  const [users, setUsers] = useState<User[]>([]);
  const [orgTree, setOrgTree] = useState<TreeNode[]>([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [status, setStatus] = useState<ProjectStatus>('PLANNING');
  const [projectType, setProjectType] = useState<ProjectType>('CASUAL');
  const [ownerUserId, setOwnerUserId] = useState<number | ''>('');
  const [organizationId, setOrganizationId] = useState<number | null>(null);
  const [pmoUserId, setPmoUserId] = useState<number | ''>('');
  const [pmoCandidates, setPmoCandidates] = useState<EffectivePmoDetail[]>([]);
  const [pmoLoading, setPmoLoading] = useState(false);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [enabled, setEnabled] = useState(true);
  const [formError, setFormError] = useState<string | null>(null);

  // v0.0.64 — race protection for pmoCandidates: only accept the latest async response.
  const lastPmoRequestId = useRef(0);

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
    void getOrganizationTree().then((nodes) =>
      setOrgTree(nodes.map((n) => ({ id: n.id, name: n.name, parentId: n.parentId }))),
    );
    if (editing) {
      setName(editing.name);
      setDescription(editing.description ?? '');
      setStatus(editing.status);
      setProjectType(editing.projectType);
      setOrganizationId(editing.organizationId ?? null);
      setPmoUserId(editing.pmoUserId ?? '');
      setStartDate(editing.startDate ?? '');
      setEndDate(editing.endDate ?? '');
      setEnabled(editing.enabled);
    } else {
      setName('');
      setDescription('');
      setStatus('PLANNING');
      setProjectType('CASUAL');
      setOrganizationId(null);
      setPmoUserId('');
      setStartDate('');
      setEndDate('');
      setEnabled(true);
    }
  }, [open, editing, currentLoginName]);

  /**
   * v0.0.64 — Owner 变 → 自动填团队 = owner 的主组织。仅在 CREATE 模式 + 用户未手动改过团队时触发，
   * 避免覆盖用户的显式选择。EDITING 模式保留 server 已存的团队字段。
   */
  useEffect(() => {
    if (!open || editing || ownerUserId === '') return;
    void listUserOrganizations({ userId: Number(ownerUserId), activeOnly: true, size: 100 }).then(
      (r) => {
        const primary = r.content.find((uo) => uo.isPrimary);
        if (primary) {
          setOrganizationId(primary.organizationId);
        }
      },
    );
  }, [open, editing, ownerUserId]);

  /**
   * v0.0.64 — Team 变 → 拉 effective-PMOs 重列 + 默认选首条。lastPmoRequestId 防竞态。
   * EDITING 模式: 保留 editing.pmoUserId 即使不在候选列表里（防止 UI 把存好的值清空）。
   */
  useEffect(() => {
    if (!open) return;
    if (organizationId == null) {
      setPmoCandidates([]);
      if (!editing) setPmoUserId('');
      return;
    }
    const reqId = ++lastPmoRequestId.current;
    setPmoLoading(true);
    void listEffectivePmos(organizationId)
      .then((res) => {
        if (reqId !== lastPmoRequestId.current) return; // stale response
        setPmoCandidates(res);
        // CREATE 模式：默认取首条；EDITING 模式：保留既有值
        if (!editing) {
          setPmoUserId(res.length > 0 ? res[0].userId : '');
        }
      })
      .catch(() => {
        if (reqId === lastPmoRequestId.current) setPmoCandidates([]);
      })
      .finally(() => {
        if (reqId === lastPmoRequestId.current) setPmoLoading(false);
      });
  }, [open, editing, organizationId]);

  const save = async () => {
    if (ownerUserId === '' || ownerUserId === null) {
      setFormError('请选择负责人');
      return;
    }
    setFormError(null);
    const orgId = organizationId == null ? null : organizationId;
    const pmoId = pmoUserId === '' ? null : pmoUserId;
    if (editing) {
      await onUpdate(editing.id, {
        name,
        description,
        status,
        projectType,
        ownerUserId,
        organizationId: orgId,
        pmoUserId: pmoId,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        enabled,
      });
    } else {
      await onCreate({
        name,
        description: description || undefined,
        status,
        projectType,
        ownerUserId,
        organizationId: orgId,
        pmoUserId: pmoId,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        enabled,
      });
    }
  };

  return (
    <Drawer
      open={open}
      title={editing ? '编辑项目' : '新建项目'}
      onClose={onClose}
      footer={
        <>
          <Button type="button" variant="secondary" onClick={onClose}>
            取消
          </Button>
          <Button type="button" onClick={() => void save()} disabled={pmoLoading}>
            保存
          </Button>
        </>
      }
    >
      <Input label="名称" value={name} onChange={(e) => setName(e.target.value)} />
      <Input
        label="描述"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
      />
      <div className="rainier-form-group">
        <label className="rainier-form-label">状态</label>
        <select
          className="rainier-form-select"
          value={status}
          onChange={(e) => setStatus(e.target.value as ProjectStatus)}
          data-testid="projects-status-select"
        >
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>
              {PROJECT_STATUS_LABELS[s] ?? s}
            </option>
          ))}
        </select>
      </div>
      <div className="rainier-form-group">
        <label className="rainier-form-label">项目类型</label>
        <select
          className="rainier-form-select"
          value={projectType}
          onChange={(e) => setProjectType(e.target.value as ProjectType)}
          data-testid="projects-type-select"
        >
          {PROJECT_TYPE_OPTIONS.map((t) => (
            <option key={t} value={t}>
              {PROJECT_TYPE_LABELS[t]}
            </option>
          ))}
        </select>
      </div>
      <div className="rainier-form-group">
        <label className="rainier-form-label">负责人（默认为当前登录用户，可改）</label>
        <select
          className="rainier-form-select"
          value={ownerUserId}
          onChange={(e) => {
            setOwnerUserId(e.target.value === '' ? '' : Number(e.target.value));
            setFormError(null);
          }}
          data-testid="projects-owner-select"
        >
          <option value="">请选择</option>
          {users.map((u) => (
            <option key={u.id} value={u.id}>
              {u.name}（{u.loginName}）
            </option>
          ))}
        </select>
      </div>
      <div className="rainier-form-group">
        <label className="rainier-form-label">负责团队（默认取负责人主组织，可改）</label>
        <TreeSelect
          value={organizationId}
          nodes={orgTree}
          onChange={(id) => setOrganizationId(id)}
          placeholder="选择团队"
        />
      </div>
      <div className="rainier-form-group">
        <label className="rainier-form-label">
          项目 PMO {pmoLoading ? '· 加载中…' : ''}
        </label>
        <select
          className="rainier-form-select"
          value={pmoUserId}
          onChange={(e) => setPmoUserId(e.target.value === '' ? '' : Number(e.target.value))}
          disabled={pmoLoading}
          data-testid="projects-pmo-select"
        >
          <option value="">未指定</option>
          {pmoCandidates.map((c) => {
            const label =
              c.inheritedFromOrgId === organizationId
                ? `${c.userName ?? c.userLoginName ?? '#' + c.userId}`
                : `${c.userName ?? c.userLoginName ?? '#' + c.userId}（继承自 ${c.inheritedFromOrgName ?? ''}）`;
            return (
              <option key={c.userId} value={c.userId}>
                {label}
              </option>
            );
          })}
        </select>
      </div>
      {formError && (
        <div className="rainier-error-banner" data-testid="projects-form-error">
          {formError}
        </div>
      )}
      <Input
        label="开始日期 (YYYY-MM-DD)"
        value={startDate}
        onChange={(e) => setStartDate(e.target.value)}
      />
      <Input
        label="结束日期 (YYYY-MM-DD)"
        value={endDate}
        onChange={(e) => setEndDate(e.target.value)}
      />
      <div className="rainier-form-group">
        <label style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          <input
            type="checkbox"
            checked={enabled}
            onChange={(e) => setEnabled(e.target.checked)}
          />
          启用
        </label>
      </div>
    </Drawer>
  );
}
