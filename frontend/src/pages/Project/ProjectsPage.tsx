import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import { OwnerChip, StatusChip } from '../../components/board';
import { PROJECT_STATUS_LABELS } from '../../constants/labels';
import { formatDate } from '../../utils/formatDate';
import {
  createProject,
  deleteProject,
  listProjects,
  PROJECT_TYPE_LABELS,
  PROJECT_TYPE_OPTIONS,
  type Project,
  type ProjectType,
} from '../../api/project';
import { usePaginated } from '../../hooks/usePaginated';
import { ProjectEditDrawer } from './ProjectEditDrawer';

/**
 * v0.0.62 — Projects list page. Row click → /pm/projects/:id detail page (replaces the prior
 * "click 里程碑 to inline-expand MilestonesPanel + click 编辑 for drawer" pattern). 新建 still uses
 * the shared ProjectEditDrawer; edits happen on the detail page.
 */
export function ProjectsPage() {
  const navigate = useNavigate();
  // type filter is folded into the fetcher closure; usePaginated only auto-refetches on
  // page/size/search, so a dedicated effect re-queries when the filter changes.
  const [typeFilter, setTypeFilter] = useState<ProjectType | ''>('');
  const fetcher = useCallback(
    async ({ page, size, search }: { page: number; size: number; search: string }) =>
      listProjects({
        page,
        size,
        search: search || undefined,
        projectType: typeFilter || undefined,
      }),
    [typeFilter],
  );
  const list = usePaginated<Project>(fetcher, { initialSize: 20 });
  const typeFilterMounted = useRef(false);
  useEffect(() => {
    if (!typeFilterMounted.current) {
      typeFilterMounted.current = true;
      return;
    }
    void list.refetch();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [typeFilter]);

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<Project | null>(null);

  const columns: TableColumn<Project>[] = [
    { key: 'code', title: '编码', render: (r) => r.code },
    { key: 'name', title: '名称', render: (r) => r.name },
    {
      key: 'status',
      title: '状态',
      render: (r) => <StatusChip status={r.status} label={PROJECT_STATUS_LABELS[r.status]} />,
    },
    {
      key: 'projectType',
      title: '类型',
      render: (r) => PROJECT_TYPE_LABELS[r.projectType] ?? r.projectType,
    },
    {
      key: 'owner',
      title: '负责人',
      render: (r) => <OwnerChip name={r.ownerName} loginName={r.ownerLoginName} />,
    },
    {
      key: 'organization',
      title: '团队',
      render: (r) => r.organizationName ?? '—',
    },
    { key: 'startDate', title: '开始', render: (r) => formatDate(r.startDate) },
    { key: 'endDate', title: '结束', render: (r) => formatDate(r.endDate) },
    { key: 'enabled', title: '启用', render: (r) => (r.enabled ? '是' : '否') },
    {
      key: 'actions',
      title: '操作',
      render: (r) => (
        <span onClick={(e) => e.stopPropagation()}>
          <Button type="button" variant="secondary" onClick={() => setConfirmDelete(r)}>
            删除
          </Button>
        </span>
      ),
    },
  ];

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2>项目</h2>
        <span className="rainier-spacer" />
        <select
          className="rainier-select"
          value={typeFilter}
          onChange={(e) => setTypeFilter(e.target.value as ProjectType | '')}
          data-testid="projects-type-filter"
          aria-label="类型过滤"
        >
          <option value="">全部类型</option>
          {PROJECT_TYPE_OPTIONS.map((t) => (
            <option key={t} value={t}>
              {PROJECT_TYPE_LABELS[t]}
            </option>
          ))}
        </select>
        <Button
          type="button"
          onClick={() => setDrawerOpen(true)}
          data-testid="projects-new-btn"
        >
          新建项目
        </Button>
      </div>
      <Card>
        <Table<Project>
          columns={columns}
          dataSource={list.items}
          rowKey="id"
          onRowClick={(p) => navigate(`/pm/projects/${p.id}`)}
          rowTestId={(p) => `project-row-${p.id}`}
        />
        <Pagination
          page={list.page}
          size={list.size}
          total={list.total}
          onPageChange={list.setPage}
        />
      </Card>
      <ProjectEditDrawer
        open={drawerOpen}
        editing={null}
        onClose={() => setDrawerOpen(false)}
        onCreate={async (body) => {
          await createProject(body);
          setDrawerOpen(false);
          void list.refetch();
        }}
        onUpdate={async () => {
          // List page only creates; edits happen on the detail page.
        }}
      />
      <ConfirmDialog
        open={confirmDelete !== null}
        title="删除项目"
        message={`确认删除项目「${confirmDelete?.name ?? ''}」？有关联需求/用户角色时会被拒绝。`}
        onCancel={() => setConfirmDelete(null)}
        onConfirm={async () => {
          if (confirmDelete) await deleteProject(confirmDelete.id);
          setConfirmDelete(null);
          void list.refetch();
        }}
      />
    </div>
  );
}
