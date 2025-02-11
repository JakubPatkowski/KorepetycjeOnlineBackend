package com.example.ekorki.repository;

import com.example.ekorki.entity.LoginAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttemptEntity, Long> {

    @Query(value = """
        SELECT COUNT(*) FROM e_korki.login_attempts 
        WHERE email = :email 
        AND successful = false 
        AND attempt_time > :timeThreshold
    """, nativeQuery = true)
    int countFailedAttempts(@Param("email") String email,
                            @Param("timeThreshold") LocalDateTime timeThreshold);

    @Query(value = """
    SELECT COUNT(*) FROM e_korki.login_attempts 
    WHERE ip = :hashedIp 
    AND successful = false 
    AND attempt_time > :timeThreshold
""", nativeQuery = true)
    int countFailedAttemptsFromIp(@Param("hashedIp") String hashedIp,
                                  @Param("timeThreshold") LocalDateTime timeThreshold);
}
