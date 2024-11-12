package com.example.demo.repository;

import com.example.demo.entity.ChapterEntity;
import com.example.demo.entity.CourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<ChapterEntity, Long> {
//    List<ChapterEntity> findByCourseIdOrderByOrderAsc(Long courseId);
//
//    Optional<ChapterEntity> findByCourseIdAndId(Long courseId, Long chapterId);

}
