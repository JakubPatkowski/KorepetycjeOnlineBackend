package com.example.ekorki.dto.course;

import com.example.ekorki.dto.chapter.ChapterUpdateDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseUpdateDTO {
    private Long id;
    private Optional<String> name;
    private Optional<Map<String, Object>> banner;
    private Optional<BigDecimal> price;
    private Optional<BigDecimal> duration;
    private Optional<List<String>> tags;
    private Optional<String> description;
    private Optional<List<ChapterUpdateDTO>> chapters;
}
