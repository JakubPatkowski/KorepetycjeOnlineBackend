package com.example.ekorki.exception;

import com.example.ekorki.dto.http.HttpResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

//    @ExceptionHandler(EntityNotFoundException.class)
//    public ResponseEntity<HttpResponseDTO> handleEntityNotFound(EntityNotFoundException ex) {
//        return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                .body(HttpResponseDTO.builder()
//                        .timestamp(LocalDateTime.now().toString())
//                        .message(ex.getMessage())
//                        .status(HttpStatus.NOT_FOUND)
//                        .statusCode(HttpStatus.NOT_FOUND.value())
//                        .build());
//    }
//
//    @ExceptionHandler(ApiException.class)
//    public ResponseEntity<HttpResponseDTO> handleApiException(ApiException ex) {
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                .body(HttpResponseDTO.builder()
//                        .timestamp(LocalDateTime.now().toString())
//                        .message(ex.getMessage())
//                        .status(HttpStatus.BAD_REQUEST)
//                        .statusCode(HttpStatus.BAD_REQUEST.value())
//                        .build());
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<HttpResponseDTO> handleGenericException(Exception ex) {
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(HttpResponseDTO.builder()
//                        .timestamp(LocalDateTime.now().toString())
//                        .message("An unexpected error occurred")
//                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
//                        .build());
//    }

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<HttpResponseDTO> handleApiException(ApiException ex) {
        logger.error("API Exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<HttpResponseDTO> handleEntityNotFound(EntityNotFoundException ex) {
        logger.error("Entity not found: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<HttpResponseDTO> handleAccessDenied(AccessDeniedException ex) {
        logger.error("Access denied: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.FORBIDDEN, "Access denied: " + ex.getMessage(), null);
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<HttpResponseDTO> handleAuthenticationException(Exception ex) {
        logger.error("Authentication error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.UNAUTHORIZED, "Authentication failed: " + ex.getMessage(), null);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<HttpResponseDTO> handleValidationExceptions(Exception ex) {
        Map<String, Object> errors = new HashMap<>();
        List<FieldError> fieldErrors;

        if (ex instanceof MethodArgumentNotValidException validationEx) {
            fieldErrors = validationEx.getBindingResult().getFieldErrors();
        } else {
            BindException bindEx = (BindException) ex;
            fieldErrors = bindEx.getFieldErrors();
        }

        Map<String, List<String>> errorMap = fieldErrors.stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        HashMap::new,
                        Collectors.mapping(
                                FieldError::getDefaultMessage,
                                Collectors.toList()
                        )
                ));

        errors.put("validationErrors", errorMap);

        logger.error("Validation error: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(HttpResponseDTO.builder()
                        .timestamp(LocalDateTime.now().toString())
                        .message("Validation failed")
                        .status(HttpStatus.BAD_REQUEST)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .data(errors)
                        .build());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<HttpResponseDTO> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        logger.error("File size exceeded: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.BAD_REQUEST, "File size exceeds the maximum allowed limit", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<HttpResponseDTO> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        String message = determineUserFriendlyMessage(ex);

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, message, null);
    }

    private String determineUserFriendlyMessage(Exception ex) {
        // W środowisku deweloperskim zwracamy pełny komunikat błędu
        if (isDevelopmentEnvironment()) {
            return "Internal server error: " + ex.getMessage();
        }
        // W produkcji zwracamy ogólny komunikat
        return "An unexpected error occurred. Please try again later.";
    }

    private boolean isDevelopmentEnvironment() {
        // Można to zastąpić właściwą konfiguracją ze Spring profiles
        return true; // dla celów developerskich
    }

    private ResponseEntity<HttpResponseDTO> buildResponse(HttpStatus status, String message, Map<String, Object> errors) {
        HttpResponseDTO.HttpResponseDTOBuilder response = HttpResponseDTO.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(status)
                .statusCode(status.value())
                .message(message);

        if (errors != null) {
            response.data(Map.of("errors", errors));
        }

        return ResponseEntity.status(status).body(response.build());
    }

}
