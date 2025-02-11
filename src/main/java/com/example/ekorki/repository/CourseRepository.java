package com.example.ekorki.repository;

import com.example.ekorki.entity.CourseEntity;
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

    // Base q
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
        SELECT c.* FROM e_korki.courses c 
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
        SELECT c.* FROM e_korki.courses c 
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
        SELECT c.* FROM e_korki.courses c 
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

    @Query(value = """
    SELECT c.* FROM e_korki.courses c 
    WHERE c.tags && cast(:tags as character varying[])
    """ + BASE_SORT_QUERY + """
    LIMIT :pageSize
    OFFSET :offset
    """, nativeQuery = true)
    List<CourseEntity> findByTags(
            @Param("tags") List<String> tags,
            @Param("sortBy") String sortBy,
            @Param("pageSize") int pageSize,
            @Param("offset") long offset
    );

    // 4. Wyszukiwanie po nazwie i tagu
    @Query(value = """
        SELECT c.* FROM e_korki.courses c 
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

    @Query(value = """
        SELECT c.* FROM e_korki.courses c 
        WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
        AND c.tags && ARRAY[:tags]
        """ + BASE_SORT_QUERY + """
        LIMIT :pageSize
        OFFSET :offset
        """, nativeQuery = true)
    List<CourseEntity> findByNameAndTags(
            @Param("search") String search,
            @Param("tags") String[] tags,  // Zmień List<String> na String[]
            @Param("sortBy") String sortBy,
            @Param("pageSize") int pageSize,
            @Param("offset") long offset
    );


    // Zapytania COUNT
    @Query(value = """
        SELECT COUNT(*) FROM e_korki.courses c 
        WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
        """, nativeQuery = true)
    long countByNameContaining(@Param("search") String search);

    @Query(value = """
        SELECT COUNT(*) FROM e_korki.courses c 
        WHERE :tag = ANY(c.tags)
        """, nativeQuery = true)
    long countByTag(@Param("tag") String tag);

    @Query(value = """
    SELECT COUNT(*) FROM e_korki.courses c 
    WHERE c.tags && cast(:tags as character varying[])
    """, nativeQuery = true)
    long countByTags(@Param("tags") List<String> tags);

    @Query(value = """
        SELECT COUNT(*) FROM e_korki.courses c 
        WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
        AND :tag = ANY(c.tags)
        """, nativeQuery = true)
    long countByNameAndTag(
            @Param("search") String search,
            @Param("tag") String tag
    );

    @Query(value = """
    SELECT COUNT(*) FROM e_korki.courses c 
    WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
    AND c.tags && cast(:tags as character varying[])
    """, nativeQuery = true)
    long countByNameAndTags(
            @Param("search") String search,
            @Param("tags") String[] tags
    );

    @Query(value = "SELECT COUNT(*) FROM e_korki.courses", nativeQuery = true)
    long countAll();

    @Query(value = """
            WITH RECURSIVE TagsList AS (
                SELECT DISTINCT unnest(tags) as tag
                FROM e_korki.courses
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
        SELECT c.* FROM e_korki.courses c 
        WHERE c.user_id != :userId 
        AND NOT EXISTS (
            SELECT 1 FROM e_korki.purchased_courses pc 
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
        SELECT c.* FROM e_korki.courses c 
        WHERE c.user_id != :userId 
        AND NOT EXISTS (
            SELECT 1 FROM e_korki.purchased_courses pc 
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
    SELECT c.* FROM e_korki.courses c 
    WHERE c.user_id != :userId 
    AND NOT EXISTS (
        SELECT 1 FROM e_korki.purchased_courses pc 
        WHERE pc.course_id = c.id AND pc.user_id = :userId
    )
    AND c.tags && cast(:tags as character varying[])
    """ + BASE_SORT_QUERY + """
    LIMIT :pageSize
    OFFSET :offset
    """, nativeQuery = true)
    List<CourseEntity> findAvailableByTagsForUser(
            @Param("tags") List<String> tags,
            @Param("userId") Long userId,
            @Param("sortBy") String sortBy,
            @Param("pageSize") int pageSize,
            @Param("offset") long offset
    );

    @Query(value = """
        SELECT c.* FROM e_korki.courses c 
        WHERE c.user_id != :userId 
        AND NOT EXISTS (
            SELECT 1 FROM e_korki.purchased_courses pc 
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

    @Query(value = """
    SELECT c.* FROM e_korki.courses c 
    WHERE c.user_id != :userId 
    AND NOT EXISTS (
        SELECT 1 FROM e_korki.purchased_courses pc 
        WHERE pc.course_id = c.id AND pc.user_id = :userId
    )
    AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
    AND c.tags && cast(:tags as character varying[])
    """ + BASE_SORT_QUERY + """
    LIMIT :pageSize
    OFFSET :offset
    """, nativeQuery = true)
    List<CourseEntity> findAvailableByNameAndTagsForUser(
            @Param("search") String search,
            @Param("tags") List<String> tags,
            @Param("userId") Long userId,
            @Param("sortBy") String sortBy,
            @Param("pageSize") int pageSize,
            @Param("offset") long offset
    );

    // Nowe zapytania COUNT dla zalogowanego użytkownika
    @Query(value = """
        SELECT COUNT(*) FROM e_korki.courses c 
        WHERE c.user_id != :userId 
        AND NOT EXISTS (
            SELECT 1 FROM e_korki.purchased_courses pc 
            WHERE pc.course_id = c.id AND pc.user_id = :userId
        )
        AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
        """, nativeQuery = true)
    long countAvailableByNameForUser(
            @Param("search") String search,
            @Param("userId") Long userId
    );

    @Query(value = """
        SELECT COUNT(*) FROM e_korki.courses c 
        WHERE c.user_id != :userId 
        AND NOT EXISTS (
            SELECT 1 FROM e_korki.purchased_courses pc 
            WHERE pc.course_id = c.id AND pc.user_id = :userId
        )
        AND :tag = ANY(c.tags)
        """, nativeQuery = true)
    long countAvailableByTagForUser(
            @Param("tag") String tag,
            @Param("userId") Long userId
    );

    @Query(value = """
    SELECT COUNT(*) FROM e_korki.courses c 
    WHERE c.user_id != :userId 
    AND NOT EXISTS (
        SELECT 1 FROM e_korki.purchased_courses pc 
        WHERE pc.course_id = c.id AND pc.user_id = :userId
    )
    AND  c.tags && cast(:tags as character varying[])
    """, nativeQuery = true)
    long countAvailableByTagsForUser(
            @Param("tags") List<String> tags,
            @Param("userId") Long userId
    );

    @Query(value = """
        SELECT COUNT(*) FROM e_korki.courses c 
        WHERE c.user_id != :userId 
        AND NOT EXISTS (
            SELECT 1 FROM e_korki.purchased_courses pc 
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
    SELECT COUNT(*) FROM e_korki.courses c 
    WHERE c.user_id != :userId 
    AND NOT EXISTS (
        SELECT 1 FROM e_korki.purchased_courses pc 
        WHERE pc.course_id = c.id AND pc.user_id = :userId
    )
    AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
    AND  c.tags && cast(:tags as character varying[])
    """, nativeQuery = true)
    long countAvailableByNameAndTagsForUser(
            @Param("search") String search,
            @Param("tags") List<String> tags,
            @Param("userId") Long userId
    );

    @Query(value = """
        SELECT c.* FROM e_korki.courses c 
        WHERE c.user_id != :userId 
        AND NOT EXISTS (
            SELECT 1 FROM e_korki.purchased_courses pc 
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
        SELECT COUNT(*) FROM e_korki.courses c 
        WHERE c.user_id != :userId 
        AND NOT EXISTS (
            SELECT 1 FROM e_korki.purchased_courses pc 
            WHERE pc.course_id = c.id AND pc.user_id = :userId
        )
        """, nativeQuery = true)
    long countAllAvailableForUser(@Param("userId") Long userId);

    @Query(value = """
    SELECT c.* FROM e_korki.courses c
    WHERE c.review_number > 0
    AND (:userId IS NULL OR (
        c.user_id != :userId 
        AND NOT EXISTS (
            SELECT 1 FROM e_korki.purchased_courses pc 
            WHERE pc.course_id = c.id AND pc.user_id = :userId
        )
    ))
    ORDER BY 
        (c.review + 1.96 * 1.96 / (2 * c.review_number) - 
        1.96 * SQRT((c.review * (5 - c.review) + 1.96 * 1.96 / (4 * c.review_number)) / c.review_number)) /
        (1 + 1.96 * 1.96 / c.review_number) DESC
    LIMIT 3
    """, nativeQuery = true)
    List<CourseEntity> findBestCourses(@Param("userId") Long userId);

    @Query("SELECT c FROM CourseEntity c " +
            "LEFT JOIN FETCH c.chapters ch " +
            "LEFT JOIN FETCH ch.subchapters " +
            "WHERE c.id = :courseId")
    Optional<CourseEntity> findByIdWithChaptersAndSubchapters(@Param("courseId") Long courseId);
}