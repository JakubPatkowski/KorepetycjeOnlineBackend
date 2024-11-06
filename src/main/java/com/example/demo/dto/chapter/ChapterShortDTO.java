package com.example.demo.dto.chapter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@NoArgsConstructor
@AllArgsConstructor
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
