import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { login } from '../../api/auth';
import { Button, Card, Input } from '../../components/ui';
import { useAuthStore } from '../../store/auth';
import './Login.css';

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const setAuth = useAuthStore((s) => s.setAuth);
  const navigate = useNavigate();

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await login(username, password);
      setAuth(res.token, res.user);
      navigate('/', { replace: true });
    } catch {
      setError('登录失败，请检查账号密码');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="rainier-login-wrap">
      <Card className="rainier-login-card">
        <h1 className="rainier-login-title">Rainier 登录</h1>
        <form onSubmit={handleSubmit} className="rainier-login-form">
          <label className="rainier-login-label">
            账号
            <Input
              placeholder="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              autoFocus
              data-testid="login-username"
            />
          </label>
          <label className="rainier-login-label">
            密码
            <Input
              type="password"
              placeholder="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              data-testid="login-password"
            />
          </label>
          {error && (
            <div role="alert" className="rainier-login-error">
              {error}
            </div>
          )}
          <Button type="submit" disabled={loading} data-testid="login-submit">
            {loading ? '登录中...' : '登录'}
          </Button>
        </form>
      </Card>
    </div>
  );
}
