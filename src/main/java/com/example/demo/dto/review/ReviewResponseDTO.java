package com.example.demo.dto.review;

import com.example.demo.dto.userProfile.UserProfileResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponseDTO {
    private Long id;
    private Integer rating;
    private String content;
    private LocalDateTime lastModified; // created_at lub updated_at, cokolwiek jest późniejsze
    private UserProfileResponseDTO userProfile;  // Zmienione z Map<String, Object>
}
