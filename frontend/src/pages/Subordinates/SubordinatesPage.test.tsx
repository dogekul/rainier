import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { SubordinatesPage } from './SubordinatesPage';

vi.mock('../../api/subordinates', () => ({
  listSubordinates: vi.fn(),
}));

import { listSubordinates } from '../../api/subordinates';

const mock = listSubordinates as unknown as ReturnType<typeof vi.fn>;

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/me/subordinates']}>
      <Routes>
        <Route path="/me/subordinates" element={<SubordinatesPage />} />
        <Route path="/users/:id/profile" element={<div data-testid="profile-route">profile</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('SubordinatesPage (v0.0.111 H4)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  /** TC-SUB-FE-01: renders one row per subordinate with name / org / weekly / total. */
  it('renders subordinate rows with all columns', async () => {
    mock.mockResolvedValueOnce([
      {
        id: 11,
        loginName: 'bob',
        displayName: 'Bob',
        primaryOrgName: '采购小队',
        contributionSummary: { weeklyTasksDone: 3, totalTasks: 12 },
      },
      {
        id: 12,
        loginName: 'carol',
        displayName: 'Carol',
        primaryOrgName: '采购小队',
        contributionSummary: { weeklyTasksDone: 0, totalTasks: 7 },
      },
    ]);
    renderPage();
    await waitFor(() => expect(screen.getByTestId('subord-row-11')).toBeInTheDocument());
    expect(screen.getByTestId('subord-row-12')).toBeInTheDocument();
    expect(screen.getByTestId('subord-week-11')).toHaveTextContent('3');
    expect(screen.getByTestId('subord-total-11')).toHaveTextContent('12');
    expect(screen.getByText('Bob')).toBeInTheDocument();
    expect(screen.getByText('@bob')).toBeInTheDocument();
  });

  /** TC-SUB-FE-02: empty list → empty state. */
  it('shows the empty state when there are no subordinates', async () => {
    mock.mockResolvedValueOnce([]);
    renderPage();
    await waitFor(() => expect(screen.getByTestId('subord-empty')).toBeInTheDocument());
  });

  /** TC-SUB-FE-03: 查看档案 link points at /users/{id}/profile. */
  it('renders 查看档案 link to /users/{id}/profile', async () => {
    mock.mockResolvedValueOnce([
      {
        id: 42,
        loginName: 'dora',
        displayName: 'Dora',
        primaryOrgName: '前端',
        contributionSummary: { weeklyTasksDone: 1, totalTasks: 4 },
      },
    ]);
    renderPage();
    await waitFor(() => expect(screen.getByTestId('subord-profile-42')).toBeInTheDocument());
    expect(screen.getByTestId('subord-profile-42')).toHaveAttribute('href', '/users/42/profile');
  });
});
