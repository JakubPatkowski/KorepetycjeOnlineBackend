package com.example.ekorki.repository;

import com.example.ekorki.entity.SubchapterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubchapterRepository extends JpaRepository<SubchapterEntity, Long> {
    List<SubchapterEntity> findByChapterIdOrderByOrderAsc(Long chapterId);
    Optional<SubchapterEntity> findByChapterIdAndId(Long chapterId, Long subchapterId);

}
