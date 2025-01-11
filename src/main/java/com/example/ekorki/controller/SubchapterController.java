package com.example.ekorki.controller;

import com.example.ekorki.dto.http.HttpResponseDTO;
import com.example.ekorki.dto.subchapter.SubchapterDetailsDTO;
import com.example.ekorki.exception.ApiException;
import com.example.ekorki.model.UserPrincipals;
import com.example.ekorki.service.entity.SubchapterService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static java.time.LocalDateTime.now;

@RestController
@RequestMapping(path = "/subchapter")
@RequiredArgsConstructor
public class SubchapterController {
    private final SubchapterService subchapterService;

    @GetMapping("/get/{subchapterId}")
    public ResponseEntity<HttpResponseDTO> getSubchapterDetails(
            @PathVariable Long subchapterId,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long loggedInUserId = ((UserPrincipals) userDetails).getId();

        try {
            SubchapterDetailsDTO subchapterDetails = subchapterService.getSubchapterDetails(subchapterId, loggedInUserId);
            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .data(Map.of("subchapter", subchapterDetails))
                    .message("Subchapter details retrieved successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (ApiException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message(e.getMessage())
                    .status(HttpStatus.FORBIDDEN)
                    .statusCode(HttpStatus.FORBIDDEN.value())
                    .build());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("Subchapter not found")
                    .status(HttpStatus.NOT_FOUND)
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("An error occurred while retrieving subchapter details: " + e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }
    }
}
