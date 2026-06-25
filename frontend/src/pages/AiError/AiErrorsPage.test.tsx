import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AiErrorsPage } from './AiErrorsPage';
import { fixAiError, listAiErrors, type AiError } from '../../api/aiErrors';
import { useAuthStore } from '../../store/auth';

vi.mock('../../api/aiErrors', async (orig) => ({
  ...(await orig<typeof import('../../api/aiErrors')>()),
  listAiErrors: vi.fn(),
  fixAiError: vi.fn(() => Promise.resolve({} as AiError)),
}));

function err(id: number, status: AiError['status'] = 'OPEN'): AiError {
  return {
    id,
    occurredAt: '2026-06-25T10:00:00',
    aiAction: 'STATUS_SYNC',
    errorDesc: 'AI 误判任务状态',
    affectedEntityType: 'Task',
    affectedEntityId: 42,
    status,
  };
}

function page(rows: AiError[]) {
  return { content: rows, total: rows.length, page: 0, size: 100 };
}

function renderPage() {
  return render(
    <MemoryRouter>
      <AiErrorsPage />
    </MemoryRouter>,
  );
}

const ADMIN = {
  username: 'alice',
  roles: [{ id: 1, name: 'admin', adminAccess: true }],
};
const PLAIN = {
  username: 'bob',
  roles: [{ id: 2, name: 'engineer', adminAccess: false }],
};

describe('AiErrorsPage', () => {
  beforeEach(() => {
    vi.mocked(fixAiError).mockClear();
    vi.mocked(listAiErrors).mockReset();
    useAuthStore.setState({ user: PLAIN as unknown as ReturnType<typeof useAuthStore.getState>['user'] });
  });

  /** TC-AIEP-01: renders title and an OPEN row with status chip. */
  it('renders the error board and rows (TC-AIEP-01)', async () => {
    vi.mocked(listAiErrors).mockResolvedValue(page([err(7)]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('ai-error-row-7')).toBeInTheDocument());
    expect(screen.getByText('AI 错误公示板')).toBeInTheDocument();
    expect(screen.getByTestId('ai-error-row-7')).toHaveTextContent('AI 误判任务状态');
    expect(screen.getByTestId('ai-error-summary')).toBeInTheDocument();
    // Plain user: NO 标记修复 button shown.
    expect(screen.queryByTestId('ai-error-fix-7')).not.toBeInTheDocument();
  });

  /** TC-AIEP-02: admin sees the 标记修复 button and submitting calls fixAiError. */
  it('admin can mark an OPEN error as fixed (TC-AIEP-02)', async () => {
    useAuthStore.setState({ user: ADMIN as unknown as ReturnType<typeof useAuthStore.getState>['user'] });
    vi.mocked(listAiErrors)
      .mockResolvedValueOnce(page([err(9)]))
      .mockResolvedValueOnce(page([err(9, 'FIXED')]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('ai-error-fix-9')).toBeInTheDocument());

    fireEvent.click(screen.getByTestId('ai-error-fix-9'));
    fireEvent.change(screen.getByTestId('ai-error-fix-action'), {
      target: { value: '已重跑同步任务' },
    });
    fireEvent.click(screen.getByTestId('ai-error-fix-submit'));

    await waitFor(() => expect(fixAiError).toHaveBeenCalledWith(9, '已重跑同步任务'));
    await waitFor(() => expect(listAiErrors).toHaveBeenCalledTimes(2));
  });

  /** TC-AIEP-03: empty list → empty state. */
  it('shows an empty state when there are no errors (TC-AIEP-03)', async () => {
    vi.mocked(listAiErrors).mockResolvedValue(page([]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('ai-error-empty')).toBeInTheDocument());
  });
});
