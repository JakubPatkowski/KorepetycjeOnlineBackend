package com.example.ekorki.controller;

import com.example.ekorki.dto.token.AccessTokenRequestDTO;
import com.example.ekorki.exception.ApiException;
import com.example.ekorki.dto.http.HttpResponseDTO;
import com.example.ekorki.service.entity.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.time.LocalDate.now;
import static java.util.Map.of;

@RestController
@RequestMapping(path = "/user")
public class RefreshTokenController {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @PostMapping("/access-token")
    public ResponseEntity<HttpResponseDTO> generateNewAccessToken(
        @Valid @RequestBody AccessTokenRequestDTO requestDTO,
        BindingResult result,
        HttpServletRequest request)
    {
        if (result.hasErrors()) {
            Map<String, List<String>> errors = new HashMap<>();

            result.getFieldErrors().forEach(error -> {
                String fieldName = error.getField();
                String errorMessage = error.getDefaultMessage();
                errors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(errorMessage);
            });

            return ResponseEntity.badRequest().body(
                    HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("Invalid data")
                            .status(HttpStatus.BAD_REQUEST)
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .data(of("errors", errors))
                            .build());
        }

        try {
            String clientIp = request.getRemoteAddr();
            String newAccessToken = refreshTokenService.generateNewAccessToken(requestDTO.token(), clientIp);

            return ResponseEntity.ok().body(
                    HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .data(of("accessToken", newAccessToken))
                            .message("New access token generated")
                            .status(HttpStatus.OK)
                            .statusCode(HttpStatus.OK.value())
                            .build());
        } catch (ApiException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message(e.getMessage())
                            .status(HttpStatus.UNAUTHORIZED)
                            .statusCode(HttpStatus.UNAUTHORIZED.value())
                            .build());
        }
    }
}
