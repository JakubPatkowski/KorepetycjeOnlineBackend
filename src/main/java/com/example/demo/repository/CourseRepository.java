package com.example.demo.repository;

import com.example.demo.entity.CourseEntity;
import com.example.demo.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<CourseEntity, Long> {
    Optional<List<CourseEntity>> findAllByUserId(Long userId);

    Optional<CourseEntity> findByUserIdAndId(Long userId, Long courseId);

    @Query("SELECT DISTINCT c FROM CourseEntity c " +
            "WHERE EXISTS (SELECT t FROM c.tags t WHERE t IN :tags)")
    Page<CourseEntity> findByTagsContainingAny(@Param("tags") List<String> tags, Pageable pageable);

    @Query(value = """
            WITH RECURSIVE TagsList AS (
                SELECT DISTINCT unnest(tags) as tag
                FROM courses
            )
            SELECT tag FROM TagsList
            WHERE :search IS NULL 
            OR LOWER(tag) LIKE LOWER(CONCAT('%', :search, '%'))
            """,
            nativeQuery = true)
    List<String> searchTags(@Param("search") String search);

    @Query("SELECT DISTINCT c FROM CourseEntity c " +
            "LEFT JOIN FETCH c.user " +
            "WHERE (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (COALESCE(:tags, NULL) IS NULL OR EXISTS (SELECT t FROM c.tags t WHERE t IN :tags))")
    Page<CourseEntity> searchCourses(
            @Param("search") String search,
            @Param("tags") List<String> tags,
            Pageable pageable

    );

    @Query(value = """
            WITH RECURSIVE TagsList AS (
                SELECT DISTINCT unnest(tags) as tag
                FROM courses
            )
            SELECT tag FROM TagsList
            """,
            nativeQuery = true)
    List<String> findAllTags();
}

