import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { SprintsPage } from './SprintsPage';
import { useAuthStore } from '../../store/auth';

vi.mock('../../api/sprint', async () => {
  const actual = await vi.importActual<typeof import('../../api/sprint')>('../../api/sprint');
  return {
    ...actual,
    listSprints: vi.fn().mockResolvedValue({
      content: [
        {
          id: 10,
          code: 'SPR-A',
          name: 'Phase 1',
          status: 'ACTIVE',
          requirementId: 1,
          requirementCode: 'REQ-1',
          requirementTitle: '登录流程',
          ownerUserId: 1,
          ownerName: 'Alice',
          ownerLoginName: 'alice',
          storyCount: 2,
        },
      ],
      total: 1,
      page: 0,
      size: 20,
    }),
  };
});

describe('SprintsPage', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'tk', user: { username: 'alice' } });
  });

  /** TC-FES-SPR-07 (v0.0.10 → v0.0.61): 列表渲染 + 行点击跳转 /pm/sprints/:id（替代之前的 row 展开 StoryListPanel）。 */
  it('renders sprints list and row click navigates to /pm/sprints/:id (TC-FES-SPR-07)', async () => {
    render(
      <MemoryRouter initialEntries={['/pm/sprints']}>
        <Routes>
          <Route path="/pm/sprints" element={<SprintsPage />} />
          <Route path="/pm/sprints/:id" element={<div data-testid="sprint-detail-stub">detail</div>} />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('SPR-A')).toBeInTheDocument();
    });
    expect(screen.getByText('Phase 1')).toBeInTheDocument();
    expect(screen.getByTestId('sprint-row-10')).toBeInTheDocument();
    fireEvent.click(screen.getByTestId('sprint-row-10'));
    await waitFor(() => {
      expect(screen.getByTestId('sprint-detail-stub')).toBeInTheDocument();
    });
  });
});
