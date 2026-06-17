import type { ReactNode } from 'react';
import { Card } from '../ui/Card';
import './board.css';

export interface DashboardCardProps {
  title: ReactNode;
  /** Optional right-aligned slot (a count, a link, a filter). */
  extra?: ReactNode;
  children?: ReactNode;
  testId?: string;
}

/**
 * v0.0.22 board-kit — a Card with the standard dashboard header (title + optional extra slot) so no
 * panel is a bare div and every card reads identically.
 */
export function DashboardCard({ title, extra, children, testId }: DashboardCardProps) {
  return (
    <Card data-testid={testId}>
      <div className="rainier-dashcard-head">
        <span className="rainier-dashcard-title">{title}</span>
        {extra != null && <span className="rainier-dashcard-extra">{extra}</span>}
      </div>
      {children}
    </Card>
  );
}
