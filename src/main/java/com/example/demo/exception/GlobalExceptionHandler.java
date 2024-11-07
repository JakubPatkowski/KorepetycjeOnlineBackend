package com.example.demo.exception;

import com.example.demo.dto.http.HttpResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

//    @ExceptionHandler(ApiException.class)
//    public ResponseEntity<HttpResponseDTO> handleApiException(ApiException ex) {
//        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
//                HttpResponseDTO.builder()
//                        .timestamp(LocalDateTime.now().toString())
//                        .message(ex.getMessage())
//                        .status(HttpStatus.UNAUTHORIZED)
//                        .statusCode(HttpStatus.UNAUTHORIZED.value())
//                        .build()
//        );
//    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<HttpResponseDTO> handleEntityNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(HttpResponseDTO.builder()
                        .timestamp(LocalDateTime.now().toString())
                        .message(ex.getMessage())
                        .status(HttpStatus.NOT_FOUND)
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .build());
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<HttpResponseDTO> handleApiException(ApiException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(HttpResponseDTO.builder()
                        .timestamp(LocalDateTime.now().toString())
                        .message(ex.getMessage())
                        .status(HttpStatus.BAD_REQUEST)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<HttpResponseDTO> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(HttpResponseDTO.builder()
                        .timestamp(LocalDateTime.now().toString())
                        .message("An unexpected error occurred")
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .build());
    }

}
