package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

@Data
@Entity
@Table(schema = "demo", name = "refresh_tokens")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_DEFAULT)
public class RefreshTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "ref_token")
    private String refreshToken;

    @Column(nullable = false, name = "user_id")
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private Long userId;

    @Column(nullable = false)
    private String ip;

    @Column(nullable = false)
    private Instant expiration;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;

}
