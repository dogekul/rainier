import { render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { OrganizationsPage } from './OrganizationsPage';

// Mock the organization API so the page mounts without network.
vi.mock('../../api/organization', async () => {
  const actual = await vi.importActual<typeof import('../../api/organization')>(
    '../../api/organization',
  );
  return {
    ...actual,
    listOrganizations: vi.fn().mockResolvedValue({
      content: [
        {
          id: 1,
          parentId: null,
          type: 'COMPANY',
          code: 'HQ',
          name: '总公司',
          path: '/1',
          wholeName: '总公司',
          enabled: true,
        },
      ],
      total: 1,
      page: 0,
      size: 20,
    }),
  };
});

describe('OrganizationsPage', () => {
  it('does NOT render a "PMO" column header (TC-RMP-FE-002)', async () => {
    render(
      <MemoryRouter>
        <OrganizationsPage />
      </MemoryRouter>,
    );

    // Wait for the list fetch to populate the table (row count = header + 1 body row).
    await waitFor(() => {
      expect(screen.getAllByRole('row')).toHaveLength(2);
    });

    const headers = screen
      .getAllByRole('columnheader')
      .map((h) => (h.textContent ?? '').trim());

    // PMO column header must be absent.
    expect(headers).not.toContain('PMO');

    // Surviving headers (sanity baseline).
    expect(headers).toEqual(
      expect.arrayContaining(['编码', '名称', '类型', '全路径', '操作']),
    );

    // Defensive: row body for the seeded org also must not include "PMO" cell text.
    const tableEl = screen.getByTestId('rainier-table');
    expect(within(tableEl).queryByText('PMO')).toBeNull();
  });
});
