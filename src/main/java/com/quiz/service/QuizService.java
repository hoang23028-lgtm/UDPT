package com.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quiz.dto.QuestionRequest;
import com.quiz.dto.QuizCreateRequest;
import com.quiz.dto.QuizDetailResponse;
import com.quiz.dto.QuizSummaryResponse;
import com.quiz.dto.QuizExportPayload;
import com.quiz.dto.QuizUpdateRequest;
import com.quiz.dto.ResultResponse;
import com.quiz.dto.SubmitQuizRequest;
import com.quiz.entity.Question;
import com.quiz.entity.QuestionType;
import com.quiz.entity.Quiz;
import com.quiz.entity.Result;
import com.quiz.entity.StudyGroup;
import com.quiz.entity.Submission;
import com.quiz.entity.User;
import com.quiz.entity.UserRole;
import com.quiz.event.QuizSubmittedApplicationEvent;
import com.quiz.exception.ApiException;
import com.quiz.repository.QuizRepository;
import com.quiz.repository.SchoolClassRepository;
import com.quiz.repository.StudyGroupRepository;
import com.quiz.repository.SubmissionRepository;
import com.quiz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.CannotSerializeTransactionException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ObjectMapper objectMapper;
    private final QuizDtoMapper quizDtoMapper;
    private final LeaderboardService leaderboardService;
    private final QuizAccessService quizAccessService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<QuizSummaryResponse> listQuizzes(Long viewerUserId, boolean isAdmin, UserRole role) {
        if (isAdmin) {
            return quizRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                    .map(quizDtoMapper::toSummary)
                    .toList();
        }
        if (role == UserRole.TEACHER && viewerUserId != null) {
            Set<Long> ids = new LinkedHashSet<>();
            for (Quiz q : quizRepository.findByCreatedBy_IdOrderByIdAsc(viewerUserId)) {
                ids.add(q.getId());
            }
            for (Long id : quizRepository.findPublishedVisibleQuizIds(viewerUserId)) {
                ids.add(id);
            }
            if (ids.isEmpty()) {
                return List.of();
            }
            return quizRepository.findAllById(ids).stream()
                    .sorted(Comparator.comparing(Quiz::getId))
                    .map(quizDtoMapper::toSummary)
                    .toList();
        }
        List<Long> ids = quizRepository.findPublishedVisibleQuizIds(viewerUserId);
        if (ids.isEmpty()) {
            return List.of();
        }
        return quizRepository.findAllById(ids).stream()
                .sorted(Comparator.comparing(Quiz::getId))
                .map(quizDtoMapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizDetailResponse getQuiz(Long id, Long viewerUserId, boolean viewerIsAdmin) {
        Quiz quiz = quizRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Quiz not found"));
        boolean canSee = (quiz.isPublished() && quizAccessService.canViewPublished(quiz, viewerUserId))
                || quizAccessService.canViewDraftAsAuthorOrAdmin(quiz, viewerUserId, viewerIsAdmin);
        if (!canSee) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Quiz not found");
        }
        boolean reveal = shouldRevealSolutions(quiz, viewerUserId, viewerIsAdmin);
        return quizDtoMapper.toDetail(quiz, reveal);
    }

    @Transactional(readOnly = true)
    public boolean shouldRevealSolutions(Quiz quiz, Long viewerUserId, boolean viewerIsAdmin) {
        if (viewerIsAdmin) {
            return true;
        }
        if (viewerUserId != null && quiz.getCreatedBy() != null
                && quiz.getCreatedBy().getId().equals(viewerUserId)) {
            return true;
        }
        if (!quiz.isShowAnswersToStudents()) {
            return false;
        }
        return viewerUserId != null && submissionRepository.existsByUser_IdAndQuiz_Id(viewerUserId, quiz.getId());
    }

    @Transactional(readOnly = true)
    public QuizExportPayload exportQuizBackup(Long id) {
        Quiz quiz = quizRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Quiz not found"));
        List<QuestionRequest> qrs = new ArrayList<>();
        for (Question q : quiz.getQuestions()) {
            qrs.add(new QuestionRequest(
                    q.getText(),
                    quizDtoMapper.readChoices(q.getChoicesJson()),
                    q.getCorrectChoiceIndex() == null ? null : Math.toIntExact(q.getCorrectChoiceIndex()),
                    Math.toIntExact(q.getOrderIndex()),
                    q.getQuestionType().name(),
                    Math.toIntExact(q.getPoints()),
                    q.getExplanation()
            ));
        }
        List<Long> gids = quiz.getRestrictedToGroups().stream().map(StudyGroup::getId).sorted().toList();
        List<Long> cids = quiz.getAssignedClasses().stream().map(c -> c.getId()).sorted().toList();
        List<Long> sids = quiz.getAssignedStudents().stream().map(User::getId).sorted().toList();
        QuizCreateRequest snapshot = new QuizCreateRequest(
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.isPublished(),
                qrs,
                gids.isEmpty() ? null : gids,
                quiz.getTimeLimitSeconds(),
                quiz.getTimeBonusMax(),
                cids.isEmpty() ? null : cids,
                sids.isEmpty() ? null : sids,
                quiz.getOpensAt(),
                quiz.getClosesAt(),
                Math.toIntExact(quiz.getMaxAttempts()),
                quiz.isShuffleQuestions(),
                quiz.isShuffleOptions(),
                null,
                quiz.isBlockCopyPaste(),
                quiz.getMaxFullscreenExits() == null ? null : Math.toIntExact(quiz.getMaxFullscreenExits()),
                quiz.isShowAnswersToStudents()
        );
        return new QuizExportPayload(QuizExportPayload.CURRENT_FORMAT_VERSION, quiz.getId(), snapshot);
    }

    @Retryable(
            retryFor = {ConcurrencyFailureException.class, CannotSerializeTransactionException.class},
            maxAttempts = 12,
            backoff = @Backoff(delay = 25, multiplier = 2, maxDelay = 500, random = true)
    )
    @Transactional
    public QuizDetailResponse createQuiz(QuizCreateRequest request, Long creatorUserId) {
        validateQuestions(request.questions());
        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Creator user not found"));
        validateTimeBonusConfig(request.timeLimitSeconds(), request.timeBonusMax());
        Quiz quiz = Quiz.builder()
                .title(request.title().trim())
                .description(request.description() == null ? null : request.description().trim())
                .createdBy(creator)
                .published(request.published())
                .timeLimitSeconds(request.timeLimitSeconds())
                .timeBonusMax(request.timeBonusMax())
                .opensAt(request.opensAt())
                .closesAt(request.closesAt())
                .maxAttempts(request.maxAttempts() == null || request.maxAttempts() < 1 ? 1 : request.maxAttempts())
                .shuffleQuestions(Boolean.TRUE.equals(request.shuffleQuestions()))
                .shuffleOptions(Boolean.TRUE.equals(request.shuffleOptions()))
                .blockCopyPaste(Boolean.TRUE.equals(request.blockCopyPaste()))
                .maxFullscreenExits(request.maxFullscreenExits() == null ? null : request.maxFullscreenExits().longValue())
                .showAnswersToStudents(Boolean.TRUE.equals(request.showAnswersToStudents()))
                .build();
        if (request.examPassword() != null && !request.examPassword().isBlank()) {
            quiz.setExamPasswordHash(passwordEncoder.encode(request.examPassword().trim()));
        }
        applyStudyGroups(quiz, request.studyGroupIds());
        applyAssignments(quiz, request.assignedClassIds(), request.assignedStudentUserIds());
        for (QuestionRequest qr : request.questions()) {
            quiz.addQuestion(buildQuestion(qr));
        }
        quiz = quizRepository.save(quiz);
        return quizDtoMapper.toDetail(quizRepository.findByIdWithQuestions(quiz.getId()).orElseThrow(), true);
    }

    @Retryable(
            retryFor = {ConcurrencyFailureException.class, CannotSerializeTransactionException.class},
            maxAttempts = 12,
            backoff = @Backoff(delay = 25, multiplier = 2, maxDelay = 500, random = true)
    )
    @Transactional
    public QuizDetailResponse updateQuiz(Long id, QuizUpdateRequest request) {
        Quiz quiz = quizRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Quiz not found"));
        if (request.title() != null) {
            quiz.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            quiz.setDescription(request.description().trim());
        }
        if (request.published() != null) {
            quiz.setPublished(request.published());
        }
        if (request.timeLimitSeconds() != null) {
            quiz.setTimeLimitSeconds(request.timeLimitSeconds());
        }
        if (request.timeBonusMax() != null) {
            quiz.setTimeBonusMax(request.timeBonusMax());
        }
        if (request.opensAt() != null || request.closesAt() != null) {
            if (request.opensAt() != null) {
                quiz.setOpensAt(request.opensAt());
            }
            if (request.closesAt() != null) {
                quiz.setClosesAt(request.closesAt());
            }
        }
        if (request.maxAttempts() != null) {
            if (request.maxAttempts() < 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "maxAttempts must be at least 1");
            }
            quiz.setMaxAttempts(request.maxAttempts());
        }
        if (request.shuffleQuestions() != null) {
            quiz.setShuffleQuestions(request.shuffleQuestions());
        }
        if (request.shuffleOptions() != null) {
            quiz.setShuffleOptions(request.shuffleOptions());
        }
        if (request.blockCopyPaste() != null) {
            quiz.setBlockCopyPaste(request.blockCopyPaste());
        }
        if (request.maxFullscreenExits() != null) {
            quiz.setMaxFullscreenExits(request.maxFullscreenExits().longValue());
        }
        if (request.showAnswersToStudents() != null) {
            quiz.setShowAnswersToStudents(request.showAnswersToStudents());
        }
        if (request.examPassword() != null) {
            if (request.examPassword().isBlank()) {
                quiz.setExamPasswordHash(null);
            } else {
                quiz.setExamPasswordHash(passwordEncoder.encode(request.examPassword().trim()));
            }
        }
        validateTimeBonusConfig(quiz.getTimeLimitSeconds(), quiz.getTimeBonusMax());
        if (request.studyGroupIds() != null) {
            applyStudyGroups(quiz, request.studyGroupIds());
        }
        if (request.assignedClassIds() != null || request.assignedStudentUserIds() != null) {
            applyAssignments(quiz, request.assignedClassIds(), request.assignedStudentUserIds());
        }
        if (request.questions() != null) {
            if (request.questions().isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "questions, if provided, must not be empty");
            }
            validateQuestions(request.questions());
            quiz.clearQuestions();
            for (QuestionRequest qr : request.questions()) {
                quiz.addQuestion(buildQuestion(qr));
            }
        }
        if (quiz.isPublished() && quiz.getQuestions().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot publish a quiz with no questions");
        }
        quiz = quizRepository.save(quiz);
        return quizDtoMapper.toDetail(quizRepository.findByIdWithQuestions(quiz.getId()).orElseThrow(), true);
    }

    @Retryable(
            retryFor = {ConcurrencyFailureException.class, CannotSerializeTransactionException.class},
            maxAttempts = 12,
            backoff = @Backoff(delay = 25, multiplier = 2, maxDelay = 500, random = true)
    )
    @Transactional
    public void deleteQuiz(Long id) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Quiz not found"));
        if (submissionRepository.countByQuiz_Id(id) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot delete quiz with existing submissions");
        }
        quizRepository.delete(quiz);
    }

    @Retryable(
            retryFor = {
                    ConcurrencyFailureException.class,
                    CannotSerializeTransactionException.class,
                    DataIntegrityViolationException.class,
                    TransientDataAccessResourceException.class,
                    QueryTimeoutException.class
            },
            maxAttempts = 12,
            backoff = @Backoff(delay = 50, multiplier = 2, maxDelay = 1_000, random = true)
    )
    @Transactional
    public ResultResponse submitQuiz(Long quizId, Long userId, SubmitQuizRequest request, String resolvedIdempotencyKey) {
        if (resolvedIdempotencyKey != null) {
            Optional<Submission> prior = submissionRepository.findByUser_IdAndQuiz_IdAndIdempotencyKey(
                    userId, quizId, resolvedIdempotencyKey);
            if (prior.isPresent()) {
                return toResultResponse(prior.get());
            }
        }
        Quiz quiz = quizRepository.findByIdWithQuestions(quizId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Quiz not found"));
        if (!quiz.isPublished()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Quiz is not published");
        }
        if (!quizAccessService.canViewPublished(quiz, userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Quiz not found");
        }
        validateExamWindow(quiz);
        verifyExamPassword(quiz, request.examPassword());
        long maxAttemptUsed = submissionRepository.findMaxAttemptNumber(userId, quizId);
        if (maxAttemptUsed >= quiz.getMaxAttempts()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Maximum number of attempts (" + quiz.getMaxAttempts() + ") reached for this quiz");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "User not found"));
        if (user.isAccountLocked()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is locked");
        }
        Map<String, Object> normalizedAnswers = normalizeAnswersPayload(request.answers(), quiz);
        int maxPoints = (int) quiz.getQuestions().stream().mapToLong(Question::getPoints).sum();
        int mcqScorePoints = scoreMcqPortion(quiz, normalizedAnswers);
        String answersJson;
        try {
            answersJson = objectMapper.writeValueAsString(normalizedAnswers);
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid answers payload");
        }
        Instant submittedAt = Instant.now();
        double timeBonus = TimeBonusCalculator.computeTimeBonus(quiz, request.attemptStartedAt(), submittedAt, true);
        long attemptNumber = maxAttemptUsed + 1;
        double rankScore = TimeBonusCalculator.rankScore(mcqScorePoints, timeBonus);
        Submission submission = Submission.builder()
                .user(user)
                .quiz(quiz)
                .answersJson(answersJson)
                .idempotencyKey(resolvedIdempotencyKey)
                .submittedAt(submittedAt)
                .attemptStartedAt(request.attemptStartedAt())
                .attemptNumber(attemptNumber)
                .build();
        double percentage = maxPoints == 0 ? 0.0 : Math.round(10000.0 * mcqScorePoints / maxPoints) / 100.0;
        Result result = Result.builder()
                .submission(submission)
                .score(mcqScorePoints)
                .maxScore(maxPoints)
                .percentage(percentage)
                .timeBonus(timeBonus)
                .rankScore(rankScore)
                .essayPoints(0)
                .build();
        submission.setResult(result);
        submission = submissionRepository.saveAndFlush(submission);
        applicationEventPublisher.publishEvent(
                new QuizSubmittedApplicationEvent(submission.getId(), quizId, userId));
        leaderboardService.invalidateLeaderboard(quizId);
        Result persisted = submission.getResult();
        return new ResultResponse(
                persisted.getId(),
                submission.getId(),
                user.getId(),
                quiz.getId(),
                Math.toIntExact(persisted.getScore()),
                Math.toIntExact(persisted.getMaxScore()),
                persisted.getPercentage(),
                persisted.getCalculatedAt(),
                persisted.getTimeBonus(),
                persisted.getRankScore()
        );
    }

    @Transactional
    public ResultResponse gradeEssayPoints(Long submissionId, Long teacherUserId, boolean teacherIsAdmin, int essayPointsToAdd) {
        Submission submission = submissionRepository.findByIdWithResult(submissionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Submission not found"));
        Quiz quiz = submission.getQuiz();
        if (!teacherIsAdmin && (quiz.getCreatedBy() == null || !quiz.getCreatedBy().getId().equals(teacherUserId))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to grade this submission");
        }
        if (essayPointsToAdd < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "essayPointsToAdd must be non-negative");
        }
        Result result = submission.getResult();
        int mcqBaseline = (int) result.getScore() - (int) result.getEssayPoints();
        int newEssay = (int) (result.getEssayPoints() + essayPointsToAdd);
        int newScore = mcqBaseline + newEssay;
        if (newScore > result.getMaxScore()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Total score cannot exceed max score");
        }
        result.setEssayPoints(newEssay);
        result.setScore(newScore);
        double percentage = result.getMaxScore() == 0 ? 0.0
                : Math.round(10000.0 * newScore / result.getMaxScore()) / 100.0;
        result.setPercentage(percentage);
        result.setRankScore(TimeBonusCalculator.rankScore(newScore, result.getTimeBonus()));
        leaderboardService.invalidateLeaderboard(quiz.getId());
        return toResultResponse(submission);
    }

    private static void assertCompatibleIdempotency(Submission existing, String requestKey) {
        if (requestKey == null) {
            return;
        }
        String stored = existing.getIdempotencyKey();
        if (stored == null || !stored.equals(requestKey)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This quiz was already submitted with a different or missing idempotency key");
        }
    }

    private static ResultResponse toResultResponse(Submission submission) {
        Result result = submission.getResult();
        return new ResultResponse(
                result.getId(),
                submission.getId(),
                submission.getUser().getId(),
                submission.getQuiz().getId(),
                Math.toIntExact(result.getScore()),
                Math.toIntExact(result.getMaxScore()),
                result.getPercentage(),
                result.getCalculatedAt(),
                result.getTimeBonus(),
                result.getRankScore()
        );
    }

    private void applyStudyGroups(Quiz quiz, List<Long> groupIds) {
        quiz.getRestrictedToGroups().clear();
        if (groupIds == null || groupIds.isEmpty()) {
            return;
        }
        List<StudyGroup> found = studyGroupRepository.findAllById(groupIds);
        if (found.size() != groupIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "One or more study group ids are unknown");
        }
        quiz.getRestrictedToGroups().addAll(found);
    }

    private void applyAssignments(Quiz quiz, List<Long> classIds, List<Long> studentUserIds) {
        if (classIds != null) {
            quiz.getAssignedClasses().clear();
            if (!classIds.isEmpty()) {
                var classes = schoolClassRepository.findAllById(classIds);
                if (classes.size() != classIds.size()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "One or more class ids are unknown");
                }
                quiz.getAssignedClasses().addAll(classes);
            }
        }
        if (studentUserIds != null) {
            quiz.getAssignedStudents().clear();
            if (!studentUserIds.isEmpty()) {
                List<User> students = userRepository.findAllById(studentUserIds);
                if (students.size() != studentUserIds.size()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "One or more student user ids are unknown");
                }
                quiz.getAssignedStudents().addAll(students);
            }
        }
    }

    private Question buildQuestion(QuestionRequest qr) {
        QuestionType qt = parseQuestionType(qr.questionType());
        validateQuestionRequest(qr, qt);
        int points = qr.points() == null ? 1 : qr.points();
        String expl = qr.explanation() == null ? null : qr.explanation().trim();
        Long correct = null;
        if (qt == QuestionType.MCQ) {
            correct = qr.correctChoiceIndex().longValue();
        }
        return Question.builder()
                .text(qr.text().trim())
                .choicesJson(quizDtoMapper.writeChoices(qr.choices()))
                .correctChoiceIndex(correct)
                .orderIndex(qr.orderIndex())
                .questionType(qt)
                .points(points)
                .explanation(expl)
                .build();
    }

    private static QuestionType parseQuestionType(String raw) {
        if (raw == null || raw.isBlank()) {
            return QuestionType.MCQ;
        }
        try {
            return QuestionType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid questionType: " + raw);
        }
    }

    private static void validateQuestionRequest(QuestionRequest qr, QuestionType qt) {
        if (qt == QuestionType.MCQ) {
            if (qr.choices().size() < 2) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "MCQ requires at least 2 choices");
            }
            if (qr.correctChoiceIndex() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "MCQ requires correctChoiceIndex");
            }
            if (qr.correctChoiceIndex() < 0 || qr.correctChoiceIndex() >= qr.choices().size()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "correctChoiceIndex out of range for a question");
            }
        } else {
            if (qr.correctChoiceIndex() != null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ESSAY must not set correctChoiceIndex");
            }
        }
    }

    private static void validateTimeBonusConfig(Long timeLimitSeconds, Double timeBonusMax) {
        if (timeBonusMax != null && timeBonusMax > 0) {
            if (timeLimitSeconds == null || timeLimitSeconds <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "timeLimitSeconds is required and must be positive when timeBonusMax is set");
            }
        }
    }

    private static void validateQuestions(List<QuestionRequest> questions) {
        for (QuestionRequest qr : questions) {
            validateQuestionRequest(qr, parseQuestionType(qr.questionType()));
        }
    }

    private void validateExamWindow(Quiz quiz) {
        Instant now = Instant.now();
        if (quiz.getOpensAt() != null && now.isBefore(quiz.getOpensAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Quiz is not open yet");
        }
        if (quiz.getClosesAt() != null && now.isAfter(quiz.getClosesAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Quiz submission period has ended");
        }
    }

    private void verifyExamPassword(Quiz quiz, String plain) {
        String hash = quiz.getExamPasswordHash();
        if (hash == null || hash.isBlank()) {
            return;
        }
        if (plain == null || !passwordEncoder.matches(plain, hash)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid exam password");
        }
    }

    private Map<String, Object> normalizeAnswersPayload(JsonNode answers, Quiz quiz) {
        if (!answers.isObject()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "answers must be a JSON object");
        }
        Map<String, Object> out = new HashMap<>();
        Set<Long> validIds = new HashSet<>();
        for (Question q : quiz.getQuestions()) {
            validIds.add(q.getId());
        }
        var iter = answers.fields();
        while (iter.hasNext()) {
            var e = iter.next();
            long qid;
            try {
                qid = Long.parseLong(e.getKey().trim());
            } catch (NumberFormatException ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid question id key: " + e.getKey());
            }
            if (!validIds.contains(qid)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown question id: " + qid);
            }
            Question q = quiz.getQuestions().stream().filter(x -> x.getId().equals(qid)).findFirst().orElseThrow();
            JsonNode v = e.getValue();
            if (v == null || v.isNull()) {
                continue;
            }
            if (q.getQuestionType() == QuestionType.MCQ) {
                if (!v.isIntegralNumber()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "MCQ answer must be an integer for question " + qid);
                }
                int idx = v.intValue();
                int n = quizDtoMapper.readChoices(q.getChoicesJson()).size();
                if (idx < 0 || idx >= n) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid choice index for question " + qid);
                }
                out.put(Long.toString(qid), idx);
            } else {
                if (!v.isTextual()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "ESSAY answer must be a string for question " + qid);
                }
                String text = v.textValue().trim();
                if (text.length() > 20000) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "ESSAY answer too long for question " + qid);
                }
                out.put(Long.toString(qid), text);
            }
        }
        return out;
    }

    private int scoreMcqPortion(Quiz quiz, Map<String, Object> answersByQuestionId) {
        int score = 0;
        for (Question question : quiz.getQuestions()) {
            if (question.getQuestionType() != QuestionType.MCQ) {
                continue;
            }
            Object raw = answersByQuestionId.get(Long.toString(question.getId()));
            if (!(raw instanceof Integer selected)) {
                continue;
            }
            if (question.getCorrectChoiceIndex() != null
                    && selected.longValue() == question.getCorrectChoiceIndex()) {
                score += question.getPoints();
            }
        }
        return score;
    }
}
