import { beforeEach, describe, expect, it, vi } from 'vitest';
import client from './client';
import { getMyProfile, getUserProfile } from './profile';

vi.mock('./client', () => ({
  default: {
    get: vi.fn(),
  },
}));

const mockClient = client as unknown as { get: ReturnType<typeof vi.fn> };

describe('profile api', () => {
  beforeEach(() => {
    mockClient.get.mockReset();
  });

  it('loads the current profile from /me/profile (TC-UPROF-003)', async () => {
    mockClient.get.mockResolvedValueOnce({ data: { loginName: 'alice' } });

    await getMyProfile();

    expect(mockClient.get).toHaveBeenCalledWith('/me/profile');
  });

  it('loads a routed user profile from /users/{id}/profile (TC-UPROF-002)', async () => {
    mockClient.get.mockResolvedValueOnce({ data: { loginName: 'bob' } });

    await getUserProfile(42);

    expect(mockClient.get).toHaveBeenCalledWith('/users/42/profile');
  });
});
