package com.quiz.controller;

import com.quiz.dto.PasswordChangeRequest;
import com.quiz.dto.UserProfileUpdateRequest;
import com.quiz.dto.UserResponse;
import com.quiz.dto.MySubmissionSummaryResponse;
import com.quiz.security.UserPrincipal;
import com.quiz.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Validated
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse getMe(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return userService.getById(principal.getId());
    }

    @PatchMapping("/me")
    public UserResponse patchMe(
            Authentication authentication,
            @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return userService.updateProfile(principal.getId(), request);
    }

    @PostMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            Authentication authentication,
            @Valid @RequestBody PasswordChangeRequest request
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        userService.changePassword(principal.getId(), request);
    }

    @GetMapping("/me/submissions")
    public List<MySubmissionSummaryResponse> mySubmissions(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return userService.listMySubmissions(principal.getId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@userSecurity.isSelfOrAdmin(#id)")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getById(id);
    }
}
