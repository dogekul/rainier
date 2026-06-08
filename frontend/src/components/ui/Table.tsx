import { ReactNode } from 'react';
import './Table.css';

export interface TableColumn<T> {
  key: string;
  title: string;
  render?: (row: T) => ReactNode;
  width?: string | number;
}

export interface TableProps<T> {
  columns: TableColumn<T>[];
  dataSource: T[];
  rowKey: keyof T | ((row: T) => string);
  emptyText?: string;
  /**
   * v0.0.9: optional expandable row support. When `isExpanded` returns true for a row, a second
   * `<tr>` is inserted immediately below it spanning all columns, containing the result of
   * `renderExpanded(row)`. Used by RequirementsPage drilldown for Story sub-tables.
   */
  isExpanded?: (row: T) => boolean;
  renderExpanded?: (row: T) => ReactNode;
}

/** Minimal Feishu-style table. Controlled, no internal state. */
export function Table<T>({
  columns,
  dataSource,
  rowKey,
  emptyText = '暂无数据',
  isExpanded,
  renderExpanded,
}: TableProps<T>) {
  const getKey = (row: T): string =>
    typeof rowKey === 'function' ? rowKey(row) : String(row[rowKey] ?? '');
  return (
    <table className="rainier-table" data-testid="rainier-table">
      <thead>
        <tr>
          {columns.map((c) => (
            <th key={c.key} style={c.width ? { width: c.width } : undefined}>
              {c.title}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {dataSource.length === 0 ? (
          <tr>
            <td className="rainier-table-empty" colSpan={columns.length}>
              {emptyText}
            </td>
          </tr>
        ) : (
          dataSource.flatMap((row) => {
            const key = getKey(row);
            const rows = [
              <tr key={key}>
                {columns.map((c) => (
                  <td key={c.key}>
                    {c.render
                      ? c.render(row)
                      : (row[c.key as keyof T] as ReactNode) ?? ''}
                  </td>
                ))}
              </tr>,
            ];
            if (isExpanded && renderExpanded && isExpanded(row)) {
              rows.push(
                <tr key={key + '-expanded'} className="rainier-table-expanded-row">
                  <td colSpan={columns.length}>{renderExpanded(row)}</td>
                </tr>,
              );
            }
            return rows;
          })
        )}
      </tbody>
    </table>
  );
}
