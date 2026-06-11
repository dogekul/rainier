import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { AuditLogsPage } from './AuditLogsPage';
import { useAuthStore } from '../../store/auth';
import * as auditApi from '../../api/auditLog';

vi.mock('../../api/auditLog', async () => {
  const actual = await vi.importActual<typeof import('../../api/auditLog')>('../../api/auditLog');
  return {
    ...actual,
    listAuditLogs: vi.fn().mockResolvedValue({
      content: [
        {
          id: 1,
          actor: 'alice',
          entityType: 'REQUIREMENT',
          entityId: 5,
          action: 'CREATE',
          summary: 'CREATE REQUIREMENT#5',
          createTime: '2026-06-11T10:00:00Z',
        },
        {
          id: 2,
          actor: 'system',
          entityType: 'PRODUCT',
          entityId: 9,
          action: 'UPDATE',
          summary: 'UPDATE PRODUCT#9',
          createTime: '2026-06-11T09:00:00Z',
        },
      ],
      total: 2,
      page: 0,
      size: 20,
    }),
  };
});

describe('AuditLogsPage', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'tk', user: { username: 'alice' } });
    vi.clearAllMocks();
  });

  /** TC-FES-AUD-002: 渲染审计表格 + 只读（无新建按钮）. */
  it('renders the audit table read-only (TC-FES-AUD-002)', async () => {
    render(<AuditLogsPage />);
    await waitFor(() => {
      expect(screen.getByText('CREATE REQUIREMENT#5')).toBeInTheDocument();
    });
    // 表头列（"操作人/实体类型/实体ID" 同时是过滤标签 → getAllByText ≥1；"动作" 仅列头唯一）.
    expect(screen.getAllByText('操作人').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('实体类型').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('实体ID').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('动作').length).toBeGreaterThanOrEqual(1);
    // 摘要列头唯一（无同名过滤）
    expect(screen.getByText('摘要')).toBeInTheDocument();
    // 2 行内容（summary 唯一）
    expect(screen.getByText('CREATE REQUIREMENT#5')).toBeInTheDocument();
    expect(screen.getByText('UPDATE PRODUCT#9')).toBeInTheDocument();
    // 只读：无新建按钮
    expect(screen.queryByText('新建')).toBeNull();
    expect(screen.queryByText('新建审计')).toBeNull();
  });

  /** TC-FES-AUD-003: 按 entityType 过滤触发查询. */
  it('triggers query with entityType filter (TC-FES-AUD-003)', async () => {
    render(<AuditLogsPage />);
    await waitFor(() => {
      expect(screen.getByText('CREATE REQUIREMENT#5')).toBeInTheDocument();
    });
    // Clear the initial-mount call so the next call is attributable to the 查询 button.
    (auditApi.listAuditLogs as ReturnType<typeof vi.fn>).mockClear();
    fireEvent.change(screen.getByLabelText('实体类型'), {
      target: { value: 'REQUIREMENT' },
    });
    fireEvent.click(screen.getByTestId('audit-query-btn'));
    await waitFor(() => {
      expect(auditApi.listAuditLogs).toHaveBeenCalledWith(
        expect.objectContaining({ entityType: 'REQUIREMENT' }),
      );
    });
    // Every call after the click carries the filter (no stale unfiltered query slipped through).
    const calls = (auditApi.listAuditLogs as ReturnType<typeof vi.fn>).mock.calls;
    expect(calls.every((c) => c[0]?.entityType === 'REQUIREMENT')).toBe(true);
  });
});
