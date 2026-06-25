import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CompliancePage } from './CompliancePage';
import {
  getAuditSummary,
  getResidualPermissions,
  revokeResidualRoles,
} from '../../api/compliance';

vi.mock('../../api/compliance', async (orig) => ({
  ...(await orig<typeof import('../../api/compliance')>()),
  getAuditSummary: vi.fn(),
  getResidualPermissions: vi.fn(),
  revokeResidualRoles: vi.fn(),
}));

const SUMMARY = {
  total: 5,
  byAction: [
    { label: 'CREATE', count: 3 },
    { label: 'UPDATE', count: 2 },
  ],
  byEntityType: [{ label: 'TASK', count: 3 }],
  recent: [
    {
      id: 9,
      actor: 'system',
      entityType: 'TASK',
      entityId: 1,
      action: 'CREATE' as const,
      summary: 'CREATE TASK',
      createTime: '2026-06-18T00:00:00Z',
    },
  ],
};

function renderPage() {
  return render(
    <MemoryRouter>
      <CompliancePage />
    </MemoryRouter>,
  );
}

describe('CompliancePage', () => {
  beforeEach(() => {
    vi.mocked(getAuditSummary).mockReset();
    vi.mocked(getResidualPermissions).mockReset();
    vi.mocked(revokeResidualRoles).mockReset();
  });

  /** TC-COMPP-01: renders audit summary + residual table + recent. */
  it('renders summary, residual reconciliation and recent activity (TC-COMPP-01)', async () => {
    vi.mocked(getAuditSummary).mockResolvedValue(SUMMARY);
    vi.mocked(getResidualPermissions).mockResolvedValue([
      { userId: 2, name: 'Ghost', loginName: 'ghost', roleCount: 1, roleNames: ['DEV'] },
    ]);
    renderPage();
    await waitFor(() => expect(screen.getByTestId('compliance-residual-row-2')).toBeInTheDocument());
    expect(screen.getByTestId('compliance-summary')).toHaveTextContent('5');
    expect(screen.getByTestId('compliance-residual-row-2')).toHaveTextContent('ghost');
    expect(screen.getByTestId('compliance-residual-row-2')).toHaveTextContent('DEV');
    expect(screen.getByTestId('compliance-by-action')).toHaveTextContent('CREATE');
    expect(screen.getByTestId('compliance-recent-row-9')).toBeInTheDocument();
  });

  /** TC-COMPP-02: no residual → empty state. */
  it('shows an empty state when there are no residual permissions (TC-COMPP-02)', async () => {
    vi.mocked(getAuditSummary).mockResolvedValue({ ...SUMMARY, recent: [] });
    vi.mocked(getResidualPermissions).mockResolvedValue([]);
    renderPage();
    await waitFor(() => expect(screen.getByTestId('compliance-residual-empty')).toBeInTheDocument());
  });

  /** TC-RPRP-01: 「一键回收」 button calls revokeResidualRoles and reloads the residual list. */
  it('one-click revoke calls API and reloads (TC-RPRP-01)', async () => {
    vi.mocked(getAuditSummary).mockResolvedValue(SUMMARY);
    vi.mocked(getResidualPermissions)
      .mockResolvedValueOnce([
        { userId: 2, name: 'Ghost', loginName: 'ghost', roleCount: 1, roleNames: ['DEV'] },
      ])
      .mockResolvedValueOnce([]);
    vi.mocked(revokeResidualRoles).mockResolvedValue({
      ok: true,
      revokedCount: 1,
      alreadyDisabled: true,
    });

    renderPage();
    const btn = await screen.findByTestId('compliance-residual-revoke-2');
    fireEvent.click(btn);

    await waitFor(() => expect(vi.mocked(revokeResidualRoles)).toHaveBeenCalledWith(2));
    await waitFor(() =>
      expect(screen.getByTestId('compliance-residual-empty')).toBeInTheDocument(),
    );
  });
});
