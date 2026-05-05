package com.quiz.repository;

import com.quiz.entity.GradeLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GradeLevelRepository extends JpaRepository<GradeLevel, Long> {

    Optional<GradeLevel> findByCode(String code);
}
