package com.example.ekorki.dto.courseShop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseShopDetailsResponseDTO {
    private CourseShopDetailsDTO courseData;
    private OwnerDataDTO ownerData;
}
