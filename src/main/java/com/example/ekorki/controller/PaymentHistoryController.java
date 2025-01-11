package com.example.ekorki.controller;

import com.example.ekorki.dto.http.HttpResponseDTO;
import com.example.ekorki.dto.paymentHistory.PaymentHistoryResponseDTO;
import com.example.ekorki.model.UserPrincipals;
import com.example.ekorki.service.entity.PaymentHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/payment-history")
@RequiredArgsConstructor
public class PaymentHistoryController {
    private final PaymentHistoryService paymentHistoryService;

    @GetMapping
    public ResponseEntity<HttpResponseDTO> getPaymentHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UserPrincipals userDetails = (UserPrincipals) authentication.getPrincipal();
        Long userId = userDetails.getId();

        try {
            Page<PaymentHistoryResponseDTO> transactions = paymentHistoryService.getUserTransactions(userId, page, size);

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(LocalDateTime.now().toString())
                    .data(Map.of(
                            "transactions", transactions.getContent(),
                            "currentPage", transactions.getNumber(),
                            "totalItems", transactions.getTotalElements(),
                            "totalPages", transactions.getTotalPages()
                    ))
                    .message("Transaction history retrieved successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(HttpResponseDTO.builder()
                            .timestamp(LocalDateTime.now().toString())
                            .message("Error retrieving transaction history: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
        }
    }
}
