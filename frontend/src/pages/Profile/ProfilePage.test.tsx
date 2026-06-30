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
    capabilities: [],
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

  /** I1-S1: capabilities embedded in the profile payload render as skill chips. */
  it('renders capability tags with level and source (I1-S1)', async () => {
    vi.mocked(getMyProfile).mockResolvedValue(
      profile({
        capabilities: [
          {
            capabilityTagId: 11,
            tagName: 'Java',
            tagCategory: 'TECH',
            level: 4,
            source: 'SELF',
          },
          {
            capabilityTagId: 12,
            tagName: '沟通',
            tagCategory: 'SOFT',
            level: 2,
            source: 'MANAGER',
          },
        ],
      }),
    );
    renderPage();

    await waitFor(() => expect(screen.getByTestId('profile-capabilities')).toBeInTheDocument());
    expect(screen.getByTestId('profile-capability-11')).toHaveTextContent('Java');
    expect(screen.getByTestId('profile-capability-11')).toHaveTextContent('技术');
    expect(screen.getByTestId('profile-capability-11')).toHaveTextContent('L4');
    expect(screen.getByTestId('profile-capability-12')).toHaveTextContent('主管');
  });

  /** I1-S2: no capabilities keeps the card visible with a clear empty state. */
  it('shows an empty capability state when no tags are present (I1-S2)', async () => {
    vi.mocked(getMyProfile).mockResolvedValue(profile({ capabilities: [] }));
    renderPage();

    await waitFor(() => expect(screen.getByTestId('profile-capabilities-empty')).toBeInTheDocument());
  });
});
