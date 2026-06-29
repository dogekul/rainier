import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { PmoPortfolioPage } from './PmoPortfolioPage';
import { getPmoPortfolio } from '../../api/pmoPortfolio';

function project(id: number, code: string, ryg: string) {
  return {
    projectId: id,
    projectCode: code,
    projectName: code,
    projectStatus: 'ACTIVE',
    organizationId: null,
    openTasks: 3,
    overdueTasks: 0,
    blockedTasks: 0,
    overdueMilestones: 0,
    ryg,
  };
}

vi.mock('../../api/pmoPortfolio', async (orig) => ({
  ...(await orig<typeof import('../../api/pmoPortfolio')>()),
  getPmoPortfolio: vi.fn((groupBy: string) =>
    Promise.resolve(
      groupBy === 'owner'
        ? [
            {
              group: { id: 7, name: 'Alice', type: 'USER' },
              projects: [project(101, 'P-OWN', 'GREEN')],
              rygCount: { red: 0, yellow: 0, green: 1, gray: 0 },
            },
          ]
        : [
            {
              group: { id: 1, name: 'Engineering', type: 'DEPARTMENT' },
              projects: [project(11, 'P11', 'RED'), project(12, 'P12', 'YELLOW')],
              rygCount: { red: 1, yellow: 1, green: 0, gray: 0 },
            },
            {
              group: { id: 2, name: 'Product', type: 'DEPARTMENT' },
              projects: [project(21, 'P21', 'GREEN')],
              rygCount: { red: 0, yellow: 0, green: 1, gray: 0 },
            },
          ],
    ),
  ),
}));

function renderPage() {
  return render(
    <MemoryRouter>
      <PmoPortfolioPage />
    </MemoryRouter>,
  );
}

describe('PmoPortfolioPage', () => {
  /** TC-PMOFE-01: default groupBy=organization renders group cards + RYG chips + project rows. */
  it('renders organization-grouped cards by default (TC-PMOFE-01)', async () => {
    renderPage();
    await waitFor(() =>
      expect(screen.getByTestId('pmo-group-DEPARTMENT-1')).toBeInTheDocument(),
    );
    expect(screen.getByTestId('pmo-group-DEPARTMENT-2')).toBeInTheDocument();
    expect(screen.getByTestId('pmo-group-DEPARTMENT-1-red')).toHaveTextContent('红 1');
    expect(screen.getByTestId('pmo-row-11')).toBeInTheDocument();
    expect(screen.getByTestId('pmo-ryg-11')).toHaveAttribute('data-tier', 'red');
    expect(screen.getByTestId('pmo-row-11').querySelector('a')).toHaveAttribute(
      'href',
      '/pm/tasks?projectId=11',
    );
  });

  /** TC-PMOFE-02: switching groupBy triggers a refetch with the new dimension. */
  it('refetches when groupBy toggles (TC-PMOFE-02)', async () => {
    renderPage();
    await waitFor(() =>
      expect(screen.getByTestId('pmo-group-DEPARTMENT-1')).toBeInTheDocument(),
    );
    fireEvent.change(screen.getByTestId('pmo-groupby'), { target: { value: 'owner' } });
    await waitFor(() => expect(screen.getByTestId('pmo-group-USER-7')).toBeInTheDocument());
    expect(getPmoPortfolio).toHaveBeenCalledWith('owner');
    expect(screen.getByTestId('pmo-row-101')).toBeInTheDocument();
  });
});
