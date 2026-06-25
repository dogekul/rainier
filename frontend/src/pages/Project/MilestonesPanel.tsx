import { useCallback, useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import {
  createMilestone,
  deleteMilestone,
  listMilestones,
  updateMilestone,
  type Milestone,
  type MilestoneStatus,
} from '../../api/milestone';
import { formatDate } from '../../utils/formatDate';

const STATUS_OPTIONS: MilestoneStatus[] = ['PLANNED', 'REACHED', 'MISSED'];
const STATUS_LABELS: Record<MilestoneStatus, string> = {
  PLANNED: '计划中',
  REACHED: '已达成',
  MISSED: '已错过',
};

export interface MilestonesPanelProps {
  projectId: number;
}

/**
 * v0.0.17: inline 里程碑 panel for a Project — lists the project's milestones (by sortOrder) and
 * provides inline create / edit / delete. Mounted from a ProjectsPage row button (SprintFeaturePanel
 * pattern). Project-scoped: every read/write carries this projectId.
 */
export function MilestonesPanel({ projectId }: MilestonesPanelProps) {
  const [items, setItems] = useState<Milestone[]>([]);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [targetDate, setTargetDate] = useState('');
  const [statusVal, setStatusVal] = useState<MilestoneStatus>('PLANNED');
  const [actualDate, setActualDate] = useState('');
  const [sortOrder, setSortOrder] = useState('0');

  const refetch = useCallback(async () => {
    const r = await listMilestones({ projectId, size: 100 });
    setItems(r.content);
  }, [projectId]);

  useEffect(() => {
    void refetch();
  }, [refetch]);

  const resetForm = () => {
    setEditingId(null);
    setCode('');
    setName('');
    setTargetDate('');
    setStatusVal('PLANNED');
    setActualDate('');
    setSortOrder('0');
  };

  const startEdit = (m: Milestone) => {
    setEditingId(m.id);
    setCode(m.code);
    setName(m.name);
    setTargetDate(m.targetDate);
    setStatusVal(m.status);
    setActualDate(m.actualDate ?? '');
    setSortOrder(String(m.sortOrder));
  };

  const save = async () => {
    const common = {
      code,
      name,
      targetDate,
      status: statusVal,
      actualDate: actualDate || undefined,
      sortOrder: sortOrder === '' ? undefined : Number(sortOrder),
    };
    if (editingId !== null) {
      await updateMilestone(editingId, common);
    } else {
      await createMilestone({ projectId, ...common });
    }
    resetForm();
    await refetch();
  };

  return (
    <div
      style={{
        padding: 12,
        background: 'var(--rainier-color-bg-2)',
        borderRadius: 4,
        marginTop: 8,
      }}
      data-testid={`milestones-panel-${projectId}`}
    >
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'flex-end' }}>
        <Input
          label="编码"
          value={code}
          onChange={(e) => setCode(e.target.value)}
          data-testid="milestone-code-input"
        />
        <Input
          label="名称"
          value={name}
          onChange={(e) => setName(e.target.value)}
          data-testid="milestone-name-input"
        />
        <Input
          label="目标日期 (YYYY-MM-DD)"
          value={targetDate}
          onChange={(e) => setTargetDate(e.target.value)}
          data-testid="milestone-targetdate-input"
        />
        <div>
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)', display: 'block' }}>
            状态
          </label>
          <select
            className="rainier-form-select"
            value={statusVal}
            onChange={(e) => setStatusVal(e.target.value as MilestoneStatus)}
            data-testid="milestone-status-select"
          >
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>
                {STATUS_LABELS[s]}
              </option>
            ))}
          </select>
        </div>
        <Input
          label="实际日期 (可空)"
          value={actualDate}
          onChange={(e) => setActualDate(e.target.value)}
          data-testid="milestone-actualdate-input"
        />
        <Input
          label="排序"
          value={sortOrder}
          onChange={(e) => setSortOrder(e.target.value)}
          data-testid="milestone-sortorder-input"
        />
        <Button type="button" onClick={save} data-testid="milestone-save-btn">
          {editingId !== null ? '保存' : '新建里程碑'}
        </Button>
        {editingId !== null && (
          <Button type="button" variant="secondary" onClick={resetForm}>
            取消
          </Button>
        )}
      </div>
      <ul style={{ listStyle: 'none', padding: 0, marginTop: 12 }}>
        {items.map((m) => (
          <li
            key={m.id}
            data-testid={`milestone-row-${m.id}`}
            style={{ display: 'flex', gap: 8, alignItems: 'center', padding: '4px 0' }}
          >
            <span style={{ minWidth: 160 }}>{m.name}</span>
            <span>{STATUS_LABELS[m.status]}</span>
            <span style={{ color: 'var(--rainier-color-text-2)' }}>{formatDate(m.targetDate)}</span>
            <Button type="button" variant="secondary" onClick={() => startEdit(m)}>
              编辑
            </Button>
            <Button
              type="button"
              variant="secondary"
              data-testid={`milestone-delete-btn-${m.id}`}
              onClick={() => {
                void deleteMilestone(m.id).then(refetch);
              }}
            >
              删除
            </Button>
          </li>
        ))}
      </ul>
    </div>
  );
}
