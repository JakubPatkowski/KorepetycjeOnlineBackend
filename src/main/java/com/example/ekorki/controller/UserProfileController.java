package com.example.ekorki.controller;

import com.example.ekorki.dto.http.HttpResponseDTO;

import com.example.ekorki.dto.userProfile.UserProfileResponseDTO;
import com.example.ekorki.dto.userProfile.UserProfileUpdateDTO;
import com.example.ekorki.model.UserPrincipals;
import com.example.ekorki.service.entity.TeacherProfileService;
import com.example.ekorki.service.entity.UserProfileService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.time.LocalDateTime.now;

@RestController
@RequestMapping(path = "/user-profile")
@RequiredArgsConstructor
public class UserProfileController {
    private final UserProfileService userProfileService;
    private final TeacherProfileService teacherProfileService;

    @PutMapping("/update")
    public ResponseEntity<HttpResponseDTO> updateUserProfile(
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "picture", required = false) MultipartFile picture,
            @RequestParam(value = "badgesVisible", required = false) Boolean badgesVisible,
            Authentication authentication
    ) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long loggedInUserId = ((UserPrincipals) userDetails).getId();

        try {

            UserProfileUpdateDTO updateDTO = new UserProfileUpdateDTO(
                    fullName != null ? Optional.of(fullName) : Optional.empty(),
                    description != null ? Optional.of(description) : Optional.empty(),
                    badgesVisible != null ? Optional.of(badgesVisible) : Optional.empty()
            );
            userProfileService.updateUserProfile(updateDTO, picture, loggedInUserId);
            return ResponseEntity.ok().body(
                    HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("User profile updated successfully")
                            .status(HttpStatus.OK)
                            .statusCode(HttpStatus.OK.value())
                            .build()
            );
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("User profile not found")
                            .status(HttpStatus.NOT_FOUND)
                            .statusCode(HttpStatus.NOT_FOUND.value())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("An error occurred while updating the user profile. Eroor: " + e)
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build()
            );
        }
    }

    @GetMapping("/get-logged-in")
    public ResponseEntity<HttpResponseDTO> getLoggedInUserProfile(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long loggedInUserId = ((UserPrincipals) userDetails).getId();
        try {
            UserProfileResponseDTO profileDTO = userProfileService.getUserProfile(loggedInUserId);

            return ResponseEntity.ok().body(
                    HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("User profile found")
                            .data(Map.of("UserProfile", profileDTO))
                            .status(HttpStatus.OK)
                            .statusCode(HttpStatus.OK.value())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("An error occurred while getting the user profile. Error: " + e)
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build()
            );
        }
    }

    @GetMapping("/get/{userId}")
    public ResponseEntity<HttpResponseDTO> getUserProfile(@PathVariable Long userId) {
        try {
            UserProfileResponseDTO profileDTO = userProfileService.getUserProfile(userId);

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .data(Map.of("profile", profileDTO))
                    .message("Profile retrieved successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("Profile not found")
                            .status(HttpStatus.NOT_FOUND)
                            .statusCode(HttpStatus.NOT_FOUND.value())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("An error occurred while retrieving the profile: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build()
            );
        }
    }

    @GetMapping("/get-best")
    public ResponseEntity<HttpResponseDTO> getBestTeachers(Authentication authentication) {
        try {
            Long loggedInUserId = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipals) {
                loggedInUserId = ((UserPrincipals) authentication.getPrincipal()).getId();
            }

            List<UserProfileResponseDTO> bestTeachers = teacherProfileService.getBestTeachers(loggedInUserId);

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .data(Map.of("teachers", bestTeachers))
                    .message("Best teachers retrieved successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("An error occurred while retrieving best teachers: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
        }
    }
}
