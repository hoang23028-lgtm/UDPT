package com.quiz.controller;

import com.quiz.dto.BankQuestionRequest;
import com.quiz.entity.UserRole;
import com.quiz.security.UserPrincipal;
import com.quiz.service.BankQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/bank-questions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class TeacherBankQuestionController {

    private final BankQuestionService bankQuestionService;

    @GetMapping
    public List<Map<String, Object>> list(Authentication authentication) {
        UserPrincipal p = (UserPrincipal) authentication.getPrincipal();
        return bankQuestionService.listForTeacher(p.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(Authentication authentication, @Valid @RequestBody BankQuestionRequest body) {
        UserPrincipal p = (UserPrincipal) authentication.getPrincipal();
        return bankQuestionService.create(p.getId(), body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        UserPrincipal p = (UserPrincipal) authentication.getPrincipal();
        boolean admin = p.getRole() == UserRole.ADMIN;
        bankQuestionService.delete(p.getId(), id, admin);
    }
}
