import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import type { QuizSummary } from '../api/types';
import { apiFetch } from '../api/client';
import { QuizGridSkeleton } from '../components/Skeleton';

export function Home() {
  const [quizzes, setQuizzes] = useState<QuizSummary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const data = await apiFetch<QuizSummary[]>('/api/quizzes');
        if (!cancelled) {
          setQuizzes(data);
        }
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : 'Lỗi tải danh sách');
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
  }, []);

  return (
    <div>
      <section className="page-hero">
        <h1>Làm bài thi, xem kết quả, bảng xếp hạng</h1>
        <p>
          Học sinh xem các đề được gán (lớp / nhóm / công khai), làm trắc nghiệm hoặc tự luận, nộp bài nhiều lần trong
          giới hạn giáo viên cho phép. Giáo viên và quản trị tạo đề từ mục «Tạo đề thi».
        </p>
      </section>

      <div className="page-title-row">
        <h1>Danh sách đề / bài thi</h1>
        {!loading && !error && (
          <span style={{ color: 'var(--muted)', fontSize: '0.9rem' }}>{quizzes.length} đề</span>
        )}
      </div>

      {loading && <QuizGridSkeleton />}

      {!loading && error && <div className="error-banner">{error}</div>}

      {!loading && !error && quizzes.length === 0 && (
        <div className="card" style={{ maxWidth: '560px' }}>
          <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Chưa có quiz</h2>
          <p style={{ color: 'var(--muted)', marginBottom: 0 }}>
            Đăng nhập bằng tài khoản <strong style={{ color: 'var(--text)' }}>TEACHER</strong> hoặc{' '}
            <strong style={{ color: 'var(--text)' }}>ADMIN</strong>, dùng «Tạo đề thi» để thêm bộ câu hỏi.
          </p>
        </div>
      )}

      {!loading && !error && quizzes.length > 0 && (
        <div className="quiz-grid">
          {quizzes.map((q) => (
            <Link key={q.id} to={`/quiz/${q.id}`} className="card quiz-card quiz-card-link">
              <h2 style={{ margin: '0 0 0.35rem', fontSize: '1.08rem', lineHeight: 1.35 }}>{q.title}</h2>
              <div className="quiz-card-meta">
                <span className={q.published ? 'badge badge-live' : 'badge badge-draft'}>
                  {q.published ? 'Đã xuất bản' : 'Bản nháp'}
                </span>
                {q.groupRestricted || q.classOrDirectRestricted ? (
                  <span className="badge" style={{ background: 'var(--surface2)', color: 'var(--muted)' }}>
                    Gán lớp/Nhóm
                  </span>
                ) : null}
                <span style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>
                  {q.questionCount} câu hỏi
                </span>
              </div>
              {q.description ? <p className="quiz-card-desc">{q.description}</p> : null}
              <p style={{ margin: '0.85rem 0 0', fontSize: '0.82rem', color: 'var(--accent)' }}>Mở quiz →</p>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
