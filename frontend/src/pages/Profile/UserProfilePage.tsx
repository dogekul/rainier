import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getUserProfile, type UserProfile } from '../../api/profile';
import { ProfileView } from './ProfileView';

/** v0.0.118 — read-only profile page for an authorized subordinate / user. */
export function UserProfilePage() {
  const { id } = useParams<{ id: string }>();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);

  const userId = Number(id);
  const validUserId = Number.isInteger(userId) && userId > 0;

  useEffect(() => {
    if (!validUserId) {
      setProfile(null);
      setLoading(false);
      return;
    }

    let active = true;
    setLoading(true);
    void getUserProfile(userId)
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
  }, [userId, validUserId]);

  if (!validUserId) {
    return (
      <div className="rainier-page">
        <div className="rainier-page-head">
          <h2>成员档案</h2>
        </div>
        <p data-testid="profile-invalid-id" style={{ color: 'var(--rainier-color-text-2)' }}>
          无效的用户 ID。
        </p>
      </div>
    );
  }

  if (loading || !profile) {
    return (
      <div className="rainier-page">
        <div className="rainier-page-head">
          <h2>成员档案</h2>
        </div>
      </div>
    );
  }

  return <ProfileView title="成员档案" profile={profile} perspective="member" />;
}
