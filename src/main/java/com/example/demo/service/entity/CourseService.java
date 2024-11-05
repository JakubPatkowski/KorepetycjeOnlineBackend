package com.example.demo.service.entity;
import com.example.demo.dto.chapter.ChapterCreateDTO;
import com.example.demo.dto.contentItem.ContentItemCreateDTO;
import com.example.demo.dto.subchapter.SubchapterCreateDTO;
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
    private final ChapterRepository chapterRepository;

    @Autowired
    private final SubchapterRepository subchapterRepository;

    @Autowired
    private final ContentItemRepository contentItemRepository;

    @Transactional
    public boolean createCourse(
            CourseCreateDTO createDTO,
            MultipartFile bannerFile,
            MultipartFile[] contentFiles,
            Long loggedInUserId){
        try{
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

            if (createDTO.getChapters() != null){
                for(int i = 0; i < createDTO.getChapters().size(); i++){
                    ChapterCreateDTO chapterDTO = createDTO.getChapters().get(i);
                    ChapterEntity chapter = createChapter(chapterDTO, course, i);

                    if(chapterDTO.getSubchapters() != null){
                        for (int j = 0; j < chapterDTO.getSubchapters().size(); j++){
                            SubchapterCreateDTO subchapterDTO = chapterDTO.getSubchapters().get(j);
                            SubchapterEntity subchapter = createSubchapter(subchapterDTO, chapter, j);

                            if(subchapterDTO.getContentItems() != null){
                                createContentItems(subchapterDTO.getContentItems(), subchapter, contentFiles);
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

    @Transactional //do zwracania właścicielowi kursu do edycji
    public CourseUpdateDTO getEditCourseData(Long userId, Long courseId){
        Optional<CourseEntity> optionalCourseEntity = courseRepository.findByUserIdAndId(userId,courseId);
        if (optionalCourseEntity.isPresent()){
            CourseEntity courseEntity = optionalCourseEntity.get();
            return CourseUpdateDTO.builder()
                    .id(courseEntity.getId())
                    .name(courseEntity.getName())
                    .bannerField(courseEntity.getBanner())
                    .price(courseEntity.getPrice())
                    .duration(courseEntity.getDuration())
                    .tags(courseEntity.getTags())
                    .description(courseEntity.getDescription())
                    .chaptersShortInfo(mapChaptersToShortInfo(courseEntity.getChapters()))
                    .build();
        } else {
            throw new ApiException("Course not found");
        }
    }

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

    private ChapterEntity createChapter(ChapterCreateDTO dto, CourseEntity course, int order) {
        ChapterEntity chapter = ChapterEntity.builder()
                .course(course)
                .name(dto.getName())
                .order(order)
                .review(BigDecimal.ZERO)
                .reviewNumber(0)
                .build();

        return chapterRepository.save(chapter);
    }

    private SubchapterEntity createSubchapter(SubchapterCreateDTO dto, ChapterEntity chapter, int order) {
        SubchapterEntity subchapter = SubchapterEntity.builder()
                .chapter(chapter)
                .name(dto.getName())
                .order(order)
                .build();

        return subchapterRepository.save(subchapter);
    }

    private void createContentItems(List<ContentItemCreateDTO> dtos,
                                    SubchapterEntity subchapter,
                                    MultipartFile[] contentFiles) throws IOException {
        int fileIndex = 0;
        ObjectMapper objectMapper = new ObjectMapper();

        for(int i = 0; i < dtos.size(); i++){
            ContentItemCreateDTO dto = dtos.get(i);
            ContentItemEntity contentItem = ContentItemEntity.builder()
                    .subchapter(subchapter)
                    .type(dto.getType())
                    .order(i)
                    .build();

            switch (dto.getType().toLowerCase()){
                case "text":
                    contentItem.setTextContent(dto.getTextContent());
                    contentItem.setFontSize(dto.getFontSize());
                    contentItem.setFontWeight(dto.getFontWeight());
                    contentItem.setItalics(dto.getItalics());
                    contentItem.setEmphasis(dto.getEmphasis());
                    break;
                case "video":
                case "image":
                    if (contentFiles != null && fileIndex < contentFiles.length) {
                        MultipartFile file = contentFiles[fileIndex++];
                        validateFile(file);
                        contentItem.setFile(file.getBytes());
                    }
                    break;
                case "quiz":
                    if (dto.getQuizData() != null) {
                        try {
                            // Konwersja String na JsonNode i z powrotem do String
                            // aby upewnić się, że mamy poprawny format JSON
                            JsonNode jsonNode = objectMapper.readTree(dto.getQuizData());
                            contentItem.setQuizData(objectMapper.writeValueAsString(jsonNode));
                        } catch (JsonProcessingException e) {
                            throw new ApiException("Invalid quiz data format", e);
                        }
                    }
                    break;
                default:
                    throw new ApiException("Invalid content type: " + dto.getType());
            }
            contentItemRepository.save(contentItem);
        }
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
