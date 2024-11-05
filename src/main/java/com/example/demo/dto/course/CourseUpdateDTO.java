package com.example.demo.dto.course;

import com.example.demo.dto.chapter.ChapterShortDTO;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CourseUpdateDTO {
    private Long id;
    private String name;
    private byte[] bannerField;
    private BigDecimal price;
    private BigDecimal duration;
    private List<String> tags;
    private String description;
    private List<ChapterShortDTO> chaptersShortInfo;
}
