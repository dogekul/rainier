import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { DashboardCard } from './DashboardCard';
import { EmptyState } from './EmptyState';
import { OwnerChip } from './OwnerChip';
import { StatusBar } from './StatusBar';
import { StatusChip } from './StatusChip';

describe('board-kit components', () => {
  /** TC-BK-C01: StatusBar renders one fill per non-zero segment, width = count/total, tier color. */
  it('renders proportional distribution fills (TC-BK-C01)', () => {
    render(
      <StatusBar
        segments={[
          { label: 'TODO', count: 3, status: 'TODO' },
          { label: 'DONE', count: 1, status: 'DONE' },
          { label: 'X', count: 0, status: 'BLOCKED' },
        ]}
      />,
    );
    const todo = screen.getByTestId('statusbar-fill-TODO');
    const done = screen.getByTestId('statusbar-fill-DONE');
    expect(todo).toHaveStyle({ width: '75%' });
    expect(done).toHaveStyle({ width: '25%' });
    expect(todo).toHaveAttribute('data-tier', 'yellow');
    expect(done).toHaveAttribute('data-tier', 'green');
    // zero-count segment is omitted
    expect(screen.queryByTestId('statusbar-fill-X')).toBeNull();
    // legend present
    expect(screen.getByTestId('statusbar-legend-TODO')).toHaveTextContent('TODO 3');
  });

  /** TC-BK-C02: StatusBar with max uses count/max (ratio mode) + explicit tier. */
  it('renders a ratio bar with explicit tier (TC-BK-C02)', () => {
    render(
      <StatusBar
        segments={[{ label: '负载', count: 5, tier: 'yellow' }]}
        max={10}
        testId="load"
      />,
    );
    expect(screen.getByTestId('load-fill-负载')).toHaveStyle({ width: '50%' });
    expect(screen.getByTestId('load-fill-负载')).toHaveAttribute('data-tier', 'yellow');
  });

  /** TC-BK-C03: empty distribution shows a placeholder, not a zero bar. */
  it('shows an empty placeholder when total is 0 (TC-BK-C03)', () => {
    render(<StatusBar segments={[{ label: 'A', count: 0, status: 'TODO' }]} />);
    expect(screen.getByTestId('statusbar-empty')).toBeInTheDocument();
    expect(screen.queryByTestId('statusbar-fill-A')).toBeNull();
  });

  /** TC-BK-C04: DashboardCard shows title + extra + children. */
  it('renders title, extra and children (TC-BK-C04)', () => {
    render(
      <DashboardCard title="任务" extra={<span>共 4</span>} testId="dc">
        <div>body</div>
      </DashboardCard>,
    );
    expect(screen.getByTestId('dc')).toHaveTextContent('任务');
    expect(screen.getByText('共 4')).toBeInTheDocument();
    expect(screen.getByText('body')).toBeInTheDocument();
  });

  /** TC-BK-C05: StatusChip carries the tier; OwnerChip shows the initial + name. */
  it('renders StatusChip tier and OwnerChip initial+name (TC-BK-C05)', () => {
    render(
      <>
        <StatusChip status="BLOCKED" label="阻塞" />
        <OwnerChip name="黎立" />
      </>,
    );
    expect(screen.getByTestId('status-chip')).toHaveAttribute('data-tier', 'red');
    expect(screen.getByTestId('status-chip')).toHaveTextContent('阻塞');
    expect(screen.getByTestId('owner-chip')).toHaveTextContent('黎');
    expect(screen.getByTestId('owner-chip')).toHaveTextContent('黎立');
  });

  /** TC-BK-C06: EmptyState fires its single CTA onClick. */
  it('fires the EmptyState CTA onClick (TC-BK-C06)', () => {
    const onClick = vi.fn();
    render(<EmptyState message="暂无数据" cta={{ label: '去创建', onClick }} />);
    expect(screen.getByText('暂无数据')).toBeInTheDocument();
    fireEvent.click(screen.getByTestId('empty-state-cta'));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  /** TC-BK-C07: EmptyState with `to` renders a Link to the route. */
  it('renders an EmptyState CTA link for a route (TC-BK-C07)', () => {
    render(
      <MemoryRouter>
        <EmptyState message="无项目" cta={{ label: '去创建项目', to: '/pm/projects' }} />
      </MemoryRouter>,
    );
    expect(screen.getByTestId('empty-state-cta')).toHaveAttribute('href', '/pm/projects');
  });
});
