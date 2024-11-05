package com.example.demo.dto.course;

import com.example.demo.dto.chapter.ChapterShortDTO;
import lombok.Builder;
import lombok.Data;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CourseInfoDTO {
    private Long id;
    private String name;
    private byte[] banner;
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
