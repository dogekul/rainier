import { useCallback, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { Input } from '../../components/ui/Input';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import { listAuditLogs, type AuditLog } from '../../api/auditLog';
import { usePaginated } from '../../hooks/usePaginated';

const ACTION_OPTIONS = ['', 'CREATE', 'UPDATE', 'DELETE'] as const;

/** v0.0.15: read-only audit trail browser. No create/edit/delete — append-only domain. */
export function AuditLogsPage() {
  const [actor, setActor] = useState('');
  const [entityType, setEntityType] = useState('');
  const [entityId, setEntityId] = useState('');
  const [action, setAction] = useState('');

  const fetcher = useCallback(
    async ({ page, size }: { page: number; size: number; search: string }) =>
      listAuditLogs({
        page,
        size,
        actor: actor || undefined,
        entityType: entityType || undefined,
        entityId: entityId === '' ? undefined : Number(entityId),
        action: action || undefined,
      }),
    [actor, entityType, entityId, action],
  );
  const list = usePaginated<AuditLog>(fetcher, { initialSize: 20 });

  const columns: TableColumn<AuditLog>[] = [
    { key: 'actor', title: '操作人', render: (a) => a.actor ?? '—' },
    { key: 'entityType', title: '实体类型', render: (a) => a.entityType },
    { key: 'entityId', title: '实体ID', render: (a) => (a.entityId ?? '—').toString() },
    { key: 'action', title: '动作', render: (a) => a.action },
    { key: 'summary', title: '摘要', render: (a) => a.summary ?? '—' },
    { key: 'createTime', title: '时间', render: (a) => a.createTime ?? '—' },
  ];

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2 style={{ margin: 0 }}>审计日志</h2>
        <div style={{ flex: 1 }} />
        <div
          style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'flex-end' }}
          data-testid="audit-filters"
        >
          <Input label="操作人" value={actor} onChange={(e) => setActor(e.target.value)} />
          <Input
            label="实体类型"
            value={entityType}
            onChange={(e) => setEntityType(e.target.value)}
          />
          <Input label="实体ID" value={entityId} onChange={(e) => setEntityId(e.target.value)} />
          <div style={{ marginBottom: 12 }}>
            <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>动作</label>
            <select
              className="rainier-select"
              value={action}
              onChange={(e) => setAction(e.target.value)}
              data-testid="audit-action-select"
            >
              {ACTION_OPTIONS.map((a) => (
                <option key={a} value={a}>
                  {a === '' ? '全部' : a}
                </option>
              ))}
            </select>
          </div>
          <Button
            type="button"
            data-testid="audit-query-btn"
            onClick={() => {
              list.setPage(0);
              void list.refetch();
            }}
          >
            查询
          </Button>
        </div>
      </div>
      <Card>
        <Table<AuditLog> columns={columns} dataSource={list.items} rowKey="id" />
        <Pagination
          page={list.page}
          size={list.size}
          total={list.total}
          onPageChange={list.setPage}
        />
      </Card>
    </div>
  );
}
