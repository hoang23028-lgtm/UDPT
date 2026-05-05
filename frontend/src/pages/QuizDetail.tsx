import { FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import type { QuizDetail, ResultResponse } from '../api/types';
import { apiFetch } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { formatDateTime } from '../lib/format';
import { Skeleton } from '../components/Skeleton';

const LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';

function isEssay(q: { questionType?: string }): boolean {
  return (q.questionType ?? 'MCQ').toUpperCase() === 'ESSAY';
}

export function QuizDetail() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [quiz, setQuiz] = useState<QuizDetail | null>(null);
  const [answers, setAnswers] = useState<Record<string, number | string>>({});
  const [examPassword, setExamPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<ResultResponse | null>(null);
  const attemptStartedAtRef = useRef<string | null>(null);

  useEffect(() => {
    attemptStartedAtRef.current = null;
  }, [id]);

  useEffect(() => {
    if (!id) {
      return;
    }
    let cancelled = false;
    void (async () => {
      try {
        const data = await apiFetch<QuizDetail>(`/api/quizzes/${id}`);
        if (!cancelled) {
          setQuiz(data);
          if (attemptStartedAtRef.current == null) {
            attemptStartedAtRef.current = new Date().toISOString();
          }
        }
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : 'Không tải được quiz');
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
  }, [id]);

  const orderedQuestions = useMemo(() => {
    if (!quiz) {
      return [];
    }
    return quiz.questions.slice().sort((a, b) => a.orderIndex - b.orderIndex);
  }, [quiz]);

  const answeredCount = useMemo(() => {
    return orderedQuestions.filter((q) => {
      const v = answers[q.id];
      if (v === undefined || v === null) {
        return false;
      }
      if (isEssay(q)) {
        return String(v).trim().length > 0;
      }
      return typeof v === 'number';
    }).length;
  }, [orderedQuestions, answers]);

  const progressPct =
    orderedQuestions.length === 0 ? 0 : Math.round((answeredCount / orderedQuestions.length) * 100);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!id || !user) {
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      const idem = crypto.randomUUID();
      const bodyObj: Record<string, unknown> = {
        answers,
        attemptStartedAt: attemptStartedAtRef.current,
      };
      if (quiz?.examPasswordRequired && examPassword.trim()) {
        bodyObj.examPassword = examPassword.trim();
      }
      const body = JSON.stringify(bodyObj);
      const res = await apiFetch<ResultResponse>(`/api/quizzes/${id}/submit`, {
        method: 'POST',
        headers: { 'Idempotency-Key': idem },
        body,
      });
      setResult(res);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Nộp bài thất bại');
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return (
      <div aria-busy="true" aria-label="Đang tải quiz">
        <Skeleton style={{ height: '2.5rem', width: '55%', maxWidth: '420px', marginBottom: '1rem' }} />
        <div className="card" style={{ padding: '1.25rem' }}>
          <Skeleton style={{ height: '1rem', width: '100%', marginBottom: '0.65rem' }} />
          <Skeleton style={{ height: '1rem', width: '88%' }} />
        </div>
        <div style={{ marginTop: '1rem' }} className="card">
          <Skeleton style={{ height: '5rem', width: '100%' }} />
        </div>
      </div>
    );
  }

  if (error && !quiz) {
    return (
      <div>
        <div className="toolbar">
          <Link to="/" className="link-muted">
            ← Về danh sách
          </Link>
        </div>
        <div className="error-banner">{error}</div>
      </div>
    );
  }

  if (!quiz) {
    return null;
  }

  const canEdit = Boolean(
    user &&
      (user.role === 'ADMIN' || (user.role === 'TEACHER' && quiz.createdByUserId === user.id)),
  );

  async function deleteQuiz() {
    if (!id) return;
    if (!confirm('Xóa đề thi này? (Chỉ xóa được khi chưa có bài nộp)')) return;
    setError(null);
    setSubmitting(true);
    try {
      await apiFetch<void>(`/api/quizzes/${id}`, { method: 'DELETE' });
      navigate('/');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Xóa thất bại');
    } finally {
      setSubmitting(false);
    }
  }

  const canSubmit = Boolean(
    user &&
      orderedQuestions.length > 0 &&
      orderedQuestions.every((q) => {
        const v = answers[q.id];
        if (v === undefined || v === null) {
          return false;
        }
        if (isEssay(q)) {
          return String(v).trim().length > 0;
        }
        return typeof v === 'number';
      }) &&
      (!quiz.examPasswordRequired || examPassword.trim().length > 0),
  );

  return (
    <div>
      <div className="toolbar">
        <Link to="/" className="link-muted">
          ← Danh sách
        </Link>
        <Link to={`/quiz/${quiz.id}/leaderboard`} className="link-muted">
          Bảng xếp hạng →
        </Link>
        {canEdit ? (
          <div style={{ marginLeft: 'auto', display: 'flex', gap: '0.5rem' }}>
            <button type="button" className="btn btn-ghost" onClick={() => navigate(`/admin/quizzes/${quiz.id}/edit`)}>
              Sửa
            </button>
            <button type="button" className="btn btn-danger" disabled={submitting} onClick={() => void deleteQuiz()}>
              Xóa
            </button>
          </div>
        ) : null}
      </div>

      <section className="card" style={{ marginBottom: '1rem' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', alignItems: 'center', marginBottom: '0.65rem' }}>
          <span className={quiz.published ? 'badge badge-live' : 'badge badge-draft'}>
            {quiz.published ? 'Đang mở' : 'Bản nháp'}
          </span>
          <span style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>
            {orderedQuestions.length} câu · tối đa {quiz.maxAttempts} lần làm
            {quiz.opensAt || quiz.closesAt ? (
              <>
                {' '}
                · mở {quiz.opensAt ? formatDateTime(quiz.opensAt) : '—'} — đóng{' '}
                {quiz.closesAt ? formatDateTime(quiz.closesAt) : '—'}
              </>
            ) : null}
            {quiz.timeLimitSeconds != null && quiz.timeLimitSeconds > 0 ? (
              <>
                {' '}
                · giới hạn {quiz.timeLimitSeconds}s
                {quiz.timeBonusMax != null && quiz.timeBonusMax > 0
                  ? ` · thưởng tốc đa ${quiz.timeBonusMax}`
                  : ''}
              </>
            ) : null}
          </span>
        </div>
        <h1 style={{ margin: '0 0 0.5rem', fontSize: 'clamp(1.2rem, 2.2vw, 1.45rem)', lineHeight: 1.3 }}>
          {quiz.title}
        </h1>
        {quiz.description ? <p style={{ margin: 0, color: 'var(--muted)', maxWidth: '65ch' }}>{quiz.description}</p> : null}
        {quiz.blockCopyPaste ? (
          <p style={{ color: 'var(--muted)', fontSize: '0.85rem', margin: '0.75rem 0 0' }}>
            Giáo viên bật chế độ hạn chế sao chép/dán khi làm bài (thực thi chủ yếu phía client).
          </p>
        ) : null}
      </section>

      {result && (
        <div className="success-banner">
          <h2>Đã nộp bài thành công</h2>
          <p style={{ margin: 0, fontSize: '0.95rem' }}>Hệ thống đã ghi nhận lượt làm bài này.</p>
          <div className="result-grid">
            <div className="result-stat">
              <span>Điểm</span>
              <strong>
                {result.score} / {result.maxScore}
              </strong>
            </div>
            <div className="result-stat">
              <span>Tỷ lệ</span>
              <strong>{result.percentage.toFixed(1)}%</strong>
            </div>
            <div className="result-stat">
              <span>Thưởng tốc độ</span>
              <strong>{result.timeBonus.toFixed(2)}</strong>
            </div>
            <div className="result-stat">
              <span>Tổng xếp hạng</span>
              <strong>{result.rankScore.toFixed(2)}</strong>
            </div>
            <div className="result-stat">
              <span>Thời điểm chấm</span>
              <strong style={{ fontSize: '0.95rem', fontWeight: 700 }}>{formatDateTime(result.calculatedAt)}</strong>
            </div>
          </div>
          <p style={{ margin: '1rem 0 0', fontSize: '0.88rem' }}>
            <Link to={`/quiz/${quiz.id}/leaderboard`}>Xem bảng xếp hạng quiz này</Link>
          </p>
        </div>
      )}

      {error && (
        <div className="error-banner" style={{ marginTop: '1rem' }}>
          {error}
        </div>
      )}

      {!user ? (
        <div className="card" style={{ marginTop: '1.25rem' }}>
          <p style={{ margin: '0 0 0.75rem' }}>Đăng nhập để nộp bài và ghi điểm lên leaderboard.</p>
          <Link to="/login" className="btn btn-primary" style={{ display: 'inline-flex' }}>
            Đăng nhập
          </Link>
        </div>
      ) : (
        <form onSubmit={onSubmit} style={{ marginTop: '1.25rem' }}>
          {!result && orderedQuestions.length > 0 && (
            <div className="progress-wrap">
              <div className="progress-label">
                <span>Tiến độ trả lời</span>
                <span>
                  {answeredCount} / {orderedQuestions.length} câu ({progressPct}%)
                </span>
              </div>
              <div className="progress-track">
                <div className="progress-fill" style={{ width: `${progressPct}%` }} />
              </div>
            </div>
          )}

          {quiz.examPasswordRequired && !result && (
            <div className="card" style={{ marginBottom: '1rem' }}>
              <label className="field" style={{ display: 'block' }}>
                <span>Mật khẩu đề thi</span>
                <input
                  type="password"
                  value={examPassword}
                  onChange={(e) => setExamPassword(e.target.value)}
                  autoComplete="off"
                  placeholder="Nhập mật khẩu do giáo viên cung cấp"
                />
              </label>
            </div>
          )}

          {orderedQuestions.map((q, idx) => (
            <div key={q.id} className="card question-block">
              <div className="question-head">
                <div className="question-num">{idx + 1}</div>
                <p className="question-text">
                  {q.text}
                  <span style={{ color: 'var(--muted)', fontSize: '0.82rem', marginLeft: '0.35rem' }}>
                    ({isEssay(q) ? 'Tự luận' : 'Trắc nghiệm'} · {q.points}đ)
                  </span>
                </p>
              </div>
              {isEssay(q) ? (
                <textarea
                  className="field"
                  style={{ width: '100%', minHeight: '120px', marginTop: '0.5rem' }}
                  disabled={!!result}
                  value={typeof answers[q.id] === 'string' ? (answers[q.id] as string) : ''}
                  onChange={(e) => setAnswers((prev) => ({ ...prev, [q.id]: e.target.value }))}
                  placeholder="Nhập câu trả lời…"
                />
              ) : (
                <div className="option-list" role="group" aria-label={`Câu ${idx + 1}`}>
                  {q.choices.map((c, i) => {
                    const letter = LETTERS[i] ?? String(i + 1);
                    const selected = answers[q.id] === i;
                    return (
                      <label
                        key={i}
                        className={`option-row${selected ? ' option-row--selected' : ''}`}
                      >
                        <input
                          type="radio"
                          name={`q-${q.id}`}
                          checked={selected}
                          disabled={!!result}
                          onChange={() => setAnswers((prev) => ({ ...prev, [q.id]: i }))}
                        />
                        <span className="option-key">{letter}</span>
                        <span className="option-body">{c}</span>
                      </label>
                    );
                  })}
                </div>
              )}
              {result && q.explanation && (
                <p style={{ marginTop: '0.75rem', fontSize: '0.88rem', color: 'var(--accent)' }}>
                  <strong>Lời giải:</strong> {q.explanation}
                </p>
              )}
            </div>
          ))}

          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem', alignItems: 'center', marginTop: '0.25rem' }}>
            <button type="submit" className="btn btn-primary" disabled={!canSubmit || submitting || !!result}>
              {submitting ? 'Đang nộp…' : result ? 'Đã nộp bài' : 'Nộp bài'}
            </button>
            {!result && (
              <span style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>
                {canSubmit
                  ? 'Mỗi lần nộp tính một lượt làm (trong giới hạn cho phép).'
                  : 'Trả lời đủ các câu và nhập mật khẩu đề (nếu có) để nộp.'}
              </span>
            )}
          </div>
        </form>
      )}
    </div>
  );
}
