package com.quiz.service;

import com.quiz.dto.PasswordChangeRequest;
import com.quiz.dto.UserProfileUpdateRequest;
import com.quiz.dto.UserRegistrationRequest;
import com.quiz.dto.UserResponse;
import com.quiz.dto.MySubmissionSummaryResponse;
import com.quiz.entity.User;
import com.quiz.entity.UserRole;
import com.quiz.exception.ApiException;
import com.quiz.repository.BankQuestionRepository;
import com.quiz.repository.QuizRepository;
import com.quiz.repository.SubmissionRepository;
import com.quiz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.CannotSerializeTransactionException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final QuizDtoMapper quizDtoMapper;
    private final SubmissionRepository submissionRepository;
    private final QuizRepository quizRepository;
    private final BankQuestionRepository bankQuestionRepository;

    @Retryable(
            retryFor = {
                    ConcurrencyFailureException.class,
                    CannotSerializeTransactionException.class,
                    CannotAcquireLockException.class,
                    TransientDataAccessResourceException.class
            },
            maxAttempts = 12,
            backoff = @Backoff(delay = 25, multiplier = 2, maxDelay = 500, random = true)
    )
    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        UserRole role = resolveRegistrationRole(request.role());
        User user = User.builder()
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName().trim())
                .role(role)
                .build();
        user = userRepository.save(user);
        return quizDtoMapper.toUserResponse(user);
    }

    private static UserRole resolveRegistrationRole(String raw) {
        if (raw == null || raw.isBlank()) {
            return UserRole.STUDENT;
        }
        UserRole parsed;
        try {
            parsed = UserRole.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid role (allowed: STUDENT, TEACHER)");
        }
        if (parsed != UserRole.STUDENT && parsed != UserRole.TEACHER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid role (allowed: STUDENT, TEACHER)");
        }
        return parsed;
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return quizDtoMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public List<MySubmissionSummaryResponse> listMySubmissions(Long userId) {
        return submissionRepository.findByUser_IdWithResultAndQuiz(userId).stream()
                .map(s -> new MySubmissionSummaryResponse(
                        s.getId().toString(),
                        s.getQuiz().getId().toString(),
                        s.getQuiz().getTitle(),
                        s.getQuiz().isPublished(),
                        s.getAttemptNumber(),
                        s.getSubmittedAt(),
                        Math.toIntExact(s.getResult().getScore()),
                        Math.toIntExact(s.getResult().getMaxScore()),
                        s.getResult().getPercentage(),
                        s.getResult().getTimeBonus(),
                        s.getResult().getRankScore()
                ))
                .toList();
    }

    @Retryable(
            retryFor = {ConcurrencyFailureException.class, CannotSerializeTransactionException.class},
            maxAttempts = 12,
            backoff = @Backoff(delay = 25, multiplier = 2, maxDelay = 500, random = true)
    )
    @Transactional
    public UserResponse updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (request.displayName() != null) {
            String dn = request.displayName().trim();
            if (dn.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "displayName must not be empty");
            }
            user.setDisplayName(dn);
        }
        if (request.phone() != null) {
            user.setPhone(request.phone().trim().isEmpty() ? null : request.phone().trim());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl().trim().isEmpty() ? null : request.avatarUrl().trim());
        }
        user = userRepository.save(user);
        return quizDtoMapper.toUserResponse(user);
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Retryable(
            retryFor = {ConcurrencyFailureException.class, CannotSerializeTransactionException.class},
            maxAttempts = 12,
            backoff = @Backoff(delay = 25, multiplier = 2, maxDelay = 500, random = true)
    )
    @Transactional
    public UserResponse adminCreateUser(com.quiz.dto.AdminUserCreateRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        UserRole role;
        try {
            role = UserRole.valueOf(request.role().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid role");
        }
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName().trim())
                .role(role)
                .build();
        user = userRepository.save(user);
        return quizDtoMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse adminPatchUser(Long id, com.quiz.dto.AdminUserPatchRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (request.accountLocked() != null) {
            user.setAccountLocked(request.accountLocked());
        }
        if (request.role() != null && !request.role().isBlank()) {
            try {
                user.setRole(UserRole.valueOf(request.role().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid role");
            }
        }
        user = userRepository.save(user);
        return quizDtoMapper.toUserResponse(user);
    }

    @Transactional
    public void adminResetPassword(Long id, com.quiz.dto.AdminResetPasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void adminDeleteUser(Long id, Long adminId) {
        if (adminId != null && adminId.equals(id)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot delete your own account");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() == UserRole.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot delete an ADMIN account");
        }
        long submissions = submissionRepository.countByUser_Id(id);
        if (submissions > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot delete user with existing submissions");
        }
        // Quizzes will keep working (created_by_user_id is nullable / ON DELETE SET NULL),
        // but we prevent deletion if the teacher still owns content to avoid orphaning authorship.
        long createdQuizzes = quizRepository.countByCreatedBy_Id(id);
        long bankQuestions = bankQuestionRepository.countByTeacher_Id(id);
        if (createdQuizzes > 0 || bankQuestions > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot delete user who still owns quizzes/question bank items");
        }
        userRepository.delete(user);
    }
}
