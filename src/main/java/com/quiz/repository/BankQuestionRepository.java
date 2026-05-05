package com.quiz.repository;

import com.quiz.entity.BankQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankQuestionRepository extends JpaRepository<BankQuestion, Long> {

    List<BankQuestion> findByTeacher_IdOrderByIdDesc(Long teacherId);

    long countByTeacher_Id(Long teacherId);
}
