import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';
import type { Priority } from './demand';

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'BLOCKED' | 'CANCELLED';

/** v0.0.50 — 任务状态选项顺序 + 中文标签，供 TasksPage / 工作台 / 驾驶舱 / 编辑抽屉 共用展示。 */
export const TASK_STATUS_OPTIONS: TaskStatus[] = [
  'TODO',
  'IN_PROGRESS',
  'DONE',
  'BLOCKED',
  'CANCELLED',
];
export const TASK_STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: '待办',
  IN_PROGRESS: '进行中',
  DONE: '已完成',
  BLOCKED: '阻塞',
  CANCELLED: '已取消',
};

export interface Task {
  id: number;
  code: string;
  title: string;
  description?: string | null;
  status: TaskStatus;
  priority: Priority;
  projectId: number;
  projectName?: string | null;
  projectCode?: string | null;
  sprintId?: number | null;
  sprintCode?: string | null;
  sprintName?: string | null;
  storyId?: number | null;
  storyCode?: string | null;
  storyTitle?: string | null;
  assigneeUserId?: number | null;
  assigneeName?: string | null;
  assigneeLoginName?: string | null;
  dueDate?: string | null;
  closeReason?: string | null;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

export interface TaskCreate {
  code: string;
  title: string;
  description?: string;
  status?: TaskStatus;
  priority?: Priority;
  projectId: number;
  sprintId?: number | null;
  storyId?: number | null;
  assigneeUserId?: number | null;
  dueDate?: string;
  closeReason?: string;
}

export interface TaskUpdate {
  code: string;
  title: string;
  description?: string;
  status: TaskStatus;
  priority: Priority;
  assigneeUserId?: number | null;
  dueDate?: string;
  closeReason?: string;
}

export interface TaskListParams {
  projectId?: number;
  sprintId?: number;
  storyId?: number;
  status?: TaskStatus;
  priority?: Priority;
  assigneeUserId?: number;
  search?: string;
  page?: number;
  size?: number;
}

export async function listTasks(params: TaskListParams = {}): Promise<PaginatedResult<Task>> {
  const res = await client.get<PaginatedResult<Task>>('/tasks', { params });
  return res.data;
}

export async function getTask(id: number): Promise<Task> {
  const res = await client.get<Task>(`/tasks/${id}`);
  return res.data;
}

export async function createTask(body: TaskCreate): Promise<Task> {
  const res = await client.post<Task>('/tasks', body);
  return res.data;
}

export async function updateTask(id: number, body: TaskUpdate): Promise<Task> {
  const res = await client.put<Task>(`/tasks/${id}`, body);
  return res.data;
}

export async function deleteTask(id: number): Promise<void> {
  await client.delete(`/tasks/${id}`);
}
