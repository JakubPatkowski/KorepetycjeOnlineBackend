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

    private String text;
    private String fontSize;
    private Boolean bolder;
    private Boolean italics;
    private Boolean underline;
    private String textColor;

    private Object quizData;




}
