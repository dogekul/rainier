import { statusColor } from '../../utils/board';
import './board.css';

export interface StatusChipProps {
  status: string;
  /** Display label (defaults to the raw status). */
  label?: string;
  testId?: string;
}

/** v0.0.22 board-kit — a small tinted chip colored by the status' tier. */
export function StatusChip({ status, label, testId }: StatusChipProps) {
  const tier = statusColor(status);
  return (
    <span
      className="rainier-status-chip"
      data-testid={testId ?? 'status-chip'}
      data-tier={tier}
      style={{
        color: `var(--rainier-status-${tier})`,
        background: `var(--rainier-status-${tier}-bg)`,
      }}
    >
      {label ?? status}
    </span>
  );
}
