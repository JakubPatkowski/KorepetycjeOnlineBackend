package com.example.demo.service;

import com.example.demo.entity.RefreshTokenEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JWTService jwtService;

    @Value("${jwt.refreshTokenExpiration}")
    private int refreshTokenExpirationTime;

    public String generateRefreshToken(Long userId, String clientIp) {
        Optional<RefreshTokenEntity> existingToken = refreshTokenRepository.findByUserIdAndIp(userId, clientIp);
        RefreshTokenEntity refreshTokenEntity;

        if (existingToken.isPresent()) {
            refreshTokenEntity = existingToken.get();
        } else {
            refreshTokenEntity = new RefreshTokenEntity();
            refreshTokenEntity.setUserId(userId);
            refreshTokenEntity.setIp(clientIp);
        }

        String token = UUID.randomUUID().toString();
        refreshTokenEntity.setRefreshToken(token);
        refreshTokenEntity.setCreatedAt(Instant.now());
        refreshTokenEntity.setExpiration(Instant.now().plusMillis(refreshTokenExpirationTime));
        refreshTokenRepository.save(refreshTokenEntity);
        return token;
    }

    public boolean verifyExpiration(RefreshTokenEntity token) {
        if (token.getExpiration().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new signin request");
        }
        return true;
    }

    public String generateNewAccessToken(String userRefreshToken, String clientIp) {
        RefreshTokenEntity refreshTokenEntity = refreshTokenRepository.findByRefreshTokenAndIp(userRefreshToken, clientIp);
        if (refreshTokenEntity == null) {
            throw new RuntimeException("Refresh token not found");
        }

        Optional<UserEntity> user = userRepository.findById(refreshTokenEntity.getUserId());
        if (verifyExpiration(refreshTokenEntity) && user.isPresent()) {
            return jwtService.generateAccessToken(user.get().getEmail());
        }

        throw new RuntimeException("Invalid refresh token");
    }


}
