package com.example.ekorki.dto.courseShop;

import com.example.ekorki.dto.chapter.ChapterShortDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseShopDetailsDTO {
    private Long id;
    private String name;
    private Map<String, Object> banner;
    private BigDecimal price;
    private BigDecimal duration;
    private List<String> tags;
    private BigDecimal review;
    private Integer reviewNumber;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer chaptersCount;
    private Long ownerId;
    private CourseRelationshipType relationshipType;
    private List<ChapterShortDTO> chaptersShortInfo;


}
