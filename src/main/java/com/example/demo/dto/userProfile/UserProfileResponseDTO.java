package com.example.demo.dto.userProfile;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserProfileResponseDTO {
    private Long id;
    private String fullName;
    private String description;
    private Date createdAt;
    private Map<String, Object> picture;
    private Boolean badgesVisible;
    private Set<String> roles;
    private BigDecimal review;
    private Integer reviewNumber;
    private Date teacherProfileCreatedAt;
}
