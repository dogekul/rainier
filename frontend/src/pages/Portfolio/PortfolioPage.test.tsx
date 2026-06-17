import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { PortfolioPage } from './PortfolioPage';
import { getPortfolio } from '../../api/portfolio';

function row(id: number, code: string, ryg: string, overdue = 0) {
  return {
    projectId: id, projectCode: code, projectName: code, projectStatus: 'ACTIVE',
    organizationId: null, openTasks: 5, overdueTasks: overdue, blockedTasks: 0, overdueMilestones: 0, ryg,
  };
}

vi.mock('../../api/portfolio', async (orig) => ({
  ...(await orig<typeof import('../../api/portfolio')>()),
  getPortfolio: vi.fn((scope: string) =>
    Promise.resolve(
      scope === 'all'
        ? [row(1, 'A', 'RED', 4), row(2, 'B', 'GREEN'), row(3, 'C', 'GRAY')]
        : scope === 'led'
          ? []
          : [row(9, 'MINE', 'YELLOW', 1)],
    ),
  ),
}));

function renderPage() {
  return render(
    <MemoryRouter>
      <PortfolioPage />
    </MemoryRouter>,
  );
}

describe('PortfolioPage', () => {
  /** TC-PFP-01: default scope=mine renders rows + summary + RYG chip. */
  it('renders the mine-scope portfolio (TC-PFP-01)', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId('portfolio-row-9')).toBeInTheDocument());
    expect(screen.getByTestId('portfolio-ryg-9')).toHaveAttribute('data-tier', 'yellow');
    expect(screen.getByTestId('portfolio-row-9').querySelector('a')).toHaveAttribute(
      'href',
      '/pm/tasks?projectId=9',
    );
    expect(screen.getByTestId('portfolio-summary')).toBeInTheDocument();
  });

  /** TC-PFP-02: switching scope re-fetches; all-scope shows the company rows worst-first. */
  it('refetches on scope change (TC-PFP-02)', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId('portfolio-row-9')).toBeInTheDocument());
    fireEvent.change(screen.getByTestId('portfolio-scope'), { target: { value: 'all' } });
    await waitFor(() => expect(screen.getByTestId('portfolio-row-1')).toBeInTheDocument());
    expect(getPortfolio).toHaveBeenCalledWith('all');
    expect(screen.getByTestId('portfolio-ryg-1')).toHaveAttribute('data-tier', 'red');
    // worst-first: RED row before GRAY row
    const html = screen.getByTestId('portfolio-list').innerHTML;
    expect(html.indexOf('portfolio-row-1')).toBeLessThan(html.indexOf('portfolio-row-3'));
  });

  /** TC-PFP-03: empty scope → friendly empty state. */
  it('shows an empty state when the scope has no projects (TC-PFP-03)', async () => {
    renderPage();
    fireEvent.change(screen.getByTestId('portfolio-scope'), { target: { value: 'led' } });
    await waitFor(() => expect(screen.getByTestId('portfolio-empty')).toBeInTheDocument());
  });
});
