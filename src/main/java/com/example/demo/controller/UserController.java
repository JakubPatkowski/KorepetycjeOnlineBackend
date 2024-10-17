package com.example.demo.controller;

import com.example.demo.dto.UserResponseDTO;
import com.example.demo.dto.UserLoginDTO;
import com.example.demo.dto.UserRegisterDTO;
import com.example.demo.dto.HttpResponseDTO;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
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

            return ResponseEntity.ok().body(
                    HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .data(of("user", userDTO))
                            .message("User logged in")
                            .status(HttpStatus.OK)
                            .statusCode(HttpStatus.OK.value())
                            .build());
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<HttpResponseDTO> verifyEmail(@RequestParam String token){
        boolean verified = userService.verifyEmail(token);
        if(verified){
            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("Email verified successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } else {
            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("Invalid or expired verification link")
                    .status(HttpStatus.BAD_REQUEST)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }

    @PostMapping("/change-email/initiate")
    public ResponseEntity<HttpResponseDTO> initiateEmailChange(@RequestBody Map<String, String> body){
        Long userId = Long.valueOf(body.get("userId"));
        boolean initiated = userService.initiateEmailChange(userId);
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

    @PostMapping("/change-email/complete")
    public ResponseEntity<HttpResponseDTO> completeEmailChange(@RequestBody Map<String, String> body){
        String code = body.get("code");
        String newEmail = body.get("newEmail");

        boolean changed = userService.completeEmailChange(code, newEmail);

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
    }

    @PostMapping("/change-password/initiate")
    public ResponseEntity<HttpResponseDTO> initiatePasswordChange(@RequestBody Map<String, String> body) {
        Long userId = Long.valueOf(body.get("userId"));
        boolean initiated = userService.initiatePasswordChange(userId);
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
    }

    @PostMapping("/change-password/complete")
    public ResponseEntity<HttpResponseDTO> completePasswordChange(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String newPassword = body.get("newPassword");

        boolean changed = userService.completePasswordChange(code, newPassword);

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
    }

//    @PutMapping("/change-email")
//    public ResponseEntity<HttpResponseDTO> updateUserEmail(@Valid @RequestBody Map<String, String> body, BindingResult result) {
//        if (result.hasErrors()) {
//            Map<String, List<String>> errors = new HashMap<>();
//            result.getFieldErrors().forEach(error -> {
//                String fieldName = error.getField();
//                String errorMessage = error.getDefaultMessage();
//                errors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(errorMessage);
//            });
//
//            return ResponseEntity.badRequest().body(
//                    HttpResponseDTO.builder()
//                            .timestamp(now().toString())
//                            .message("Invalid data")
//                            .status(HttpStatus.BAD_REQUEST)
//                            .statusCode(HttpStatus.BAD_REQUEST.value())
//                            .data(of("errors", errors))
//                            .build());
//        } else {
//            boolean isEmailUpdated = userService.changeEmail(Long.valueOf(body.get("id")), body.get("email"));
//            String message = isEmailUpdated ? "User updated successfully" : "User email update failed";
//            return ResponseEntity.ok().body(
//                    HttpResponseDTO.builder()
//                            .timestamp(now().toString())
//                            .message(message)
//                            .status(HttpStatus.OK)
//                            .statusCode(HttpStatus.OK.value())
//                            .build());
//        }
//    }

    private URI getUri() {
        return URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/user/get/<userId>").toUriString());
    }
}


