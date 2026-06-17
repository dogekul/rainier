import { useCallback, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import { StatusChip } from '../../components/board';
import {
  createFeature,
  deleteFeature,
  listFeatures,
  updateFeature,
  type Feature,
} from '../../api/feature';
import { usePaginated } from '../../hooks/usePaginated';
import { FeatureEditDrawer } from './FeatureEditDrawer';
import { FeatureSprintsPanel } from './FeatureSprintsPanel';

export function FeaturesPage() {
  const fetcher = useCallback(
    async ({ page, size, search }: { page: number; size: number; search: string }) =>
      listFeatures({ page, size, search: search || undefined }),
    [],
  );
  const list = usePaginated<Feature>(fetcher, { initialSize: 20 });

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editing, setEditing] = useState<Feature | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<Feature | null>(null);
  const [sprintsPanelFor, setSprintsPanelFor] = useState<number | null>(null);

  const columns: TableColumn<Feature>[] = [
    { key: 'code', title: '编码', render: (f) => f.code },
    { key: 'name', title: '名称', render: (f) => f.name },
    {
      key: 'module',
      title: '所属模块',
      render: (f) =>
        f.moduleName ? `${f.moduleName}（${f.moduleCode ?? ''}）` : '—',
    },
    { key: 'status', title: '状态', render: (f) => <StatusChip status={f.status} /> },
    {
      key: 'owner',
      title: '负责人',
      render: (f) =>
        f.ownerName ? `${f.ownerName}（${f.ownerLoginName ?? ''}）` : '—',
    },
    {
      key: 'actions',
      title: '操作',
      render: (f) => (
        <>
          <Button
            type="button"
            variant="secondary"
            onClick={() => {
              setEditing(f);
              setDrawerOpen(true);
            }}
          >
            编辑
          </Button>{' '}
          <Button
            type="button"
            variant="secondary"
            data-testid={`feature-sprints-toggle-${f.id}`}
            onClick={() => setSprintsPanelFor((cur) => (cur === f.id ? null : f.id))}
          >
            所在迭代
          </Button>{' '}
          <Button type="button" variant="secondary" onClick={() => setConfirmDelete(f)}>
            删除
          </Button>
        </>
      ),
    },
  ];

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2 style={{ margin: 0 }}>功能</h2>
        <div style={{ flex: 1 }} />
        <Button
          type="button"
          onClick={() => {
            setEditing(null);
            setDrawerOpen(true);
          }}
          data-testid="feature-new-btn"
        >
          新建功能
        </Button>
      </div>
      <Card>
        <Table<Feature> columns={columns} dataSource={list.items} rowKey="id" />
        {sprintsPanelFor != null && (
          <FeatureSprintsPanel key={sprintsPanelFor} featureId={sprintsPanelFor} />
        )}
        <Pagination
          page={list.page}
          size={list.size}
          total={list.total}
          onPageChange={list.setPage}
        />
      </Card>
      <FeatureEditDrawer
        key={editing?.id ?? 'new'}
        open={drawerOpen}
        editing={editing}
        onClose={() => setDrawerOpen(false)}
        onCreate={async (body) => {
          await createFeature(body);
          setDrawerOpen(false);
          void list.refetch();
        }}
        onUpdate={async (id, body) => {
          await updateFeature(id, body);
          setDrawerOpen(false);
          void list.refetch();
        }}
      />
      <ConfirmDialog
        open={confirmDelete !== null}
        title="删除功能"
        message={`确认删除功能「${confirmDelete?.code ?? ''}」？`}
        onCancel={() => setConfirmDelete(null)}
        onConfirm={async () => {
          if (confirmDelete) await deleteFeature(confirmDelete.id);
          setConfirmDelete(null);
          void list.refetch();
        }}
      />
    </div>
  );
}
