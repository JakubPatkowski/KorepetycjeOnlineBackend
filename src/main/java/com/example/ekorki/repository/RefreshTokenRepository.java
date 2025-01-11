package com.example.ekorki.repository;

import com.example.ekorki.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {
    RefreshTokenEntity findByRefreshTokenAndIp(String refToken, String clientIp);
    void deleteByRefreshTokenAndIp(String refToken, String clientIp);

    Optional<RefreshTokenEntity> findByUserIdAndIp(Long userId, String clientIp);
    //void deketeByUserId(Long userId);
}