import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import type { LeaderboardEntry, LeaderboardPeriod } from '../api/types';
import { apiFetch } from '../api/client';
import { Skeleton } from '../components/Skeleton';

function rankClass(rank: number): string {
  if (rank === 1) {
    return 'rank-medal-1';
  }
  if (rank === 2) {
    return 'rank-medal-2';
  }
  if (rank === 3) {
    return 'rank-medal-3';
  }
  return '';
}

export function Leaderboard() {
  const { id } = useParams<{ id: string }>();
  const [period, setPeriod] = useState<LeaderboardPeriod>('ALL');
  const [rows, setRows] = useState<LeaderboardEntry[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [fetchedAt, setFetchedAt] = useState<number | null>(null);

  const load = useCallback(async () => {
    if (!id) {
      return;
    }
    const q = period === 'ALL' ? '' : `?period=${encodeURIComponent(period)}`;
    const data = await apiFetch<LeaderboardEntry[]>(`/api/quizzes/${id}/leaderboard${q}`);
    setRows(data);
    setFetchedAt(Date.now());
  }, [id, period]);

  useEffect(() => {
    if (!id) {
      return;
    }
    let cancelled = false;
    void (async () => {
      try {
        setError(null);
        await load();
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : 'Lỗi tải bảng xếp hạng');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id, load, period]);

  async function onRefresh() {
    if (!id) {
      return;
    }
    setError(null);
    setRefreshing(true);
    try {
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Lỗi tải bảng xếp hạng');
    } finally {
      setRefreshing(false);
    }
  }

  if (loading) {
    return (
      <div aria-busy="true" aria-label="Đang tải bảng xếp hạng">
        <Skeleton style={{ height: '2rem', width: '40%', marginBottom: '1rem' }} />
        <div className="card" style={{ padding: '1rem' }}>
          <Skeleton style={{ height: '12rem', width: '100%' }} />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div>
        <div className="toolbar">
          <Link to={`/quiz/${id}`} className="link-muted">
            ← Về quiz
          </Link>
        </div>
        <div className="error-banner">{error}</div>
      </div>
    );
  }

  const timeLabel =
    fetchedAt != null
      ? new Intl.DateTimeFormat('vi-VN', { timeStyle: 'medium', dateStyle: 'short' }).format(new Date(fetchedAt))
      : null;

  return (
    <div>
      <div className="toolbar">
        <Link to={`/quiz/${id}`} className="link-muted">
          ← Về quiz
        </Link>
        <button type="button" className="btn btn-ghost" disabled={refreshing} onClick={() => void onRefresh()}>
          {refreshing ? 'Đang làm mới…' : 'Làm mới'}
        </button>
      </div>

      <div className="page-title-row">
        <h1>Bảng xếp hạng</h1>
        {timeLabel && (
          <span style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>Dữ liệu lúc {timeLabel}</span>
        )}
      </div>

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', marginBottom: '1rem' }}>
        {(['WEEK', 'MONTH', 'ALL'] as const).map((p) => (
          <button
            key={p}
            type="button"
            className={p === period ? 'btn btn-primary' : 'btn btn-ghost'}
            onClick={() => {
              setPeriod(p);
              setLoading(true);
            }}
          >
            {p === 'WEEK' ? 'Tuần này (UTC)' : p === 'MONTH' ? 'Tháng này (UTC)' : 'Toàn thời gian'}
          </button>
        ))}
      </div>

      <p style={{ color: 'var(--muted)', margin: '0 0 1.25rem', maxWidth: '60ch', fontSize: '0.92rem' }}>
        Xếp hạng theo điểm tổng (đúng + thưởng tốc độ nếu có), lọc theo thời điểm nộp bài. Cache Redis theo từng quiz và
        khoảng thời gian — dùng «Làm mới» sau khi vừa nộp bài.
      </p>

      {rows.length === 0 ? (
        <div className="card">
          <p style={{ margin: 0 }}>Chưa có lượt chơi nào được ghi nhận cho quiz này.</p>
        </div>
      ) : (
        <div className="card lb-table-wrap">
          <table className="lb-table">
            <thead>
              <tr>
                <th style={{ width: '4rem' }}>#</th>
                <th>Người chơi</th>
                <th>Đúng / tổng</th>
                <th style={{ width: '5.5rem' }}>Thưởng</th>
                <th style={{ width: '5.5rem' }}>Tổng điểm</th>
                <th style={{ width: '5rem' }}>%</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={`${r.rank}-${r.userId}`}>
                  <td className={`rank-cell ${rankClass(r.rank)}`}>{r.rank}</td>
                  <td>{r.displayName}</td>
                  <td>
                    {r.score} / {r.maxScore}
                  </td>
                  <td className="pct-cell">{r.timeBonus.toFixed(2)}</td>
                  <td className="pct-cell">{r.rankScore.toFixed(2)}</td>
                  <td className="pct-cell">{r.percentage.toFixed(1)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
