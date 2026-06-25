import { useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { LinkPanel } from '../../components/LinkPanel';
import { listUsers, type User } from '../../api/user';
import { useAuthStore } from '../../store/auth';
import { PRIORITY_LABELS, type Priority } from '../../api/demand';
import type { Complexity } from '../../api/requirement';
import {
  type Story,
  type StoryCreate,
  type StoryStatus,
  type StoryUpdate,
} from '../../api/story';

const STATUS_OPTIONS: StoryStatus[] = [
  'DRAFT',
  'READY',
  'IN_PROGRESS',
  'DONE',
  'BLOCKED',
  'CANCELLED',
];
const PRIORITY_OPTIONS: Priority[] = ['URGENT', 'HIGH', 'MEDIUM', 'LOW', 'LOWEST'];
const COMPLEXITY_OPTIONS: Complexity[] = ['XS', 'S', 'M', 'L', 'XL'];

export interface StoryEditDrawerProps {
  open: boolean;
  /** v0.0.10: parent Sprint context (locked display). */
  sprintId: number;
  sprintCode: string;
  sprintName: string;
  /** Grandparent Requirement display info (also shown in locked panel). */
  requirementCode: string;
  requirementTitle: string;
  /** Edit mode when non-null. */
  editing: Story | null;
  onClose: () => void;
  onCreate: (body: StoryCreate) => Promise<void> | void;
  onUpdate: (id: number, body: StoryUpdate) => Promise<void> | void;
}

export function StoryEditDrawer({
  open,
  sprintId,
  sprintCode,
  sprintName,
  requirementCode,
  requirementTitle,
  editing,
  onClose,
  onCreate,
  onUpdate,
}: StoryEditDrawerProps) {
  const currentLoginName = useAuthStore((s) => s.user?.username ?? null);

  const [users, setUsers] = useState<User[]>([]);
  const [code, setCode] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [acceptanceCriteria, setAcceptanceCriteria] = useState('');
  const [status, setStatus] = useState<StoryStatus>('DRAFT');
  const [priority, setPriority] = useState<Priority>('MEDIUM');
  const [complexity, setComplexity] = useState<Complexity | ''>('');
  const [ownerUserId, setOwnerUserId] = useState<number | ''>('');
  /**
   * v0.0.81: reviewer is patch-like. ''  means "use whatever sentinel below"; we track
   * a separate `reviewerDirty` flag so unchanged loads do NOT send the key (backend
   * keeps existing reviewer). User picks "（无）" → '' + dirty=true sends explicit null.
   */
  const [reviewerUserId, setReviewerUserId] = useState<number | ''>('');
  const [reviewerDirty, setReviewerDirty] = useState(false);
  const [reviewStatus, setReviewStatus] = useState<Story['reviewStatus']>(null);
  const [closeReason, setCloseReason] = useState('');
  // v0.0.9: surface validation errors (sibling of v0.0.8.1 Code-M7 pattern).
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      setFormError(null);
      return;
    }
    // PageParams 校验 size <= 100。
    void listUsers({ size: 100 }).then((r) => {
      setUsers(r.content);
      if (editing) {
        setOwnerUserId(editing.ownerUserId);
      } else {
        // Default to current logged-in user matched by loginName (sibling of ProjectsPage).
        const me = currentLoginName
          ? r.content.find((u) => u.loginName === currentLoginName)
          : undefined;
        setOwnerUserId(me ? me.id : '');
      }
    });
    if (editing) {
      setCode(editing.code);
      setTitle(editing.title);
      setDescription(editing.description ?? '');
      setAcceptanceCriteria(editing.acceptanceCriteria ?? '');
      setStatus(editing.status);
      setPriority(editing.priority);
      setComplexity(editing.complexity ?? '');
      setReviewerUserId(editing.reviewerUserId ?? '');
      setReviewerDirty(false);
      setReviewStatus(editing.reviewStatus ?? null);
      setCloseReason(editing.closeReason ?? '');
    } else {
      setCode('');
      setTitle('');
      setDescription('');
      setAcceptanceCriteria('');
      setStatus('DRAFT');
      setPriority('MEDIUM');
      setComplexity('');
      setReviewerUserId('');
      setReviewerDirty(false);
      setReviewStatus(null);
      setCloseReason('');
    }
  }, [open, editing, currentLoginName]);

  return (
    <Drawer
      open={open}
      title={editing ? '编辑 Story' : '新建 Story'}
      onClose={onClose}
    >
      <div className="rainier-form-group">
        <label className="rainier-form-label">所属 Sprint</label>
        <div
          style={{
            padding: '6px 10px',
            background: 'var(--rainier-color-bg-2)',
            borderRadius: 4,
            fontSize: 13,
          }}
          data-testid="story-drawer-sprint-display"
        >
          {sprintName}（{sprintCode}）— 创建时锁定
        </div>
        <div
          style={{
            padding: '4px 10px',
            background: 'var(--rainier-color-bg-2)',
            borderRadius: 4,
            fontSize: 11,
            color: 'var(--rainier-color-text-2)',
            marginTop: 2,
          }}
          data-testid="story-drawer-requirement-display"
        >
          上级需求：{requirementTitle}（{requirementCode}）
        </div>
      </div>
      <Input
        label="编码 (STR-...)"
        value={code}
        onChange={(e) => setCode(e.target.value)}
        disabled={editing !== null}
      />
      <Input label="标题" value={title} onChange={(e) => setTitle(e.target.value)} />
      <Input
        label="描述"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
      />
      <Input
        label="验收标准"
        value={acceptanceCriteria}
        onChange={(e) => setAcceptanceCriteria(e.target.value)}
      />
      <div className="rainier-form-group">
        <label className="rainier-form-label">状态</label>
        <select
          className="rainier-form-select"
          value={status}
          onChange={(e) => setStatus(e.target.value as StoryStatus)}
          data-testid="story-status-select"
        >
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>
      <div className="rainier-form-group">
        <label className="rainier-form-label">优先级</label>
        <select
          className="rainier-form-select"
          value={priority}
          onChange={(e) => setPriority(e.target.value as Priority)}
        >
          {PRIORITY_OPTIONS.map((p) => (
            <option key={p} value={p}>
              {PRIORITY_LABELS[p]}
            </option>
          ))}
        </select>
      </div>
      <div className="rainier-form-group">
        <label className="rainier-form-label">复杂度</label>
        <select
          className="rainier-form-select"
          value={complexity}
          onChange={(e) => setComplexity((e.target.value as Complexity) || '')}
        >
          <option value="">（不评估）</option>
          {COMPLEXITY_OPTIONS.map((c) => (
            <option key={c} value={c}>
              {c}
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
          data-testid="story-owner-select"
        >
          <option value="">请选择</option>
          {users.map((u) => (
            <option key={u.id} value={u.id}>
              {u.name}（{u.loginName}）
            </option>
          ))}
        </select>
      </div>
      {editing && (
        <div className="rainier-form-group">
          <label className="rainier-form-label">
            评审人（可选，patch-like — 留空发送不修改原值）
          </label>
          <select
            className="rainier-form-select"
            value={reviewerUserId}
            onChange={(e) => {
              setReviewerUserId(e.target.value === '' ? '' : Number(e.target.value));
              setReviewerDirty(true);
            }}
            data-testid="story-reviewer-select"
          >
            <option value="">（无）</option>
            {users.map((u) => (
              <option key={u.id} value={u.id}>
                {u.name}（{u.loginName}）
              </option>
            ))}
          </select>
          {reviewStatus && (
            <div
              style={{
                marginTop: 4,
                fontSize: 11,
                color: 'var(--rainier-color-text-2)',
              }}
              data-testid="story-review-status-chip"
            >
              评审状态：{reviewStatus}（请使用评审队列 /review 端点变更）
            </div>
          )}
        </div>
      )}
      {status === 'CANCELLED' && (
        <Input
          label="取消原因"
          value={closeReason}
          onChange={(e) => setCloseReason(e.target.value)}
        />
      )}
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
          data-testid="story-form-error"
        >
          {formError}
        </div>
      )}
      {/* v0.0.31: relate the PRD / 设计稿 once the Story exists (PO's core action). */}
      {editing && <LinkPanel targetType="STORY" targetId={editing.id} />}
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
            setFormError(null);
            if (editing) {
              // v0.0.81: reviewerUserId only sent when user actively changed it
              // (reviewerDirty). Backend treats omission as "keep existing reviewer"
              // — this is the fix for the PUT-wipes-reviewer bug.
              const updateBody: StoryUpdate = {
                code,
                title,
                description: description || undefined,
                acceptanceCriteria: acceptanceCriteria || undefined,
                status,
                priority,
                complexity: (complexity || undefined) as Complexity | undefined,
                ownerUserId,
                closeReason: closeReason || undefined,
              };
              if (reviewerDirty) {
                updateBody.reviewerUserId = reviewerUserId === '' ? null : reviewerUserId;
              }
              await onUpdate(editing.id, updateBody);
            } else {
              await onCreate({
                code,
                title,
                description: description || undefined,
                acceptanceCriteria: acceptanceCriteria || undefined,
                status,
                priority,
                complexity: (complexity || undefined) as Complexity | undefined,
                sprintId,
                ownerUserId,
                closeReason: closeReason || undefined,
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
