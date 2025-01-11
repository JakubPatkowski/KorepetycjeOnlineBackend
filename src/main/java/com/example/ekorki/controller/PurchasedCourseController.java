package com.example.ekorki.controller;

import com.example.ekorki.dto.http.HttpResponseDTO;
import com.example.ekorki.exception.ApiException;
import com.example.ekorki.model.UserPrincipals;
import com.example.ekorki.service.entity.PurchasedCourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.example.ekorki.dto.courseShop.CourseShopResponseDTO;

import java.util.Map;

import static java.time.LocalDate.now;

@RestController
@RequestMapping(path = "/course")
@RequiredArgsConstructor
public class PurchasedCourseController {
    private final PurchasedCourseService purchasedCourseService;

    @PostMapping("/buy")
    public ResponseEntity<HttpResponseDTO> purchaseCourse(
            @RequestBody Map<String, Long> body,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long loggedInUserId = ((UserPrincipals) userDetails).getId();
        Long courseId = body.get("courseId");

        try {
            boolean purchased = purchasedCourseService.purchaseCourse(courseId, loggedInUserId);
            if (purchased) {
                return ResponseEntity.ok(HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Course purchased successfully")
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .build());
            } else {
                return ResponseEntity.badRequest().body(HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Failed to purchase course")
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

    @GetMapping("/get-purchased")
    public ResponseEntity<HttpResponseDTO> getPurchasedCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long loggedInUserId = ((UserPrincipals) userDetails).getId();

        try {
            Page<CourseShopResponseDTO> purchasedCourses = purchasedCourseService.getPurchasedCourses(loggedInUserId, page, size);
            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .data(Map.of(
                            "courses", purchasedCourses.getContent(),
                            "currentPage", purchasedCourses.getNumber(),
                            "totalItems", purchasedCourses.getTotalElements(),
                            "totalPages", purchasedCourses.getTotalPages()
                    ))
                    .message("Retrieved purchased courses")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("An error occurred: " + e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }
    }

    @GetMapping("/can-review-teacher/{teacherId}")
    public ResponseEntity<HttpResponseDTO> canReviewTeacher(
            @PathVariable Long teacherId,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long loggedInUserId = ((UserPrincipals) userDetails).getId();

        try {
            boolean canReview = purchasedCourseService.canReviewTeacher(loggedInUserId, teacherId);

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .data(Map.of("canReview", canReview))
                    .message("Permission check completed")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (ApiException e) {
            return ResponseEntity.badRequest().body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message(e.getMessage())
                    .status(HttpStatus.BAD_REQUEST)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("An error occurred: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
        }
    }
}
