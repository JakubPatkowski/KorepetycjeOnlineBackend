package com.example.ekorki.dto.contentItem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentItemResponseDTO {
    public Long id;
    public Long subchapterId;
    public String type;
    public Integer order;
    public String text;
    public String fontSize;
    public Boolean bolder;
    public String textColor;
    public String italics;
    public String underline;
    private Map<String, Object> file;
    public Object quizContent;
}
