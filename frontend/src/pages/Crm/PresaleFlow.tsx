import { useCallback, useEffect, useState } from 'react';
import { DashboardCard, EmptyState, StatTiles, StatusChip } from '../../components/board';
import { Button } from '../../components/ui/Button';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Drawer } from '../../components/ui/Drawer';
import { Input } from '../../components/ui/Input';
import { listUsers, type User } from '../../api/user';
import {
  advanceOpportunity,
  createOpportunity,
  listOpportunities,
  OPP_GATE_STAGES,
  OPP_PRESALE_STAGES,
  OPP_STAGE_LABELS,
  type Opportunity,
} from '../../api/opportunity';

const PRESALE_SET = new Set<string>(OPP_PRESALE_STAGES);

/**
 * v0.0.44 —「售前流转」(pre-sales operations). The actionable surface for 售前 opportunities (status=OPEN,
 * stage ∈ 线索..合同签订): 新建商机(线索) + 逐节点推进 + 关口决策(商机/投标/合同 通过/否决，否决→丢单). 赢单(合同 PASS)
 * 后商机自动进入实施环节，离开本页。只读进展总览在「商机看板」；实施操作在「实施流转」。
 */
export function PresaleFlow() {
  const [rows, setRows] = useState<Opportunity[]>([]);
  const [busyId, setBusyId] = useState<number | null>(null);
  // 否决 → 丢单 is terminal/irreversible; guard it behind a confirm.
  const [rejectId, setRejectId] = useState<number | null>(null);

  // create-drawer state (moved here from the now read-only 商机看板)
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [users, setUsers] = useState<User[]>([]);
  const [customerName, setCustomerName] = useState('');
  const [title, setTitle] = useState('');
  const [amount, setAmount] = useState('');
  const [commercial, setCommercial] = useState<number | ''>('');
  const [solution, setSolution] = useState<number | ''>('');
  const [pm, setPm] = useState<number | ''>('');
  const [ops, setOps] = useState<number | ''>('');
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(() => {
    return listOpportunities({ size: 100 })
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
    setAmount('');
    setCommercial('');
    setSolution('');
    setPm('');
    setOps('');
    void listUsers({ size: 100 }).then((r) => setUsers(r.content));
  }, [drawerOpen]);

  // 售前在办 = OPEN 且 stage ∈ 售前环节。
  const items = rows.filter((r) => r.status === 'OPEN' && PRESALE_SET.has(r.stage));
  const openAmount = items.reduce((sum, r) => sum + (r.amount ?? 0), 0);

  const save = async () => {
    if (!customerName.trim() || !title.trim()) {
      setFormError('请填写客户名称和商机标题');
      return;
    }
    if (amount.trim() && Number.isNaN(Number(amount))) {
      setFormError('金额必须是数字');
      return;
    }
    setFormError(null);
    setSaving(true);
    try {
      await createOpportunity({
        customerName: customerName.trim(),
        title: title.trim(),
        amount: amount.trim() ? Number(amount) : undefined,
        commercialOwnerUserId: commercial === '' ? undefined : commercial,
        solutionOwnerUserId: solution === '' ? undefined : solution,
        pmUserId: pm === '' ? undefined : pm,
        opsOwnerUserId: ops === '' ? undefined : ops,
      });
      setDrawerOpen(false);
      await load();
    } finally {
      setSaving(false);
    }
  };

  const advance = async (id: number, decision?: 'PASS' | 'REJECT') => {
    setBusyId(id);
    try {
      await advanceOpportunity(id, decision);
      await load();
    } finally {
      setBusyId(null);
    }
  };

  const ownerSelect = (
    label: string,
    value: number | '',
    setter: (v: number | '') => void,
    testId: string,
  ) => (
    <div style={{ marginBottom: 12 }}>
      <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>{label}</label>
      <select
        className="rainier-treeselect-trigger"
        value={value}
        onChange={(e) => setter(e.target.value === '' ? '' : Number(e.target.value))}
        data-testid={testId}
      >
        <option value="">（未指定）</option>
        {users.map((u) => (
          <option key={u.id} value={u.id}>
            {u.name}（{u.loginName}）
          </option>
        ))}
      </select>
    </div>
  );

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2 style={{ margin: 0 }}>售前流转</h2>
        <div style={{ flex: 1 }} />
        <Button type="button" onClick={() => setDrawerOpen(true)} data-testid="presale-new-btn">
          新建商机
        </Button>
      </div>

      <StatTiles
        testId="presale-summary"
        tiles={[
          { label: '售前在办', value: items.length, tier: items.length > 0 ? 'yellow' : 'gray' },
          { label: '在谈金额', value: openAmount },
        ]}
      />

      {items.length === 0 ? (
        <EmptyState message="当前没有售前在办的商机。点「新建商机」创建线索。" testId="presale-empty" />
      ) : (
        <DashboardCard title="售前在办商机" testId="presale-list">
          <table className="rainier-list-table">
            <tbody>
              {items.map((r) => {
                const isGate = OPP_GATE_STAGES.includes(r.stage);
                return (
                  <tr key={r.id} data-testid={`presale-row-${r.id}`}>
                    <td style={{ padding: '6px 8px', width: 130 }}>
                      <StatusChip
                        status={r.stage}
                        label={OPP_STAGE_LABELS[r.stage] + (isGate ? ' ⭐' : '')}
                        testId={`presale-stage-${r.id}`}
                      />
                    </td>
                    <td style={{ padding: '6px 8px' }}>
                      <div style={{ fontWeight: 600 }}>{r.customerName}</div>
                      <div style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>{r.title}</div>
                    </td>
                    <td style={{ padding: '6px 8px', width: 110 }}>
                      {r.amount != null ? `¥${r.amount}` : '—'}
                    </td>
                    <td style={{ padding: '6px 8px', width: 130 }}>
                      {r.commercialOwnerName ?? r.solutionOwnerName ?? '—'}
                    </td>
                    <td style={{ padding: '6px 8px', width: 180, textAlign: 'right' }}>
                      {isGate ? (
                        <>
                          <Button
                            type="button"
                            variant="primary"
                            disabled={busyId === r.id}
                            onClick={() => void advance(r.id, 'PASS')}
                            data-testid={`presale-pass-${r.id}`}
                          >
                            通过
                          </Button>
                          <Button
                            type="button"
                            variant="secondary"
                            style={{ marginLeft: 6 }}
                            disabled={busyId === r.id}
                            onClick={() => setRejectId(r.id)}
                            data-testid={`presale-reject-${r.id}`}
                          >
                            否决
                          </Button>
                        </>
                      ) : (
                        <Button
                          type="button"
                          variant="primary"
                          disabled={busyId === r.id}
                          onClick={() => void advance(r.id)}
                          data-testid={`presale-advance-${r.id}`}
                        >
                          推进
                        </Button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </DashboardCard>
      )}

      <Drawer open={drawerOpen} title="新建商机" onClose={() => setDrawerOpen(false)}>
        <Input
          label="客户名称"
          value={customerName}
          onChange={(e) => setCustomerName(e.target.value)}
          data-testid="presale-new-customer"
        />
        <Input
          label="商机标题"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          data-testid="presale-new-title"
        />
        <Input
          label="金额（元，可空）"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          data-testid="presale-new-amount"
        />
        {ownerSelect('商务负责人', commercial, setCommercial, 'presale-owner-commercial')}
        {ownerSelect('解决方案负责人', solution, setSolution, 'presale-owner-solution')}
        {ownerSelect('项目经理', pm, setPm, 'presale-owner-pm')}
        {ownerSelect('运营经理', ops, setOps, 'presale-owner-ops')}
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
            data-testid="presale-form-error"
          >
            {formError}
          </div>
        )}
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <Button type="button" variant="secondary" onClick={() => setDrawerOpen(false)}>
            取消
          </Button>
          <Button
            type="button"
            disabled={saving}
            onClick={() => void save()}
            data-testid="presale-save-btn"
          >
            保存
          </Button>
        </div>
      </Drawer>

      <ConfirmDialog
        open={rejectId != null}
        title="确认否决（丢单）"
        message="否决后该商机将标记为「丢单」，不可恢复。确认否决？"
        confirmText="确认否决"
        onConfirm={() => {
          const id = rejectId;
          setRejectId(null);
          if (id != null) void advance(id, 'REJECT');
        }}
        onCancel={() => setRejectId(null)}
      />
    </div>
  );
}
