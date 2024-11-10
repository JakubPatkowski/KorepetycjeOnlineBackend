package com.example.demo.controller;

import com.example.demo.dto.user.*;
import com.example.demo.dto.http.HttpResponseDTO;
import com.example.demo.entity.UserEntity;
import com.example.demo.exception.ApiException;
import com.example.demo.model.UserPrincipals;
import com.example.demo.service.entity.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Map.of;

import static java.time.LocalDateTime.now;
import org.slf4j.LoggerFactory;


@RestController
@RequestMapping(path = "/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

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
    public Object verifyEmail(@RequestParam String token,
                              @RequestHeader(value = "Accept", defaultValue = "") String accept) {
        boolean verified = userService.verifyEmail(token);

        // Jeśli klient żąda HTML
        if (accept.contains("text/html")) {
            return new ModelAndView("email-verification"); // nazwa szablonu HTML
        }

        // Domyślna odpowiedź JSON
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
            logger.warn("Access denied "+e.getMessage());

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message(e.getMessage())
                    .status(HttpStatus.FORBIDDEN)
                    .statusCode(HttpStatus.FORBIDDEN.value())
                    .build());
        } catch (Exception e) {
            logger.warn("Error occurred "+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("An error occurred while initiating email change: " + e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }


    }

    @PostMapping("/change-email/complete")
    public ResponseEntity<HttpResponseDTO> completeEmailChange(@Valid @RequestBody EmailChangeDTO emailChangeDTO, Authentication authentication){
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long loggedInUserId = ((UserPrincipals) userDetails).getId();

        try {
            boolean changed = userService.completeEmailChange(emailChangeDTO.code(), emailChangeDTO.newEmail(), loggedInUserId);

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
            logger.warn("Access Denied "+e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message(e.getMessage())
                    .status(HttpStatus.FORBIDDEN)
                    .statusCode(HttpStatus.FORBIDDEN.value())
                    .build());
        } catch (Exception e) {
            logger.warn("Error occurred"+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("An error occurred while completing email change: " + e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }

    }

    @PostMapping("/change-password/initiate")
    public ResponseEntity<HttpResponseDTO> initiatePasswordChange(Authentication authentication) {
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
            logger.warn("Access Denied "+e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message(e.getMessage())
                    .status(HttpStatus.FORBIDDEN)
                    .statusCode(HttpStatus.FORBIDDEN.value())
                    .build());
        } catch (Exception e) {
            logger.warn("Error occurred"+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("An error occurred while initiating password change: " + e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }

    }

    @PostMapping("/change-password/complete")
    public ResponseEntity<HttpResponseDTO> completePasswordChange(@Valid @RequestBody PasswordChangeDTO passwordChangeDTO, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long loggedInUserId = ((UserPrincipals) userDetails).getId();

        try{
            boolean changed = userService.completePasswordChange(passwordChangeDTO.code(), passwordChangeDTO.newPassword(), loggedInUserId);

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
            logger.warn("Access Denied "+e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message(e.getMessage())
                    .status(HttpStatus.FORBIDDEN)
                    .statusCode(HttpStatus.FORBIDDEN.value())
                    .build());
        } catch (Exception e) {
            logger.warn("Error occurred"+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("An error occurred while completing password change: " + e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }

    }

    @GetMapping("/get")
    public ResponseEntity<HttpResponseDTO> getLoggedInUser(Authentication authentication){
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long loggedInUserId = ((UserPrincipals) userDetails).getId();


        try{
            UserResponseDTO userResponseDTO = userService.getUserEntity(loggedInUserId);
            if(userResponseDTO != null) {
                return ResponseEntity.ok().body(
                        HttpResponseDTO.builder()
                                .timestamp(now().toString())
                                .data(of("user", userResponseDTO))
                                .message("User data")
                                .status(HttpStatus.OK)
                                .statusCode(HttpStatus.OK.value())
                                .build());
            }
            else {
                return ResponseEntity.ok().body(
                        HttpResponseDTO.builder()
                                .timestamp(now().toString())
                                .message("Failed to get user")
                                .status(HttpStatus.OK)
                                .statusCode(HttpStatus.OK.value())
                                .build());
            }
        } catch (EntityNotFoundException e) {
            logger.warn("User profile not found "+e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("User profile not found")
                            .status(HttpStatus.NOT_FOUND)
                            .statusCode(HttpStatus.NOT_FOUND.value())
                            .build()
            );
        } catch (Exception e) {
            logger.warn("Error occurred"+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("An error occurred while getting the user profile. Eroor: " + e)
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build()
            );
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<HttpResponseDTO> resendVerificationEmail(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long loggedInUserId = ((UserPrincipals) userDetails).getId();

        try {
            boolean sent = userService.resendVerificationEmail(loggedInUserId);
            if (sent) {
                return ResponseEntity.ok(HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Verification email has been resent")
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .build());
            } else {
                return ResponseEntity.badRequest().body(HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Failed to resend verification email")
                        .status(HttpStatus.BAD_REQUEST)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .build());
            }
        } catch (ApiException e) {
            return ResponseEntity.badRequest().body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message(e.getMessage())
                    .status(HttpStatus.BAD_REQUEST)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }

    private URI getUri() {
        return URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/user/get/<userId>").toUriString());
    }
}


