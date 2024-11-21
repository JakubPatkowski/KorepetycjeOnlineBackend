package com.example.demo.dto.user;

import lombok.Data;

import java.util.Date;
import java.util.Set;
import java.util.Map;

@Data
public class UserResponseDTO {
    private String accessToken;
    private String refreshToken;
    private Long id;
    private String fullName;
    private String email;
    private int points;
    private Set<String> roles;
    private Map<String, Object> picture;
    private String[] badges;
    private Boolean badgesVisible;
    private String description;
    private Date createdAt;


}
