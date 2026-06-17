import { Link } from 'react-router-dom';
import { Button } from '../ui/Button';
import './board.css';

export interface EmptyStateProps {
  message: string;
  /** A single primary CTA — either an in-app route (`to`) or a click handler. Optional. */
  cta?: { label: string; to?: string; onClick?: () => void };
  testId?: string;
}

/**
 * v0.0.22 board-kit — a friendly empty state with at most ONE primary CTA, so a no-data board is never
 * a dead blank screen.
 */
export function EmptyState({ message, cta, testId }: EmptyStateProps) {
  return (
    <div className="rainier-empty-state" data-testid={testId ?? 'empty-state'}>
      <p className="rainier-empty-state-msg">{message}</p>
      {cta &&
        (cta.to ? (
          <Link to={cta.to} className="rainier-empty-state-cta" data-testid="empty-state-cta">
            {cta.label}
          </Link>
        ) : (
          <Button type="button" onClick={cta.onClick} data-testid="empty-state-cta">
            {cta.label}
          </Button>
        ))}
    </div>
  );
}
