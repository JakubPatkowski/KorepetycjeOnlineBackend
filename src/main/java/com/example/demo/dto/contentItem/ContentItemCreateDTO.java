package com.example.demo.dto.contentItem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentItemCreateDTO {
    @NotBlank(message = "Content type cannot be empty")
    private String type;

    private Integer order;

    private String textContent;
    private String fontSize;
    private String fontWeight;
    private Boolean italics;
    private Boolean emphasis;

    private Object quizData;




}
