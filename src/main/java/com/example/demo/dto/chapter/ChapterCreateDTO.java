package com.example.demo.dto.chapter;

import com.example.demo.dto.subchapter.SubchapterCreateDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChapterCreateDTO {
    @NotBlank(message = "Chapter name cannot be empty")
    private String name;
    private Integer order;
    private List<SubchapterCreateDTO> subchapters;

}
