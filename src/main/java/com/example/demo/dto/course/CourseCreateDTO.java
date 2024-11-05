package com.example.demo.dto.course;

import com.example.demo.dto.chapter.ChapterCreateDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseCreateDTO {
    @NotBlank(message = "Name is required")
    private String name;
    private String description;
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;
    private BigDecimal duration;
    private List<String> tags;
    private List<ChapterCreateDTO> chapters;
}