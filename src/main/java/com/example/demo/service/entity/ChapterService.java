package com.example.demo.service.entity;

import com.example.demo.dto.chapter.ChapterCreateDTO;
import com.example.demo.dto.chapter.ChapterUpdateDTO;
import com.example.demo.entity.ChapterEntity;
import com.example.demo.entity.CourseEntity;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.ChapterRepository;
import com.example.demo.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChapterService {
    @Autowired
    private final ChapterRepository chapterRepository;
    @Autowired
    private final SubchapterService subchapterService;

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

    public List<ChapterUpdateDTO> mapChaptersToUpdateDTO(List<ChapterEntity> chapters) {
        return chapters.stream()
                .map(this::mapChapterToUpdateDTO)
                .collect(Collectors.toList());
    }

    public ChapterUpdateDTO mapChapterToUpdateDTO(ChapterEntity chapter) {
        return ChapterUpdateDTO.builder()
                .id(chapter.getId())
                .name(Optional.ofNullable(chapter.getName()))
                .order(Optional.ofNullable(chapter.getOrder()))
                .subchapters(Optional.of(subchapterService.mapSubchaptersToUpdateDTO(chapter.getSubchapters())))
                .deleted(Optional.of(false))
                .build();
    }
}
