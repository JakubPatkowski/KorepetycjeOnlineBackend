package com.example.ekorki.repository;

import com.example.ekorki.entity.ContentItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentItemRepository extends JpaRepository<ContentItemEntity, Long> {
    List<ContentItemEntity> findBySubchapterIdOrderByOrderAsc(Long subchapterId);




}
