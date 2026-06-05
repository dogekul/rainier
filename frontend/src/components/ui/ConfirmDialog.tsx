import './ConfirmDialog.css';

export interface ConfirmDialogProps {
  open: boolean;
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  onConfirm: () => void;
  onCancel: () => void;
}

/** Simple yes/no confirmation modal. */
export function ConfirmDialog({
  open,
  title,
  message,
  confirmText = '确认',
  cancelText = '取消',
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  if (!open) return null;
  return (
    <div className="rainier-confirm-overlay" data-testid="rainier-confirm-overlay">
      <div className="rainier-confirm-backdrop" onClick={onCancel} />
      <div className="rainier-confirm" role="dialog" data-testid="rainier-confirm">
        <div className="rainier-confirm-title">{title}</div>
        <div className="rainier-confirm-message">{message}</div>
        <div className="rainier-confirm-actions">
          <button type="button" className="rainier-confirm-btn" onClick={onCancel}>
            {cancelText}
          </button>
          <button
            type="button"
            className="rainier-confirm-btn rainier-confirm-btn-primary"
            onClick={onConfirm}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  );
}
