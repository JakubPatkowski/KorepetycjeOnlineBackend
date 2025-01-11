package com.example.ekorki.dto.chapter;

import com.example.ekorki.dto.subchapter.SubchapterShortDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChapterDetailsDTO {
    private Long id;
    private Long courseId;
    private String name;
    private Integer order;
    private BigDecimal review;
    private Integer reviewNumber;
    private List<SubchapterShortDTO> subchapters;
}
