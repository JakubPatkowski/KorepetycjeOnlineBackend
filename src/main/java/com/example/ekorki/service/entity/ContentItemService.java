package com.example.ekorki.service.entity;

import com.example.ekorki.dto.contentItem.ContentItemCreateDTO;
import com.example.ekorki.dto.contentItem.ContentItemUpdateDTO;
import com.example.ekorki.entity.ContentItemEntity;
import com.example.ekorki.entity.SubchapterEntity;
import com.example.ekorki.exception.ApiException;
import com.example.ekorki.repository.ContentItemRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.cache.annotation.CacheConfig;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "contentItems")
public class ContentItemService {
    @Autowired
    private final ContentItemRepository contentItemRepository;

    @Transactional
    public void createContentItems(List<ContentItemCreateDTO> dtos,
                                   SubchapterEntity subchapter,
                                   MultipartFile[] contentFiles) throws IOException {
        if (contentFiles == null) {
            contentFiles = new MultipartFile[0];
        }

        int fileIndex = 0;
        ObjectMapper objectMapper = new ObjectMapper();

        for (ContentItemCreateDTO dto : dtos) {
            ContentItemEntity contentItem = ContentItemEntity.builder()
                    .subchapter(subchapter)
                    .type(dto.getType())
                    .order(dto.getOrder())
                    .build();

            switch (dto.getType().toLowerCase()) {
                case "text":
                    contentItem.setText(dto.getText());
                    contentItem.setFontSize(dto.getFontSize());
                    contentItem.setBolder(dto.getBolder());
                    contentItem.setItalics(dto.getItalics());
                    contentItem.setUnderline(dto.getUnderline());
                    contentItem.setTextColor(dto.getTextColor());
                    break;

                case "image":
                case "video":
                    if (fileIndex >= contentFiles.length) {
                        throw new ApiException("Missing file for " + dto.getType() + " content at index " + fileIndex);
                    }

                    MultipartFile file = contentFiles[fileIndex];
                    if (file == null) {
                        throw new ApiException("File is null at index " + fileIndex);
                    }

                    processFile(contentItem, file);
                    fileIndex++;
                    break;

                case "quiz":
                    if (dto.getQuizContent() != null) {
                        processQuizContent(contentItem, dto.getQuizContent());
                    }
                    break;

                default:
                    throw new ApiException("Invalid content type: " + dto.getType());
            }

            contentItemRepository.save(contentItem);
        }
    }

    private void processQuizContent(ContentItemEntity contentItem, Object quizContent) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String quizDataJson;
            JsonNode jsonNode;

            // Konwersja wejścia na JsonNode
            if (quizContent instanceof String) {
                jsonNode = objectMapper.readTree((String) quizContent);
            } else {
                jsonNode = objectMapper.valueToTree(quizContent);
            }

            // Sprawdź czy dane są już w poprawnym formacie
            if (jsonNode.has("questions")) {
                contentItem.setQuizContent(objectMapper.writeValueAsString(jsonNode));
            } else {
                // Jeśli nie, owiń w obiekt z kluczem "questions"
                Map<String, JsonNode> wrappedContent = new HashMap<>();
                wrappedContent.put("questions", jsonNode);
                contentItem.setQuizContent(objectMapper.writeValueAsString(wrappedContent));
            }
        } catch (JsonProcessingException e) {
            throw new ApiException("Invalid quiz data format: " + e.getMessage());
        }
    }

    @Transactional
    public void updateContentItems(SubchapterEntity subchapter, List<ContentItemUpdateDTO> contentItemsDTO,
                                   MultipartFile[] contentFiles, Map<Long, Integer> fileIndexMap) {
        // Usuń elementy oznaczone do usunięcia
        contentItemsDTO.stream()
                .filter(itemDTO -> itemDTO.getId() != null && itemDTO.getDeleted().orElse(false))
                .forEach(itemDTO -> subchapter.getContent().removeIf(
                        item -> item.getId().equals(itemDTO.getId())));

        // Aktualizuj lub dodaj nowe elementy
        for (ContentItemUpdateDTO itemDTO : contentItemsDTO) {
            if (itemDTO.getDeleted().orElse(false)) continue;




            if (itemDTO.getId() != null) {
                ContentItemEntity item = subchapter.getContent().stream()
                        .filter(content -> content.getId().equals(itemDTO.getId()))
                        .findFirst()
                        .orElseThrow(() -> new ApiException("Content item not found: " + itemDTO.getId()));

                updateContentItemFields(item, itemDTO, contentFiles, fileIndexMap);
                contentItemRepository.save(item);
            } else {
                ContentItemEntity newItem = new ContentItemEntity();
                newItem.setSubchapter(subchapter);
                newItem.setOrder(itemDTO.getOrder().orElse(subchapter.getContent().size()));

                updateContentItemFields(newItem, itemDTO, contentFiles, fileIndexMap);
                subchapter.getContent().add(newItem);
                contentItemRepository.save(newItem);
            }
        }
    }

//    private void processContentItem(ContentItemEntity contentItem, ContentItemCreateDTO dto,
//                                    MultipartFile[] contentFiles, int fileIndex, ObjectMapper objectMapper) {
//        switch (dto.getType().toLowerCase()) {
//            case "text":
//                contentItem.setText(dto.getText());
//                contentItem.setFontSize(dto.getFontSize());
//                contentItem.setBolder(dto.getBolder());
//                contentItem.setItalics(dto.getItalics());
//                contentItem.setUnderline(dto.getUnderline());
//                contentItem.setTextColor(dto.getTextColor());
//                break;
//            case "video":
//            case "image":
//                if (contentFiles != null && fileIndex < contentFiles.length) {
//                    try {
//                        MultipartFile file = contentFiles[fileIndex];
//                        validateFile(file);
//                        contentItem.setFile(file.getBytes());
//                    } catch (IOException e) {
//                        throw new ApiException("Error processing file", e);
//                    }
//                }
//                break;
//            case "quiz":
//                if (dto.getQuizContent() != null) {
//                    try {
//                        // Konwertuj Object na String JSON
//                        String quizDataJson;
//                        if (dto.getQuizContent() instanceof String) {
//                            quizDataJson = (String) dto.getQuizContent();
//                        } else {
//                            quizDataJson = objectMapper.writeValueAsString(dto.getQuizContent());
//                        }
//
//                        // Waliduj JSON
//                        JsonNode jsonNode = objectMapper.readTree(quizDataJson);
//                        contentItem.setQuizContent(objectMapper.writeValueAsString(jsonNode));
//                    } catch (JsonProcessingException e) {
//                        throw new ApiException("Invalid quiz data format: " + e.getMessage(), e);
//                    }
//                }
//                break;
//            default:
//                throw new ApiException("Invalid content type: " + dto.getType());
//        }
//    }

    private void updateContentItemFields(ContentItemEntity item, ContentItemUpdateDTO itemDTO,
                                         MultipartFile[] contentFiles, Map<Long, Integer> fileIndexMap) {
        itemDTO.getType().ifPresent(item::setType);
        itemDTO.getOrder().ifPresent(item::setOrder);

        if ("text".equals(item.getType())) {
            itemDTO.getText().ifPresent(item::setText);
            itemDTO.getFontSize().ifPresent(item::setFontSize);
            itemDTO.getBolder().ifPresent(item::setBolder);
            itemDTO.getItalics().ifPresent(item::setItalics);
            itemDTO.getUnderline().ifPresent(item::setUnderline);
            itemDTO.getTextColor().ifPresent(item::setTextColor);
        } else if ("quiz".equals(item.getType())) {
            updateQuizData(item, itemDTO);
        } else if (Arrays.asList("video", "image").contains(item.getType())) {
            updateFileContent(item, itemDTO, contentFiles, fileIndexMap);
        }
    }

    private void updateQuizData(ContentItemEntity item, ContentItemUpdateDTO itemDTO) {
        itemDTO.getQuizContent().ifPresent(quizData -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode;

                // Konwersja wejścia na JsonNode
                if (quizData instanceof String) {
                    jsonNode = mapper.readTree((String) quizData);
                } else {
                    jsonNode = mapper.valueToTree(quizData);
                }

                // Sprawdź czy dane są już w poprawnym formacie
                if (jsonNode.has("questions")) {
                    item.setQuizContent(mapper.writeValueAsString(jsonNode));
                } else {
                    // Jeśli nie, owiń w obiekt z kluczem "questions"
                    Map<String, JsonNode> wrappedContent = new HashMap<>();
                    wrappedContent.put("questions", jsonNode);
                    item.setQuizContent(mapper.writeValueAsString(wrappedContent));
                }
            } catch (JsonProcessingException e) {
                throw new ApiException("Invalid quiz data format: " + e.getMessage());
            }
        });
    }

    private void updateFileContent(ContentItemEntity item, ContentItemUpdateDTO itemDTO,
                                   MultipartFile[] contentFiles, Map<Long, Integer> fileIndexMap) {
        if (itemDTO.getUpdateFile().orElse(false) && contentFiles != null) {
            int fileIndex = fileIndexMap.getOrDefault(item.getId(), fileIndexMap.size());
            if (fileIndex < contentFiles.length) {
                processFile(item, contentFiles[fileIndex]);
                fileIndexMap.put(item.getId(), fileIndex);
            }
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null) {
            throw new ApiException("File is required");
        }

        if (file.isEmpty()) {
            throw new ApiException("File is empty");
        }

        if (file.getSize() > 100 * 1024 * 1024) { // 100MB limit
            throw new ApiException("File size exceeds maximum limit of 100MB");
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
        Map<String, Object> fileData = null;
        if (item.getFile() != null && (item.getType().equals("image") || item.getType().equals("video"))) {
            fileData = new HashMap<>();
            fileData.put("data", item.getFile());
            fileData.put("mimeType", item.getMimeType());
        }

        Optional<Object> quizContentOpt = Optional.empty();
        if (item.getQuizContent() != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(item.getQuizContent());
                // Pobierz tablicę questions
                if (rootNode.has("questions")) {
                    quizContentOpt = Optional.of(rootNode.get("questions"));
                }
            } catch (JsonProcessingException e) {
                throw new ApiException("Error parsing quiz content: " + e.getMessage());
            }
        }

        return ContentItemUpdateDTO.builder()
                .id(item.getId())
                .type(Optional.ofNullable(item.getType()))
                .order(Optional.ofNullable(item.getOrder()))
                .text(Optional.ofNullable(item.getText()))
                .fontSize(Optional.ofNullable(item.getFontSize()))
                .bolder(Optional.ofNullable(item.getBolder()))
                .italics(Optional.ofNullable(item.getItalics()))
                .underline(Optional.ofNullable(item.getUnderline()))
                .textColor(Optional.ofNullable(item.getTextColor()))
                .quizContent(quizContentOpt)
                .file(Optional.ofNullable(fileData))
                .deleted(Optional.of(false))
                .updateFile(Optional.of(false))
                .build();
    }

    private void processFile(ContentItemEntity contentItem, MultipartFile file) {
        try {
            validateFile(file);
            contentItem.setFile(file.getBytes());
            contentItem.setMimeType(file.getContentType());
        } catch (IOException e) {
            throw new ApiException("Error processing file", e);
        }
    }
}

