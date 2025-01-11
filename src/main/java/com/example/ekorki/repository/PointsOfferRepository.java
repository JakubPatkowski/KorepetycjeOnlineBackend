package com.example.ekorki.repository;

import com.example.ekorki.entity.PointsOfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PointsOfferRepository extends JpaRepository<PointsOfferEntity, Long> {
    List<PointsOfferEntity> findByOfferType(PointsOfferEntity.OfferType offerType);
    Optional<PointsOfferEntity> findByIdAndOfferType(Long id, PointsOfferEntity.OfferType offerType);
}
