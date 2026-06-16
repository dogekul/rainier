import { describe, expect, it } from 'vitest';
import { navGroups } from './AppLayout';
import { isAdminPath } from './ProtectedRoute';

/**
 * v0.0.20 (review PA-1): the admin route guard (`isAdminPath`) and the Sider's `requiresAdmin` group
 * flags are two separate sources of truth. This test pins them together so they can't silently drift
 * into a guard gap — e.g. adding an admin nav item whose path the guard doesn't cover.
 */
describe('nav ↔ admin-route-guard consistency', () => {
  for (const group of navGroups) {
    for (const item of group.items) {
      it(`${item.to} guarded === requiresAdmin(${group.key})`, () => {
        expect(isAdminPath(item.to)).toBe(!!group.requiresAdmin);
      });
    }
  }
});
