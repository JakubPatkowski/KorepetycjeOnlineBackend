package com.example.demo.controller;

import com.example.demo.dto.http.HttpResponseDTO;
import com.example.demo.dto.review.ReviewCreateDTO;
import com.example.demo.dto.review.ReviewResponseDTO;
import com.example.demo.dto.review.ReviewTargetType;
import com.example.demo.exception.ApiException;
import com.example.demo.service.entity.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import com.example.demo.model.UserPrincipals;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;

import static java.time.LocalDate.now;

@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping("/add/course/{courseId}")
    public ResponseEntity<HttpResponseDTO> addCourseReview(
            @PathVariable Long courseId,
            @RequestBody @Valid ReviewCreateDTO reviewDTO,
            Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Long loggedInUserId = ((UserPrincipals) userDetails).getId();

            reviewService.addCourseReview(courseId, loggedInUserId, reviewDTO);

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("Review added successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message(e.getMessage())
                    .status(HttpStatus.BAD_REQUEST)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }

    @PostMapping("/add/chapter/{chapterId}")
    public ResponseEntity<HttpResponseDTO> addChapterReview(
            @PathVariable Long chapterId,
            @RequestBody @Valid ReviewCreateDTO reviewDTO,
            Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Long loggedInUserId = ((UserPrincipals) userDetails).getId();

            reviewService.addChapterReview(chapterId, loggedInUserId, reviewDTO);

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("Review added successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message(e.getMessage())
                    .status(HttpStatus.BAD_REQUEST)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }

    @GetMapping("/get/course/{courseId}")
    public ResponseEntity<HttpResponseDTO> getCourseReviews(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {

        try {
            // Pobierz ID zalogowanego użytkownika (jeśli jest zalogowany)
            Long loggedInUserId = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipals) {
                loggedInUserId = ((UserPrincipals) authentication.getPrincipal()).getId();
            }

            Page<ReviewResponseDTO> reviewsPage = reviewService.getReviews(
                    courseId,
                    ReviewTargetType.COURSE,
                    page,
                    size,
                    sortBy,
                    sortDir,
                    loggedInUserId
            );

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .data(Map.of(
                            "reviews", reviewsPage.getContent(),
                            "currentPage", reviewsPage.getNumber(),
                            "totalItems", reviewsPage.getTotalElements(),
                            "totalPages", reviewsPage.getTotalPages()
                    ))
                    .message("Reviews retrieved successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("Error retrieving reviews: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
        }
    }

    @GetMapping("/get/chapter/{chapterId}")
    public ResponseEntity<HttpResponseDTO> getChapterReviews(
            @PathVariable Long chapterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {

        try {
            // Pobierz ID zalogowanego użytkownika (jeśli jest zalogowany)
            Long loggedInUserId = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipals) {
                loggedInUserId = ((UserPrincipals) authentication.getPrincipal()).getId();
            }

            Page<ReviewResponseDTO> reviewsPage = reviewService.getReviews(
                    chapterId,
                    ReviewTargetType.CHAPTER,
                    page,
                    size,
                    sortBy,
                    sortDir,
                    loggedInUserId
            );

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .data(Map.of(
                            "reviews", reviewsPage.getContent(),
                            "currentPage", reviewsPage.getNumber(),
                            "totalItems", reviewsPage.getTotalElements(),
                            "totalPages", reviewsPage.getTotalPages()
                    ))
                    .message("Reviews retrieved successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("Error retrieving reviews: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
        }
    }

    @PutMapping("/update/{reviewId}")
    public ResponseEntity<HttpResponseDTO> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewCreateDTO updateDTO,
            Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Long loggedInUserId = ((UserPrincipals) userDetails).getId();

            reviewService.updateReview(reviewId, loggedInUserId, updateDTO);

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("Review updated successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (ApiException e) {
            return ResponseEntity.badRequest()
                    .body(HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message(e.getMessage())
                            .status(HttpStatus.BAD_REQUEST)
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("Error updating review: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
        }
    }

    @DeleteMapping("/delete/{reviewId}")
    public ResponseEntity<HttpResponseDTO> deleteReview(
            @PathVariable Long reviewId,
            Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Long loggedInUserId = ((UserPrincipals) userDetails).getId();

            reviewService.deleteReview(reviewId, loggedInUserId);

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("Review deleted successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (ApiException e) {
            return ResponseEntity.badRequest()
                    .body(HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message(e.getMessage())
                            .status(HttpStatus.BAD_REQUEST)
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("Error deleting review: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
        }
    }

    @GetMapping("/user/course/{courseId}")
    public ResponseEntity<HttpResponseDTO> getUserCourseReview(
            @PathVariable Long courseId,
            Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Long loggedInUserId = ((UserPrincipals) userDetails).getId();

            ReviewResponseDTO review = reviewService.getUserReview(courseId, ReviewTargetType.COURSE, loggedInUserId);

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .data(Map.of("review", review))
                    .message("User review retrieved successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (ApiException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message(e.getMessage())
                            .status(HttpStatus.NOT_FOUND)
                            .statusCode(HttpStatus.NOT_FOUND.value())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("Error retrieving review: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
        }
    }

    @GetMapping("/user/chapter/{chapterId}")
    public ResponseEntity<HttpResponseDTO> getUserChapterReview(
            @PathVariable Long chapterId,
            Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Long loggedInUserId = ((UserPrincipals) userDetails).getId();

            ReviewResponseDTO review = reviewService.getUserReview(chapterId, ReviewTargetType.CHAPTER, loggedInUserId);

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .data(Map.of("review", review))
                    .message("User review retrieved successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (ApiException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message(e.getMessage())
                            .status(HttpStatus.NOT_FOUND)
                            .statusCode(HttpStatus.NOT_FOUND.value())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("Error retrieving review: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
        }
    }

    @PostMapping("/add/teacher/{teacherId}")
    public ResponseEntity<HttpResponseDTO> addTeacherReview(
            @PathVariable Long teacherId,
            @RequestBody @Valid ReviewCreateDTO reviewDTO,
            Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Long loggedInUserId = ((UserPrincipals) userDetails).getId();

            reviewService.addTeacherReview(teacherId, loggedInUserId, reviewDTO);

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("Review added successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message(e.getMessage())
                    .status(HttpStatus.BAD_REQUEST)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }

    @GetMapping("/get/teacher/{teacherId}")
    public ResponseEntity<HttpResponseDTO> getTeacherReviews(
            @PathVariable Long teacherId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {
        try {
            Long loggedInUserId = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipals) {
                loggedInUserId = ((UserPrincipals) authentication.getPrincipal()).getId();
            }

            Page<ReviewResponseDTO> reviewsPage = reviewService.getReviews(
                    teacherId,
                    ReviewTargetType.TEACHER,
                    page,
                    size,
                    sortBy,
                    sortDir,
                    loggedInUserId
            );

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .data(Map.of(
                            "reviews", reviewsPage.getContent(),
                            "currentPage", reviewsPage.getNumber(),
                            "totalItems", reviewsPage.getTotalElements(),
                            "totalPages", reviewsPage.getTotalPages()
                    ))
                    .message("Reviews retrieved successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("Error retrieving reviews: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
        }
    }

    @GetMapping("/user/teacher/{teacherId}")
    public ResponseEntity<HttpResponseDTO> getUserTeacherReview(
            @PathVariable Long teacherId,
            Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Long loggedInUserId = ((UserPrincipals) userDetails).getId();

            ReviewResponseDTO review = reviewService.getUserReview(teacherId, ReviewTargetType.TEACHER, loggedInUserId);

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .data(Map.of("review", review))
                    .message("User review retrieved successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (ApiException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message(e.getMessage())
                            .status(HttpStatus.NOT_FOUND)
                            .statusCode(HttpStatus.NOT_FOUND.value())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("Error retrieving review: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
        }
    }
}
