import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { StatTiles } from '../../components/board';
import { Button } from '../../components/ui/Button';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { listUsers, type User } from '../../api/user';
import {
  advanceOperation,
  createOperation,
  listOperations,
  OPR_STAGE_LABELS,
  OPR_STAGE_ORDER,
  type Operation,
  type OperationStage,
} from '../../api/operation';

/**
 * v0.0.44 — 运营看板 (after-sales pipeline). Columns by stage (回款维保→运营→复购); ACTIVE engagements show
 * as cards with 推进 (advancing from 复购 closes them). 新建运营单 opens a form (客户/标题/运营负责人). The
 *「售后环节」of the 客户全流程.
 */
export function OperationBoard() {
  const navigate = useNavigate();
  const [rows, setRows] = useState<Operation[]>([]);
  const [busyId, setBusyId] = useState<number | null>(null);

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [users, setUsers] = useState<User[]>([]);
  const [customerName, setCustomerName] = useState('');
  const [title, setTitle] = useState('');
  const [owner, setOwner] = useState<number | ''>('');
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(() => {
    // size capped at 100 by PageParams.
    return listOperations({ size: 100 })
      .then((r) => setRows(r.content))
      .catch(() => setRows([]));
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!drawerOpen) {
      setFormError(null);
      return;
    }
    setCustomerName('');
    setTitle('');
    setOwner('');
    void listUsers({ size: 100 }).then((r) => setUsers(r.content));
  }, [drawerOpen]);

  const active = rows.filter((r) => r.status === 'ACTIVE');
  const closed = rows.filter((r) => r.status === 'CLOSED').length;

  const save = async () => {
    if (!customerName.trim() || !title.trim()) {
      setFormError('请填写客户名称和运营单标题');
      return;
    }
    setFormError(null);
    setSaving(true);
    try {
      await createOperation({
        customerName: customerName.trim(),
        title: title.trim(),
        opsOwnerUserId: owner === '' ? undefined : owner,
      });
      setDrawerOpen(false);
      await load();
    } finally {
      setSaving(false);
    }
  };

  const advance = async (id: number) => {
    setBusyId(id);
    try {
      await advanceOperation(id);
      await load();
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2>运营看板</h2>
        <span className="rainier-spacer" />
        <Button type="button" onClick={() => setDrawerOpen(true)} data-testid="opr-new-btn">
          新建运营单
        </Button>
      </div>

      <StatTiles
        testId="opr-summary"
        tiles={[
          { label: '运营中', value: active.length, tier: 'green' },
          { label: '已关闭', value: closed, tier: 'gray' },
        ]}
      />

      <div style={{ display: 'flex', gap: 10, overflowX: 'auto', paddingBottom: 4 }}>
        {OPR_STAGE_ORDER.map((stage: OperationStage) => {
          const col = active.filter((r) => r.stage === stage);
          return (
            <div
              key={stage}
              data-testid={`opr-col-${stage}`}
              style={{
                minWidth: 220,
                flex: '1 0 220px',
                border: '1px solid var(--rainier-border)',
                borderRadius: 8,
                padding: 8,
                background: 'var(--rainier-bg-hover)',
              }}
            >
              <div style={{ fontWeight: 600, fontSize: 13, marginBottom: 6 }}>
                {OPR_STAGE_LABELS[stage]}{' '}
                <span style={{ color: 'var(--rainier-color-text-2)' }}>({col.length})</span>
              </div>
              {col.map((r) => (
                <div
                  key={r.id}
                  data-testid={`opr-card-${r.id}`}
                  style={{
                    border: '1px solid var(--rainier-border)',
                    borderRadius: 6,
                    padding: '6px 8px',
                    marginBottom: 6,
                    background: 'var(--rainier-color-bg, #fff)',
                  }}
                >
                  <div style={{ fontWeight: 600, fontSize: 13 }}>{r.customerName}</div>
                  <div style={{ fontSize: 13 }}>{r.title}</div>
                  <div style={{ marginTop: 4, display: 'flex', gap: 6 }}>
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={() => navigate(`/crm/operations/${r.id}`)}
                      data-testid={`opr-detail-${r.id}`}
                    >
                      详情
                    </Button>
                    <Button
                      type="button"
                      variant="primary"
                      disabled={busyId === r.id}
                      onClick={() => void advance(r.id)}
                      data-testid={`opr-advance-${r.id}`}
                    >
                      推进
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          );
        })}
      </div>

      <Drawer open={drawerOpen} title="新建运营单" onClose={() => setDrawerOpen(false)}>
        <Input
          label="客户名称"
          value={customerName}
          onChange={(e) => setCustomerName(e.target.value)}
          data-testid="opr-new-customer"
        />
        <Input
          label="运营单标题"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          data-testid="opr-new-title"
        />
        <div className="rainier-form-group">
          <label className="rainier-form-label">运营负责人</label>
          <select
            className="rainier-form-select"
            value={owner}
            onChange={(e) => setOwner(e.target.value === '' ? '' : Number(e.target.value))}
            data-testid="opr-owner-select"
          >
            <option value="">（未指定）</option>
            {users.map((u) => (
              <option key={u.id} value={u.id}>
                {u.name}（{u.loginName}）
              </option>
            ))}
          </select>
        </div>
        {formError && (
          <div className="rainier-error-banner" data-testid="opr-form-error">
            {formError}
          </div>
        )}
        <div className="rainier-form-footer">
          <Button type="button" variant="secondary" onClick={() => setDrawerOpen(false)}>
            取消
          </Button>
          <Button type="button" disabled={saving} onClick={() => void save()} data-testid="opr-save-btn">
            保存
          </Button>
        </div>
      </Drawer>
    </div>
  );
}
