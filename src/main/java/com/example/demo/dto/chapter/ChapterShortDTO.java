package com.example.demo.dto.chapter;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.relational.core.sql.In;

import java.math.BigDecimal;
@Data
@Builder
public class ChapterShortDTO {
    private Long id;
    private Long courseId;
    private String name;
    private Integer order;
    private BigDecimal review;
    private Integer reviewNumber;
    private Integer subchapterNumber;
}