package com.quiz.repository;

import com.quiz.entity.ClassRoster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ClassRosterRepository extends JpaRepository<ClassRoster, Long> {

    @Query("select distinct cr.schoolClass.id from ClassRoster cr where cr.student.id = :studentId")
    List<Long> findSchoolClassIdsByStudentId(@Param("studentId") Long studentId);

    boolean existsByStudent_IdAndSchoolClass_IdIn(Long studentId, Collection<Long> classIds);
}
