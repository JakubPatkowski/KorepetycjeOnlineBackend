package com.example.demo.dto.userProfile;

import lombok.Data;

import java.util.Date;
import java.util.Map;

@Data
public class UserProfileResponseDTO {
    private Long id;
    private String fullName;
    private Long userId;
    private String description;
    private Date createdAt;
    private Map<String, Object> picture;
    private Boolean badgesVisible;

}
