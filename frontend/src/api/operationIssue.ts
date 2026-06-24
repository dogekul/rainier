import client from './client';

export type IssueSeverity = 'HIGH' | 'MEDIUM' | 'LOW';
export type IssueStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';

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
