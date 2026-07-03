import { useEffect, useState } from 'react';
import { getMyProfile, type UserProfile } from '../../api/profile';
import { ProfileView } from './ProfileView';

/**
 * v0.0.40 —「我的档案」(my profile). Consumes GET /api/me/profile: the current user's org identity +
 * 岗位 + 直接上级 + contribution counts. All-users, read-only — the team-member growth landing the
 * audit (#9) found missing, and the base for team/subgroup/domain leads' people views.
 */
export function ProfilePage() {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    void getMyProfile()
      .then((p) => {
        if (active) {
          setProfile(p);
          setLoading(false);
        }
      })
      .catch(() => {
        if (active) {
          setProfile(null);
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, []);

  if (loading || !profile) {
    return (
      <div className="rainier-page">
        <div className="rainier-page-head">
          <h2>我的档案</h2>
        </div>
      </div>
    );
  }

  return <ProfileView title="我的档案" profile={profile} />;
}
