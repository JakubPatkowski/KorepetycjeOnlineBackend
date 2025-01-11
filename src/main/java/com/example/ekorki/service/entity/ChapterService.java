package com.example.ekorki.service.entity;

import com.example.ekorki.dto.chapter.ChapterCreateDTO;
import com.example.ekorki.dto.chapter.ChapterDetailsDTO;
import com.example.ekorki.dto.chapter.ChapterShortDTO;
import com.example.ekorki.dto.chapter.ChapterUpdateDTO;
import com.example.ekorki.dto.subchapter.SubchapterShortDTO;
import com.example.ekorki.entity.ChapterEntity;
import com.example.ekorki.entity.CourseEntity;
import com.example.ekorki.exception.ApiException;
import com.example.ekorki.repository.ChapterRepository;
import com.example.ekorki.repository.CourseRepository;
import com.example.ekorki.repository.PurchasedCourseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;


import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "chapters")
public class ChapterService {
    @Autowired
    private final ChapterRepository chapterRepository;
    @Autowired
    private final SubchapterService subchapterService;
    @Autowired
    private final PurchasedCourseRepository purchasedCourseRepository;
    @Autowired
    private final CourseRepository courseRepository;

    @CacheEvict(value = {"chapters", "courses"}, allEntries = true)
    @Transactional
    public ChapterEntity createChapter(ChapterCreateDTO dto, CourseEntity course, int order) {
        ChapterEntity chapter = ChapterEntity.builder()
                .course(course)
                .name(dto.getName())
                .order(order)
                .review(BigDecimal.ZERO)
                .reviewNumber(0)
                .build();

        return chapterRepository.save(chapter);
    }

    @Cacheable(key = "#chapterId + '_user_' + #userId")
    @Transactional(readOnly = true)
    public ChapterDetailsDTO getChapterDetails(Long chapterId, Long userId){
        ChapterEntity chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new EntityNotFoundException("Chapter not found"));
        
        CourseEntity course = chapter.getCourse();
        
        if(!course.getUser().getId().equals(userId)){
            boolean hasPurchased = purchasedCourseRepository.existsByUserIdAndCourseId(userId, course.getId());
            if(!hasPurchased){
                throw new ApiException("You don`t have access to this chapter");
            }
        }
        return mapToChapterDetailsDTO(chapter);
    }

    private ChapterDetailsDTO mapToChapterDetailsDTO(ChapterEntity chapter) {
        return ChapterDetailsDTO.builder()
                .id(chapter.getId())
                .courseId(chapter.getCourse().getId())
                .name(chapter.getName())
                .order(chapter.getOrder())
                .review(chapter.getReview())
                .reviewNumber(chapter.getReviewNumber())
                .subchapters(chapter.getSubchapters().stream()
                        .map(subchapter -> SubchapterShortDTO.builder()
                                .id(subchapter.getId())
                                .name(subchapter.getName())
                                .order(subchapter.getOrder())
                                .build())
                        .sorted(Comparator.comparing(SubchapterShortDTO::getOrder))
                        .collect(Collectors.toList()))
                .build();
    }

    @CacheEvict(value = {"chapters", "courses"}, allEntries = true)
    @Transactional
    public void updateChapters(CourseEntity course, List<ChapterUpdateDTO> chaptersDTO, MultipartFile[] contentFiles) {
        Map<Long, Integer> fileIndexMap = new HashMap<>();

        // Usuń rozdziały oznaczone do usunięcia
        chaptersDTO.stream()
                .filter(chapterDTO -> chapterDTO.getId() != null &&
                        chapterDTO.getDeleted().orElse(false))
                .forEach(chapterDTO -> course.getChapters().removeIf(
                        chapter -> chapter.getId().equals(chapterDTO.getId())));

        // Aktualizuj lub dodaj nowe rozdziały
        for (ChapterUpdateDTO chapterDTO : chaptersDTO) {
            if (chapterDTO.getDeleted().orElse(false)) continue;

            if (chapterDTO.getId() != null) {
                // Aktualizuj istniejący rozdział
                ChapterEntity chapter = course.getChapters().stream()
                        .filter(ch -> ch.getId().equals(chapterDTO.getId()))
                        .findFirst()
                        .orElseThrow(() -> new ApiException("Chapter not found: " + chapterDTO.getId()));

                updateExistingChapter(chapter, chapterDTO);

                // Aktualizuj podrozdziały jeśli zostały przesłane
                chapterDTO.getSubchapters().ifPresent(subchapters ->
                        subchapterService.updateSubchapters(chapter, subchapters, contentFiles, fileIndexMap));
            } else {
                // Utwórz nowy rozdział
                ChapterEntity newChapter = createChapter(
                        ChapterCreateDTO.builder()
                                .name(chapterDTO.getName().orElse("New Chapter"))
                                .build(),
                        course,
                        chapterDTO.getOrder().orElse(course.getChapters().size())
                );

                course.getChapters().add(newChapter);

                // Dodaj podrozdziały do nowego rozdziału jeśli zostały przesłane
                chapterDTO.getSubchapters().ifPresent(subchapters ->
                        subchapterService.updateSubchapters(newChapter, subchapters, contentFiles, fileIndexMap));
            }
        }
    }

    private void updateExistingChapter(ChapterEntity chapter, ChapterUpdateDTO chapterDTO) {
        chapterDTO.getName().ifPresent(chapter::setName);
        chapterDTO.getOrder().ifPresent(chapter::setOrder);
        chapterRepository.save(chapter);
    }

    @Transactional
    public List<ChapterUpdateDTO> mapChaptersToUpdateDTO(List<ChapterEntity> chapters) {
        return chapters.stream()
                .map(this::mapChapterToUpdateDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChapterUpdateDTO mapChapterToUpdateDTO(ChapterEntity chapter) {
        return ChapterUpdateDTO.builder()
                .id(chapter.getId())
                .name(Optional.ofNullable(chapter.getName()))
                .order(Optional.ofNullable(chapter.getOrder()))
                .subchapters(Optional.of(subchapterService.mapSubchaptersToUpdateDTO(chapter.getSubchapters())))
                .deleted(Optional.of(false))
                .build();
    }

    @Cacheable(key = "'course_chapters_' + #courseId")
    @Transactional(readOnly = true)
    public List<ChapterShortDTO> getCourseChapters(Long courseId) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        return course.getChapters().stream()
                .map(chapter -> ChapterShortDTO.builder()
                        .id(chapter.getId())
                        .courseId(courseId)
                        .name(chapter.getName())
                        .order(chapter.getOrder())
                        .review(chapter.getReview())
                        .reviewNumber(chapter.getReviewNumber())
                        .subchapterNumber(chapter.getSubchapters().size())
                        .build())
                .sorted(Comparator.comparing(ChapterShortDTO::getOrder))
                .collect(Collectors.toList());
    }


}
