import { useCallback, useEffect, useState } from 'react';

export interface PaginatedResult<T> {
  content: T[];
  page: number;
  size: number;
  total: number;
}

export interface UsePaginatedOptions {
  initialPage?: number;
  initialSize?: number;
  initialSearch?: string;
}

export interface UsePaginatedReturn<T> {
  page: number;
  size: number;
  search: string;
  items: T[];
  total: number;
  loading: boolean;
  error: string | null;
  setPage: (page: number) => void;
  setSize: (size: number) => void;
  setSearch: (search: string) => void;
  refetch: () => Promise<void>;
}

/**
 * Generic list-state hook. Caller supplies a fetcher that returns the standard
 * {content, page, size, total} envelope; the hook owns search + pagination state and
 * re-fetches when any of those three change.
 */
export function usePaginated<T>(
  fetcher: (args: { page: number; size: number; search: string }) => Promise<PaginatedResult<T>>,
  options: UsePaginatedOptions = {},
): UsePaginatedReturn<T> {
  const [page, setPage] = useState(options.initialPage ?? 0);
  const [size, setSize] = useState(options.initialSize ?? 20);
  const [search, setSearch] = useState(options.initialSearch ?? '');
  const [items, setItems] = useState<T[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refetch = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetcher({ page, size, search });
      setItems(result.content);
      setTotal(result.total);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }, [fetcher, page, size, search]);

  useEffect(() => {
    void refetch();
    // We intentionally only depend on the primitive params so changes to fetcher identity
    // (a common React closure pattern) don't trigger infinite loops.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, size, search]);

  return {
    page,
    size,
    search,
    items,
    total,
    loading,
    error,
    setPage,
    setSize,
    setSearch,
    refetch,
  };
}
