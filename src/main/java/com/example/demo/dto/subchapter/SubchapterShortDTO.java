package com.example.demo.dto.subchapter;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubchapterShortDTO {
    private Long id;
    private String name;
    private Integer order;
}
