package com.example.ekorki.repository;

import com.example.ekorki.entity.PaymentHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistoryEntity, Long> {
    Page<PaymentHistoryEntity> findByUserIdOrderByTransactionTimeDesc(Long userId, Pageable pageable);
}

