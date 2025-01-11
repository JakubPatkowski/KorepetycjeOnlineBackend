package com.example.ekorki.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;


@Entity
@Table(schema = "e_korki", name = "verification_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity userId;

    @Column(nullable = false, name = "expiration")
    private Instant expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "token_type")
    private TokenType tokenType;

    @Column(name = "new_email")
    private String newEmail;

    public enum TokenType {
        EMAIL_VERIFICATION,
        EMAIL_CHANGE,
        PASSWORD_CHANGE
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiryDate);
    }
}
