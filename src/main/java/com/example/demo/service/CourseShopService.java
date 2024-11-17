package com.example.demo.service;

import com.example.demo.dto.course.CourseShortDTO;
import com.example.demo.dto.courseShop.CourseDataDTO;
import com.example.demo.dto.courseShop.CourseShopResponseDTO;
import com.example.demo.dto.courseShop.OwnerDataDTO;
import com.example.demo.dto.mapper.UserProfileMapper;
import com.example.demo.dto.userProfile.UserProfileResponseDTO;
import com.example.demo.entity.CourseEntity;
import com.example.demo.entity.UserProfileEntity;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.UserProfileRepository;
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
    private final UserProfileMapper userProfileMapper;



    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Page<CourseShopResponseDTO> searchCourses(
            String search,
            String tag,
            int page,
            int size,
            String sortBy) {

        try {
            validateSearchParams(page, size);

            sortBy = validateAndGetSortBy(sortBy);

            long offset = calculateOffset(page, size);

            List<CourseEntity> courses;
            long total;

            try {
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

                List<CourseShopResponseDTO> dtos = courses.stream()
                        .map(this::mapToCourseShopResponseDTO)
                        .collect(Collectors.toList());

                return new PageImpl<>(dtos, PageRequest.of(page, size), total);
            } catch (DataAccessException e) {
                log.error("Database error while searching courses: {}", e.getMessage());
                throw new ServiceException("Error accessing course data", e);
            }

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
}
