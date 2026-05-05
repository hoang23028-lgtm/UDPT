import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';
import type { QuizDetail } from '../api/types';
import { apiFetch } from '../api/client';
import { useAuth } from '../context/AuthContext';

type QuestionDraft = {
  id: string;
  text: string;
  choicesLines: string;
  correctChoiceIndex: number;
};

function splitChoices(lines: string): string[] {
  return lines
    .split('\n')
    .map((s) => s.trim())
    .filter(Boolean);
}

const LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';

export function EditQuiz() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [quiz, setQuiz] = useState<QuizDetail | null>(null);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [published, setPublished] = useState(true);
  const [maxAttempts, setMaxAttempts] = useState(1);
  const [questions, setQuestions] = useState<QuestionDraft[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);

  const canManage = user?.role === 'ADMIN' || user?.role === 'TEACHER';

  useEffect(() => {
    if (!user || !canManage) {
      setLoading(false);
      return;
    }
    if (!id) return;
    let cancelled = false;
    void (async () => {
      setLoading(true);
      setError(null);
      try {
        const d = await apiFetch<QuizDetail>(`/api/quizzes/${id}`);
        if (user.role !== 'ADMIN' && d.createdByUserId !== user.id) {
          throw new Error('Bạn không có quyền sửa đề thi này.');
        }
        if (cancelled) return;
        setQuiz(d);
        setTitle(d.title);
        setDescription(d.description ?? '');
        setPublished(Boolean(d.published));
        setMaxAttempts(d.maxAttempts ?? 1);
        const qDrafts: QuestionDraft[] = d.questions
          .slice()
          .sort((a, b) => a.orderIndex - b.orderIndex)
          .map((q) => ({
            id: q.id,
            text: q.text,
            choicesLines: q.choices.join('\n'),
            correctChoiceIndex: q.correctChoiceIndex ?? 0,
          }));
        setQuestions(qDrafts);
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Không tải được đề thi');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id, user, canManage]);

  const show = useMemo(() => quiz, [quiz]);

  function addQuestion() {
    setQuestions((q) => [...q, { id: crypto.randomUUID(), text: '', choicesLines: 'A\nB', correctChoiceIndex: 0 }]);
  }

  function removeQuestion(i: number) {
    setQuestions((q) => q.filter((_, j) => j !== i));
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!id) return;
    setError(null);
    setBusy(true);
    try {
      const built = questions.map((q, orderIndex) => {
        const choices = splitChoices(q.choicesLines);
        if (choices.length < 2) {
          throw new Error(`Câu ${orderIndex + 1}: cần ít nhất 2 đáp án.`);
        }
        if (q.correctChoiceIndex < 0 || q.correctChoiceIndex >= choices.length) {
          throw new Error(`Câu ${orderIndex + 1}: chưa chọn đáp án đúng hợp lệ.`);
        }
        if (!q.text.trim()) {
          throw new Error(`Câu ${orderIndex + 1}: nội dung trống.`);
        }
        return {
          text: q.text.trim(),
          choices,
          correctChoiceIndex: q.correctChoiceIndex,
          orderIndex,
          questionType: 'MCQ',
          points: 1,
          explanation: null as string | null,
        };
      });

      const payload = {
        title: title.trim() || undefined,
        description: description.trim() || null,
        published,
        questions: built,
        studyGroupIds: null as number[] | null,
        timeLimitSeconds: null as number | null,
        timeBonusMax: null as number | null,
        assignedClassIds: null as number[] | null,
        assignedStudentUserIds: null as number[] | null,
        opensAt: null as string | null,
        closesAt: null as string | null,
        maxAttempts,
        shuffleQuestions: false,
        shuffleOptions: false,
        examPassword: null as string | null,
        blockCopyPaste: false,
        maxFullscreenExits: null as number | null,
        showAnswersToStudents: false,
      };

      await apiFetch<QuizDetail>(`/api/quizzes/${id}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
      });
      navigate(`/quiz/${id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Cập nhật thất bại');
    } finally {
      setBusy(false);
    }
  }

  async function onDelete() {
    if (!id) return;
    if (!confirm('Xóa đề thi này? (Chỉ xóa được khi chưa có bài nộp)')) return;
    setBusy(true);
    setError(null);
    try {
      await apiFetch<void>(`/api/quizzes/${id}`, { method: 'DELETE' });
      navigate('/');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Xóa thất bại');
    } finally {
      setBusy(false);
    }
  }

  if (!user) return <Navigate to="/login" replace />;
  if (!canManage) return <Navigate to="/" replace />;

  if (loading) return <div className="card">Đang tải…</div>;
  if (error && !show) return <div className="error-banner">{error}</div>;
  if (!show) return null;

  return (
    <div>
      <div className="toolbar" style={{ justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
          <Link to={`/quiz/${show.id}`} className="link-muted">
            ← Quay lại
          </Link>
          <h1 style={{ margin: 0 }}>Sửa đề thi</h1>
        </div>
        <button type="button" className="btn btn-danger" disabled={busy} onClick={() => void onDelete()}>
          Xóa đề thi
        </button>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <form onSubmit={onSubmit} className="card">
        <div className="field">
          <label htmlFor="title">Tiêu đề</label>
          <input id="title" value={title} onChange={(e) => setTitle(e.target.value)} required maxLength={200} />
        </div>
        <div className="field">
          <label htmlFor="desc">Mô tả</label>
          <textarea id="desc" value={description} onChange={(e) => setDescription(e.target.value)} maxLength={2000} />
        </div>
        <div className="field">
          <label style={{ flexDirection: 'row', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}>
            <input type="checkbox" checked={published} onChange={(e) => setPublished(e.target.checked)} />
            Xuất bản
          </label>
        </div>
        <div className="field">
          <label htmlFor="maxAttempts">Số lần làm tối đa</label>
          <input
            id="maxAttempts"
            type="number"
            min={1}
            max={50}
            value={maxAttempts}
            onChange={(e) => setMaxAttempts(Number(e.target.value || 1))}
          />
        </div>

        <h2 style={{ fontSize: '1.1rem', marginTop: '1.5rem' }}>Câu hỏi</h2>
        {questions.map((q, i) => {
          const choiceLabels = splitChoices(q.choicesLines);
          return (
            <div key={q.id} className="question-editor-card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.65rem' }}>
                <strong>Câu {i + 1}</strong>
                {questions.length > 1 && (
                  <button type="button" className="btn btn-ghost" onClick={() => removeQuestion(i)}>
                    Xóa câu
                  </button>
                )}
              </div>
              <div className="field">
                <label>Nội dung câu hỏi</label>
                <textarea
                  value={q.text}
                  onChange={(e) => {
                    const v = e.target.value;
                    setQuestions((qs) => qs.map((x, j) => (j === i ? { ...x, text: v } : x)));
                  }}
                  required
                  maxLength={2000}
                />
              </div>
              <div className="field">
                <label>Đáp án — mỗi dòng một lựa chọn</label>
                <textarea
                  value={q.choicesLines}
                  onChange={(e) => {
                    const v = e.target.value;
                    setQuestions((qs) =>
                      qs.map((x, j) => {
                        if (j !== i) return x;
                        const choices = splitChoices(v);
                        const maxIdx = Math.max(0, choices.length - 1);
                        const ci = choices.length === 0 ? 0 : Math.min(Math.max(0, x.correctChoiceIndex), maxIdx);
                        return { ...x, choicesLines: v, correctChoiceIndex: ci };
                      }),
                    );
                  }}
                  required
                />
              </div>
              <div className="field" style={{ marginBottom: 0 }}>
                <span>Đáp án đúng</span>
                {choiceLabels.length < 2 ? (
                  <p style={{ margin: '0.35rem 0 0', color: 'var(--muted)', fontSize: '0.88rem' }}>
                    Nhập ít nhất hai dòng để chọn đáp án.
                  </p>
                ) : (
                  <div className="correct-pick-list" role="radiogroup" aria-label={`Đáp án đúng câu ${i + 1}`}>
                    {choiceLabels.map((label, choiceIdx) => {
                      const letter = LETTERS[choiceIdx] ?? String(choiceIdx + 1);
                      return (
                        <label key={choiceIdx} className="correct-pick-row">
                          <input
                            type="radio"
                            name={`correct-${i}`}
                            checked={q.correctChoiceIndex === choiceIdx}
                            onChange={() =>
                              setQuestions((qs) => qs.map((x, j) => (j === i ? { ...x, correctChoiceIndex: choiceIdx } : x)))
                            }
                          />
                          <span className="mono" style={{ minWidth: '1.25rem' }}>
                            {letter}.
                          </span>
                          <span>{label}</span>
                        </label>
                      );
                    })}
                  </div>
                )}
              </div>
            </div>
          );
        })}

        <button type="button" className="btn btn-ghost" onClick={addQuestion} style={{ marginBottom: '1rem' }}>
          + Thêm câu hỏi
        </button>

        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button type="submit" className="btn btn-primary" disabled={busy}>
            {busy ? 'Đang lưu…' : 'Lưu thay đổi'}
          </button>
          <Link to={`/quiz/${show.id}`} className="btn btn-ghost">
            Hủy
          </Link>
        </div>
      </form>
    </div>
  );
}

