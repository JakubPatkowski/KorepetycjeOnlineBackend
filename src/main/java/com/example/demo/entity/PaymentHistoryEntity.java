package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(schema = "demo", name = "payment_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "transaction_time", nullable = false)
    private LocalDateTime transactionTime;

    @Column(name = "transaction_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Column(name = "points_amount", nullable = false)
    private Integer pointsAmount;

    @Column(nullable = false)
    private String description;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "related_entity_type")
    @Enumerated(EnumType.STRING)
    private RelatedEntityType relatedEntityType;

    public enum TransactionType {
        POINTS_PURCHASE,
        POINTS_WITHDRAWAL,
        COURSE_PURCHASE,
        COURSE_SOLD,
        TASK_PUBLISHED,
        TASK_SOLVED
    }

    public enum RelatedEntityType {
        COURSE,
        TASK
    }
}