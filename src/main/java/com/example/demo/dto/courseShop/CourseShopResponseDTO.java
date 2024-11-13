package com.example.demo.dto.courseShop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseShopResponseDTO {
    private CourseDataDTO courseData;
    private OwnerDataDTO ownerData;
}
