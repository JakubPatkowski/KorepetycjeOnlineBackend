package com.example.ekorki.dto.course;

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
public class CourseInfoDTO {
    private Long id;
    private String name;
    private Map<String, Object> banner;
    private BigDecimal review;
    private BigDecimal duration;
    private Long ownerId;
    private List<String> tags;
    private Integer reviewNumber;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChapterShortDTO> chaptersShortInfo;
}
