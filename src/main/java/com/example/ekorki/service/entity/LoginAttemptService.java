package com.example.ekorki.service.entity;

import com.example.ekorki.entity.LoginAttemptEntity;
import com.example.ekorki.service.IpHasher;

import com.example.ekorki.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private IpHasher ipHasher;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int BLOCK_DURATION_MINUTES = 5;

    @Transactional(readOnly = true)
    public boolean isAccountBlocked(String email, String clientIp) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(BLOCK_DURATION_MINUTES);

        // Check failed attempts for email
        int emailFailedAttempts = loginAttemptRepository.countFailedAttempts(email, threshold);
        logger.info("Failed login attempts for email {}: {}", email, emailFailedAttempts);

        String hashedIp = ipHasher.hashIp(clientIp);
        int ipFailedAttempts = loginAttemptRepository.countFailedAttemptsFromIp(hashedIp, threshold);

        if (ipFailedAttempts >= MAX_FAILED_ATTEMPTS) {
            return true;
        }
        return false;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginAttempt(String email, String clientIp, boolean successful) {
        try {
            String hashedIp = ipHasher.hashIp(clientIp);
            LoginAttemptEntity attempt = LoginAttemptEntity.builder()
                    .email(email)
                    .ipAddress(hashedIp)
                    .attemptTime(LocalDateTime.now())
                    .successful(successful)
                    .build();

            loginAttemptRepository.save(attempt);

            if (!successful) {
                logger.warn("Failed login attempt recorded - Email: {}, IP: {}", email, clientIp);
            } else {
                logger.info("Successful login recorded - Email: {}", email);
            }
        } catch (Exception e) {
            logger.error("Error recording login attempt", e);
            // Don't rethrow - we don't want this to affect the main login flow
        }
    }
}