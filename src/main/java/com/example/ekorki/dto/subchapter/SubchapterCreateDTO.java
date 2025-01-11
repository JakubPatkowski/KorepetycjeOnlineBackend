package com.example.ekorki.dto.subchapter;

import com.example.ekorki.dto.contentItem.ContentItemCreateDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import lombok.NoArgsConstructor;


import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubchapterCreateDTO {
    @NotBlank(message = "Subchapter name cannot be empty")
    private String name;
    private Integer order;
    private List<ContentItemCreateDTO> content;


}
