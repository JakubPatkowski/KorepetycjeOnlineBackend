package com.example.demo.service.entity;

import com.example.demo.dto.contentItem.ContentItemCreateDTO;
import com.example.demo.dto.contentItem.ContentItemUpdateDTO;
import com.example.demo.entity.ContentItemEntity;
import com.example.demo.entity.SubchapterEntity;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.ContentItemRepository;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentItemService {
    @Autowired
    private final ContentItemRepository contentItemRepository;

    @Transactional
    public void createContentItems(List<ContentItemCreateDTO> dtos,
                                   SubchapterEntity subchapter,
                                   MultipartFile[] contentFiles) throws IOException {
        int fileIndex = 0;
        ObjectMapper objectMapper = new ObjectMapper();

        for(int i = 0; i < dtos.size(); i++) {
            ContentItemCreateDTO dto = dtos.get(i);
            ContentItemEntity contentItem = ContentItemEntity.builder()
                    .subchapter(subchapter)
                    .type(dto.getType())
                    .order(i)
                    .build();

            processContentItem(contentItem, dto, contentFiles, fileIndex++, objectMapper);
            contentItemRepository.save(contentItem);
        }
    }

    @Transactional
    public void updateContentItems(SubchapterEntity subchapter, List<ContentItemUpdateDTO> contentItemsDTO,
                                   MultipartFile[] contentFiles, Map<Long, Integer> fileIndexMap) {
        // Usuń elementy oznaczone do usunięcia
        contentItemsDTO.stream()
                .filter(itemDTO -> itemDTO.getId() != null && itemDTO.getDeleted().orElse(false))
                .forEach(itemDTO -> subchapter.getContentItems().removeIf(
                        item -> item.getId().equals(itemDTO.getId())));

        // Aktualizuj lub dodaj nowe elementy
        for (ContentItemUpdateDTO itemDTO : contentItemsDTO) {
            if (itemDTO.getDeleted().orElse(false)) continue;

            if (itemDTO.getId() != null) {
                ContentItemEntity item = subchapter.getContentItems().stream()
                        .filter(content -> content.getId().equals(itemDTO.getId()))
                        .findFirst()
                        .orElseThrow(() -> new ApiException("Content item not found: " + itemDTO.getId()));

                updateContentItemFields(item, itemDTO, contentFiles, fileIndexMap);
                contentItemRepository.save(item);
            } else {
                ContentItemEntity newItem = new ContentItemEntity();
                newItem.setSubchapter(subchapter);
                newItem.setOrder(itemDTO.getOrder().orElse(subchapter.getContentItems().size()));

                updateContentItemFields(newItem, itemDTO, contentFiles, fileIndexMap);
                subchapter.getContentItems().add(newItem);
                contentItemRepository.save(newItem);
            }
        }
    }

    private void processContentItem(ContentItemEntity contentItem, ContentItemCreateDTO dto,
                                    MultipartFile[] contentFiles, int fileIndex, ObjectMapper objectMapper) {
        switch (dto.getType().toLowerCase()) {
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
                    try {
                        MultipartFile file = contentFiles[fileIndex];
                        validateFile(file);
                        contentItem.setFile(file.getBytes());
                    } catch (IOException e) {
                        throw new ApiException("Error processing file", e);
                    }
                }
                break;
            case "quiz":
                if (dto.getQuizData() != null) {
                    try {
                        // Konwertuj Object na String JSON
                        String quizDataJson;
                        if (dto.getQuizData() instanceof String) {
                            quizDataJson = (String) dto.getQuizData();
                        } else {
                            quizDataJson = objectMapper.writeValueAsString(dto.getQuizData());
                        }

                        // Waliduj JSON
                        JsonNode jsonNode = objectMapper.readTree(quizDataJson);
                        contentItem.setQuizData(objectMapper.writeValueAsString(jsonNode));
                    } catch (JsonProcessingException e) {
                        throw new ApiException("Invalid quiz data format: " + e.getMessage(), e);
                    }
                }
                break;
            default:
                throw new ApiException("Invalid content type: " + dto.getType());
        }
    }

    private void updateContentItemFields(ContentItemEntity item, ContentItemUpdateDTO itemDTO,
                                         MultipartFile[] contentFiles, Map<Long, Integer> fileIndexMap) {
        itemDTO.getType().ifPresent(item::setType);
        itemDTO.getOrder().ifPresent(item::setOrder);

        if ("text".equals(item.getType())) {
            itemDTO.getTextContent().ifPresent(item::setTextContent);
            itemDTO.getFontSize().ifPresent(item::setFontSize);
            itemDTO.getFontWeight().ifPresent(item::setFontWeight);
            itemDTO.getItalics().ifPresent(item::setItalics);
            itemDTO.getEmphasis().ifPresent(item::setEmphasis);
        } else if ("quiz".equals(item.getType())) {
            updateQuizData(item, itemDTO);
        } else if (Arrays.asList("video", "image").contains(item.getType())) {
            updateFileContent(item, itemDTO, contentFiles, fileIndexMap);
        }
    }

    private void updateQuizData(ContentItemEntity item, ContentItemUpdateDTO itemDTO) {
        itemDTO.getQuizData().ifPresent(quizData -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(quizData);
                item.setQuizData(mapper.writeValueAsString(jsonNode));
            } catch (JsonProcessingException e) {
                throw new ApiException("Invalid quiz data format", e);
            }
        });
    }

    private void updateFileContent(ContentItemEntity item, ContentItemUpdateDTO itemDTO,
                                   MultipartFile[] contentFiles, Map<Long, Integer> fileIndexMap) {
        if (itemDTO.getUpdateFile().orElse(false) && contentFiles != null) {
            int fileIndex = fileIndexMap.getOrDefault(item.getId(), fileIndexMap.size());
            if (fileIndex < contentFiles.length) {
                try {
                    validateFile(contentFiles[fileIndex]);
                    item.setFile(contentFiles[fileIndex].getBytes());
                    fileIndexMap.put(item.getId(), fileIndex);
                } catch (IOException e) {
                    throw new ApiException("Error processing file", e);
                }
            }
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

    public List<ContentItemUpdateDTO> mapContentItemsToUpdateDTO(List<ContentItemEntity> contentItems) {
        return contentItems.stream()
                .map(this::mapContentItemToUpdateDTO)
                .collect(Collectors.toList());
    }

    public ContentItemUpdateDTO mapContentItemToUpdateDTO(ContentItemEntity item) {
        return ContentItemUpdateDTO.builder()
                .id(item.getId())
                .type(Optional.ofNullable(item.getType()))
                .order(Optional.ofNullable(item.getOrder()))
                .textContent(Optional.ofNullable(item.getTextContent()))
                .fontSize(Optional.ofNullable(item.getFontSize()))
                .fontWeight(Optional.ofNullable(item.getFontWeight()))
                .italics(Optional.ofNullable(item.getItalics()))
                .emphasis(Optional.ofNullable(item.getEmphasis()))
                .quizData(Optional.ofNullable(item.getQuizData()))
                .deleted(Optional.of(false))
                .updateFile(Optional.of(false))
                .build();
    }
}
