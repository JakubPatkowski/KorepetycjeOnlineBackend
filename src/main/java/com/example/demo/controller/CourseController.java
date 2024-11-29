package com.example.demo.controller;

import com.example.demo.dto.course.CourseCreateDTO;
import com.example.demo.dto.http.HttpResponseDTO;
import com.example.demo.dto.course.CourseInfoDTO;
import com.example.demo.dto.course.CourseUpdateDTO;
import com.example.demo.exception.ApiException;
import com.example.demo.model.UserPrincipals;
import com.example.demo.service.entity.CourseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.demo.dto.courseShop.CourseDataDTO;


import java.util.List;
import java.util.Map;
import static java.util.Map.of;


import static java.time.LocalDateTime.now;

@RestController
@RequestMapping(path = "/course")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HttpResponseDTO> createCourse(
            @RequestPart(value = "courseData") @Valid String courseDataJson,
            @RequestPart(value = "banner", required = false) MultipartFile bannerFile,
            @RequestPart(value = "contentFiles", required = false) MultipartFile[] contentFiles,
            Authentication authentication) {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long loggedInUserId = ((UserPrincipals) userDetails).getId();

        try{
            ObjectMapper mapper = new ObjectMapper();
            boolean isCreated = courseService.createCourse(mapper.readValue(courseDataJson, CourseCreateDTO.class), bannerFile, contentFiles, loggedInUserId);
            if(isCreated){
                return ResponseEntity.ok(HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Course created")
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .build());
            } else {
                return ResponseEntity.badRequest().body(HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Failed to created course")
                        .status(HttpStatus.BAD_REQUEST)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .build());
            }
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("An error occurred while creating course: " + e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }
    }

    @GetMapping("{courseId}/edit")
    public ResponseEntity<HttpResponseDTO> getCourseForEdit(
            @PathVariable Long courseId,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long loggedInUserId = ((UserPrincipals) userDetails).getId();

        try {
            CourseUpdateDTO courseData = courseService.getCourseForEdit(courseId, loggedInUserId);
            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .data(Map.of("course", courseData))
                    .message("Course data retrieved successfully")
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
                    .message("Course not found")
                    .status(HttpStatus.NOT_FOUND)
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("An error occurred while retrieving course data: " + e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }
    }

    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HttpResponseDTO> updateCourse(
            @RequestPart(value = "courseData") String courseDataJson,
            @RequestPart(value = "banner", required = false) MultipartFile bannerFile,
            @RequestPart(value = "contentFiles", required = false) MultipartFile[] contentFiles,
            Authentication authentication) {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long loggedInUserId = ((UserPrincipals) userDetails).getId();

        try {
            boolean isUpdated = courseService.updateCourse(courseDataJson, bannerFile, contentFiles, loggedInUserId);

            if (isUpdated) {
                return ResponseEntity.ok(HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Course updated successfully")
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .build());
            } else {
                return ResponseEntity.badRequest().body(HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Failed to update course")
                        .status(HttpStatus.BAD_REQUEST)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .build());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("An error occurred while updating course: " + e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }
    }


    @GetMapping("/user/{userId}")
    public ResponseEntity<HttpResponseDTO> getUserCourses(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<CourseDataDTO> userCourses = courseService.getUserCourses(userId, page, size);
            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("Courses owned by user with id = " + userId)
                    .status(HttpStatus.OK)
                    .data(Map.of(
                            "courses", userCourses.getContent(),
                            "currentPage", userCourses.getNumber(),
                            "totalItems", userCourses.getTotalElements(),
                            "totalPages", userCourses.getTotalPages()
                    ))
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("An error occurred during data processing: " + e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }
    }

    @GetMapping("/get-info/{courseId}")
    public ResponseEntity<HttpResponseDTO> getCourseData(@PathVariable Long courseId){
        try{
            CourseInfoDTO courseInfoDTO = courseService.getCourseData(Long.valueOf(courseId));

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("Data of Course")
                    .status(HttpStatus.OK)
                    .data(of("course", courseInfoDTO))
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message("An error occurred during data processing: " + e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }
    }
}
