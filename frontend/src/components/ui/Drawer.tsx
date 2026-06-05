import { ReactNode, useEffect } from 'react';
import './Drawer.css';

export interface DrawerProps {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
  width?: number;
}

/** Right-anchored slide-in panel. Controlled by parent (open, onClose). */
export function Drawer({ open, title, onClose, children, width = 480 }: DrawerProps) {
  useEffect(() => {
    if (!open) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [open, onClose]);

  if (!open) return null;
  return (
    <div className="rainier-drawer-overlay" data-testid="rainier-drawer-overlay">
      <div className="rainier-drawer-backdrop" onClick={onClose} />
      <aside className="rainier-drawer" style={{ width }} data-testid="rainier-drawer">
        <header className="rainier-drawer-header">
          <span className="rainier-drawer-title">{title}</span>
          <button
            type="button"
            className="rainier-drawer-close"
            onClick={onClose}
            aria-label="关闭"
          >
            ×
          </button>
        </header>
        <div className="rainier-drawer-body">{children}</div>
      </aside>
    </div>
  );
}
