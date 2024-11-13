package com.example.demo.dto.subchapter;

import com.example.demo.dto.contentItem.ContentItemResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubchapterDetailsDTO {
    public Long id;
    public Long chapterId;
    public String name;
    public Integer order;
    public List<ContentItemResponseDTO> content;
}
