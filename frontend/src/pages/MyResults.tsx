import { useEffect, useMemo, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import type { MySubmissionSummary } from '../api/types';
import { apiFetch } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { formatDateTime } from '../lib/format';

export function MyResults() {
  const { user } = useAuth();
  const [items, setItems] = useState<MySubmissionSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    void (async () => {
      setLoading(true);
      setError(null);
      try {
        const xs = await apiFetch<MySubmissionSummary[]>('/api/users/me/submissions');
        if (!cancelled) setItems(xs);
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Không tải được kết quả');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [user]);

  const rows = useMemo(() => items, [items]);

  if (!user) return <Navigate to="/login" replace />;

  return (
    <div>
      <div className="toolbar" style={{ justifyContent: 'space-between' }}>
        <h1 style={{ margin: 0 }}>Kết quả của tôi</h1>
        <Link to="/" className="btn btn-ghost">
          Về danh sách
        </Link>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {loading ? (
        <div className="card">Đang tải…</div>
      ) : rows.length === 0 ? (
        <div className="card">Bạn chưa làm bài nào.</div>
      ) : (
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <table className="table">
            <thead>
              <tr>
                <th>Đề thi</th>
                <th>Lần</th>
                <th>Điểm</th>
                <th>%</th>
                <th>Nộp lúc</th>
                <th style={{ width: '1%' }}>Xem</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.submissionId}>
                  <td>
                    <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
                      <Link to={`/quiz/${r.quizId}`}>{r.quizTitle}</Link>
                      {r.quizPublished ? (
                        <span className="badge badge-live">LIVE</span>
                      ) : (
                        <span className="badge badge-draft">DRAFT</span>
                      )}
                    </div>
                    <div className="mono" style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>
                      submission {r.submissionId}
                    </div>
                  </td>
                  <td className="mono">{r.attemptNumber}</td>
                  <td>
                    <strong>
                      {r.score} / {r.maxScore}
                    </strong>
                    {r.timeBonus > 0 ? (
                      <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>
                        thưởng +{r.timeBonus.toFixed(2)} (rank {r.rankScore.toFixed(2)})
                      </div>
                    ) : (
                      <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>rank {r.rankScore.toFixed(2)}</div>
                    )}
                  </td>
                  <td className="mono">{r.percentage.toFixed(2)}</td>
                  <td>{formatDateTime(r.submittedAt)}</td>
                  <td>
                    <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                      <Link to={`/quiz/${r.quizId}/leaderboard`} className="btn btn-ghost">
                        BXH
                      </Link>
                      <Link to={`/quiz/${r.quizId}`} className="btn btn-ghost">
                        Chi tiết
                      </Link>
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

