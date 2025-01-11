package com.example.ekorki.controller;

import com.example.ekorki.dto.user.*;
import com.example.ekorki.dto.http.HttpResponseDTO;
import com.example.ekorki.exception.ApiException;
import com.example.ekorki.model.UserPrincipals;
import com.example.ekorki.service.entity.RefreshTokenService;
import com.example.ekorki.service.entity.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.core.io.Resource;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static java.util.Map.of;

import static java.time.LocalDateTime.now;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping(path = "/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    private final RefreshTokenService refreshTokenService;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Value("classpath:templates/verification-success.html")
    private Resource successTemplate;

    @Value("classpath:templates/verification-error.html")
    private Resource errorTemplate;

    private String getVerificationSuccessHtml() throws IOException {
        return new String(successTemplate.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private String getVerificationErrorHtml() throws IOException {
        return new String(errorTemplate.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    @PostMapping("/register")
    public ResponseEntity<HttpResponseDTO> saveUser(@Valid @RequestBody UserRegisterDTO userRegisterDTO) {
        logger.debug("Attempting to register new user with email: {}", userRegisterDTO.email());
        try {
            boolean isRegistered = userService.registerUser(userRegisterDTO);
            if (isRegistered) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(HttpResponseDTO.builder()
                                .timestamp(LocalDateTime.now().toString())
                                .message("User registered successfully")
                                .status(HttpStatus.CREATED)
                                .statusCode(HttpStatus.CREATED.value())
                                .build());
            } else {
                throw new ApiException("Failed to register user");
            }
        } catch (ApiException e) {
            throw e; // Let GlobalExceptionHandler handle it
        } catch (Exception e) {
            logger.error("Unexpected error during user registration", e);
            throw new ApiException("An unexpected error occurred during registration");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<HttpResponseDTO> loginUser(
            @Valid @RequestBody UserLoginDTO userLoginDTO,
            HttpServletRequest request) {
        logger.debug("Login attempt for user: {}", userLoginDTO.email());
        try {
            String clientIp = request.getRemoteAddr();
            UserResponseDTO userDTO = userService.login(userLoginDTO, clientIp);

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(LocalDateTime.now().toString())
                    .data(Map.of("user", userDTO))
                    .message("Login successful")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during login", e);
            throw new ApiException("An unexpected error occurred during login");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<HttpResponseDTO> logout(
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Long loggedInUserId = ((UserPrincipals) userDetails).getId();
            String refreshToken = body.get("refreshToken");
            String clientIp = request.getRemoteAddr();

            if (refreshToken == null) {
                return ResponseEntity.badRequest().body(
                        HttpResponseDTO.builder()
                                .timestamp(now().toString())
                                .message("Refresh token is required")
                                .status(HttpStatus.BAD_REQUEST)
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .build());
            }

            refreshTokenService.logout(refreshToken, clientIp, loggedInUserId);

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("Successfully logged out")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("An error occurred during logout: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) throws IOException {
        try {
            boolean verified = userService.verifyEmail(token);
            String htmlSuccess = getVerificationSuccessHtml();
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(htmlSuccess);
        } catch (ApiException e) {
            String htmlError = getVerificationErrorHtml();
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(htmlError);
        } catch (IOException e) {
            throw new ApiException("error" + e);
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

    @PostMapping("/upgrade-to-teacher")
    public ResponseEntity<HttpResponseDTO> upgradeToTeacher(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long loggedInUserId = ((UserPrincipals) userDetails).getId();

        try {
            boolean upgraded = userService.upgradeToTeacher(loggedInUserId);
            if (upgraded) {
                return ResponseEntity.ok(HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Successfully upgraded to Teacher role")
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .build());
            } else {
                return ResponseEntity.badRequest().body(HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Failed to upgrade to Teacher role")
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
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("An error occurred while upgrading to teacher: " + e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }
    }



    private URI getUri() {
        return URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/user/get/<userId>").toUriString());
    }
}


