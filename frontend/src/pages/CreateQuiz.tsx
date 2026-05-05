import { FormEvent, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { apiFetch } from '../api/client';
import type { QuizDetail } from '../api/types';

type QuestionDraft = {
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

export function CreateQuiz() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [title, setTitle] = useState('Quiz mới');
  const [description, setDescription] = useState('');
  const [published, setPublished] = useState(true);
  const [questions, setQuestions] = useState<QuestionDraft[]>([
    { text: '2 + 2 = ?', choicesLines: '3\n4\n5', correctChoiceIndex: 1 },
  ]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (user.role !== 'ADMIN' && user.role !== 'TEACHER') {
    return (
      <div className="card">
        <p>Chỉ tài khoản Giáo viên hoặc Quản trị mới tạo được đề thi. Liên hệ quản trị để được cấp quyền TEACHER.</p>
      </div>
    );
  }

  function addQuestion() {
    setQuestions((q) => [...q, { text: '', choicesLines: 'A\nB', correctChoiceIndex: 0 }]);
  }

  function removeQuestion(i: number) {
    setQuestions((q) => q.filter((_, j) => j !== i));
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const built = questions.map((q, orderIndex) => {
        const choices = splitChoices(q.choicesLines);
        if (choices.length < 2) {
          throw new Error(`Câu ${orderIndex + 1}: cần ít nhất 2 đáp án (mỗi dòng một đáp án).`);
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
        title: title.trim(),
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
        maxAttempts: 1,
        shuffleQuestions: false,
        shuffleOptions: false,
        examPassword: null as string | null,
        blockCopyPaste: false,
        maxFullscreenExits: null as number | null,
        showAnswersToStudents: false,
      };
      const created = await apiFetch<QuizDetail>('/api/quizzes', {
        method: 'POST',
        body: JSON.stringify(payload),
      });
      navigate(`/quiz/${created.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Tạo quiz thất bại');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>Tạo quiz</h1>
      <p className="admin-hint">
        Mỗi câu: nhập đề bài, liệt kê đáp án (một dòng một lựa chọn), rồi chọn đúng một đáp án đúng bằng danh sách radio.
        Quiz xuất bản sẽ hiển thị trên trang chủ cho người chơi.
      </p>
      {error && <div className="error-banner">{error}</div>}
      <form onSubmit={onSubmit} className="card">
        <div className="field">
          <label htmlFor="title">Tiêu đề</label>
          <input id="title" value={title} onChange={(e) => setTitle(e.target.value)} required maxLength={200} />
        </div>
        <div className="field">
          <label htmlFor="desc">Mô tả (tùy chọn)</label>
          <textarea id="desc" value={description} onChange={(e) => setDescription(e.target.value)} maxLength={2000} />
        </div>
        <div className="field">
          <label style={{ flexDirection: 'row', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}>
            <input type="checkbox" checked={published} onChange={(e) => setPublished(e.target.checked)} />
            Xuất bản ngay sau khi tạo
          </label>
        </div>

        <h2 style={{ fontSize: '1.1rem', marginTop: '1.5rem' }}>Câu hỏi</h2>
        {questions.map((q, i) => {
          const choiceLabels = splitChoices(q.choicesLines);
          return (
            <div key={i} className="question-editor-card">
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
                <label>Đáp án — mỗi dòng một lựa chọn (tối thiểu 2 dòng)</label>
                <textarea
                  value={q.choicesLines}
                  onChange={(e) => {
                    const v = e.target.value;
                    setQuestions((qs) =>
                      qs.map((x, j) => {
                        if (j !== i) {
                          return x;
                        }
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
                    Nhập ít nhất hai dòng ở ô phía trên để chọn đáp án đúng.
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
                              setQuestions((qs) =>
                                qs.map((x, j) => (j === i ? { ...x, correctChoiceIndex: choiceIdx } : x)),
                              )
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

        <div>
          <button type="submit" className="btn btn-primary" disabled={busy}>
            {busy ? 'Đang tạo…' : 'Tạo quiz'}
          </button>
        </div>
      </form>
    </div>
  );
}
