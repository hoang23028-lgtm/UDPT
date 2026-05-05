package com.quiz.repository;

import com.quiz.entity.TeacherAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, Long> {
}
