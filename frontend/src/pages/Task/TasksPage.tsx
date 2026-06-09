import { useCallback, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Pagination } from '../../components/ui/Pagination';
import { Table, type TableColumn } from '../../components/ui/Table';
import { createTask, deleteTask, listTasks, updateTask, type Task } from '../../api/task';
import { usePaginated } from '../../hooks/usePaginated';
import { TaskEditDrawer } from './TaskEditDrawer';

export function TasksPage() {
  const fetcher = useCallback(
    async ({ page, size, search }: { page: number; size: number; search: string }) =>
      listTasks({ page, size, search: search || undefined }),
    [],
  );
  const list = usePaginated<Task>(fetcher, { initialSize: 20 });

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editing, setEditing] = useState<Task | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<Task | null>(null);

  const columns: TableColumn<Task>[] = [
    { key: 'code', title: '编码', render: (t) => t.code },
    { key: 'title', title: '标题', render: (t) => t.title },
    {
      key: 'project',
      title: '项目',
      render: (t) => (t.projectName ? `${t.projectName}（${t.projectCode ?? ''}）` : '—'),
    },
    {
      key: 'sprint',
      title: 'Sprint',
      render: (t) => (t.sprintCode ? `${t.sprintName ?? ''}（${t.sprintCode}）` : '—'),
    },
    {
      key: 'story',
      title: 'Story',
      render: (t) => (t.storyCode ? `${t.storyTitle ?? ''}（${t.storyCode}）` : '—'),
    },
    {
      key: 'assignee',
      title: '指派人',
      render: (t) =>
        t.assigneeName ? `${t.assigneeName}（${t.assigneeLoginName ?? ''}）` : '待分配',
    },
    { key: 'status', title: '状态', render: (t) => t.status },
    { key: 'priority', title: '优先级', render: (t) => t.priority },
    { key: 'dueDate', title: '到期日', render: (t) => t.dueDate ?? '—' },
    {
      key: 'actions',
      title: '操作',
      render: (t) => (
        <>
          <Button
            type="button"
            variant="secondary"
            onClick={() => {
              setEditing(t);
              setDrawerOpen(true);
            }}
          >
            编辑
          </Button>{' '}
          <Button type="button" variant="secondary" onClick={() => setConfirmDelete(t)}>
            删除
          </Button>
        </>
      ),
    },
  ];

  return (
    <Card>
      <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
        <Button
          type="button"
          onClick={() => {
            setEditing(null);
            setDrawerOpen(true);
          }}
          data-testid="task-new-btn"
        >
          新建任务
        </Button>
      </div>
      <Table<Task> columns={columns} dataSource={list.items} rowKey="id" />
      <Pagination
        page={list.page}
        size={list.size}
        total={list.total}
        onPageChange={list.setPage}
      />
      <TaskEditDrawer
        key={editing?.id ?? 'new'}
        open={drawerOpen}
        editing={editing}
        onClose={() => setDrawerOpen(false)}
        onCreate={async (body) => {
          await createTask(body);
          setDrawerOpen(false);
          void list.refetch();
        }}
        onUpdate={async (id, body) => {
          await updateTask(id, body);
          setDrawerOpen(false);
          void list.refetch();
        }}
      />
      <ConfirmDialog
        open={confirmDelete !== null}
        title="删除任务"
        message={`确认删除任务「${confirmDelete?.code ?? ''}」？`}
        onCancel={() => setConfirmDelete(null)}
        onConfirm={async () => {
          if (confirmDelete) await deleteTask(confirmDelete.id);
          setConfirmDelete(null);
          void list.refetch();
        }}
      />
    </Card>
  );
}
