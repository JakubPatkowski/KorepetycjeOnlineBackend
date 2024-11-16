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
}

