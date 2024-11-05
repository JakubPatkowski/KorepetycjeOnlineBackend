package com.example.demo.repository;

import com.example.demo.entity.PointsOfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointsOfferRepository extends JpaRepository<PointsOfferEntity, Long> {
    List<PointsOfferEntity> findByActiveTrue();
}
