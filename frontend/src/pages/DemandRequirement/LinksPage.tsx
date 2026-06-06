import { useCallback, useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Drawer } from '../../components/ui/Drawer';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import { listDemands, type Demand } from '../../api/demand';
import { listRequirements, type Requirement } from '../../api/requirement';
import {
  createDemandRequirement,
  deleteDemandRequirement,
  listDemandRequirements,
  type DemandRequirementLink,
  type LinkType,
} from '../../api/demandRequirement';
import { usePaginated } from '../../hooks/usePaginated';

const LINK_TYPES: LinkType[] = ['DERIVED', 'RELATED'];

export function LinksPage() {
  const fetcher = useCallback(
    async ({ page, size }: { page: number; size: number; search: string }) =>
      listDemandRequirements({ page, size }),
    [],
  );
  const list = usePaginated<DemandRequirementLink>(fetcher, { initialSize: 20 });

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<DemandRequirementLink | null>(null);

  const [demands, setDemands] = useState<Demand[]>([]);
  const [requirements, setRequirements] = useState<Requirement[]>([]);
  const [demandId, setDemandId] = useState<number | ''>('');
  const [requirementId, setRequirementId] = useState<number | ''>('');
  const [linkType, setLinkType] = useState<LinkType>('RELATED');

  useEffect(() => {
    if (!drawerOpen) return;
    // PageParams 校验 size <= 100；v0 池大小足够。
    void listDemands({ size: 100 }).then((r) => setDemands(r.content));
    void listRequirements({ size: 100 }).then((r) => setRequirements(r.content));
    setDemandId('');
    setRequirementId('');
    setLinkType('RELATED');
  }, [drawerOpen]);

  const columns: TableColumn<DemandRequirementLink>[] = [
    { key: 'demandId', title: '诉求 ID', render: (r) => r.demandId },
    { key: 'requirementId', title: '需求 ID', render: (r) => r.requirementId },
    { key: 'linkType', title: '类型', render: (r) => r.linkType },
    {
      key: 'actions',
      title: '操作',
      render: (r) => (
        <Button type="button" variant="secondary" onClick={() => setConfirmDelete(r)}>
          删除
        </Button>
      ),
    },
  ];

  return (
    <Card>
      <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
        <Button
          type="button"
          onClick={() => setDrawerOpen(true)}
          data-testid="links-new-btn"
        >
          新建关联
        </Button>
      </div>
      <Table<DemandRequirementLink> columns={columns} dataSource={list.items} rowKey="id" />
      <Pagination
        page={list.page}
        size={list.size}
        total={list.total}
        onPageChange={list.setPage}
      />
      <Drawer open={drawerOpen} title="新建关联" onClose={() => setDrawerOpen(false)}>
        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>诉求</label>
          <select
            className="rainier-treeselect-trigger"
            value={demandId}
            onChange={(e) => setDemandId(e.target.value === '' ? '' : Number(e.target.value))}
            data-testid="links-demand-select"
          >
            <option value="">请选择</option>
            {demands.map((d) => (
              <option key={d.id} value={d.id}>
                #{d.id} {d.title}
              </option>
            ))}
          </select>
        </div>
        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>需求</label>
          <select
            className="rainier-treeselect-trigger"
            value={requirementId}
            onChange={(e) =>
              setRequirementId(e.target.value === '' ? '' : Number(e.target.value))
            }
            data-testid="links-req-select"
          >
            <option value="">请选择</option>
            {requirements.map((r) => (
              <option key={r.id} value={r.id}>
                {r.code} — {r.title}
              </option>
            ))}
          </select>
        </div>
        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: 'var(--rainier-color-text-2)' }}>类型</label>
          <select
            className="rainier-treeselect-trigger"
            value={linkType}
            onChange={(e) => setLinkType(e.target.value as LinkType)}
          >
            {LINK_TYPES.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </div>
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <Button type="button" variant="secondary" onClick={() => setDrawerOpen(false)}>
            取消
          </Button>
          <Button
            type="button"
            onClick={async () => {
              if (!demandId || !requirementId) return;
              await createDemandRequirement({ demandId, requirementId, linkType });
              setDrawerOpen(false);
              void list.refetch();
            }}
          >
            保存
          </Button>
        </div>
      </Drawer>
      <ConfirmDialog
        open={confirmDelete !== null}
        title="删除关联"
        message={`确认删除关联（demand #${confirmDelete?.demandId} → requirement #${confirmDelete?.requirementId}）？硬删，不可撤销。`}
        onCancel={() => setConfirmDelete(null)}
        onConfirm={async () => {
          if (confirmDelete) await deleteDemandRequirement(confirmDelete.id);
          setConfirmDelete(null);
          void list.refetch();
        }}
      />
    </Card>
  );
}
