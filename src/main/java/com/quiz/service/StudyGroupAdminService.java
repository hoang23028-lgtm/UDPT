package com.quiz.service;

import com.quiz.dto.StudyGroupResponse;
import com.quiz.entity.StudyGroup;
import com.quiz.entity.User;
import com.quiz.exception.ApiException;
import com.quiz.repository.StudyGroupRepository;
import com.quiz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyGroupAdminService {

    private final StudyGroupRepository studyGroupRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<StudyGroupResponse> listGroups() {
        return studyGroupRepository.findAll().stream()
                .map(g -> new StudyGroupResponse(g.getId(), g.getName(), g.getDescription(), g.getCreatedAt()))
                .toList();
    }

    @Transactional
    public StudyGroupResponse createGroup(String name, String description) {
        String n = name.trim();
        if (n.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "name must not be blank");
        }
        StudyGroup g = StudyGroup.builder()
                .name(n)
                .description(description == null ? null : description.trim())
                .build();
        g = studyGroupRepository.save(g);
        return new StudyGroupResponse(g.getId(), g.getName(), g.getDescription(), g.getCreatedAt());
    }

    @Transactional
    public void addMember(Long groupId, Long userId) {
        StudyGroup g = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Study group not found"));
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        u.getStudyGroups().add(g);
    }

    @Transactional
    public void removeMember(Long groupId, Long userId) {
        StudyGroup g = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Study group not found"));
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        u.getStudyGroups().remove(g);
    }
}
