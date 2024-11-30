package com.example.demo.dto.courseShop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerDataDTO {
    private Long id;
    private String fullName;
    private Long userId;
    private String description;
    private Date createdAt;
    private Map<String, Object> picture;
    private Boolean badgesVisible;
    private Set<String> roles;
}
