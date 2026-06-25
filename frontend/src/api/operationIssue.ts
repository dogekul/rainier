import client from './client';

export type IssueSeverity = 'HIGH' | 'MEDIUM' | 'LOW';
export type IssueStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'CONVERTED';

export const ISSUE_SEVERITY_LABELS: Record<IssueSeverity, string> = {
  HIGH: '高',
  MEDIUM: '中',
  LOW: '低',
};
export const ISSUE_STATUS_LABELS: Record<IssueStatus, string> = {
  OPEN: '待处理',
  IN_PROGRESS: '处理中',
  RESOLVED: '已解决',
  CLOSED: '已关闭',
  CONVERTED: '已转工单',
};

/** Map IssueStatus → board status tier (tints in the chip). */
export function issueStatusTier(s: IssueStatus): 'red' | 'yellow' | 'green' | 'gray' {
  switch (s) {
    case 'OPEN':
      return 'red';
    case 'IN_PROGRESS':
      return 'yellow';
    case 'RESOLVED':
      return 'green';
    case 'CLOSED':
    case 'CONVERTED':
    default:
      return 'gray';
  }
}

export interface OperationIssue {
  id: number;
  operationId: number;
  title: string;
  description?: string | null;
  severity: IssueSeverity;
  status: IssueStatus;
  reporterUserId: number;
  reporterName?: string | null;
  assigneeUserId?: number | null;
  assigneeName?: string | null;
  closeReason?: string | null;
  createTime?: string;
  updateTime?: string;
}

export interface OperationIssueCreate {
  title: string;
  description?: string;
  severity?: IssueSeverity;
  reporterUserId: number;
  assigneeUserId?: number;
}

export interface OperationIssueUpdate {
  title: string;
  description?: string;
  severity?: IssueSeverity;
  status?: IssueStatus;
  assigneeUserId?: number | null;
  closeReason?: string;
}

export async function listOperationIssues(operationId: number): Promise<OperationIssue[]> {
  const res = await client.get<OperationIssue[]>(`/operations/${operationId}/issues`);
  return res.data;
}

export async function createOperationIssue(
  operationId: number,
  body: OperationIssueCreate,
): Promise<OperationIssue> {
  const res = await client.post<OperationIssue>(`/operations/${operationId}/issues`, body);
  return res.data;
}

export async function updateOperationIssue(
  id: number,
  body: OperationIssueUpdate,
): Promise<OperationIssue> {
  const res = await client.put<OperationIssue>(`/operation-issues/${id}`, body);
  return res.data;
}

export async function deleteOperationIssue(id: number): Promise<void> {
  await client.delete(`/operation-issues/${id}`);
}

/** v0.0.95 — paged issues for the Operation detail board. */
export interface PagedOperationIssues {
  content: OperationIssue[];
  page: number;
  size: number;
  total: number;
}

export async function listOperationIssuesPaged(
  operationId: number,
  params: { page?: number; size?: number; status?: IssueStatus; severity?: IssueSeverity } = {},
): Promise<PagedOperationIssues> {
  const res = await client.get<PagedOperationIssues>(`/operations/${operationId}/issues/page`, {
    params,
  });
  return res.data;
}

/** v0.0.95 — convert an issue to a Task; returns the created Task (subset). */
export interface ConvertedTask {
  id: number;
  code: string;
  title: string;
  projectId: number;
}

export async function convertOperationIssueToTask(
  issueId: number,
  projectId: number,
): Promise<ConvertedTask> {
  const res = await client.post<ConvertedTask>(`/operation-issues/${issueId}/convert-to-task`, {
    projectId,
  });
  return res.data;
}
