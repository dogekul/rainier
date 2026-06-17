import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { DemandSubmitPage } from './DemandSubmitPage';
import { useAuthStore } from '../../store/auth';
import { createDemand } from '../../api/demand';

vi.mock('../../api/demand', async (orig) => ({
  ...(await orig<typeof import('../../api/demand')>()),
  createDemand: vi.fn().mockResolvedValue({ id: 42, title: '登录慢', submitterUserId: 5, status: 'PENDING', priority: 'HIGH', source: 'WEB' }),
}));

function renderPage() {
  return render(
    <MemoryRouter>
      <DemandSubmitPage />
    </MemoryRouter>,
  );
}

describe('DemandSubmitPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({ token: 'tk', user: { username: 'biz', id: 5, name: '业务张三', roles: [], projects: [] } });
  });

  /** TC-DL-01: minimal form — read-only submitter, default priority 中, no submitter picker. */
  it('shows a minimal form with read-only submitter and default priority (TC-DL-01)', () => {
    renderPage();
    expect(screen.getByTestId('demand-submit-submitter')).toHaveTextContent('业务张三');
    expect((screen.getByTestId('demand-submit-priority') as HTMLSelectElement).value).toBe('MEDIUM');
    expect(screen.getByTestId('demand-submit-title')).toBeInTheDocument();
    // Only ONE <select> (优先级) — proving there is no submitter / status / source picker.
    expect(screen.getAllByRole('combobox')).toHaveLength(1);
  });

  /** TC-DL-02: empty title → submit disabled. */
  it('disables submit when title is empty (TC-DL-02)', () => {
    renderPage();
    expect(screen.getByTestId('demand-submit-btn')).toBeDisabled();
  });

  /** TC-DL-03: submit sends title + submitterUserId + priority, description omitted. */
  it('submits the essentials with the current user as submitter (TC-DL-03)', async () => {
    renderPage();
    fireEvent.change(screen.getByTestId('demand-submit-title'), { target: { value: '登录慢' } });
    fireEvent.change(screen.getByTestId('demand-submit-priority'), { target: { value: 'HIGH' } });
    fireEvent.click(screen.getByTestId('demand-submit-btn'));
    await waitFor(() => expect(createDemand).toHaveBeenCalledTimes(1));
    expect(createDemand).toHaveBeenCalledWith({ title: '登录慢', submitterUserId: 5, priority: 'HIGH' });
  });

  /** TC-DL-04 + TC-DL-05: success confirmation, then 再提一个 resets. */
  it('shows a confirmation then resets on 再提一个 (TC-DL-04, TC-DL-05)', async () => {
    renderPage();
    fireEvent.change(screen.getByTestId('demand-submit-title'), { target: { value: '登录慢' } });
    fireEvent.click(screen.getByTestId('demand-submit-btn'));
    await waitFor(() => expect(screen.getByTestId('demand-submit-success')).toBeInTheDocument());
    expect(screen.getByTestId('demand-submit-success')).toHaveTextContent('登录慢');
    expect(screen.getByTestId('demand-submit-success')).toHaveTextContent('#42');
    fireEvent.click(screen.getByTestId('demand-submit-again'));
    expect((screen.getByTestId('demand-submit-title') as HTMLInputElement).value).toBe('');
  });

  /** TC-DL-06: null user.id → submit disabled + a guiding message. */
  it('blocks submission when the user has no id (TC-DL-06)', () => {
    useAuthStore.setState({ token: 'tk', user: { username: 'ghost', id: null, roles: [], projects: [] } });
    renderPage();
    expect(screen.getByTestId('demand-submit-no-user')).toBeInTheDocument();
    expect(screen.queryByTestId('demand-submit-btn')).toBeNull();
  });

  /** TC-DL-07: createDemand rejects → inline error, entered title kept. */
  it('keeps the form and shows an error when submit fails (TC-DL-07)', async () => {
    (createDemand as ReturnType<typeof vi.fn>).mockRejectedValueOnce(new Error('boom'));
    renderPage();
    fireEvent.change(screen.getByTestId('demand-submit-title'), { target: { value: '保留我' } });
    fireEvent.click(screen.getByTestId('demand-submit-btn'));
    await waitFor(() => expect(screen.getByTestId('demand-submit-error')).toBeInTheDocument());
    expect((screen.getByTestId('demand-submit-title') as HTMLInputElement).value).toBe('保留我');
  });
});
