package com.example.ekorki.repository;

import com.example.ekorki.entity.UserEntity;
import com.example.ekorki.entity.VerificationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationTokenEntity, Long> {
    Optional<VerificationTokenEntity> findByToken(String token);
    void deleteByUserIdAndTokenType(UserEntity user, VerificationTokenEntity.TokenType tokenType);

    Optional<VerificationTokenEntity> findByTokenAndTokenType(String token, VerificationTokenEntity.TokenType tokenType);
}

