import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import type { PublicStatus } from '../api/types';
import { apiFetch } from '../api/client';
import { useAuth } from '../context/AuthContext';

function navClass(isActive: boolean): string {
  return `nav-link${isActive ? ' nav-link--active' : ''}`;
}

export function Layout() {
  const { user, loading, logout } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const isTeacher = user?.role === 'TEACHER';
  const canCreateQuiz = isAdmin || isTeacher;
  const [publicStatus, setPublicStatus] = useState<PublicStatus | null>(null);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const s = await apiFetch<PublicStatus>('/api/public/status');
        if (!cancelled) {
          setPublicStatus(s);
        }
      } catch {
        /* banner is optional */
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="app-shell">
      <header
        style={{
          borderBottom: '1px solid var(--surface2)',
          background: 'rgba(26, 34, 45, 0.88)',
          backdropFilter: 'blur(10px)',
          position: 'sticky',
          top: 0,
          zIndex: 10,
        }}
      >
        <div
          className="container"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '1rem',
            flexWrap: 'wrap',
            padding: '0.85rem 0',
          }}
        >
          <Link to="/" style={{ fontWeight: 800, fontSize: '1.1rem', color: 'var(--text)', letterSpacing: '-0.02em' }}>
            UDPT Quiz
          </Link>
          <nav style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', flexWrap: 'wrap' }}>
            <NavLink to="/" end className={({ isActive }) => navClass(isActive)}>
              Danh sách
            </NavLink>
            {canCreateQuiz && (
              <NavLink to="/admin/create" className={({ isActive }) => navClass(isActive)}>
                Tạo đề thi
              </NavLink>
            )}
            {canCreateQuiz && (
              <NavLink to="/admin/quizzes" className={({ isActive }) => navClass(isActive)}>
                Quản lý đề thi
              </NavLink>
            )}
            {isAdmin && (
              <NavLink to="/admin/users" className={({ isActive }) => navClass(isActive)}>
                Quản lý user
              </NavLink>
            )}
            {user && (
              <NavLink to="/profile" className={({ isActive }) => navClass(isActive)}>
                Hồ sơ
              </NavLink>
            )}
            {user && (
              <NavLink to="/me/results" className={({ isActive }) => navClass(isActive)}>
                Kết quả
              </NavLink>
            )}
            {loading ? (
              <span style={{ color: 'var(--muted)', fontSize: '0.9rem', paddingLeft: '0.35rem' }}>Đang tải phiên…</span>
            ) : user ? (
              <>
                <span
                  style={{
                    color: 'var(--muted)',
                    fontSize: '0.88rem',
                    paddingLeft: '0.5rem',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '0.35rem',
                    flexWrap: 'wrap',
                  }}
                >
                  <span style={{ color: 'var(--text)', fontWeight: 600 }}>{user.displayName}</span>
                  <span className="badge badge-role">{user.role}</span>
                </span>
                <button type="button" className="btn btn-ghost" onClick={logout}>
                  Đăng xuất
                </button>
              </>
            ) : (
              <>
                <NavLink to="/login" className={({ isActive }) => navClass(isActive)}>
                  Đăng nhập
                </NavLink>
                <NavLink to="/register" className={({ isActive }) => navClass(isActive)}>
                  Đăng ký
                </NavLink>
              </>
            )}
          </nav>
        </div>
      </header>
      {publicStatus?.maintenanceMode ? (
        <div
          className="container"
          style={{
            padding: '0.65rem 0 0',
            color: '#fec89a',
            fontSize: '0.88rem',
            textAlign: 'center',
          }}
        >
          Hệ thống đang bảo trì. Người dùng thường có thể gặp lỗi 503 khi gọi API; tài khoản ADMIN vẫn thao tác được.
        </div>
      ) : null}
      {publicStatus?.announcementMessage ? (
        <div
          className="container"
          style={{
            padding: '0.65rem 0 0',
            color: 'var(--accent)',
            fontSize: '0.9rem',
            textAlign: 'center',
            maxWidth: '960px',
            margin: '0 auto',
          }}
        >
          {publicStatus.announcementMessage}
        </div>
      ) : null}
      <main className="container" style={{ flex: 1, padding: '1.75rem 0 2rem' }}>
        <Outlet />
      </main>
      <footer className="container site-footer">
        Hệ thống thi/quiz phân tán — vai trò Học sinh / Giáo viên / Quản trị; Spring Boot, CockroachDB, Redis, Kafka,
        Nginx.
      </footer>
    </div>
  );
}
