import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getUserProfile, type UserProfile } from '../../api/profile';
import { UserProfilePage } from './UserProfilePage';

vi.mock('../../api/profile', async (orig) => ({
  ...(await orig<typeof import('../../api/profile')>()),
  getUserProfile: vi.fn(),
}));

function profile(over: Partial<UserProfile> = {}): UserProfile {
  return {
    userId: 42,
    loginName: 'bob',
    name: 'Bob',
    positionName: '前端工程师',
    positionCategory: 'TECH',
    memberships: [
      {
        organizationId: 7,
        organizationName: '采购小队',
        organizationType: 'TEAM',
        role: 'MEMBER',
        isPrimary: true,
      },
    ],
    manager: { userId: 1, name: 'Alice', loginName: 'alice' },
    ownedStoryCount: 2,
    assignedTaskCount: 4,
    capabilities: [],
    ...over,
  };
}

function renderPage(path = '/users/42/profile') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/users/:id/profile" element={<UserProfilePage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('UserProfilePage', () => {
  beforeEach(() => {
    vi.mocked(getUserProfile).mockReset();
  });

  /** TC-UPROF-002: path id drives GET /api/users/{id}/profile through api/profile.ts. */
  it('loads the routed user profile and renders it (TC-UPROF-002)', async () => {
    vi.mocked(getUserProfile).mockResolvedValue(profile());

    renderPage();

    await waitFor(() => expect(getUserProfile).toHaveBeenCalledWith(42));
    expect(screen.getByTestId('profile-identity')).toHaveTextContent('Bob');
    expect(screen.getByTestId('profile-position')).toHaveTextContent('前端工程师');
    expect(screen.getByTestId('profile-stats')).toHaveTextContent('负责的 Story');
    expect(screen.getByTestId('profile-stats')).toHaveTextContent('2');
    expect(screen.getByTestId('profile-stats')).toHaveTextContent('分配的任务');
    expect(screen.getByTestId('profile-stats')).toHaveTextContent('4');
    expect(screen.getByTestId('profile-manager')).toHaveTextContent('Alice');
  });

  /** TC-UPROF-002b: invalid ids are rejected client-side instead of calling a malformed endpoint. */
  it('does not call the profile API for an invalid route id', async () => {
    renderPage('/users/not-a-number/profile');

    await waitFor(() => expect(screen.getByTestId('profile-invalid-id')).toBeInTheDocument());
    expect(getUserProfile).not.toHaveBeenCalled();
  });
});
