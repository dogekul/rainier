import { useEffect } from 'react';
import { me } from '../../api/auth';
import { Card } from '../../components/ui';
import { useAuthStore } from '../../store/auth';

export default function Home() {
  const token = useAuthStore((s) => s.token);
  const user = useAuthStore((s) => s.user);
  const setAuth = useAuthStore((s) => s.setAuth);

  // Hydrate `user` on a cold reload (we persist only the token).
  useEffect(() => {
    if (token && !user) {
      me()
        .then((res) => setAuth(token, { username: res.username }))
        .catch(() => {
          /* 401 path already handled by the axios response interceptor */
        });
    }
  }, [token, user, setAuth]);

  return (
    <Card>
      <h2 data-testid="home-greeting">Hello, {user?.username ?? '...'}</h2>
      <p>欢迎使用 Rainier 项目管理系统（v0 占位）。</p>
    </Card>
  );
}
