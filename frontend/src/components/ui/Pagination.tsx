import './Pagination.css';

export interface PaginationProps {
  page: number;
  size: number;
  total: number;
  onPageChange: (page: number) => void;
}

/** Minimal paginator: prev/next with current page indicator. */
export function Pagination({ page, size, total, onPageChange }: PaginationProps) {
  const totalPages = Math.max(1, Math.ceil(total / size));
  return (
    <div className="rainier-pagination" data-testid="rainier-pagination">
      <button
        type="button"
        className="rainier-pagination-btn"
        disabled={page === 0}
        onClick={() => onPageChange(page - 1)}
      >
        上一页
      </button>
      <span className="rainier-pagination-current">
        {page + 1} / {totalPages}
      </span>
      <button
        type="button"
        className="rainier-pagination-btn"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
      >
        下一页
      </button>
      <span className="rainier-pagination-total">共 {total} 条</span>
    </div>
  );
}
