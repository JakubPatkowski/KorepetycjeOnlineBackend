package com.example.demo.dto.courseShop;


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
public class CourseDataDTO {
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
}
