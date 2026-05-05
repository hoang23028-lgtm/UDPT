import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Navigate } from 'react-router-dom';
import type { User } from '../api/types';
import { apiFetch } from '../api/client';
import { useAuth } from '../context/AuthContext';

type NewUserForm = {
  email: string;
  password: string;
  displayName: string;
  role: 'STUDENT' | 'TEACHER' | 'ADMIN';
};

export function AdminUsers() {
  const { user } = useAuth();
  const [items, setItems] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [form, setForm] = useState<NewUserForm>({
    email: '',
    password: '',
    displayName: '',
    role: 'STUDENT',
  });

  const isAdmin = user?.role === 'ADMIN';

  async function refresh() {
    setLoading(true);
    setError(null);
    try {
      const xs = await apiFetch<User[]>('/api/admin/users');
      setItems(xs);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không tải được danh sách user');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (!user || !isAdmin) {
      setLoading(false);
      return;
    }
    void refresh();
  }, [user, isAdmin]);

  const rows = useMemo(() => items.slice(), [items]);

  async function createUser(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setCreating(true);
    try {
      const body = JSON.stringify({
        email: form.email.trim(),
        password: form.password,
        displayName: form.displayName.trim(),
        role: form.role,
      });
      const created = await apiFetch<User>('/api/admin/users', { method: 'POST', body });
      setItems((xs) => [created, ...xs]);
      setForm({ email: '', password: '', displayName: '', role: 'STUDENT' });
    } catch (e2) {
      setError(e2 instanceof Error ? e2.message : 'Tạo user thất bại');
    } finally {
      setCreating(false);
    }
  }

  async function patchUser(id: string, patch: { accountLocked?: boolean; role?: string }) {
    setBusyId(id);
    setError(null);
    try {
      const updated = await apiFetch<User>(`/api/admin/users/${id}`, { method: 'PATCH', body: JSON.stringify(patch) });
      setItems((xs) => xs.map((x) => (x.id === id ? updated : x)));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Cập nhật thất bại');
    } finally {
      setBusyId(null);
    }
  }

  async function resetPassword(id: string) {
    const pw = prompt('Nhập mật khẩu mới (>= 8 ký tự):');
    if (!pw) return;
    setBusyId(id);
    setError(null);
    try {
      await apiFetch<void>(`/api/admin/users/${id}/reset-password`, {
        method: 'POST',
        body: JSON.stringify({ newPassword: pw }),
      });
      alert('Đã reset mật khẩu.');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Reset mật khẩu thất bại');
    } finally {
      setBusyId(null);
    }
  }

  async function deleteUser(id: string) {
    if (!confirm('Xóa tài khoản này?')) return;
    setBusyId(id);
    setError(null);
    try {
      await apiFetch<void>(`/api/admin/users/${id}`, { method: 'DELETE' });
      setItems((xs) => xs.filter((x) => x.id !== id));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Xóa thất bại');
    } finally {
      setBusyId(null);
    }
  }

  if (!user) return <Navigate to="/login" replace />;
  if (!isAdmin) return <Navigate to="/" replace />;

  return (
    <div>
      <div className="toolbar" style={{ justifyContent: 'space-between' }}>
        <h1 style={{ margin: 0 }}>Quản lý tài khoản</h1>
        <button type="button" className="btn btn-ghost" onClick={() => void refresh()} disabled={loading}>
          Tải lại
        </button>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <section className="card" style={{ marginBottom: '1rem' }}>
        <h2 style={{ marginTop: 0, fontSize: '1.05rem' }}>Thêm tài khoản</h2>
        <form onSubmit={createUser} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
          <div className="field" style={{ margin: 0 }}>
            <label>Email</label>
            <input
              value={form.email}
              onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
              required
              type="email"
              autoComplete="off"
            />
          </div>
          <div className="field" style={{ margin: 0 }}>
            <label>Mật khẩu</label>
            <input
              value={form.password}
              onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))}
              required
              type="password"
              minLength={8}
              autoComplete="new-password"
            />
          </div>
          <div className="field" style={{ margin: 0 }}>
            <label>Tên hiển thị</label>
            <input
              value={form.displayName}
              onChange={(e) => setForm((f) => ({ ...f, displayName: e.target.value }))}
              required
              maxLength={120}
            />
          </div>
          <div className="field" style={{ margin: 0 }}>
            <label>Role</label>
            <select value={form.role} onChange={(e) => setForm((f) => ({ ...f, role: e.target.value as NewUserForm['role'] }))}>
              <option value="STUDENT">STUDENT</option>
              <option value="TEACHER">TEACHER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </div>
          <div style={{ gridColumn: '1 / -1', display: 'flex', justifyContent: 'flex-end' }}>
            <button type="submit" className="btn btn-primary" disabled={creating}>
              {creating ? 'Đang tạo…' : 'Tạo tài khoản'}
            </button>
          </div>
        </form>
      </section>

      {loading ? (
        <div className="card">Đang tải…</div>
      ) : (
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Email</th>
                <th>Tên</th>
                <th>Role</th>
                <th>Khóa</th>
                <th style={{ width: '1%' }}>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((u) => {
                const isSelf = u.id === user.id;
                const disabled = busyId === u.id;
                return (
                  <tr key={u.id}>
                    <td className="mono">{u.id}</td>
                    <td>{u.email}</td>
                    <td>{u.displayName}</td>
                    <td>
                      <select
                        value={u.role}
                        disabled={disabled}
                        onChange={(e) => void patchUser(u.id, { role: e.target.value })}
                      >
                        <option value="STUDENT">STUDENT</option>
                        <option value="TEACHER">TEACHER</option>
                        <option value="ADMIN">ADMIN</option>
                      </select>
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      <input
                        type="checkbox"
                        checked={u.accountLocked}
                        disabled={disabled}
                        onChange={(e) => void patchUser(u.id, { accountLocked: e.target.checked })}
                      />
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                        <button type="button" className="btn btn-ghost" disabled={disabled} onClick={() => void resetPassword(u.id)}>
                          Reset PW
                        </button>
                        <button
                          type="button"
                          className="btn btn-danger"
                          disabled={disabled || isSelf}
                          onClick={() => void deleteUser(u.id)}
                          title={isSelf ? 'Không thể xóa chính mình' : 'Xóa user'}
                        >
                          Xóa
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

