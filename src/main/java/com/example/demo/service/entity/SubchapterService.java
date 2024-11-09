package com.example.demo.service.entity;

import com.example.demo.dto.contentItem.ContentItemCreateDTO;
import com.example.demo.dto.subchapter.SubchapterCreateDTO;
import com.example.demo.dto.subchapter.SubchapterUpdateDTO;
import com.example.demo.entity.ChapterEntity;
import com.example.demo.entity.CourseEntity;
import com.example.demo.entity.SubchapterEntity;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.ChapterRepository;
import com.example.demo.repository.SubchapterRepository;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubchapterService {
    @Autowired
    private final SubchapterRepository subchapterRepository;

    @Autowired
    private final ContentItemService contentItemService;

    @Transactional
    public SubchapterEntity createSubchapter(SubchapterCreateDTO dto, ChapterEntity chapter, int order) {
        SubchapterEntity subchapter = SubchapterEntity.builder()
                .chapter(chapter)
                .name(dto.getName())
                .order(order)
                .build();

        return subchapterRepository.save(subchapter);
    }

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
