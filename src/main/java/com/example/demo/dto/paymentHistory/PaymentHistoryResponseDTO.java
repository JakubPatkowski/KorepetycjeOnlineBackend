package com.example.demo.dto.paymentHistory;

import com.example.demo.entity.PaymentHistoryEntity.TransactionType;
import com.example.demo.entity.PaymentHistoryEntity.RelatedEntityType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentHistoryResponseDTO {
    private Long id;
    private LocalDateTime transactionTime;
    private TransactionType transactionType;
    private Integer pointsAmount;
    private String description;
    private Integer balanceAfter;
    private Long relatedEntityId;
    private RelatedEntityType relatedEntityType;
}