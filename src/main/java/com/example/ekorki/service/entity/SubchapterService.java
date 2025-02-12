package com.example.ekorki.service.entity;

import com.example.ekorki.dto.contentItem.ContentItemResponseDTO;
import com.example.ekorki.dto.subchapter.SubchapterCreateDTO;
import com.example.ekorki.dto.subchapter.SubchapterDetailsDTO;
import com.example.ekorki.dto.subchapter.SubchapterUpdateDTO;
import com.example.ekorki.entity.ChapterEntity;
import com.example.ekorki.entity.ContentItemEntity;
import com.example.ekorki.entity.CourseEntity;
import com.example.ekorki.entity.SubchapterEntity;
import com.example.ekorki.exception.ApiException;
import com.example.ekorki.repository.PurchasedCourseRepository;
import com.example.ekorki.repository.SubchapterRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "subchapters")
public class SubchapterService {
    @Autowired
    private final SubchapterRepository subchapterRepository;

    @Autowired
    private final ContentItemService contentItemService;

    @Autowired
    private final PurchasedCourseService purchasedCourseService;

    @CacheEvict(value = {"subchapters", "chapters", "contentItems"}, allEntries = true)
    @Transactional
    public SubchapterEntity createSubchapter(SubchapterCreateDTO dto, ChapterEntity chapter, int order) {
        SubchapterEntity subchapter = SubchapterEntity.builder()
                .chapter(chapter)
                .name(dto.getName())
                .order(order)
                .build();

        return subchapterRepository.save(subchapter);
    }

    @CacheEvict(value = {"subchapters", "chapters", "contentItems"}, allEntries = true)
    @Transactional
    public void updateSubchapters(ChapterEntity chapter, List<SubchapterUpdateDTO> subchaptersDTO,
                                  MultipartFile[] contentFiles, Map<Long, Integer> fileIndexMap) {
        // Usuń podrozdziały oznaczone do usunięcia
        subchaptersDTO.stream()
                .filter(subchapterDTO -> subchapterDTO.getId() != null &&
                        subchapterDTO.getDeleted().orElse(false))
                .forEach(subchapterDTO -> chapter.getSubchapters().removeIf(
                        subchapter -> subchapter.getId().equals(subchapterDTO.getId())));

        // Aktualizuj lub dodaj nowe podrozdziały
        for (SubchapterUpdateDTO subchapterDTO : subchaptersDTO) {
            if (subchapterDTO.getDeleted().orElse(false)) continue;

            if (subchapterDTO.getId() != null) {
                SubchapterEntity subchapter = chapter.getSubchapters().stream()
                        .filter(sub -> sub.getId().equals(subchapterDTO.getId()))
                        .findFirst()
                        .orElseThrow(() -> new ApiException("Subchapter not found: " + subchapterDTO.getId()));

                updateExistingSubchapter(subchapter, subchapterDTO);

                subchapterDTO.getContent().ifPresent(contentItems ->
                        contentItemService.updateContentItems(subchapter, contentItems, contentFiles, fileIndexMap));
            } else {
                SubchapterEntity newSubchapter = createSubchapter(
                        SubchapterCreateDTO.builder()
                                .name(subchapterDTO.getName().orElse("New Subchapter"))
                                .build(),
                        chapter,
                        subchapterDTO.getOrder().orElse(chapter.getSubchapters().size())
                );

                chapter.getSubchapters().add(newSubchapter);

                subchapterDTO.getContent().ifPresent(contentItems ->
                        contentItemService.updateContentItems(newSubchapter, contentItems, contentFiles, fileIndexMap));
            }
        }
    }

    @Cacheable(key = "#subchapterId + '_user_' + #userId")
    @Transactional(readOnly = true)
    public SubchapterDetailsDTO getSubchapterDetails(Long subchapterId, Long userId){
        SubchapterEntity subchapter = subchapterRepository.findById(subchapterId)
                .orElseThrow(() -> new EntityNotFoundException("Subchapter not found"));

        CourseEntity course =subchapter.getChapter().getCourse();
        if (!course.getUser().getId().equals(userId)) {
            boolean hasPurchased = purchasedCourseService.hasUserPurchasedCourse(userId, course.getId());
            if (!hasPurchased) {
                throw new ApiException("You don't have access to this subchapter");
            }
        }

        return mapToSubchapterDetailsDTO(subchapter);
    }

    private SubchapterDetailsDTO mapToSubchapterDetailsDTO(SubchapterEntity subchapter) {
        return SubchapterDetailsDTO.builder()
                .id(subchapter.getId())
                .chapterId(subchapter.getChapter().getId())
                .name(subchapter.getName())
                .order(subchapter.getOrder())
                .content(mapContentItems(subchapter.getContent()))
                .build();
    }

    private List<ContentItemResponseDTO> mapContentItems(List<ContentItemEntity> contentItems) {
        return contentItems.stream()
                .map(this::mapContentItem)
                .sorted(Comparator.comparing(ContentItemResponseDTO::getOrder))
                .collect(Collectors.toList());
    }

    private ContentItemResponseDTO mapContentItem(ContentItemEntity item) {
        ContentItemResponseDTO.ContentItemResponseDTOBuilder builder = ContentItemResponseDTO.builder()
                .id(item.getId())
                .subchapterId(item.getSubchapter().getId())
                .type(item.getType())
                .order(item.getOrder())
                .quizContent(item.getQuizContent());

        switch (item.getType().toLowerCase()) {
            case "text":
                builder.text(item.getText())
                        .fontSize(item.getFontSize())
                        .bolder(item.getBolder())
                        .textColor(item.getTextColor())
                        .italics(String.valueOf(item.getItalics()))
                        .underline(String.valueOf(item.getUnderline()));
                break;

            case "quiz":
                if (item.getQuizContent() != null) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode rootNode = mapper.readTree(item.getQuizContent());
                        // Pobierz bezpośrednio tablicę questions
                        JsonNode questionsArray = rootNode.get("questions");
                        if (questionsArray != null && questionsArray.isArray()) {
                            // Ustaw samą tablicę jako quizContent
                            builder.quizContent(mapper.treeToValue(questionsArray, Object.class));
                        }
                    } catch (JsonProcessingException e) {
                        throw new ApiException("Error parsing quiz content: " + e.getMessage());
                    }
                }
                break;

            case "video":
            case "image":
                if (item.getFile() != null) {
                    Map<String, Object> fileData = new HashMap<>();
                    fileData.put("data", item.getFile());
                    fileData.put("mimeType", item.getMimeType());
                    builder.file(fileData);
                }
                break;
        }

        return builder.build();
    }


    private void updateExistingSubchapter(SubchapterEntity subchapter, SubchapterUpdateDTO subchapterDTO) {
        subchapterDTO.getName().ifPresent(subchapter::setName);
        subchapterDTO.getOrder().ifPresent(subchapter::setOrder);
        subchapterRepository.save(subchapter);
    }

    public List<SubchapterUpdateDTO> mapSubchaptersToUpdateDTO(List<SubchapterEntity> subchapters) {
        return subchapters.stream()
                .map(this::mapSubchapterToUpdateDTO)
                .collect(Collectors.toList());
    }

    public SubchapterUpdateDTO mapSubchapterToUpdateDTO(SubchapterEntity subchapter) {
        return SubchapterUpdateDTO.builder()
                .id(subchapter.getId())
                .name(Optional.ofNullable(subchapter.getName()))
                .order(Optional.ofNullable(subchapter.getOrder()))
                .content(Optional.of(contentItemService.mapContentItemsToUpdateDTO(subchapter.getContent())))
                .deleted(Optional.of(false))
                .build();
    }

}
