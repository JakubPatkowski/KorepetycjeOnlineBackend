package com.example.ekorki.service.entity;

import com.example.ekorki.entity.RefreshTokenEntity;
import com.example.ekorki.entity.UserEntity;
import com.example.ekorki.exception.ApiException;
import com.example.ekorki.repository.RefreshTokenRepository;
import com.example.ekorki.repository.UserRepository;
import com.example.ekorki.service.IpHasher;
import com.example.ekorki.service.JWTService;
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

    @Autowired
    private IpHasher ipHasher;

    @Value("${jwt.refreshTokenExpiration}")
    private int refreshTokenExpirationTime;

    public String generateRefreshToken(Long userId, String clientIp) {
        String hashedIp = ipHasher.hashIp(clientIp);
        Optional<RefreshTokenEntity> existingToken = refreshTokenRepository.findByUserIdAndIp(userId, hashedIp);
        RefreshTokenEntity refreshTokenEntity;

        if (existingToken.isPresent()) {
            refreshTokenEntity = existingToken.get();
        } else {
            refreshTokenEntity = new RefreshTokenEntity();
            refreshTokenEntity.setUserId(userId);
            refreshTokenEntity.setIp(hashedIp);
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
        String hashedIp = ipHasher.hashIp(clientIp);
        RefreshTokenEntity refreshTokenEntity = refreshTokenRepository.findByRefreshTokenAndIp(userRefreshToken, hashedIp);

        if (refreshTokenEntity == null) {
            throw new RuntimeException("Refresh token not found");
        }

        if (!ipHasher.compareIp(clientIp, refreshTokenEntity.getIp())) {
            throw new RuntimeException("Invalid IP address");
        }

        Optional<UserEntity> user = userRepository.findById(refreshTokenEntity.getUserId());
        if (verifyExpiration(refreshTokenEntity) && user.isPresent()) {
            return jwtService.generateAccessToken(user.get().getEmail());
        }

        throw new RuntimeException("Invalid refresh token");
    }

    public void logout(String refreshToken, String clientIp, Long userId) {
        String hashedIp = ipHasher.hashIp(clientIp);
        RefreshTokenEntity token = refreshTokenRepository.findByRefreshTokenAndIp(refreshToken, hashedIp);

        if (token == null) {
            throw new ApiException("Invalid refresh token");
        }

        // Sprawdzamy czy token należy do zalogowanego użytkownika
        if (!token.getUserId().equals(userId)) {
            throw new ApiException("Unauthorized logout attempt");
        }

        refreshTokenRepository.delete(token);
    }
}
