import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Table } from './Table';

interface Row {
  id: string;
  name: string;
}

describe('Table', () => {
  it('renders columns header and data rows (TC-FES-202)', () => {
    const rows: Row[] = [
      { id: '1', name: 'A' },
      { id: '2', name: 'B' },
    ];
    render(
      <Table<Row>
        columns={[
          { key: 'name', title: '名称', render: (r) => r.name },
          { key: 'id', title: 'ID', render: (r) => r.id },
        ]}
        dataSource={rows}
        rowKey="id"
      />,
    );
    expect(screen.getByTestId('rainier-table')).toBeInTheDocument();
    expect(screen.getByText('名称')).toBeInTheDocument();
    expect(screen.getByText('ID')).toBeInTheDocument();
    expect(screen.getAllByRole('row')).toHaveLength(3); // 1 header + 2 body
    expect(screen.getByText('A')).toBeInTheDocument();
    expect(screen.getByText('B')).toBeInTheDocument();
  });

  it('renders empty placeholder when no data', () => {
    render(
      <Table<Row>
        columns={[{ key: 'name', title: '名称' }]}
        dataSource={[]}
        rowKey="id"
      />,
    );
    expect(screen.getByText('暂无数据')).toBeInTheDocument();
  });
});
