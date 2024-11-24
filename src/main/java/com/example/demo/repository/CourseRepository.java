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

    // Podstawowa kwerenda dla wszystkich przypadków
    String BASE_SORT_QUERY = """
        ORDER BY 
        CASE 
            WHEN :sortBy = 'review' THEN COALESCE(c.review, 0)
            WHEN :sortBy = 'reviewNumber' THEN COALESCE(c.review_number, 0)
            ELSE EXTRACT(EPOCH FROM c.created_at)
        END DESC
        """;

    // 1. Wyszukiwanie wszystkich
    @Query(value = """
        SELECT c.* FROM courses c 
        """ + BASE_SORT_QUERY + """
        LIMIT :pageSize
        OFFSET :offset
        """, nativeQuery = true)
    List<CourseEntity> findAllCoursesPaged(
            @Param("sortBy") String sortBy,
            @Param("pageSize") int pageSize,
            @Param("offset") long offset
    );

    // 2. Wyszukiwanie po nazwie
    @Query(value = """
        SELECT c.* FROM courses c 
        WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
        """ + BASE_SORT_QUERY + """
        LIMIT :pageSize
        OFFSET :offset
        """, nativeQuery = true)
    List<CourseEntity> findByNameContaining(
            @Param("search") String search,
            @Param("sortBy") String sortBy,
            @Param("pageSize") int pageSize,
            @Param("offset") long offset
    );

    // 3. Wyszukiwanie po tagu
    @Query(value = """
        SELECT c.* FROM courses c 
        WHERE :tag = ANY(c.tags)
        """ + BASE_SORT_QUERY + """
        LIMIT :pageSize
        OFFSET :offset
        """, nativeQuery = true)
    List<CourseEntity> findByTag(
            @Param("tag") String tag,
            @Param("sortBy") String sortBy,
            @Param("pageSize") int pageSize,
            @Param("offset") long offset
    );

    // 4. Wyszukiwanie po nazwie i tagu
    @Query(value = """
        SELECT c.* FROM courses c 
        WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
        AND :tag = ANY(c.tags)
        """ + BASE_SORT_QUERY + """
        LIMIT :pageSize
        OFFSET :offset
        """, nativeQuery = true)
    List<CourseEntity> findByNameAndTag(
            @Param("search") String search,
            @Param("tag") String tag,
            @Param("sortBy") String sortBy,
            @Param("pageSize") int pageSize,
            @Param("offset") long offset
    );


    // Zapytania COUNT
    @Query(value = """
        SELECT COUNT(*) FROM courses c 
        WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
        """, nativeQuery = true)
    long countByNameContaining(@Param("search") String search);

    @Query(value = """
        SELECT COUNT(*) FROM courses c 
        WHERE :tag = ANY(c.tags)
        """, nativeQuery = true)
    long countByTag(@Param("tag") String tag);

    @Query(value = """
        SELECT COUNT(*) FROM courses c 
        WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
        AND :tag = ANY(c.tags)
        """, nativeQuery = true)
    long countByNameAndTag(
            @Param("search") String search,
            @Param("tag") String tag
    );

    @Query(value = "SELECT COUNT(*) FROM courses", nativeQuery = true)
    long countAll();

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

    @Query("SELECT c FROM CourseEntity c WHERE c.id = :courseId")
    Optional<CourseEntity> findByIdForDetails(@Param("courseId") Long courseId);

    // Nowe zapytania dla filtrowania kursów zalogowanego użytkownika
    @Query(value = """
        SELECT c.* FROM courses c 
        WHERE c.user_id != :userId 
        AND NOT EXISTS (
            SELECT 1 FROM purchased_courses pc 
            WHERE pc.course_id = c.id AND pc.user_id = :userId
        )
        AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
        """ + BASE_SORT_QUERY + """
        LIMIT :pageSize
        OFFSET :offset
        """, nativeQuery = true)
    List<CourseEntity> findAvailableByNameForUser(
            @Param("search") String search,
            @Param("userId") Long userId,
            @Param("sortBy") String sortBy,
            @Param("pageSize") int pageSize,
            @Param("offset") long offset
    );

    @Query(value = """
        SELECT c.* FROM courses c 
        WHERE c.user_id != :userId 
        AND NOT EXISTS (
            SELECT 1 FROM purchased_courses pc 
            WHERE pc.course_id = c.id AND pc.user_id = :userId
        )
        AND :tag = ANY(c.tags)
        """ + BASE_SORT_QUERY + """
        LIMIT :pageSize
        OFFSET :offset
        """, nativeQuery = true)
    List<CourseEntity> findAvailableByTagForUser(
            @Param("tag") String tag,
            @Param("userId") Long userId,
            @Param("sortBy") String sortBy,
            @Param("pageSize") int pageSize,
            @Param("offset") long offset
    );

    @Query(value = """
        SELECT c.* FROM courses c 
        WHERE c.user_id != :userId 
        AND NOT EXISTS (
            SELECT 1 FROM purchased_courses pc 
            WHERE pc.course_id = c.id AND pc.user_id = :userId
        )
        AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
        AND :tag = ANY(c.tags)
        """ + BASE_SORT_QUERY + """
        LIMIT :pageSize
        OFFSET :offset
        """, nativeQuery = true)
    List<CourseEntity> findAvailableByNameAndTagForUser(
            @Param("search") String search,
            @Param("tag") String tag,
            @Param("userId") Long userId,
            @Param("sortBy") String sortBy,
            @Param("pageSize") int pageSize,
            @Param("offset") long offset
    );

    // Nowe zapytania COUNT dla zalogowanego użytkownika
    @Query(value = """
        SELECT COUNT(*) FROM courses c 
        WHERE c.user_id != :userId 
        AND NOT EXISTS (
            SELECT 1 FROM purchased_courses pc 
            WHERE pc.course_id = c.id AND pc.user_id = :userId
        )
        AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
        """, nativeQuery = true)
    long countAvailableByNameForUser(
            @Param("search") String search,
            @Param("userId") Long userId
    );

    @Query(value = """
        SELECT COUNT(*) FROM courses c 
        WHERE c.user_id != :userId 
        AND NOT EXISTS (
            SELECT 1 FROM purchased_courses pc 
            WHERE pc.course_id = c.id AND pc.user_id = :userId
        )
        AND :tag = ANY(c.tags)
        """, nativeQuery = true)
    long countAvailableByTagForUser(
            @Param("tag") String tag,
            @Param("userId") Long userId
    );

    @Query(value = """
        SELECT COUNT(*) FROM courses c 
        WHERE c.user_id != :userId 
        AND NOT EXISTS (
            SELECT 1 FROM purchased_courses pc 
            WHERE pc.course_id = c.id AND pc.user_id = :userId
        )
        AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
        AND :tag = ANY(c.tags)
        """, nativeQuery = true)
    long countAvailableByNameAndTagForUser(
            @Param("search") String search,
            @Param("tag") String tag,
            @Param("userId") Long userId
    );

    @Query(value = """
        SELECT c.* FROM courses c 
        WHERE c.user_id != :userId 
        AND NOT EXISTS (
            SELECT 1 FROM purchased_courses pc 
            WHERE pc.course_id = c.id AND pc.user_id = :userId
        )
        """ + BASE_SORT_QUERY + """
        LIMIT :pageSize
        OFFSET :offset
        """, nativeQuery = true)
    List<CourseEntity> findAllAvailableForUser(
            @Param("userId") Long userId,
            @Param("sortBy") String sortBy,
            @Param("pageSize") int pageSize,
            @Param("offset") long offset
    );

    @Query(value = """
        SELECT COUNT(*) FROM courses c 
        WHERE c.user_id != :userId 
        AND NOT EXISTS (
            SELECT 1 FROM purchased_courses pc 
            WHERE pc.course_id = c.id AND pc.user_id = :userId
        )
        """, nativeQuery = true)
    long countAllAvailableForUser(@Param("userId") Long userId);
}

