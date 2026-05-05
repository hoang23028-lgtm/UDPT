package com.quiz.repository;

import com.quiz.entity.SchoolYear;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolYearRepository extends JpaRepository<SchoolYear, Long> {
}
