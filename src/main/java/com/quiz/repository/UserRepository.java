package com.quiz.repository;

import com.quiz.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    @Query("select distinct u from User u left join fetch u.studyGroups where u.id = :id")
    Optional<User> findByIdWithStudyGroups(@Param("id") Long id);
}
