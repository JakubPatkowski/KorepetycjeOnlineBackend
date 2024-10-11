package com.example.demo.dto;

import jakarta.validation.constraints.*;

public record UserLoginDTO (
        @NotNull(message = "Email cannot be null")
        @NotBlank(message = "Email cannot be blank")
        @NotEmpty(message = "Email cannot be empty")
        @Email(message = "Enter a valid email")
        String email,
        @NotNull
        @NotNull(message = "Password cannot be null")
        @NotBlank(message = "Password cannot be blank")
        @NotEmpty(message = "Password cannot be empty")
        String password){
}
