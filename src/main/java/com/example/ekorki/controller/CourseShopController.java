package com.example.ekorki.controller;

import com.example.ekorki.dto.courseShop.CourseShopDetailsResponseDTO;
import com.example.ekorki.dto.courseShop.CourseShopResponseDTO;
import com.example.ekorki.dto.http.HttpResponseDTO;
import com.example.ekorki.entity.CourseEntity;
import com.example.ekorki.model.UserPrincipals;
import com.example.ekorki.repository.CourseRepository;
import com.example.ekorki.service.CourseShopService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import static java.util.Map.of;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static java.time.LocalDate.now;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
public class CourseShopController {
    private final CourseShopService shopService;
    private final CourseRepository courseRepository;

    @GetMapping("/get")
    public ResponseEntity<HttpResponseDTO> getCourses(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String tag,
            Authentication authentication) {

        try {
            // Pobierz ID zalogowanego użytkownika (jeśli jest zalogowany)
            Long loggedInUserId = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipals) {
                loggedInUserId = ((UserPrincipals) authentication.getPrincipal()).getId();
            }

            Page<CourseShopResponseDTO> coursesPage = shopService.searchCourses(search, tag, page, size, sortBy, loggedInUserId);

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(LocalDateTime.now().toString())
                    .data(Map.of(
                            "courses", coursesPage.getContent(),
                            "currentPage", coursesPage.getNumber(),
                            "totalItems", coursesPage.getTotalElements(),
                            "totalPages", coursesPage.getTotalPages()
                    ))
                    .message("Courses retrieved successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(HttpResponseDTO.builder()
                            .timestamp(LocalDateTime.now().toString())
                            .message("An error occurred while retrieving courses: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
        }
    }

    @GetMapping("/tags/search")
    public ResponseEntity<HttpResponseDTO> searchTags(
            @RequestParam(required = false) String search) {
        List<String> tags = shopService.searchTags(search);

        return ResponseEntity.ok(HttpResponseDTO.builder()
                .timestamp(now().toString())
                .data(of("tags", tags))
                .message("Tags retrieved successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .build());
    }

    @GetMapping("/get-all")
    public ResponseEntity<HttpResponseDTO> getAllCourses() {
        try {
            List<CourseShopResponseDTO> dtos = shopService.getAll();


            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .data(Map.of("courses", dtos))
                    .message("All courses retrieved successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("An error occurred while retrieving courses: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
        }
    }

    @GetMapping("/get-one/{courseId}")
    @Transactional(readOnly = true)
    public ResponseEntity<HttpResponseDTO> getCourseWithDetails(
            @PathVariable Long courseId,
            Authentication authentication) {
        try {
            // Obsługa ID zalogowanego użytkownika - może być null
            Long loggedInUserId = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipals) {
                loggedInUserId = ((UserPrincipals) authentication.getPrincipal()).getId();
            }

            CourseEntity course = courseRepository.findByIdForDetails(courseId)
                    .orElseThrow(() -> new EntityNotFoundException("Course not found"));

            CourseShopDetailsResponseDTO courseDetails = shopService.getCourseWithDetails(course, loggedInUserId);

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .data(Map.of("courseDetails", courseDetails))
                    .message("Course details retrieved successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("Course not found")
                            .status(HttpStatus.NOT_FOUND)
                            .statusCode(HttpStatus.NOT_FOUND.value())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("An error occurred while retrieving course details: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
        }
    }

    @GetMapping("/get-best")
    public ResponseEntity<HttpResponseDTO> getBestCourses(Authentication authentication) {
        try {
            // Pobierz ID zalogowanego użytkownika (jeśli jest zalogowany)
            Long loggedInUserId = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipals) {
                loggedInUserId = ((UserPrincipals) authentication.getPrincipal()).getId();
            }

            List<CourseShopResponseDTO> bestCourses = shopService.getBestCourses(loggedInUserId);

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(LocalDateTime.now().toString())
                    .data(Map.of("courses", bestCourses))
                    .message("Best courses retrieved successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(HttpResponseDTO.builder()
                            .timestamp(LocalDateTime.now().toString())
                            .message("An error occurred while retrieving best courses: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
        }
    }
}
