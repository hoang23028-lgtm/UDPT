import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [role, setRole] = useState<'STUDENT' | 'TEACHER'>('STUDENT');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await register(email.trim(), password, displayName.trim(), role);
      navigate('/');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Đăng ký thất bại');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="card auth-panel" style={{ maxWidth: '440px', marginInline: 'auto' }}>
      <h1 style={{ marginTop: 0 }}>Đăng ký</h1>
      <p className="auth-lead">
        Tạo tài khoản người chơi với tên hiển thị — tên này sẽ xuất hiện cạnh điểm số trên leaderboard.
      </p>
      {error && <div className="error-banner">{error}</div>}
      <form onSubmit={onSubmit}>
        <div className="field">
          <label htmlFor="role">Vai trò</label>
          <select id="role" value={role} onChange={(e) => setRole(e.target.value as 'STUDENT' | 'TEACHER')}>
            <option value="STUDENT">Student (Học sinh)</option>
            <option value="TEACHER">Teacher (Giáo viên)</option>
          </select>
        </div>
        <div className="field">
          <label htmlFor="displayName">Tên hiển thị</label>
          <input
            id="displayName"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            required
            maxLength={120}
          />
        </div>
        <div className="field">
          <label htmlFor="email">Email</label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>
        <div className="field">
          <label htmlFor="password">Mật khẩu (tối thiểu 8 ký tự)</label>
          <input
            id="password"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
            maxLength={100}
          />
        </div>
        <button type="submit" className="btn btn-primary" disabled={busy}>
          {busy ? 'Đang xử lý…' : 'Tạo tài khoản'}
        </button>
      </form>
      <p style={{ marginTop: '1.25rem', color: 'var(--muted)', fontSize: '0.9rem' }}>
        Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
      </p>
    </div>
  );
}
