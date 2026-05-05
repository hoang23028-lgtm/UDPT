package com.quiz.service;

import com.quiz.dto.BankQuestionRequest;
import com.quiz.entity.BankQuestion;
import com.quiz.entity.DifficultyLevel;
import com.quiz.entity.QuestionType;
import com.quiz.entity.Subject;
import com.quiz.entity.User;
import com.quiz.exception.ApiException;
import com.quiz.repository.BankQuestionRepository;
import com.quiz.repository.SubjectRepository;
import com.quiz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BankQuestionService {

    private final BankQuestionRepository bankQuestionRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final QuizDtoMapper quizDtoMapper;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listForTeacher(Long teacherId) {
        return bankQuestionRepository.findByTeacher_IdOrderByIdDesc(teacherId).stream()
                .map(this::toRow)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> create(Long teacherId, BankQuestionRequest req) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Teacher not found"));
        QuestionType qt = parseQt(req.questionType());
        validate(req, qt);
        Subject subject = null;
        if (req.subjectId() != null) {
            subject = subjectRepository.findById(req.subjectId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown subject id"));
        }
        BankQuestion b = BankQuestion.builder()
                .teacher(teacher)
                .subject(subject)
                .chapter(req.chapter() == null ? null : req.chapter().trim())
                .difficulty(parseDiff(req.difficulty()))
                .questionType(qt)
                .stem(req.stem().trim())
                .choicesJson(quizDtoMapper.writeChoices(req.choices()))
                .correctChoiceIndex(qt == QuestionType.MCQ ? req.correctChoiceIndex().longValue() : null)
                .points(req.points())
                .explanation(req.explanation() == null ? null : req.explanation().trim())
                .build();
        b = bankQuestionRepository.save(b);
        return toRow(b);
    }

    @Transactional
    public void delete(Long teacherId, Long id, boolean admin) {
        BankQuestion b = bankQuestionRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bank question not found"));
        if (!admin && !b.getTeacher().getId().equals(teacherId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your bank question");
        }
        bankQuestionRepository.delete(b);
    }

    private Map<String, Object> toRow(BankQuestion b) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", b.getId());
        m.put("subjectId", b.getSubject() == null ? null : b.getSubject().getId());
        m.put("chapter", b.getChapter());
        m.put("difficulty", b.getDifficulty().name());
        m.put("questionType", b.getQuestionType().name());
        m.put("stem", b.getStem());
        m.put("choices", quizDtoMapper.readChoices(b.getChoicesJson()));
        m.put("correctChoiceIndex", b.getCorrectChoiceIndex());
        m.put("points", b.getPoints());
        m.put("explanation", b.getExplanation());
        return m;
    }

    private static QuestionType parseQt(String raw) {
        if (raw == null || raw.isBlank()) {
            return QuestionType.MCQ;
        }
        return QuestionType.valueOf(raw.trim().toUpperCase());
    }

    private static DifficultyLevel parseDiff(String raw) {
        if (raw == null || raw.isBlank()) {
            return DifficultyLevel.MEDIUM;
        }
        return DifficultyLevel.valueOf(raw.trim().toUpperCase());
    }

    private static void validate(BankQuestionRequest req, QuestionType qt) {
        if (qt == QuestionType.MCQ) {
            if (req.choices().size() < 2) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "MCQ needs at least 2 choices");
            }
            if (req.correctChoiceIndex() == null
                    || req.correctChoiceIndex() < 0
                    || req.correctChoiceIndex() >= req.choices().size()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid correctChoiceIndex");
            }
        } else if (req.correctChoiceIndex() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ESSAY must not set correctChoiceIndex");
        }
    }
}
