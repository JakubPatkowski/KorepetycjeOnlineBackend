package com.example.demo.repository;

import com.example.demo.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    RefreshToken findByRefreshTokenAndIp(String refToken, String clientIp);
    void deleteByRefreshTokenAndIp(String refToken, String clientIp);

    Optional<RefreshToken> findByUserIdAndIp(Long userId, String clientIp);
    //void deketeByUserId(Long userId);
}