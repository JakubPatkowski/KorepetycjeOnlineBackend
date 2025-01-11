package com.example.ekorki.dto.review;

import com.example.ekorki.dto.userProfile.UserProfileResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
