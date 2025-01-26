package com.example.ekorki.service.entity;
import com.example.ekorki.dto.chapter.ChapterCreateDTO;
import com.example.ekorki.dto.subchapter.SubchapterCreateDTO;
import com.example.ekorki.entity.*;
import com.example.ekorki.dto.chapter.ChapterShortDTO;
import com.example.ekorki.dto.course.CourseCreateDTO;
import com.example.ekorki.dto.course.CourseInfoDTO;
import com.example.ekorki.dto.course.CourseUpdateDTO;
import com.example.ekorki.exception.ApiException;
import com.example.ekorki.repository.CourseRepository;
import com.example.ekorki.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.EntityNotFoundException;
import com.example.ekorki.dto.courseShop.CourseDataDTO;
import org.springframework.cache.annotation.CacheEvict;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {
    @Autowired
    private final CourseRepository courseRepository;

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final ChapterService chapterService;

    @Autowired
    private final SubchapterService subchapterService;

    @Autowired
    private final ContentItemService contentItemService;

    @Autowired
    private final ObjectMapper objectMapper;

    @CacheEvict(value = "courses", allEntries = true)
    @Transactional
    public boolean createCourse(
            CourseCreateDTO createDTO,
            MultipartFile bannerFile,
            MultipartFile[] contentFiles,
            Long loggedInUserId) {
        try {
            UserEntity teacher = userRepository.findById(loggedInUserId)
                    .orElseThrow(() -> new UsernameNotFoundException("Teacher not found"));

            CourseEntity course = CourseEntity.builder()
                    .name(createDTO.getName())
                    .description(createDTO.getDescription())
                    .price(createDTO.getPrice())
                    .duration(createDTO.getDuration())
                    .tags(createDTO.getTags().stream()
                            .map(String::toLowerCase)
                            .collect(Collectors.toList()))
                    .user(teacher)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .review(BigDecimal.ZERO)
                    .reviewNumber(0)
                    .build();

            if (bannerFile != null && !bannerFile.isEmpty()) {
                validateFile(bannerFile);
                course.setBanner(bannerFile.getBytes());
                course.setMimeType(bannerFile.getContentType());
            }

            courseRepository.save(course);

            // Tworzenie rozdziałów i ich zawartości
            if (createDTO.getChapters() != null) {
                for (int i = 0; i < createDTO.getChapters().size(); i++) {
                    ChapterCreateDTO chapterDTO = createDTO.getChapters().get(i);
                    ChapterEntity chapter = chapterService.createChapter(chapterDTO, course, i);

                    if (chapterDTO.getSubchapters() != null) {
                        for (int j = 0; j < chapterDTO.getSubchapters().size(); j++) {
                            SubchapterCreateDTO subchapterDTO = chapterDTO.getSubchapters().get(j);
                            SubchapterEntity subchapter = subchapterService.createSubchapter(subchapterDTO, chapter, j);

                            if (subchapterDTO.getContent() != null) {
                                contentItemService.createContentItems(subchapterDTO.getContent(), subchapter, contentFiles);
                            }
                        }
                    }
                }
            }

            return true;
        } catch (Exception exception) {
            throw new ApiException("Error occurred while creating course"+exception.getMessage(), exception);
        }
    }

    @CacheEvict(value = "courses", allEntries = true)
    @Transactional
    public boolean updateCourse(
            String courseDataJson,
            MultipartFile bannerFile,
            MultipartFile[] contentFiles,
            Long loggedInUserId) {
        try {
            CourseUpdateDTO updateDTO = objectMapper.readValue(courseDataJson, CourseUpdateDTO.class);
            CourseEntity existingCourse = courseRepository.findById(updateDTO.getId())
                    .orElseThrow(() -> new ApiException("Course not found"));

            if (!existingCourse.getUser().getId().equals(loggedInUserId)) {
                throw new ApiException("You don't have permission to update this course");
            }

            updateBasicCourseInfo(existingCourse, updateDTO);

            if (bannerFile != null && !bannerFile.isEmpty()) {
                validateFile(bannerFile);
                existingCourse.setBanner(bannerFile.getBytes());
                existingCourse.setMimeType(bannerFile.getContentType());
            }

            // Aktualizacja rozdziałów i ich zawartości
            updateDTO.getChapters().ifPresent(chapters ->
                    chapterService.updateChapters(existingCourse, chapters, contentFiles));

            existingCourse.setUpdatedAt(LocalDateTime.now());
            courseRepository.save(existingCourse);
            return true;

        } catch (Exception e) {
            throw new ApiException("Error occurred while updating course: " + e.getMessage(), e);
        }
    }

    private void updateBasicCourseInfo(CourseEntity course, CourseUpdateDTO updateDTO) {
        updateDTO.getName().ifPresent(course::setName);
        updateDTO.getDescription().ifPresent(course::setDescription);
        updateDTO.getPrice().ifPresent(course::setPrice);
        updateDTO.getDuration().ifPresent(course::setDuration);
        updateDTO.getTags().ifPresent(course::setTags);
    }

    @Transactional(readOnly = true)
    public Page<CourseDataDTO> getUserCourses(Long userId, int page, int size) {
        validatePaginationParams(page, size);

        Optional<List<CourseEntity>> optionalCourseEntityList = courseRepository.findAllByUserId(userId);
        if (optionalCourseEntityList.isPresent()) {
            List<CourseEntity> allCourses = optionalCourseEntityList.get();

            int start = page * size;
            int end = Math.min(start + size, allCourses.size());

            List<CourseDataDTO> courseDTOs = allCourses.subList(start, end).stream()
                    .map(this::mapToCourseData)
                    .collect(Collectors.toList());

            return new PageImpl<>(
                    courseDTOs,
                    PageRequest.of(page, size),
                    allCourses.size()
            );
        }
        throw new ApiException("Courses not found");
    }

    private void validatePaginationParams(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
    }

    public CourseDataDTO mapToCourseData(CourseEntity course){
        Hibernate.initialize(course.getChapters());
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
                .chaptersCount(course.getChapters().size())
                .ownerId(course.getUser().getId())
                .build();
    }

    @Transactional
    public CourseInfoDTO getCourseData(Long courseId){
        Optional<CourseEntity> optionalCourseEntity = courseRepository.findById(courseId);
        if(optionalCourseEntity.isPresent()){
            CourseEntity courseEntity = optionalCourseEntity.get();
            return mapToCourseInfo(courseEntity);
        }
        else {
            throw new ApiException("Course not found");
        }
    }

    public CourseInfoDTO mapToCourseInfo(CourseEntity course){
        Hibernate.initialize(course.getChapters());
        Map<String, Object> bannerData = new HashMap<>();
        bannerData.put("data", course.getBanner());
        bannerData.put("mimeType", course.getMimeType());
        return CourseInfoDTO.builder()
                .id(course.getId())
                .name(course.getName())
                .banner(bannerData)
                .review(course.getReview())
                .duration(course.getDuration())
                .ownerId(course.getUser().getId())
                .tags(course.getTags())
                .reviewNumber(course.getReviewNumber())
                .description(course.getDescription())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .chaptersShortInfo(course.getChapters() == null|| course.getChapters().isEmpty()
                        ? new ArrayList<>()
                        : mapChaptersToShortInfo(course.getChapters()) )
                .build();
    }

    private List<ChapterShortDTO> mapChaptersToShortInfo(List<ChapterEntity> chapters) {
        if (chapters == null) {
            return Collections.emptyList();
        }

        return chapters.stream()
                .map(chapter -> ChapterShortDTO.builder()
                        .id(chapter.getId())
                        .courseId(chapter.getCourse().getId())
                        .name(chapter.getName())
                        .order(chapter.getOrder())
                        .review(chapter.getReview())
                        .reviewNumber(chapter.getReviewNumber())
                        .subchapterNumber(chapter.getSubchapters().size())
                        .build())
                .sorted(Comparator.comparing(ChapterShortDTO::getOrder))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CourseUpdateDTO getCourseForEdit(Long courseId, Long loggedInUserId) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        if (!course.getUser().getId().equals(loggedInUserId)) {
            throw new ApiException("You don't have permission to edit this course");
        }

        return mapCourseToUpdateDTO(course);
    }

    private CourseUpdateDTO mapCourseToUpdateDTO(CourseEntity course) {
        Map<String, Object> bannerData = new HashMap<>();
        bannerData.put("data", course.getBanner());
        bannerData.put("mimeType", course.getMimeType());
        return CourseUpdateDTO.builder()
                .id(course.getId())
                .name(Optional.ofNullable(course.getName()))
                .banner(Optional.ofNullable(bannerData))
                .description(Optional.ofNullable(course.getDescription()))
                .price(Optional.ofNullable(course.getPrice()))
                .duration(Optional.ofNullable(course.getDuration()))
                .tags(Optional.ofNullable(course.getTags()))
                .chapters(Optional.of(chapterService.mapChaptersToUpdateDTO(course.getChapters())))
                .build();
    }


    private void validateFile(MultipartFile file) {
        if (file.getSize() > 100 * 1024 * 1024) { // 100MB limit
            throw new ApiException("File size exceeds maximum limit of 100MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !(contentType.startsWith("image/") || contentType.startsWith("video/"))) {
            throw new ApiException("Only image and video files are allowed");
        }
    }
}
