package com.example.demo.controller;

import com.example.demo.dto.course.CourseShortDTO;
import com.example.demo.dto.courseShop.CourseShopResponseDTO;
import com.example.demo.dto.http.HttpResponseDTO;
import com.example.demo.service.CourseShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import static java.util.Map.of;

import java.util.List;

import static java.time.LocalDate.now;

@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
public class CourseShopController {
    private final CourseShopService shopService;

    @GetMapping("/get")
    public ResponseEntity<HttpResponseDTO> getCourses(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String tag) {

        try {
            Page<CourseShopResponseDTO> coursesPage = shopService.searchCourses(search, tag, page, size, sortBy);

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .data(of(
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
                            .timestamp(now().toString())
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
}
