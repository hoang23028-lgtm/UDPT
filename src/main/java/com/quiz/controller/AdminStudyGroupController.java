package com.quiz.controller;

import com.quiz.dto.StudyGroupResponse;
import com.quiz.service.StudyGroupAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/study-groups")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStudyGroupController {

    private final StudyGroupAdminService studyGroupAdminService;

    @GetMapping
    public List<StudyGroupResponse> list() {
        return studyGroupAdminService.listGroups();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudyGroupResponse create(@Valid @RequestBody StudyGroupCreateRequest request) {
        return studyGroupAdminService.createGroup(request.name(), request.description());
    }

    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addMember(@PathVariable Long groupId, @Valid @RequestBody MemberIdRequest request) {
        studyGroupAdminService.addMember(groupId, request.userId());
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long groupId, @PathVariable Long userId) {
        studyGroupAdminService.removeMember(groupId, userId);
    }

    public record StudyGroupCreateRequest(
            @NotBlank String name,
            String description
    ) {
    }

    public record MemberIdRequest(@NotNull Long userId) {
    }
}
