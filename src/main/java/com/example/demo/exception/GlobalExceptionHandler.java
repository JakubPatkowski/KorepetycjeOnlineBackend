package com.example.demo.exception;

import com.example.demo.dto.HttpResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<HttpResponseDTO> handleApiException(ApiException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                HttpResponseDTO.builder()
                        .timestamp(LocalDateTime.now().toString())
                        .message(ex.getMessage())
                        .status(HttpStatus.UNAUTHORIZED)
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .build()
        );
    }

}
