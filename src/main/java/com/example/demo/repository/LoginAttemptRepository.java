package com.example.demo.repository;

import com.example.demo.entity.LoginAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttemptEntity, Long> {

    @Query(value = """
        SELECT COUNT(*) FROM demo.login_attempts 
        WHERE email = :email 
        AND successful = false 
        AND attempt_time > :timeThreshold
    """, nativeQuery = true)
    int countFailedAttempts(@Param("email") String email,
                            @Param("timeThreshold") LocalDateTime timeThreshold);

    @Query(value = """
        SELECT COUNT(*) FROM demo.login_attempts 
        WHERE email = :email 
        AND ip_address = :ipAddress
        AND successful = false 
        AND attempt_time > :timeThreshold
    """, nativeQuery = true)
    int countFailedAttemptsByEmailAndIp(
            @Param("email") String email,
            @Param("ipAddress") String ipAddress,
            @Param("timeThreshold") LocalDateTime timeThreshold
    );
}
