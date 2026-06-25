import { useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
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
 * v0.0.62 — extracted from ProjectsPage so the new ProjectDetailPage can also edit. Form
 * fields/validation are identical to the previous inline drawer; pure refactor + reuse.
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
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [status, setStatus] = useState<ProjectStatus>('PLANNING');
  const [projectType, setProjectType] = useState<ProjectType>('CASUAL');
  const [ownerUserId, setOwnerUserId] = useState<number | ''>('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [enabled, setEnabled] = useState(true);
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
      setName(editing.name);
      setDescription(editing.description ?? '');
      setStatus(editing.status);
      setProjectType(editing.projectType);
      setStartDate(editing.startDate ?? '');
      setEndDate(editing.endDate ?? '');
      setEnabled(editing.enabled);
    } else {
      setName('');
      setDescription('');
      setStatus('PLANNING');
      setProjectType('CASUAL');
      setStartDate('');
      setEndDate('');
      setEnabled(true);
    }
  }, [open, editing, currentLoginName]);

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
                  name,
                  description,
                  status,
                  projectType,
                  ownerUserId,
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
                  startDate: startDate || undefined,
                  endDate: endDate || undefined,
                  enabled,
                });
              }
            }}
          >
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
