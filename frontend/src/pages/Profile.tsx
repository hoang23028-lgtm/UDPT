import { FormEvent, useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { apiFetch } from '../api/client';
import type { User } from '../api/types';

export function Profile() {
  const { user, loading, refreshMe } = useAuth();
  const [displayName, setDisplayName] = useState('');
  const [phone, setPhone] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');
  const [currentPw, setCurrentPw] = useState('');
  const [newPw, setNewPw] = useState('');
  const [msg, setMsg] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!user) {
      return;
    }
    setDisplayName(user.displayName);
    setPhone(user.phone ?? '');
    setAvatarUrl(user.avatarUrl ?? '');
  }, [user]);

  if (!loading && !user) {
    return <Navigate to="/login" replace />;
  }

  async function saveProfile(e: FormEvent) {
    e.preventDefault();
    setErr(null);
    setMsg(null);
    setBusy(true);
    try {
      await apiFetch<User>('/api/users/me', {
        method: 'PATCH',
        body: JSON.stringify({
          displayName: displayName.trim() || undefined,
          phone: phone.trim() || null,
          avatarUrl: avatarUrl.trim() || null,
        }),
      });
      await refreshMe();
      setMsg('Đã lưu hồ sơ.');
    } catch (e) {
      setErr(e instanceof Error ? e.message : 'Lỗi');
    } finally {
      setBusy(false);
    }
  }

  async function savePassword(e: FormEvent) {
    e.preventDefault();
    setErr(null);
    setMsg(null);
    setBusy(true);
    try {
      await apiFetch('/api/users/me/password', {
        method: 'POST',
        body: JSON.stringify({ currentPassword: currentPw, newPassword: newPw }),
      });
      setCurrentPw('');
      setNewPw('');
      setMsg('Đã đổi mật khẩu.');
    } catch (e) {
      setErr(e instanceof Error ? e.message : 'Lỗi đổi mật khẩu');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>Tài khoản</h1>
      <p style={{ color: 'var(--muted)', maxWidth: '60ch' }}>
        Vai trò: <strong>{user?.role}</strong> — học sinh cập nhật thông tin cơ bản; giáo viên và quản trị có thêm quyền
        trên API.
      </p>
      {err && <div className="error-banner">{err}</div>}
      {msg && <div className="success-banner" style={{ marginBottom: '1rem' }}>{msg}</div>}

      <form onSubmit={saveProfile} className="card" style={{ marginBottom: '1.25rem' }}>
        <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Thông tin cá nhân</h2>
        <div className="field">
          <label htmlFor="dn">Tên hiển thị</label>
          <input id="dn" value={displayName} onChange={(e) => setDisplayName(e.target.value)} maxLength={120} />
        </div>
        <div className="field">
          <label htmlFor="ph">Số điện thoại</label>
          <input id="ph" value={phone} onChange={(e) => setPhone(e.target.value)} maxLength={32} />
        </div>
        <div className="field">
          <label htmlFor="av">URL ảnh đại diện</label>
          <input id="av" value={avatarUrl} onChange={(e) => setAvatarUrl(e.target.value)} maxLength={500} />
        </div>
        <button type="submit" className="btn btn-primary" disabled={busy}>
          Lưu hồ sơ
        </button>
      </form>

      <form onSubmit={savePassword} className="card">
        <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Đổi mật khẩu</h2>
        <div className="field">
          <label htmlFor="cp">Mật khẩu hiện tại</label>
          <input id="cp" type="password" autoComplete="current-password" value={currentPw} onChange={(e) => setCurrentPw(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="np">Mật khẩu mới (tối thiểu 8 ký tự)</label>
          <input id="np" type="password" autoComplete="new-password" value={newPw} onChange={(e) => setNewPw(e.target.value)} />
        </div>
        <button type="submit" className="btn btn-primary" disabled={busy || newPw.length < 8}>
          Đổi mật khẩu
        </button>
      </form>
    </div>
  );
}
