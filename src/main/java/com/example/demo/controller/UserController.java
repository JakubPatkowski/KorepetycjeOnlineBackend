package com.example.demo.controller;

import com.example.demo.dto.UserResponseDTO;
import com.example.demo.dto.UserLoginDTO;
import com.example.demo.dto.UserRegisterDTO;
import com.example.demo.dto.HttpResponseDTO;
import com.example.demo.entity.UserEntity;
import com.example.demo.model.UserPrincipals;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Map.of;

import static java.time.LocalDateTime.now;

@RestController
@RequestMapping(path = "/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<HttpResponseDTO> saveUser(@Valid @RequestBody UserRegisterDTO userRegisterDTO, BindingResult result){
        if(result.hasErrors()){

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
                            .data(of("errors",errors))
                            .build());
        } else {
            boolean isRegistered = userService.registerUser(userRegisterDTO);
            if (isRegistered) {
                return ResponseEntity.created(getUri()).body(
                        HttpResponseDTO.builder()
                                .timestamp(now().toString())
                                .message("User created")
                                .status(HttpStatus.CREATED)
                                .statusCode(HttpStatus.CREATED.value())
                                .build()
                );
            } else {
                return ResponseEntity.badRequest().body(
                        HttpResponseDTO.builder()
                                .timestamp(now().toString())
                                .message("User not created")
                                .status(HttpStatus.BAD_REQUEST)
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .build()
                );
            }
        }


    }

    @PostMapping("/login")
    public ResponseEntity<HttpResponseDTO> loginUser(@Valid @RequestBody UserLoginDTO userLoginDTO, BindingResult result, HttpServletRequest request) {
        if(result.hasErrors()){
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
                            .data(of("errors",errors))
                            .build());
        } else {
            String clientIp = request.getRemoteAddr();

            UserResponseDTO userDTO = userService.login(userLoginDTO, clientIp);
            if(userDTO != null) {
                return ResponseEntity.ok().body(
                        HttpResponseDTO.builder()
                                .timestamp(now().toString())
                                .data(of("user", userDTO))
                                .message("User logged in")
                                .status(HttpStatus.OK)
                                .statusCode(HttpStatus.OK.value())
                                .build());
            }
            else {
                return ResponseEntity.badRequest().body(
                        HttpResponseDTO.builder()
                                .timestamp(now().toString())
                                .message("Failed to log in")
                                .status(HttpStatus.BAD_REQUEST)
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .build());
            }
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<HttpResponseDTO> verifyEmail(@RequestParam String token){
        boolean verified = userService.verifyEmail(token);
        if(verified){
            return ResponseEntity.ok(
                    HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Email verified successfully")
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .build());
        } else {
            return ResponseEntity.badRequest().body(
                    HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Invalid or expired verification link")
                        .status(HttpStatus.BAD_REQUEST)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .build());
        }
    }

    @PostMapping("/change-email/initiate")
    public ResponseEntity<HttpResponseDTO> initiateEmailChange(Authentication authentication){
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long loggedInUserId = ((UserPrincipals) userDetails).getId();
        try{
            boolean initiated = userService.initiateEmailChange(loggedInUserId);
            if(initiated){
                return ResponseEntity.ok(HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Verification code sent to current email")
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .build());
            } else {
                return ResponseEntity.ok(HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Failed to initiate email change")
                        .status(HttpStatus.BAD_REQUEST)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .build());
            }
        }
        catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message(e.getMessage())
                    .status(HttpStatus.FORBIDDEN)
                    .statusCode(HttpStatus.FORBIDDEN.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("An error occurred while initiating email change: " + e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }


    }

    @PostMapping("/change-email/complete")
    public ResponseEntity<HttpResponseDTO> completeEmailChange(@RequestBody Map<String, String> body, Authentication authentication){
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long loggedInUserId = ((UserPrincipals) userDetails).getId();

        String code = body.get("code");
        String newEmail = body.get("newEmail");
        try {
            boolean changed = userService.completeEmailChange(code, newEmail, loggedInUserId);

            if(changed){
                return ResponseEntity.ok(HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Email changed successfully. Please check your new email to verify your account.")
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .build());
            } else {
                return ResponseEntity.ok(HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Invalid or expired code")
                        .status(HttpStatus.BAD_REQUEST)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .build());
            }
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message(e.getMessage())
                    .status(HttpStatus.FORBIDDEN)
                    .statusCode(HttpStatus.FORBIDDEN.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("An error occurred while completing email change: " + e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }

    }

    @PostMapping("/change-password/initiate")
    public ResponseEntity<HttpResponseDTO> initiatePasswordChange(@RequestBody Map<String, String> body, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long loggedInUserId = ((UserPrincipals) userDetails).getId();
        try {
            boolean initiated = userService.initiatePasswordChange(loggedInUserId);
            if (initiated) {
                return ResponseEntity.ok(HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Verification code sent to your email")
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .build());
            } else {
                return ResponseEntity.ok(HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Failed to initiate password change")
                        .status(HttpStatus.BAD_REQUEST)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .build());
            }
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message(e.getMessage())
                    .status(HttpStatus.FORBIDDEN)
                    .statusCode(HttpStatus.FORBIDDEN.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("An error occurred while initiating password change: " + e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }

    }

    @PostMapping("/change-password/complete")
    public ResponseEntity<HttpResponseDTO> completePasswordChange(@RequestBody Map<String, String> body, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long loggedInUserId = ((UserPrincipals) userDetails).getId();

        String code = body.get("code");
        String newPassword = body.get("newPassword");

        try{
            boolean changed = userService.completePasswordChange(code, newPassword, loggedInUserId);

            if (changed) {
                return ResponseEntity.ok(HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Password changed successfully.")
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .build());
            } else {
                return ResponseEntity.ok(HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Invalid or expired code")
                        .status(HttpStatus.BAD_REQUEST)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .build());
            }
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message(e.getMessage())
                    .status(HttpStatus.FORBIDDEN)
                    .statusCode(HttpStatus.FORBIDDEN.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("An error occurred while completing password change: " + e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }

    }

    private URI getUri() {
        return URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/user/get/<userId>").toUriString());
    }
}


