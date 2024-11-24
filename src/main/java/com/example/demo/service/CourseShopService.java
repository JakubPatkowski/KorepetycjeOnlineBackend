package com.example.demo.service;

import com.example.demo.dto.chapter.ChapterShortDTO;
import com.example.demo.dto.courseShop.*;
import com.example.demo.entity.CourseEntity;
import com.example.demo.entity.UserProfileEntity;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.PurchasedCourseRepository;
import com.example.demo.repository.UserProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CourseShopService {
    private final CourseRepository courseRepository;
    private final UserProfileRepository userProfileRepository;
    private final PurchasedCourseRepository purchasedCourseRepository;

    @Autowired
    private CacheManager cacheManager;

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Page<CourseShopResponseDTO> searchCourses(
            String search,
            String tag,
            int page,
            int size,
            String sortBy,
            Long loggedInUserId) {

        try {
            validateSearchParams(page, size);
            sortBy = validateAndGetSortBy(sortBy);
            long offset = calculateOffset(page, size);

            List<CourseEntity> courses;
            long total;

            // Logika dla zalogowanego użytkownika
            if (loggedInUserId != null) {
                if (search != null && tag != null) {
                    courses = courseRepository.findAvailableByNameAndTagForUser(search, tag, loggedInUserId, sortBy, size, offset);
                    total = courseRepository.countAvailableByNameAndTagForUser(search, tag, loggedInUserId);
                } else if (search != null) {
                    courses = courseRepository.findAvailableByNameForUser(search, loggedInUserId, sortBy, size, offset);
                    total = courseRepository.countAvailableByNameForUser(search, loggedInUserId);
                } else if (tag != null) {
                    courses = courseRepository.findAvailableByTagForUser(tag, loggedInUserId, sortBy, size, offset);
                    total = courseRepository.countAvailableByTagForUser(tag, loggedInUserId);
                } else {
                    courses = courseRepository.findAllAvailableForUser(loggedInUserId, sortBy, size, offset);
                    total = courseRepository.countAllAvailableForUser(loggedInUserId);
                }
            }
            // Logika dla niezalogowanego użytkownika (pozostaje bez zmian)
            else {
                if (search != null && tag != null) {
                    courses = courseRepository.findByNameAndTag(search, tag, sortBy, size, offset);
                    total = courseRepository.countByNameAndTag(search, tag);
                } else if (search != null) {
                    courses = courseRepository.findByNameContaining(search, sortBy, size, offset);
                    total = courseRepository.countByNameContaining(search);
                } else if (tag != null) {
                    courses = courseRepository.findByTag(tag, sortBy, size, offset);
                    total = courseRepository.countByTag(tag);
                } else {
                    courses = courseRepository.findAllCoursesPaged(sortBy, size, offset);
                    total = courseRepository.countAll();
                }
            }

            List<CourseShopResponseDTO> dtos = courses.stream()
                    .map(this::mapToCourseShopResponseDTO)
                    .collect(Collectors.toList());

            return new PageImpl<>(dtos, PageRequest.of(page, size), total);
        } catch (Exception e) {
            log.error("Unexpected error in searchCourses: {}", e.getMessage());
            throw new ServiceException("Error processing course search", e);
        }
    }


    @Transactional
    public List<String> searchTags(String search) {
        return courseRepository.searchTags(search);
    }

    @Cacheable(value = "courseOwnerProfiles", key = "#course.id")
    public CourseShopResponseDTO mapToCourseShopResponseDTO(CourseEntity course) {
        try {
            UserProfileEntity ownerProfile = userProfileRepository.findByUserId(course.getUser().getId())
                    .orElse(null);

            if (ownerProfile == null) {
                log.warn("Owner profile not found for course ID: {}", course.getId());
            }

            return CourseShopResponseDTO.builder()
                    .courseData(mapToCourseDataDTO(course))
                    .ownerData(mapToOwnerDataDTO(ownerProfile))
                    .build();

        } catch (Exception e) {
            log.error("Error mapping course to DTO: {}", e.getMessage());
            return null;  // Return null instead of throwing to allow partial results
        }
    }

    private CourseDataDTO mapToCourseDataDTO(CourseEntity course) {
        if (course == null) {
            return null;
        }

        try {
            Map<String, Object> bannerData = Optional.ofNullable(course.getBanner())
                    .map(banner -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("data", banner);
                        data.put("mimeType", course.getMimeType());
                        return data;
                    })
                    .orElse(null);

            return CourseDataDTO.builder()
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
                    .chaptersCount(Optional.ofNullable(course.getChapters())
                            .map(List::size)
                            .orElse(0))
                    .ownerId(course.getUser().getId())
                    .build();

        } catch (Exception e) {
            log.error("Error mapping course data: {}", e.getMessage());
            throw new ServiceException("Error processing course data", e);
        }
    }

    @Cacheable(value = "ownerProfiles", key = "#profile.id")
    public OwnerDataDTO mapToOwnerDataDTO(UserProfileEntity profile) {
        if (profile == null) {
            return null;
        }

        try {
            Map<String, Object> pictureData = Optional.ofNullable(profile.getPicture())
                    .map(picture -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("data", picture);
                        data.put("mimeType", profile.getPictureMimeType());
                        return data;
                    })
                    .orElse(null);

            return OwnerDataDTO.builder()
                    .id(profile.getId())
                    .fullName(profile.getFullName())
                    .userId(profile.getUserId())
                    .description(profile.getDescription())
                    .createdAt(profile.getCreatedAt())
                    .picture(pictureData)
                    .badgesVisible(profile.getBadgesVisible())
                    .build();

        } catch (Exception e) {
            log.error("Error mapping owner data: {}", e.getMessage());
            return null;  // Return null instead of throwing to allow partial results
        }
    }


    public List<CourseShopResponseDTO> getAll() {
        return courseRepository.findAll().stream()
                .map(this::mapToCourseShopResponseDTO)
                .collect(Collectors.toList());
    }

    private void validateSearchParams(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
    }

    private String validateAndGetSortBy(String sortBy) {
        List<String> validSortOptions = Arrays.asList("review", "reviewNumber", "date");
        return validSortOptions.contains(sortBy) ? sortBy : "date";
    }

    private long calculateOffset(int page, int size) {
        // Prevent integer overflow
        if (page > Integer.MAX_VALUE / size) {
            throw new IllegalArgumentException("Page number too large");
        }
        return (long) page * size;
    }

    @Transactional
    public CourseShopDetailsResponseDTO getCourseWithDetails(CourseEntity course, Long loggedInUserId) {
        List<ChapterShortDTO> chapters = course.getChapters().stream()
                .map(chapter -> ChapterShortDTO.builder()
                        .id(chapter.getId())
                        .courseId(course.getId())
                        .name(chapter.getName())
                        .order(chapter.getOrder())
                        .review(chapter.getReview())
                        .reviewNumber(chapter.getReviewNumber())
                        .subchapterNumber(chapter.getSubchapters().size())
                        .build())
                .sorted(Comparator.comparing(ChapterShortDTO::getOrder))
                .toList();

        CourseShopDetailsDTO courseData = mapToCourseShopDetailsDTO(course, loggedInUserId, chapters);
        UserProfileEntity ownerProfile = userProfileRepository.findByUserId(course.getUser().getId())
                .orElseThrow(() -> new EntityNotFoundException("Course owner profile not found"));

        return CourseShopDetailsResponseDTO.builder()
                .courseData(courseData)
                .ownerData(mapToOwnerDataDTO(ownerProfile))
                .build();
    }


    private CourseShopDetailsDTO mapToCourseShopDetailsDTO(CourseEntity course, Long loggedInUserId, List<ChapterShortDTO> chapters) {
        Map<String, Object> bannerData = new HashMap<>();
        bannerData.put("data", course.getBanner());
        bannerData.put("mimeType", course.getMimeType());

        return CourseShopDetailsDTO.builder()
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
                .ownerId(course.getUser().getId())
                .relationshipType(determineRelationshipType(course, loggedInUserId))
                .chaptersShortInfo(chapters)
                .build();
    }


    private CourseRelationshipType determineRelationshipType(CourseEntity course, Long loggedInUserId) {
        // Jeśli użytkownik nie jest zalogowany, kurs jest dostępny
        if (loggedInUserId == null) {
            return CourseRelationshipType.AVAILABLE;
        }

        // Sprawdzanie czy użytkownik jest właścicielem
        if (course.getUser().getId().equals(loggedInUserId)) {
            return CourseRelationshipType.OWNER;
        }

        // Sprawdzanie czy użytkownik kupił kurs
        boolean isPurchased = purchasedCourseRepository.existsByUserIdAndCourseId(loggedInUserId, course.getId());
        if (isPurchased) {
            return CourseRelationshipType.PURCHASED;
        }

        return CourseRelationshipType.AVAILABLE;
    }
}
