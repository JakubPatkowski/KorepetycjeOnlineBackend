package com.example.demo.repository;

import com.example.demo.entity.TeacherProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherProfileRepository extends JpaRepository<TeacherProfileEntity, Long> {
    boolean existsByUserId(Long userId);

    Optional<TeacherProfileEntity> findByUserId(Long userId);

    @Query(value = """
    SELECT tp.* FROM demo.teacher_profiles tp
    JOIN demo.user_profiles up ON tp.user_id = up.user_id
    WHERE tp.review_number > 0
    AND (:userId IS NULL OR tp.user_id != :userId)
    ORDER BY 
        (tp.review + 1.96 * 1.96 / (2 * tp.review_number) - 
        1.96 * SQRT((tp.review * (5 - tp.review) + 1.96 * 1.96 / (4 * tp.review_number)) / tp.review_number)) /
        (1 + 1.96 * 1.96 / tp.review_number) DESC
    LIMIT 3
    """, nativeQuery = true)
    List<TeacherProfileEntity> findBestTeachers(@Param("userId") Long userId);
}
