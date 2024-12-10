package com.example.demo.repository;

import com.example.demo.entity.PurchasedCourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchasedCourseRepository extends JpaRepository<PurchasedCourseEntity, Long> {
    List<PurchasedCourseEntity> findByUserId(Long userId);
    Optional<PurchasedCourseEntity> findByUserIdAndCourseId(Long userId, Long courseId);
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    boolean existsByUserIdAndCourseUserId(Long userId, Long teacherId);

}
