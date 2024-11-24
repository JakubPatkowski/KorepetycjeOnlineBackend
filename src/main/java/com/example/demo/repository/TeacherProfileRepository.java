package com.example.demo.repository;

import com.example.demo.entity.TeacherProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeacherProfileRepository extends JpaRepository<TeacherProfileEntity, Long> {
    boolean existsByUserId(Long userId);

    Optional<TeacherProfileEntity> findByUserId(Long userId);

}
