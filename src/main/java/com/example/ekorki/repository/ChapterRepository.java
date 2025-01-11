package com.example.ekorki.repository;

import com.example.ekorki.entity.ChapterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChapterRepository extends JpaRepository<ChapterEntity, Long> {
//    List<ChapterEntity> findByCourseIdOrderByOrderAsc(Long courseId);
//
//    Optional<ChapterEntity> findByCourseIdAndId(Long courseId, Long chapterId);

}
