import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ProfilePage } from './ProfilePage';
import { getMyProfile, type UserProfile } from '../../api/profile';

vi.mock('../../api/profile', async (orig) => ({
  ...(await orig<typeof import('../../api/profile')>()),
  getMyProfile: vi.fn(),
}));

function profile(over: Partial<UserProfile> = {}): UserProfile {
  return {
    userId: 1,
    loginName: 'alice',
    name: 'Alice',
    positionName: '后端工程师',
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
    manager: { userId: 2, name: 'Bob', loginName: 'bob' },
    ownedStoryCount: 3,
    assignedTaskCount: 5,
    ...over,
  };
}

function renderPage() {
  return render(
    <MemoryRouter>
      <ProfilePage />
    </MemoryRouter>,
  );
}

describe('ProfilePage', () => {
  beforeEach(() => {
    vi.mocked(getMyProfile).mockReset();
  });

  /** TC-PROFP-01: identity + contribution + manager. */
  it('renders identity, contribution and manager (TC-PROFP-01)', async () => {
    vi.mocked(getMyProfile).mockResolvedValue(profile());
    renderPage();
    await waitFor(() => expect(screen.getByTestId('profile-identity')).toBeInTheDocument());
    expect(screen.getByTestId('profile-position')).toHaveTextContent('后端工程师');
    expect(screen.getByTestId('profile-stats')).toHaveTextContent('3');
    expect(screen.getByTestId('profile-stats')).toHaveTextContent('5');
    expect(screen.getByTestId('profile-manager')).toHaveTextContent('Bob');
  });

  /** TC-PROFP-02: org membership list. */
  it('renders the org membership list (TC-PROFP-02)', async () => {
    vi.mocked(getMyProfile).mockResolvedValue(profile());
    renderPage();
    await waitFor(() => expect(screen.getByTestId('profile-org-7')).toBeInTheDocument());
    expect(screen.getByTestId('profile-org-7')).toHaveTextContent('采购小队');
    expect(screen.getByTestId('profile-role-7')).toHaveTextContent('成员');
  });

  /** TC-PROFP-03: no orgs → empty state. */
  it('shows an empty state when there are no org memberships (TC-PROFP-03)', async () => {
    vi.mocked(getMyProfile).mockResolvedValue(profile({ memberships: [], manager: null }));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('profile-orgs-empty')).toBeInTheDocument());
    expect(screen.getByTestId('profile-manager-none')).toBeInTheDocument();
  });
});
