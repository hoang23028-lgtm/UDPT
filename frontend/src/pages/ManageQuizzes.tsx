import { useEffect, useMemo, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import type { QuizSummary, QuizDetail } from '../api/types';
import { apiFetch } from '../api/client';
import { useAuth } from '../context/AuthContext';

type QuizRow = QuizSummary & { createdByUserId: string | null };

export function ManageQuizzes() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [items, setItems] = useState<QuizRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const canManage = user?.role === 'ADMIN' || user?.role === 'TEACHER';
  if (!user) return <Navigate to="/login" replace />;
  if (!canManage) return <Navigate to="/" replace />;

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      setLoading(true);
      setError(null);
      try {
        const qs = await apiFetch<QuizSummary[]>('/api/quizzes');
        const details = await Promise.all(
          qs.map(async (q) => {
            try {
              const d = await apiFetch<QuizDetail>(`/api/quizzes/${q.id}`);
              return { ...q, createdByUserId: d.createdByUserId };
            } catch {
              return { ...q, createdByUserId: null };
            }
          }),
        );
        if (!cancelled) {
          setItems(details);
        }
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Không tải được danh sách đề thi');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const rows = useMemo(() => {
    if (user.role === 'ADMIN') return items;
    return items.filter((q) => q.createdByUserId === user.id);
  }, [items, user.id, user.role]);

  async function deleteQuiz(id: string) {
    if (!confirm('Xóa đề thi này? (Chỉ xóa được khi chưa có bài nộp)')) return;
    setBusyId(id);
    setError(null);
    try {
      await apiFetch<void>(`/api/quizzes/${id}`, { method: 'DELETE' });
      setItems((xs) => xs.filter((x) => x.id !== id));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Xóa thất bại');
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div>
      <div className="toolbar" style={{ justifyContent: 'space-between' }}>
        <h1 style={{ margin: 0 }}>Quản lý đề thi</h1>
        <button type="button" className="btn btn-primary" onClick={() => navigate('/admin/create')}>
          + Tạo đề thi
        </button>
      </div>
      <p style={{ color: 'var(--muted)', marginTop: '0.25rem' }}>
        {user.role === 'ADMIN'
          ? 'ADMIN xem và quản lý tất cả đề thi.'
          : 'TEACHER chỉ thấy các đề thi mình tạo.'}
      </p>
      {error && <div className="error-banner">{error}</div>}
      {loading ? (
        <div className="card">Đang tải…</div>
      ) : rows.length === 0 ? (
        <div className="card">Chưa có đề thi nào.</div>
      ) : (
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Tiêu đề</th>
                <th>Trạng thái</th>
                <th style={{ width: '1%' }}>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((q) => (
                <tr key={q.id}>
                  <td className="mono">{q.id}</td>
                  <td>
                    <Link to={`/quiz/${q.id}`}>{q.title}</Link>
                    {q.description ? (
                      <div style={{ color: 'var(--muted)', fontSize: '0.9rem' }}>{q.description}</div>
                    ) : null}
                  </td>
                  <td>{q.published ? <span className="badge badge-live">LIVE</span> : <span className="badge badge-draft">DRAFT</span>}</td>
                  <td>
                    <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                      <button
                        type="button"
                        className="btn btn-ghost"
                        disabled={busyId === q.id}
                        onClick={() => navigate(`/admin/quizzes/${q.id}/edit`)}
                      >
                        Sửa
                      </button>
                      <button
                        type="button"
                        className="btn btn-danger"
                        disabled={busyId === q.id}
                        onClick={() => void deleteQuiz(q.id)}
                      >
                        Xóa
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

