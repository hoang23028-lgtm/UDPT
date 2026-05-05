package com.quiz.controller;

import com.quiz.dto.AdminResetPasswordRequest;
import com.quiz.dto.AdminUserCreateRequest;
import com.quiz.dto.AdminUserPatchRequest;
import com.quiz.dto.UserResponse;
import com.quiz.security.UserPrincipal;
import com.quiz.repository.UserRepository;
import com.quiz.service.QuizDtoMapper;
import com.quiz.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final QuizDtoMapper quizDtoMapper;

    @GetMapping
    public List<UserResponse> list() {
        return userRepository.findAll().stream()
                .map(quizDtoMapper::toUserResponse)
                .sorted(Comparator.comparing(UserResponse::id))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody AdminUserCreateRequest request) {
        return userService.adminCreateUser(request);
    }

    @PatchMapping("/{id}")
    public UserResponse patch(@PathVariable Long id, @Valid @RequestBody AdminUserPatchRequest request) {
        return userService.adminPatchUser(id, request);
    }

    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody AdminResetPasswordRequest request) {
        userService.adminResetPassword(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        userService.adminDeleteUser(id, principal.getId());
    }
}
