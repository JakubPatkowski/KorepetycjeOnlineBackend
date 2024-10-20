package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateDTO {
    @NotNull(message = "User ID cannot be null")
    private Long userId;
    private Optional<String> fullName;
    private Optional<String> description;
    private Optional<byte[]> picture;
}
