package com.example.demo.dto;

import lombok.Data;

@Data
public class UserResponseDTO {
    private String accessToken;
    private String refreshToken;
    private Long id;
    private String nameAndSurname;
    private String email;
    private int points;
    private String role;
    private boolean verified;
    private boolean blocked;
    private boolean mfa;
    private byte[] picture;


}
