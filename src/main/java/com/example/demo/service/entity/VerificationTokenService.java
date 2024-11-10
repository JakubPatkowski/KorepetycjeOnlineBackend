package com.example.demo.service.entity;

import com.example.demo.entity.UserEntity;
import com.example.demo.entity.VerificationTokenEntity;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

// logi
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class VerificationTokenService {

    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(VerificationTokenService.class);


    public String generateEmailVerificationToken(UserEntity user){
        String token = UUID.randomUUID().toString();
        return createToken(user, token, VerificationTokenEntity.TokenType.EMAIL_VERIFICATION, 24*60);
    }

    public String generateEmailChangeCode(UserEntity user){
        String code = generateRandomCode();
        return createToken(user, code, VerificationTokenEntity.TokenType.EMAIL_CHANGE, 15);
    }
    public String generatePasswordChangeCode(UserEntity user){
        String code = generateRandomCode();
        return createToken(user, code, VerificationTokenEntity.TokenType.PASSWORD_CHANGE, 15);
    }


    private String createToken(UserEntity user, String tokenString, VerificationTokenEntity.TokenType tokenType, int expirationTimeInMinutes){
        VerificationTokenEntity tokenEntity = new VerificationTokenEntity();
        tokenEntity.setToken(tokenString);
        tokenEntity.setUserId(user);
        tokenEntity.setExpiryDate(Instant.now().plusSeconds(expirationTimeInMinutes * 60L));
        tokenEntity.setTokenType(tokenType);

        tokenRepository.deleteByUserIdAndTokenType(user, tokenType);
        tokenRepository.save(tokenEntity);
        return tokenString;
    }

    private String generateRandomCode() {
        return String.format("%08d", new Random().nextInt(100000000));
    }

    public Optional<UserEntity> getUserByToken(String token, VerificationTokenEntity.TokenType tokenType) {
        Optional<VerificationTokenEntity> optionalToken = tokenRepository.findByTokenAndTokenType(token, tokenType);

        if (optionalToken.isEmpty() || optionalToken.get().isExpired()) {
            return Optional.empty();
        }

        return Optional.of(optionalToken.get().getUserId());
    }

    public Optional<VerificationTokenEntity> findByToken(String token, VerificationTokenEntity.TokenType tokenType) {
        return tokenRepository.findByTokenAndTokenType(token, tokenType);
    }

    public void deleteToken(String token){
        tokenRepository.findByToken(token).ifPresent(tokenRepository::delete);
    }
}
