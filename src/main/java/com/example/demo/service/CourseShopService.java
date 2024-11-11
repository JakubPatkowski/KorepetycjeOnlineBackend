package com.example.demo.service;

import com.example.demo.dto.course.CourseShortDTO;
import com.example.demo.entity.CourseEntity;
import com.example.demo.entity.UserProfileEntity;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)

public class CourseShopService {
    private final CourseRepository courseRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional(readOnly = true)
    public Page<CourseShortDTO> searchCourses(
            String search,
            int page,
            int size,
            String sortBy,
            List<String> tags) {

        Sort sort = createSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        try {
            // Najpierw pobierz stronę kursów
            Page<CourseEntity> coursesPage = courseRepository.findAll(pageable);

            // Mapuj wyniki
            return coursesPage.map(course -> {
                try {
                    UserProfileEntity ownerProfile = userProfileRepository
                            .findByUserId(course.getUser().getId())
                            .orElse(null);

                    Map<String, Object> bannerData = null;
                    if (course.getBanner() != null) {
                        bannerData = new HashMap<>();
                        bannerData.put("data", course.getBanner());
                        bannerData.put("mimeType", course.getMimeType());
                    }

                    return CourseShortDTO.builder()
                            .id(course.getId())
                            .name(course.getName())
                            .banner(bannerData)
                            .price(course.getPrice())
                            .duration(course.getDuration())
                            .tags(course.getTags())
                            .review(course.getReview())
                            .reviewNumber(course.getReviewNumber())
                            .description(course.getDescription())
                            .createdAt(course.getCreatedAt())
                            .updatedAt(course.getUpdatedAt())
                            .chaptersCount(course.getChapters() != null ? course.getChapters().size() : 0)
                            .owner(ownerProfile)
                            .build();
                } catch (Exception e) {
                    throw new RuntimeException("Error mapping course to DTO", e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Error fetching courses", e);
        }
    }

    public List<String> searchTags(String search) {
        return courseRepository.searchTags(search);
    }

    public Page<CourseShortDTO> getCourses(int page, int size, String sortBy, List<String> tags) {
        Sort sort = createSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CourseEntity> coursesPage;
        if (tags != null && !tags.isEmpty()) {
            coursesPage = courseRepository.findByTagsContainingAny(tags, pageable);
        } else {
            coursesPage = courseRepository.findAll(pageable);
        }

        return coursesPage.map(this::mapToCourseShortDTO);
    }

    private Sort createSort(String sortBy) {
        if (sortBy == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return switch (sortBy.toLowerCase()) {
            case "reviews" -> Sort.by(Sort.Direction.DESC, "reviewNumber");
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "rating" -> Sort.by(Sort.Direction.DESC, "review");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }


    private CourseShortDTO mapToCourseShortDTO(CourseEntity course) {
        Map<String, Object> bannerData = null;
        if (course.getBanner() != null) {
            bannerData = new HashMap<>();
            bannerData.put("data", course.getBanner());
            bannerData.put("mimeType", course.getMimeType());
        }

        UserProfileEntity ownerProfile = userProfileRepository.findByUserId(course.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Owner profile not found"));



        return CourseShortDTO.builder()
                .id(course.getId())
                .name(course.getName())
                .banner(bannerData)
                .price(course.getPrice())
                .duration(course.getDuration())
                .tags(course.getTags())
                .review(course.getReview())
                .reviewNumber(course.getReviewNumber())
                .description(course.getDescription())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .chaptersCount(course.getChapters().size())
                .owner(ownerProfile)
                .build();
    }

    public List<String> getAllTags() {
        return courseRepository.findAllTags();
    }
}
