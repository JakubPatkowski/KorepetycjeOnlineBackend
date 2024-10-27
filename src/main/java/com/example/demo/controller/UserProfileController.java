package com.example.demo.controller;

import com.example.demo.dto.HttpResponseDTO;
import com.example.demo.dto.UserProfileUpdateDTO;
import com.example.demo.model.UserPrincipals;
import com.example.demo.service.entity.UserProfileService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import static java.time.LocalDateTime.now;

@RestController
@RequestMapping(path = "/user-profile")
@RequiredArgsConstructor
public class UserProfileController {
    private final UserProfileService userProfileService;

    @PutMapping("/update")
    public ResponseEntity<HttpResponseDTO> updateUserProfile(
            @Valid @RequestBody UserProfileUpdateDTO updateDTO,
            Authentication authentication
    ) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long loggedInUserId = ((UserPrincipals) userDetails).getId();

        try {
            userProfileService.updateUserProfile(updateDTO, loggedInUserId);
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
}
