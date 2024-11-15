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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)

public class CourseShopService {
    private final CourseRepository courseRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;

    public Page<CourseShopResponseDTO> searchCourses(
            String search,
            String tag,
            int page,
            int size,
            String sortBy) {

        // Domyślne sortowanie po dacie
        if (sortBy == null || !Arrays.asList("review", "reviewNumber", "date").contains(sortBy)) {
            sortBy = "date";
        }

        List<CourseEntity> courses;
        long total;

        if (search != null && tag != null) {
            // Przypadek 3: name i tag
            courses = courseRepository.findByNameAndTag(search, tag, sortBy, size, (long) page * size);
            total = courseRepository.countByNameAndTag(search, tag);
        } else if (search != null) {
            // Przypadek 1: tylko name
            courses = courseRepository.findByNameContaining(search, sortBy, size, (long) page * size);
            total = courseRepository.countByNameContaining(search);
        } else if (tag != null) {
            // Przypadek 2: tylko tag
            courses = courseRepository.findByTag(tag, sortBy, size, (long) page * size);
            total = courseRepository.countByTag(tag);
        } else {
            // Przypadek 4: wszystkie kursy
            courses = courseRepository.findAllCoursesPaged(sortBy, size, (long) page * size);
            total = courseRepository.countAll();
        }

        List<CourseShopResponseDTO> dtos = courses.stream()
                .map(this::mapToCourseShopResponseDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, PageRequest.of(page, size), total);
    }



    public List<String> searchTags(String search) {
        return courseRepository.searchTags(search);
    }

    private CourseShopResponseDTO mapToCourseShopResponseDTO(CourseEntity course) {
        UserProfileEntity ownerProfile = userProfileRepository.findByUserId(course.getUser().getId())
                .orElse(null);

        return CourseShopResponseDTO.builder()
                .courseData(mapToCourseDataDTO(course))
                .ownerData(mapToOwnerDataDTO(ownerProfile))
                .build();
    }

    private CourseDataDTO mapToCourseDataDTO(CourseEntity course) {
        Map<String, Object> bannerData = new HashMap<>();
        bannerData.put("data", course.getBanner());
        bannerData.put("mimeType", course.getMimeType());

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
                .chaptersCount(course.getChapters() != null ? course.getChapters().size() : 0)
                .ownerId(course.getUser().getId())
                .build();
    }

    private OwnerDataDTO mapToOwnerDataDTO(UserProfileEntity profile) {
        if (profile == null) {
            return null;
        }

        Map<String, Object> pictureData = new HashMap<>();
        pictureData.put("data", profile.getPicture());
        pictureData.put("mimeType", profile.getPictureMimeType());

        return OwnerDataDTO.builder()
                .id(profile.getId())
                .fullName(profile.getFullName())
                .userId(profile.getUserId())
                .description(profile.getDescription())
                .createdAt(profile.getCreatedAt())
                .picture(pictureData)
                .badgesVisible(profile.getBadgesVisible())
                .build();
    }
}
