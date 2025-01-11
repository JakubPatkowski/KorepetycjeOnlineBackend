package com.example.ekorki.service.entity;

import com.example.ekorki.dto.paymentHistory.PaymentHistoryResponseDTO;
import com.example.ekorki.entity.PaymentHistoryEntity;
import com.example.ekorki.entity.UserEntity;
import com.example.ekorki.repository.PaymentHistoryRepository;
import com.example.ekorki.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentHistoryService {
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public void addTransaction(
            Long userId,
            PaymentHistoryEntity.TransactionType type,
            Integer pointsAmount,
            String description,
            Long relatedEntityId,
            PaymentHistoryEntity.RelatedEntityType relatedEntityType
    ) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        PaymentHistoryEntity transaction = PaymentHistoryEntity.builder()
                .user(user)
                .transactionTime(LocalDateTime.now())
                .transactionType(type)
                .pointsAmount(pointsAmount)
                .description(description)
                .balanceAfter(user.getPoints())
                .relatedEntityId(relatedEntityId)
                .relatedEntityType(relatedEntityType)
                .build();

        paymentHistoryRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public Page<PaymentHistoryResponseDTO> getUserTransactions(Long userId, int page, int size) {
        return paymentHistoryRepository.findByUserIdOrderByTransactionTimeDesc(userId, PageRequest.of(page, size))
                .map(this::mapToDTO);
    }

    private PaymentHistoryResponseDTO mapToDTO(PaymentHistoryEntity entity) {
        return PaymentHistoryResponseDTO.builder()
                .id(entity.getId())
                .transactionTime(entity.getTransactionTime())
                .transactionType(entity.getTransactionType())
                .pointsAmount(entity.getPointsAmount())
                .description(entity.getDescription())
                .balanceAfter(entity.getBalanceAfter())
                .relatedEntityId(entity.getRelatedEntityId())
                .relatedEntityType(entity.getRelatedEntityType())
                .build();
    }
}

