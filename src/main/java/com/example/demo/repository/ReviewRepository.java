package com.example.demo.repository;

import com.example.demo.entity.ReviewEntity;
import com.example.demo.dto.review.ReviewTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    boolean existsByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, ReviewTargetType targetType);

    Optional<ReviewEntity> findByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, ReviewTargetType targetType);

    @Query(value = """
        SELECT r.* FROM demo.reviews r
        WHERE r.target_id = :targetId 
        AND r.target_type = :targetType
        ORDER BY 
        CASE 
            WHEN :sortBy = 'rating' AND :sortDir = 'asc' THEN r.rating
        END ASC,
        CASE 
            WHEN :sortBy = 'rating' AND :sortDir = 'desc' THEN r.rating
        END DESC,
        CASE 
            WHEN :sortBy = 'date' AND :sortDir = 'asc' THEN COALESCE(r.updated_at, r.created_at)
        END ASC,
        CASE 
            WHEN :sortBy = 'date' AND :sortDir = 'desc' THEN COALESCE(r.updated_at, r.created_at)
        END DESC
        LIMIT :pageSize
        OFFSET :offset
        """, nativeQuery = true)
    List<ReviewEntity> findReviewsWithSorting(
            @Param("targetId") Long targetId,
            @Param("targetType") String targetType,
            @Param("sortBy") String sortBy,
            @Param("sortDir") String sortDir,
            @Param("pageSize") int pageSize,
            @Param("offset") long offset
    );

    @Query(value = """
        SELECT COUNT(*) FROM demo.reviews r
        WHERE r.target_id = :targetId AND r.target_type = :targetType
        """, nativeQuery = true)
    long countReviews(@Param("targetId") Long targetId, @Param("targetType") String targetType);

    @Query(value = """
    SELECT r.* FROM demo.reviews r
    WHERE r.target_id = :targetId 
    AND r.target_type = :targetType
    AND r.user_id != :userId
    ORDER BY 
    CASE 
        WHEN :sortBy = 'rating' AND :sortDir = 'asc' THEN r.rating
    END ASC,
    CASE 
        WHEN :sortBy = 'rating' AND :sortDir = 'desc' THEN r.rating
    END DESC,
    CASE 
        WHEN :sortBy = 'date' AND :sortDir = 'asc' THEN COALESCE(r.updated_at, r.created_at)
    END ASC,
    CASE 
        WHEN :sortBy = 'date' AND :sortDir = 'desc' THEN COALESCE(r.updated_at, r.created_at)
    END DESC
    LIMIT :pageSize
    OFFSET :offset
    """, nativeQuery = true)
    List<ReviewEntity> findReviewsWithSortingExcludingUser(
            @Param("targetId") Long targetId,
            @Param("targetType") String targetType,
            @Param("userId") Long userId,
            @Param("sortBy") String sortBy,
            @Param("sortDir") String sortDir,
            @Param("pageSize") int pageSize,
            @Param("offset") long offset
    );

    @Query(value = """
    SELECT COUNT(*) FROM demo.reviews r
    WHERE r.target_id = :targetId 
    AND r.target_type = :targetType
    AND r.user_id != :userId
    """, nativeQuery = true)
    long countReviewsExcludingUser(
            @Param("targetId") Long targetId,
            @Param("targetType") String targetType,
            @Param("userId") Long userId
    );
}
