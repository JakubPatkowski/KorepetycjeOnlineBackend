package com.example.demo.service.entity;
import com.example.demo.dto.chapter.ChapterCreateDTO;
import com.example.demo.dto.chapter.ChapterUpdateDTO;
import com.example.demo.dto.contentItem.ContentItemCreateDTO;
import com.example.demo.dto.contentItem.ContentItemUpdateDTO;
import com.example.demo.dto.subchapter.SubchapterCreateDTO;
import com.example.demo.dto.subchapter.SubchapterUpdateDTO;
import com.example.demo.entity.*;
import com.example.demo.repository.ChapterRepository;
import com.example.demo.repository.SubchapterRepository;
import com.example.demo.repository.ContentItemRepository;
import com.example.demo.dto.chapter.ChapterShortDTO;
import com.example.demo.dto.course.CourseCreateDTO;
import com.example.demo.dto.course.CourseInfoDTO;
import com.example.demo.dto.course.CourseUpdateDTO;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
                    .tags(createDTO.getTags())
                    .user(teacher)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .review(BigDecimal.ZERO)
                    .reviewNumber(0)
                    .build();

            if (bannerFile != null && !bannerFile.isEmpty()) {
                validateFile(bannerFile);
                course.setBanner(bannerFile.getBytes());
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

                            if (subchapterDTO.getContentItems() != null) {
                                contentItemService.createContentItems(subchapterDTO.getContentItems(), subchapter, contentFiles);
                            }
                        }
                    }
                }
            }

            return true;
        } catch (Exception exception) {
            throw new ApiException("Error occurred while creating course", exception);
        }
    }

    @Transactional
    public boolean updateCourse(
            String courseDataJson,
            MultipartFile bannerFile,
            MultipartFile[] contentFiles,
            Long loggedInUserId) {
        try {
            // Deserializacja JSON do CourseUpdateDTO
            CourseUpdateDTO updateDTO = objectMapper.readValue(courseDataJson, CourseUpdateDTO.class);

            CourseEntity existingCourse = courseRepository.findById(updateDTO.getId())
                    .orElseThrow(() -> new ApiException("Course not found"));

            // Sprawdzenie uprawnień
            if (!existingCourse.getUser().getId().equals(loggedInUserId)) {
                throw new ApiException("You don't have permission to update this course");
            }

            // Aktualizacja podstawowych danych kursu
            updateBasicCourseInfo(existingCourse, updateDTO);

            // Obsługa bannera
            if (bannerFile != null && !bannerFile.isEmpty()) {
                validateFile(bannerFile);
                existingCourse.setBanner(bannerFile.getBytes());
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

//    @Transactional //do zwracania właścicielowi kursu do edycji
//    public CourseUpdateDTO getEditCourseData(Long userId, Long courseId){
//        Optional<CourseEntity> optionalCourseEntity = courseRepository.findByUserIdAndId(userId,courseId);
//        if (optionalCourseEntity.isPresent()){
//            CourseEntity courseEntity = optionalCourseEntity.get();
//            return CourseUpdateDTO.builder()
//                    .id(courseEntity.getId())
//                    .name(courseEntity.getName())
//                    .bannerField(courseEntity.getBanner())
//                    .price(courseEntity.getPrice())
//                    .duration(courseEntity.getDuration())
//                    .tags(courseEntity.getTags())
//                    .description(courseEntity.getDescription())
//                    .chaptersShortInfo(mapChaptersToShortInfo(courseEntity.getChapters()))
//                    .build();
//        } else {
//            throw new ApiException("Course not found");
//        }
//    }



    public List<CourseInfoDTO> getUserCourses(Long userId){
        Optional<List<CourseEntity>> optionalCourseEntityList = courseRepository.findAllByUserId(userId);
        if(optionalCourseEntityList.isPresent()){
            List<CourseEntity> courseEntityList = optionalCourseEntityList.get();
            return courseEntityList.stream()
                    .map(this::mapToCourseInfo)
                    .collect(Collectors.toList());
        } else  {
            throw new ApiException("Courses not found");
        }
    }

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

    private CourseInfoDTO mapToCourseInfo(CourseEntity course){
        Hibernate.initialize(course.getChapters());
        return CourseInfoDTO.builder()
                .id(course.getId())
                .name(course.getName())
                .banner(course.getBanner())
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

    private void validateFile(MultipartFile file) {
        if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
            throw new ApiException("File size exceeds maximum limit of 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !(contentType.startsWith("image/") || contentType.startsWith("video/"))) {
            throw new ApiException("Only image and video files are allowed");
        }
    }
}
