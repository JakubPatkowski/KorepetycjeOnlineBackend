package com.example.demo.dto.user;

import lombok.Data;

@Data
public class UserResponseDTO {
    private String accessToken;
    private String refreshToken;
    private Long id;
    private String fullName;
    private String email;
    private int points;
    private String role;
    private byte[] picture;
    private String[] badges;
    private Boolean badgesVisible;
    private String description;


}
